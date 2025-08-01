/* groovylint-disable LineLength, DuplicateNumberLiteral, DuplicateStringLiteral, ImplementationAsType, ImplicitClosureParameter, InvertedCondition, LineLength, MethodReturnTypeRequired, MethodSize, NestedBlockDepth, NglParseError, NoDef, NoJavaUtilDate, NoWildcardImports, ParameterReassignment, UnnecessaryGString, UnnecessaryObjectReferences, UnnecessaryToString, UnusedImport, VariableTypeRequired */
/**
 *  Govee Integration
 *
 *  Copyright 2018 CRAIG KING
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License
 */

definition(
    name: 'Ollama Integration',
    namespace: 'Mavrrick',
    author: 'CRAIG KING',
    description: 'Ollama Integration for Chat and AI Interations',
    category: 'AI',
    importUrl: 'https://raw.githubusercontent.com/Mavrrick/Hubitat-by-Mavrrick/refs/heads/main/OllamawithHubitat/Mavrrick.OllamaDriver.groovy',
//    documentationLink: "https://docs.google.com/document/d/e/2PACX-1vRsjfv0eefgPGKLYffNpbZWydtp0VqxFL_Xcr-xjRKgl8vga18speyGITyCQOqlQmyiO0_xLJ9_wRqU/pub",
    iconUrl: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    iconX2Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    iconX3Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    singleThreaded: false,
    singleInstance: true)

/*
* Initial release v1.0.0
*/

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field
import hubitat.helper.HexUtils

@Field static List child = []
@Field static List childDNI = []
@Field static Map goveeScene = [:]
@Field static String conversation = null

/**
 * Maps common color names to their HSL values (Hue: 0-360, Saturation: 0-100, Lightness: 0-100).
 */
@Field Map<String, Map<String, Integer>> colorNameToHsl = [
    "red": [hue: 0, saturation: 100, lightness: 50],
    "green": [hue: 33, saturation: 100, lightness: 50],
    "blue": [hue: 66, saturation: 100, lightness: 50],
    "yellow": [hue: 17, saturation: 100, lightness: 50],
    "cyan": [hue: 50, saturation: 100, lightness: 50],
    "magenta": [hue: 83, saturation: 100, lightness: 50],
    "white": [hue: 0, saturation: 0, lightness: 100],
    "black": [hue: 0, saturation: 0, lightness: 0],
    "orange": [hue: 8, saturation: 100, lightness: 50],
    "purple": [hue: 75, saturation: 100, lightness: 50],
    "pink": [hue: 92, saturation: 100, lightness: 70]
]

preferences
{
    page(name: 'mainPage', title: 'Govee Integration')
    page(name: 'setup', title: 'Setup of Ollama environment')
    page(name: 'deviceSelection', title: 'Device Install Process')
    page(name: 'about', title: 'About')
}

/*
    mainPage

    UI Page: Main menu for the app.
*/
def mainPage() {
    statusMessage = ""
    def int childCount = child.size()
    dynamicPage(name: 'mainPage', title: 'Main menu', uninstall: true, install: true, submitOnChange: true)
    {
//        section('<b>Ollama Setup</b>') {
        section('') {            
            href 'setup', title: 'Ollama Setup Menu' 
            href 'deviceSelection', title: 'Device selection for use with Ollama' 

        }

        section('<b>Logging Options</b>') {
            input(
                name: 'configLoggingLevelIDE',
                title: 'IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.',
                type: 'enum',
                options: [
                    '0' : 'None',
                    '1' : 'Error',
                    '2' : 'Warning',
                    '3' : 'Info',
                    '4' : 'Debug',
                    '5' : 'Trace'
                ],
                defaultValue: '1',
                displayDuringSetup: true,
                required: false
            )
        }
        section('About') {
            href 'about', title: 'About Information menu', description: 'About Menu for Govee Integration'
        }
    }
}

def setup() {
    app.clearSetting("modelID")
    if (serverIP) {
        options = getModels()
        state.modelCapabilities = showModels()    
    }
    logger(" setup() $options", 'debug')
    dynamicPage(name: 'setup', title: 'Setup Ollama integration Parms', uninstall: false, install: false, nextPage: "mainPage", submitOnChange: true)
    {
        section('<b>Server Information</b>')
        {
            paragraph 'Please enter your Ollama server local network IP address and port. It should look something like 192.168.1.125:11434'
            input(name: 'serverIP', type: 'string', required:false, description: 'Please enter the IP and port of your server.', multiple:false, submitOnchange: true,)
        }
        if (serverIP) { 
            if (options != null) {
                section('<b>Model</b>') {
                    paragraph 'Please select the desired model from the selection below'
                    input(name: 'model', type: 'enum', required:false, description: 'Please select the downloaded LLM model you wish to use.', multiple:false, submitOnChange: true,
                        options: options.sort() , width: 8, height: 1)
                    paragraph ' The model supports '+state.modelCapabilities+' capabilities' 
                    if (state.modelCapabilities.contains("thinking")) {
                        input(name: 'thinking', title: "Would you like to hide the LLM thinking from the response", type: 'bool', defaultvalue: false , description: 'If you LLM Mode supports thinking this allow it the thinking to be hidden')    
                    }
                }   
            }
        
            section('<b>Manage Models in Ollama</b>') {
                paragraph "Enter the LLM Identifier in the field below and then click on the button for the action you want to take"
                input(name: 'modelID', type: 'string', required:false, description: 'Please enter a valid LLM idenitifier such as qwen3:latest .', multiple:false)
                input "modelPull" , "button",  title: "Pull Model"
//              input "modelDelete" , "button",  title: "Delete Model" // Needed call not supported
            }
        }
    }
}

def deviceSelection() {
    dynamicPage(name: 'deviceSelection', title: 'Select Devices to work with Ollama', uninstall: false, install: false, nextPage: "mainPage", submitOnChange: true)
    {
        section 
		{    
			paragraph "Hubitat devices shared Ollma has access to"

			input "devices", "capability.*", title: "Select a device", multiple: true, required: true
       	}
    }
}


def about() {
    dynamicPage(name: 'about', title: 'About Govee Integration with HE', uninstall: false, install: false, nextPage: "mainPage")
    {
        section()
        {
            paragraph image: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg', 'Govee Integration'
        }
        section('Support the Project')
        {
            paragraph 'Govee is provided free for personal and non-commercial use.  I have worked on this app in my free time to fill the needs I have found for myself and others like you.  I will continue to make improvements where I can. If you would like you can donate to continue to help with development please use the link below.'
            href(name: 'donate', style:'embedded', title: "Consider making a \$5 or \$10 donation today to support my ongoing effort to continue improving this integration.", url: 'https://www.paypal.me/mavrrick58')
            paragraph("<style>/* The icon */ .help-tip{ 	position: absolute; 	top: 50%; 	left: 50%; 	transform: translate(-50%, -50%); 	margin: auto; 	text-align: center; 	border: 2px solid #444; 	border-radius: 50%; 	width: 40px; 	height: 40px; 	font-size: 24px; 	line-height: 42px; 	cursor: default; } .help-tip:before{     content:'?';     font-family: sans-serif;     font-weight: normal;     color:#444; } .help-tip:hover p{     display:block;     transform-origin: 100% 0%;     -webkit-animation: fadeIn 0.3s ease;     animation: fadeIn 0.3s ease; } /* The tooltip */ .help-tip p {    	display: none; 	font-family: sans-serif; 	text-rendering: optimizeLegibility; 	-webkit-font-smoothing: antialiased; 	text-align: center; 	background-color: #FFFFFF; 	padding: 12px 16px; 	width: 178px; 	height: auto; 	position: absolute; 	left: 50%; 	transform: translate(-50%, 5%); 	border-radius: 3px; /* 	border: 1px solid #E0E0E0; */ 	box-shadow: 0 0px 20px 0 rgba(0,0,0,0.1); 	color: #37393D; 	font-size: 12px; 	line-height: 18px; 	z-index: 99; } .help-tip p a { 	color: #067df7; 	text-decoration: none; } .help-tip p a:hover { 	text-decoration: underline; } /* The pointer of the tooltip */ .help-tip p:before { 	position: absolute; 	content: ''; 	width: 0; 	height: 0; 	border: 10px solid transparent; 	border-bottom-color:#FFFFFF; 	top: -9px; 	left: 50%; 	transform: translate(-50%, -50%); }  /* Prevents the tooltip from being hidden */ .help-tip p:after { 	width: 10px; 	height: 40px; 	content:''; 	position: absolute; 	top: -40px; 	left: 0; } /* CSS animation */ @-webkit-keyframes fadeIn {     0% { opacity:0; }     100% { opacity:100%; } } @keyframes fadeIn {     0% { opacity:0; }     100% { opacity:100%; } }</style><div class='help-tip'><p>This is the inline help tip! It can contain all kinds of HTML. Style it as you please.<br /><a href='#'>Here is a link</a></p></div>")
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
            addChildDevice('Mavrrick', 'Ollama Driver', "Ollama" , location.hubs[0].id, [
            'name': 'Ollama',
            'label': 'Ollama',
             'data': [
//                'server': settings.serverIP,
//                'model': settings.model
             ],
             'completedSetup': true,
         ])
    state.isInstalled = true
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    child = getChildDevices()
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
}

def uninstalled() {
    // external cleanup. No need to unsubscribe or remove scheduled jobs
    // 1.4 Remove dead virtual devices
    getChildDevices()?.each
    { childDevice ->
            deleteChildDevice(childDevice.deviceNetworkId)
    }
}

def sendnotification (type, value) {
    if (notifyEnabled) {
        String notificationText = "${type} has reached threshold of ${value}. You will have issues if this get to zero"
        notificationDevice?.each {
            it.deviceNotification(notificationText)
        }
    }
}


/**
 *  logger()
 *
 *  Wrapper function for all logging.
 **/
private logger(msg, level = 'debug') {
    switch (level) {
        case 'error':
            if (state.loggingLevelIDE >= 1) { log.error msg };
            break;
        case 'warn':
            if (state.loggingLevelIDE >= 2)  { log.warn msg };
            break;
        case 'info':
            if (state.loggingLevelIDE >= 3) { log.info msg };
            break;
        case 'debug':
            if (state.loggingLevelIDE >= 4) { log.debug msg };
            break;
        case 'trace':
            if (state.loggingLevelIDE >= 5) { log.trace msg };
            break;
        default:
            log.debug msg;
            break;
    }
}


/**
 *  appButtonHandler()
 *
 *  Handler for when Buttons are pressed in App. These are used for the Scene extract page.
 **/

private def appButtonHandler(button) {
    if (button == "modelPull") {
        pullModels()
    } else if (button == "modelDelete") {
        deleteModels()
    } 
}

///////////////////////////////////////////
// Helper methods for certain tasks // 
///////////////////////////////////////////


private String escapeStringForPassword(String str) {
    //logger("$str", "info")
    if (str) {
//        str = str.replaceAll(" ", "\\\\ ") // Escape spaces.
//        str = str.replaceAll(",", "\\\\,") // Escape commas.
        str = str.replaceAll("=", "\u003D") // Escape equal signs.
//        str = str.replaceAll("\"", "\u0022") // Escape double quotes.
//    str = str.replaceAll("'", "_")  // Replace apostrophes with underscores.
    }
    else {
        str = 'null'
    }
    return str
}

def getBaseUrl() {
	def scheme = sslEnabled ? "https" : "http"
	def port = sslEnabled ? "8443" : "8080"
	return "$scheme://127.0.0.1:$port"
}

def getBackgroundStatusMessage() {
	return statusMessage
}

def showHideNextButton(show) {
	if(show) paragraph "<script>\$('button[name=\"_action_next\"]').show()</script>"
	else paragraph "<script>\$('button[name=\"_action_next\"]').hide()</script>"
}

def base64ToHex(base64Str) { // Proper conversion from Base64 to Hex for scene creation.
    // Decode Base64 string to byte array
    byte[] decodedBytes = base64Str.decodeBase64()

    // Convert byte array to hex string
    def hexString = decodedBytes.collect { String.format("%02x", it) }.join('')
    
    return hexString
}


def calculateChecksum8Xor(String hexString) {
    int checksum = 0
    for (int i = 0; i < hexString.length(); i += 2) {
        String byteStr = hexString.substring(i, Math.min(i + 2, hexString.length()))
        int byteValue = Integer.parseInt(byteStr, 16)
        checksum ^= byteValue
    }
    return String.format("%02X", checksum) // Format as two-digit hex
}

def hexToBase64(String hexString) {
    if (!hexString) {
        return null
    }

    try {
        byte[] bytes = new byte[hexString.length() / 2]
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hexString.substring(i, i + 2), 16)
        }

        return base64Encode(bytes)

    } catch (NumberFormatException e) {
        log.error "Invalid hex string: ${e.message}"
        return null
    } catch (IllegalArgumentException e) {
        log.error "Invalid hex string length: ${e.message}"
        return null
    }
}



private String base64Encode(byte[] data) {
    StringBuilder sb = new StringBuilder();
    int b;
    int dataLen = data.length;
    int i = 0;
    while (i < dataLen) {
        b = data[i++] & 0xff;
        sb.append(BASE64_CHARS.charAt(b >> 2));
        if (i == dataLen) {
            sb.append(BASE64_CHARS.charAt((b & 0x3) << 4));
            sb.append("==");
            break;
        }
        b = (b & 0x3) << 4 | (data[i] & 0xff) >> 4;
        sb.append(BASE64_CHARS.charAt(b));
        if (++i == dataLen) {
            sb.append(BASE64_CHARS.charAt((data[i - 1] & 0xf) << 2));
            sb.append("=");
            break;
        }
        b = (data[i - 1] & 0xf) << 2 | (data[i] & 0xff) >> 6;
        sb.append(BASE64_CHARS.charAt(b));
        sb.append(BASE64_CHARS.charAt(data[i++] & 0x3f));
    }
    return sb.toString();
}

def isWholeNumber(number) {
    if (number == null) {
        return false // Handle null case
    }
    return number == number.intValue() // Compare to int value
}

/* 
    Retrieve List of avaliable Models
*/

def getModels() {
    List models = []
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/tags',
        contentType: "application/json",
    ]
        try {
        httpGet(params) { resp ->
            if (debugLog) {log.debug "getModels(): Response Data is "+resp.data.models}
            if (resp.status == 200) {
                if (debugLog) {log.debug "getModels(): Successful poll. Parsing data to apply to device"}
                resp.data.models.each {
                    if (debugLog) {log.debug "getModels(): Adding model "+it.model+" to list"}
                    models.add(it.model)
                }
            }
        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
    }
    if (debugLog) {log.debug "getModels() List of loaded models are ${models}"}
    return models
}


/* 
    Pull Models
*/

def pullModels() {
    bodyparm = '{"model": "'+modelID+'","stream": false}'
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/pull',
//        contentType: "application/json",
        body : bodyparm,
        timeout : 300
    ]
        try {
        httpPost(params) { resp ->
            logger("pullModels(): Response Data is ${resp.data}", 'info')
            if (resp.status == 200) {
                logger("pullModels(): Sucessfully Pulled Model", 'info')
            }
        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
    }
}

/* 
   Show Model information to retrieve Capabilities
*/

def showModels() {
    List modelCapabilities = []
    bodyparm = '{"model": "'+model+'"}'
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/show',
        contentType: "application/json",
        body : bodyparm
    ]
        try {
        httpPost(params) { resp ->
            logger("showModels(): Response Data is ${resp.data}", 'trace')
            
            if (resp.status == 200) {
                modelCapabilities = resp.data.capabilities
                logger("showModels(): Capabilities found ${resp.data.capabilities}", 'info')
            }
        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
    }
//    logger("showModels(): Response Data is ${resp.data}", 'info')
    return modelCapabilities
}

//
// Ollama Conversation Procesing
//

def chat(question) {
    def conversationAdd = null
    def toolResponse = null
    def processJSON = null
    logger("chat(): Conversation History : ${conversation}", 'trace')
    if (conversation == null) {
        conversation  = '{"role":"user","content":"'+question+'"}'
    } else {
    conversation  =  conversation + ',{"role":"user","content":"'+question+'"}' 
    }
    logger("chat(): Current Submited Conversation : ${conversation}", 'trace')
    if (state.modelCapabilities.contains("tools")) {
        processJSON = toolFunctions()
    } else {
        processJSON = structuredOutputJSON()
    }
    String bodyParm = ""
    
    if (state.modelCapabilities.contains("thinking")) {
        bodyParm = '{"model":"'+model+'","messages":['+conversation+'],"think":true,"stream":false,'+processJSON+'}'
    } else {
        bodyParm = '{"model":"'+model+'","messages":['+conversation+'],"stream":false,'+processJSON+'}'    
    }

    def params = [
        uri   : "http://"+serverIP,
        path  : '/api/chat',
        contentType: "application/json",
        timeout: 600, 
        body: bodyParm
    ] 
    logger("chat(): Attempting to send request to ollama server at ${serverIP}, parms: ${params}", 'debug')
    sendEvent(name: "prompt", value: question)
    try {
        httpPostJson(params) { resp ->
            logger("chat(): Response Data is ${resp.data}", 'trace')
            if (resp.status == 200) {
                tokens_per_sec = resp.data.eval_count.toInteger() / (resp.data.eval_duration.toInteger() / 1000000000)
                logger("chat(): Successful query to Ollama. Parsing response data for action", 'info')
                resp.data.each {
                    if (it.key == "response") {
                    }  else if (it.key == "message") {
                        conversationAdd = JsonOutput.toJson(it.value)
                        if (it.value.tool_calls) {
                            size = it.value.tool_calls.size()
                            logger("chat(): Number of functions ${size}", 'info')
                            counter = 0 
                            while (size >= counter +1) {
                                logger("chat(): Function identified by Ollama ${it.value.tool_calls.get(counter).function.name}", 'info')
                                logger("chat(): Arguments extracted by Ollama ${it.value.tool_calls.get(counter).function.arguments}", 'info')
                                if ( counter > 0) {
                                    toolResponse = toolResponse +","+ "${it.value.tool_calls.get(counter).function.name}"(it.value.tool_calls.get(counter).function.arguments)
                                } else {
                                    toolResponse = "${it.value.tool_calls.get(counter).function.name}"(it.value.tool_calls.get(counter).function.arguments)   
                                }
//                                toolConvAdd = "${it.value.tool_calls.get(counter).function.name}"(it.value.tool_calls.get(counter).function.arguments)
                                conversationAdd = conversationAdd+',{"role":"tool","content":"'+toolResponse+'","tool_name":"'+it.value.tool_calls.get(counter).function.name+'"}'
                            //    logger("chat(): tool conversation add ${conversationAdd}", 'trace')
                                counter = counter +1
                            }
//                            conversationAdd = conversationAdd+',{"role":"tool","content":"'+toolResponse+'","tool_name":"'+it.value.tool_calls.get(counter).function.name+'"}'
                            logger("chat(): tool conversation add ${conversationAdd}", 'trace')
                        } else {
                            logger("chat(): No Function was called. Responding with response Message", 'info')
                            message = JsonOutput.toJson(it.value.content)
                            toolResponse = message
                        }
                    } else {
                    }
                }
                logger("chat(): Tokens per second ${tokens_per_sec}", 'info')
//                logger("chat(): Curent Conversation value is ${conversation}", 'info')
            }               
        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
    } 
    conversation  = conversation + ',' + conversationAdd
    logger("chat(): tool conversation add ${conversationAdd}", 'debug')
    logger("chat(): Curent Conversation value is ${conversation}", 'debug')
    return toolResponse
} 


/*
    Custom Functions json entries
*/

def toolFunctions() {
    def toolsJSON = '''"tools": [
    {
      "type": "function",
      "function": {
        "name": "get_time",
        "description": "Get the time",
        "parameters": {
          "type": "object",
          "properties": {
            "location": {
              "type": "string",
              "description": "The location to get the time for, e.g. San Francisco, CA"
            },
            "format": {
              "type": "string",
              "description": "The format to return the weather in, e.g. 'celsius' or 'fahrenheit'",
              "enum": ["24 hour", "AM/PM"]
            }
          },
          "required": ["location", "format"]
        }
      }
    },
     {
      "type": "function",
      "function": {
        "name": "control_device",
        "description": "Change a device to a given state",
        "parameters": {
          "type": "object",
          "properties": {
            "device": {
              "type": "array",
              "description": "Device(s) name to control"
            },
            "area": {
              "type": "string",
              "description": "Area, Room, or grouping of devices"
            },
            "state": {
              "type": "string",
              "description": "The requested state for a device to be changed to"
            },
            "stateType": {
              "type": "string",
              "description": "This is the type of state being asked to change, e.g. switch, brightness, color, temperature, or color temperature"
            }
          },
          "required": ["state","device","stateType"]
        }
      }
    },
     {
      "type": "function",
      "function": {
        "name": "lookup_all_Devices",
        "description": "Review a list of all devices",
        "parameters": {
          "type": "object",
          "properties": {
            "area": {
              "type": "string",
              "description": "Area, Room, or grouping of devices"
            }
          },
          "required": ["area"]
        }
      }
    },
     {
      "type": "function",
      "function": {
        "name": "set_temp",
        "description": "Used to set temp of thermostat",
        "parameters": {
          "type": "object",
          "properties": {
            "device": {
              "type": "string",
              "description": "Device name to turn on or activate, e.g. Lyra Lamp or Light Switch"
            },
            "area": {
              "type": "string",
              "description": "Area, Room, or grouping of devices"
            },
            "state": {
              "type": "desiredTemp",
              "description": "The requested state for a device to be changed to"
            }
          },
          "required": ["state"]
        }
      }
    },
     {
      "type": "function",
      "function": {
        "name": "device_state_lookup",
        "description": "Used to query a device for current associated attributes",
        "parameters": {
          "type": "object",
          "properties": {
            "device": {
              "type": "string",
              "description": "Device name to look up current state, e.g. Lyra Lamp or Light Switch"
            },
            "stateType": {
              "type": "string",
              "description": "The type of stateType to check e.g. switch, temperature, speed, humidity, presence, contact, open/close"
            },
            "state": {
              "type": "string",
              "description": "The value of state  often relating to a device attribute or sensor value"
            }
          },
          "required": ["stateType","device"]
        }
      }
    }
  ]'''
    return toolsJSON
}

def structuredOutputJSON() {
    def structuredJSON = '''"format": {
    "type": "object",
    "properties": {
      "deviceName": {
        "type": "string"
      },
       "Value": {
        "type": "string"
      },
       "attribute": {
        "type": "string"
      },
       "command": {
        "type": "string"
      }
    },
    "required": [
      "deviceName" 
    ]
  }'''
    return structuredJSON
}


//
// Ollama function Processing
//

def control_device(parms) {
    logger("control_device(): Enter device control routine", 'info')
    controlState = parms.state
    controlDevice = parms.device
    value  = ""
    unit = ""
    switch(parms.stateType){
        case "switch":
        devices.each {
            if (parms.device.contains(it.toString())) {
                it."${parms.state}"()
                logger("control_device(): ${it} has been switched ${parms.state}", 'info')
            }
        }
        break;
        case "brightness":
        devices.each {
            if (parms.device.contains(it.toString())) {
                dimNum = controlState.replace("%", "").toInteger()
                it.setLevel(dimNum, 0)                
                logger("control_device(): Dimming  ${it} to  ${dimNum}%", 'info')
            }
        }
        break;
        case "color":
        devices.each {
            if (parms.device.contains(it.toString())) {
                logger("control_device(): Set Color to ${parms.state} in HSL ${colorNameToHsl[parms.state]}", 'info')
                it.setColor(colorNameToHsl[parms.state])                
                logger("control_device(): Setting color to ${controlState}", 'info')
            }
        }
        break;
        case "color temperature":
        devices.each {
            if (parms.device.contains(it.toString())) {
                logger("control_device(): Set Color Temperature to ${parms.state}", 'info')
                it.setColorTemperature(controlState.toInteger())                
                logger("control_device(): Setting color temperature to ${controlState}", 'info')
            }
        }
        break;
        default:
        logger("control_device(): Control request with stateType: ${parms.stateType}. Please report to developer to add this handler the routine", 'info') 
        break;
    }
    responseMessage = "${controlDevice.toString()} ${parms.stateType} changed to ${controlState}"
    return responseMessage
//    logger("control_device(): Control request made with the following vaules ${parms}", 'info')
}

def device_state_lookup(parms) {
    logger("device_state_lookup(): Entered Device State Lookup Routine.", 'info')
    lookupState = parms.stateType
    lookupDevice = parms.device
    value  = ""
    unit = ""
    responseMessage = ""
    logger("device_state_lookup(): StateType = ${parms.stateType}: Device = ${parms.device}.", 'info')
    switch(parms.stateType){
        case "temperature":
        logger("device_state_lookup(): stateType matched on temperature", 'info')
        devices.each {
            if (it.toString() == parms.device.toString()) { 
                value = it.currentTemperature
                unit = "Degrees"
                logger("device_state_lookup(): State is ${parms.state} State Value is ${value}", 'info')
            }
            responseMessage = "Temperature on ${parms.device}  is ${value} ${unit}"
        }
        break;
        case "switch":
        case "on":
        case "off":
        devices.each {
            if (it.toString() == parms.device.toString()) { 
                value = it.currentSwitch
                logger("device_state_lookup() State Value is ${value}", 'info')
            }
            responseMessage = "${parms.device} is ${value} ${unit}"
        }  
        break;
        case "humidity":
        devices.each {
            if (it.toString() == parms.device.toString()) { 
                value = it.currentHumidity
                unit = "%"
                logger("device_state_lookup(): State is ${parms.state} State Value is ${value}", 'info')
            }
            responseMessage = "The humidity on ${parms.device} is ${value} ${unit}"
        }
        break;
        case "brightness":
        devices.each {
            if (it.toString() == parms.device.toString()) { 
                value = it.currentLevel
                unit = "%"
                logger("device_state_lookup(): State is ${parms.state} State Value is ${value}", 'info')
            }
            responseMessage = "Brightness on the ${parms.device} is ${value} ${unit}"
        }
        break;
        case "presence":
        devices.each {
            if (it.toString() == parms.device.toString()) { 
                value = it.currentPresence
                unit = ""
                logger("device_state_lookup(): State is ${parms.state} State Value is ${value}", 'info')
            }
            responseMessage = "${parms.device} is ${value} ${unit}"
        }
        break;
        case "contact":
        case "open/close":
        devices.each {
            if (it.toString() == parms.device.toString()) { 
                value = it.currentContact
                unit = ""
                logger("device_state_lookup(): State is ${parms.state} State Value is ${value}", 'info')
            }
            responseMessage = "${parms.device} is ${value}"
        }
        break;
        default:
        logger("device_state_lookup(): stateType is not recognized ${parms.stateType}. Forward to developer to update function for this task.", 'info')
        break;
    }
    return responseMessage
}

def set_temp(parms) {  // not yet implemented. Here as a place holder for already created function to prevent errors
/*    lookupState = parms.state
    lookupDevice = parms.device
    value  = ""
    unit = ""
    switch(parms.state){
        case "temperature":
        devices.each {
            if (it.toString() == parms.device.toString()) { 
                value = it.currentTemperature
                unit = "Degrees"
                logger("device_state_lookup(): State is ${parms.state} State Value is ${value}", 'info')
            }
        }
        break; 
    }*/
    responseMessage = "${parms.device} current ${parms.state} is "
    return responseMessage 
}

///
//  Helper Method to pass context when a new Conversation is started
///

def clearConversation() {    
    conversation = null
    logger("clearConversation(): Conversation has been cleared. Passing along context info in new chat", 'info')

    deviceContext2 = [:]
    deviceList= [:]
    devices.forEach {
        logger("clarConversation(): generating Device information: Device Name ${it}", 'debug')
        deviceDetails = [:]
        attributeList = [:]
        if (it.getLabel() != null) {
            deviceDetails["name"] = it.getLabel()
        } else {
            deviceDetails["name"] = it
        } 
        if (it.getRoomName() != null) {
            deviceDetails["room"] = it.getRoomName()
        }
        deviceList[it.getId()] = deviceDetails
        logger("clarConversation(): generating Device information: ${deviceDetails} : ${deviceList}", 'debug')
    }
    deviceContext2["devices"] = deviceList
    logger("clarConversation(): generating Device information: ${deviceContext2}", 'debug')    
    chat("You are a assistant to control a Hubitat Home Automation system. Here is a map of all of the devices as well as their commands and attributes ${deviceContext2}")
}

    
    
    
    