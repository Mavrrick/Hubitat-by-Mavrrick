import os
import requests
from datetime import datetime
from pydantic import BaseModel, Field


class Tools:
    def __init__(self):
        pass

    # Add your custom tools using pure Python code here, make sure to add type hints and descriptions

    def get_user_name_and_email_and_id(self, __user__: dict = {}) -> str:
        """
        Get the user name, Email and ID from the user object.
        """

        # Do not include a descrption for __user__ as it should not be shown in the tool's specification
        # The session user object will be passed as a parameter when the function is called

        print(__user__)
        result = ""

        if "name" in __user__:
            result += f"User: {__user__['name']}"
        if "id" in __user__:
            result += f" (ID: {__user__['id']})"
        if "email" in __user__:
            result += f" (Email: {__user__['email']})"

        if result == "":
            result = "User: Unknown"

        return result

    def get_current_time(self) -> str:
        """
        Get the current time in a more human-readable format.
        """

        now = datetime.now()
        current_time = now.strftime("%I:%M:%S %p")  # Using 12-hour format with AM/PM
        current_date = now.strftime(
            "%A, %B %d, %Y"
        )  # Full weekday, month name, day, and year

        return f"Current Date and Time = {current_date}, {current_time}"

    def get_current_weather(
        self,
        city: str = Field(
            "New York, NY", description="Get the current weather for a given city."
        ),
    ) -> str:
        """
        Get the current weather for a given city.
        """

        #        api_key = os.getenv("OPENWEATHER_API_KEY")
        api_key = "04811e146d6f0e5b312753013890309c"
        if not api_key:
            return (
                "API key is not set in the environment variable 'OPENWEATHER_API_KEY'."
            )

        base_url = "http://api.openweathermap.org/data/2.5/weather"
        params = {
            "q": city,
            "appid": api_key,
            "units": "metric",  # Optional: Use 'imperial' for Fahrenheit
        }

        try:
            response = requests.get(base_url, params=params)
            response.raise_for_status()  # Raise HTTPError for bad responses (4xx and 5xx)
            data = response.json()

            if data.get("cod") != 200:
                return f"Error fetching weather data: {data.get('message')}"

            weather_description = data["weather"][0]["description"]
            temperature = data["main"]["temp"]
            humidity = data["main"]["humidity"]
            wind_speed = data["wind"]["speed"]

            return f"Weather in {city}: {temperature}°C"
        except requests.RequestException as e:
            return f"Error fetching weather data: {str(e)}"

    def control_device(
        self,
        device: str = Field("Kitchen Light", description="Device(s) name to control"),
        #        room: str = Field("Kitchen", description="Area, Room, or grouping of devices"),
        command: str = Field("on", description="Command to call on device"),
        newValue: str = Field("on", description="Vale of command"),
    ) -> str:
        """
        Control device in Hubitat home
        """
        api_key = os.getenv("hubitat_apiKey")
        appID = os.getenv("hubitat_appID")
        #        api_key = "1a07b092-3b1a-4c56-9938-91facdf09e16"
        if not api_key:
            return (
                "API key is not set in the environment variable 'OPENWEATHER_API_KEY'."
            )

        base_url = "http://192.168.86.35/apps/api/" + appID + "/"

        params = {
            "access_token": api_key,
            "tool_call": "control_device",
            "device": device,
            #            "room": room,
            "command": command,
            "value": newValue,  # Optional: Use 'imperial' for Fahrenheit
        }

        try:
            response = requests.post(base_url, params=params)
            response.raise_for_status()  # Raise HTTPError for bad responses (4xx and 5xx)
            data = response.json()

            if data.get("status") != "success":
                return f"Error fetching weather data: {data.get('message')}"

            #            weather_description = data["weather"][0]["description"]
            #            temperature = data["main"]["temp"]
            #            humidity = data["main"]["humidity"]
            #            wind_speed = data["wind"]["speed"]

            return f"{data.get('message')}"
        except requests.RequestException as e:
            return f"Error fetching weather data: {str(e)}"

    def device_state_lookup(
        self,
        device: str = Field("Kitchen Light", description="Device(s) name to control"),
        stateType: str = Field(
            "temperature", description="State attribute to look up "
        ),
    ) -> str:
        """
        Used to look up device state/attributes in the home
        """

        api_key = os.getenv("hubitat_apiKey")
        appID = os.getenv("hubitat_appID")
        #        api_key = "1a07b092-3b1a-4c56-9938-91facdf09e16"
        if not api_key:
            return (
                "API key is not set in the environment variable 'OPENWEATHER_API_KEY'."
            )

        base_url = "http://192.168.86.35/apps/api/" + appID + "/"

        params = {
            "access_token": api_key,
            "tool_call": "device_state_lookup",
            "device": device,
            "stateType": stateType,
        }

        try:
            response = requests.post(base_url, params=params)
            response.raise_for_status()  # Raise HTTPError for bad responses (4xx and 5xx)
            data = response.json()

            if data.get("status") != "success":
                return f"Error fetching weather data: {data.get('message')}"

            #            weather_description = data["weather"][0]["description"]
            #            temperature = data["main"]["temp"]
            #            humidity = data["main"]["humidity"]
            #            wind_speed = data["wind"]["speed"]

            return f"{data.get('message')}"
        except requests.RequestException as e:
            return f"Error fetching weather data: {str(e)}"
