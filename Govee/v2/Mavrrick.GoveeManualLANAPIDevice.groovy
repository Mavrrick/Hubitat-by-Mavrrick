// Hubitat driver for Govee Manual LAN API device setup
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices


import hubitat.helper.InterfaceUtils
import hubitat.helper.HexUtils
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

#include Mavrrick.Govee_LAN_API

def commandPort() { "4003" }

metadata {
	definition(name: "Govee Manual LAN API Device", namespace: "Mavrrick", author: "Mavrrick") {
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
		
		attribute "colorName", "string"
        attribute "cloudAPI", "string"
        attribute "effectNum", "integer" 
        command "activateDIY", [
            [name: "diyName", type: "STRING", description: "DIY Number to activate"]
           ]
        command "sceneLoad" 

    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:30", defaultValue:30, submitOnChange: true, width:4)
            input("fadeInc", "decimal", title: "% Change each Increment of fade", defaultValue: 1)
                input("retryInt", "number", title: "Retry Interval", description: "Time between command Retries in milliseconds. Default:3000", defaultValue:3000, range: 750..30000, width:5)
                input("maxRetry", "number", title: "Max number of Retries", description: "Max number of time the command will be resubmited. Default:2", defaultValue:2, range: 0..10, width:2)
            input(name: "lanScenesFile", type: "string", title: "LAN Scene File", description: "Please enter the file name with the Scenes for this device", defaultValue: "GoveeLanScenes_"+getDataValue("deviceModel")+".json")
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
    devStatus() 
}

def updated() {
    initialize()
}

def initialize(){
    if (debugLog) {log.warn "initialize(): Driver Initializing"}    
    lanInitDefaultValues()
    unschedule()
	if (lanControl) resetApiStatus()
    if (debugLog) runIn(1800, logsOff)
//    if (pollRate > 0) runIn(pollRate,poll)
    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        randomOffset(pollRateInt)
        runIn(offset,poll)
    }
    sceneLoad()
    devStatus()
    
}


def installed(){
    initialize()
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

 def sceneLoad() {
       
        getDevType()
        retrieveScenes()
} 


/////////////////////////
// Commands for Driver //
/////////////////////////

def on() {
        lanOn() 
}

def off() {
        lanOff() 
}

def setColorTemperature(value,level = null,transitionTime = null) {
    lanCT(value, level, transitionTime)    
}

def  setColor(value) {
        lanSetColor (value)
}

def  setHue(h) {
        lanSetHue (h)
}

def setSaturation(s) {
        lanSetSaturation (s)
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

def setLevel(float v,duration = 0){
    lanSetLevel(v,duration)
}

def setLevel2(int v){
    lanSetLevel(v)
}


def  setEffect(effectNo) {
    lanSetEffect (effectNo)
    
}

def setNextEffect() {
    lanSetNextEffect () 
}
      
def setPreviousEffect() {
    lanSetPreviousEffect () 
}


def activateDIY(diyActivate) {
    lanActivateDIY (diyActivate)
}



def getColor(hue, saturation)
{
   
    def color = "Unknown"
    
   	//Set the hue and saturation for the specified color.
	switch(hue) {
		case 0:
            if (saturation == 0)
              color = "White"
            else color = "Red"
		    break;
        case 53:
            color = "Daylight"
			break;
        case 23:
  		    color = "Soft White"
			break;
        case 20:
		    color = "Warm White"
			break;
        case 61:
            color = "Navy Blue"
            break;
        case 65:
		    color = "Blue"
			break;
		case 35:
			color = "Green"
			break;
        case 47:
        	color = "Turquoise"
            break;
        case 50:
            color = "Aqua"
            break;
        case 13:
            color = "Amber"
            break;
		case 17:
        case 25:
			color = "Yellow"
			break; 
        case 7:
            color ="Safety Orange" 
            break;
		case 10:
			color = "Orange"
			break;
        case 73:
            color = "Indigo"
            break;
		case 82:
			color = "Purple"
			break;
        case 90:
        case 91:
		case 90.78:
			color = "Pink"
			break;
        case 94:
            color = "Rasberry"
            break;
        case 4:
            color = "Brick Red"
            break;  
        case 69:
            color = "Slate Blue"
            break;
    }
  return color
}


///////////////////////////////////////////
// Helper Methods /////////////////////////
///////////////////////////////////////////

def randomOffset(int pollRateInt){
    if (debugLog) {log.debug "randomOffset(): Entered random offset Calc with ${pollRateInt}"} 
    Random random = new Random()
    offset = random.nextInt(pollRateInt) // see explanation below
//    int number = random.nextInt(pollRateInt) + start; // see explanation below
    if (debugLog) {log.debug "randomOffset(): random offset is ${offset}"}    
    return offset
}
