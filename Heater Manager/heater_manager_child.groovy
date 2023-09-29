/**
 *  Dewpoint Aware Humidifity control using an external temp sensor or weather underground
 *
 *  Copyright 2015 Bryan Greffin
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
 /**
 * 10/31/2018
 * Updated required fields so the application can be used simply to humidify or dehumidify and not require both functions
 * Corrected value used for humidification low value to turn on assigned switch
 */
definition(
    name: "Heater Manager-Child",
    namespace: "Mavrrick",
    author: "Craig King",
    description: "Manages Heat in home using Space Heaters",
    category: "Convenience",
    parent: "Mavrrick:Heater Manager",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn?displaySize=2x"
)


preferences { 
    page(name: "page1", title: "Select sensors and set confortable humidity range", install: true, uninstall: true){ 
	 section("Give this app a name?") {
     label(name: "label",
     title: "Give this app a name?",
     required: false,
     multiple: false)
	}
	section("Room Temp Sensor:") {
	input ("tempInput", "capability.temperatureMeasurement", required: true)
	}   
    section("Space Heater?") {
		input ("switch2", "capability.switch", required: false)
	}
    section("Temp On/Off range") {
        input ("tempRange", "number", title: "Desired Temperature Variance for on/off range above and below the setpoint.  Default is 1%.", required: false)
    }
     section( "Setpoint set by:" ) {
        input ("setPointSupply", "enum", title: "which source?",
              options: ["Entry", "Thermostat"], required: true, submitOnChange: true)
    }
    
    if (setPointSupply == "Entry") {
	    section("Enter Setpoint") {
        input ("tempMin", "number", title: "Desired minimum temp.", required: false)
        }
}
	else {
    section("Which sensor will supply the temperature?") { 
    input ("thermostat", "capability.thermostat", required: false)
    }
 }
    		section(/*getFormat("header-green", "${getImage("Blank")}"+*/ " General") /*)*/ {
//            label title: "Enter a name for this automation", required: false
            input "logEnable", "bool", defaultValue: false, title: "Enable Debug Logging", description: "debugging"
		}
}
}
        
def installed() {
	subscribe(tempInput, "temperature", tempAction)
    log.info("Installed Heater manager application")
}

def updated() {
	unsubscribe()
	subscribe(tempInput, "temperature", tempAction)
    log.info("Updated Heater manager application")
}
     
     
def tempAction(evt) {
    if (logEnable) log.debug("tempAction(): Temp action to validate temp event ${evt.value}") 
    long evt2 = evt.value.toLong()
    if (setPointSupply == "Entry") {
        tempOn = (tempMin - tempRange)
        tempOff = (tempMin + tempRange)
    } else {
        if (logEnable) log.debug("tempAction(): Current thermostat set point is ${thermostat.currentValue("heatingSetpoint")}")
         setpoint = thermostat.currentValue("heatingSetpoint").toLong()
         tempOn = (setpoint - tempRange)
         tempOff = (setpoint + tempRange)
    }
    if (logEnable) log.debug("tempAction(): Temp to turn off at ${tempOff}, Temp to turn on at ${tempOn} based on variance ") 
    if (evt2 < tempOn && "off" == switch2.currentSwitch) {
        if (logEnable) log.debug("tempAction(): Temp below minimum = ${evt.value}. Turning On Heater") 
        switch2.on()
    }
    else if (evt2 > tempOff && "on" == switch2.currentSwitch) {
        if (logEnable) log.debug("tempAction(): Temp above setpoint = ${evt.value}. Turning off Heater")
        switch2.off()
    }
    else  {
    if (logEnable) log.debug("tempAction(): no change = ${evt.value}")
    }
}
