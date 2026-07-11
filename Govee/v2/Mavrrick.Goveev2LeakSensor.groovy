// Hubitat driver for Govee Water Leak Sensor using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_MQTT

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 Leak Sensor", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Initialize"
        capability "Refresh"
        capability "WaterSensor"         
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
}


def installed(){
    initialize()
}

def initialize() {
    checkDevData()

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

