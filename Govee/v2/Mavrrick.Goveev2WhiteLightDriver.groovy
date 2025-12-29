// Hubitat driver for Govee Light, Plug, Switch driver using Cloud API
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
#include Mavrrick.Govee_Cloud_Level
#include Mavrrick.Govee_LAN_API

def commandPort() { "4003" }

metadata {
	definition(name: "Govee v2 White Light Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Light"
		capability "SwitchLevel"
		capability "Refresh"
        capability "Initialize"
        
        attribute "cloudAPI", "string"
        attribute "online", "string"        
        
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)
            if (ipLookup() != "N/A") { 
            input(name: "lanControl", type: "bool", title: "Enable Local LAN control", description: "This is a advanced feature that only worked with some devices. Do not enable unless you are sure your device supports it", defaultValue: false)
            }
            if (lanControl) {
                input("retryInt", "number", title: "Retry Interval", description: "Time between command Retries in milliseconds. Default:3000", defaultValue:3000, range: 750..30000, width:5)
                input("maxRetry", "number", title: "Max number of Retries", description: "Max number of time the command will be resubmited. Default:2", defaultValue:2, range: 0..10, width:2)
                input("fadeInc", "decimal", title: "% Change each Increment of fade", defaultValue: 1)
            }
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
            input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true) 
		}
		
	}
}

def poll() {
    if (debugLog) {log.warn "poll(): Poll Initated"}
	refresh()
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
    unschedule(poll)
    if (pollRate > 0) runIn(pollRate,poll)
    if (lanControl) { 
        devStatus() 
    } else {
        getDeviceState()
    }
}

def updated() {
    configure()
}

def configure() {
    if (debugLog) {log.warn "configure(): Configuration Changed"}
    if(device.getDataValue("retrievable") =='true'){
        if (debugLog) {log.warn "configure(): Device is retrievable. Setting up Polling"}
        unschedule()
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
    }
    if (debugLog) runIn(1800, logsOff) 
}

def initialize(){
    if (debugLog) {log.warn "initialize(): Driver Initializing"}
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
    if(device.getDataValue("retrievable") =='true'){
        if (debugLog) {log.warn "initialize(): Device is retrievable. Setting up Polling"}
        unschedule()
        resetApiStatus()
        if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        randomOffset(pollRateInt)
        runIn(offset,poll)
    }
        getDeviceState()
    }
    retrieveIPAdd()
    if (debugLog) runIn(1800, logsOff)
}


def installed(){
    if (debugLog) {log.warn "installed(): Driver Installed"}
    if(device.getDataValue("commands").contains("color")) {
        sendEvent(name: "hue", value: 0)
        sendEvent(name: "saturation", value: 100)
        sendEvent(name: "level", value: 100) }
    if(device.getDataValue("retrievable") =='true'){
        if (debugLog) {log.warn "installed(): Device is retrievable. Setting up Polling"}
        unschedule()
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
    }
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

def setLevel(float v,duration = 0) {
    if (lanControl) {
        lanSetLevel(v,duration) 
    } else {
        cloudSetLevel( v, 0)
        }
}
