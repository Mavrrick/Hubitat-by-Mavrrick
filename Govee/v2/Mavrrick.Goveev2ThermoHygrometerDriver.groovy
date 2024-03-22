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
	definition(name: "Govee v2 Thermo/Hygrometer Driver", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Initialize"
		capability "Refresh" 
        capability "TemperatureMeasurement"         
        capability "RelativeHumidityMeasurement"
        attribute "online", "string"
        attribute "cloudAPI", "string"
    }

	preferences {		
		section("Device Info") {  
            input("tempUnit", "enum", title: "Temp Unit Selection", defaultValue: 'Fahrenheit', options: [    "Fahrenheit",     "Celsius"], required: true)
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)            
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
		}
		
	}
}



def updated() {
if (logEnable) runIn(1800, logsOff)
}


def installed(){
    if (pollRate > 0) runIn(pollRate,poll)
    getDeviceTempHumid()
}

def initialize() {
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
        unschedule(poll)
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceTempHumid()
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
    getDeviceTempHumid()
    if (debugLog) runIn(1800, logsOff)
}

def configure() {
    if (debugLog) {log.warn "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)         
    if (debugLog) runIn(1800, logsOff) 
}
