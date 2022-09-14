 *  Air Gradient DIY to recieve commands from Node-red 
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
    definition (name: "Air Gradient Virtual", namespace: "mavrrick", author: "Mavrrick")
    {
        capability "AirQuality"
		capability "TemperatureMeasurement"
		capability "RelativeHumidityMeasurement"
		capability "CarbonDioxideMeasurement"
		
		attribute "tvoc", "number"

        command "update", [[name: "CO2 Mesurement", type: "NUMBER", description: "CO2 from Air Gradient"],
                          [name: "PM Mesurement", type: "NUMBER", description: "PM25 mesurement from Air Gradient"],
                          [name: "TVOC Mesurement", type: "NUMBER", description: "tVOC mesurement from Air Gradient"],
                          [name: "Temperature", type: "NUMBER", description: "Temperature from Air Gradient"],
                          [name: "Humidity", type: "NUMBER", description: "Humidity from Air Gradient"]]
                          
	}

	preferences
	{	
		section
		{
			input "cOrF", "bool", title: "Turn on to show value in Farenhite", defaultValue: false, required: false		
            input "enableDebug", "bool", title: "Enables debug logging for 30 minutes", defaultValue: false, required: false
		}
	}
}


def installed()
{
	log.info "Air Gradient is loaded. Waiting for updates"
	settingsInitialize()
	refresh()
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
	log.info "Air Gradient Driver restarting"

	}



/*
    Updates provided by Air Gradient
*/
def update( carbonDioxide , pm, tvoc, temperature, humidity  ){
    sendEvent(name: "carbonDioxide ", value: carbonDioxide )
    sendEvent(name: "pm", value: pm)
    sendEvent(name: "tvoc", value: tvoc)
    if (cOrF) { temperature=(temperature*9/5)+32 
               sendEvent(name: "temperature", value: temperature)}
    else sendEvent(name: "temperature", value: temperature)
    sendEvent(name: "humidity", value: humidity)
//Calculate AQI from PM and TVOC
    if (pm <= 12.0) { aqicalc1 = ((50-0)/(12.0)*(pm)+ 0).toDouble() }
    else if (pm <= 35.4) { aqicalc1 = ((100 - 50) / (35.4 - 12.0) * (pm - 12.0) + 50).toDouble()}
    else if (pm <= 55.4) { aqicalc1 = ((150 - 100) / (55.4 - 35.4) * (pm - 35.4) + 100).toDouble()}
    else if (pm <= 150.4) { aqicalc1 = ((200 - 150) / (150.4 - 55.4) * (pm - 55.4) + 150).toDouble()}
    else if (pm <= 250.4) { aqicalc1 = ((300 - 200) / (250.4 - 150.4) * (pm - 150.4) + 200).toDouble()}
    else if (pm <= 350.4) { aqicalc1 = ((400 - 300) / (350.4 - 250.4) * (pm - 250.4) + 300).toDouble()}
    else if (pm <= 500.4) { aqicalc1 = ((500 - 400) / (500.4 - 350.4) * (pm - 350.4) + 400).toDouble()}
    def aqi = aqicalc1+tvoc
    aqi = aqi.round(1)
//    log.debug "aqi value is : ${aqi}"
    if (aqi>=1) { sendEvent(name: "airQualityIndex", value: aqi )}
    else sendEvent(name: "airQualityIndex", value: 0 )
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
