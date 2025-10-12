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
            input("multiSocketAdd", "bool", title: "Enable Multi-Socket Support", description: "By flipping this switch you tell the driver to enable Child devices for each outlet if possible", defaultValue: true)
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
    if (outlets != getChildDevices().size() && multiSocketAdd == true) {
        socketChildAdd()
    }
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
    int outlets = device.getDataValue("commands").count("socketToggle")
    if (outlets != getChildDevices().size() && multiSocketAdd == true) {
        socketChildAdd()
    }
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

///////////////////////////
// Child Device Routines //
///////////////////////////

def socketChildAdd() {
    int sockets = device.getDataValue("commands").count("socketToggle")
    sockets.times{
        outletNum = it+1
        log.info "socketChildAdd(): creating device for outlet $outletNum"
        addSocketDeviceHelper(outletNum)
    }    
}

def addSocketDeviceHelper(outletNum) {
	//Driver Settings
    driver = "Govee v2 Sockets Driver - Component"
    deviceID = device.getDataValue("deviceID")
    deviceName = device.label+"_Outlet"+outletNum
    deviceModel = device.getDataValue("deviceModel")
	Map deviceType = [namespace:"Mavrrick", typeName: driver]
	Map deviceTypeBak = [:]
	String devModel = deviceModel
    String dni = "Govee_${deviceID}_Outlet${outletNum}"
    APIKey = device.getDataValue("apiKey")
	Map properties = [name: driver, label: deviceName, deviceID: deviceID, deviceModel: deviceModel, socketNumber: outletNum, apiKey: APIKey]
    if (debugLog) { log.debug "Creating Child Device"}

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	} 
}

def childSwitchUpdate(instance, toggle) {
    int outlet = instance.substring(12).toInteger()
    child = getChildDevice("Govee_${device.getDataValue("deviceID")}_Outlet${outlet}") 
     if (debugLog) {log.debug "childSwitchUpdate(): Update to child switch device. Child $child switch state to $toggle"} 
    child.sendEvent(name: "switch", value: toggle)
}
