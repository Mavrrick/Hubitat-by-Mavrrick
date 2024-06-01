// Hubitat driver for Govee Device Manager Driver
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

import groovy.json.JsonSlurper
import groovy.transform.Field

#include Mavrrick.Govee_Lan_Scenes

metadata {
	definition(name: "Govee v2 Device Manager", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Initialize"
		capability "Refresh" 
        attribute "connectionState", "string"
        attribute "msgCount", "integer" 
        command "allSceneReload"
    }

	preferences {		
		section("Device Info") {            
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
            input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true) 
		}
		
	}
}

//////////////////////////////////////
// Standard Methods for all drivers //
//////////////////////////////////////

def updated() {
    if (logEnable) runIn(1800, logsOff)
    sendEvent(name: "msgCount", value: 0)
    disconnect()
	pauseExecution(1000)
    mqttConnectionAttempt()
}


def installed(){
    initialize()
}

def initialize() {
//    goveeAPI = parent.state.goveeAppAPI
    disconnect()
    pauseExecution(1000)
    mqttConnectionAttempt()
    sendEvent(name: "msgCount", value: 0)
    state.childCount = getChildDevices().size()
    allSceneReload()
//    log.warn "Govee API Data is ${parent.state.goveeAppAPI}"
//    log.warn "Govee API Data is ${goveeAppAPI}"
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
    if (debugLog) runIn(1800, logsOff)
//    lightEffectSetup()
}

def configure() {
    if (debugLog) {log.warn "configure(): Driver Updated"}       
    if (debugLog) runIn(1800, logsOff) 
}

// put methods, etc. here

def mqttConnectionAttempt() {
//    mqttApiKey = '['+device.getDataValue("apiKey")+']'
//    if (debugLog) log.debug "In mqttConnectionAttempt: ${mqttApiKey}"
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
//    log.debug "Subscribe to: ${topic} or ${mqttTopic}"
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
//  subscribe("${device.getDataValue("apiKey")}")
    subscribe()
}

def disconnect() {
	unschedule(heartbeat)

	if (interfaces.mqtt.isConnected()) {
//		publishLwt("offline")
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

def parse(String event) {
    def jsonSlurper = new JsonSlurper()     
    if (descLog) log.info "parse() MQTT message recieved. Parsing and sending to device"
//    def message = interfaces.mqtt.parseMessage(event)    
//    def payloadJson = jsonSlurper.parseText(message.payload)
    def payloadJson = jsonSlurper.parseText(interfaces.mqtt.parseMessage(event).payload)
    int mqttMsgCount = device.currentValue("msgCount").toInteger() + 1
    sendEvent(name: "msgCount", value: mqttMsgCount)
    
        if (debugLog) log.debug "In parse, received message: ${payloadJson} for deviceid is ${payloadJson.device} capability is ${payloadJson.capabilities}"
//        if (debugLog) log.debug "In parse, payloadJson is ${payloadJson}"
//        if (debugLog) log.debug "In parse, deviceid is ${payloadJson.device} capability is ${payloadJson.capabilities} "
//        if (debugLog) log.debug "In parse, instance is ${payloadJson.capabilities.get(0).instance}" 
//        if (debugLog) log.debug "In parse, state is ${payloadJson.capabilities.get(0).state.get(0).name}"

        mqttEventCreate(payloadJson.device, payloadJson.capabilities.get(0).instance, payloadJson.capabilities.get(0).state.get(0).name)
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
	Map properties = [name: driver, label: deviceName, deviceID: deviceID, deviceModel: deviceModel, apiKey: parent.APIKey, commands: commands, ctMin: ctMin, ctMax: ctMax, capTypes: capType]
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

//////////////////////////////////////////////////////////////////////////////////
// Method to return the Govee DIY Scene Data for specific device from Prent App //
/////////////////////////////////////////////////////////////////////////////////

def retrieveGoveeDIY(deviceModel) {
    if (parent.state.diyEffects.containsKey(deviceModel) == false) {
        if (debugLog) {log.debug ("retrieveScenes(): No DIY Scenes to retrieve for device")} 
    } else {
        if (debugLog) "retrieveGoveeDIY(): ${deviceModel}"
        def diyScenes = parent.state.diyEffects.get(deviceModel)
        if (debugLog) "retrieveGoveeDIY(): ${diyScenes}"
        return diyScenes
    }    
}

def allSceneReload(){
    child = getChildDevices()
    if (debugLog) {log.debug ("allSceneReload(): ${child}")}
    child.each {
        if (debugLog) {log.debug ("allSceneReload(): ${it.data.commands} list command")}
        if (it.data.commands.contains("lightScene")) {
            if (debugLog) {log.debug ("allSceneReload(): Light Device ${it} has command lightScene")}
            it.sceneLoad()
        }  else if (it.data.commands.contains("nightlightScene")) { 
            if (debugLog) {log.debug ("allSceneReload(): Light Device ${it} has command nightlightScene")}
            it.retrieveStateData()
        } else {
            if (debugLog) {log.debug ("allSceneReload(): Device ${it} does not have Scene effects")} 
        }
    }
}


