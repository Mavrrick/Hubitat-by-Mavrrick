library (
 author: "Mavrrick",
 category: "Govee",
 description: "GoveeCloudAPI",
 name: "Govee_Cloud_Level",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

def cloudSetLevel(float v,duration = 0){
    int intv = v.toInteger()
    switch(true) {
        case intv > 100:
        	if (debugLog) {log.debug ("cloudSetLevel(): Value of ${v} is over 100. Setting to 100")};
            intv = 100;
        break;
        case intv < 0:
        	if (debugLog) {log.debug ("cloudSetLevel(): Value of ${v} is below 0. Setting to 0")};
            intv = 0;
        break;
        default: 
            if (debugLog) {log.debug ("cloudSetLevel(): Setting Level to ${v}")};
        break;
    }
    if (device.currentValue("cloudAPI") == "Retry") {
        log.error "cloudSetLevel(): CloudAPI already in retry state. Aborting call." 
    } else {
        cloudsetLevel2(intv)
    }
}

def cloudsetLevel2(int v){
    
        if (device.currentValue("colorMode") == "RGB") {
            if (debugLog) log.info "cloudSetLevel2(): Setting level for ${device.label} in RGB mode current level is ${device.currentValue("level", true)}."           
            cloudSetHsb(device.currentValue("hue"),device.currentValue("saturation"),v)
        } else {
            sendEvent(name: "cloudAPI", value: "Pending")
            sendCommand("brightness", v,"devices.capabilities.range")
        }
}

def cloudSetGoveeBrightness(v) {
    sendCommand("brightness", v,"devices.capabilities.range") 
}

