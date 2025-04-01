library (
 author: "Mavrrick",
 category: "Govee",
 description: "GoveeCloudAPI",
 name: "Govee_Cloud_RGB",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

// put methods, etc. here

def setColor(value) {
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) { log.debug "setColor(): HSBColor = "+ value + "${device.currentValue("level")}"}
	if (value instanceof Map) {
		def h = value.containsKey("hue") ? value.hue : null
		def s = value.containsKey("saturation") ? value.saturation : null
		def b = value.containsKey("level") ? value.level : null
        
        def theColor = getColor(h,s)
        if (descLog)
        {
            if (theColor == "Unknown")
            {
                if (debugLog) log.debug "trying alt. color name method"
                theColor = convertHueToGenericColorName(h,s)
                if (debugLog) log.debug "alt. method got back $theColor"
            }
            if (theColor != "Unknown") log.info "${device.label} Color is $theColor"
            else log.info "${device.label} Color is $value"
            sendEvent(name: "colorName", value: theColor)
        }        
        
        if (b == null) { b = device.currentValue("level") }
		setHsb(h, s, b)
	} else {
        if (debugLog) {log.debug "setColor(): Invalid argument for setColor: ${value}"}
    }
}

def setHsb(h,s,b)
{

	hsbcmd = [h,s,b]
    if (debugLog) { log.debug "setHsb(): Cmd = ${hsbcmd}"}

	rgb = hubitat.helper.ColorUtils.hsvToRGB(hsbcmd)
    if (debugLog) { log.debug "setHsb(): Cmd = ${rgb}"}
	def rgbmap = [:]
	rgbmap.r = rgb[0]
	rgbmap.g = rgb[1]
	rgbmap.b = rgb[2]   
    rgbvalue = ((rgb[0] & 0xFF) << 16) | ((rgb[1] & 0xFF) << 8) | ((rgb[2] & 0xFF) << 0)
    if (debugLog) { log.debug "setHsb(): rgbvalue = ${rgbvalue}"}
    if (lanControl) {
        if (debugLog) { log.debug "setHsb(): ${rgbmap}"}
        sendCommandLan(GoveeCommandBuilder("colorwc",rgbmap,"rgb"))
      	sendEvent(name: "hue", value: "${h}")
        sendEvent(name: "saturation", value: "${s}")
        sendEvent(name: "switch", value: "on")
   		sendEvent(name: "colorMode", value: "RGB")
        if (effectNum != 0){
            sendEvent(name: "effectNum", value: 0)
            sendEvent(name: "effectName", value: "None") 
        }
    } else {
        if (device.currentValue("cloudAPI") == "Retry") {
            log.error "setHsb(): CloudAPI already in retry state. Aborting call." 
        } else { 
        sendEvent(name: "cloudAPI", value: "Pending")
        sendCommand("colorRgb", rgbvalue,"devices.capabilities.color_setting")
        }
    }
    if(100 != device.currentValue("level")?.toInteger()) {
    setLevel(100)
    }
}

def setHue(h)
{
    setHsb(h,device.currentValue( "saturation" )?:100,device.currentValue("level")?:100)
    if (descLog) log.info "${device.label} Hue was set to ${h}"    
}

def setSaturation(s)
{
	setHsb(device.currentValue("hue")?:0,s,device.currentValue("level")?:100)
    if (descLog) log.info "${device.label} Saturation was set to ${s}%"    
}

def snapshot(effectNo) {
     sendCommand("snapshot", effectNo,"devices.capabilities.dynamic_scene")
    if (descLog) log.info "${device.label} was set to shapshot number ${effectNo}"     
}

def gradient(on_off) {
    switch(on_off) {
        case "on":
        sendCommand("gradientToggle", 1 ,"devices.capabilities.toggle");
        break; 
        case "off":
        sendCommand("gradientToggle", 0 ,"devices.capabilities.toggle");
        break;
        default: 
            if (debugLog) {log.debug ("gradient(): Unknown toggle value}")}; 
        break;
    }
}

def cloudSetEffect (effectNo) {
    if (debugLog) {log.debug ("setEffect(): Setting effect via cloud api to scene number  ${effectNo}") }
//                effectNumber = effectNo.toString()
                sendCommand("lightScene", effectNo,"devices.capabilities.dynamic_scene")
}

def cloudSetNextEffect () {
        keys = []
        names = []
        keys.addAll(state.scenes.keySet())
        names.addAll(state.scenes.values())
//        if (debugLog) {log.debug ("setNextEffect(): current Name ${names.get(state.sceneValue)} value ${keys.get(state.sceneValue)}")}
        if (state.sceneValue == state.sceneMax) state.sceneValue = 0  
        if (state.sceneValue == 0) {
            if (debugLog) {log.debug ("setNextEffect(): Current scene value is 0 setting to first scene in list")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneValue + 1
        } else {
            if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneValue + 1
        }  
}

def cloudSetPreviousEffect () {
        keys = []
        names = []
        keys.addAll(state.scenes.keySet())
        names.addAll(state.scenes.values())
//        if (debugLog) {log.debug ("setPreviousEffect(): current Name ${names.get(state.sceneValue)} value ${keys.get(state.sceneValue)}")}       
            if (state.sceneValue == 0) {
            if (debugLog) {log.debug ("setPreviousEffect(): Current scene value is 0 setting to first scene in list")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneMax-1 
        } else {
            if (debugLog) {log.debug ("setPreviousEffect(): Increment to next scene")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneValue - 1
        }          
}

def cloudActivateDIY (diyActivate) {
    sendCommand("diyScene", diyActivate,"devices.capabilities.dynamic_scene")         
}

def dreamview(on_off) {
    switch(on_off) {
        case "on":
        sendCommand("dreamViewToggle", 1 ,"devices.capabilities.toggle");
        break; 
        case "off":
        sendCommand("dreamViewToggle", 0 ,"devices.capabilities.toggle");
        break;
        default: 
            if (debugLog) {log.debug ("dreamview(): Unknown toggle value}")}; 
        break;
    }
}

def getColor(hue, saturation)
{
   
    def color = "Unknown"
    
   	//Set the hue and saturation for the specified color.
	switch(hue) {
		case 0:
            if (saturation == 0)
              color = "White"
            else color = "Red"
		    break;
        case 53:
            color = "Daylight"
			break;
        case 23:
  		    color = "Soft White"
			break;
        case 20:
		    color = "Warm White"
			break;
        case 61:
            color = "Navy Blue"
            break;
        case 65:
		    color = "Blue"
			break;
		case 35:
			color = "Green"
			break;
        case 47:
        	color = "Turquoise"
            break;
        case 50:
            color = "Aqua"
            break;
        case 13:
            color = "Amber"
            break;
		case 17:
        case 25:
			color = "Yellow"
			break; 
        case 7:
            color ="Safety Orange" 
            break;
		case 10:
			color = "Orange"
			break;
        case 73:
            color = "Indigo"
            break;
		case 82:
			color = "Purple"
			break;
        case 90:
        case 91:
		case 90.78:
			color = "Pink"
			break;
        case 94:
            color = "Rasberry"
            break;
        case 4:
            color = "Brick Red"
            break;  
        case 69:
            color = "Slate Blue"
            break;
    }
  return color
}