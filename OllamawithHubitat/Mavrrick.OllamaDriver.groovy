/**
 *  Ollama LLM Integration tool
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
import groovy.json.JsonOutput

metadata
{
    definition (
        name: "Ollama Driver", 
        namespace: "Mavrrick", 
        author: "Mavrrick",
        importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/roku/device/roku-tv.groovy')
    {        
        attribute "prompt", "string"
        attribute "response", "string"
        attribute "model", "string"
        attribute "ConvTime", "number"
        attribute "ollamaState", "string"
        
        command "askQuestion", [
            [name: "question", type: "STRING", description: "AI Prompt for question or action"]
           ]
        command "newConversation"
                          
	}

	preferences
	{	
		section
        {
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
		}
	}
}


def installed()
{
	log.info "Air Gradient is loaded. Waiting for updates"
    initialize()
	settingsInitialize()
}


def updated()
{
	log.info "Air Gradient is loaded. Waiting for updates"
    initialize()
	
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
}

void configure(){
}

/*
    Retrieve data states from device
*/
def askQuestion(question) {
    sendEvent(name: "ollamaState", value: "Processing Request")
    long startTime = now()
    sendEvent(name: "prompt", value: question)
    response = parent.chat(question)
    sendEvent(name: "response", value: response)
    long endTime = now()
    long duration = endTime - startTime
    def formattedDuration = formatDuration(duration)
    if (debugLog) log.info "askQuestion() Elapse time ${formattedDuration}."
    sendEvent(name: "ConvTime", value: formattedDuration)
    sendEvent(name: "ollamaState", value: "Ready")
} 


def formatDuration(long milliseconds) {
    if (milliseconds < 1000) {
        return "${milliseconds} ms"
    }

    long seconds = milliseconds / 1000
    if (seconds < 60) {
        return "${seconds} s ${milliseconds % 1000} ms"
    }

    long minutes = seconds / 60
    if (minutes < 60) {
        return "${minutes} min ${seconds % 60} s ${milliseconds % 1000} ms"
    }

    long hours = minutes / 60
    return "${hours} h ${minutes % 60} min ${seconds % 60} s ${milliseconds % 1000} ms"
}

def newConversation() {
    sendEvent(name: "ollamaState", value: "Starting New Conversation")
    sendEvent(name: "prompt", value: "New Conversation")
    parent.clearConversation()
    sendEvent(name: "ollamaState", value: "Ready")
    sendEvent(name: "response", value: " ")
    
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
	updateData
    
	Routine to update data values from parent app.
*/
def updateData()
{
    log.warn "updateData(): Checking for updated configuration values"
	if (device.getDataValue("model") != parent?.model) {
        if (debugLog) {log.debug "apiKeyUpdate(): Detected new Model value. Applying"}
        device.updateDataValue("model", parent?.model)
    }
    if (device.getDataValue("server") != parent?.serverIP) {
        if (debugLog) {log.debug "apiKeyUpdate(): Detected new Model value. Applying"}
        device.updateDataValue("model", parent?.serverIP)
    }
}


