library (
 author: "Mavrrick",
 category: "Govee",
 description: "GoveeCloudAPI",
 name: "Govee_Cloud_MQTT",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)


def mqttPost(instance, state){
    log.debug "mqttPost(): posting MQTT Update"
        if (instance == 'lackWaterEvent') { 
            log.debug "mqttPost(): lackWaterEvent Found"
        time = new Date().format("MM/dd/yyyy HH:mm:ss")
        sendEvent(name: instance, value: time, displayed: true)
    } 
    else if (instance == 'bodyAppearedEvent') {
        if (state == "Presence") {
            sendEvent(name: "presence", value: "present", displayed: true)
            sendEvent(name: "motion", value: "active", displayed: true)
        } else if (state == "Absence") {
            sendEvent(name: "presence", value: "not present", displayed: true)
            sendEvent(name: "motion", value: "inactive", displayed: true)
            
        }
    }
}