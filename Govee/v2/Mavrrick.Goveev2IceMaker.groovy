// Hubitat driver for Govee Ice Maker using Cloud API
// Version 1.0.1
//
// 2022-09-12 -	Initial Driver release for Govee Appliance devices


#include Mavrrick.Govee_Cloud_API
//#include Mavrrick.Govee_Cloud_RGB
#include Mavrrick.Govee_Cloud_MQTT

metadata {
	definition(name: "Govee v2 Ice Maker", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Actuator"
        capability "Initialize"
        capability "Refresh"
        capability "Configuration"        
        
		attribute "gear", "number"
        attribute "mode", "number"
        attribute "cloudAPI", "string"
        attribute "online", "string" 
        attribute "lackWaterEvent", "string"        
        
//		command "gear" , [[name: "Fan or Mist Speed", type: 'NUMBER', description: "Gear will adjust values for Fan speed or Misting depending on what device you have. Valid values are  "]]
        command "workingMode", [[name: "workMode", type: "ENUM", constraints: [ 'Large Ice',      'Medium Ice',       'Small Ice'], description: "Mode of device"]]
//                          [name: "gearMode", type: "NUMBER", description: "When Mode is 1 sets hudifier speed. When set to Auto sets desired Humidity"]]
//        command "desiredHumidity", [[name: desiredHumidityValue, type: 'NUMBER', description: "Set the desired Humidity the device will try to maintain"]]

        
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

def workingMode(mode){
    log.debug "workingMode(): Processing Working Mode command. ${mode} ${gear}"
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(mode){
        case "Large Ice":
            modenum = 1;           
        break;
        case "Medium Ice":
            modenum = 2;
        break;
        case "Small Ice":
            modenum = 3;
        break;
    default:
    log.debug "not valid value for mode";
    break;
    }
    values = '{"workMode":'+modenum+',"modeValue":0}'
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}     



def updated() {
if (logEnable) runIn(1800, logsOff)
    retrieveStateData()
}


def installed(){
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
