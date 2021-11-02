// Tasmota RGB & Harmony IR Fireplace  by Gassgs,  Gary G

// Original Tasmota driver developed by Brett Sheleski
// Ported to Hubitat by dkkohler
// HSB Color Settings by Eric Maycock (erocm123)
// Moddified for Fireplace Harmony IR controlled with Tasmota RGB Light Strip

// Version 1.5.0

metadata {
	definition(name: "Harmony Controlled Fireplace + Tasmota RGB", namespace: "Gassgs", author: "GaryG") {
		capability "ColorControl"
		capability "Light"
		capability "Switch"
		capability "SwitchLevel"
        capability "Actuator"
        
        command "setHeat", [[name:"Set Heat Mode", type: "ENUM",description: "Set Heat Mode", constraints: ["Off", "Low", "High"]]]
        
        attribute "heat","string"
        attribute "colorMode","string"
    }

	preferences {		
		section("Sonoff Host") {
            input(name: "ipAddress", type: "string", title: "IP Address", displayDuringSetup: true, required: true)
			input(name: "port", type: "number", title: "Port", displayDuringSetup: true, required: true, defaultValue: 80)
		}
        section("Settings") {
            input(name: "infoLog", type: "bool", title: "Enable Info Text Logging", displayDuringSetup: false, required: false, defaultValue: false)
            input(name: "logging", type: "bool", title: "Enable Debug Logging", displayDuringSetup: false, required: false, defaultValue: false)
		}        
	}
}

def parse(String description) {
	def message = parseLanMessage(description)
	def isParsed = false;

	// parse result from current formats
	def resultJson = {}
	if (message?.json) {
		// current json data format
		resultJson = message.json
        if (logging) {
            log.debug resultJson
        }
	}  
}

def setHeat(value) {
    if (infoLog) log.info "$device.label Heat set to $value"
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

def on() {
    if (infoLog) log.info "$device.label On"
    sendEvent(name: "switch", value: "on")
	sendCommand("Power", "On")
}

def off() {
    if (infoLog) log.info "$device.label Off"
    sendEvent(name: "switch", value: "off")
	sendCommand("Power", "Off")
}   
    
def setColor(value) {
    if (device.currentValue("switch") == "off"){
        on()
    }
    if (infoLog) log.info "$device.label Color set $value"
	if (logging) {
        log.debug "HSBColor = "+ value
    }
    currentLevel = device.currentValue("level")
    if (value.level == null){
        value.level = currentLevel
    }
	   if (value instanceof Map) {
        def h = value.containsKey("hue") ? value.hue : null
        def s = value.containsKey("saturation") ? value.saturation : null
        def b = value.containsKey("level") ? value.level : null
    	setHsb(h, s, b)
    } else {
        if (logging) {
           log.warn "Invalid argument for setColor: ${value}"
        }
    }
}

def setHsb(h,s,b)
{
	if (logging) {
        log.debug("setHSB - ${h},${s},${b}")
    }
	myh = h*4
	if( myh > 360 ) { myh = 360 }
	hsbcmd = "${myh},${s},${b}"
	if (logging) {
        log.debug "Cmd = ${hsbcmd}"
    }
    state.hue = h
	state.saturation = s
	state.level = b
	state.colorMode = "RGB"
    sendEvent(name: "hue", value: "${h}")
    sendEvent(name: "saturation", value: "${s}")
    sendEvent(name: "level", value: "${b}")
	if (hsbcmd == "0,0,100") {
        state.colorMode = "white"
        sendEvent(name: "colorMode", value: "CT")
        white()
        }
    else {
        sendEvent(name: "colorMode", value: "RGB")
        sendCommand("hsbcolor", hsbcmd)
    }
}

def setHue(h)
{
    setHsb(h,state.saturation,state.level)
}

def setSaturation(s)
{
	setHsb(state.hue,s,state.level)
}

def setLevel(v)
{
    setLevel(v, 0)
}

def setLevel(v, duration)
{
    sendEvent(name: "level", value: "${v}")
    if (duration == 0) {
        if (state.colorMode == "RGB") {
            setHsb(state.hue,state.saturation, v)    
        }
        else {
            sendCommand("Dimmer", "${v}")
        }
    }
    else if (duration > 0) {
        if (state.colorMode == "RGB") {
            setHsb(state.hue,state.saturation, v)    
        }
        else {
            if (duration > 7) {duration = 7}
            cdelay = duration * 10
            DurCommand = "fade%201%3Bspeed%20" + "$duration" + "%3Bdimmer%20" + "$v" + "%3BDelay%20"+ "$cdelay" + "%3Bfade%200"
            sendCommand("backlog", DurCommand)
        }
   }
}

private def sendCommand(String command, payload) {
    sendCommand(command, payload.toString())
}

private def sendCommand(String command, String payload) {
	//log.debug "sendCommand(${command}:${payload}) to device at $ipAddress:$port"

	if (!ipAddress || !port) {
		if (logging) {
            log.warn "aborting. ip address or port of device not set"
        }
		return null;
	}
	def hosthex = convertIPtoHex(ipAddress)
	def porthex = convertPortToHex(port)

	def path = "/cm"
	if (payload){
		path += "?cmnd=${command}%20${payload}"
	}
	else{
		path += "?cmnd=${command}"
	}

	if (username){
		path += "&user=${username}"
		if (password){
			path += "&password=${password}"
		}
	}

	def result = new hubitat.device.HubAction(
		method: "GET",
		path: path,
		headers: [
			HOST: "${ipAddress}:${port}"
		]
	)

    return result
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format('%04x', port.toInteger())
	return hexport
}
