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
#include Mavrrick.Govee_Cloud_RGB
#include Mavrrick.Govee_Cloud_Level
#include Mavrrick.Govee_LAN_API

/*** Static Lists and Settings ***/
@Field static Map scenes =  [:]
@Field static Map diyScenes =  [:]

def commandPort() { "4003" }

metadata {
	definition(name: "Govee v2 Color Lights Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "Actuator"
		capability "ColorControl"
		capability "ColorTemperature"
		capability "Light"
		capability "SwitchLevel"
		capability "ColorMode"
		capability "Refresh"
        capability "Initialize"
        capability "LightEffects"

        attribute "online", "string"
		attribute "colorName", "string"
        attribute "colorRGBNum", "number"        
        attribute "cloudAPI", "string"
        attribute "effectNum", "integer" 
        command "activateDIY", [
            [name: "diyName", type: "STRING", description: "DIY Number to activate"]
           ]
        command "snapshot", [
            [name: "snapshotNum", type: "STRING", description: "Activate Snapshot"]
           ]
/*        command "segmentedColorRgb", [
            [name: "segment", type: "STRING", description: "which segment to change exp [1,4,6,7,8,9]"],
            [name: "color", type: "COLOR_MAP", description: "color to set between 0 and 16777215"]
           ]
        command "segmentedBrightness", [
            [name: "segment", type: "STRING", description: "which segment to change exp [1,4,6,7,8,9]"],
            [name: "brightness", type: "NUMBER", description: "color to set between 0 and 100"]
           ] */
        command "musicMode", [
            [name: "musicMode", type: "NUMBER", description: "Music Mode Value"],
            [name: "sensitivity ", type: "NUMBER", description: "% sensativity"],
            [name: "autoColor", type: "ENUM", constraints: [0:"off", 1:"on"], description: "which segment to change exp [1,4,6,7,8,9]"],
            [name: "color ", type: "COLOR_MAP", description: "color to set"]            
           ]
        command "gradient", [
            [name: "Toggle", type: "ENUM", constraints: [0:"off", 1:"on"], description: "which segment to change exp [1,4,6,7,8,9]"],           
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
        unschedule(poll)
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
}

def updated() {
    initialize() 
}

def initialize(){
    if (debugLog) {log.warn "initialize(): Driver Initializing"}    
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
        }
    if(device.getDataValue("commands").contains("lightScene")) {
        sendEvent(name: "effectNum", value: 0) }
    unschedule()
    if (lanControl && ip) { 
        getDevType()
        retrieveScenes()
    } else if (lanControl == false) { 
        retrieveScenes2()
        retrieveStateData()
        if (state.diyScene.isEmpty()) {
            if (debugLog) {log.warn "configure(): retrieveScenes2() returned empty diyScenes list. Running retrieveDIYScenes() to get list from API"}
            retrieveDIYScenes()
        }
    }
    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        randomOffset(pollRateInt)
        runIn(offset,poll)
    }
    if (debugLog) runIn(1800, logsOff)
    
}


def installed(){
    if (debugLog) {log.warn "installed(): Driver Installed"}
    if(device.getDataValue("commands").contains("color")) {
        sendEvent(name: "hue", value: 0)
        sendEvent(name: "saturation", value: 100)
    }
    if(device.getDataValue("commands").contains("lightScene")) {
        sendEvent(name: "effectNum", value: 0) }
    if (pollRate > 0) runIn(pollRate,poll)
    retrieveScenes2()
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


def setColorTemperature(value,level = null,transitionTime = null)
{
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) { log.debug "setColorTemperature(): ${value}"}
    if (value < device.getDataValue("ctMin").toInteger()) { value = device.getDataValue("ctMin")}
    if (value > device.getDataValue("ctMax").toInteger()) { value = device.getDataValue("ctMax")}
    if (debugLog) { log.debug "setColorTemperate(): ColorTemp = " + value }
	int intvalue = value.toInteger()
    if (lanControl) {
        sendCommandLan(GoveeCommandBuilder("colorwc",value, "ct"))
        if (level != null) setLevel(level,transitionTime);
        sendEvent(name: "colorTemperature", value: intvalue)
        sendEvent(name: "switch", value: "on")
        sendEvent(name: "colorMode", value: "CT")
        if (effectNum != 0){
            sendEvent(name: "effectNum", value: 0)
            sendEvent(name: "effectName", value: "None") 
        }
	    setCTColorName(intvalue)
        }
    else {
       if (device.currentValue("cloudAPI") == "Retry") {
            log.error "setColorTemperature(): CloudAPI already in retry state. Aborting call." 
         } else {        
            sendEvent(name: "cloudAPI", value: "Pending")
            if (level != null) setLevel(level,transitionTime);
		    sendCommand("colorTemperatureK", intvalue,"devices.capabilities.color_setting")
       }
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

def segmentedColorRgb(segment, value) {
    if (value instanceof Map) {
		def h = value.containsKey("hue") ? value.hue : null
		def s = value.containsKey("saturation") ? value.saturation : null
		def b = value.containsKey("level") ? value.level : null
        if (b == null) { b = device.currentValue("level") }
    	hsbcmd = [h,s,b]
    }
    if (debugLog) { log.debug "segmentedColorRgb(): Cmd = ${hsbcmd}"}

	rgb = hubitat.helper.ColorUtils.hsvToRGB(hsbcmd)
	def rgbmap = [:]
	rgbmap.r = rgb[0]
	rgbmap.g = rgb[1]
	rgbmap.b = rgb[2]       
    rgbvalue = ((rgb[0] & 0xFF) << 16) | ((rgb[1] & 0xFF) << 8) | ((rgb[2] & 0xFF) << 0)
    if (debugLog) {log.debug ("retrieveScenes(): ${rgbvalue}")} 
    values = '{"segment":'+segment+',"rgb":'+rgbvalue+'}'
//    values = '{"segment":'+segment+',"rgb":'+value+'}'
    sendCommand("segmentedColorRgb", values,"devices.capabilities.segment_color_setting")    
}

def segmentedBrightness(segment, brightness) {
    values = '{"segment":'+segment+',"brightness":'+brightness+'}'
    sendCommand("segmentedBrightness", values,"devices.capabilities.segment_color_setting")    
}

def musicMode(musicMode, sensitivity, autoColor, color) {
        if (debugLog) {log.debug ("retrieveScenes(): auto color is set to ${autoColor}")}
    if (autoColor == "on")  { 
        if (debugLog) {log.debug ("retrieveScenes(): auto color is set to ${autoColor}")}
        autocolor2 = 1
    }
    if (autoColor == "off")  {
        autocolor2 = 0
    }
    if (debugLog) {log.debug ("retrieveScenes(): auto color is set to ${autocolor2}")}
    values = '{"musicMode":'+musicMode+',"sensitivity":'+sensitivity+',"autoColor":'+autocolor2+'}'
//    values = '{"musicMode":'+musicMode+',"autoColor":'+autocolor2+'}'
    sendCommand("musicMode", values,"devices.capabilities.music_setting")    
}

////////////////////
// Helper Methods //
////////////////////

def setCTColorName(int value)
{
		if (value < 2600) {
			sendEvent(name: "colorName", value: "Warm White")
		}
		else if (value < 3500) {
			sendEvent(name: "colorName", value: "Incandescent")
		}
		else if (value < 4500) {
			sendEvent(name: "colorName", value: "White")
		}
		else if (value < 5500) {
			sendEvent(name: "colorName", value: "Daylight")
		}
		else if (value >=  5500) {
			sendEvent(name: "colorName", value: "Cool White")
		}	
}
