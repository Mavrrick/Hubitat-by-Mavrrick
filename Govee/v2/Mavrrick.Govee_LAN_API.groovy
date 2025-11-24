library (
 author: "Mavrrick",
 category: "Govee",
 description: "Govee LAN API",
 name: "Govee_LAN_API",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

//////////////////////////////
// Standard device Commands //
//////////////////////////////

def lanOn() {
    sendCommandLan(GoveeCommandBuilder("turn",1, "turn"))
    runInMillis(500, 'devStatus')
    if (descLog) log.info "${device.label} was turned on."  
}

def lanOff() {
    sendCommandLan(GoveeCommandBuilder("turn",0, "turn"))
    runInMillis(500, 'devStatus')
    if (descLog) log.info "${device.label} was turned off."
} 

def lanCT(value, level, transitionTime) {
    int intvalue = value.toInteger()
    sendCommandLan(GoveeCommandBuilder("colorwc",value, "ct"))
    if (level != null) lanSetLevel(level,transitionTime);
    sendEvent(name: "colorMode", value: "CT")
    runInMillis(500, 'devStatus')   
    if (effectNum != 0){
        sendEvent(name: "effectNum", value: 0)
        sendEvent(name: "effectName", value: "None") 
    }
	setCTColorName(intvalue)
}

def lanSetLevel(float v,duration = 0){
    int intv = v.toInteger()
    switch(true) {
        case intv > 100:
        	if (debugLog) {log.debug ("lanSetLevel(): Value of ${v} is over 100. Setting to 100")};
            intv = 100;
        break;
        case intv < 0:
        	if (debugLog) {log.debug ("lanSetLevel(): Value of ${v} is below 0. Setting to 0")};
            intv = 0;
        break;
        default: 
            if (debugLog) {log.debug ("lanSetLevel(): Setting Level to ${v}")};
        break;
    } 
    if (duration>0){
        int intduration = duration.toInteger()
    runInMillis(500, 'devStatus')
        fade(intv,intduration)
    }
    else {
        lanSetLevel2(intv)
    }
}

def lanSetLevel2(int v){
    if (descLog) log.info "${device.label} Level was set to ${v}%"      
    sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
    runInMillis(500, 'devStatus')
}

def fade(int v,float duration){
    unschedule(fadeUp)
    unschedule(fadeDown)
    int curLevel = device.currentValue("level")
    if (v < curLevel){
    float fadeRep = (curLevel-v)/fadeInc
    float fadeInt = (duration*1000)/fadeRep
    fadeDown(curLevel, v, fadeRep, fadeInt)
        }
    else if (v > curLevel){
    float fadeRep = (v-curLevel)/fadeInc
    float fadeInt = (duration*1000)/fadeRep
    fadeUp(curLevel, v, fadeRep, fadeInt)
        }
    else {
        if (debugLog) {log.debug "fade(): Level is not changing"}
    }
}

def fadeDown( int curLevel, int level, fadeInt, fadeRep) {
    if (debugLog) {log.debug "fadeDown(): curLevel ${curLevel}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
    int v = (curLevel-fadeInc).toInteger()
    if (v <= level) {
        if (debugLog) {log.debug "Final Loop Setting level to ${level}"}
        if ( level == 0 ) {
            log.debug "fadeDown(): to off"
            off()
        } else {
            log.debug "fadeDown(): Final fade to ${level}"
            sendCommandLan(GoveeCommandBuilder("brightness",level, "level")) 
            runInMillis(500, 'devStatus')
        }
    } else {
        log.debug "fadeDown(): Fade to ${v}"
            sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
            runInMillis(500, 'devStatus')
            def int delay = fadeRep
            if (debugLog) {log.debug "fadeDown(): delay ia ${delay}"}
            if (debugLog) {log.debug "fadeDown(): executing loop to fadedown() with values curLevel ${v}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
            runInMillis(delay, fadeDown, [data:[v ,level, fadeInt,fadeRep]])
    }
} 

def fadeUp( int curLevel, int level, fadeInt, fadeRep) {
    if (debugLog) {log.debug "fadeUp(): curLevel ${curLevel}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
    int v= (curLevel+fadeInc).toInteger()
    if (v >= level)    {
        log.debug "fadeUp(): Final fade to ${level}"
        sendCommandLan(GoveeCommandBuilder("brightness",level, "level"))
        runInMillis(500, 'devStatus')
    }
    else {
        log.debug "fadeUp(): Fade to ${v}"
        def int delay= fadeRep
        if (debugLog) {log.debug "fadeUp(): delay ia ${delay}"}
        if (debugLog) {log.debug "fadeUp(): executing loop to fadeup() with values curLevel ${v}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
        sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
        runInMillis(500, 'devStatus')
        runInMillis(delay, fadeUp, [data:[v ,level, fadeInt,fadeRep]])
    }
} 

def lanSetEffect (effectNo) {
    effectNumber = effectNo.toString()
    lanScenes = loadSceneFile()
    if (descLog) log.info "${device.label} SetEffect: ${effectNumber}"
    if (lanScenes.keySet().contains(device.getDataValue("DevType"))) {
        tag = device.getDataValue("DevType")
    } else if (lanScenes.keySet().contains(device.getDataValue("deviceModel"))) {
        tag = device.getDataValue("deviceModel")
    } 
    if (debugLog) log.debug "${lanScenes.get("${tag}").keySet()}"
    if (lanScenes.get("${tag}").containsKey(effectNumber)) {
        String sceneInfo =  lanScenes.get("${tag}").get(effectNumber).name
        String sceneCmd =  lanScenes.get("${tag}").get(effectNumber).cmd
        if (debugLog) {log.debug ("setEffect(): Activate effect number ${effectNo} called ${sceneInfo} with command ${sceneCmd}")}
        if (debugLog) log.debug "Scene number is present"
        sendEvent(name: "colorMode", value: "EFFECTS")
        sendEvent(name: "effectName", value: sceneInfo)
        sendEvent(name: "effectNum", value: effectNumber)
        sendEvent(name: "switch", value: "on")
        String cmd2 = '{"msg":{"cmd":"ptReal","data":{"command":'+sceneCmd+'}}}'
        if (debugLog) {log.debug ("setEffect(): command to be sent to ${cmd2}")}
        sendCommandLan(cmd2)
   } else {
        if (debugLog) {log.debug ("setEffect(): Effect Number not found for built in scenes. Passing  ${effectNumber}to Activate DIY ")}
        lanActivateDIY(effectNumber)
        
        
/*        sendEvent(name: "effectNum", value: effectNumber)
        sendEvent(name: "switch", value: "on")
    // Cozy Light Effect (static Scene to very warm light)
    if (effectNo == 6) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Cozy Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        sendEvent(name: "colorTemperature", value: 2000)
	    setCTColorName(2000)
        sendEvent(name: "effectName", value: "Cozy")
    }
    // Sunrise Effect
    if (effectNo == 9) {
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",1, "level"))
        sendEvent(name: "level", value: 1)
        fade(100,1800)        
        sendEvent(name: "effectName", value: "Sunrise")
    }
    // Sunset Effect
    if (effectNo == 10) {
        sendCommandLan(GoveeCommandBuilder("colorwc",6500, "ct"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        fade(0,1800)
        sendEvent(name: "effectName", value: "Sunset")
    }
    // Warm White Light Effect (static Scene to very warm light)
    if (effectNo == 11) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Warm White Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",3500, "ct"))
        sendEvent(name: "colorTemperature", value: 3500)
	    setCTColorName(3500)
        sendEvent(name: "effectName", value: "Warm White")
    } 
    // Daylight Light Effect    
    if (effectNo == 12) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Daylight Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",5600, "ct"))
        sendEvent(name: "colorTemperature", value: 5600)
	    setCTColorName(5600)
        sendEvent(name: "effectName", value: "Daylight")
    }
    // Cool White Light Effect    
    if (effectNo == 13) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Cool White Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",6500, "ct"))
        sendEvent(name: "colorTemperature", value: 6500)
	    setCTColorName(6500)
        sendEvent(name: "effectName", value: "Cool White")
    }  
    // Night Light Effect   
    if (effectNo == 14) {
        if (debugLog) {log.debug ("setEffect(): Static Night Light Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        sendEvent(name: "colorTemperature", value: 2000)
	    setCTColorName(2000)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",5, "level"))
        sendEvent(name: "level", value: 5)
        sendEvent(name: "effectName", value: "Night Light")
    }
    // Focus Effect   
    if (effectNo == 15) {
        if (debugLog) {log.debug ("setEffect(): Focus Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",4500, "ct"))
        sendEvent(name: "colorTemperature", value: 4500)
	    setCTColorName(4500)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        sendEvent(name: "effectName", value: "Focus")
    } 
    // Relax Effect   
    if (effectNo == 16) {
        if (debugLog) {log.debug ("setEffect(): Static Relax Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:255, g:194, b:194], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        sendEvent(name: "effectName", value: "Relax")
    }
    // True Color Effect   
    if (effectNo == 17) {
        if (debugLog) {log.debug ("setEffect(): True Color Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",3350, "ct"))
        sendEvent(name: "colorTemperature", value: 3350)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "effectName", value: "True Color")
    }
    // TV Time Effect   
    if (effectNo == 18) {
        if (debugLog) {log.debug ("setEffect(): Static TV Time Effect Called. Calling CT Command directly")}        
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:179, g:134, b:254], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",45, "level"))
        sendEvent(name: "level", value: 45)
        sendEvent(name: "effectName", value: "TV Time")
    }
    // Plant Growth Effect   
    if (effectNo == 19) {
        if (debugLog) {log.debug ("setEffect(): Static Plant Growth Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:247, g:154, b:254], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",45, "level"))
        sendEvent(name: "level", value: 45)
        sendEvent(name: "effectName", value: "Plant Growth")
    } */
    } 
}

def lanSetNextEffect () {
    if (debugLog) {log.debug ("setNextEffect(): Current Color mode ${device.currentValue("colorMode")}")}
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) {log.debug ("setNextEffect(): current effectNum ${device.currentValue("effectNum")}")}
    if (device.currentValue("effectNum") == "0" || device.currentValue("effectNum") == device.getDataValue("maxScene")) {
        setEffect(1)
    } 
    else if (device.currentValue("effectNum") == "21") {
        if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
        setEffect(101) 
    } else {
        if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
        int nextEffect = device.currentValue("effectNum").toInteger()+1
        setEffect(nextEffect)
        }  
}

def lanSetPreviousEffect () {
        if (debugLog) {log.debug ("setPreviousEffect(): Current Color mode ${device.currentValue("colorMode")}")}
        unschedule(fadeUp)
        unschedule(fadeDown)
        if (device.currentValue("effectNum") == "0" || device.currentValue("effectNum") == "1") {
            setEffect(device.getDataValue("maxScene"))
            } else if (device.currentValue("effectNum") == 101) {
            setEffect(21) 
            } else {
                if (debugLog) {log.debug ("setNextEffect(): Increment to next scene}")}
                int prevEffect = device.currentValue("effectNum").toInteger()-1
                setEffect(prevEffect)
        }
}

def lanActivateDIY (diyActivate) {
    diyScenes = loadDIYFile()
    if (descLog) log.info "${device.label} ActivateDIY: ${diyActivate}"
    if (debugLog) {log.debug ("activateDIY(): Activate effect number ${diyActivate} from ${diyScenes}")}
        String diyEffectNumber = diyActivate.toString()
        String sceneInfo = diyScenes.get(device.getDataValue("deviceModel")).get(diyEffectNumber).name
        String sceneCmd = diyScenes.get(device.getDataValue("deviceModel")).get(diyEffectNumber).cmd
        if (debugLog) {log.debug ("activateDIY(): Activate effect number ${diyActivate} called ${sceneInfo} with command ${sceneCmd}")}
        sendEvent(name: "effectName", value: sceneInfo)
        sendEvent(name: "effectNum", value: diyEffectNumber)
        sendEvent(name: "switch", value: "on")
        sendEvent(name: "colorMode", value: "DIY_EFFECTS")
        String cmd2 = '{"msg":{"cmd":"ptReal","data":{"command":'+sceneCmd+'}}}'
        if (debugLog) {log.debug ("activateDIY(): command to be sent to ${cmd2}")}
        sendCommandLan(cmd2)
}

/////////////////////////////////////////////////////
// Helper methods to retrieve data or send command //
/////////////////////////////////////////////////////

def retrieveScenes() {
    state.scenes = [:]
    state.diyScene = [:]
    lanScenes = loadSceneFile()
    if (lanScenes != null) {
    if (debugLog) {log.debug ("retrieveScenes(): Scenes Keyset ${lanScenes.get(device.getDataValue("DevType"))}")}
    if (debugLog) {log.debug ("retrieveScenes(): Scenes Keyset ${lanScenes.keySet()}")}
    if (lanScenes.keySet().contains(device.getDataValue("DevType"))) {
        tag = device.getDataValue("DevType")
    } else if (lanScenes.keySet().contains(device.getDataValue("deviceModel"))) {
        tag = device.getDataValue("deviceModel")
    }                                                                  
    lanScenes.get("${tag}").each {
        if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
        if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}   
            state.scenes[it.getKey()] = it.value.name
        }
        state.scenes = state.scenes.sort()
    }

    if (parent.label == "Govee v2 Device Manager") {   
        diyScenes = loadDIYFile()
        if (debugLog) {log.debug ("retrieveScenes(): Retrieving DIYScenes from integration app ${diyScenes}")}
        if (diyScenes == null) {
            if (debugLog) {log.debug ("retrieveScenes(): Device has no DIY Scenes")}
        } else {        
      diyScenes.get(device.getDataValue("deviceModel")).each {   
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
                state.diyScene[it.getKey()] = it.value.name
            }
            state.diyScene = state.diyScene.sort()
        }
    } else {
        diyScenes = loadDIYFile()
        if (diyScenes.containsKey((device.getDataValue("deviceModel"))) == false) {
            if (debugLog) {log.debug ("retrieveScenes(): No DIY Scenes to retrieve for device")}    
        } else {
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
            diyScenes.get(device.getDataValue("deviceModel")).each {
                if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
                if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
                state.diyScene[it.getKey()] = it.value.name                
            }
            state.diyScene = state.diyScene.sort()
        }        
    }
    def le = new groovy.json.JsonBuilder(state.scenes + state.diyScene)
    sendEvent(name: "lightEffects", value: le)
}

void getDevType() {
    // Retrieve deviceModel once to avoid repeated calls
    def model = device.getDataValue("deviceModel")
    def newDevType = "Generic" // Default value

    // Use a Map for O(1) lookup, much more efficient than a long switch or repeated if/else if
    def modelToDevTypeMap = [
        // RGBIC_STRIP
        "H6117": "RGBIC_Strip", "H6163": "RGBIC_Strip", "H6168": "RGBIC_Strip",
        "H6172": "RGBIC_Strip", "H6173": "RGBIC_Strip", "H6175": "RGBIC_Strip",
        "H6176": "RGBIC_Strip", "H617A": "RGBIC_Strip", "H617C": "RGBIC_Strip",
        "H617E": "RGBIC_Strip", "H617F": "RGBIC_Strip", "H618A": "RGBIC_Strip",
        "H618B": "RGBIC_Strip", "H618C": "RGBIC_Strip", "H618E": "RGBIC_Strip",
        "H618F": "RGBIC_Strip", "H619A": "RGBIC_Strip", "H619B": "RGBIC_Strip",
        "H619C": "RGBIC_Strip", "H619D": "RGBIC_Strip", "H619E": "RGBIC_Strip",
        "H619Z": "RGBIC_Strip", "H61A0": "RGBIC_Strip", "H61A1": "RGBIC_Strip",
        "H61A2": "RGBIC_Strip", "H61A3": "RGBIC_Strip", "H61A5": "RGBIC_Strip",
        "H61A8": "RGBIC_Strip", "H61A9": "RGBIC_Strip", "H61B2": "RGBIC_Strip",
        "H61C2": "RGBIC_Strip", "H61C3": "RGBIC_Strip", "H61C5": "RGBIC_Strip",
        "H61E1": "RGBIC_Strip", "H61E0": "RGBIC_Strip", "H6167": "RGBIC_Strip",
        "H616C": "RGBIC_Strip", "H616D": "RGBIC_Strip", "H616E": "RGBIC_Strip",

        // Hexa_Light
        "H6066": "Hexa_Light", "H606A": "Hexa_Light", "H6061": "Hexa_Light",

        // Other Specific Types
        "H6067": "Tri_Light", // Not added yet - consider if this should be 'null' or 'Generic' if truly not implemented
        "H6065": "Y_Light",
        "H6072": "Lyra_Lamp",
        "H607C": "Lyra_Pro", "H6079": "Lyra_Pro",
        "H6076": "Basic_Lamp",
        "H6078": "Cylinder_Lamp",
        "H6052": "Table_Lamp", "H6051": "Table_Lamp",
        "H6038": "Wall_Sconce", "H6039": "Wall_Sconce",
        "H6022": "Table_Lamp_2",

        // XMAS_Light
        "H70C1": "XMAS_Light", "H70C2": "XMAS_Light", "H70C4": "XMAS_Light",
        "H70C5": "XMAS_Light", "H70C7": "XMAS_Light", "H70C9": "XMAS_Light",
        "H70CB": "XMAS_Light",

        // Wall_Light_Bar
        "H610A": "Wall_Light_Bar", "H610B": "Wall_Light_Bar", "H6062": "Wall_Light_Bar",

        // TV_Light_Bar
        "H6046": "TV_Light_Bar", "H6056": "TV_Light_Bar", "H6047": "TV_Light_Bar",

        // Indoor_Pod_Lights
        "H6088": "Indoor_Pod_Lights", "H6087": "Indoor_Pod_Lights", "H608A": "Indoor_Pod_Lights",
        "H608B": "Indoor_Pod_Lights", "H608C": "Indoor_Pod_Lights",

        // Outdoor_Perm_Light
        "H705A": "Outdoor_Perm_Light", "H705B": "Outdoor_Perm_Light", "H705C": "Outdoor_Perm_Light",
        "H706A": "Outdoor_Perm_Light", "H706B": "Outdoor_Perm_Light", "H706C": "Outdoor_Perm_Light",

        // Outdoor_Pod_Light
        "H7050": "Outdoor_Pod_Light", "H7051": "Outdoor_Pod_Light", "H7052": "Outdoor_Pod_Light",
        "H7055": "Outdoor_Pod_Light",

        // Outdoor_Flood_Light
        "H7060": "Outdoor_Flood_Light", "H7061": "Outdoor_Flood_Light", "H7062": "Outdoor_Flood_Light",
        "H7065": "Outdoor_Flood_Light", "H7066": "Outdoor_Flood_Light",

        // Curtain_Light
        "H70B1": "Curtain_Light", "H70BC": "Curtain_Light",

        // Curtain_Light2
        "H70B3": "Curtain_Light2", "H70B4": "Curtain_Light2", "H70B5": "Curtain_Light2",

        // Outdoor_Wall_Light
        "H7075": "Outdoor_Wall_Light",

        // Net_Lights
        "H6811": "Net_Lights",

        // Galaxy_Projector
        "H6091": "Galaxy_Projector", "H6092": "Galaxy_Projector",

        // Outdoor_String_Light
        "H7020": "Outdoor_String_Light", "H7021": "Outdoor_String_Light", "H7028": "Outdoor_String_Light",
        "H7041": "Outdoor_String_Light", "H7042": "Outdoor_String_Light"
    ]

    newDevType = modelToDevTypeMap.get(model, "Generic") // If model is not a key, default to "Generic"

    if (debugLog) {
        log.debug("getDevType(): Model ${model} resolved to DevType: ${newDevType}")
    }

    device.updateDataValue("DevType", newDevType)
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
    return device.getDataValue("IP")+":"+commandPort()
}


def parse(message) {  
    log.error "Got something to parseUDP"
    valueMap = [:]
    def message2 = message.replaceAll(/\s+/, "")  
    message2.split(",").each{ item ->
        valueMap.put(item.substring(0,(item.indexOf(':'))),item.substring((item.indexOf(':')+1),item.length()))
        } 
    def utf8String = new String(valueMap.payload.decodeBase64(), "UTF-8")
    log.error "UDP Response -> Error Type: ${valueMap.type} Error Message: ${utf8String}"    
}

def loadSceneFile() {
    
    String name = lanScenesFile
    
    if (name == null) {
        if (debugLog) {log.debug "loadSceneFile: File name is null using default values"}
        name = "GoveeLanScenes_"+getDataValue("DevType")+".json"    
    } 
    try {
        byte[] dBytes = downloadHubFile(name)
        if (debugLog) {log.debug "File loaded starting parse."}
        tmpEffects = (new JsonSlurper().parseText(new String(dBytes))) as List
        scenes = tmpEffects.get(0)
        return scenes 
    }
    catch (Exception e) {      
        if (debugLog) {log.debug "loadSceneFile: ${e}"}
    }
}

def loadDIYFile() {
    byte[] dBytes
    try {
        dBytes = downloadHubFile("GoveeLanDIYScenes.json")
    }
    catch (Exception e) {
        if (debugLog) {log.debug "loadDIYFile: ${e}"}
    }
    if (dBytes != null) {
        tmpEffects = (new JsonSlurper().parseText(new String(dBytes))) as List
        if (debugLog) {log.debug "loadDIYFile: Loaded ${tmpEffects.get(0)} from GoveeLanDIYScenes.json"}
        diyEffects = tmpEffects.get(0)
        return diyEffects
    }
}

void devStatus() {    
        sendCommandLan(GoveeCommandBuilder("devStatus", null , "status"))
        if (debugLog) log.info "${device.label} status was requested."  
}

def ipLookup() {
    if (debugLog) {log.info("ipLookup: Looking up Alt ip of ${getDataValue("IP")}")}
    ipAddress = getDataValue("IP")
    return ipAddress
}

void lanAPIPost(data) {
    if (debugLog) {log.info("lanAPIPost: Processing update from LAN API. Data: ${data}")}
    if (data.onOff == 1) { onOffSwitch = on}
    if (data.onOff == 0) { onOffSwitch = off}
    
        if (onOffSwitch == "on") {
            if (onOffSwitch != device.currentValue("switch")) {
                if (debugLog) {log.info("lanAPIPost: Switch Changed to on.")}
                sendEvent(name: "switch", value: onOffSwitch)
            }
            if (data.brightness != device.currentValue("level")) {
                sendEvent(name: "level", value: data.brightness)
            } else {
                if (debugLog) {log.info("lanAPIPost: Brightness has not changed. Ignoring")}
            }
            if (data.colorTemInKelvin != device.currentValue("colorTemperature")) {
                sendEvent(name: "colorTemperature", value: data.colorTemInKelvin)
            } else {
                if (debugLog) {log.info("lanAPIPost: Color Temperature has not changed. Ignoring")}
            }
            rgb = []
            rgb = [data.color.r,data.color.g, data.color.b]
            hsv = hubitat.helper.ColorUtils.rgbToHSV(rgb)
            if (debugLog) {log.info("lanAPIPost: Hue is ${hsv.get(0)}.")}
            if (hsv.get(0) != device.currentValue("hue")) {
                sendEvent(name: "hue", value: hsv.get(0))
            } else {
                if (debugLog) {log.info("lanAPIPost: Color Hue has not changed. Ignoring")}
            } 
            if (debugLog) {log.info("lanAPIPost: Saturation is  is ${hsv.get(1)}.")}
            if (hsv.get(1) != device.currentValue("saturation")) {
                sendEvent(name: "saturation", value: hsv.get(1))
            } else {
                if (debugLog) {log.info("lanAPIPost: Color Saturation has not changed. Ignoring")}
            } 
        } else {
            if (onOffSwitch != device.currentValue("switch")) {
                if (debugLog) {log.info("lanAPIPost: Switch Changed to off.")}
                sendEvent(name: "switch", value: onOffSwitch)
            }
        }
}

void updateIPAdd(ipAddress) {
    if (debugLog) {log.info("updateIPAdd: New Ip Address fund for Device, Updating with ${ipAddress}")}
    device.updateDataValue("IP", ipAddress);
}

void retrieveIPAdd() {
    if (debugLog) {log.info("retrieveIPAdd: Reaching out to Parent device for IP Address")}
    deviceID = device.getDataValue("deviceID")
    if (parent.retrieveApiDevices().keySet().contains(deviceID)) {
        ipAddress = parent.retrieveApiDevices()."${deviceID}".ip
    } else {
        ipAddress = "N/A"
    }
    if (debugLog) {log.info("retrieveIPAdd: LAN API Ip Address for device is ${ipAddress}")}
    device.updateDataValue("IP", ipAddress)
}

void lanInitDefaultValues() {
        sendEvent(name: "hue", value: 0)
        sendEvent(name: "saturation", value: 100)
        sendEvent(name: "effectNum", value: 0) 
    	sendEvent(name: "colorMode", value: "CT") 
        sendEvent(name: "level", value: 0)  
        sendEvent(name: "colorTemperature", value: 2000)  
}
