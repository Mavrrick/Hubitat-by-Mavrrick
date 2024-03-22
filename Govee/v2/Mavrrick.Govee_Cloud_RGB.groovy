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
}

def setSaturation(s)
{
	setHsb(device.currentValue("hue")?:0,s,device.currentValue("level")?:100)
}

def snapshot(effectNo) {
     sendCommand("snapshot", effectNo,"devices.capabilities.dynamic_scene")
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