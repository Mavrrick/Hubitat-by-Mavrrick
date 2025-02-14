/**
 *  Air Gradient Universal Driver with Polling
 *
 *  Copyright 2016 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import groovy.json.JsonSlurper
metadata
{
    definition (name: "Air Gradient Universal Driver", namespace: "mavrrick", author: "Mavrrick")
    {
		capability "TemperatureMeasurement"
		capability "RelativeHumidityMeasurement"
        capability "CarbonDioxideMeasurement"
        capability "Polling"
        capability "Initialize"
        capability "Configuration"
        
        attribute "rssi", "number"
        attribute "pm01", "number"		
		attribute "pm25", "number"
        attribute "pm10", "number"
        attribute "pm003Count", "number"
        attribute "tvoc", "number"
        attribute "nox", "number"
        
        command "ledBarTest"
        command "co2CalibrateRequest"
                          
	}

	preferences
	{	
		section
		{
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)
            input("ip", "text", title: "IP Address", description: "IP address of your Air Gradient Device", required: false)
			input(name: "cOrF", type: "enum", title: "Select your Temperature Unit", options: ["Celsius", "Fahrenheit"], defaultValue: "Celsius")		
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
            input("displayBright", "number", title: "Level of brightness for onboard display\nDefault:100", defaultValue:100, range: '0..100', width:3, required: false)
            input("ledBarBrightness", "number", title: "Level of brightness of LED Bar\nDefault:100", defaultValue:100, range: '0..100',width:3, required: false)
            input("noxLearnOffset", "number", title: "Set NOx learning gain offset\nDefault:12", defaultValue:12, range: '1..720', width:3, required: false)
            input("tvocLearnOffset", "number", title: "Set VOC learning gain offset.\nDefault:12", defaultValue:12, range: '1..720',width:3, required: false)
            input "co2Baseline", "number", title: "Days use for CO2 Baseline", defaultValue: 8, range: '7..200', required: false		
            input(name: "postDataToAirGradient", type: "bool", title: "Post data to AirGradient Cloud", defaultValue: true)
            input(name: "configControl", type: "enum", title: "What sources can control the Config", options: ["both", "local", "cloud"], defaultValue: "both")
            input(name: "ledBarMode", type: "enum", title: "Mode LED bar can be set to", options: ["co2", "pm", "off"], defaultValue: "co2")
            input(name: "pmStandard", type: "enum", title: "Mode LED bar can be set to", options: ["ugm3", "us-aqi"], defaultValue: "ugm3")
		}
	}
}


def installed()
{
	log.info "Air Gradient is loaded. Waiting for updates"
	settingsInitialize()
}


def updated()
{
	log.info "Air Gradient is loaded. Waiting for updates"
	settingsInitialize()
}


def settingsInitialize()
{
	if (enableDebug)
	{
		log.info "Verbose logging has been enabled for the next 30 minutes."
		runIn(1800, logsOff)
	}
}

//	runs when HUB boots, starting the device refresh cycle, if any
void initialize()
	{
	log.info "Air Gradient Driver is initializing"
    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        runIn(pollRateInt,poll)
    }        
}

void configure(){
    if (debugLog) {log.debug "configure(): updating configuration of device with preferences"}
    curConfig = retrieveCurConfig()
    curConfig.each {
        switch(it.key) {
            case "abcDays":
                if (it.value != co2Baseline) { sendConfig(it.key, co2Baseline)};
                if (debugLog) {log.debug "configure(): Set ${it.key} to ${it.value}"};    
            break;
            case "configurationControl":
                if (it.value != configControl) { sendConfig(it.key, '"'+configControl+'"')};
                if (debugLog) {log.debug "configure(): Set ${it.key} to ${it.value}"};     
            break;
            case "displayBrightness":
                if (it.value != displayBright) { sendConfig(it.key, displayBright)};
                if (debugLog) {log.debug "configure(): Set ${it.key} to ${it.value}"};     
            break;
            case "ledBarBrightness":
                if (it.value != ledBarBrightness) { sendConfig(it.key, ledBarBrightness)};
                if (debugLog) {log.debug "configure(): Set ${it.key} to ${it.value}"};     
            break;
            case "ledBarMode":
                if (it.value != ledBarMode) { sendConfig(it.key, '"'+ledBarMode+'"')};
                if (debugLog) {log.debug "configure(): Set ${it.key} to ${it.value}"};     
            break;
            case "noxLearningOffset":
                if (it.value != noxLearnOffset) { sendConfig(it.key, noxLearnOffset)};
                if (debugLog) {log.debug "configure(): Set ${it.key} to ${it.value}"};     
            break;
            case "pmStandard":
                if (it.value != pmStandard) { sendConfig(it.key, '"'+pmStandard+'"')};
                if (debugLog) {log.debug "configure(): Set ${it.key} to ${it.value}"};     
            break;
            case "postDataToAirGradient":
                if (it.value != postDataToAirGradient) { sendConfig(it.key, postDataToAirGradient)};
                if (debugLog) {log.debug "configure(): Set ${it.key} to ${it.value}"};     
            break;
            case "temperatureUnit":
                unit = "c"
                if (cOrF == "Fahrenheit") { unit = "f"};
                if (it.value != unit) { sendConfig(it.key, '"'+unit+'"')};
                if (debugLog) {log.debug "configure(): Set ${it.key} to ${it.value}"};     
            break;
            case "tvocLearningOffset":
                if (it.value != tvocLearnOffset) { sendConfig(it.key, tvocLearnOffset)};
                if (debugLog) {log.debug "configure(): Set ${it.key} to ${it.value}"};     
            break;
            default: 
                if (debugLog) {log.debug ("configure(): Unknown setting value")}; 
            break;     
        }
    }
}

/*
    Retrieve data states from device
*/
def poll() {
    def params = [
        uri   : "http://"+ip,
        path  : '/measures/current',
        contentType: "application/json"   
    ]  
    httpGet(params) { resp ->
        if (debugLog) {log.debug "poll(): Response Data is "+resp.data}
        if (resp.status == 200) {
            if (debugLog) {log.debug "Poll(): Successful poll. Parsing data to apply to device"}
            resp.data.each {
                if (debugLog) {log.debug "poll(): Respone Data Key Name "+it.key}
                if (debugLog) {log.debug "poll(): Respone Data Value "+it.value}
                if (it.key == "pm01") {
                    sendEvent(name: "pm01", value: it.value)
                } else if (it.key == "pm02") {
                    sendEvent(name: "pm25", value: it.value)
                } else if (it.key == "pm10") {
                    sendEvent(name: "pm10", value: it.value)
                }  else if (it.key == "pm003Count") {
                    sendEvent(name: "pm003Count", value: it.value)
                } else if (it.key == "rhumCompensated") {
                    sendEvent(name: "humidity", value: it.value)
                } else if (it.key == "wifi") {
                    sendEvent(name: "rssi", value: it.value)
                } else if (it.key == "rco2") {
                    sendEvent(name: "carbonDioxide", value: it.value)
                } else if (it.key == "tvocIndex") {
                    sendEvent(name: "tvoc", value: it.value)
                }  else if (it.key == "noxIndex") {
                    sendEvent(name: "nox", value: it.value)
                } else if (it.key == "atmpCompensated") {
                    float temp = it.value
                    if (cOrF == "Fahrenheit") { temperature=((temp*9/5)+32).round(1) 
                        sendEvent(name: "temperature", value: temperature)
                    } else sendEvent(name: "temperature", value: temp.round(1))
                } else {
                    if (debugLog) {log.debug "Unknown parm to parse ${it.key} = ${it.value}"}
                }
            }
        } 
    }
    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        runIn(pollRateInt,poll)
    }
} 

/* 
    Update Configuration
*/

def sendConfig(parm, parmvalue) {
    String bodyParm = '{"'+parm+'":'+parmvalue+'}'
        def params = [
        uri   : "http://"+ip,
        path  : '/config',
        headers: ["Content-Type": "application/json"],    
        contentType: "application/json",
        body: bodyParm
    ]
    if (debugLog) { log.debug "sendConfig(): ${params}"}
    try {
	    httpPut(params) { resp ->
            if (debugLog) {log.debug "poll(): Response Data is "+resp.data}
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        if (e.statusCode != 200){
		    log.error "Error: e.statusCode ${e.statusCode}"
		    log.error "${e}"     
		    return 'unknown'
        }
	}
}


/* 
    Retrieve Current Configuration
*/

def retrieveCurConfig() {
        def params = [
        uri   : "http://"+ip,
        path  : '/config',   
        contentType: "application/json"
    ]
    if (debugLog) { log.debug "retrieveCurConfig(): ${params}"}
    try {
	    httpGet(params) { resp ->
            if (debugLog) {log.debug "poll(): Response Data is "+resp.data}
            return resp.data
        }
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"     
		return 'unknown'
	}
}


/*
	logsOff
    
	Disables debug logging.
*/
def logsOff()
{
    log.warn "debug logging disabled..."
	device.updateSetting("enableDebug", [value:"false",type:"bool"])
}

/*
    Custom Functions
*/

def ledBarTest() {
    command = "ledBarTestRequested"
//    request = true
    sendConfig(command, true)
}

def co2CalibrateRequest() {
    command = "co2CalibrationRequested"
//    request = true
    sendConfig(command, true)
}
