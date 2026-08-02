"""
{
    "plugin_type": ["scheduled"],
    "scheduled_args_config": [
        {
            "name": "config_file_path",
            "example": "mqtt_config.toml",
            "description": "Path to TOML configuration file (absolute or relative to PLUGIN_DIR).",
            "required": false
        },
        {
            "name": "broker_host",
            "example": "broker.hivemq.com",
            "description": "MQTT broker hostname or IP address.",
            "required": true
        },
        {
            "name": "broker_port",
            "example": "1883",
            "description": "MQTT broker port. Default: 1883 for non-TLS, 8883 for TLS.",
            "required": false
        },
        {
            "name": "topics",
            "example": "sensors/temperature sensors/humidity",
            "description": "Space-separated list of MQTT topics to subscribe to.",
            "required": true
        },
        {
            "name": "format",
            "example": "json",
            "description": "Message format: 'json', 'lineprotocol', or 'text'. Default: 'json'.",
            "required": false
        },
        {
            "name": "table_name",
            "example": "sensor_data",
            "description": "InfluxDB table name for JSON and text messages.",
            "required": false
        },
        {
            "name": "table_name_field",
            "example": "measurement",
            "description": "Field or regex used to extract the table name.",
            "required": false
        },
        {
            "name": "tags",
            "example": "location sensor_id",
            "description": "Tag mappings.",
            "required": false
        },
        {
            "name": "fields",
            "example": "temp:float=temperature hum:int=humidity",
            "description": "Field mappings.",
            "required": false
        },
        {
            "name": "timestamp_field",
            "example": "timestamp:ms",
            "description": "Timestamp field.",
            "required": false
        },
        {
            "name": "qos",
            "example": "1",
            "description": "MQTT QoS level.",
            "required": false
        },
        {
            "name": "client_id",
            "example": "influxdb3_mqtt_client",
            "description": "MQTT client identifier.",
            "required": false
        },
        {
            "name": "username",
            "example": "mqtt_user",
            "description": "MQTT username.",
            "required": false
        },
        {
            "name": "password",
            "example": "mqtt_password",
            "description": "MQTT password.",
            "required": false
        },
        {
            "name": "ca_cert",
            "example": "certs/ca.crt",
            "description": "CA certificate path.",
            "required": false
        },
        {
            "name": "client_cert",
            "example": "certs/client.crt",
            "description": "Client certificate path.",
            "required": false
        },
        {
            "name": "client_key",
            "example": "certs/client.key",
            "description": "Client private key path.",
            "required": false
        }
    ]
}
"""

import hashlib
import json
import os
import re
import time
import tomllib
import uuid

from dataclasses import dataclass
from datetime import datetime
from queue import Empty, Queue
from typing import Any, Protocol, runtime_checkable

from jsonpath_ng import parse as jsonpath_parse
from paho.mqtt.client import CallbackAPIVersion, Client


# =============================================================================
# Line builder helpers
# =============================================================================

@runtime_checkable
class _LineBuilderInterface(Protocol):
    def build(self) -> str:
        ...


class _BatchLines:
    """
    Helper for batching multiple line protocol builders into one write.
    """

    def __init__(self, line_builders: list[_LineBuilderInterface]):
        self._line_builders = list(line_builders)
        self._built: str | None = None

    def build(self) -> str:
        if self._built is None:
            lines = [str(builder.build()) for builder in self._line_builders]

            if not lines:
                raise ValueError("batch_write received no lines to build")

            self._built = "\n".join(lines)

        return self._built


def add_field_with_type(
    line,
    field_key: str,
    value: Any,
    field_type: str,
):
    """
    Add a field to LineBuilder with explicit type conversion.

    Supported types:
        int, uint, float, string, bool
    """
    if field_type == "int":
        line.int64_field(field_key, int(value))

    elif field_type == "uint":
        line.uint64_field(field_key, int(value))

    elif field_type == "float":
        line.float64_field(field_key, float(value))

    elif field_type == "string":
        line.string_field(field_key, str(value))

    elif field_type == "bool":
        if isinstance(value, str):
            converted = value.lower() in (
                "true",
                "t",
                "1",
                "yes",
                "on",
            )
        else:
            converted = bool(value)

        line.bool_field(field_key, converted)

    else:
        raise ValueError(
            f"Unknown field type: {field_type}. "
            "Supported types: int, uint, float, string, bool"
        )


# =============================================================================
# Timestamp helpers
# =============================================================================

def convert_timestamp(value: Any, time_format: str) -> int:
    """
    Convert a timestamp to nanoseconds.

    Supported formats:
        ns, ms, s, datetime
    """
    if time_format == "ns":
        return int(value)

    if time_format == "ms":
        return int(value) * 1_000_000

    if time_format == "s":
        return int(value) * 1_000_000_000

    if time_format == "datetime":
        if not isinstance(value, str):
            raise ValueError(
                f"datetime format requires string value, got {type(value)}"
            )

        dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
        return int(dt.timestamp() * 1_000_000_000)

    raise ValueError(
        f"Unknown time format: {time_format}. "
        "Supported formats: ns, ms, s, datetime"
    )


DEFAULT_MAX_AGE_DAYS = 90


def is_timestamp_within_retention(
    timestamp_ns: int,
    max_age_days: int = DEFAULT_MAX_AGE_DAYS,
) -> bool:
    cutoff_ns = time.time_ns() - (
        max_age_days * 24 * 60 * 60 * 1_000_000_000
    )

    return timestamp_ns >= cutoff_ns


# =============================================================================
# Unchanged-value filtering
# =============================================================================

@dataclass
class PendingRecord:
    """
    A record waiting to be written.

    The key identifies the measurement/tag/field combination.
    """
    key: str
    value: Any
    line: Any


class UnchangedRecordFilter:
    """
    Suppresses JSON records whose value has not changed since the last
    successfully written value.

    Values are persisted using influxdb3_local.cache.
    """

    CACHE_KEY = "mqtt_last_written_values"

    def __init__(self, influxdb3_local, task_id: str):
        self.influxdb3_local = influxdb3_local
        self.task_id = task_id

        cached_values = influxdb3_local.cache.get(self.CACHE_KEY)

        if isinstance(cached_values, dict):
            self.last_values: dict[str, Any] = cached_values
        else:
            self.last_values = {}

    @staticmethod
    def _normalize_value(value: Any) -> Any:
        """
        Normalize values before comparison.

        Numeric values are normalized to float so values such as 1 and 1.0
        compare as equal. Booleans remain separate from numeric values.
        """
        if isinstance(value, bool):
            return value

        if isinstance(value, int | float):
            return float(value)

        if value is None:
            return None

        return str(value)

    @staticmethod
    def _create_key(
        table_name: str,
        tags: dict[str, Any],
        field_name: str,
    ) -> str:
        """
        Create a stable key for:

            measurement/table + tags + field name
        """
        normalized_tags = [
            (str(key), str(value))
            for key, value in sorted(
                tags.items(),
                key=lambda item: str(item[0]),
            )
        ]

        raw_key = json.dumps(
            {
                "table": table_name,
                "tags": normalized_tags,
                "field": field_name,
            },
            sort_keys=True,
            separators=(",", ":"),
        )

        return hashlib.sha256(
            raw_key.encode("utf-8")
        ).hexdigest()

    def should_write(
        self,
        table_name: str,
        tags: dict[str, Any],
        field_name: str,
        value: Any,
    ) -> tuple[bool, str]:
        """
        Determine whether a record should be written.

        The cache is intentionally not updated here. It is updated only after
        the InfluxDB write succeeds.
        """
        key = self._create_key(
            table_name=table_name,
            tags=tags,
            field_name=field_name,
        )

        normalized_value = self._normalize_value(value)

        if key in self.last_values:
            if self.last_values[key] == normalized_value:
                return False, key

        return True, key

    def mark_written(self, records: list[PendingRecord]) -> None:
        """
        Update the cache after a successful database write.
        """
        for record in records:
            self.last_values[record.key] = self._normalize_value(
                record.value
            )

        self.influxdb3_local.cache.put(
            self.CACHE_KEY,
            self.last_values,
        )


# =============================================================================
# Configuration
# =============================================================================

class MQTTConfig:
    """
    Configuration loader and validator for the MQTT plugin.
    """

    VALID_TIMESTAMP_FORMATS = {
        "ns",
        "ms",
        "s",
        "datetime",
    }

    VALID_FIELD_TYPES = {
        "int",
        "uint",
        "float",
        "string",
        "bool",
    }

    def __init__(
        self,
        influxdb3_local,
        args: dict[str, str] | None,
        task_id: str,
    ):
        self.influxdb3_local = influxdb3_local
        self.args: dict[str, Any] = args or {}
        self.config: dict[str, Any] = {}
        self.task_id = task_id

        self._load_config()

    def _load_config(self):
        config_file = self.args.get("config_file_path")

        if config_file:
            self.config = self._load_toml_config(config_file)
        else:
            self.config = self._build_config_from_args()

    @staticmethod
    def _resolve_path(path: str, description: str) -> str:
        if os.path.isabs(path):
            return path

        plugin_dir = os.environ.get("PLUGIN_DIR")

        if not plugin_dir:
            raise ValueError(
                "PLUGIN_DIR environment variable not set. "
                f"Required for relative {description} path: {path}"
            )

        return os.path.join(plugin_dir, path)

    def _load_toml_config(self, config_file: str) -> dict[str, Any]:
        config_path = self._resolve_path(
            config_file,
            "configuration file",
        )

        if not os.path.exists(config_path):
            raise FileNotFoundError(
                f"Configuration file not found: {config_path}"
            )

        with open(config_path, "rb") as file:
            config = tomllib.load(file)

        self._validate_toml_config(config)

        return config

    def _validate_and_parse_timestamp_field(
        self,
        timestamp_field: str,
        field_name: str,
    ) -> dict[str, str]:
        if ":" in timestamp_field:
            field_path, time_format = timestamp_field.rsplit(":", 1)

            field_path = field_path.strip()
            time_format = time_format.strip()

            if time_format not in self.VALID_TIMESTAMP_FORMATS:
                raise ValueError(
                    f"Invalid timestamp format in '{field_name}': "
                    f"'{time_format}'. Supported formats: "
                    f"{', '.join(sorted(self.VALID_TIMESTAMP_FORMATS))}"
                )

            return {
                "field": field_path,
                "format": time_format,
            }

        return {
            "field": timestamp_field.strip(),
            "format": "ns",
        }

    def _validate_toml_config(self, config: dict[str, Any]):
        if "mqtt" not in config:
            raise ValueError(
                "Missing required 'mqtt' section in configuration"
            )

        mqtt_config = config["mqtt"]

        if "broker_host" not in mqtt_config:
            raise ValueError(
                "Missing required parameter 'mqtt.broker_host'"
            )

        if "topics" not in mqtt_config:
            raise ValueError(
                "Missing required parameter 'mqtt.topics'"
            )

        topics = mqtt_config["topics"]

        if not isinstance(topics, list) or not topics:
            raise ValueError(
                "Parameter 'mqtt.topics' must be a non-empty list"
            )

        message_format = mqtt_config.get("format", "json")

        if message_format == "json":
            self._validate_json_mapping(config)

        elif message_format == "text":
            self._validate_text_mapping(config)

        elif message_format == "lineprotocol":
            pass

        else:
            raise ValueError(
                f"Invalid message format: {message_format}. "
                "Supported formats: json, text, lineprotocol"
            )

    def _validate_json_mapping(self, config: dict[str, Any]):
        if "mapping" not in config or "json" not in config["mapping"]:
            raise ValueError(
                "Missing required 'mapping.json' section"
            )

        json_mapping = config["mapping"]["json"]

        if (
            "table_name" not in json_mapping
            and "table_name_field" not in json_mapping
        ):
            raise ValueError(
                "Missing required parameter 'mapping.json.table_name' "
                "or 'mapping.json.table_name_field'"
            )

        if "fields" not in json_mapping or not json_mapping["fields"]:
            raise ValueError(
                "Missing required parameter 'mapping.json.fields'"
            )

        if "timestamp_field" in json_mapping:
            parsed_timestamp = (
                self._validate_and_parse_timestamp_field(
                    json_mapping["timestamp_field"],
                    "mapping.json.timestamp_field",
                )
            )

            json_mapping["timestamp_config"] = parsed_timestamp
            del json_mapping["timestamp_field"]

    def _validate_text_mapping(self, config: dict[str, Any]):
        if "mapping" not in config or "text" not in config["mapping"]:
            raise ValueError(
                "Missing required 'mapping.text' section"
            )

        text_mapping = config["mapping"]["text"]

        if (
            "table_name" not in text_mapping
            and "table_name_field" not in text_mapping
        ):
            raise ValueError(
                "Missing required parameter 'mapping.text.table_name' "
                "or 'mapping.text.table_name_field'"
            )

        if "fields" not in text_mapping or not text_mapping["fields"]:
            raise ValueError(
                "Missing required parameter 'mapping.text.fields'"
            )

        fields = text_mapping["fields"]

        for field_name, field_config in fields.items():
            if (
                not isinstance(field_config, list)
                or len(field_config) != 2
            ):
                raise ValueError(
                    f"Invalid field configuration for "
                    f"'mapping.text.fields.{field_name}'. "
                    'Expected format: ["pattern", "type"]'
                )

            pattern, field_type = field_config

            if not isinstance(pattern, str) or not pattern:
                raise ValueError(
                    f"Invalid pattern for "
                    f"'mapping.text.fields.{field_name}'"
                )

            if field_type not in self.VALID_FIELD_TYPES:
                raise ValueError(
                    f"Invalid field type '{field_type}' for "
                    f"'mapping.text.fields.{field_name}'. Supported types: "
                    f"{', '.join(sorted(self.VALID_FIELD_TYPES))}"
                )

        if "timestamp_field" in text_mapping:
            parsed_timestamp = (
                self._validate_and_parse_timestamp_field(
                    text_mapping["timestamp_field"],
                    "mapping.text.timestamp_field",
                )
            )

            text_mapping["timestamp_config"] = parsed_timestamp
            del text_mapping["timestamp_field"]

    def _build_config_from_args(self) -> dict[str, Any]:
        required_keys = [
            "topics",
            "broker_host",
        ]

        message_format = self.args.get("format", "json")

        if message_format in {"json", "text"}:
            if not self.args.get("table_name_field"):
                required_keys.append("table_name")

        if not self.args or any(
            key not in self.args for key in required_keys
        ):
            raise ValueError(
                "Missing some required arguments: "
                f"{', '.join(required_keys)}"
            )

        topics_arg = self.args.get("topics", "")
        topics_list = topics_arg.split()

        auth_config: dict[str, Any] = {}

        username = self.args.get("username")
        password = self.args.get("password")

        if username and password:
            auth_config = {
                "username": username,
                "password": password,
            }

        elif username or password:
            raise ValueError(
                "Both username and password must be provided"
            )

        tls_config: dict[str, Any] = {}

        ca_cert = self.args.get("ca_cert")
        client_cert = self.args.get("client_cert")
        client_key = self.args.get("client_key")

        if ca_cert:
            tls_config["ca_cert"] = ca_cert

            if client_cert and client_key:
                tls_config["client_cert"] = client_cert
                tls_config["client_key"] = client_key

            elif client_cert or client_key:
                raise ValueError(
                    "Both client_cert and client_key must be provided"
                )

        return {
            "mqtt": {
                "broker_host": self.args.get("broker_host"),
                "broker_port": int(
                    self.args.get("broker_port", 1883)
                ),
                "topics": topics_list,
                "qos": int(self.args.get("qos", 1)),
                "client_id": self.args.get(
                    "client_id",
                    "influxdb3_mqtt_subscriber",
                ),
                "format": message_format,
                "auth": auth_config,
                "tls": tls_config,
            },
            "mapping": self._build_mapping_from_args(),
        }

    def _build_mapping_from_args(self) -> dict[str, Any]:
        message_format = self.args.get("format", "json")

        if message_format == "json":
            return self._build_json_mapping_from_args()

        if message_format == "text":
            return self._build_text_mapping_from_args()

        if message_format == "lineprotocol":
            return {}

        raise ValueError(
            f"Unsupported format: {message_format}"
        )

    def _build_json_mapping_from_args(self) -> dict[str, Any]:
        tags_config: dict[str, str] = {}

        tags_arg = self.args.get("tags")

        if tags_arg:
            for tag_name in tags_arg.split():
                tag_name = tag_name.strip()

                if tag_name:
                    tags_config[tag_name] = f"$.{tag_name}"

        fields_config: dict[str, list[str]] = {}

        fields_arg = self.args.get("fields")

        if fields_arg:
            for field_spec in fields_arg.split():
                field_spec = field_spec.strip()

                if not field_spec:
                    continue

                if ":" not in field_spec:
                    raise ValueError(
                        f"Invalid field specification: '{field_spec}'"
                    )

                field_name, rest = field_spec.split(":", 1)

                if "=" not in rest:
                    raise ValueError(
                        f"Invalid field specification: '{field_spec}'"
                    )

                field_type, json_path = rest.split("=", 1)

                field_name = field_name.strip()
                field_type = field_type.strip()
                json_path = json_path.strip()

                if field_type not in self.VALID_FIELD_TYPES:
                    raise ValueError(
                        f"Invalid field type '{field_type}'"
                    )

                if field_name and json_path:
                    fields_config[field_name] = [
                        f"$.{json_path}",
                        field_type,
                    ]

        timestamp_config = None
        timestamp_arg = self.args.get("timestamp_field")

        if timestamp_arg:
            if ":" not in timestamp_arg:
                raise ValueError(
                    "timestamp_field must use field_name:time_format"
                )

            field_name, time_format = timestamp_arg.split(":", 1)

            if time_format not in self.VALID_TIMESTAMP_FORMATS:
                raise ValueError(
                    f"Invalid timestamp format: {time_format}"
                )

            timestamp_config = {
                "field": f"$.{field_name.strip()}",
                "format": time_format.strip(),
            }

        json_config: dict[str, Any] = {
            "timestamp_config": timestamp_config,
            "tags": tags_config,
            "fields": fields_config,
        }

        table_name = self.args.get("table_name")

        if table_name:
            json_config["table_name"] = table_name

        table_name_field = self.args.get("table_name_field")

        if table_name_field:
            json_config["table_name_field"] = (
                f"$.{table_name_field}"
            )

        return {
            "json": json_config,
        }

    def _build_text_mapping_from_args(self) -> dict[str, Any]:
        tags_config: dict[str, str] = {}

        tags_arg = self.args.get("tags")

        if tags_arg:
            for tag_spec in tags_arg.split():
                if "=" not in tag_spec:
                    raise ValueError(
                        f"Invalid tag specification: '{tag_spec}'"
                    )

                tag_name, pattern = tag_spec.split("=", 1)

                if tag_name and pattern:
                    tags_config[tag_name.strip()] = pattern.strip()

        fields_config: dict[str, list[str]] = {}

        fields_arg = self.args.get("fields")

        if fields_arg:
            for field_spec in fields_arg.split():
                if ":" not in field_spec:
                    raise ValueError(
                        f"Invalid field specification: '{field_spec}'"
                    )

                field_name, rest = field_spec.split(":", 1)

                if "=" not in rest:
                    raise ValueError(
                        f"Invalid field specification: '{field_spec}'"
                    )

                field_type, pattern = rest.split("=", 1)

                field_name = field_name.strip()
                field_type = field_type.strip()
                pattern = pattern.strip()

                if field_type not in self.VALID_FIELD_TYPES:
                    raise ValueError(
                        f"Invalid field type '{field_type}'"
                    )

                if field_name and pattern:
                    fields_config[field_name] = [
                        pattern,
                        field_type,
                    ]

        timestamp_config = None
        timestamp_arg = self.args.get("timestamp_field")

        if timestamp_arg:
            if ":" not in timestamp_arg:
                raise ValueError(
                    "timestamp_field must use regex:time_format"
                )

            pattern, time_format = timestamp_arg.rsplit(":", 1)

            if time_format not in self.VALID_TIMESTAMP_FORMATS:
                raise ValueError(
                    f"Invalid timestamp format: {time_format}"
                )

            timestamp_config = {
                "field": pattern.strip(),
                "format": time_format.strip(),
            }

        text_config: dict[str, Any] = {
            "timestamp_config": timestamp_config,
            "tags": tags_config,
            "fields": fields_config,
        }

        table_name = self.args.get("table_name")

        if table_name:
            text_config["table_name"] = table_name

        table_name_field = self.args.get("table_name_field")

        if table_name_field:
            text_config["table_name_field"] = table_name_field

        return {
            "text": text_config,
        }

    def get(self, key: str, default: Any = None):
        return self.config.get(key, default)

    def get_mqtt_config(self) -> dict[str, Any]:
        return self.config.get("mqtt", {})

    def get_mapping_config(self, format_type: str) -> dict[str, Any]:
        mapping = self.config.get("mapping", {})
        return mapping.get(format_type, {})


# =============================================================================
# MQTT connection
# =============================================================================

class MQTTConnectionManager:
    """
    Manages the MQTT client connection and message queue.
    """

    def __init__(
        self,
        config: dict[str, Any],
        influxdb3_local,
        task_id: str,
    ):
        self.config = config
        self.influxdb3_local = influxdb3_local
        self.task_id = task_id

        self.client = None
        self.message_queue: Queue = Queue()
        self.connected = False
        self.subscribed_topics: set[str] = set()

    @staticmethod
    def _create_mqtt_client(client_id: str):
        return Client(
            callback_api_version=CallbackAPIVersion.VERSION2,
            client_id=client_id,
            clean_session=False,
        )

    @staticmethod
    def _resolve_path(path: str, description: str) -> str:
        if os.path.isabs(path):
            return path

        plugin_dir = os.environ.get("PLUGIN_DIR")

        if not plugin_dir:
            raise ValueError(
                "PLUGIN_DIR environment variable not set. "
                f"Required for relative {description} path: {path}"
            )

        return os.path.join(plugin_dir, path)

    def _configure_tls(self, tls_config: dict[str, Any]):
        ca_cert = tls_config.get("ca_cert")
        client_cert = tls_config.get("client_cert")
        client_key = tls_config.get("client_key")

        if not ca_cert:
            self.influxdb3_local.info(
                f"[{self.task_id}] No ca_cert specified; "
                "skipping TLS configuration"
            )
            return

        ca_cert = self._resolve_path(ca_cert, "CA certificate")

        if client_cert:
            client_cert = self._resolve_path(
                client_cert,
                "client certificate",
            )

        if client_key:
            client_key = self._resolve_path(
                client_key,
                "client key",
            )

        if not os.path.exists(ca_cert):
            raise FileNotFoundError(
                f"CA certificate not found: {ca_cert}"
            )

        if client_cert and not os.path.exists(client_cert):
            raise FileNotFoundError(
                f"Client certificate not found: {client_cert}"
            )

        if client_key and not os.path.exists(client_key):
            raise FileNotFoundError(
                f"Client key not found: {client_key}"
            )

        self.client.tls_set(
            ca_certs=ca_cert,
            certfile=client_cert,
            keyfile=client_key,
        )

        self.influxdb3_local.info(
            f"[{self.task_id}] TLS configured successfully"
        )

    def connect(self) -> bool:
        try:
            client_id = self.config.get(
                "client_id",
                "influxdb3_mqtt_subscriber",
            )

            self.client = self._create_mqtt_client(client_id)

            self.client.on_connect = self._on_connect
            self.client.on_disconnect = self._on_disconnect
            self.client.on_message = self._on_message

            auth_config = self.config.get("auth", {})

            username = auth_config.get("username")
            password = auth_config.get("password")

            if username:
                self.client.username_pw_set(username, password)

            tls_config = self.config.get("tls")

            if tls_config:
                self._configure_tls(tls_config)

            broker = self.config.get("broker_host")
            port = self.config.get("broker_port", 1883)

            self.influxdb3_local.info(
                f"[{self.task_id}] Connecting to MQTT broker: "
                f"{broker}:{port}"
            )

            self.client.connect(broker, port)
            self.client.loop_start()

            timeout = 10
            start_time = time.time()

            while (
                not self.connected
                and time.time() - start_time < timeout
            ):
                time.sleep(0.1)

            if not self.connected:
                self.influxdb3_local.error(
                    f"[{self.task_id}] Failed to connect to MQTT broker "
                    "within timeout"
                )
                return False

            return True

        except Exception as error:
            self.influxdb3_local.error(
                f"[{self.task_id}] Error connecting to MQTT broker: "
                f"{error}"
            )
            return False

    def _on_connect(
        self,
        client,
        userdata,
        flags,
        reason_code,
        properties,
    ):
        if reason_code == 0:
            self.connected = True

            self.influxdb3_local.info(
                f"[{self.task_id}] MQTT client connected successfully"
            )

            topics = self.config.get("topics", [])
            qos = self.config.get("qos", 1)

            for topic in topics:
                if topic in self.subscribed_topics:
                    continue

                try:
                    client.subscribe(topic, qos)
                    self.subscribed_topics.add(topic)

                    self.influxdb3_local.info(
                        f"[{self.task_id}] Subscribed to topic: "
                        f"{topic} (QoS {qos})"
                    )

                except Exception as error:
                    self.influxdb3_local.error(
                        f"[{self.task_id}] Error subscribing to topic "
                        f"{topic}: {error}"
                    )

        else:
            self.connected = False

            self.influxdb3_local.error(
                f"[{self.task_id}] MQTT connection failed with code: "
                f"{reason_code}"
            )

    def _on_disconnect(
        self,
        client,
        userdata,
        disconnect_flags,
        reason_code,
        properties,
    ):
        self.connected = False

        if reason_code != 0:
            self.influxdb3_local.info(
                f"[{self.task_id}] MQTT disconnected with code: "
                f"{reason_code}"
            )

    def _on_message(self, client, userdata, msg):
        try:
            try:
                payload = msg.payload.decode("utf-8")

            except UnicodeDecodeError:
                self.influxdb3_local.warn(
                    f"[{self.task_id}] Skipping binary message on topic "
                    f"{msg.topic} ({len(msg.payload)} bytes)"
                )
                return

            if not payload or not payload.strip():
                self.influxdb3_local.warn(
                    f"[{self.task_id}] Skipping empty message on topic "
                    f"{msg.topic}"
                )
                return

            self.message_queue.put(
                {
                    "topic": msg.topic,
                    "payload": payload,
                    "qos": msg.qos,
                }
            )

        except Exception as error:
            self.influxdb3_local.error(
                f"[{self.task_id}] Error processing MQTT message: "
                f"{error}"
            )

    def get_messages(self) -> list[dict[str, Any]]:
        messages = []

        try:
            while True:
                messages.append(self.message_queue.get_nowait())

        except Empty:
            pass

        return messages

    def disconnect(self):
        if self.client:
            self.client.loop_stop()
            self.client.disconnect()
            self.connected = False

            self.influxdb3_local.info(
                f"[{self.task_id}] Disconnected from MQTT broker"
            )


# =============================================================================
# JSON parser
# =============================================================================

class JSONParser:
    """
    JSON parser supporting:

    - 1:N attribute mapping
    - Dynamic field typing
    - Attribute filtering
    - Static tags
    - JSONPath mappings
    """

    def __init__(
        self,
        mapping_config: dict[str, Any],
        task_id: str,
        influxdb3_local,
    ):
        self.mapping_config = mapping_config
        self.task_id = task_id
        self.influxdb3_local = influxdb3_local

        self.raw_tags = mapping_config.get("tags", {})

        self.included_attributes = set(
            mapping_config.get("included_attributes", [])
        )

        self._compiled_paths = (
            self._compile_jsonpath_expressions()
        )

    def _compile_jsonpath_expressions(self) -> dict[str, Any]:
        compiled: dict[str, Any] = {}

        def smart_compile(path_str):
            if not str(path_str).startswith("$"):
                return None

            return jsonpath_parse(path_str)

        if "attribute_name_path" in self.mapping_config:
            compiled["attr_name"] = smart_compile(
                self.mapping_config["attribute_name_path"]
            )

        if "table_name_field" in self.mapping_config:
            compiled["table_name"] = smart_compile(
                self.mapping_config["table_name_field"]
            )

        for tag_key, path in self.raw_tags.items():
            expression = smart_compile(path)

            if expression:
                compiled[f"tag:{tag_key}"] = expression

        for field_key, spec in self.mapping_config.get(
            "fields",
            {},
        ).items():
            if isinstance(spec, list):
                compiled[f"field:{field_key}"] = smart_compile(
                    spec[0]
                )

            elif isinstance(spec, dict):
                compiled[f"field:{field_key}"] = smart_compile(
                    spec.get("path")
                )

                if "type_path" in spec:
                    compiled[f"type:{field_key}"] = smart_compile(
                        spec["type_path"]
                    )

        if "timestamp_config" in self.mapping_config:
            timestamp_path = self.mapping_config[
                "timestamp_config"
            ].get("field")

            if timestamp_path:
                compiled["timestamp"] = smart_compile(timestamp_path)

        return compiled

    def parse(self, payload: str) -> list[LineBuilder]:
        try:
            data = json.loads(payload)
            items = data if isinstance(data, list) else [data]

            results = []

            for item in items:
                results.extend(self._parse_single_object(item))

            return results

        except Exception as error:
            self.influxdb3_local.error(
                f"[{self.task_id}] Parse failed: {error}"
            )
            return []

    def _parse_single_object(
        self,
        data: dict,
    ) -> list[LineBuilder]:
        static_table = self.mapping_config.get("table_name")

        table_name_matches = (
            self._get_values(data, "table_name")
            if not static_table
            else []
        )

        attribute_name_matches = self._get_values(
            data,
            "attr_name",
        )

        tag_results = {}

        for tag_key in self.raw_tags:
            tag_results[tag_key] = self._get_values(
                data,
                f"tag:{tag_key}",
            )

        field_map = {}
        max_rows = 1

        for field_key, spec in self.mapping_config.get(
            "fields",
            {},
        ).items():
            is_dict = isinstance(spec, dict)

            default_type = (
                spec.get("type", "string")
                if is_dict
                else spec[1]
            )

            type_mapping = (
                spec.get("type_mapping", {})
                if is_dict
                else {}
            )

            matches = self._get_values(
                data,
                f"field:{field_key}",
            )

            type_matches = (
                self._get_values(
                    data,
                    f"type:{field_key}",
                )
                if is_dict
                else []
            )

            if matches:
                field_map[field_key] = (
                    matches,
                    default_type,
                    type_matches,
                    type_mapping,
                )

                max_rows = max(max_rows, len(matches))

        if attribute_name_matches:
            max_rows = max(
                max_rows,
                len(attribute_name_matches),
            )

        if table_name_matches:
            max_rows = max(
                max_rows,
                len(table_name_matches),
            )

        base_timestamp = self._get_timestamp(data)

        if not is_timestamp_within_retention(base_timestamp):
            return []

        builders = []

        for index in range(max_rows):
            if (
                self.included_attributes
                and attribute_name_matches
            ):
                current_attribute = str(
                    attribute_name_matches[index]
                    if index < len(attribute_name_matches)
                    else attribute_name_matches[-1]
                )

                if current_attribute not in self.included_attributes:
                    continue

            current_table = static_table

            if not current_table:
                if table_name_matches:
                    current_table = str(
                        table_name_matches[index]
                        if index < len(table_name_matches)
                        else table_name_matches[-1]
                    )
                else:
                    continue

            line = LineBuilder(current_table)

            for tag_key, values in tag_results.items():
                if values:
                    tag_value = (
                        values[index]
                        if index < len(values)
                        else values[0]
                    )

                    line.tag(tag_key, str(tag_value))

            fields_added = 0

            for (
                field_key,
                (
                    field_values,
                    default_type,
                    type_matches,
                    type_mapping,
                ),
            ) in field_map.items():
                if not field_values:
                    continue

                value = (
                    field_values[index]
                    if index < len(field_values)
                    else field_values[-1]
                )

                field_type = default_type

                if type_matches:
                    raw_type = str(
                        type_matches[index]
                        if index < len(type_matches)
                        else type_matches[-1]
                    )

                    field_type = type_mapping.get(
                        raw_type,
                        default_type,
                    )

                add_field_with_type(
                    line,
                    field_key,
                    value,
                    field_type,
                )

                fields_added += 1

            if fields_added > 0:
                line.time_ns(base_timestamp)
                builders.append(line)

        return builders

    def _get_values(
        self,
        data: Any,
        cache_key: str,
    ) -> list[Any]:
        expression = self._compiled_paths.get(cache_key)

        if expression:
            return [
                match.value
                for match in expression.find(data)
            ]

        if cache_key.startswith("tag:"):
            tag_name = cache_key.replace("tag:", "")
            raw_value = self.raw_tags.get(tag_name)

            if raw_value and not str(raw_value).startswith("$"):
                return [raw_value]

        return []

    def _get_timestamp(self, data: dict) -> int:
        timestamp_config = self.mapping_config.get(
            "timestamp_config"
        )

        if not timestamp_config:
            return time.time_ns()

        matches = self._get_values(data, "timestamp")

        if not matches or matches[0] is None:
            return time.time_ns()

        return convert_timestamp(
            matches[0],
            timestamp_config.get("format", "ns"),
        )


class FilteredJSONParser(JSONParser):
    """
    JSON parser that suppresses records whose values have not changed.

    It returns PendingRecord instances so the cache can be updated only after
    the batch write succeeds.
    """

    def __init__(
        self,
        mapping_config: dict[str, Any],
        task_id: str,
        influxdb3_local,
        unchanged_filter: UnchangedRecordFilter,
    ):
        super().__init__(
            mapping_config,
            task_id,
            influxdb3_local,
        )

        self.unchanged_filter = unchanged_filter

    def parse(self, payload: str) -> list[PendingRecord]:
        try:
            data = json.loads(payload)
            items = data if isinstance(data, list) else [data]

            records: list[PendingRecord] = []

            for item in items:
                records.extend(
                    self._parse_single_object(item)
                )

            return records

        except Exception as error:
            self.influxdb3_local.error(
                f"[{self.task_id}] Parse failed: {error}"
            )
            return []

    def _parse_single_object(
        self,
        data: dict,
    ) -> list[PendingRecord]:
        static_table = self.mapping_config.get("table_name")

        table_name_matches = (
            self._get_values(data, "table_name")
            if not static_table
            else []
        )

        attribute_name_matches = self._get_values(
            data,
            "attr_name",
        )

        tag_results = {}

        for tag_key in self.raw_tags:
            tag_results[tag_key] = self._get_values(
                data,
                f"tag:{tag_key}",
            )

        field_map = {}
        max_rows = 1

        for field_key, spec in self.mapping_config.get(
            "fields",
            {},
        ).items():
            is_dict = isinstance(spec, dict)

            default_type = (
                spec.get("type", "string")
                if is_dict
                else spec[1]
            )

            type_mapping = (
                spec.get("type_mapping", {})
                if is_dict
                else {}
            )

            matches = self._get_values(
                data,
                f"field:{field_key}",
            )

            type_matches = (
                self._get_values(
                    data,
                    f"type:{field_key}",
                )
                if is_dict
                else []
            )

            if matches:
                field_map[field_key] = (
                    matches,
                    default_type,
                    type_matches,
                    type_mapping,
                )

                max_rows = max(max_rows, len(matches))

        if attribute_name_matches:
            max_rows = max(
                max_rows,
                len(attribute_name_matches),
            )

        if table_name_matches:
            max_rows = max(
                max_rows,
                len(table_name_matches),
            )

        base_timestamp = self._get_timestamp(data)

        if not is_timestamp_within_retention(base_timestamp):
            return []

        pending_records: list[PendingRecord] = []

        for index in range(max_rows):
            if (
                self.included_attributes
                and attribute_name_matches
            ):
                current_attribute = str(
                    attribute_name_matches[index]
                    if index < len(attribute_name_matches)
                    else attribute_name_matches[-1]
                )

                if current_attribute not in self.included_attributes:
                    continue

            current_table = static_table

            if not current_table:
                if table_name_matches:
                    current_table = str(
                        table_name_matches[index]
                        if index < len(table_name_matches)
                        else table_name_matches[-1]
                    )
                else:
                    continue

            row_tags: dict[str, str] = {}

            for tag_key, values in tag_results.items():
                if values:
                    tag_value = (
                        values[index]
                        if index < len(values)
                        else values[0]
                    )

                    row_tags[tag_key] = str(tag_value)

            for (
                field_key,
                (
                    field_values,
                    default_type,
                    type_matches,
                    type_mapping,
                ),
            ) in field_map.items():
                if not field_values:
                    continue

                value = (
                    field_values[index]
                    if index < len(field_values)
                    else field_values[-1]
                )

                field_type = default_type

                if type_matches:
                    raw_type = str(
                        type_matches[index]
                        if index < len(type_matches)
                        else type_matches[-1]
                    )

                    field_type = type_mapping.get(
                        raw_type,
                        default_type,
                    )

                should_write, cache_key = (
                    self.unchanged_filter.should_write(
                        table_name=current_table,
                        tags=row_tags,
                        field_name=field_key,
                        value=value,
                    )
                )

                if not should_write:
                    continue

                line = LineBuilder(current_table)

                for tag_key, tag_value in row_tags.items():
                    line.tag(tag_key, tag_value)

                add_field_with_type(
                    line,
                    field_key,
                    value,
                    field_type,
                )

                line.time_ns(base_timestamp)

                pending_records.append(
                    PendingRecord(
                        key=cache_key,
                        value=value,
                        line=line,
                    )
                )

        return pending_records


# =============================================================================
# Line protocol parser
# =============================================================================

class LineProtocolParser:
    """
    Parse InfluxDB line protocol and convert it to LineBuilder.
    """

    def __init__(self, influxdb3_local, task_id: str):
        self.influxdb3_local = influxdb3_local
        self.task_id = task_id

    def parse(self, payload: str) -> LineBuilder:
        try:
            payload = payload.strip()

            parts = self._split_quoted(
                payload,
                " ",
                max_splits=2,
                skip_empty=True,
            )

            if len(parts) < 2:
                raise ValueError(
                    "Invalid line protocol format: "
                    "missing field set"
                )

            measurement_and_tags = parts[0]
            fields_str = parts[1]

            timestamp_ns = (
                int(parts[2])
                if len(parts) == 3
                else None
            )

            measurement, tags = (
                self._parse_measurement_and_tags(
                    measurement_and_tags
                )
            )

            line = LineBuilder(measurement)

            for tag_key, tag_value in tags.items():
                line.tag(tag_key, tag_value)

            fields = self._parse_fields(fields_str)

            if not fields:
                raise ValueError(
                    "No fields found in line protocol"
                )

            for field_key, (
                field_value,
                field_type,
            ) in fields.items():
                add_field_with_type(
                    line,
                    field_key,
                    field_value,
                    field_type,
                )

            if timestamp_ns is not None:
                line.time_ns(timestamp_ns)

            return line

        except Exception as error:
            self.influxdb3_local.error(
                f"[{self.task_id}] Error parsing line protocol: "
                f"{error}"
            )
            raise

    def _split_quoted(
        self,
        text: str,
        delimiter: str,
        max_splits: int = -1,
        skip_empty: bool = False,
    ) -> list[str]:
        parts = []
        current = []
        in_quotes = False
        splits_made = 0

        for char in text:
            if char == '"':
                backslash_count = 0
                index = len(current) - 1

                while (
                    index >= 0
                    and current[index] == "\\"
                ):
                    backslash_count += 1
                    index -= 1

                if backslash_count % 2 == 0:
                    in_quotes = not in_quotes

                current.append(char)

            elif char == delimiter and not in_quotes:
                if 0 < max_splits <= splits_made:
                    current.append(char)

                else:
                    if current or not skip_empty:
                        parts.append("".join(current))

                    current = []
                    splits_made += 1

            else:
                current.append(char)

        if current or not skip_empty:
            parts.append("".join(current))

        return parts

    def _parse_measurement_and_tags(
        self,
        measurement_and_tags: str,
    ) -> tuple[str, dict[str, str]]:
        parts = measurement_and_tags.split(",")

        measurement = parts[0]
        tags = {}

        for tag_part in parts[1:]:
            if "=" not in tag_part:
                raise ValueError(
                    "Invalid line protocol tag: "
                    f"{tag_part}"
                )

            key, value = tag_part.split("=", 1)
            tags[key] = self._unescape_value(value)

        return measurement, tags

    def _parse_fields(
        self,
        fields_str: str,
    ) -> dict[str, tuple[Any, str]]:
        fields = {}

        field_parts = self._split_quoted(
            fields_str,
            ",",
        )

        for field_part in field_parts:
            if "=" not in field_part:
                continue

            key, value_str = field_part.split("=", 1)

            key = key.strip()
            value_str = value_str.strip()

            value, field_type = (
                self._parse_field_value(value_str)
            )

            fields[key] = (value, field_type)

        return fields

    def _parse_field_value(
        self,
        value_str: str,
    ) -> tuple[Any, str]:
        if (
            value_str.startswith('"')
            and value_str.endswith('"')
            and len(value_str) >= 2
        ):
            return (
                self._unescape_value(value_str[1:-1]),
                "string",
            )

        if value_str.endswith("i"):
            return int(value_str[:-1]), "int"

        if value_str.endswith("u"):
            return int(value_str[:-1]), "uint"

        lower_value = value_str.lower()

        if lower_value in {
            "true",
            "t",
            "false",
            "f",
        }:
            return (
                lower_value in {"true", "t"},
                "bool",
            )

        try:
            return float(value_str), "float"

        except ValueError:
            raise ValueError(
                f"Invalid field value: {value_str}"
            )

    def _unescape_value(self, value: str) -> str:
        return (
            value
            .replace("\\,", ",")
            .replace("\\=", "=")
            .replace("\\ ", " ")
            .replace("\\\\", "\\")
            .replace('\\"', '"')
        )


# =============================================================================
# Text parser
# =============================================================================

class TextParser:
    """
    Parse text messages using regular expressions.
    """

    def __init__(
        self,
        mapping_config: dict[str, Any],
        task_id: str,
        influxdb3_local,
    ):
        self.mapping_config = mapping_config
        self.task_id = task_id
        self.influxdb3_local = influxdb3_local

        self._compiled_patterns = (
            self._compile_regex_patterns()
        )

    def _compile_regex_patterns(self) -> dict[str, re.Pattern]:
        compiled = {}

        table_name_field = self.mapping_config.get(
            "table_name_field"
        )

        if table_name_field:
            try:
                compiled["table_name"] = re.compile(
                    table_name_field
                )

            except re.error as error:
                self.influxdb3_local.warn(
                    f"[{self.task_id}] Invalid table_name_field regex: "
                    f"{error}"
                )

        for tag_key, pattern_str in self.mapping_config.get(
            "tags",
            {},
        ).items():
            try:
                compiled[f"tag:{tag_key}"] = re.compile(
                    pattern_str
                )

            except re.error as error:
                self.influxdb3_local.warn(
                    f"[{self.task_id}] Invalid regex for tag "
                    f"'{tag_key}': {error}"
                )

        for field_key, pattern_config in self.mapping_config.get(
            "fields",
            {},
        ).items():
            try:
                compiled[f"field:{field_key}"] = re.compile(
                    pattern_config[0]
                )

            except re.error as error:
                self.influxdb3_local.warn(
                    f"[{self.task_id}] Invalid regex for field "
                    f"'{field_key}': {error}"
                )

        timestamp_config = self.mapping_config.get(
            "timestamp_config"
        )

        if timestamp_config:
            pattern_str = timestamp_config.get("field")

            if pattern_str:
                try:
                    compiled["timestamp"] = re.compile(
                        pattern_str
                    )

                except re.error as error:
                    self.influxdb3_local.warn(
                        f"[{self.task_id}] Invalid timestamp regex: "
                        f"{error}"
                    )

        return compiled

    def _get_table_name(self, payload: str) -> str | None:
        table_name = self.mapping_config.get("table_name")

        if table_name:
            return table_name

        table_name_field = self.mapping_config.get(
            "table_name_field"
        )

        if table_name_field:
            return self._extract_value(
                payload,
                table_name_field,
                "table_name",
                "table_name",
            )

        return None

    def parse(self, payload: str) -> LineBuilder:
        try:
            table_name = self._get_table_name(payload)

            if not table_name:
                raise ValueError(
                    "Could not determine table name"
                )

            line = LineBuilder(table_name)

            for tag_key, pattern_str in self.mapping_config.get(
                "tags",
                {},
            ).items():
                value = self._extract_value(
                    payload,
                    pattern_str,
                    tag_key,
                    f"tag:{tag_key}",
                )

                if value is not None:
                    line.tag(tag_key, value)

            fields_config = self.mapping_config.get(
                "fields",
                {},
            )

            if not fields_config:
                raise ValueError(
                    "No field patterns configured"
                )

            field_count = 0

            for field_key, pattern_config in fields_config.items():
                pattern_str = pattern_config[0]
                field_type = pattern_config[1]

                value = self._extract_value(
                    payload,
                    pattern_str,
                    field_key,
                    f"field:{field_key}",
                )

                if value is not None:
                    add_field_with_type(
                        line,
                        field_key,
                        value,
                        field_type,
                    )

                    field_count += 1

            if field_count == 0:
                raise ValueError(
                    "No fields were extracted"
                )

            line.time_ns(self._get_timestamp(payload))

            return line

        except Exception as error:
            self.influxdb3_local.error(
                f"[{self.task_id}] Error parsing text: {error}"
            )
            raise

    def _extract_value(
        self,
        text: str,
        pattern_str: str,
        field_name: str,
        cache_key: str | None = None,
    ) -> str | None:
        try:
            if (
                cache_key
                and cache_key in self._compiled_patterns
            ):
                pattern = self._compiled_patterns[cache_key]
            else:
                pattern = re.compile(pattern_str)

            match = pattern.search(text)

            if not match:
                self.influxdb3_local.warn(
                    f"[{self.task_id}] Pattern for '{field_name}' "
                    f"did not match: {pattern_str}"
                )
                return None

            if match.groups():
                return match.group(1)

            return match.group(0)

        except re.error as error:
            self.influxdb3_local.error(
                f"[{self.task_id}] Invalid regex for '{field_name}': "
                f"{error}"
            )
            return None

    def _get_timestamp(self, payload: str) -> int:
        timestamp_config = self.mapping_config.get(
            "timestamp_config"
        )

        if not timestamp_config:
            return time.time_ns()

        pattern_str = timestamp_config.get("field")

        if not pattern_str:
            return time.time_ns()

        timestamp_value = self._extract_value(
            payload,
            pattern_str,
            "timestamp",
            "timestamp",
        )

        if timestamp_value is None:
            return time.time_ns()

        time_format = timestamp_config.get(
            "format",
            "ns",
        )

        try:
            return convert_timestamp(
                timestamp_value,
                time_format,
            )

        except Exception as error:
            self.influxdb3_local.error(
                f"[{self.task_id}] Failed to convert timestamp "
                f"'{timestamp_value}': {error}"
            )
            return time.time_ns()


# =============================================================================
# Statistics and exception storage
# =============================================================================

class MQTTStats:
    """
    Track and persist MQTT plugin statistics.
    """

    def __init__(self):
        self.reset()

    def reset(self):
        self.messages_received = 0
        self.messages_processed = 0
        self.messages_failed = 0

        self.stats_by_topic: dict[str, dict[str, int]] = {}
        self.last_message_time: int | None = None
        self.current_topic: str | None = None

    def record_message_received(
        self,
        topic: str,
        count: int = 1,
    ):
        self.messages_received += count
        self.last_message_time = time.time_ns()
        self.current_topic = topic

        if topic not in self.stats_by_topic:
            self.stats_by_topic[topic] = {
                "received": 0,
                "processed": 0,
                "failed": 0,
            }

        self.stats_by_topic[topic]["received"] += count

    def record_message_processed(self, count: int = 1):
        self.messages_processed += count

        if (
            self.current_topic
            and self.current_topic in self.stats_by_topic
        ):
            self.stats_by_topic[
                self.current_topic
            ]["processed"] += count

    def record_message_failed(self, count: int = 1):
        self.messages_failed += count

        if (
            self.current_topic
            and self.current_topic in self.stats_by_topic
        ):
            self.stats_by_topic[
                self.current_topic
            ]["failed"] += count

    def get_topic_stats(self) -> dict[str, dict[str, Any]]:
        result = {}

        for topic, stats in self.stats_by_topic.items():
            total = stats["processed"] + stats["failed"]

            success_rate = (
                stats["processed"] / total * 100
                if total > 0
                else 0.0
            )

            result[topic] = {
                "received": stats["received"],
                "processed": stats["processed"],
                "failed": stats["failed"],
                "success_rate": round(success_rate, 2),
            }

        return result


def write_stats(
    influxdb3_local,
    stats: MQTTStats,
    broker_host: str,
    task_id: str,
):
    try:
        topic_stats = stats.get_topic_stats()
        lines = []

        for topic, topic_data in topic_stats.items():
            line = LineBuilder("mqtt_stats")

            line.tag("topic", topic)
            line.tag("broker_host", broker_host)

            line.int64_field(
                "messages_received",
                topic_data["received"],
            )

            line.int64_field(
                "messages_processed",
                topic_data["processed"],
            )

            line.int64_field(
                "messages_failed",
                topic_data["failed"],
            )

            line.float64_field(
                "success_rate",
                topic_data["success_rate"],
            )

            line.time_ns(time.time_ns())
            lines.append(line)

        if lines:
            influxdb3_local.write_sync(
                _BatchLines(lines),
                no_sync=True,
            )

        influxdb3_local.info(
            f"[{task_id}] Wrote statistics for "
            f"{len(topic_stats)} topics"
        )

    except Exception as error:
        influxdb3_local.error(
            f"[{task_id}] Failed to write statistics: {error}"
        )


def write_exception(
    influxdb3_local,
    topic: str,
    error_type: str,
    error_message: str,
    raw_message: str,
    task_id: str,
):
    try:
        line = LineBuilder("mqtt_exceptions")

        line.tag("topic", topic)
        line.tag("error_type", error_type)

        line.string_field(
            "error_message",
            error_message,
        )

        line.string_field(
            "raw_message",
            raw_message,
        )

        line.time_ns(time.time_ns())

        influxdb3_local.write_sync(
            line,
            no_sync=True,
        )

        influxdb3_local.info(
            f"[{task_id}] Wrote exception to mqtt_exceptions: "
            f"{error_type}"
        )

    except Exception as error:
        influxdb3_local.error(
            f"[{task_id}] Failed to write exception: {error}"
        )


# =============================================================================
# Scheduled entry point
# =============================================================================

def process_scheduled_call(
    influxdb3_local,
    call_time: datetime,
    args: dict | None = None,
):
    """
    Main scheduled plugin entry point.
    """
    task_id = str(uuid.uuid4())
    mqtt_client: MQTTConnectionManager | None = None

    if not args:
        influxdb3_local.error(
            f"[{task_id}] No arguments provided"
        )
        return

    try:
        cached_config = influxdb3_local.cache.get(
            "mqtt_config"
        )

        if cached_config is None:
            config_loader = MQTTConfig(
                influxdb3_local,
                args,
                task_id,
            )

            cached_config = {
                "mqtt": config_loader.get_mqtt_config(),
                "mapping": {
                    "json": config_loader.get_mapping_config(
                        "json"
                    ),
                    "text": config_loader.get_mapping_config(
                        "text"
                    ),
                },
            }

            influxdb3_local.cache.put(
                "mqtt_config",
                cached_config,
            )

            influxdb3_local.info(
                f"[{task_id}] MQTT plugin initialized; "
                f"format: {cached_config['mqtt'].get('format')}"
            )

        mqtt_config = cached_config["mqtt"]
        message_format = mqtt_config.get("format", "json")

        stats = influxdb3_local.cache.get("mqtt_stats")

        if stats is None:
            stats = MQTTStats()
            influxdb3_local.cache.put(
                "mqtt_stats",
                stats,
            )

        unchanged_filter = UnchangedRecordFilter(
            influxdb3_local,
            task_id,
        )

        influxdb3_local.info(
            f"[{task_id}] Creating new MQTT connection"
        )

        mqtt_client = MQTTConnectionManager(
            mqtt_config,
            influxdb3_local,
            task_id,
        )

        if not mqtt_client.connect():
            influxdb3_local.error(
                f"[{task_id}] Failed to connect to MQTT broker"
            )
            return

        time.sleep(0.5)

        messages = mqtt_client.get_messages()

        call_count = influxdb3_local.cache.get(
            "mqtt_call_count"
        )

        if call_count is None:
            call_count = 0

        call_count += 1

        broker_string = (
            f"{mqtt_config.get('broker_host')}:"
            f"{mqtt_config.get('broker_port')}"
        )

        if call_count >= 10:
            write_stats(
                influxdb3_local,
                stats,
                broker_string,
                task_id,
            )
            call_count = 0

        influxdb3_local.cache.put(
            "mqtt_call_count",
            call_count,
        )

        if not messages:
            return

        influxdb3_local.info(
            f"[{task_id}] Processing {len(messages)} messages"
        )

        if message_format == "json":
            mapping_config = cached_config["mapping"].get(
                "json",
                {},
            )

            parser = FilteredJSONParser(
                mapping_config,
                task_id,
                influxdb3_local,
                unchanged_filter,
            )

        elif message_format == "lineprotocol":
            parser = LineProtocolParser(
                influxdb3_local,
                task_id,
            )

        elif message_format == "text":
            mapping_config = cached_config["mapping"].get(
                "text",
                {},
            )

            parser = TextParser(
                mapping_config,
                task_id,
                influxdb3_local,
            )

        else:
            influxdb3_local.error(
                f"[{task_id}] Unknown message format: "
                f"{message_format}"
            )
            return

        # ---------------------------------------------------------------------
        # Phase 1: Parse all messages and collect records
        # ---------------------------------------------------------------------

        all_line_builders = []
        pending_records: list[PendingRecord] = []

        # Tuple format:
        #   (message, status, record_count_or_exception)
        parse_results = []

        for message in messages:
            topic = message.get("topic", "unknown")
            payload = message.get("payload", "")

            stats.record_message_received(topic)

            try:
                if message_format == "json":
                    records = parser.parse(payload)

                    pending_records.extend(records)

                    all_line_builders.extend(
                        record.line
                        for record in records
                    )

                    parse_results.append(
                        (
                            message,
                            "ok",
                            len(records),
                        )
                    )

                else:
                    line_builder = parser.parse(payload)

                    if line_builder:
                        all_line_builders.append(
                            line_builder
                        )

                        parse_results.append(
                            (
                                message,
                                "ok",
                                1,
                            )
                        )
                    else:
                        parse_results.append(
                            (
                                message,
                                "ok",
                                0,
                            )
                        )

            except Exception as error:
                parse_results.append(
                    (
                        message,
                        "fail",
                        error,
                    )
                )

        # ---------------------------------------------------------------------
        # Phase 2: Write changed records
        # ---------------------------------------------------------------------

        write_failed = False

        if all_line_builders:
            try:
                influxdb3_local.write_sync(
                    _BatchLines(all_line_builders),
                    no_sync=True,
                )

                # Critical: update the unchanged-value cache only after the
                # InfluxDB write succeeds.
                if pending_records:
                    unchanged_filter.mark_written(
                        pending_records
                    )

            except Exception as error:
                write_failed = True

                influxdb3_local.error(
                    f"[{task_id}] Batch write failed: {error}"
                )

        # ---------------------------------------------------------------------
        # Phase 3: Update statistics and write exceptions
        # ---------------------------------------------------------------------

        success_count = 0
        error_count = 0

        for message, status, result in parse_results:
            topic = message.get("topic", "unknown")
            payload = message.get("payload", "")

            if status == "ok" and not write_failed:
                success_count += result
                stats.record_message_processed(1)
                continue

            error_count += 1
            stats.record_message_failed()

            if status == "fail":
                error_type = type(result).__name__
                error_message = str(result)

            else:
                error_type = "BatchWriteError"
                error_message = (
                    "Batch write to InfluxDB failed"
                )

            influxdb3_local.error(
                f"[{task_id}] Error processing message from "
                f"{topic}: {error_message}"
            )

            write_exception(
                influxdb3_local,
                topic,
                error_type,
                error_message,
                payload[:1000],
                task_id,
            )

        influxdb3_local.info(
            f"[{task_id}] Data write complete: "
            f"{success_count} records inserted; "
            f"{error_count} errors"
        )

    except Exception as error:
        influxdb3_local.error(
            f"[{task_id}] Error in MQTT plugin: {error}"
        )

        # Configuration errors should force configuration reload on the next
        # scheduled execution.
        influxdb3_local.cache.delete(
            "mqtt_config"
        )

    finally:
        if mqtt_client is not None:
            mqtt_client.disconnect()
