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

private def sendCommandlegacy(String command, payload) {


     def params = [
            uri   : "https://developer-api.govee.com",
            path  : '/v1/devices/control',
			headers: ["Govee-API-Key": device.getDataValue("apiKey"), "Content-Type": "application/json"],
            contentType: "application/json",      
			body: [device: device.getDataValue("deviceID"), model: device.getDataValue("deviceModel"), cmd: ["name": command, "value": payload]],
        ]
    

try {

			httpPut(params) { resp ->
				
                if (debugLog) { log.debug "sendCommand(): response.data="+resp.data}
                code = resp.data.code
                if (code == 200 && command == "turn") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: payload)
                    } 
                 else if (code == 200 && command == "brightness") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "level", value: payload)
                    }
/*                 else if (code == 200 && command == "colorTem") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "colorMode", value: "CT")
                    sendEvent(name: "colorTemperature", value: payload)
                    setCTColorName(intvalue)
                    } */
                else if (code == 200 && command == "color") {
                    r=payload.r
					g=payload.g
					b=payload.b
					HSVlst=hubitat.helper.ColorUtils.rgbToHSV([r,g,b])
					hue=HSVlst[0].toInteger()
					sat=HSVlst[1].toInteger()
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "colorMode", value: "RGB")
					sendEvent(name: "hue", value: hue)
					sendEvent(name: "saturation", value: sat)
                    }
                resp.headers.each {
                    if (debugLog) { log.debug "sendCommand(): ${it.name}: ${it.value}" }                   
                    name = it.name
                    value=it.value
                    if (name == "X-RateLimit-Remaining") {
                        state.DailyLimitRemaining = value
                        parent.apiRateLimits("DailyLimitRemaining", value)
                    }
                    if (name == "API-RateLimit-Remaining") {
                        state.MinRateLimitRemainig = value
                        parent.apiRateLimits("MinRateLimitRemainig", value)
                    }
            }
                return resp.data
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
//        if (debugLog) {log.debug "sendCommand(): ${resp.header}"}
                if (e.statusCode == 429) {
            log.error "sendCommand():Cloud API Returned code 429, Rate Limit exceeded. Attempting again in one min."
                       sendEvent(name: "cloudAPI", value: "Retry")
                       pauseExecution(60000)
                       sendCommand(command, payload)                    
        } 
        else {
          log.error "sendCommand():Unknwon Error. Attempting again in one min." 
            sendEvent(name: "cloudAPI", value: "Retry")
            pauseExecution(60000)
            sendCommand(command, payload)
        }    
		return 'unknown'
	}
}

def poll() {
    if (debugLog) {log.warn "poll(): Poll Initated"}
	refresh()
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
    if(device.getDataValue("retrievable") =='true'){
        if (debugLog) {log.warn "refresh(): Device is retrievable. Setting up Polling"}
        unschedule(poll)
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
    }
    if (debugLog) runIn(1800, logsOff)
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

def GoveeCommandBuilder(String command1, value1, String type) {   
    if (type=="ct") {
        if (debugLog) {log.debug "GoveeCommandBuilder(): Color temp action"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            color {
            r 0
            g 0
            b 0
        }
            colorTemInKelvin value1}
    }
    def  command = cmd1.toString()
        if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
   else if (type=="rgb") {
       if (debugLog) {log.debug "GoveeCommandBuilder(): rgb"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            color {
            r value1.r
            g value1.g
            b value1.b
                
        }
            colorTemInKelvin 0}
    }
    def  command = cmd1.toString()
       if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
       else if (type=="status") {
           if (debugLog) {log.debug "GoveeCommandBuilder():status"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            }
    }
    def  command = cmd1.toString()
           if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
    else { 
        if (debugLog) {log.debug "GoveeCommandBuilder():other action"}
    JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            value value1}
        }
    def  command = cmd1.toString()
        if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command
}
}

def sendCommandLan(String cmd) {
  def addr = getIPString();
    if (debugLog) {log.debug ("sendCommandLan(): ${cmd}")}

  pkt = new hubitat.device.HubAction(cmd,
                     hubitat.device.Protocol.LAN,
                     [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                     ignoreResponse    : false,
                     callback: parse,
                     parseWarning: true,
                     destinationAddress: addr])  
  try {    
      if (debugLog) {log.debug("sendCommandLan(): ${pkt} to ip ${addr}")}
    sendHubCommand(pkt) 
      
  }
  catch (Exception e) {      
      logDebug e
  }      
}

def getIPString() {
   return ip+":"+commandPort()
}


def parse(message) {  
  log.error "Got something to parseUDP"
  log.error "UDP Response -> ${message}"    
}



def  setEffect(effectNo) {
    effectNumber = effectNo.toString()
    if ((parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}" != null) && (parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}".containsKey(effectNumber))) {
        String sceneInfo =  parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}".get(effectNumber).name
        String sceneCmd =  parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}".get(effectNumber).cmd
        if (debugLog) {log.debug ("setEffect(): Activate effect number ${effectNo} called ${sceneInfo} with command ${sceneCmd}")}
        sendEvent(name: "effectName", value: sceneInfo)
        sendEvent(name: "effectNum", value: effectNumber)
        sendEvent(name: "switch", value: "on")
        String cmd2 = '{"msg":{"cmd":"ptReal","data":{"command":'+sceneCmd+'}}}'
        if (debugLog) {log.debug ("setEffect(): command to be sent to ${cmd2}")}
        sendCommandLan(cmd2)
   } else {

    }     
}

def setNextEffect() {
if (debugLog) {log.debug ("setNextEffect(): Current Color mode ${device.currentValue("colorMode")}")}
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) {log.debug ("setNextEffect(): current effectNum ${device.currentValue("effectNum")}")}
    if (device.currentValue("effectNum") == "0" || device.currentValue("effectNum") == device.getDataValue("maxScene")) {
        setEffect(101)
    } else {
        if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
        int nextEffect = device.currentValue("effectNum").toInteger()+1
        setEffect(nextEffect)
        }  
}
      
def setPreviousEffect() {
if (debugLog) {log.debug ("setNextEffect(): Current Color mode ${device.currentValue("colorMode")}")}
    unschedule(fadeUp)
    unschedule(fadeDown)
      if (device.currentValue("effectNum") == "0" || device.currentValue("effectNum") == "101") {
        setEffect(device.getDataValue("maxScene"))
        } else {
            if (debugLog) {log.debug ("setNextEffect(): Increment to next scene}")}
            int prevEffect = device.currentValue("effectNum").toInteger()-1
            setEffect(prevEffect)
        }  
}


def activateDIY(diyActivate) {
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
}

def retrieveScenes() {
    state.scenes = [] as List
    state.diyEffects = [] as List
    if (debugLog) {log.debug ("retrieveScenes(): Retrieving Scenes from parent app")}
    if (debugLog) {log.debug ("retrieveScenes(): DIY Keyset ${parent.state.diyEffects.keySet()}")}
    if (parent.state.diyEffects.containsKey((device.getDataValue("deviceModel"))) == false) {
        if (debugLog) {log.debug ("retrieveScenes(): No DIY Scenes to retrieve for device")}    
    } else {
        parent.state.diyEffects.(device.getDataValue("deviceModel")).each {
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
            String sceneValue = it.getKey() + "=" + it.value.name
            state.diyEffects.add(sceneValue)
            state.diyEffects = state.diyEffects.sort()
        }
    }
    if ( parent.atomicState."${"lightEffect_"+(device.getDataValue("DevType"))}" == null ) {
        if (debugLog) {log.debug ("retrieveScenes(): No Scenes to retrieve for device")}    
    } else { 
        parent.atomicState."${"lightEffect_"+(device.getDataValue("DevType"))}".each {
            if (it.getKey() == "999") {
                if (debugLog) {log.debug ("retrieveScenes(): Processing max scene value ${it.getKey()} of ${it.value.maxScene}")}
                device.updateDataValue("maxScene", it.value.maxScene.toString())
            } else {
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
            String sceneValue = it.getKey() + "=" + it.value.name
            state.scenes.add(sceneValue)
            state.scenes = state.scenes.sort()
            }
        }
    }
}

def getDevType() {
    switch(device.getDataValue("deviceModel")) {
        case "H6091":
        case "H6092":
            if (debugLog) {log.debug ("getDevType(): Found   ${device.getDataValue("deviceModel")} setting DevType to Galaxy_Projector")}; 
            device.updateDataValue("DevType", "Galaxy_Projector");
            break;         
        default: 
            if (debugLog) {log.debug ("getDevType(): Unknown device Type  ${device.getDataValue("deviceModel")}")}; 
        break; 
        
    }       
}
