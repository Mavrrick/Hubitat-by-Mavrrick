// Hubitat driver for Govee Appliances with Lights using Cloud API
// Version 1.0.19
//
// 2022-09-12 -	Initial Driver release for Govee Appliance devices
// 2022-10-19 - Added Attributes to driver
// 2022-11-3 - Send Rate Limits to parent.
// 2022-11-5 - Added update to Switch value to On when Gear or Mode are used
// 2022-11-20 - Added a pending change condition and validation that the call was successful
// ----------- A retry of the last call will be attempted if rate limit is the cause for it failing
// 2022-11-21 Moved status of cloud api call to the it's own attribute so it can be monitored easily
// 2022-12-19 Added Actuator capbility to more easily integrate with RM
// 2023-4-4   API Key update is now possible
// 2023-4-7   Update Initialize and getDeviceStatus routine to reset CloudAPI Attribute

// "supportCmds": [ "turn", "mode", "mist:switch", "mist:duration", "mist:gear", "light:switch", "light:duration", "light:brightness", "light:color", "light:scene" ]

import hubitat.helper.InterfaceUtils
import hubitat.helper.HexUtils

#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_MQTT

metadata {
	definition(name: "Govee v2 Aroma Diffuser Driver with Lights", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Actuator"
        capability "Initialize"
        capability "ColorControl"
        capability "Configuration" 
        attribute "mist_gear", "number"
        attribute "mist_switch", "string"
        attribute "mode", "string"
        attribute "cloudAPI", "string"
        attribute "presetScene","string"
        attribute "light_brightness","number"
        attribute "light_switch","string"
        attribute "lackWaterEvent", "string"        
        
//legacy commands for Appliance devices        
        command "mode" , [[name: modeValue, type: 'NUMBER', description: "Mode will adjust the device operating mode. Valid values are   "]]

//new commands for Appliance devices        
        command "mist_switch_on" , [[name: "Mist on/off", description: "will turn on misting effect"]]
        command "mist_switch_off" , [[name: "Mist on/off", description: "will turn off the misting effect "]]
        command "mist_duration" , [[name: "Mist duration", type: 'NUMBER', description: "will set a sleep timer for the misting  "]]
        command "mist_gear" , [[name: "Mist Speed", type: 'NUMBER', description: "Gear will adjust speed of misting from device  "]]
		command "light_switch_on" , [[name: "light on", description: "Will turn Light on"]]
		command "light_switch_off" , [[name: "light off", description: "will turn light off "]]
        command "light_duration" , [[name: "light duration", type: 'NUMBER', description: "Will set sleep timer for light "]]
        command "light_brightness" , [[name: "Brightness", type: 'NUMBER', description: "Not currently working  ", range: '0..100']]
        command "presetScene" , [[name: "light scene", type: 'NUMBER', description: "Allows you to select scene for light effects"]] 
        
    }

	preferences {		
		section("Device Info") {
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
		}
		
	}
}


/*
Commands for standard functions for entire device
*/
def on() {
         if (device.currentValue("cloudAPI") == "Retry") {
             log.error "on(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 1 ,"devices.capabilities.on_off")
            }
}

def off() {
        if (device.currentValue("cloudAPI") == "Retry") {
             log.error "off(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 0 ,"devices.capabilities.on_off")
            }
} 

def presetScene(presetScene){
        if (device.currentValue("cloudAPI") == "Retry") {
             log.error "off(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("presetScene", presetScene ,"devices.capabilities.mode")
            }
}

def mode(modeValue){
    sendEvent(name: "cloudAPI", value: "Pending")    
    sendCommandlegacy("mode", modeValue)
}

/*
Commands for Misting functions functions
*/

def mist_duration(duration){
    sendEvent(name: "cloudAPI", value: "Pending")
    sendCommandlegacy("mist:duration", duration)
} 

def mist_gear(gear){
    sendEvent(name: "cloudAPI", value: "Pending")
    sendCommandlegacy("mist:gear", gear)
}

def mist_switch_on() {
    sendEvent(name: "cloudAPI", value: "Pending")
	sendCommandlegacy("mist:switch", 1 )
}

def mist_switch_off() {
    sendEvent(name: "cloudAPI", value: "Pending")
	sendCommandlegacy("mist:switch", 0 )
}

/*
Standard commands for Light functions of device.
*/


def light_switch_on() {
    sendEvent(name: "cloudAPI", value: "Pending")
	sendCommandlegacy("light:switch", 1 )
}

def light_switch_off() {
    sendEvent(name: "cloudAPI", value: "Pending")
	sendCommandlegacy("light:switch", 0 )
}

def light_brightness(bright) {
    sendEvent(name: "cloudAPI", value: "Pending")
	sendCommandlegacy("light:brightness", bright)
}

def light_duration(ltdur) {
    sendEvent(name: "cloudAPI", value: "Pending")
	sendCommandlegacy("light:duration", ltdur)
}

def light_scene(sceneNum) {
    sendEvent(name: "cloudAPI", value: "Pending")
    sendCommandlegacy("light:switch", 1 )
	sendCommandlegacy("light:scene", sceneNum)
}


def setColor(value) {
    if (debugLog) { log.debug "setColor(): HSBColor = "+ value + "${device.currentValue("level")}"}
	if (value instanceof Map) {
		def h = value.containsKey("hue") ? value.hue : null
		def s = value.containsKey("saturation") ? value.saturation : null
		def b = value.containsKey("level") ? value.level : null
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
     
        if (device.currentValue("cloudAPI") == "Retry") {
            log.error "setHsb(): CloudAPI already in retry state. Aborting call." 
        } else { 
        sendEvent(name: "cloudAPI", value: "Pending")
        sendCommand("light:color", rgbmap)
        }
//    }
    if(100 != device.currentValue("level")?.toInteger()) {
    light_brightness(100)
    }
}

def setHue(h)
{
    setHsb(h,device.currentValue( "saturation" )?:100,device.currentValue("level")?:100)
}

def setSaturation(s)
{
	setHsb(device.currentValue("hue")?:0,s,device.currentValue("level")?:100)
}



/*
Method for sending commands to Govee Cloud API
*/

private def sendCommandlegacy(String command, payload) {


     def params = [
            uri   : "https://developer-api.govee.com",
            path  : '/v1/appliance/devices/control',
			headers: ["Govee-API-Key": device.getDataValue("apiKey"), "Content-Type": "application/json"],
            contentType: "application/json",      
			body: [device: device.getDataValue("deviceID"), model: device.getDataValue("deviceModel"), cmd: ["name": command, "value": payload]],
        ]
    

try {

			httpPut(params) { resp ->
				
                if (debugLog) {log.debug "response.data="+resp.data}
		        if (debugLog) { log.debug "response.data=" + resp.data.code }
                if (resp.data.code == 200 && command == "turn") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: payload)
                    }    
                else if (resp.data.code == 200 && command == "mode") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "mode", value: payload)
                    }
                else if (resp.data.code == 200 && command == "mist:switch") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "mist:switch", value: payload)
                    }
                else if (resp.data.code == 200 && command == "light:switch") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "light:switch", value: payload)
                    }
                else if (resp.data.code == 200 && command == "light:brightness") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "light_brightness", value: payload)
                    }
                else if (resp.data.code == 200 && command == "mist:gear") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "mist_switch", value: "on")
                    sendEvent(name: "mist:gear", value: payload)
                    }
                else if (resp.data.code == 200 && command == "mist:duration") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "mist_switch", value: "on")
                    sendEvent(name: "mist_duration", value: payload)
                    }
                else if (resp.data.code == 200 && command == "light:duration") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "light_switch", value: "on")
                    sendEvent(name: "light_duration", value: payload)
                    }
                else if (resp.data.code == 200 && command == "light:scene") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "light_switch", value: "on")
                    sendEvent(name: "colorMode", value: "EFFECTS")
                    sendEvent(name: "light:scene", value: payload)                  
                    }
               else if (resp.data.code == 200 && command == "light:color") {
                    r=payload.r
					g=payload.g
					b=payload.b
					HSVlst=hubitat.helper.ColorUtils.rgbToHSV([r,g,b])
					hue=HSVlst[0].toInteger()
					sat=HSVlst[1].toInteger()
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "light:switch", value: "on")
                    sendEvent(name: "colorMode", value: "RGB")
					sendEvent(name: "hue", value: hue)
					sendEvent(name: "saturation", value: sat)
                    }
                resp.headers.each {
                    if (debugLog) {log.debug "${it.name}: ${it.value}"}                    
                    name = it.name
                    value=it.value
                    if (name == "X-RateLimit-Remaining") {
                        state.DailyLimitRemaining = value
                        parent.apiRateLimits("DailyLimitRemainingV2", value)
                    }
                    if (name == "API-RateLimit-Remaining") {
                        state.MinRateLimitRemainig = value
                        parent.apiRateLimits("MinRateLimitRemainigV2", value)
                    }
            }
				return resp.data
		        return resp.header
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
        if (e.statusCode == 429) {
            log.error "sendCommand():Cloud API Returned code 429, Rate Limit exceeded. Attempting again in one min."
            sendEvent(name: "cloudAPI", value: "Retry")
            pauseExecution(60000)
            sendCommand(command, payload)
        } 
        else {
          log.error "sendCommand():Unknwon Error. Attempting again in one min." 
//            sendEvent(name: "cloudAPI", value: "Retry")
//            pauseExecution(60000)
//            sendCommand(command, payload)
        }
		return 'unknown'
	}
}


def updated() {
if (logEnable) runIn(1800, logsOff)
    retrieveStateData()
}


def installed(){
    getDeviceState()    
    retrieveStateData() 
}

def initialize() {
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
    unschedule(poll)
    if (pollRate > 0) runIn(pollRate,poll)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

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

def configure() {
    if (debugLog) {log.warn "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)     
    retrieveStateData()    
    if (debugLog) runIn(1800, logsOff) 
}
