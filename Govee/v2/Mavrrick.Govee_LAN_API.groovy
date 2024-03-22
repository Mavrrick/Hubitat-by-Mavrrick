library (
 author: "Mavrrick",
 category: "Govee",
 description: "Govee LAN API",
 name: "Govee_LAN_API",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

// put methods, etc. here

def retrieveScenes() {
    state.remove("sceneOptions")
    state.remove("diySceneOptions")    
    state.scenes = [] as List
    state.diyEffects = [] as List
    if (debugLog) {log.debug ("retrieveScenes(): Retrieving Scenes from parent app")}
    if (debugLog) {log.debug ("retrieveScenes(): DIY Keyset ${parent.state.diyEffects.keySet()}")}
    if (parent.state.diyEffects.containsKey((device.getDataValue("deviceModel"))) == false) {
        if (debugLog) {log.debug ("retrieveScenes(): No DIY Scenes to retrieve for device")}    
    } else {
        parent.state.diyEffects.(device.getDataValue("deviceModel")).each {
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
            String sceneValue = it.getKey() + "=" + it.value.name
            state.diyEffects.add(sceneValue)
            state.diyEffects = state.diyEffects.sort()
        }
    }
    if ( parent.atomicState."${"lightEffect_"+(device.getDataValue("DevType"))}" == null ) {
        if (debugLog) {log.debug ("retrieveScenes(): No Scenes to retrieve for device")}    
    } else { 
        parent.atomicState."${"lightEffect_"+(device.getDataValue("DevType"))}".each {
            if (it.getKey() == "999") {
                if (debugLog) {log.debug ("retrieveScenes(): Processing max scene value ${it.getKey()} of ${it.value.maxScene}")}
                device.updateDataValue("maxScene", it.value.maxScene.toString())
            } else {
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
            String sceneValue = it.getKey() + "=" + it.value.name
            state.scenes.add(sceneValue)
            state.scenes = state.scenes.sort()
            }
        }
    }
}

def getDevType() {
//    String state.DevType = null= 
    switch(device.getDataValue("deviceModel")) {
        case "H6117":
        case "H6163":
        case "H6168":
        case "H6172":
        case "H6173":
        case "H6175":
        case "H6176":
        case "H618A":
        case "H618B":
        case "H618C":
        case "H618E":
        case "H618F":
        case "H619A":
        case "H619B": 
        case "H619C": 
        case "H619D":
        case "H619E":
        case "H619Z":
        case "H61A0":
        case "H61A1":
        case "H61A2":
        case "H61A3":
        case "H61A5":
        case "H61A8":
        case "H61A9":
        case "H61B2":
        case "H61C2": 
        case "H61C3":
        case "H61C5":
        case "H61E1":
        case "H61E0":
            if (debugLog) {log.debug ("getDevType(): Found   ${device.getDataValue("deviceModel")} setting DevType to RGBIC_STRIP")}; 
            device.updateDataValue("DevType", "RGBIC_Strip");
            break; 
        case "H6066": 
        case "H606A":
        case "H6061":
            if (debugLog) {log.debug ("getDevType(): Found   ${device.getDataValue("deviceModel")} setting DevType to Hexa_Light")};
            device.updateDataValue("DevType", "Hexa_Light");            
            break;
        case "H6067": //Not added yet
            device.updateDataValue("DevType", "Tri_Light"); 
            break;
        case "H6065":
            device.updateDataValue("DevType", "Y_Light");
            break;        
        case "H6072": 
        case "H6079":
            device.updateDataValue("DevType", "Lyra_Lamp");
            break;
        case "H6076":
            device.updateDataValue("DevType", "Basic_Lamp");
            break;
        case "H6078":
            device.updateDataValue("DevType", "Cylinder_Lamp");
            break;        
        case "H6052": 
        case "H6051":
            device.updateDataValue("DevType", "Table_Lamp");
            break;
        case "H70C1":
        case "H70C2":
        case "H70CB":
            device.updateDataValue("DevType", "XMAS_Light");
            break;
        case "H610A": 
        case "H610B": 
        case "H6062":
            device.updateDataValue("DevType", "Wall_Light_Bar");
            break;
        case "H6046":
        case "H6056":
        case "H6047":
        case "H70CB":
            device.updateDataValue("DevType", "TV_Light_Bar"); 
            break;        
        case "H6088":
        case "H6087":
        case "H608A": 
        case "H608B":
        case "H608C":
            device.updateDataValue("DevType", "Indoor_Pod_Lights");
            break;        
        case "H705A":
        case "H705B":
        case "H705C":
        case "H706A":
        case "H706B":
        case "H706C":
            device.updateDataValue("DevType", "Outdoor_Perm_Light");
            break;
        case "H7050":
        case "H7051":
        case "H7055":
            device.updateDataValue("DevType", "Outdoor_Pod_Light");
            break;
        case "H7060":
        case "H7061":
        case "H7062":
        case "H7065":
        case "H7066":
            device.updateDataValue("DevType", "Outdoor_Flood_Light");
            break;
        case "H70B1":
            device.updateDataValue("DevType", "Curtain_Light");
            break;
        case "H7020":
        case "H7021":
        case "H7028":
        case "H7041":
        case "H7042":
            device.updateDataValue("DevType", "Outdoor_String_Light");
            break;
        default: 
            if (debugLog) {log.debug ("getDevType(): Unknown device Type  ${device.getDataValue("deviceModel")}")}; 
        break; 
        
    }       
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
   return ip+":"+commandPort()
}


def parse(message) {  
  log.error "Got something to parseUDP"
  log.error "UDP Response -> ${message}"    
}