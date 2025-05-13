// Hubitat driver for Govee Galaxy Projector driver using Cloud API
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
#include Mavrrick.Govee_Cloud_RGB
#include Mavrrick.Govee_Cloud_Level
#include Mavrrick.Govee_LAN_API

/*** Static Lists and Settings ***/
// @Field static Map scenes =  [:]
// @Field static Map diyScenes =  [:]

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
        command "sceneLoad"
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)
			input(name: "aRngBright", type: "bool", title: "Alternate Brightness Range", description: "For devices that expect a brightness range of 0-254", defaultValue: false)
            if (ipLookup() != "N/A") { 
            input(name: "lanControl", type: "bool", title: "Enable Local LAN control", description: "This is a advanced feature that only worked with some devices. Do not enable unless you are sure your device supports it", defaultValue: false)
            }
            if (lanControl) {
            input(name: "lanScenes", type: "bool", title: "Enable Local LAN Scene Control", description: "If this is active your device will use Local Scenes control. Leave off to use Scenes/DIY's/Snapshots from the cloud API", defaultValue: false)     
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
    unschedule(poll)
    if (pollRate > 0) runIn(pollRate,poll)
    if (lanControl) { 
        devStatus() 
    } else {
        getDeviceState()
    }
}

def updated() {
    initialize()
    sceneLoad()
}

def initialize(){
    if (debugLog) {log.warn "initialize(): Driver Initializing"}    
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
        }
    initDefaultValues()
    unschedule()

    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        randomOffset(pollRateInt)
        runIn(offset,poll)
    }
    device.removeSetting(ip) //remove legacy IP preference value
    retrieveIPAdd()
    if (debugLog) runIn(1800, logsOff)
    
}

def installed(){
    if (debugLog) {log.warn "installed(): Driver Installed"}
    initDefaultValues()
    if (pollRate > 0) runIn(pollRate,poll)
    retrieveScenes2()
    retrieveStateData()
    getDevType()
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

def sceneLoad() {
    
    if (lanScenes == null) {
    if (debugLog) {log.debug "sceneLoad(): lanScenes not set"}
        device.updateSetting('lanScenes', [type: "bool", value: true])
    }
    
    if (lanControl && lanScenes) { 
        getDevType()
        retrieveScenes()   
    } else if ((lanControl == false) || (lanControl && lanScenes == false)) { 
        retrieveScenes2()
        retrieveStateData()
        if (state.diyScene.isEmpty()) {
            if (debugLog) {log.warn "configure(): retrieveScenes2() returned empty diyScenes list. Running retrieveDIYScenes() to get list from API"}
            retrieveDIYScenes()
        }
    }
}

def initDefaultValues() {
    if (lanControl) { 
        lanInitDefaultValues() 
    } else {
        cloudInitDefaultValues()
    }
    if (lanControl) { 
        devStatus() 
    } else {
        getDeviceState()
    }
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
