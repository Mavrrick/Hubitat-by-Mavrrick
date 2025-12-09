// Hubitat driver for Govee Device Manager Driver
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

import groovy.json.JsonSlurper
import hubitat.helper.HexUtils
import groovy.transform.Field

@Field static Number procTime = 0
@Field static Number callCount = 0

metadata {
	definition(name: "Govee v2 Device Manager", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Initialize" 
        attribute "connectionState", "string"
        attribute "msgCount", "integer" 
        command "allSceneReload"
        command "LookupLanAPIDevices"
        command "installNewDevices"
    }

	preferences {		
		section("Device Info") {  
            input(name: "disableMQTT", type: "bool", title: "Enable/Disable MQTT communication", defaultValue: true)
            input(name: "enableLanApiInstall", type: "bool", title: "Enable/Disable Automatic install of LAN API Devices", defaultValue: false)
            input(name: "scanRate", type: "number", title: "Scan rate in minutes(0-59) for new LAN API Devices", defaultValue:1, range: 0..59, submitOnChange: true, width:2)
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
            input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true) 
		}
		
	}
}

//////////////////////////////////////
// Standard Methods for all drivers //
//////////////////////////////////////

def updated() {
    initialize()
}


def installed(){
    initialize()
}

def initialize() {
//    goveeAPI = parent.state.goveeAppAPI
    disconnect()
    pauseExecution(1000)
    if (disableMQTT == true) {
        mqttConnectionAttempt()
    }
    sendEvent(name: "msgCount", value: 0)
    state.childCount = getChildDevices().size()
    allSceneReload()
    multicastCloseSocket(4001)
    multicastCloseSocket(4002)
    multicastListenerSocket(4001)
    multicastListenerSocket(4002)
    LookupLanAPIDevices()
    if (scanRate == 0) {
        unschedule()
    } else if (scanRate <= 59) {
        unschedule()
        String scanCron = '0 */'+scanRate+' * ? * *' 
        schedule(scanCron, LookupLanAPIDevices)
    } else {
        log.warn "ScanRate is invalid it will be ignored until fixed."    
    }
    RetrievechildDeviceInfo()
    if (debugLog) runIn(1800, logsOff)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

// put methods, etc. here

def mqttConnectionAttempt() {
	if (debugLog) log.debug "In mqttConnectionAttempt"
 
	if (!interfaces.mqtt.isConnected()) {
		try {   
			interfaces.mqtt.connect("ssl://mqtt.openapi.govee.com:8883",
							   "hubitat_${getHubId()}", 
                               device.getDataValue("apiKey"), 
							   device.getDataValue("apiKey"),
                               cleanSession: true)    

			// delay for connection
			pauseExecution(1000)
            
		} catch(Exception e) {
            log.error "In mqttConnectionAttempt: Error initializing. ${e}"
			if (!interfaces.mqtt.isConnected()) disconnected()
		}
	}
    
	if (interfaces.mqtt.isConnected()) {
        if (debugLog) log.debug "In mqttConnectionAttempt: Success connecting."
		unschedule(connect)
		connected()
    } else {
        if (debugLog) log.debug "In mqttConnectionAttempt: Failure connecting."
        disconnected()
    }
}

def subscribe() {
    mqttTopic = 'GA/'+device.getDataValue("apiKey")
    if (!interfaces.mqtt.isConnected()) {
        connect()
    }

    if (debugLog) log.debug "Subscribe to: ${mqttTopic}"
    interfaces.mqtt.subscribe(mqttTopic)
}

def unsubscribe(topic) {
    if (!interfaces.mqtt.isConnected()) {
        connect()
    }
    
    if (debugLog) log.debug "Unsubscribe from: hubitat/${getHubId()}/${topic}"
    interfaces.mqtt.unsubscribe("hubitat/${getHubId()}/${topic}")
}

def connect() {
    mqttConnectionAttempt()
}

def connected() {
	log.info "In connected: Connected to broker"
    sendEvent (name: "connectionState", value: "connected")
    subscribe()
}

def disconnect() {
	unschedule(heartbeat)

	if (interfaces.mqtt.isConnected()) {
		pauseExecution(1000)
		try {
			interfaces.mqtt.disconnect()
			pauseExecution(500)
			disconnected()
		} catch(e) {
			log.warn "Disconnection from broker failed."
			if (interfaces.mqtt.isConnected()) {
				connected()
			}
			else {
				disconnected()
			}
			return;
		}
	} 
	else {
		disconnected()
	}
}

def disconnected() {
	log.info "In disconnected: Disconnected from broker"
	sendEvent (name: "connectionState", value: "disconnected")
}

/////////////////////////////////////////////////////////////////////
// Parse
/////////////////////////////////////////////////////////////////////

void parse(String event) {
    long startTime = now()
    if (event.matches("(.*)topic(.*)")) {
        def jsonSlurper = new JsonSlurper()     
        if (debugLog) log.info "parse() MQTT message recieved. Parsing and sending to device"
        def payloadJson = jsonSlurper.parseText(interfaces.mqtt.parseMessage(event).payload)
        int mqttMsgCount = device.currentValue("msgCount").toInteger() + 1
        sendEvent(name: "msgCount", value: mqttMsgCount)
    
        if (debugLog) log.debug "In parse, received message: ${payloadJson} for deviceid is ${payloadJson.device} capability is ${payloadJson.capabilities}"

        mqttEventCreate(payloadJson.device, payloadJson.capabilities.get(0).instance, payloadJson.capabilities.get(0).state.get(0).name)
        
    } else {
        slurper = new JsonSlurper()
        fromIp = slurper.parseText(event).fromIp
        payloadJson = slurper.parse(HexUtils.hexStringToByteArray(slurper.parseText(event).payload))
        if (payloadJson.msg.cmd == "devStatus") {  
            if (!state.ipxdni ) {
                if (debugLog) log.info "parse() ipxdni is null. LAN API Device StatusMessage recieved but ignored. Calling for device Scan"  
                LookupLanAPIDevices()
            } else {
                if (debugLog) log.info "parse() LAN API Device StatusMessage received. Parsing and sending to device."
                def msg = payloadJson.msg
                def data = msg.data
                if (debugLog) log.info "received: sourceIP: ${fromIp}, cmd: ${msg.cmd}, data: ${data}"
                def deviceNetworkId = state.ipxdni[fromIp]
                if (deviceNetworkId) { 
                    def childDevice = getChildDevice(deviceNetworkId)
                    if (childDevice) {
                        if (debugLog) log.info "received: Device '${childDevice.deviceNetworkId}' is configured. Sending LAN API Post."
                        childDevice.lanAPIPost(data) 
                    } else {
                        log.warn "parse() Child device with DNI '${deviceNetworkId}' found in state.ipxdni for IP '${fromIp}' but not found as an actual child device. Cannot send LAN API Post."
                    }
                } else {
                    if (debugLog) log.info "parse() No child device configured for IP: '${fromIp}' in state.ipxdni. Skipping LAN API Post."
                }
            }
        } else if (payloadJson.msg.cmd == "scan") {
            if (payloadJson.msg.data.containsKey("account_topic")) {
                if (debugLog) log.info "parse() LAN API Discovery Message recieved. Return message ${payloadJson.msg.data}, sourceIP: ${fromIp}"
            } else {
                if (debugLog) log.info "parse() Returned entire Json: ${payloadJson} -- Source ip is ${fromIp}" //added for debug of missing IP 
                if (debugLog) log.info "parse() Device IP: ${payloadJson.msg.data.ip}, sku: ${payloadJson.msg.data.sku}, deviceId: ${payloadJson.msg.data.device}"
                if (!state.lanApiDevices) {
                    state.lanApiDevices = [:]
                } 
                if (!state.ipxdni) {
                    state.ipxdni = [:]
                } 
                if (state.lanApiDevices.containsKey(payloadJson.msg.data.device)) { 
                    def deviceId = payloadJson.msg.data.device
                    def newIpAddress = payloadJson.msg.data.ip
                    if (newIpAddress == null) {
                        if (debugLog) log.info "parse() Ip not pased returned by Govee Device. Using Source of Multicast response"
                        newIpAddress = fromIp
                    } 
                    def existingDeviceInfo = state.lanApiDevices[deviceId]
                    if (debugLog) log.info "parse() Device '${deviceId}' Found. Checking for update."
                    def currentTime = new Date().toString()
                    if (existingDeviceInfo.ip != newIpAddress) {
                        if (debugLog) log.info "parse() IP address for '${deviceId}' changed from ${existingDeviceInfo.ip} to ${newIpAddress}. Processing update."
                        existingDeviceInfo.ip = newIpAddress
                        existingDeviceInfo."last response" = currentTime
                        def childDevice = getChildDevice("Govee_${deviceId}")                    
                        if (childDevice) {
                            if (debugLog) log.info "parse() Found child device '${childDevice.deviceNetworkId}'. Calling updateIPAdd."
                            RetrievechildDeviceInfo()
                            childDevice.updateIPAdd(newIpAddress)
                        } else {
                            if (debugLog) log.warn "parse() Child device 'Govee_${deviceId}' not found for IP update, even though it's in lanApiDevices."
                        }
                    } else {
                        if (debugLog) log.info "parse() No IP change for device '${deviceId}'. Updating last response timestamp only."
                        existingDeviceInfo."last response" = currentTime
                    }
                } else {
                    def msgData = payloadJson.msg.data
                    if (debugLog) log.info "parse() Device not found in current list. Adding to list"
                    def newIpAddress = payloadJson.msg.data.ip
                    if (newIpAddress == null) {
                        if (debugLog) log.info "parse() Ip not pased returned by Govee Device. Using Source of Multicast response"
                        newIpAddress = fromIp
                    }
                    def deviceInfo = [
                        ip: newIpAddress,
                        sku: msgData.sku,
                        "last response": new Date().toString()
                    ]
                    if (debugLog) log.info "parse() Device info: ${deviceInfo}"
                    state.lanApiDevices[msgData.device] = deviceInfo
                    state.ipxdni[newIpAddress] = "Govee_${msgData.device}" 
                    def deviceNetworkId = "Govee_${msgData.device}" 
                    def childDevice = getChildDevice(deviceNetworkId) 
                    if (childDevice) { 
                        if (debugLog) log.info "parse() Sending call to existing device '${deviceNetworkId}' to update ip: ${childDevice}"
                        childDevice.updateIPAdd(newIpAddress)
                    } else if (enableLanApiInstall) {
                        runIn(30, installNewDevices)
                    }
                }
            }
        }    
    }
    long endTime = now()
    long duration = endTime - startTime
    def formattedDuration = formatDuration(duration)
    if (debugLog) log.info "parse() Elapse time ${formattedDuration}."
    procTime = procTime + duration
    callCount = callCount + 1
    long avg = procTime/callCount
    sendEvent(name: "avgTime", value: avg)
} 

def formatDuration(long milliseconds) {
    if (milliseconds < 1000) {
        return "${milliseconds} ms"
    }

    long seconds = milliseconds / 1000
    if (seconds < 60) {
        return "${seconds} s ${milliseconds % 1000} ms"
    }

    long minutes = seconds / 60
    if (minutes < 60) {
        return "${minutes} min ${seconds % 60} s ${milliseconds % 1000} ms"
    }

    long hours = minutes / 60
    return "${hours} h ${minutes % 60} min ${seconds % 60} s ${milliseconds % 1000} ms"
}
    

/////////////////////////////////////////////////////////////////////
// Helper Functions
/////////////////////////////////////////////////////////////////////

def normalize(name) {
    return name.replaceAll("[^a-zA-Z0-9]+","-").toLowerCase()
}

def getHubId() {
    def hub = location.hub
    def hubNameNormalized = normalize(hub.name)
    hubNameNormalized = hubNameNormalized.toLowerCase()
    return hubNameNormalized
}

def heartbeat() {
	if (interfaces.mqtt.isConnected()) {
		publishMqtt("heartbeat", now().toString())
	}				
}

def mqttClientStatus(status) {
	if (debugLog) log.debug "In mqttClientStatus: ${status}"
    
    if (status.substring(0,6) != "Status") {
        if (debugLog) log.debug "In mqttClientStatus: Error."
    } else {
        if (debugLog) log.debug "In mqttClientStatus: Success."    
    }
}

///////////////////////////////////////////
// MQTT child device add                 // 
///////////////////////////////////////////

def addMQTTDeviceHelper(String driver, String deviceID, String deviceName, String deviceModel, List commands, List capType) {
	//Driver Settings
	Map deviceType = [namespace:"Mavrrick", typeName: driver]
	Map deviceTypeBak = [:]
	String devModel = deviceModel
	String dni = "Govee_${deviceID}"
	Map properties = [name: driver, label: deviceName, deviceID: deviceID, deviceModel: deviceModel, apiKey: parent.APIKey, commands: commands, capTypes: capType]
	if (debugLog) "Creating Child Device"

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	}
}


def addLightDeviceHelper(String driver, String deviceID, String deviceName, String deviceModel, List commands, ctMin, ctMax, List capType) {
	//Driver Settings
	Map deviceType = [namespace:"Mavrrick", typeName: driver]
	Map deviceTypeBak = [:]
	String devModel = deviceModel
	String dni = "Govee_${deviceID}"
    String ip = "N/A"
    if (!state.lanApiDevices) {
        if (debugLog) "addLightDeviceHelper(): No LAN API Device found/present on network. Leaving default value of N/A"
    } else if (state.lanApiDevices.containsKey(deviceID)) {
        ip = state.lanApiDevices."${deviceID}".ip
    }
	Map properties = [name: driver, label: deviceName, deviceID: deviceID, IP: ip, deviceModel: deviceModel, apiKey: parent.APIKey, commands: commands, ctMin: ctMin, ctMax: ctMax, capTypes: capType]
	if (debugLog) "Creating Child Device"

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	}
}

def addLightDeviceHelper(String driver, String deviceID, String deviceName, String deviceModel, List commands, List capType) {
	//Driver Settings
	Map deviceType = [namespace:"Mavrrick", typeName: driver]
	Map deviceTypeBak = [:]
	String devModel = deviceModel
	String dni = "Govee_${deviceID}"
    String ip = "N/A"
    if (!state.lanApiDevices) {
        if (debugLog) "addLightDeviceHelper(): No LAN API Device found/present on network. Leaving default value of N/A"
    } else if (state.lanApiDevices.containsKey(deviceID)) {
        ip = state.lanApiDevices."${deviceID}".ip
    }
	Map properties = [name: driver, label: deviceName, deviceID: deviceID, IP: ip, deviceModel: deviceModel, apiKey: parent.APIKey, commands: commands, ctMin: 2000, ctMax: 9000, capTypes: capType]
	if (debugLog) "Creating Child Device"

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	}
}

def addManLightDeviceHelper(String driver, String deviceID, String ip, String deviceName, String deviceModel) {
	//Driver Settings
	Map deviceType = [namespace:"Mavrrick", typeName: driver]
	Map deviceTypeBak = [:]
	String devModel = deviceModel
	String dni = "Govee_${deviceID}"
	Map properties = [name: driver, label: deviceName, deviceID: deviceID, IP: ip, deviceModel: deviceModel, ctMin: 2000, ctMax: 9000]
	if (debugLog) "Creating Child Device"

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	}
}

///////////////////////////////////////////
// MQTT Helper to route events to device // 
///////////////////////////////////////////

private def mqttEventCreate(deviceID, instance, state){
    if (debugLog) "mqttEventCreate(): ${deviceID} ${instance} ${state}"
    device = getChildDevice('Govee_'+deviceID)
    if (device == null) {
        if (debugLog) "The MQTT event is for a device that is not setup"
    } else {
        logger.info "Posting MQTT vent to device", 'info'
        device.mqttPost(instance, state)
    }
}

///////////////////////////////////////////////////////////////////////////
// Method to return the Govee API Data for specific device from Prent App //
///////////////////////////////////////////////////////////////////////////

def retrieveGoveeAPI(deviceid) {
    if (debugLog) "retrieveGoveeAPI(): ${deviceid}"
    def goveeAppAPI = parent.state.goveeAppAPI.find{it.device==deviceid}
    return goveeAppAPI
}

///////////////////////////////////////////////////////////////////////////
def retrieveApiDevices () {
    if (!state.lanApiDevices) {
        state.lanApiDevices = [:]    
    }
    lanApiDevices = state.lanApiDevices
    return  lanApiDevices
}


//////////////////////////////////////////////////////////////////////////////////
// Method to return the Govee DIY Scene Data for specific device from Prent App //
/////////////////////////////////////////////////////////////////////////////////

def allSceneReload() { 
    long startTime = now()
    def childDevices = getChildDevices() 
    if (debugLog) {
        log.debug("allSceneReload(): Child Devices: ${childDevices}")
    }

    childDevices.each { device -> 
        def commands = device?.data?.commands
        if (debugLog) {
            log.debug("allSceneReload(): Device ${device.displayName} commands: ${commands}") // Log device name
        }
        if (!commands) {
            if (debugLog) {
                log.debug("allSceneReload(): Ignoring device '${device.displayName}' (likely manually added/no commands data).")
            }
            return 
        }
        if (commands.contains("lightScene")) {
            if (debugLog) {
                log.debug("allSceneReload(): Light Device '${device.displayName}' has 'lightScene' command. Calling sceneLoad().")
            }
            device.sceneLoad()
        } else if (commands.contains("nightlightScene")) {
            if (debugLog) {
                log.debug("allSceneReload(): Light Device '${device.displayName}' has 'nightlightScene' command. Calling retrieveStateData().")
            }
            device.retrieveStateData()
        } else {
            if (debugLog) {
                log.debug("allSceneReload(): Device '${device.displayName}' does not have relevant Scene effects.")
            }
        }
    }
    long endTime = now()
    long duration = endTime - startTime
    def formattedDuration = formatDuration(duration)
    if (debugLog) log.info "allSceneReload() Elapse time ${formattedDuration}."
}


def apiKeyUpdate() {
    if (device.getDataValue("apiKey") != parent?.APIKey) {
        if (debugLog) {log.debug "apiKeyUpdate(): Detected new API Key. Applying"}
        device.updateDataValue("apiKey", parent?.APIKey)
        child = getChildDevices()
        child.each {
            if (debugLog) {log.debug ("apiKeyUpdate(): ${it.label} is being updated with new APIKey")}
            it.apiKeyUpdate()
        }
    }
}

////////////////////////////////////////////////
// LAN API Multisocket Helpers                //
////////////////////////////////////////////////

void multicastListenerSocket(int port) {
    log.info("received: initializeing Multicast Listening port on ${port}")
    def socket = interfaces.getMulticastSocket("239.255.255.250", port)
    if (!socket.connected) socket.connect()    
}

void multicastCloseSocket(int port) {
    log.info("received: Closing Multicast Listening porton ${port}")
    def socket = interfaces.getMulticastSocket("239.255.255.250", port)
    if (socket.connected) socket.close()   
}

void socketStatus(message) {
	log.warn("socket status is: ${message}")
}

void LookupLanAPIDevices() {
    if (debugLog) {log.info("LookupLanAPIDevices: Placing device scan call to Multicast Listening port")}
    def socket = interfaces.getMulticastSocket("239.255.255.250", 4001)
    if (!socket.connected) socket.connect()
    socket.sendMessage(HexUtils.byteArrayToHexString('{"msg":{"cmd":"scan","data":{"account_topic":"reserve"}}}'.getBytes()))
}


def RetrievechildDeviceInfo() {
    if (debugLog) {log.info("RetrievechildDeviceInfo: Retrieving Child device ID Information")}
    state.ipxdni = [:]
    state.lanApiDevices?.each { deviceId, deviceInfo ->
        if (debugLog) {
            log.debug("RetrievechildDeviceInfo(): deviceId ${deviceId} ip ${deviceInfo.ip}")
        }
        state.ipxdni[deviceInfo.ip] = "Govee_${deviceId}"
    }

    if (debugLog && state.lanApiDevices) {
        log.debug("RetrievechildDeviceInfo(): ${state.ipxdni} crossReference")
    }
    state.childCount = getChildDevices().size()
} 

void installNewDevices() {
    List childDNI = getChildDevices().deviceNetworkId
    dni = []
    childDNI.each {
        dni.add(it.minus("Govee_"))
    }
    
    foundDevices = state.lanApiDevices.keySet()
    installList = foundDevices - dni
    if (debugLog) {log.info("installNewDevicess: existing devices: ${dni} Found Devices:${foundDevices} Devices to be installed ${installList}")}
    installList.each {
        goveeDevName = state.lanApiDevices."${it}".sku
        try {
        goveeDevName = retrieveGoveeAPI(it).deviceName
        } catch(Exception e) {
            log.error "In installNewDevices: Govee Data not avaliable Using Default value"
		}
        log.info("installNewDevicess: Device Name:${goveeDevName} Device ID:${it} IP:${state.lanApiDevices."${it}".ip} sku:${state.lanApiDevices."${it}".sku}")
        String driver = "Govee Manual LAN API Device"
        addManLightDeviceHelper( driver, it, state.lanApiDevices."${it}".ip, goveeDevName, state.lanApiDevices."${it}".sku)
    }
}
