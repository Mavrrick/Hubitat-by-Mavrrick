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

/* def workingModeUnified(mode, gear, int number){
    def modenum = state.workMode.options[0].find{it.name==mode}?.value
    switch(mode) {
        case "DIY":
            gearnum = number; //>0?:1;
        break;
        case "Manual":
            gearnum = number; //>0?:1;
        break;        
        case "Boiling":
            gearnum = 0;
        break;
        case "Tea":
            gearnum = number;  // >0?:1;
        break;
        case "Coffee":
            gearnum = number; //>0?:1;
        break;
        case "Custom":
            modenum = 2;
            gearnum = number; //>0?:1;
        break;
        case "Auto":
            modenum = 3;
            gearnum = 0;
        break;
        case "Sleep":
            modenum = 5;
            gearnum = 0;
        break;
        case "gearMode":
            modenum = 1;
            switch(gear) {
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
                    gearnum = gear;
                break;
        }
        break;
        case "Fan":
        modenum = 9;        
        gearnum = 0;
        break
        default:
        gearnum = gear;
        break
    }
    log.debug "workingMode(${mode},${gear}): Processing Working Mode command. modenum:${modenum} gearnum:${gearnum}"
    sendEvent(name: "cloudAPI", value: "Pending")    
    values = '{"workMode":'+modenum+',"modeValue":'+gearnum+'}'
    sendCommand("workMode", values, "devices.capabilities.work_mode")
} */ 

def changeInterval(v=300) {
    log.debug "changeInterval(): Request to change polling interval to ${v}"
    unschedule(poll)
    device.updateSetting('pollRate', [type: "number", value: v])
    sendEvent(name: "pollInterval", value: v)
    if (v > 0)  runIn(pollRate,poll) 
}