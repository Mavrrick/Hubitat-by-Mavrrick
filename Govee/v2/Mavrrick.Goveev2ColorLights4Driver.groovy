// Hubitat driver for Govee Light, Plug, Switch driver using Cloud API
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

/*** Static Lists and Settings ***/
// @Field List sceneOptions []

def commandPort() { "4003" }

metadata {
	definition(name: "Govee v2 Color Lights 4 Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "Actuator"
		capability "ColorControl"
		capability "ColorTemperature"
		capability "Light"
		capability "SwitchLevel"
        capability "Configuration"
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
        command "segmentedColorRgb", [
            [name: "segment", type: "STRING", description: "which segment to change exp [1,4,6,7,8,9]"],
            [name: "color", type: "COLOR_MAP", description: "Color to set to Segment"]
           ]
/*        command "segmentedBrightness", [
            [name: "segment", type: "STRING", description: "which segment to change exp [1,4,6,7,8,9]"],
            [name: "brightness", type: "NUMBER", description: "color to set between 0 and 100"]
           ] */
        command "musicMode", [
            [name: "musicMode", type: "NUMBER", description: "Music Mode Value"],
            [name: "sensitivity ", type: "NUMBER", description: "% sensativity"],
            [name: "autoColor", type: "ENUM", constraints: [0:"off", 1:"on"], description: "which segment to change exp [1,4,6,7,8,9]"],
//            [name: "color ", type: "COLOR_MAP", description: "color to set"]            
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
		}
		
	}
}

def on() {
    if (lanControl) {
        sendCommandLan(GoveeCommandBuilder("turn",1, "turn"))
        sendEvent(name: "switch", value: "on")}
    else {
         if (device.currentValue("cloudAPI") == "Retry") {
             log.error "on(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 1 ,"devices.capabilities.on_off")
            }
        }
}

def off() {
    if (lanControl) {
        sendCommandLan(GoveeCommandBuilder("turn",0, "turn"))
        sendEvent(name: "switch", value: "off")}
    else {
        if (device.currentValue("cloudAPI") == "Retry") {
             log.error "off(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 0 ,"devices.capabilities.on_off")
            }
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

def poll() {
    if (debugLog) {log.warn "poll(): Poll Initated"}
	refresh()
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
        if (debugLog) {log.warn "refresh(): Device is retrievable. Setting up Polling"}
        unschedule(poll)
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
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
    } else if (lanControl == false) { 
        retrieveScenes2()
        retrieveStateData()
        if (state.diyScene.isEmpty()) {
            if (debugLog) {log.warn "configure(): retrieveScenes2() returned empty diyScenes list. Running retrieveDIYScenes() to get list from API"}
            retrieveDIYScenes()
        }
    }    
    if (debugLog) runIn(1800, logsOff) 
    state.sceneMax = state.scenes.size()
    state.sceneValue = 0
}

def initialize(){
    if (debugLog) {log.warn "initialize(): Driver Initializing"}    
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
        }
    if(device.getDataValue("commands").contains("lightScene")) {
        sendEvent(name: "effectNum", value: 0) }
    if (debugLog) {log.warn "initialize(): Device is retrievable. Setting up Polling"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)
    getDeviceState()
    if (debugLog) runIn(1800, logsOff)
    
}


def installed(){
    if (debugLog) {log.warn "installed(): Driver Installed"}
    if(device.getDataValue("commands").contains("color")) {
        sendEvent(name: "hue", value: 0)
        sendEvent(name: "saturation", value: 100)
        sendEvent(name: "level", value: 100) }
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)
    getDeviceState()
    retrieveScenes2()
    retrieveStateData()
    state.sceneMax = state.scenes.size()
    state.sceneValue = 0    
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

def  setEffect(effectNo) {
            if (lanControl) {
    effectNumber = effectNo.toString()
    if ((parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}" != null) && (parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}".containsKey(effectNumber))) {
        String sceneInfo =  parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}".get(effectNumber).name
        String sceneCmd =  parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}".get(effectNumber).cmd
        if (debugLog) {log.debug ("setEffect(): Activate effect number ${effectNo} called ${sceneInfo} with command ${sceneCmd}")}
        sendEvent(name: "effectName", value: sceneInfo)
        sendEvent(name: "effectNum", value: effectNumber)
        sendEvent(name: "switch", value: "on")
//    if (debugLog) {log.debug ("setEffect(): setEffect to ${effectNo}")}
        String cmd2 = '{"msg":{"cmd":"ptReal","data":{"command":'+sceneCmd+'}}}'
        if (debugLog) {log.debug ("setEffect(): command to be sent to ${cmd2}")}
        sendCommandLan(cmd2)
   } else {
        sendEvent(name: "effectNum", value: effectNumber)
        sendEvent(name: "switch", value: "on")
    // Cozy Light Effect (static Scene to very warm light)
    if (effectNo == 6) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Cozy Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        sendEvent(name: "colorTemperature", value: 2000)
	    setCTColorName(2000)
        sendEvent(name: "effectName", value: "Cozy")
    }
    // Sunrise Effect
    if (effectNo == 9) {
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",1, "level"))
        sendEvent(name: "level", value: 1)
        fade(100,1800)        
        sendEvent(name: "effectName", value: "Sunrise")
    }
    // Sunset Effect
    if (effectNo == 10) {
        sendCommandLan(GoveeCommandBuilder("colorwc",6500, "ct"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        fade(0,1800)
        sendEvent(name: "effectName", value: "Sunset")
    }
    // Warm White Light Effect (static Scene to very warm light)
    if (effectNo == 11) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Warm White Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",3500, "ct"))
        sendEvent(name: "colorTemperature", value: 3500)
	    setCTColorName(3500)
        sendEvent(name: "effectName", value: "Warm White")
    } 
    // Daylight Light Effect    
    if (effectNo == 12) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Daylight Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",5600, "ct"))
        sendEvent(name: "colorTemperature", value: 5600)
	    setCTColorName(5600)
        sendEvent(name: "effectName", value: "Daylight")
    }
    // Cool White Light Effect    
    if (effectNo == 13) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Cool White Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",6500, "ct"))
        sendEvent(name: "colorTemperature", value: 6500)
	    setCTColorName(6500)
        sendEvent(name: "effectName", value: "Cool White")
    }  
    // Night Light Effect   
    if (effectNo == 14) {
        if (debugLog) {log.debug ("setEffect(): Static Night Light Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        sendEvent(name: "colorTemperature", value: 2000)
	    setCTColorName(2000)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",5, "level"))
        sendEvent(name: "level", value: 5)
        sendEvent(name: "effectName", value: "Night Light")
    }
    // Focus Effect   
    if (effectNo == 15) {
        if (debugLog) {log.debug ("setEffect(): Focus Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",4500, "ct"))
        sendEvent(name: "colorTemperature", value: 4500)
	    setCTColorName(4500)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        sendEvent(name: "effectName", value: "Focus")
    } 
    // Relax Effect   
    if (effectNo == 16) {
        if (debugLog) {log.debug ("setEffect(): Static Relax Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:255, g:194, b:194], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        sendEvent(name: "effectName", value: "Relax")
    }
    // True Color Effect   
    if (effectNo == 17) {
        if (debugLog) {log.debug ("setEffect(): True Color Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",3350, "ct"))
        sendEvent(name: "colorTemperature", value: 3350)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "effectName", value: "True Color")
    }
    // TV Time Effect   
    if (effectNo == 18) {
        if (debugLog) {log.debug ("setEffect(): Static TV Time Effect Called. Calling CT Command directly")}        
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:179, g:134, b:254], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",45, "level"))
        sendEvent(name: "level", value: 45)
        sendEvent(name: "effectName", value: "TV Time")
    }
    // Plant Growth Effect   
    if (effectNo == 19) {
        if (debugLog) {log.debug ("setEffect(): Static Plant Growth Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:247, g:154, b:254], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",45, "level"))
        sendEvent(name: "level", value: 45)
        sendEvent(name: "effectName", value: "Plant Growth")
    }
    }
            } else { 
                log.debug ("setEffect(): Setting effect via cloud api to scene number  ${effectNo}")
//                effectNumber = effectNo.toString()
                sendCommand("lightScene", effectNo,"devices.capabilities.dynamic_scene")
                   }
}

def setNextEffect() {
    if (lanControl) {
    if (debugLog) {log.debug ("setNextEffect(): Current Color mode ${device.currentValue("colorMode")}")}
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) {log.debug ("setNextEffect(): current effectNum ${device.currentValue("effectNum")}")}
    if (device.currentValue("effectNum") == "0" || device.currentValue("effectNum") == device.getDataValue("maxScene")) {
        setEffect(1)
    } 
    else if (device.currentValue("effectNum") == "21") {
        if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
        setEffect(101) 
    } else {
        if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
        int nextEffect = device.currentValue("effectNum").toInteger()+1
        setEffect(nextEffect)
        }  
    } else {
        keys = []
        names = []
        keys.addAll(state.scenes.keySet())
        names.addAll(state.scenes.values())
//        if (debugLog) {log.debug ("setNextEffect(): current Name ${names.get(state.sceneValue)} value ${keys.get(state.sceneValue)}")}
        if (state.sceneValue == state.sceneMax) state.sceneValue = 0  
        if (state.sceneValue == 0) {
            if (debugLog) {log.debug ("setNextEffect(): Current scene value is 0 setting to first scene in list")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneValue + 1
        } else {
            if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneValue + 1
        }  
    }
} 
      
def setPreviousEffect() {
    if (lanControl) {
        if (debugLog) {log.debug ("setPreviousEffect(): Current Color mode ${device.currentValue("colorMode")}")}
        unschedule(fadeUp)
        unschedule(fadeDown)
        if (device.currentValue("effectNum") == "0" || device.currentValue("effectNum") == "1") {
            setEffect(device.getDataValue("maxScene"))
            } else if (device.currentValue("effectNum") == 101) {
            setEffect(21) 
            } else {
                if (debugLog) {log.debug ("setNextEffect(): Increment to next scene}")}
                int prevEffect = device.currentValue("effectNum").toInteger()-1
                setEffect(prevEffect)
        }
    } else {
        keys = []
        names = []
        keys.addAll(state.scenes.keySet())
        names.addAll(state.scenes.values())
//        if (debugLog) {log.debug ("setPreviousEffect(): current Name ${names.get(state.sceneValue)} value ${keys.get(state.sceneValue)}")}       
            if (state.sceneValue == 0) {
            if (debugLog) {log.debug ("setPreviousEffect(): Current scene value is 0 setting to first scene in list")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneMax-1 
        } else {
            if (debugLog) {log.debug ("setPreviousEffect(): Increment to next scene")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneValue - 1
        }          
    }
}


def activateDIY(diyActivate) {
    if (lanControl) {
        String diyEffectNumber = diyActivate.toString()
        String sceneInfo = parent.state.diyEffects.(device.getDataValue("deviceModel")).get(diyEffectNumber).name
        String sceneCmd = parent.state.diyEffects.(device.getDataValue("deviceModel")).get(diyEffectNumber).cmd 
        if (debugLog) {log.debug ("activateDIY(): Activate effect number ${diyActivate} called ${sceneInfo} with command ${sceneCmd}")}
        sendEvent(name: "effectName", value: sceneInfo)
        sendEvent(name: "effectNum", value: diyEffectNumber)
        sendEvent(name: "switch", value: "on")
        String cmd2 = '{"msg":{"cmd":"ptReal","data":{"command":'+sceneCmd+'}}}'
        if (debugLog) {log.debug ("activateDIY(): command to be sent to ${cmd2}")}
        sendCommandLan(cmd2)
    } else {
             sendCommand("diyScene", diyActivate,"devices.capabilities.dynamic_scene")
    }
}

def segmentedColorRgb(segment, value) {
    if (debugLog) {log.debug ("segmentedColorRgb(): ${segment} ${value}")} 
    if (value instanceof Map) {
        if (debugLog) {log.debug ("segmentedColorRgb(): instance of map ${value}")} 
		def h = value.containsKey("hue") ? value.hue : null
		def s = value.containsKey("saturation") ? value.saturation : null
		def b = value.containsKey("level") ? value.level : null
        if (b == null) { b = device.currentValue("level") }
    	hsbcmd = [h,s,b]
    } else  {
        valueMap = [:]
        value.split(",").each{ item ->
            valueMap.put(item.substring(0,(item.indexOf(':'))),item.substring((item.indexOf(':')+1),item.length()).toInteger())
        }
        if (debugLog) {log.debug ("segmentedColorRgb(): string Conversion ${value} ${valueMap}")}
		def h = valueMap.containsKey("hue") ? valueMap.hue : null
		def s = valueMap.containsKey("saturation") ? valueMap.saturation : null
		def b = valueMap.containsKey("level") ? valueMap.level : null
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
