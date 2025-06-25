/**
 *  Unifi Integration Manager Outbound-Webhook
 *
 *  Copyright 2015 
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
 *
 *
 * 06/18/2025
 *
 */
definition(
    name: "Unifi Integration Manager Outbound-Webhook",
    namespace: "Mavrrick",
    author: "Craig King",
    description: "Unifi Integration Manager webhook Child app",
    category: "Convenience",
    parent: "Mavrrick:Unifi Integration Manager",
    importUrl: "https://raw.githubusercontent.com/Mavrrick/Hubitat-by-Mavrrick/refs/heads/main/UniFi/Mavrrick.UnifiIntegrationManagerOutboundWebhook.groovy",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn"
)


preferences { 
    page(name: "Setup Webhook", title: "Setup Webhook", install: true, uninstall: true){ 
	 section("Give this outbound webhook a name?") {
     label(name: "label",
     title: "Give this outbound webhook a name?",
     required: false,
     multiple: false)
	}
	section("Select capability of device use to detect activity") {
        input ("capabilityType", "enum", title: "Capability of the device to trigger Call to Unifi Alarm Manager.",
           options:["Motion","Temperature","Contact Sensor"], submitOnChange: true) 
	}   
    section("Device") {
        if (capabilityType == "Motion") {
		    input ("motion", "capability.motionSensor", required: false)
        } else if (capabilityType == "Temperature") {
            input ("temp", "capability.temperatureMeasurement", required: false)
        } else if (capabilityType == "Contact Sensor") {
		    input ("contact", "capability.contactSensor", required: false)
	    }
    }
        section("What trigger are you looking for") {
        if (capabilityType == "Motion") {
           input ("motionState", "enum", title: "Capability of the device to trigger Call to Unifi Alarm Manager.",
           options:["inactive", "active","both"], submitOnChange: true)
        } else if (capabilityType == "Temperature") {
           input ("temperatureCompare", "enum", title: "Capability of the device to trigger Call to Unifi Alarm Manager.",
           options:[">", "<","="], submitOnChange: true)
           input ("tempValue", "number", title: "Please enter the number to validate against", required: false)
        } else if (capabilityType == "Contact Sensor") {
           input ("contactState", "enum", title: "Capability of the device to trigger Call to Unifi Alarm Manager.",
           options:["open", "closed","both"], submitOnChange: true)
        } 
    }    
    section("Enter the Complete Webhook URL") {
        input ("url", "string", title: "Please past the entire Webhook URL from Unifi Alarm Manager", required: false)
    }

    section(/*getFormat("header-green", "${getImage("Blank")}"+*/ " General") /*)*/ {
//            label title: "Enter a name for this automation", required: false
            input "logEnable", "bool", defaultValue: false, title: "Enable Debug Logging", description: "debugging"
		}
}
}
        
def installed() {
    log.info("Installed Unifi Outlook-Webhook")
    subscribeTo()
}

def updated() {
	unsubscribe()
    log.info("Installed Unifi Outlook-Webhook")
    subscribeTo()
}

def subscribeTo() {
    if (capabilityType == "Motion") {
        if (motionState == "inactive") {
            subscribe(motion, "motion.inactive", callWebhook)
        } else if (motionState == "active") {
            subscribe(motion, "motion.active", callWebhook)
        } else {
            subscribe(motion, "motion", callWebhook)
        }
    } else if (capabilityType == "Temperature") {
        subscribe(temp, "temperature", tempHandler)
    } else if (capabilityType == "Contact Sensor") {
        if (contactState == "open") {
            subscribe(contact, "contact.open", callWebhook)
        } else if (contactState == "closed") {
            subscribe(contact, "contact.closed", callWebhook)
        } else {
            subscribe(contact, "contact", callWebhook)
        }
    } 
}

def tempHandler(evt) {
    if (temperatureCompare == ">") {
        if (evt > tempValue) {
            callWebhook (evt)
        }
    } else if (temperatureCompare == "<") {
        if (evt < tempValue) {
            callWebhook (evt)
        } 
    } else if (temperatureCompare == "=") {
        if (evt == tempValue) {
            callWebhook (evt)
        } 
    }
}

     
def callWebhook(evt) {
    log.debug "An Event took place"
     def params = [
            uri   : url,
			headers: ["X-API-KEY": parent.unifiApiToken, "Content-Type": "application/json"],
            contentType: "application/json", 
            ignoreSSLIssues: true
            ]  
    if (debugLog) { log.debug "callWebhook(): ${params}"}
	try {

			httpPost(params) { resp ->

		}
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"    
		return 'unknown'
	}
}    
