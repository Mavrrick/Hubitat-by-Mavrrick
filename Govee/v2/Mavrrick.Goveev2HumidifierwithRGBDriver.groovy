// Hubitat driver for Govee Appliances using Cloud API
// Version 1.0.19
//
// 2022-09-12 -	Initial Driver release for Govee Appliance devices
// 2022-10-19 - Added Attributes to driver
// 2022-11-3 - Send Rate Limits to parent.
// 2022-11-5 - Added update to Switch value to On when Gear or Mode are used
// 2022-11-20 - Added a pending change condition and validation that the call was successful
// ----------- A retry of the last call will be attempted if rate limit is the cause for it failing
// 2022-11-21 Moved status of cloud api call to the it's own attribute so it can be monitored easily
// 2022-12-19 Added Actuator capbility to more easily integrate with RM
// 2023-4-4   API Key update is now possible
// 2023-4-7   Update Initialize and getDeviceStatus routine to reset CloudAPI Attribute

#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_RGB
#include Mavrrick.Govee_Cloud_Level
#include Mavrrick.Govee_Cloud_MQTT

metadata {
	definition(name: "Govee v2 Humidifier with RGB Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "ColorControl"        
		capability "Actuator"
        capability "Initialize"
        capability "Refresh"
        capability "RelativeHumidityMeasurement"
        capability "SwitchLevel"
        capability "LightEffects"
        capability "Configuration"
        attribute "lackWaterEvent", "string"         
        
		attribute "gear", "number"
        attribute "mode", "number"
        attribute "online", "string"
        attribute "cloudAPI", "string"
        attribute "nightLight", "string"
        attribute "lackWaterEvent", "string"        
        
        command "nightLighton_off", [[name: "Night Light", type: "ENUM", constraints: [ 'On',      'Off'] ] ]
        command "workingMode", [[name: "mode", type: "ENUM", constraints: [ 'Manual',      'Custom',       'Auto'], description: "Mode of device"],
                          [name: "gearMode", type: "NUMBER", description: "When Mode is Manual sets hudifier speed. When set to Auto sets desired Humidity"]]        

        
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)  
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
		}
		
	}
}



def on() {
         if (device.currentValue("cloudAPI") == "Retry") {
             log.error "on(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 1 ,"devices.capabilities.on_off")
            }
}

def off() {
        if (device.currentValue("cloudAPI") == "Retry") {
             log.error "off(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 0 ,"devices.capabilities.on_off")
            }
} 

def gear(gear){
    sendEvent(name: "cloudAPI", value: "Pending")
    sendCommand("gear", gear)
}

def workingMode(mode, gear){
    log.debug "workingMode(): Processing Working Mode command. ${mode} ${gear}"
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(mode){
        case "Manual":
            modenum = 1;
        
        break;
        case "Custom":
            modenum = 2;
            gear = 0;
        break;
        case "Auto":
            modenum = 3;
        break;
    default:
    log.debug "not valid value for mode";
    break;
    }
    values = '{"workMode":'+modenum+',"modeValue":'+gear+'}'
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}  


def updated() {
if (logEnable) runIn(1800, logsOff)
    retrieveStateData()
    state.sceneMax = state.nightlightScene.size()
    state.sceneValue = 0    
}


def installed(){
    getDeviceState()    
    retrieveStateData()
    state.sceneMax = state.nightlightScene.size()
    state.sceneValue = 0
}

def initialize() {
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
    unschedule(poll)
    if (pollRate > 0) runIn(pollRate,poll)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def poll() {
    if (debugLog) {log.warn "poll(): Poll Initated"}
	refresh()
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
    unschedule(poll)
    if (pollRate > 0) runIn(pollRate,poll)
    getDeviceState()
    if (debugLog) runIn(1800, logsOff)
}

def configure() {
    if (debugLog) {log.warn "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)     
    retrieveStateData()
    state.sceneMax = state.nightlightScene.size()
    state.sceneValue = 0
    if (debugLog) runIn(1800, logsOff) 
}

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
