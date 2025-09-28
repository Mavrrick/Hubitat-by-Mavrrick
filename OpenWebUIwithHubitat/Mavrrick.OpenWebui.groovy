/* groovylint-disable LineLength, DuplicateNumberLiteral, DuplicateStringLiteral, ImplementationAsType, ImplicitClosureParameter, InvertedCondition, LineLength, MethodReturnTypeRequired, MethodSize, NestedBlockDepth, NglParseError, NoDef, NoJavaUtilDate, NoWildcardImports, ParameterReassignment, UnnecessaryGString, UnnecessaryObjectReferences, UnnecessaryToString, UnusedImport, VariableTypeRequired */
/**
 *  Ollama Integration
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
    name: 'Open Webui',
    namespace: 'Mavrrick',
    author: 'CRAIG KING',
    description: 'Open Webui Integration for Chat and AI Interations',
    category: 'AI',
    importUrl: 'https://raw.githubusercontent.com/Mavrrick/Hubitat-by-Mavrrick/refs/heads/main/OllamawithHubitat/Mavrrick.OllamaIntegration.groovy',
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
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.Field
import hubitat.helper.HexUtils

@Field static List child = []
@Field static String conversation = ""

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
    page(name: 'mainPage', title: 'OpenWeb UI Integration')
    page(name: 'setupConnect', title: 'Setup of OpenWeb UI Connectivity')
    page(name: 'setupParms', title: 'Setup of OpenWeb UI environment')
    page(name: 'deviceSelection', title: 'Device Install Process')
    page(name: 'ragHandeling', title: 'Hubitat RAG Handeling')
    page(name: 'pageEnableAPI')
    page(name: "pageDisableAPI")
    page(name: 'about', title: 'About')
    mappings {
        path("/") {
            action: [
            POST: "webHook"
            ]
        }
    } 
}

/*
    mainPage

    UI Page: Main menu for the app.
*/
def mainPage() {
    app.clearSetting("modelID")
    statusMessage = ""
    def int childCount = child.size()
    dynamicPage(name: 'mainPage', title: 'Main menu', uninstall: true, install: true, submitOnChange: true)
    {
        section('') {    
            href 'setupConnect', title: 'Open WebUI Connectivity Menu' 
            if (serverIP) { 
                href 'setupParms', title: 'Open WebUI Integration Menu' 
                href 'deviceSelection', title: 'Device selection for use with AI' 
                href 'ragHandeling', title: 'Click her to update or refresh context data in Open WebUI'
            }

        }
        section("") {
        if (state.accessToken == null) {
             section("Access Token", hidden: true) {
                paragraph("API is not yet Initialized! Setup OpenWebUI integration to complete")
//                href(name: "hrefPageEnableAPI", title: "Enable API", description: "", page: "pageEnableAPI")
             }
         } else { 
/*		        section("Inbound Webhook: (Expand for directions on use)", hideable: true, hidden: true) {
                    paragraph """This url is to allow you to send information from Unifi Alarm manager to Hubitat based on known alarm manager events. You will use the below URLs with updated params for dni, type, and value to convey what the Alarm Manager event means. <br><br><ul><li>Replace %DEVICE_DNI% with the Hubitat Device DNI intended to recieve the event.</li> <li>Replace %DETECTION_TYPE% with the Detection type from Alarm Manager.</li> <li>Replace %Additional_PARM% with any additional relevant info for the Alarm Manager event like the person or license plate detected</li></ul>"""
//                    paragraph """Valid Detection types are:<br><br><ul><li>Face</li><li>LicensePlate</li><li>NFCCardScan</li><li>FingerprintScan</li><li>Sound</li><li>PersonOfInterest</li><li>KnownFace</li><li>UnknownFace</li><li>VehicleOfInterest</li><li>KnownVehicle</li><li>UnknownVehicle</li><li>Person</li><li>Vehicle</li><li>Package</li><li>Animal</li><li>LineCrossing</li><li>Loitering</li><li>DoorbellRings</li><li>Motion</li></ul> Enter exactly as shown here with proper case"""
                } */
                
  		        section("Access Token") {
//                    String localURL = "${state.localAPIEndpoint}/?access_token=${state.accessToken}&dni=%DEVICE_DNI%&type=%DETECTION_TYPE%&value=%Additional_PARM%"
                    paragraph("Access Token: ${state.accessToken}")
                }             
            }
            paragraph('<hr style="height:4px;border-width:0;color:gray;background-color:gray">')
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
            href 'about', title: 'About Information menu', description: 'About Menu for Ollama Integration'
        }
    }
}

def setupConnect() {
    if (serverIP) {  
    }
    logger(" setup() $options", 'debug')
    dynamicPage(name: 'setupConnect', title: 'Setup Open Web UI Connectivity', uninstall: false, install: false, nextPage: "pageEnableAPI", submitOnChange: true)
    {
        section('<b>Server Information</b>')
        {
            paragraph 'Please enter your Ollama server local network IP address and port. It should look something like 192.168.1.125:11434'
            input(name: 'serverIP', type: 'string', required:true, description: 'Please enter the IP and port of your server.', submitOnchange: true,)
            input(name: 'openWebUIToken', type: 'string', required:true, description: 'Please enter the API Token obtained from Open WebUI.', submitOnchange: true,)
        }
    }
}

def setupParms() {
    options = getModels()
    toolsOptions = getToolID()
    logger(" setupParms() $options", 'debug')
    dynamicPage(name: 'setupParms', title: 'Setup Open WebUI integration Parms', uninstall: false, install: false, nextPage: "deviceSelection", submitOnChange: true)
    { 
        if (options != null) {
            section('<b>Model</b>') {
                paragraph 'Please select the desired model from the selection below'
                input(name: 'model', type: 'enum', required:true, description: 'Please select the downloaded LLM model you wish to use.', multiple:false, submitOnChange: true,
                    options: options.sort() , width: 8, height: 1)
//                paragraph ' The model supports '+state.modelCapabilities+' capabilities'
                input(name: 'knowledgeName', title: "Please specify the name of the Knowledge base for Hubitat", type: 'string', required:true, defaultValue: "Hubitat_RAG" , description: 'This will be cretaed and updated in Open WebUI with context information from your Hub') 
                input(name: 'toolID', type: 'enum', required:true, description: 'Please select the tool setup in Open WebUI.', multiple:false, submitOnChange: true,
                    options: toolsOptions.sort() , width: 8, height: 1)
/*                if (state.modelCapabilities.contains("thinking")) {
                    input(name: 'showThinking', title: "Would you like to hide the LLM thinking from the response", type: 'bool', defaultvalue: false , description: 'If you LLM Mode supports thinking this allow it the thinking to be hidden')    
                } */
            }   
        }        
/*        section('<b>Manage Models in Ollama</b>') {
                paragraph "Enter the LLM Identifier in the field below and then click on the button for the action you want to take"
                input(name: 'modelID', type: 'string', required:false, description: 'Please enter a valid LLM idenitifier such as qwen3:latest .', submitOnChange: true)
                input "modelPull" , "button",  title: "Pull Model"
                input "modelDelete" , "button",  title: "Delete Model" // Needed call not supported
            } */
        
    }
}

def deviceSelection() {
    dynamicPage(name: 'deviceSelection', title: 'Select Devices to work with Ollama', uninstall: false, install: false, nextPage: "mainPage", submitOnChange: true)
    {
        section 
		{    
			paragraph "Hubitat devices shared to Open WebUI that your LLM will have access to."

			input "devices", "capability.*", title: "Select devices that will be made avaliable to your local AI", multiple: true, required: true

       	}
    }
}

def ragHandeling() {
    dynamicPage(name: 'ragHandeling', title: 'Manage Context data for Open Web UI ', uninstall: false, install: false, nextPage: "mainPage", submitOnChange: true)
    {
        section 
		{    
            paragraph "Click the button below to generate and update the RAG files in your Hubitat File Manager using the selected devices."
            input "generateButton", "button", title: "Generate RAG Files", submitOnChange: true
            input "uploadRAGButton", "button", title: "Upload RAG Files", submitOnChange: true
            input "uploadReIndex", "button", title: "Manually trigger re-index of Open WebUI Knowledge", submitOnChange: true
            input "createKnowledge", "button", title: "Create Open Webui Knowledge", submitOnChange: true
            
       	}

    }
}


def about() {
    dynamicPage(name: 'about', title: 'About Ollama Integration with HE', uninstall: false, install: false, nextPage: "mainPage")
    {
        section()
        {
            paragraph image: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg', 'Ollama Integration'
        }
        section('Support the Project')
        {
            paragraph 'Ollama is provided free for personal and non-commercial use.  I have worked on this app in my free time to fill the needs I have found for myself and others like you.  I will continue to make improvements where I can. If you would like you can donate to continue to help with development please use the link below.'
            href(name: 'donate', style:'embedded', title: "Consider making a \$5 or \$10 donation today to support my ongoing effort to continue improving this integration.", url: 'https://www.paypal.me/mavrrick58')
            paragraph("<style>/* The icon */ .help-tip{ 	position: absolute; 	top: 50%; 	left: 50%; 	transform: translate(-50%, -50%); 	margin: auto; 	text-align: center; 	border: 2px solid #444; 	border-radius: 50%; 	width: 40px; 	height: 40px; 	font-size: 24px; 	line-height: 42px; 	cursor: default; } .help-tip:before{     content:'?';     font-family: sans-serif;     font-weight: normal;     color:#444; } .help-tip:hover p{     display:block;     transform-origin: 100% 0%;     -webkit-animation: fadeIn 0.3s ease;     animation: fadeIn 0.3s ease; } /* The tooltip */ .help-tip p {    	display: none; 	font-family: sans-serif; 	text-rendering: optimizeLegibility; 	-webkit-font-smoothing: antialiased; 	text-align: center; 	background-color: #FFFFFF; 	padding: 12px 16px; 	width: 178px; 	height: auto; 	position: absolute; 	left: 50%; 	transform: translate(-50%, 5%); 	border-radius: 3px; /* 	border: 1px solid #E0E0E0; */ 	box-shadow: 0 0px 20px 0 rgba(0,0,0,0.1); 	color: #37393D; 	font-size: 12px; 	line-height: 18px; 	z-index: 99; } .help-tip p a { 	color: #067df7; 	text-decoration: none; } .help-tip p a:hover { 	text-decoration: underline; } /* The pointer of the tooltip */ .help-tip p:before { 	position: absolute; 	content: ''; 	width: 0; 	height: 0; 	border: 10px solid transparent; 	border-bottom-color:#FFFFFF; 	top: -9px; 	left: 50%; 	transform: translate(-50%, -50%); }  /* Prevents the tooltip from being hidden */ .help-tip p:after { 	width: 10px; 	height: 40px; 	content:''; 	position: absolute; 	top: -40px; 	left: 0; } /* CSS animation */ @-webkit-keyframes fadeIn {     0% { opacity:0; }     100% { opacity:100%; } } @keyframes fadeIn {     0% { opacity:0; }     100% { opacity:100%; } }</style><div class='help-tip'><p>This is the inline help tip! It can contain all kinds of HTML. Style it as you please.<br /><a href='#'>Here is a link</a></p></div>")
        }
    }
}

String initializeAPIEndpoint() {
    if(!state.accessToken) {
        if(createAccessToken() != null) {
            state.endpoint = getApiServerUrl()
            state.localAPIEndpoint = getFullLocalApiServerUrl()
            state.remoteAPIEndpoint = getFullApiServerUrl()
        }
    }
    return state.accessToken
}

/* Pages */
Map pageDisableAPI() {
    dynamicPage(name: "pageDisableAPI") {
        section() {
            if (state.accessToken != null) {
                state.accessToken = null
                state.endpoint = null
                paragraph("SUCCESS: API Access Token REVOKED! Tap Done to continue")
            }
        }
    }
}

Map pageEnableAPI() {
    dynamicPage(name: "pageEnableAPI", title: "", nextPage: "setupParms") {
        section() {
            if(state.accessToken == null) {
                initializeAPIEndpoint()
            }
            if (state.accessToken == null){
                paragraph("FAILURE: API NOT Initialized!")
            } else {
                paragraph("SUCCESS: API Initialized! Tap Done to continue")
            }
        }
    }
}



def installed() {
    log.debug "Installed with settings: ${settings}"
    state.isInstalled = true
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
    addChildDevice('Mavrrick', 'Ollama Driver', "Ollama-OpenWebUi" , location.hubs[0].id, [
            'name': 'Open WebUI',
            'label': 'Open WebUI',
             'data': [
//                'server': settings.serverIP,
//                'model': settings.model
             ],
             'completedSetup': true,
         ])
    createKnowledge()
    generateButton()
    markdownID = uploadRAGButton("RAG.md", "markdown")
    jsonID = uploadRAGButton("RAG.json", "json")
    state.markdownID = markdownID
    state.jsonID = jsonID
    addToKnowledge(markdownID)
    addToKnowledge(jsonID)
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) //? settings.configLoggingLevelIDE.toInteger() : 3
    generateButton()
    deleteRAGFile(state.markdownID)
    deleteRAGFile(state.jsonID)
    markdownID = uploadRAGButton("RAG.md", "markdown")
    jsonID = uploadRAGButton("RAG.json", "json")
    state.markdownID = markdownID
    state.jsonID = jsonID
    fileStatus(markdownID)
    addToKnowledge(markdownID)
    fileStatus(jsonID)
    addToKnowledge(jsonID)
    knowledgeReindex()
    clearConversation()

}

def uninstalled() {
    deleteRAGFile(state.markdownID)
    deleteRAGFile(state.jsonID)
    deleteKnowledge(state.knowledgeID)
    knowledgeReindex()
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
//def logger(msg, level = 'debug') {
//    log.debug "attempting to log ${msg} with ${level} level"
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
    } else if (button == "generateButton") {
    generateButton()
    } else if (button == "uploadRAGButton") {
    uploadRAGButton()
    }else if (button == "createKnowledge") {
    createKnowledge()
    }
}

///////////////////////////////////////////
// Helper methods for certain tasks // 
///////////////////////////////////////////


private String escapeString(String str) {
    //logger("$str", "info")
    if (str) {
//        str = str.replaceAll('["', "") // Escape spaces.
//        str = str.replaceAll(" ]", "") // Escape commas.
//        str = str.replaceAll("=", "\u003D") // Escape equal signs.
//        str = str.replaceAll('\"', "\u0022") // Escape double quotes.
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
    String token = "Bearer " + openWebUIToken    
    List models = []
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/models',
        headers: ['Authorization': token]    
        ]
    logger("getModels(): Params are  :${params}", 'debug')
    
//    try {
    
        httpGet(params) { resp ->      
            logger("getModels(): Response Data is ${resp.data.data.id}", 'debug')
            if (resp.status == 200) {
                logger("getModels(): Sucesssfull Poll. Parsing data to apply to device", 'debug')
                resp.data.data.forEach {
                    if (it.object == "model") {
                    logger("getModels(): Adding model ${it.name} to list", 'debug')
                    models.add(it.name)
                    }
                }
            }
/*        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}" */
    } 
    logger("getModels(): list of loaded models are ${models}", 'debug')
    return models
}

def getToolID() {
    String token = "Bearer " + openWebUIToken    
    List toolID = []
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/v1/tools/',
        headers: ['Authorization': token]    
        ]
    logger("getToolID(): Params are  :${params}", 'debug')
    
        httpGet(params) { resp ->      
            if (resp.status == 200) {
                logger("getToolID(): Sucesssfull Poll. Parsing data to apply to device", 'debug')
                resp.data.forEach {
                    logger("getToolID(): Adding model ${it.id} to list", 'debug')
                    toolID.add(it.id)
                }
            }

    } 
    logger("getModels(): list of loaded models are ${models}", 'debug')
    return toolID
}

/* 
    Create Knowledge
*/
def createKnowledge() {
    bodyJson = '{"name": "'+knowledgeName+'","description": "Knowledge created by Hubitat Integration"}'
    
    String token = "Bearer " + openWebUIToken    
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/v1/knowledge/create',
        headers: ['Authorization': token],  
        body :   bodyJson,
        ]
//    logger("createKnowledge(): Params are  :${params}", 'debug')
     
    
//    try {
    
        httpPostJson(params) { resp ->       
            logger("createKnowledge(): Response Data is ${resp.data.id}", 'debug')
            if (resp.status == 200) {
                logger("createKnowledge(): Sucesssfull Poll. Parsing data to apply to device", 'debug')
                String id = resp.data.id
                state.knowledgeID = id

                }
            }
/*        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}" 
    } */
    logger("createKnowledge(): Knowlege ID stored as  ${state.knowledgeID}", 'debug')
}


//   Check file status
def fileStatus(fileID) {
//    bodyJson = '{"file_id": "'+fileID+'"}'
    
    String token = "Bearer " + openWebUIToken    
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/v1/files/'+fileID+'/process/status',
        headers: ['Authorization': token],  
//        body :   bodyJson,
        ]
//    logger("addToKnowledge(): Params are  :${params}", 'debug')
    log.info "fileStatus(): Params are  :${params}" 
    
    try {
    
     httpGet(params) { resp ->       
         logger("fileStatus(): Response Data is ${resp.data}", 'debug')
         if (resp.status == 200) {
             logger("fileStatus(): file process status is ${resp.data.status}", 'debug')
             if (resp.data.status == "pending") {
                 pauseExecution(1000)
                 fileStatus(fileID) 
             } else if (resp.data.status == "completed") {
                 logger("fileStatus(): File finished being process and ready to move to next step", 'debug')
             }             
         }
     }
//    } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}" 
    } 
}

/* 
    Add uploaded file to knowledge
*/
def addToKnowledge(fileID) {
    bodyJson = '{"file_id": "'+fileID+'"}'
    
    String token = "Bearer " + openWebUIToken    
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/v1/knowledge/'+state.knowledgeID+'/file/add',
        headers: ['Authorization': token],  
        body :   bodyJson,
        ]
//    logger("addToKnowledge(): Params are  :${params}", 'debug')
    log.info "addToKnowledge(): Params are  :${params}" 
    
    try {
    
     httpPostJson(params) { resp ->       
         logger("addToKnowledge(): Response Data is ${resp.data}", 'debug')
         if (resp.status == 200) {
             logger("addToKnowledge(): Sucesssfully added device to Knowledge Base", 'debug')
         }
     }
//    } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}" 
    } 
}

/* 
    Delete RAG file for reload
*/
def deleteRAGFile(fileID) {
    
    String token = "Bearer " + openWebUIToken    
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/v1/files/'+fileID,
        headers: ['Authorization': token]  
        ]
    logger("deleteRAGFile(): Params are  :${params}", 'debug')
    
//    try {
    
        httpDelete(params) { resp ->       
            if (resp.status == 200) {
                logger("deleteRAGFile(): Delete of Knowledge file successfull", 'debug')
                }
            }
/*        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}" 
    } */
}

/* 
    Delete Knowledge file for reload
*/
def deleteKnowledge(fileID) {
    
    String token = "Bearer " + openWebUIToken    
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/v1/knowledge/'+fileID+'/delete',
        headers: ['Authorization': token],  
        ]
    logger("deleteRAGFile(): Params are  :${params}", 'debug')
    
//    try {
    
        httpDelete(params) { resp ->       
            if (resp.status == 200) {
                logger("deleteRAGFile(): Delete of Knowledge successfull", 'debug')

                }
            }
/*        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}" 
    } */
}

/* 
    Trigger Re-Index of RAG data
*/
def knowledgeReindex() {
    
    String token = "Bearer " + openWebUIToken    
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/v1/knowledge/reindex',
        headers: ['Authorization': token],  
        ]
    logger("knowledgeReindex(): Params are  :${params}", 'debug')
    
//    try {
    
        httpPost(params) { resp ->       
            if (resp.status == 200) {
                logger("knowledgeReindex(): ReIndex successful", 'debug')
                }
            }
/*        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}" 
    } */
}


/* 
    Pull Models
*/

def pullModels() {
    bodyparm = '{"name": "'+modelID+'","stream":false}'
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/pull',
        contentType: "application/json",
        body : bodyparm ,
        timeout : 300
    ]
    logger("pullModels(): Attempting to make puall call with parms ${params}", 'info')
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
    Pull Models
*/

def deleteModels() {
    bodyparm = '{"name": "'+modelID+'"}'
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/delete',
        contentType: "application/json",
        body : bodyparm
    ]
    logger("deleteModels(): Attempting to make delete call with parms ${params}", 'info')
        try {
        httpDelete(params) { resp ->
            logger("deleteModels(): Response Data is ${resp.data}", 'info')
            if (resp.status == 200) {
                logger("deleteModels(): Sucessfully Pulled Model", 'info')
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

/* 
   Show Model information to retrieve Capabilities
*/

def loadModel(until) {
    List modelCapabilities = []
    bodyparm = '{"model": "'+model+'","messages": [],"keep_alive": '+until+'}'
        def params = [
        uri   : "http://"+serverIP,
        path  : '/api/chat',
        contentType: "application/json",
        body : bodyparm
    ]
        try {
        httpPost(params) { resp ->
            logger("loadModel(): Response Data is ${resp.data}", 'trace')
            
/*            if (resp.status == 200) {
                modelCapabilities = resp.data.capabilities
                logger("showModels(): Capabilities found ${resp.data.capabilities}", 'info')
            } */
        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
    }
    if (until == 0) {
        logger("loadModel(): Model Unloaded", 'info')    
    } else {
        logger("loadModel(): Model loaded", 'info')
    }
}

//
// Ollama Conversation Procesing
//

def chat(question) {
//    def conversationAdd = null
    def toolResponse = null

    logger("chat(): Conversation History : ${conversation}", 'trace')
    if (conversation == "") {
        conversation  = '{"role":"user","content":"'+question+'"}'
    } else {
    conversation  =  conversation + ',{"role":"user","content":"'+question+'"}' 
    }
    logger("chat(): Current Submited Conversation : ${conversation}", 'trace')
    String bodyParm = ""
    
    bodyParm = '{"model":"'+model+'","messages":['+conversation+'],"files": [{"type": "collection", "id": "'+state.knowledgeID+'"}], "tool_ids":["'+toolID+'"]}'    


    String token = "Bearer " + openWebUIToken
    
    def params = [
        uri   : "http://"+serverIP,
        path  : '/api/chat/completions',
        contentType: "application/json",
        headers: ['Authorization': token, 'Content-Type': 'application/json'],
        timeout: 600, 
        body: bodyParm
    ] 
    logger("chat(): Attempting to send request to ollama server at ${serverIP}, parms: ${params}", 'debug')
    sendEvent(name: "prompt", value: question)
    try {
        httpPostJson(params) { resp ->
/*            resp.data.each {
                logger("chat(): Response Usage Information is ${it.key}: ${it.value}", 'trace')    
            } */
/*            resp.data.usage.each {
                logger("chat(): Response Usage Information is ${it.key}: ${it.value}", 'trace')    
            } */
            logger("chat(): Response Data sources document ${resp.data.sources.document[0]}", 'trace')
            
            if (resp.status == 200) {
                    
                logger("chat(): Successful query to Ollama. Parsing response data for action", 'info')
                resp.data.sources.each {
//                    logger("chat(): ${it.keySet()}", 'info')
                    if (it.containsKey("tool_result")) {                    
                        logger("chat(): Found Tool result", 'trace')
                        toolResponse = it.document[0]
//                        conversationAdd = JsonOutput.toJson(it.value)
                       /* if (it.value.tool_calls) {
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
                        } */
                    } else {
                    }
                }
                logger("chat(): Usage information ${resp.data.usage}", 'info')
//                logger("chat(): Tokens per second ${resp.data.usage."response_token/s"}", 'info')
//                logger("chat(): Tokens in prompt ${resp.data.usage.prompt_eval_count.toInteger()}, Tokens in Reponse ${resp.data.usage.completion_tokens.toInteger()}", 'info')
//                logger("chat(): Curent Conversation value is ${conversation}", 'info')
            }               
        } 
    } catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
    } 
//    conversation  = conversation + ',' + resp.data.sources.document
    logger("chat(): tool conversation add ${toolResponse}", 'debug')
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
        "name": "control_device",
        "description": "Change a device to a given state",
        "parameters": {
          "type": "object",
          "properties": {
            "device": {
              "type": "array",
              "description": "Device(s) name to control"
            },
            "room": {
              "type": "string",
              "description": "Area, Room, or grouping of devices"
            },
            "state": {
              "type": "string",
              "description": "The requested state for a device to be changed to"
            },
            "stateType": {
              "type": "string",
              "description": "This is the type of state being asked to change, e.g. switch, turn, brightness, color, temperature, or color temperature"
            }
          },
          "required": ["state","device","stateType"]
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

/*
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
              "description": "This is the type of state being asked to change, e.g. switch, turn, brightness, color, temperature, or color temperature"
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
} */

def structuredOutputJSON() {
    def structuredJSON = '''"format": {
    "type": "object",
    "properties": {
      "deviceName": {
        "type": "array"
      },
       "Value": {
        "type": "string"
      },
       "attribute": {
        "type": "string"
      },
       "room": {
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
    logger("control_device(): Control request made with the following vaules ${parms}", 'info')
    newValue = parms.value
    value  = ""
    unit = ""
    String deviceList = null
    def matchedDevices = buildDeviceList(parms.device.split(','))    
    logger("control_device(): device matches ${matchedDevices}", 'info')
    switch(parms.command){
        case "on":
        case "off":
        matchedDevices.each {
//            if (parms.device.contains(it.toString())) {
                it."${parms.command}"()
                logger("control_device(): ${it} has been switched ${parms.command}", 'info')
                if (deviceList == null) {
                    deviceList = it
                } else {
                    deviceList = deviceList+', '+it
                }
            }
//        }
        responseMessage = "${deviceList} was switched ${parms.command}"
        break;
        case "setBrightness":
        case "brightness":
        case "setLevel":
        case "level":
        matchedDevices.each {
//            if (parms.device.contains(it.toString())) {
                dimNum = newValue.replace("%", "").toInteger()
                it.setLevel(dimNum, 0)                
                logger("control_device(): Dimming  ${it} to  ${dimNum}%", 'info')
                if (deviceList == null) {
                    deviceList = it
                } else {
                    deviceList = deviceList+', '+it
                }
//            }
        responseMessage = "${deviceList} brightness has changed to ${newValue}"
        }
        break;
        case "color":
        case "setColor":
        case "setcolor":
        matchedDevices.each {
//            if (parms.device.contains(it.toString())) {
                logger("control_device(): Set Color to ${newValue} in HSL ${colorNameToHsl[newValue]}", 'info')
                it.setColor(colorNameToHsl[newValue])                
                logger("control_device(): Setting color to ${newValue}", 'info')
                if (deviceList == null) {
                    deviceList = it
                } else {
                    deviceList = deviceList+', '+it
                }
 //           }
            responseMessage = "${deviceList} color has changed to ${newValue}"
        }
        break;
        case "color temperature":
        case "setColorTemperature":
        matchedDevices.each {
//           if (parms.device.contains(it.toString())) {
                logger("control_device(): Set Color Temperature to ${newValue}", 'info')
                it.setColorTemperature(newValue.toInteger())                
                logger("control_device(): Setting color temperature to ${newValue}", 'info')
                if (deviceList == null) {
                    deviceList = it
                } else {
                    deviceList = deviceList+', '+it
                }
            }
            responseMessage = "${deviceList} color temperature has been changed to ${parms.command}"
//        }
        break;
        default:
        logger("control_device(): Control request with command: ${parms.command}. Please report to developer to add this handler the routine", 'info') 
        break;
    }
//    responseMessage = "${deviceList} ${parms.command} changed to ${parms.command}"
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
    def matchedDevices = buildDeviceList(parms.device.split(','))  
    logger("device_state_lookup(): StateType = ${parms.stateType}: Device = ${parms.device}.", 'info')
    switch(parms.stateType){
        case "temperature":
        logger("device_state_lookup(): stateType matched on temperature", 'info')
        matchedDevices.each {
//            if (it.toString() == parms.device.toString()) { 
                value = it.currentTemperature
                unit = "Degrees"
                logger("device_state_lookup(): State is ${it} State Value is ${value}", 'info')
            }
            responseMessage = "Temperature on ${parms.device}  is ${value} ${unit}"
//        }
        break;
        case "switch":
        case "on":
        case "off":
        matchedDevices.each {
//            if (it.toString() == parms.device.toString()) { 
                value = it.currentSwitch
                logger("device_state_lookup() State Value is ${value}", 'info')
            }
            responseMessage = "${parms.device} is ${value} ${unit}"
//        }  
        break;
        case "humidity":
        matchedDevices.each {
//            if (it.toString() == parms.device.toString()) { 
                value = it.currentHumidity
                unit = "%"
                logger("device_state_lookup(): State is ${it} State Value is ${value}", 'info')
            }
            responseMessage = "The humidity on ${parms.device} is ${value} ${unit}"
//        }
        break;
        case "brightness":
        case "setLevel":
        case "level":
        matchedDevices.each {
//            if (it.toString() == parms.device.toString()) { 
                value = it.currentLevel
                unit = "%"
                logger("device_state_lookup(): State is ${it} State Value is ${value}", 'info')
            }
            responseMessage = "Brightness on the ${parms.device} is ${value} ${unit}"
//        }
        break;
        case "presence":
        matchedDevices.each {
//            if (it.toString() == parms.device.toString()) { 
                value = it.currentPresence
                unit = ""
                logger("device_state_lookup(): State is ${it} State Value is ${value}", 'info')
            }
            responseMessage = "${parms.device} is ${value} ${unit}"
//        }
        break;
        case "contact":
        case "open/close":
        case "close":
        case "open":
        matchedDevices.each {
//            if (it.toString() == parms.device.toString()) { 
                value = it.currentContact
                unit = ""
                logger("device_state_lookup(): State is ${it} State Value is ${value}", 'info')
            }
            responseMessage = "${parms.device} is ${value}"
//        }
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

def lookup_all_Devices(parms) {
    responseMessage = "lookup_all_Devices() This hasn't been implemented yet"
    return responseMessage
}

///
//  Helper Method to pass context when a new Conversation is started
///

def clearConversation() { 
    conversation = ""
}

def buildDeviceList(passedDevices) {
    def matchedDevices = []
    logger("buildDeviceList(): devices to be matched ${passedDevices}", 'info')
    devices.each {
        deviceMatcher = it
        deviceCompare = it.toString()
        passedDevices.each {
            if (it.equalsIgnoreCase(deviceCompare)) {
                matchedDevices.add(deviceMatcher)
            }
        }
    }
    return matchedDevices
}

// Handler for the "Generate RAG Files" button
def generateButton() {
    log.info "Generating RAG files for selected devices..."
    
    // Check if the user selected any devices
    if (!devices) {
        log.warn "No devices were selected."
        return
    }

    // Get the device list from the user input
    log.debug "Device Count is ${devices.size()}"
    def deviceList = devices.collect { device ->
        [
            "name": device.currentDeviceType,
            "label": device.displayName,
            "type": device.typeName,
            "id": device.id,
            "room": device.getRoomName(),
            "capabilities": device.capabilities.collect { it.name },
            "attributes": device.currentStates.collectEntries { [(it.name): it.value] },
            "commands": device.getSupportedCommands().collect { ["command": it.name] }
        ]
    }
    
    def markdownOutput = generateConsolidatedMarkdown(deviceList)
    def jsonOutput = generateConsolidatedJson(deviceList)
    

    log.debug ("Saving Rag file output")
//    String listJson = "["+JsonOutput.toJson(state.diyEffects)+"]" as String
    uploadHubFile("RAG.md",markdownOutput.getBytes())
    uploadHubFile("RAG.json",jsonOutput.getBytes())
    
    // Write the files using the File Manager Device
/*    if (fileManagerDevice) {
        fileManagerDevice.writeFile(fileName: "all_devices_data.md", content: markdownOutput)
        fileManagerDevice.writeFile(fileName: "all_devices_data.json", content: jsonOutput)
    } else { 
        log.error "File Manager Device not configured."
    } */
//    log.debug "Markdown Output to file would be: ${markdownOutput}"
//    log.debug "Json Output to file would be: ${jsonOutput}"
    
}

// Helper function to generate consolidated Markdown content
def generateConsolidatedMarkdown(deviceList) {
    def markdown = new StringBuilder(262144)
    markdown << "# Home Automation Devices\n\n"
    deviceList.each { device ->
        def roomName = device.room ?: 'Not Specified'
        markdown << "---\n\n"
        markdown << "# Device: ${device.label}\n"
        markdown << "**Type:** ${device.type}\n"
        markdown << "**Location:** ${roomName}\n"
        markdown << "**Unique ID:** `${device.id}`\n\n"
        markdown << "## Description\n"
        markdown << "A device of type `${device.type}` located in the ${roomName} area.\n\n"
        markdown << "## Capabilities\n"
        device.capabilities?.each { cap ->
            markdown << "- **${cap}:** "
            switch(cap) {
                case 'Switch': markdown << "Can be turned `on` and `off`.\n"; break
                case 'SwitchLevel': markdown << "Brightness level can be adjusted.\n"; break
                case 'ColorControl': markdown << "Supports color setting.\n"; break
                case 'ColorTemperature': markdown << "Supports setting color temperature.\n"; break
                case 'PowerMeter': markdown << "Can measure power consumption.\n"; break
                case 'TemperatureMeasurement': markdown << "Can measure temperature.\n"; break
                case 'PresenceSensor': markdown << "Can detect presence.\n"; break
                case 'Refresh': markdown << "Can update its status.\n"; break
                case 'Actuator': markdown << "Can perform actions.\n"; break
                case 'Notification': markdown << "Can send notifications.\n"; break
                default: markdown << "The device has the `${cap}` capability.\n"; break
            }
        }
        markdown << "\n"
/*        if (device.attributes) {
            markdown << "## Current State\n"
            device.attributes.each { attr, value ->
                if (value && !(value instanceof Map)) {
                    markdown << "- **${attr.capitalize()}:** `${value}`\n"
                }
            }
            markdown << "\n"
        } */
        if (device.commands) {
            markdown << "## Supported Command\n"
            device.commands.each { cmd ->
                markdown << "- `${cmd.command}()`\n"
            }
        }
    }
//    log.debug "Markdown Output to file would be: ${markdown}"
    return markdown.toString()
}

// Helper function to generate consolidated JSON content
def generateConsolidatedJson(deviceList) {
    def jsonArray = deviceList.collect { device ->
        def roomName = device.room ?: 'Not Specified'
        def supportedCommands = device.commands?.collect { cmd ->
            def commandInfo = [command: cmd.command]
            switch(cmd.command) {
                case 'setLevel':
                    commandInfo.parameters = [["name": "level", "type": "integer", "range": "0-100"]]
                    break
                case 'setColor':
                    commandInfo.parameters = [["name": "hue", "type": "float", "range": "0-360"], ["name": "saturation", "type": "integer", "range": "0-100"], ["name": "level", "type": "integer", "range": "0-100"]]
                    break
                case 'setColorTemperature':
                    commandInfo.parameters = [["name": "temperature", "type": "integer", "range": "2000-9000"]]
                    break
                case 'setSaturation':
                    commandInfo.parameters = [["name": "saturation", "type": "integer", "range": "0-100"]]
                    break
                case 'PowerCyclePoePort':
                    commandInfo.parameters = [["name": "portId", "type": "integer"]]
                    break
                case 'SetPoePortState':
                    commandInfo.parameters = [["name": "portId", "type": "integer"], ["name": "state", "type": "string", "values": ["auto", "on", "off"]]]
                    break
                case 'SetPortName':
                    commandInfo.parameters = [["name": "portId", "type": "integer"], ["name": "name", "type": "string"]]
                    break
                case 'deviceNotification':
                    commandInfo.parameters = [["name": "text", "type": "string"]]
                    break
                default:
                    commandInfo.parameters = []
                    break
            }
            commandInfo
        }

        [
            device_name: device.label,
            unique_id: device.id,
            device_type: device.type,
            location: roomName,
//            current_state: device.attributes,
            supported_capabilities: device.capabilities,
            supported_commands: supportedCommands
        ]
    }
    def jsonBuilder = new JsonBuilder(jsonArray)
    return JsonOutput.prettyPrint(jsonBuilder.toString())
}

// Helper function to upload RAG data to OpenWeb UI
def uploadRAGButton(fileName, type){
    String token = "Bearer " + openWebUIToken  
//def uploadFile(String api_url, String file_data_base64, String filename, String form_field_name, String mime_type, String bearer_token, Map additional_params = [:]) {
    log.info "Preparing to upload file to Open Web UI API..."
    def id = ""
    def body = new StringBuilder()
    def boundary = "----HubitatBoundary${java.util.UUID.randomUUID().toString()}"
    def form_field_name = "file"
    // Add the file part
    body << "--${boundary}\r\n"
    body << "Content-Disposition: form-data; name=\"${form_field_name}\"; filename=\"${fileName}\"\r\n"
//    body << "Content-Type: ${mime_type}\r\n"
    body << "\r\n"
    body << new String(downloadHubFile(fileName))
//    body << new String(file_data_base64.decodeBase64()) // Decode Base64 data
    body << "\r\n"

    // Add the closing boundary
    body << "--${boundary}--"

    def params = [
//        method: "POST",
        uri: "http://"+serverIP,
        path: "/api/v1/files/",
//        headers: ['Authorization': token] 
        headers: [
            "Content-Type": "multipart/form-data; boundary=${boundary}",
//            "Content-Length": body.toString().getBytes("UTF-8").length,
            "Authorization": token // <--- ADDED HEADER
        ],
        body: body.toString()
    ]
//    log.info "Parms for upload are ${params}..."
    log.info "Attempting to upload file to Open WebUI..."
    httpPost(params) { resp ->
        logger("uploadRAGButton(): Response Data is ${resp.data}", 'info')
        if (resp.status == 200) {
            logger("uploadRAGButton(): Sucessfully uploaded RAG files with id: ${resp.data.id} ", 'info')
            id = resp.data.id
/*            if (type == "md"){
                state.markdownID = resp.data.id
            } else if (type == "json")  {
                state.jsonID = resp.data.id
            } */
        }
    }
    return id
}

/*	catch (e) {
		log.error "Error installing file: ${e}"
	}
} */

    
/**
*
* Routine called by using inbound Webook with at 
*
*/

def webHook () {
    log.debug("Processing a webHook() $params")
    switch(params.tool_call) {
        case 'control_device':
            message = control_device(params)
            break
        case 'device_state_lookup':
            message = device_state_lookup(params)
            break
    }
    
    def response = [:]
    
    response.status = "success"
    response.message = message
    response.data = "This is test data"
    
    return response 
}





    
    
