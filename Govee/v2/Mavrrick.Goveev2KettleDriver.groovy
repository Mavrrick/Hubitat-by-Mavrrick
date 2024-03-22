// Hubitat driver for Govee Appliances using Cloud API
// Version 1.0.19
//
// 2022-11-03 Initial Driver release for Govee Heating Appliance devices
// 2022-11-20 Added a pending change condition and validation that the call was successful
// ---------- A retry of the last call will be attempted if rate limit is the cause for it failing
// ---------- Included code to update parent app for rate limit consumption.
// 2022-11-21 Moved status of cloud api call to the it's own attribute so it can be monitored easily
// 2022-12-19 Added Actuator capbility to more easily integrate with RM
// 2023-4-4   API key update now possible
// 2023-4-7   Update Initialize and getDeviceStatus routine to reset CloudAPI Attribute

#include Mavrrick.Govee_Cloud_API
//#include Mavrrick.Govee_Cloud_RGB
//#include Mavrrick.Govee_Cloud_Level

metadata {
	definition(name: "Govee v2 Kettle Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Actuator"
        capability "Initialize"
		capability "Refresh" 
        capability "TemperatureMeasurement"
        capability "Configuration"         

        attribute "online", "string"
        attribute "mode", "number"
        attribute "cloudAPI", "string"
        attribute "online", "string"
        attribute "tempSetPoint", "string"

        command "workingMode", [[name: "mode", type: "ENUM", constraints: [ 'DIY',      'Boiling',       'Tea',   'Coffee'], description: "Mode of device"],
            [name: "gearMode", type: "NUMBER",  description: "Mode Value", range: 1..4, required: false]]
        command "tempSetPoint", [[type: "NUMBER", description: "Entered your desired temp. Celsius range is 40-100, Fahrenheit range is 104-212", required: true],
            [name: "unit", type: "ENUM", constraints: [ 'Celsius',      'Fahrenheit'],  description: "Celsius or Fahrenheit", defaultValue: "Celsius", required: true]]
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

def workingMode(mode, gear){
    log.debug "workingMode(): Processing Working Mode command. ${mode} ${gear}"
    sendEvent(name: "cloudAPI", value: "Pending")
    if (gear == null) { gear = 0 }
    switch(mode){
        case "DIY":
            modenum = 1;
            gearNum = gear;
        break;
        case "Boiling":
            modenum = 2;
            gearNum = 0;
        break;
        case "Tea":
            modenum = 3;
            gearNum = gear;
        break;
        case "Coffee":
            modenum = 4;
            gearNum = gear;
        break;
    default:
    log.debug "not valid value for mode";
    break;
    }
    values = '{"workMode":'+modenum+',"modeValue":'+gearNum+'}'
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}    

def tempSetPoint(setpoint, unit) {
    values = '{"temperature": '+setpoint+',"unit": "'+unit+'"}'
    sendCommand("sliderTemperature", values, "devices.capabilities.temperature_setting")
}

def updated() {
if (logEnable) runIn(1800, logsOff)
retrieveStateData()
}


def installed(){
    if (pollRate > 0) runIn(pollRate,poll)
    getDeviceState()
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

def configure() {
    if (debugLog) {log.warn "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)     
    retrieveStateData()    
    if (debugLog) runIn(1800, logsOff) 
}
