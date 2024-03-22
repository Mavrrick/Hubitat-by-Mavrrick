// Hubitat driver for Govee mmWave Presence Sensir using Cloud API
// Version 1.0.19
//
// 2024-03-21 Initial Driver release for Govee Heating Govee Presence devices

#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_MQTT

metadata {
	definition(name: "Govee v2 Presence Sensor", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Initialize"
		capability "Refresh" 
        capability "MotionSensor"         
        capability "PresenceSensor"
        attribute "online", "string"
        attribute "cloudAPI", "string"
    }

	preferences {		
		section("Device Info") {  
//            input("tempUnit", "enum", title: "Temp Unit Selection", defaultValue: 'Fahrenheit', options: [    "Fahrenheit",     "Celsius"], required: true)
//            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)            
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
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

/* def poll() {
    if (debugLog) {log.warn "poll(): Poll Initated"}
	refresh()
} */

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
//    unschedule(poll)
//    if (pollRate > 0) runIn(pollRate,poll)
//    getDeviceTempHumid()
    if (debugLog) runIn(1800, logsOff)
}

def configure() {
    if (debugLog) {log.warn "configure(): Driver Updated"}
//    unschedule()
//    if (pollRate > 0) runIn(pollRate,poll)         
    if (debugLog) runIn(1800, logsOff) 
}
