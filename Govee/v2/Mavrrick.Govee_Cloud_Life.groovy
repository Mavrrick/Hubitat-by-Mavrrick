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

def workingMode(mode, gear = 0){
    def modenum = state.workMode.options[0].find{it.name==mode}?.value
    if (mode == "gearMode") {
//        def gearnum = state.workMode.options[1].options[0].find{it.name==gear}?.value
      switch(gear){
        case "Low":
            gearnum = 1;
        break;
        case "Medium":
            gearnum = 2;
        break;
        case "High":
            gearnum = 3;
        break;
    default:
    gearnum = 0;
    break;
      }
    } else if (mode == "Fan") {
        gearnum = 0
    } else if (mode == "Auto") {
        gearnum = 0
    } else {
        gearnum = gear
    }
    log.debug "workingMode(): Processing Working Mode command. ${modenum} ${gearnum}"
    sendEvent(name: "cloudAPI", value: "Pending")    
    values = '{"workMode":'+modenum+',"modeValue":'+gearnum+'}'
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}

def changeInterval(v) {
    log.debug "changeInterval(): Request to change polling interval to ${v}"
    unschedule(poll)
    device.updateSetting('pollRate', [type: "number", value: v])
    sendEvent(name: "pollInterval", value: v)
    if (v > 0)  runIn(pollRate,poll) 
}