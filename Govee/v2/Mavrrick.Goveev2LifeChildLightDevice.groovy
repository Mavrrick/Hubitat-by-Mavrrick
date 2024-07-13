// Hubitat driver for Govee Appliances Child Light Device
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objects
#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_RGB
#include Mavrrick.Govee_Cloud_Level
// #include Mavrrick.Govee_Cloud_Life

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 Life Child Light Device", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "ColorControl"
		capability "Actuator"
        capability "Initialize"
		capability "Refresh" 
        capability "SwitchLevel"
        capability "LightEffects"
        capability "Configuration" 
        
        attribute "online", "string"        
        attribute "cloudAPI", "string"
        attribute "filterStatus", "string"  
        
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

// reset of device settings when preferences updated.
def updated() {
    unschedule()
    if (debugLog) runIn(1800, logsOff)

}

// linital setup when device is installed.
def installed(){
}

// initialize devices upon install and reboot.
def initialize() {
     if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
    unschedule()
    if (logEnable) runIn(1800, logsOff)
}

// update data for the device
def refresh() {
}

// retrieve setup values and initialize polling and logging
def configure() {
    if (debugLog) {log.info "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)     
    retrieveStateData()    
    if (debugLog) runIn(1800, logsOff) 
}

////////////////////
// Helper methods //
////////////////////

// turn off logging for the device
def logsOff() {
    log.info "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

//////////////////////
// Driver Commands // 
/////////////////////

def on() {
        cloudOn()
}

def off() {
        cloudOff()
}

def setColorTemperature(value,level = null,transitionTime = null) {
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) { log.debug "setColorTemperature(): ${value}"}
    if (value < device.getDataValue("ctMin").toInteger()) { value = device.getDataValue("ctMin")}
    if (value > device.getDataValue("ctMax").toInteger()) { value = device.getDataValue("ctMax")}
    if (debugLog) { log.debug "setColorTemperate(): ColorTemp = " + value }
    cloudCT(value, level, transitionTime)
}

def setLevel(float v,duration = 0) {
    cloudSetLevel( v, 0)
}

