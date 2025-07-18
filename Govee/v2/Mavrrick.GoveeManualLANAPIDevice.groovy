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
			input(name: "aRngBright", type: "bool", title: "Alternate Brightness Range", description: "For devices that expect a brightness range of 0-254", defaultValue: false)
            input("fadeInc", "decimal", title: "% Change each Increment of fade", defaultValue: 1)
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

// {"msg":{"cmd":"devStatus","data":{}}}
/* def devStatus() {
        sendCommandLan(GoveeCommandBuilder("devStatus", null , "status"))
        if (descLog) log.info "${device.label} status was requested."  
} */

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
    
def setColor(value) {
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) { log.debug "setColor(): HSBColor = "+ value + "${device.currentValue("level")}"}
	if (value instanceof Map) {
		def h = value.containsKey("hue") ? value.hue : null
		def s = value.containsKey("saturation") ? value.saturation : null
		def b = value.containsKey("level") ? value.level : null
        
        def theColor = getColor(h,s)
        if (descLog)
        {
            if (theColor == "Unknown")
            {
                if (debugLog) log.debug "trying alt. color name method"
                theColor = convertHueToGenericColorName(h,s)
                if (debugLog) log.debug "alt. method got back $theColor"
            }
            if (theColor != "Unknown") log.info "${device.label} Color is $theColor"
            else log.info "${device.label} Color is $value"
            sendEvent(name: "colorName", value: theColor)
        }
        
        if (b == null) { b = device.currentValue("level") }
		setHsb(h, s, b)
	} else {
        if (debugLog) {log.debug "setColor(): Invalid argument for setColor: ${value}"}
    }
}

def setHsb(h,s,b)
{

	hsbcmd = [h,s,b]
    if (debugLog) { log.debug "setHsb(): Cmd = ${hsbcmd}"}

	rgb = hubitat.helper.ColorUtils.hsvToRGB(hsbcmd)
	def rgbmap = [:]
	rgbmap.r = rgb[0]
	rgbmap.g = rgb[1]
	rgbmap.b = rgb[2]   
     
        if (debugLog) { log.debug "setHsb(): ${rgbmap}"}
        sendCommandLan(GoveeCommandBuilder("colorwc",rgbmap,"rgb"))
      	sendEvent(name: "hue", value: "${h}")
        sendEvent(name: "saturation", value: "${s}")
        sendEvent(name: "switch", value: "on")
   		sendEvent(name: "colorMode", value: "RGB")
        if (effectNum != 0){
            sendEvent(name: "effectNum", value: 0)
            sendEvent(name: "effectName", value: "None") 
        }
    if(100 != device.currentValue("level")?.toInteger()) {
    setLevel(100)
    }
}

def setHue(h)
{
    setHsb(h,device.currentValue( "saturation" )?:100,device.currentValue("level")?:100)
    if (descLog) log.info "${device.label} Hue was set to ${h}"    
}

def setSaturation(s)
{
	setHsb(device.currentValue("hue")?:0,s,device.currentValue("level")?:100)
    if (descLog) log.info "${device.label} Saturation was set to ${s}%"
}

def setLevel(float v,duration = 0){
    lanSetLevel(v,duration)
}

def setLevel2(int v){
    lanSetLevel(v)
}


//Turn Hubitat's 0-100 Brightness range to the 0-254 expected by some devices


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
