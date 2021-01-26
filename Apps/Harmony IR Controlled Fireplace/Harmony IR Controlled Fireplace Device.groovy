/*
 * Harmony IR Controlled Fireplace
 *
 * 
 * 
 */
metadata {
    definition(name: "Harmony IR Controlled Fireplace", namespace: "Gassgs", author: "GaryG") {
        capability "Actuator"
        capability"Switch"
        
        command "setHeat", [[name:"Set Heat Mode", type: "ENUM",description: "Set Heat Mode", constraints: ["Off", "Low", "High"]]]
        command"changeColor"
        
        attribute"color", "text"
        attribute"heat","text"
    
    }
}

preferences {
    section() {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "logInfoEnable", type: "bool", title: "Enable text info logging", defaultValue: true
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def on() {
     if (logInfoEnable) log.info "Fireplace On"
    sendEvent(name: "switch", value: "on")
}

def off() {
     if (logInfoEnable) log.info "Fireplace Off"
    sendEvent(name: "switch", value: "off")
}

def setHeat(value) {
    if (value=="High"){
    sendEvent(name: "heat", value: "high")
    }
     if (value=="Low"){
    sendEvent(name: "heat", value: "low")
     }
     if (value=="Off"){
    sendEvent(name: "heat", value: "off")
     }
}

def changeColor() {
    if (logInfoEnable) log.info "Fireplace Color Changed"
    sendEvent(name: "color", value: "on")
    runIn(1,colorOff)
}

def colorOff() {
    sendEvent(name: "color", value: "off")
}

def installed() {
}
