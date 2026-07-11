library (
 author: "Mavrrick",
 category: "Govee",
 description: "GoveeCloudAPI",
 name: "Govee_Cloud_MQTT",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

// put methods, etc. here

def mqttConnectionAttempt() {
	if (debugLog) log.debug "In mqttConnectionAttempt"
 
	if (!interfaces.mqtt.isConnected()) {
		try {   
			interfaces.mqtt.connect("ssl://mqtt.openapi.govee.com:8883",
                                    "hubitat_${getHubId()}_${device.getId()}", 
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
// MQTT Post
/////////////////////////////////////////////////////////////////////

def mqttPost(String instance, String state){
    if (debugLog) { log.debug "mqttPost(): posting MQTT Update"}
    if (instance == 'lackWaterEvent' || instance == 'iceFull' || instance == 'cleaningCompletedEvent' || instance == 'runInterruptEvent' ) {
        if (descLog) { log.debug "mqttPost(): ${instance} found"}
            time = new Date().format("MM/dd/yyyy HH:mm:ss")
            sendEvent(name: instance, value: time, displayed: true)
    } 
    else if (instance == 'bodyAppearedEvent') {
        if (state == "Presence") {
            if (descLog) { log.info "mqttPost(): bodyAppearedEvent Present found"}
            sendEvent(name: "presence", value: "present", displayed: true)
            sendEvent(name: "motion", value: "active", displayed: true)
        } else if (state == "Absence") {
            if (descLog) { log.info "mqttPost(): bodyAppearedEvent Not Present found"}
            sendEvent(name: "presence", value: "not present", displayed: true)
            sendEvent(name: "motion", value: "inactive", displayed: true)                     
        } else if (state == "LEAKED") {
            if (descLog) { log.info "mqttPost(): bodyAppearedEvent Not Present found"}
            sendEvent(name: "water", value: "wet", displayed: true)                    
        } else if (state == "UN_LEAKED") {
            if (descLog) { log.info "mqttPost(): bodyAppearedEvent Not Present found"}
            sendEvent(name: "water", value: "dry", displayed: true)                    
        }
    }
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

