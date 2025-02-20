library (
 author: "Mavrrick",
 category: "Govee",
 description: "GoveeCloudAPI",
 name: "Govee_Cloud_Life",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)


def nightLighton_off(evt) {
    log.debug "nightLighton_off(): Processing Night Light command. ${evt}"
        if (device.currentValue("cloudAPI") == "Retry") {
             log.error "nightLighton_off(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
            if (evt == "On") sendCommand("nightlightToggle", 1 ,"devices.capabilities.toggle")
            if (evt == "Off") sendCommand("nightlightToggle", 0 ,"devices.capabilities.toggle")
            }
}

def  setEffect(effectNo) {
                log.debug ("setEffect(): Setting effect via cloud api to scene number  ${effectNo}")
                sendCommand("nightlightScene", effectNo,"devices.capabilities.mode")
                   
}

def setNextEffect() {
        if (debugLog) {log.debug ("setNextEffect(): current Name ${state.nightlightScene.get(state.sceneValue).name} value ${state.nightlightScene.get(state.sceneValue).value}")}
            if (state.sceneValue == 0) {
            if (debugLog) {log.debug ("setNextEffect(): Current scene value is 0 setting to first scene in list")}
            setEffect(state.nightlightScene.get(state.sceneValue).value)
            state.sceneValue = state.sceneValue + 1
        } else {
            if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
            setEffect(state.nightlightScene.get(state.sceneValue).value)
            state.sceneValue = state.sceneValue + 1
        }  
} 
      
def setPreviousEffect() {
        if (debugLog) {log.debug ("setPreviousEffect(): current Name ${state.nightlightScene.get(state.sceneValue).name} value ${state.nightlightScene.get(state.sceneValue).value}")}
            if (state.sceneValue == 0) {
            if (debugLog) {log.debug ("setPreviousEffect(): Current scene value is 0 setting to first scene in list")}
            setEffect(state.nightlightScene.get(state.sceneValue).value)
            state.sceneValue = state.sceneMax 
        } else {
            if (debugLog) {log.debug ("setPreviousEffect(): Increment to next scene")}
            setEffect(state.nightlightScene.get(state.sceneValue).value)
            state.sceneValue = state.sceneValue - 1
        }          
}

def changeInterval(v=300) {
    log.debug "changeInterval(): Request to change polling interval to ${v}"
    unschedule(poll)
    device.updateSetting('pollRate', [type: "number", value: v])
    sendEvent(name: "pollInterval", value: v)
    if (v > 0)  runIn(pollRate,poll) 
}

def ipLookup() {
    log.info("ipLookup: Device is Govee Life object with no ip to return")
}