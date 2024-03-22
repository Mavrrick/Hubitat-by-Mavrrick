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

metadata {
	definition(name: "Govee v2 Air Purifier Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Actuator"
        capability "Initialize"
		capability "Refresh"
        capability "Configuration"
        
        attribute "online", "string"
//        attribute "gear", "number"
        attribute "mode", "number"
        attribute "cloudAPI", "string"
        attribute "filterStatus", "string"        
        command "workingMode", [[name: "mode", type: "ENUM", constraints: [ 'gearMode',      'Custom',       'Auto',       'Sleep'], description: "Mode of device"],
                          [name: "gearMode",  type: "NUMBER", description: "This will adjust your fan speed."]]        
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4) 
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
		}
		
	}
}

def parse(String description) {

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

/* def gear(gear){
    sendEvent(name: "cloudAPI", value: "Pending")
    sendCommand("gear", gear)
} */

def workingMode(mode, gear){
    log.debug "workingMode(): Processing Working Mode command. ${mode} ${gear}"
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(mode){
        case "gearMode":
            modenum = 1;
        
        break;
        case "Custom":
            modenum = 2;
        break;
        case "Auto":
            modenum = 3;
            gear = 0;
        break;
        case "Sleep":
            modenum = 5;
            gear = 0;
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
}

def configure() {
    if (debugLog) {log.warn "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)     
    retrieveStateData()    
    if (debugLog) runIn(1800, logsOff) 
}

def installed(){
    if (pollRate > 0) runIn(pollRate,poll)
    getDeviceState()
    retrieveStateData()
}

def initialize() {
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
        unschedule(poll)
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
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
