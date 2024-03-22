library (
 author: "Mavrrick",
 category: "Govee",
 description: "GoveeCloudAPI",
 name: "Govee_Cloud_Level",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

def setLevel(float v,duration = 0){
    int intv = v.toInteger()
    if (lanControl && duration>0){
        int intduration = duration.toInteger()
        sendEvent(name: "switch", value: "on")
        fade(intv,intduration)
    }
    else {
        if (device.currentValue("cloudAPI") == "Retry") {
            log.error "setLevel(): CloudAPI already in retry state. Aborting call." 
        } else {
        setLevel2(intv)
        }
    }
}

def setLevel2(int v){
        if (lanControl) {
        sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
        sendEvent(name: "level", value: v)
        sendEvent(name: "switch", value: "on")}
    else {
        sendEvent(name: "cloudAPI", value: "Pending")
		if  (aRngBright) {v=incBrightnessRange(v)}
        if (debugLog) { log.debug "setLevel2(): Sent Brightness = ${v}"}
		sendCommand("brightness", v,"devices.capabilities.range")
    }
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
    log.debug "fadeDown(): v ${v}"
    if ( v == 0 ) {
        log.debug "fadeDown(): Next fade is to 0 turning off device. Fade is complete"
        off()
    } else if (level==v) {
            if (debugLog) {log.debug "Final Loop"}
            sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
            sendEvent(name: "level", value: v)
    } else {
            sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
            sendEvent(name: "level", value: v)
            if (debugLog) {log.debug "fadeDown(): continueing  fading to ${v}"}
            def int delay = fadeRep
            if (debugLog) {log.debug "fadeDown(): delay ia ${delay}"}
            if (debugLog) {log.debug "fadeDown(): executing loop to fadedown() with values curLevel ${v}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
            runInMillis(delay, fadeDown, [data:[v ,level, fadeInt,fadeRep]])
    }
} 

def fadeUp( int curLevel, int level, fadeInt, fadeRep) {
    if (debugLog) {log.debug "fadeUp(): curLevel ${curLevel}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
    int v= (curLevel+fadeInc).toInteger()
    log.debug "fadeUp(): v ${v}"
    if (level==v)    {
        if (debugLog) {log.debug "Final Loop"}
        sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
        sendEvent(name: "level", value: v)
    }
    else {
        if (debugLog) {log.debug "fadeUp(): continueing  fading to ${v}"}
        def int delay= fadeRep
        if (debugLog) {log.debug "fadeUp(): delay ia ${delay}"}
        if (debugLog) {log.debug "fadeUp(): executing loop to fadeup() with values curLevel ${v}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
        sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
        sendEvent(name: "level", value: v)
        runInMillis(delay, fadeUp, [data:[v ,level, fadeInt,fadeRep]])
    }
} 