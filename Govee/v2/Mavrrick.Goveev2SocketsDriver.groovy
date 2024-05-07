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
	definition(name: "Govee v2 Sockets Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "Configuration"
		capability "Refresh"
        capability "Initialize"
        
        attribute "cloudAPI", "string"
        attribute "online", "string"
        
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)
            input(name: "lanControl", type: "bool", title: "Enable Local LAN control", description: "This is a advanced feature that only worked with some devices. Do not enable unless you are sure your device supports it", defaultValue: false)
            if (lanControl) {
            input("ip", "text", title: "IP Address", description: "IP address of your Govee light", required: false)}
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
        getDeviceState()
    if (debugLog) runIn(1800, logsOff) 
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


