library (
 author: "Mavrrick",
 category: "Govee",
 description: "GoveeCloudAPI",
 name: "Govee_Cloud_MQTT",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

// put methods, etc. here


/////////////////////////////////////////////////////////////////////
// Parse
/////////////////////////////////////////////////////////////////////


def mqttPost(String instance, String state){
    if (debugLog) { log.debug "mqttPost(): posting MQTT Update for ${instance} of ${state}"}
    if (instance == 'lackWaterEvent' || instance == 'iceFull' || instance == 'cleaningCompletedEvent' || instance == 'runInterruptEvent' ) {
        if (descLog) { log.debug "mqttPost(): ${instance} found"}
            time = new Date().format("MM/dd/yyyy HH:mm:ss")
            sendEvent(name: instance, value: time, displayed: true)
    } 
    else if (instance == 'bodyAppearedEvent') {
        if (descLog) { log.debug "mqttPost(): ${instance} found"}
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
    } else if (instance == 'bot') {
        if (descLog) { log.debug "mqttPost(): ${instance} found"}
        if (state == "1") {
            if (descLog) { log.info "mqttPost(): Bottom probesState event 1 Wet"}
            sendEvent(name: "probeBottom", value: "wet", displayed: true)                    
        } else if (state == "0") {
            if (descLog) { log.info "mqttPost(): Bottom probesState event 0 Dry"}
            sendEvent(name: "probeBottom", value: "dry", displayed: true)                    
        }
    } else if (instance == 'top') {
        if (descLog) { log.debug "mqttPost(): ${instance} found"}
        if (state == "1") {
            if (descLog) { log.info "mqttPost(): Top probesState event 1 Wet"}
            sendEvent(name: "probeTop", value: "wet", displayed: true)                    
        } else if (state == "0") {
            if (descLog) { log.info "mqttPost(): Top probesState event 0 Dry"}
            sendEvent(name: "probeTop", value: "dry", displayed: true)                    
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

def heartbeat() {
	if (interfaces.mqtt.isConnected()) {
		publishMqtt("heartbeat", now().toString())
	}				
}


