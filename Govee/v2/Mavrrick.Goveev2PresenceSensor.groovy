// Hubitat driver for Govee mmWave Presence Sensir using Cloud API
// Version 1.0.19
//
// 2024-03-21 Initial Driver release for Govee Heating Govee Presence devices

#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_MQTT

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 Presence Sensor", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Initialize"
        capability "Refresh"
        capability "MotionSensor"         
        capability "PresenceSensor"
        attribute "connectionState", "string"
    }

	preferences {		
		section("Device Info") {      
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
            input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true) 
		}
		
	}
}

//////////////////////////////////////
// Standard Methods for all drivers //
//////////////////////////////////////

def updated() {
if (logEnable) runIn(1800, logsOff)
    disconnect()
	pauseExecution(1000)
    mqttConnectionAttempt()
}


def installed(){
    initialize()
}

def initialize() {
    disconnect()
    pauseExecution(1000)
    mqttConnectionAttempt()
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
    if (debugLog) runIn(1800, logsOff)
}

def configure() {
    if (debugLog) {log.warn "configure(): Driver Updated"}       
    if (debugLog) runIn(1800, logsOff) 
}

