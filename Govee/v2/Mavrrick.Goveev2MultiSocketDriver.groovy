// Hubitat driver for Govee Plug, Switch driver using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

import hubitat.helper.InterfaceUtils
import hubitat.helper.HexUtils
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_LAN_API

def commandPort() { "4003" }

metadata {
	definition(name: "Govee v2 Multi-Socket Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "Configuration"
		capability "Refresh"
        capability "Initialize"
        
        attribute "cloudAPI", "string"
        attribute "online", "string"
        attribute "outlet1", "string"
        attribute "outlet2", "string"
        
        command "socket1On"
        command "socket1Off"
        command "socket2On"
        command "socket2Off"
        
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)
//            input(name: "lanControl", type: "bool", title: "Enable Local LAN control", description: "This is a advanced feature that only worked with some devices. Do not enable unless you are sure your device supports it", defaultValue: false)
//            if (socketCountLookup() > 0) {
            input("descLog", "bool", title: "Enable Multi-Socket Support", description: "By flipping this switch you tell the driver to enable Child devices for each outlet if possible", defaultValue: true) //}
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
            input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true) 
		}
		
	}
}

///////////////////////////////////////////////
// Methods to setup manage device properties //
///////////////////////////////////////////////

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

def updated() {
    configure()
}

def configure() {
    if (debugLog) {log.warn "configure(): Configuration Changed"}
        unschedule()
        if (pollRate > 0) runIn(pollRate,poll)
//        getDeviceState()
    if (debugLog) runIn(1800, logsOff) 
    if (debugLog) {log.debug "configure(): size of data value ${device.getDataValue("commands").size()}"}
    int occurrences = device.getDataValue("commands").count("socketToggle")
    if (debugLog) {log.debug "configure(): Number of sockets are ${occurrences}"}
}

def initialize(){
    if (debugLog) {log.warn "initialize(): Driver Initializing"}
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
        unschedule()
    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        randomOffset(pollRateInt)
        runIn(offset,poll)
    }
//        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
    if (debugLog) runIn(1800, logsOff)
}


def installed(){
    if (debugLog) {log.warn "installed(): Driver Installed"}
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
        retrieveStateData()
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

/////////////////////////
// Commands for Driver //
/////////////////////////

def on() {
    if (lanControl) {
        lanOn() }
    else {
        cloudOn()
        }
}

def off() {
    if (lanControl) {
        lanOff() }
    else {
        cloudOff()
        }
}

def socket1On(){
    sendCommand("socketToggle1", 1 ,"devices.capabilities.toggle")
    sendEvent(name: "outlet1", value: "on")
}

def socket2On(){
    sendCommand("socketToggle2", 1 ,"devices.capabilities.toggle")
    sendEvent(name: "outlet2", value: "on")
}

def socket1Off(){
    sendCommand("socketToggle1", 0 ,"devices.capabilities.toggle")
    sendEvent(name: "outlet1", value: "off")
}

def socket2Off(){
    sendCommand("socketToggle2", 0 ,"devices.capabilities.toggle")
    sendEvent(name: "outlet2", value: "off")
}

///////
// Helper
///

