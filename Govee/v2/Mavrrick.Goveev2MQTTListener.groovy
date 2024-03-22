// Hubitat driver for Govee MQTT Listener using Cloud API
// Version 1.0.19
//
// 2024-03-21 Initial Driver release for Govee Heating Govee Presence devices


import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 MQTT Listener", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Initialize"
		capability "Refresh" 
        attribute "connectionState", "string"
    }

	preferences {		
		section("Device Info") {            
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
		}
		
	}
}



def updated() {
if (logEnable) runIn(1800, logsOff)
    disconnect()
	pauseExecution(1000)
    mqttConnectionAttempt()
}


def installed(){
    initialize()
}

def initialize() {
    disconnect()
    pauseExecution(1000)
    mqttConnectionAttempt()
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
    if (debugLog) runIn(1800, logsOff)
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
    if (debugLog) log.debug "In parse, received a message"
    def message = interfaces.mqtt.parseMessage(event)    
    def payloadJson = jsonSlurper.parseText(message.payload)
    
        if (debugLog) log.debug "In parse, received message: ${message}"
        if (debugLog) log.debug "In parse, payloadJson is ${payloadJson}"
        if (debugLog) log.debug "In parse, deviceid is ${payloadJson.device} capability is ${payloadJson.capabilities} "
        if (debugLog) log.debug "In parse, instance is ${payloadJson.capabilities.get(0).instance}" 
        if (debugLog) log.debug "In parse, state is ${payloadJson.capabilities.get(0).state.get(0).name}"

    parent.mqttEventCreate(payloadJson.device, payloadJson.capabilities.get(0).instance, payloadJson.capabilities.get(0).state.get(0).name)
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
