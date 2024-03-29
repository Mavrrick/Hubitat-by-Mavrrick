library (
 author: "Mavrrick",
 category: "Govee",
 description: "GoveeCloudAPI",
 name: "Govee_Cloud_API",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

// put methods, etc. here

private def sendCommand(String command, payload2, type) {
     randomUUID()
     if (debugLog) { log.debug "sendCommand(): ${requestID}"}
     bodyParm = '{"requestId": "'+requestID+'", "payload": {"sku": "'+device.getDataValue("deviceModel")+'", "device": "'+device.getDataValue("deviceID")+'", "capability": {"type":  "'+type+'", "instance": "'+command+'", "value":'+payload2+'}}}'
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
				
                if (debugLog) { log.debug "sendCommand(): response.data="+resp.data}
                code = resp.data.code
                if (code == 200 && command == "powerSwitch") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    if (payload2 == 1) sendEvent(name: "switch", value: "on")
                    if (payload2 == 0) sendEvent(name: "switch", value: "off")
                    } 
                else if (code == 200 && command == "brightness") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "level", value: payload2)
                    }
                else if (code == 200 && command == "colorTemperatureK") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    sendEvent(name: "colorMode", value: "CT")
                    sendEvent(name: "colorTemperature", value: payload2)
                    setCTColorName(payload2)
                    }
                else if (code == 200 && command == "nightlightToggle") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    if (payload2 == 1) sendEvent(name: "nightLight", value: "on")
                    if (payload2 == 0) sendEvent(name: "nightLight", value: "off")
                    }
                else if (code == 200 && command == "nightlightScene") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "effectName", value: payload2)
                    }
                else if (code == 200 && command == "lightScene") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "colorMode", value: "EFFECTS")
                    sendEvent(name: "effectNum", value: payload2)
                    sendEvent(name: "effectName", value: state.scenes."${payload2}")
                    }
                else if (code == 200 && command == "diyScene") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "colorMode", value: "EFFECTS")
                    sendEvent(name: "effectNum", value: payload2)
                    sendEvent(name: "effectName", value: state.diySceneOptions ."${payload2}")
                    }                
               else if (code == 200 && command == "airDeflectorToggle") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    if (payload2 == 1) sendEvent(name: "airDeflector", value: "on")
                    if (payload2 == 0) sendEvent(name: "airDeflector", value: "off")
                    }
               else if (code == 200 && command == "workMode") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "mode ", value: payload2)
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
//            pauseExecution(60000)
//            sendCommand(command, payload2, type)
        }    
		return 'unknown'
	}
}

def getDeviceState(){
    randomUUID()
    if (debugLog) { log.debug "getDeviceState(): ${requestID}"}
    bodyParm = '{"requestId": "'+requestID+'", "payload": {"sku": "'+device.getDataValue("deviceModel")+'", "device": "'+device.getDataValue("deviceID")+'"}}'    
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
                    type = it.type
                    instance = it.instance
                    if (it.state.value || it.state.value == 0) {
                    if (debugLog) { log.debug "getDeviceState(): ${type} ${instance} ${it.state.value}" }
                        switch(type) {
                            case "devices.capabilities.online":
                                if (instance == "online") sendEvent(name: "online", value: it.state.value);
                            break;
                            case "devices.capabilities.on_off":
                            if (instance == "powerSwitch") {
                                if (it.state.value == 0)  sendEvent(name: "switch", value: "off");
                                if (it.state.value == 1)  sendEvent(name: "switch", value: "on");
                                }
                            break;
                            case "devices.capabilities.range":
                                if (instance == "brightness") sendEvent(name: "level", value: it.state.value);
                            break;
                            case "devices.capabilities.color_setting":
                                if (instance == "colorRgb"){
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
					                        sendEvent(name: "colorMode", value: "RGB");
                                            sendEvent(name: "colorRGBNum", value: it.state.value);
                                        } else {
                                            sendEvent(name: "colorRGBNum", value: it.state.value);    
                                        }
                                    } else {
                                        if (debugLog) {log.debug ("getDeviceState(): ColorRGBNum of ${it.state.value} did not change ignoring") }  
                                    }
                                }
                                if (instance == "colorTemperatureK") {
                                    if (device.currentValue("colorTemperature") != it.state.value) {
                                        if (it.state.value >= 1) {
                                            sendEvent(name: "colorTemperature", value: it.state.value);
                                            sendEvent(name: "colorMode", value: "CT");
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
                                if (instance == "lightScene") sendEvent(name: "effectName", value: it.state.value);
                            break;
                            case "devices.capabilities.music_setting":
                                if (instance == "musicMode") sendEvent(name: "effectName", value: it.state.value);
                            break;
                            case "devices.capabilities.toggle":
                                if (it.state.value == 0) toggle = "off";
                                if (it.state.value == 1) toggle = "on";
                                if (instance == "gradientToggle") sendEvent(name: "gradient", value: toggle);
                                if (instance == "nightlightToggle") sendEvent(name: "nightLight", value: toggle);
                                if (instance == "airDeflectorToggle") sendEvent(name: "airDeflector", value: toggle);
                                if (instance == "oscillationToggle") sendEvent(name: "oscillation", value: toggle);
                                if (instance == "thermostatToggle") sendEvent(name: "thermostat", value: toggle);
                                if (instance == "warmMistToggle") sendEvent(name: "warmMistT", value: toggle);
                            break; 
                            case "devices.capabilities.segment_color_setting":
                                if (instance == "segmentedBrightness") sendEvent(name: "segmentedBrightness", value: it.state.value);
                                if (instance == "segmentedColorRgb") sendEvent(name: "segmentedColorRgb", value: it.state.value);
                            break;
                            case "devices.capabilities.mode":
                                if (instance == "nightlightScene") sendEvent(name: "effectName", value: it.state.value);
                                if (instance == "presetScene") sendEvent(name: "presetScene", value: it.state.value);
                            break;
                            case "devices.capabilities.temperature_setting":
                                if (instance == "targetTemperature" && getTemperatureScale() == "C") sendEvent(name: "targetTemp", value: it.state.value.targetTemperature, unit: "C");
                                if (instance == "targetTemperature" && getTemperatureScale() == "F") sendEvent(name: "targetTemp", value: celsiusToFahrenheit(it.state.value.targetTemperature), unit: "F");
                                if (instance == "sliderTemperature") sendEvent(name: "tempSetPoint", value: it.state.value);
                            break;
                            case "devices.capabilities.property":
                                if (instance == "sensorTemperature" && getTemperatureScale() == "C") sendEvent(name: "temperature", value: fahrenheitToCelsius(it.state.value), unit: "C");
                                if (instance == "sensorTemperature" && getTemperatureScale() == "F") sendEvent(name: "temperature", value: it.state.value, unit: "F");
                                if (instance == "sensorHumidity") sendEvent(name: "humidity", value: it.state.value, unit: "%");
                            break;  
                            case "devices.capabilities.work_mode":
                                if (instance == "workMode") sendEvent(name: "mode", value: it.state.value);
                            break;                            
                        default: 
                        if (debugLog) {log.debug ("getDeviceState(): Unknown command type}")}; 
                         break;         
                    }
                    } else {
                        if (debugLog) { log.debug "getDeviceState(): Instance: ${instance} value is empty. Skipping" }
                    }
                }

                resp.headers.each {
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
                }								
				
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

def getDeviceTempHumid(){
    randomUUID()
    if (debugLog) { log.debug "getDeviceState(): ${requestID}"}
    bodyParm = '{"requestId": "'+requestID+'", "payload": {"sku": "'+device.getDataValue("deviceModel")+'", "device": "'+device.getDataValue("deviceID")+'"}}'    
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
                    type = it.type
                    instance = it.instance
//                    state = it.state
                    if (debugLog) { log.debug "getDeviceState(): ${type} ${instance} ${it.state.value}" }
                        switch(type) {
                            case "devices.capabilities.property":
                            if (instance == "sensorTemperature" && getTemperatureScale() == "C") sendEvent(name: "temperature", value: ((it.state.value.toInteger()/100)).toDouble().round(1), unit: "C"); 
                            if (instance == "sensorTemperature" && getTemperatureScale() == "F") sendEvent(name: "temperature", value: (((it.state.value.toInteger()/100)*(9/5))+32).toDouble().round(1), unit: "F");
                            if (instance == "sensorHumidity") sendEvent(name: "humidity", value: (it.state.value.currentHumidity.toInteger()/100).toDouble().round(1), unit: "%");
                            break;  
                        default: 
                        if (debugLog) {log.debug ("getDeviceState(): Unknown command type}")}; 
                         break;         
                }
                }

                resp.headers.each {
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
                }								
				
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
    randomUUID()
    if (debugLog) { log.debug "getDeviceState(): ${requestID}"}
     bodyParm = '{"requestId": "'+requestID+'", "payload": {"sku": "'+device.getDataValue("deviceModel")+'", "device": "'+device.getDataValue("deviceID")+'"}}'    
		def params = [
			uri   : "https://openapi.api.govee.com",
			path  : '/router/api/v1/device/scenes',
			headers: ["Govee-API-Key": device.getDataValue("apiKey"), "Content-Type": "application/json"],
            body: bodyParm
        ]
    
try {

			httpPost(params) { resp ->

                if (debugLog) { log.debug "retrieveScenes2():"+resp.data.payload.capabilities}
//                state.scenes = resp.data.payload.capabilities.parameters.options.get(0)
                state.remove("sceneOptions")
                state.scenes = [:]
                resp.data.payload.capabilities.parameters.options.get(0).each {
//                    String sceneName = '"'+it.name+'"'
//                    String sceneNum = '"'+it.value.id+'"'
 //                   sceneOptions = []
 //                   sceneOptions.add(sceneName+'='+sceneNum)
                    state.scenes.put(it.value.id,it.name)  
                } 
                if (debugLog) { log.debug "retrieveScenes2(): dynamic scenes loaded"}
                resp.headers.each {
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
                }								
				
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
//    state.scenes = [] as List
    randomUUID()
    if (debugLog) { log.debug "retrieveDIYScenes(): ${requestID}"}
     bodyParm = '{"requestId": "'+requestID+'", "payload": {"sku": "'+device.getDataValue("deviceModel")+'", "device": "'+device.getDataValue("deviceID")+'"}}'    
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
                state.remove("diyScene")                
                state.diySceneOptions = [:]
                resp.data.payload.capabilities.parameters.options.get(0).each {
                    state.diySceneOptions.put(it.value,it.name)  
                } 

                if (debugLog) { log.debug "retrieveDIYScenes(): dynamic scenes loaded"}
                resp.headers.each {
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
                }								
				
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
    if (device.getDataValue("commands").contains("snapshot")) { retrieveDynamicScene("snapshot") }
    if (device.getDataValue("commands").contains("presetScene")) { retrieveDynamicScene("presetScene") }    
}

def retrieveDynamicScene(type){
    state."${type}" = [] as List
    parent.state.goveeAppAPI.each {
        if (it.device == device.getDataValue("deviceID")) {
            if (debugLog) { log.debug "retrieveDynamicScene(): found matching device"}
            it.capabilities.each{
                if (debugLog) { log.debug "retrieveDynamicScene(): found ${it.get("instance")}"}
                if (it.get("instance") == type) {
                    if (debugLog) { log.debug "retrieveDynamicScene(): nightLightScene instance"}
                    if (debugLog) { log.debug "retrieveDynamicScene(): Adding ${it.parameters.options} to state value" }
                    state."${type}" = it.parameters.options

                }
            }
        }            
    }          
}

def retrieveCmdParms(type){
    if (debugLog) { log.debug "retrieveCmdParms(): Getting command data for ${type}"}
    state."${type}" = [] as List
    parent.state.goveeAppAPI.each {
        if (it.device == device.getDataValue("deviceID")) {
            if (debugLog) { log.debug "retrieveCmdParms(): found matching device"}
            it.capabilities.each{
//                if (debugLog) { log.debug "retrieveCmdParms(): found ${it.get("instance")}"}
                if (it.get("instance") == type) {
                    if (debugLog) { log.debug "retrieveCmdParms(): found ${it.get("instance")}"}
                    if (debugLog) { log.debug "retrieveCmdParms(): Adding ${it.parameters} to state value" }
                    state."${type}" = it.parameters.fields

                }
            }
        }            
    }          
}

def apiKeyUpdate() {
    if (device.getDataValue("apiKey") != parent?.APIKey) {
        if (debugLog) {log.warn "apiKeyUpdate(): Detected new API Key. Applying"}
        device.updateDataValue("apiKey", parent?.APIKey)
    }
}

def randomUUID(){
    requestID = UUID.randomUUID().toString()
    if (debugLog) {log.warn "randomUUID(): random uuid is ${requestID}"}
    return requestID
}
