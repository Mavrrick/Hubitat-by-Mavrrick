// Hubitat driver for Govee Galaxy Projector driver using Cloud API
// Version 1.0.20
//
// 9-12-22  Initial release
// 9-30-22  Resolve issue with polling, Enhance Debug Logging
// 10-5-22  Adding Lan Control options to driver. Additional Logging.
// 10-12-22 Updated text verbiage on preferences
// 11-3-22  Send Rate Limits to Parent app and adjust to work with limited devices.
// 11-4-22  Added methods for Lan control to have proper fade control. 
// 11-5-22  Updated to reflect on state when options other 'On' switch are used
// 11-22-22 Updated to validate successful api call before changing driver status.
// 12-19-22 Modifieid polling to properly allow a value of 0 for no polling
// 1-21-23  Changed position of setlLevl action in setColor command.
// 1-30-23  Added check to see if device is in Retry state and abort new commands until cleared.
// 4-4-23   Added ability for parent app to update API Key associated with device
// 4-7-23   Added reset of Cloud API State to getDeviceStatus and initialize routine

import hubitat.helper.InterfaceUtils
import hubitat.helper.HexUtils
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_RGB
#include Mavrrick.Govee_Cloud_Level
#include Mavrrick.Govee_LAN_API

def commandPort() { "4003" }

metadata {
	definition(name: "Govee v2 Galaxy Projector", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "Actuator"
		capability "ColorControl"
		capability "Light"
		capability "SwitchLevel"
		capability "ColorMode"
		capability "Refresh"
        capability "Initialize"
        capability "LightEffects"
		
		attribute "colorName", "string"
        attribute "colorRGBNum", "number"        
        attribute "cloudAPI", "string"
        attribute "effectNum", "integer" 
        command "activateDIY", [
            [name: "diyName", type: "STRING", description: "DIY Number to activate"]
           ]
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)
			input(name: "aRngBright", type: "bool", title: "Alternate Brightness Range", description: "For devices that expect a brightness range of 0-254", defaultValue: false)
            input(name: "lanControl", type: "bool", title: "Enable Local LAN control", description: "This is a advanced feature that only worked with some devices. Do not enable unless you are sure your device supports it", defaultValue: false)
            if (lanControl) {
            input("ip", "text", title: "IP Address", description: "IP address of your Govee light", required: false)
            input("fadeInc", "decimal", title: "% Change each Increment of fade", defaultValue: 1)
            }
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
//    if(device.getDataValue("retrievable") =='true'){
//        if (debugLog) {log.warn "refresh(): Device is retrievable. Setting up Polling"}
        unschedule(poll)
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
//    }
//    if (debugLog) runIn(1800, logsOff)
}

def updated() {
    configure() 
}

def configure() {
    if (debugLog) {log.warn "configure(): Driver Updated"}
    if(device.getDataValue("retrievable") =='true'){
        if (debugLog) {log.warn "configure(): Device is retrievable. Setting up Polling"}
        unschedule()
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
    }
    if (lanControl && ip) { 
        getDevType()
        if ( parent.atomicState."${"lightEffect_"+(device.getDataValue("DevType"))}" == null ) {
            if (debugLog) {log.warn "configure(): No Scenes present for device type. Initiate setup in parent app"}
            parent.lightEffectSetup()
            retrieveScenes()
        } else {
        retrieveScenes()    
        }
    } else {
        retrieveScenes2()
        retrieveDynamicScene("diyScene")
        retrieveDynamicScene("snapshot") 
    }
    if (debugLog) runIn(1800, logsOff) 
}

def initialize(){
    if (debugLog) {log.warn "initialize(): Driver Initializing"}    
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
        }
    if (device.getDataValue("retrievable") =='true'){
        if (debugLog) {log.warn "initialize(): Device is retrievable. Setting up Polling"}
        unschedule()
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
    }
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

def  setEffect(effectNo) {
    if (lanControl) {
        lanSetEffect (effectNo)
    } else { 
        cloudSetEffect (effectNo)
    }
}

def setNextEffect() {
    if (lanControl) {
        lanSetNextEffect ()
    } else {
        cloudSetNextEffect ()
    }
} 
      
def setPreviousEffect() {
    if (lanControl) {
        lanSetPreviousEffect ()
    } else {
        cloudSetPreviousEffect ()         
    }
}


def activateDIY(diyActivate) {
    if (lanControl) {
        lanActivateDIY (diyActivate)
    } else {
        cloudActivateDIY (diyActivate)
    }
}
