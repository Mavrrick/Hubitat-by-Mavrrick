library (
 author: "Mavrrick",
 category: "Govee",
 description: "GoveeCloudAPI",
 name: "Govee_Cloud_API",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

//////////////////////////////
// Standard device Commands //
//////////////////////////////

def cloudOn() {
         if (device.currentValue("cloudAPI") == "Retry") {
             log.error "on(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 1 ,"devices.capabilities.on_off")
            }
}

def cloudOff() {
        if (device.currentValue("cloudAPI") == "Retry") {
             log.error "off(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 0 ,"devices.capabilities.on_off")
            }
} 

def cloudCT(value, level, transitionTime){
       if (device.currentValue("cloudAPI") == "Retry") {
            log.error "setColorTemperature(): CloudAPI already in retry state. Aborting call." 
         } else {  
           	int intvalue = value.toInteger()
            sendEvent(name: "cloudAPI", value: "Pending")
            if (level != null) setLevel(level, transitionTime);
		    sendCommand("colorTemperatureK", intvalue,"devices.capabilities.color_setting")
       }
}



/////////////////////////////////////////////////////
// Helper methods to retrieve data or send command //
/////////////////////////////////////////////////////

private def sendCommand(String command, payload2, type) {
     requestID = randomUUID()
//     if (debugLog) { log.debug "sendCommand(): ${requestID}"}
     String bodyParm = '{"requestId": "'+requestID+'", "payload": {"sku": "'+device.getDataValue("deviceModel")+'", "device": "'+device.getDataValue("deviceID")+'", "capability": {"type":  "'+type+'", "instance": "'+command+'", "value":'+payload2+'}}}'
     def params = [
            uri   : "https://openapi.api.govee.com",
            path  : '/router/api/v1/device/control',
			headers: ["Govee-API-Key": device.getDataValue("apiKey"), "Content-Type": "application/json"],
            contentType: "application/json",   
            body: bodyParm
            ]  
    if (debugLog) { log.debug "sendCommand(): ${command}, ${type}, ${params}"}
try {

			httpPost(params) { resp ->
				
                if (debugLog) { log.debug "sendCommand(): Response data is "+resp.data}
                code = resp.data.code
                if (code == 200 && command == "powerSwitch") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    if (payload2 == 1) {
                        sendEvent(name: "switch", value: "on")
                        if (descLog) { log.info "${device.label} was turned on"}
                    }
                    if (payload2 == 0) {
                        sendEvent(name: "switch", value: "off")
                        if (descLog) { log.info "${device.label} was turned off"}
                    }
                    } 
                else if (code == 200 && command == "brightness") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "level", value: payload2)
                    if (descLog) { log.info "${device.label} Level was set to ${payload2}"}
                    }
                else if (code == 200 && command == "colorTemperatureK") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "colorMode", value: "CT")
                    sendEvent(name: "colorTemperature", value: payload2)
                    setCTColorName(payload2)
                    if (descLog) log.info "${device.label} Color Temp was set to. ${payload2}"
                    }
                else if (code == 200 && command == "nightlightToggle") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    if (payload2 == 1) {
                        sendEvent(name: "switch", value: "on")
                        if (descLog) { log.info "${device.label} nightlight was turned on"}
                    }
                    if (payload2 == 0) {
                        sendEvent(name: "switch", value: "off")
                        if (descLog) { log.info "${device.label} nightlight was turned off"}
                    }
                    }
                else if (code == 200 && command == "nightlightScene") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "colorMode", value: "EFFECTS")
                    sendEvent(name: "effectNum", value: payload2)
                    sendEvent(name: "effectName", value: state.scenes."${payload2}")
                    if (descLog) { log.info "${device.label} nightlight scene was set to ${payload2}"}
                    }
                else if (code == 200 && command == "lightScene") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "colorMode", value: "EFFECTS")
                    sendEvent(name: "effectNum", value: payload2)
                    sendEvent(name: "effectName", value: state.scenes."${payload2}")
                    if (descLog) { log.info "${device.label} scene was set to ${payload2}"}
                    }
                else if (code == 200 && command == "diyScene") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "colorMode", value: "DIY_EFFECTS")
                    sendEvent(name: "effectNum", value: payload2)
                    sendEvent(name: "effectName", value: state.diyEffects."${payload2}")
                    if (descLog) { log.info "${device.label} DIY scene was set to ${payload2}"}
                   }
                else if (code == 200 && command == "snapshot") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "colorMode", value: "Snapshot_EFFECTS")
                    sendEvent(name: "effectNum", value: payload2)
                    sendEvent(name: "effectName", value: state.snapshot."${payload2}")
                    if (descLog) { log.info "${device.label} snapshot scene was set to ${payload2}"}
                   }
               else if (code == 200 && command == "airDeflectorToggle") {
                   sendEvent(name: "cloudAPI", value: "Success")
                   if (payload2 == 1) {
                       sendEvent(name: "airDeflector", value: "on")
                       if (descLog) { log.info "${device.label} Air Deflector was turned on"}
                   }
                   if (payload2 == 0) {
                       sendEvent(name: "airDeflector", value: "off")
                       if (descLog) { log.info "${device.label} Air Deflector was turned off"}
                   }
                   }
               else if (code == 200 && command == "oscillationToggle") {
                    sendEvent(name: "cloudAPI", value: "Success")
                   if (payload2 == 1) {
                       sendEvent(name: "oscillation", value: "on")
                       if (descLog) { log.info "${device.label} Oscillation was turned on"}
                   }
                   if (payload2 == 0) {
                       sendEvent(name: "oscillation", value: "off")
                       if (descLog) { log.info "${device.label} Oscillation was turned off"}
                   }
                   }
               else if (code == 200 && command == "workMode") {
                   def jsonSlurper = new JsonSlurper()
                   def payloadJson = jsonSlurper.parseText(payload2)
                   sendEvent(name: "cloudAPI", value: "Success")
                   sendEvent(name: "switch", value: "on")
                   sendEvent(name: "mode", value: payloadJson.workMode)
                   if (payloadJson.modeValue == 0) {
                       sendEvent(name: "modeValue", value: "N/A")
                   } else {
                       sendEvent(name: "modeValue", value: payloadJson.modeValue)
                   }
                   setModeDescription(payloadJson.workMode)
                   if (descLog) { log.info "${device.label} workMode was set to ${payload2}"}
                   }
               else if (code == 200 && command == "sliderTemperature") {
                   def jsonSlurper = new JsonSlurper()
                   def payloadJson = jsonSlurper.parseText(payload2)
                   log.debug "${device.label} payloadJson= ${payloadJson}"
                   sendEvent(name: "cloudAPI", value: "Success")
                   def units = payloadJson?.unit.charAt(0)
                   sendEvent(name: "switch", value: "on")
                   sendEvent(name: "targetTemp", value: payloadJson?.temperature, unit: units)
                   sendEvent(name: "targetTempUnit", value: units)
                   if (descLog) { log.info "${device.label} TargetTemp was set to ${payloadJson?.temperature}°${units}"}
                   }
               else if (code == 200 && command == "targetTemperature") {
                   def jsonSlurper = new JsonSlurper()
                   def payloadJson = jsonSlurper.parseText(payload2)
                   log.debug "${device.label} payloadJson= ${payloadJson}"
                   sendEvent(name: "cloudAPI", value: "Success")
                   def units = payloadJson?.unit.charAt(0)
                   sendEvent(name: "switch", value: "on")
                   sendEvent(name: "heatingSetpoint", value: payloadJson?.temperature, unit: units)                   
                   sendEvent(name: "targetTemp", value: payloadJson?.temperature, unit: units)
                   sendEvent(name: "targetTempUnit", value: units)
                   if (descLog) { log.info "${device.label} TargetTemp was set to ${payloadJson?.temperature}°${units}"}
                   }                 
               else if (code == 200 && command == "colorRgb") {
                    int r = (payload2 >> 16) & 0xFF;
                    int g = (payload2 >> 8) & 0xFF;
                    int b = payload2 & 0xFF;
		            HSVlst=hubitat.helper.ColorUtils.rgbToHSV([r,g,b])
				    hue=HSVlst[0].toInteger()
	   			    sat=HSVlst[1].toInteger()
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "colorMode", value: "RGB")
                    sendEvent(name: "colorRGBNum", value: payload2)
					sendEvent(name: "hue", value: hue)
					sendEvent(name: "saturation", value: sat)
                    if (descLog) { log.info "${device.label} Color was set to ${payload2}"}
                    }
  /*              resp.headers.each {
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
            }   */
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
//            pauseExecution(60000)
//            sendCommand(command, payload2, type)
        }    
		return 'unknown'
	}
}

def getDeviceState(){
    requestID = randomUUID()
//    if (debugLog) { log.debug "getDeviceState(): ${requestID}"}
    String bodyParm = '{"requestId": "'+requestID+'", "payload": {"sku": "'+device.getDataValue("deviceModel")+'", "device": "'+device.getDataValue("deviceID")+'"}}'    
        def params = [
			uri   : "https://openapi.api.govee.com",
			path  : '/router/api/v1/device/state',
			headers: ["Govee-API-Key": device.getDataValue("apiKey"), "Content-Type": "application/json"],
            body: bodyParm
        ]
    


try {

			httpPost(params) { resp ->

                if (debugLog) { log.debug "getDeviceState():"+resp.data.payload.capabilities}
                resp.data.payload.capabilities.each {
//                    type = it.type
//                    instance = it.instance
                    if (it.state.value || it.state.value == 0) {
                    if (debugLog) { log.debug "getDeviceState(): ${it.type} ${it.instance} ${it.state.value}" } 
                        switch(it.type) {
                            case "devices.capabilities.online":
                                if (it.instance == "online") sendEvent(name: "online", value: it.state.value);
                            break;
                            case "devices.capabilities.on_off":
                                if (it.instance == "powerSwitch") {
                                    if (it.state.value == 0)  sendEvent(name: "switch", value: "off");
                                    if (it.state.value == 1)  sendEvent(name: "switch", value: "on");
                                }
                            break;
                            case "devices.capabilities.range":
                                if (it.instance == "brightness") {
                                    if (getChildDevices().size() > 0) {
                                        child = getChildDevices().get(0);
                                        child.postEvent("level" , it.state.value);
                                } else {
                                    sendEvent(name: "level", value: it.state.value);                                         
                                    }
                                }
                            break;
                            case "devices.capabilities.color_setting":                                 
                                if (it.instance == "colorRgb") {
                                    if (getChildDevices().size() > 0) {
                                        child = getChildDevices().get(0)                                    
                                        if (debugLog) {log.debug ("getDeviceState(): current child device value is ${child.currentValue("colorRGBNum")}") }  
                                        if (child.currentValue("colorRGBNum") != it.state.value) {
                                            if (it.state.value >= 1) {
                                                child = getChildDevices().get(0)
                                                int r = (it.state.value >> 16) & 0xFF;
                                                int g = (it.state.value >> 8) & 0xFF;
                                                int b = it.state.value & 0xFF;
                                                HSVlst=hubitat.helper.ColorUtils.rgbToHSV([r,g,b]);
					                            hue=HSVlst[0].toInteger();
					                            sat=HSVlst[1].toInteger();
                                                if (debugLog) {log.debug ("getDeviceState(): child = ${child} Hue = ${hue} Saturation = ${sat} ColorRGBNum = ${it.state.value})}") }
					                            child.postEvent("hue" , hue);
					                            child.postEvent("saturation", sat);
                                               child.postEvent("colorRGBNum", it.state.value);
                                            } else {
                                                child.postEvent("colorRGBNum", it.state.value);    
                                            }
                                        } else {
                                            if (debugLog) {log.debug ("getDeviceState(): ColorRGBNum of ${it.state.value} did not change ignoring") }  
                                        } 
                                    
                                    } else {
                                        if (device.currentValue("colorRGBNum") != it.state.value) {
                                            if (it.state.value >= 1) {
                                                int r = (it.state.value >> 16) & 0xFF;
                                                int g = (it.state.value >> 8) & 0xFF;
                                                int b = it.state.value & 0xFF;
                                                HSVlst=hubitat.helper.ColorUtils.rgbToHSV([r,g,b]);
					                            hue=HSVlst[0].toInteger();
					                            sat=HSVlst[1].toInteger();
					                            sendEvent(name: "hue", value: hue);
					                            sendEvent(name: "saturation", value: sat);
                                                sendEvent(name: "colorRGBNum", value: it.state.value);
                                            } else {
                                                sendEvent(name: "colorRGBNum", value: it.state.value);    
                                            }
                                        } else {
                                            if (debugLog) {log.debug ("getDeviceState(): ColorRGBNum of ${it.state.value} did not change ignoring") }  
                                        }
                                    }
                                } 
                                if (it.instance == "colorTemperatureK") {
                                    if (device.currentValue("colorTemperature") != it.state.value) {
                                        if (it.state.value >= 1) {
                                            sendEvent(name: "colorTemperature", value: it.state.value);
                                        }
                                        else {
                                            sendEvent(name: "colorTemperature", value: it.state.value);                                    
                                        } 
                                    } else {
                                        if (debugLog) { log.debug ("getDeviceState(): CT of ${it.state.value} did not change ignoring") }   
                                    }   
                                }
                            break;
                            case "devices.capabilities.dynamic_scene":
                                if (it.instance == "lightScene") sendEvent(name: "effectName", value: it.state.value);
                            break;
                            case "devices.capabilities.music_setting":
                                if (it.instance == "musicMode") sendEvent(name: "effectName", value: it.state.value);
                            break;
                            case "devices.capabilities.toggle":
                                if (it.state.value == 0) toggle = "off";
                                if (it.state.value == 1) toggle = "on";
                                if (it.instance == "gradientToggle") sendEvent(name: "gradient", value: toggle);
                                if (it.instance == "nightlightToggle") { 
                                    if (getChildDevices().size() > 0) {
                                        child = getChildDevices().get(0);
                                        if (it.state.value == 0)  value = "off";
                                        if (it.state.value == 1)  value = "on";  
                                        child.postEvent("switch" , value);
                                    } else {
                                        if (it.state.value == 0)  sendEvent(name: "switch", value: "off");
                                        if (it.state.value == 1)  sendEvent(name: "switch", value: "on");                                         
                                        }
                                    }
                                if (it.instance == "airDeflectorToggle") sendEvent(name: "airDeflector", value: toggle);
                                if (it.instance == "oscillationToggle") sendEvent(name: "oscillation", value: toggle);
                                if (it.instance == "thermostatToggle") sendEvent(name: "thermostat", value: toggle);
                                if (it.instance == "warmMistToggle") sendEvent(name: "warmMistT", value: toggle);
                            break; 
                            case "devices.capabilities.segment_color_setting":
                                if (it.instance == "segmentedBrightness") sendEvent(name: "segmentedBrightness", value: it.state.value);
                                if (it.instance == "segmentedColorRgb") sendEvent(name: "segmentedColorRgb", value: it.state.value);
                            break;
                            case "devices.capabilities.mode":
                                if (it.instance == "nightlightScene") sendEvent(name: "effectName", value: it.state.value);
                                if (it.instance == "presetScene") sendEvent(name: "presetScene", value: it.state.value);
                            break;
                            case "devices.capabilities.temperature_setting":
                               switch (it.instance) {
                                   case "targetTemperature":
                                       sendEvent(name: "targetTemp", value: it.state.value.targetTemperature, unit: getTemperatureScale() );
                                       break;
                                   case "sliderTemperature":
                                       sendEvent(name: "targetTemp", value: it.state?.value?.targetTemperature, unit: getTemperatureScale() )
                                       sendEvent(name: "tempSetPoint", value: it.state?.value);
                                       break;
                                   default:
                                       break;
                               }
                            break;
                            case "devices.capabilities.property":
                                if (it.instance == "sensorTemperature" && getTemperatureScale() == "C") sendEvent(name: "temperature", value: fahrenheitToCelsius(it.state.value.toDouble().round(2)), unit: "C");
                                if (it.instance == "sensorTemperature" && getTemperatureScale() == "F") sendEvent(name: "temperature", value: it.state.value.toDouble().round(2), unit: "F");
                                if (it.instance == "sensorHumidity") sendEvent(name: "humidity", value: it.state.value, unit: "%");
                                if (it.instance == "airQuality") sendEvent(name: "airQuality", value: it.state.value);
                                if (it.instance == "filterLifeTime") sendEvent(name: "filterLifeTime", value: it.state.value, unit: "%");                            
                            break;  
                            case "devices.capabilities.work_mode":
                                if (it.instance == "workMode") {
                                    sendEvent(name: "mode", value: it.state.value.workMode)
                                    if ( it.state.value.modeValue == null || it.state.value.modeValue == 0 ) {
                                        if (debugLog) {log.debug ("getDeviceState(): workmode value of Null or 0 found}")}
                                        sendEvent(name: "modeValue", value: "N/A")
                                    } else {
                                        sendEvent(name: "modeValue", value: it.state.value.modeValue)
                                        if (debugLog) {log.debug ("getDeviceState(): workmode value is greater then 0}")}
                                    }
                                    };
//                                if (instance == "workMode") sendEvent(name: "modeValue", value: it.state.value.modeValue);
                                setModeDescription(it.state.value.workMode);
                            break;                            
                        default: 
                        if (debugLog) {log.debug ("getDeviceState(): Unknown command type}")}; 
                         break;         
                    }
                    } else {
                        if (debugLog) { log.debug "getDeviceState(): Instance: ${it.instance} value is empty. Skipping" }
                    }
                }

/*                resp.headers.each {
                    if (debugLog) { log.debug "getDeviceState(): ${it.name}: ${it.value}"}                    
                    name = it.name
                    value = it.value
                    if (name == "X-RateLimit-Remaining") {
                        state.DailyLimitRemaining = value
                        parent.apiRateLimits("DailyLimitRemaining", value)
                    }
                    if (name == "API-RateLimit-Remaining") {
                        state.MinRateLimitRemainig = value
                        parent.apiRateLimits("MinRateLimitRemainig", value)
                    }
                }	*/							
				
                if (device.currentValue("cloudAPI") == "Retry") {
                    if (debugLog) {log.error "getDeviceState(): Cloud API in retry state. Reseting "}
                    sendEvent(name: "cloudAPI", value: "Success")
                    }				
				return resp.data
			}
			
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
		return 'unknown'
	}
}


def retrieveScenes2(){
    requestID = randomUUID()
    if (debugLog) { log.debug "getDeviceState(): ${requestID}"}
        String bodyParm = '{"requestId": "'+requestID+'", "payload": {"sku": "'+device.getDataValue("deviceModel")+'", "device": "'+device.getDataValue("deviceID")+'"}}'    
		def params = [
			uri   : "https://openapi.api.govee.com",
			path  : '/router/api/v1/device/scenes',
			headers: ["Govee-API-Key": device.getDataValue("apiKey"), "Content-Type": "application/json"],
            body: bodyParm
        ]
    
try {

			httpPost(params) { resp ->

                if (debugLog) { log.debug "retrieveScenes2():"+resp.data.payload.capabilities}
                state.scenes = [:]
                resp.data.payload.capabilities.parameters.options.get(0).each {
                    state.scenes.put(it.value.id,it.name)  
                }
                state.sceneMax = state.scenes.size()
                state.sceneValue = 0 
                if (debugLog) { log.debug "retrieveScenes2(): dynamic scenes loaded"}
/*                resp.headers.each {
                    if (debugLog) { log.debug "getDeviceState(): ${it.name}: ${it.value}"}                    
                    name = it.name
                    value = it.value
                    if (name == "X-RateLimit-Remaining") {
                        state.DailyLimitRemaining = value
                        parent.apiRateLimits("DailyLimitRemaining", value)
                    }
                    if (name == "API-RateLimit-Remaining") {
                        state.MinRateLimitRemainig = value
                        parent.apiRateLimits("MinRateLimitRemainig", value)
                    }
                }	*/							
				
                if (device.currentValue("cloudAPI") == "Retry") {
                    if (debugLog) {log.error "retrieveScenes2(): Cloud API in retry state. Reseting "}
                    sendEvent(name: "cloudAPI", value: "Success")
                    }				
				return resp.data
			}
			
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
		return 'unknown'
	}
}

def retrieveDIYScenes(){
    requestID = randomUUID()
    if (debugLog) { log.debug "retrieveDIYScenes(): ${requestID}"}
        String bodyParm = '{"requestId": "'+requestID+'", "payload": {"sku": "'+device.getDataValue("deviceModel")+'", "device": "'+device.getDataValue("deviceID")+'"}}'    
		def params = [
			uri   : "https://openapi.api.govee.com",
			path  : '/router/api/v1/device/diy-scenes',
			headers: ["Govee-API-Key": device.getDataValue("apiKey"), "Content-Type": "application/json"],
            body: bodyParm
        ]
    
try {

			httpPost(params) { resp ->

                if (debugLog) { log.debug "retrieveDIYScenes():"+resp.data.payload.capabilities}
                state.remove("diyEffects")
                state.remove("diyScene") //Needs to be removed at future date
                state.remove("diySceneOptions") // needs to be removed at future date
                state.diyEffects = [:]
                resp.data.payload.capabilities.parameters.options.get(0).each {
                    state.diyEffects.put(it.value,it.name)
                } 

                if (debugLog) { log.debug "retrieveDIYScenes(): dynamic scenes loaded"}
/*                resp.headers.each {
                    if (debugLog) { log.debug "retrieveDIYScenes(): ${it.name}: ${it.value}"}                    
                    name = it.name
                    value = it.value
                    if (name == "X-RateLimit-Remaining") {
                        state.DailyLimitRemaining = value
                        parent.apiRateLimits("DailyLimitRemaining", value)
                    }
                    if (name == "API-RateLimit-Remaining") {
                        state.MinRateLimitRemainig = value
                        parent.apiRateLimits("MinRateLimitRemainig", value)
                    }
                }	*/							
				
                if (device.currentValue("cloudAPI") == "Retry") {
                    if (debugLog) {log.error "retrieveDIYScenes(): Cloud API in retry state. Reseting "}
                    sendEvent(name: "cloudAPI", value: "Success")
                    }				
				return resp.data
			}
			
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
		return 'unknown'
	}
}

def retrieveStateData(){
    if (device.getDataValue("commands").contains("nightlightScene")) { retrieveDynamicScene("nightlightScene") }
    if (device.getDataValue("commands").contains("diyScene")) { retrieveDynamicScene("diyScene") }
    if (device.getDataValue("commands").contains("workMode")) { retrieveCmdParms("workMode") }
    if (device.getDataValue("commands").contains("targetTemperature")) { retrieveCmdParms("targetTemperature") } 
    if (device.getDataValue("commands").contains("segmentedBrightness")) { retrieveCmdParms("segmentedBrightness") }
    if (device.getDataValue("commands").contains("segmentedColorRgb")) { retrieveCmdParms("segmentedColorRgb") }
    if (device.getDataValue("commands").contains("musicMode")) { retrieveCmdParms("musicMode") }
    if (device.getDataValue("commands").contains("snapshot")) { retrieveSnapshot() }
    if (device.getDataValue("commands").contains("presetScene")) { retrieveDynamicScene("presetScene") }    
}

void retrieveSnapshot() {
    if (debugLog) { log.debug "retrieveSnapshot(): Retrieving Snapshots for device"}
    def device = device.getDataValue("deviceID")
    def goveeAppAPI = parent.retrieveGoveeAPI(device)
    state.snapshot = [:]
    goveeAppAPI.capabilities.each{
        if (debugLog) { log.debug "retrieveSnapshot(): found ${it.get("instance")}"}
        if (it.get("instance") == "snapshot") {
            if (debugLog) { log.debug "retrieveSnapshot(): Snapshot instance"}
            if (debugLog) { log.debug "retrieveSnapshot(): Adding ${it.parameters.options} to state value" }
            it.parameters.options.each {
                if (debugLog) { log.debug "retrieveSnapshot(): Adding ${it.name} = ${it.value} to state value" }
                state.snapshot.put(it.value,it.name)
            }
        }
    }
}


def retrieveDynamicScene(type){
    def device = device.getDataValue("deviceID")
    def goveeAppAPI = parent.retrieveGoveeAPI(device)
    state."${type}" = []
//    parent.goveeAppAPI.each {
//        if (it.device == device.getDataValue("deviceID")) {
            if (debugLog) { log.debug "retrieveDynamicScene(): found matching device"}
            goveeAppAPI.capabilities.each{
                if (debugLog) { log.debug "retrieveDynamicScene(): found ${it.get("instance")}"}
                if (it.get("instance") == type) {
                    if (debugLog) { log.debug "retrieveDynamicScene(): nightLightScene instance"}
                    if (debugLog) { log.debug "retrieveDynamicScene(): Adding ${it.parameters.options} to state value" }
                    state."${type}" = it.parameters.options

                }
 //           }
 //       }            
    }          
}

def retrieveCmdParms(type){
    def device = device.getDataValue("deviceID")
    def goveeAppAPI = parent.retrieveGoveeAPI(device)
    if (debugLog) { log.debug "retrieveCmdParms(): Getting command data2 for ${type} ${goveeAppAPI} ${device} ${goveeAppAPI.capabilities}"}
    state."${type}" = [] as List
//    goveeAppAPI.each {
//        if (it.device == device.getDataValue("deviceID")) {
            if (debugLog) { log.debug "retrieveCmdParms(): found matching device"}
//            it.capabilities.each{
             goveeAppAPI.capabilities.each{
//                if (debugLog) { log.debug "retrieveCmdParms(): found ${it.get("instance")}"}
                if (it.get("instance") == type) {
                    if (debugLog) { log.debug "retrieveCmdParms(): found ${it.get("instance")}"}
                    if (debugLog) { log.debug "retrieveCmdParms(): Adding ${it.parameters} to state value" }
                    state."${type}" = it.parameters.fields

                }
 //           }
 //       }            
    }          
}

void setModeDescription(mode=null) {
    mode = mode?:device.currentValue('mode',true)
    def WorkModeName = state.workMode.options[0].find{it.value==mode}?.name?:"Unknown"
    if (debugLog) {log.debug "setModeDescription(${mode}): WorkModeName=${WorkModeName}"}
    sendEvent(name: "modeDescription", value: WorkModeName )
}

def apiKeyUpdate() {
    if (parent.label == "Govee v2 Device Manager") {
        if (device.getDataValue("apiKey") != parent?.getDataValue("apiKey")) {
            if (debugLog) {log.debug "apiKeyUpdate(): Detected new API Key. Applying"}
            device.updateDataValue("apiKey", parent?.getDataValue("apiKey"))
        }
    }
    else if (device.getDataValue("apiKey") != parent?.APIKey) {
        if (debugLog) {log.debug "apiKeyUpdate(): Detected new API Key. Applying"}
        device.updateDataValue("apiKey", parent?.APIKey)
    }
}

def randomUUID(){
    String requestID = UUID.randomUUID().toString()
    if (debugLog) {log.debug "randomUUID(): random uuid is ${requestID}"}
    return requestID
}

def cloudInitDefaultValues() {
    if(device.getDataValue("commands").contains("color")) {
        sendEvent(name: "hue", value: 0)
        sendEvent(name: "saturation", value: 100)
    }
    if(device.getDataValue("commands").contains("lightScene")) {
        sendEvent(name: "effectNum", value: 0) 
    	sendEvent(name: "colorMode", value: "CT") 
    }
    if(device.getDataValue("commands").contains("brightness")) {
        sendEvent(name: "level", value: 0)  
    }
    if(device.getDataValue("commands").contains("colorTemperatureK")) {
        sendEvent(name: "colorTemperature", value: 2000)  
    }
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

void recState(){
    state.previousState = [:]
    if (device.currentValue("switch") == "on") {
        state.previousState.put("switch",device.currentValue("switch"))
        if (device.currentValue("colorMode") == "CT") {
            state.previousState.put("colorMode",device.currentValue("colorMode"))
            state.previousState.put("colorTemperature",device.currentValue("colorTemperature"))
            state.previousState.put("level",device.currentValue("level"))
        } else if (device.currentValue("colorMode") == "RGB") {
            state.previousState.put("colorMode",device.currentValue("colorMode"))
            state.previousState.put("level",device.currentValue("level"))
            state.previousState.put("hue",device.currentValue("hue"))
            state.previousState.put("saturation",device.currentValue("saturation"))     
        } else if (device.currentValue("colorMode") == "EFFECTS") {
            state.previousState.put("colorMode",device.currentValue("colorMode"))
            state.previousState.put("level",device.currentValue("level"))
            state.previousState.put("effectNum",device.currentValue("effectNum"))
        } else if (device.currentValue("colorMode") == "DIY_EFFECTS") {
            state.previousState.put("colorMode",device.currentValue("colorMode"))
            state.previousState.put("level",device.currentValue("level"))
            state.previousState.put("effectNum",device.currentValue("effectNum"))
        } else if (device.currentValue("colorMode") == "Snapshot_EFFECTS") {
            state.previousState.put("colorMode",device.currentValue("colorMode"))
            state.previousState.put("level",device.currentValue("level"))
            state.previousState.put("effectNum",device.currentValue("effectNum"))
        }       
    } else {
        state.previousState.put("switch",device.currentValue("switch"))
    }
}

void loadState(){
    if (state.previousState.switch == "on" ) {
        if (state.previousState.colorMode == "CT") {
            setColorTemperature(state.previousState.colorTemperature,state.previousState.level,transitionTime = null)
//            setLevel(float v,duration = 0)
        } else if (state.previousState.colorMode == "RGB") {
            setLevel(100,duration = 0)
            setHsb(state.previousState.hue,state.previousState.saturation,100)
        } else if (state.previousState.colorMode == "EFFECTS") {
            setLevel(state.previousState.level,duration = 0)
            setEffect(state.previousState.effectNum)
        } else if (state.previousState.colorMode == "DIY_EFFECTS") {
            setLevel(state.previousState.level,duration = 0)
            activateDIY(state.previousState.effectNum)
        } else if (state.previousState.colorMode == "Snapshot_EFFECTS") {
            setLevel(state.previousState.level,duration = 0)
            snapshot(state.previousState.effectNum)
        }
    }   else {
        off()
    }
}
