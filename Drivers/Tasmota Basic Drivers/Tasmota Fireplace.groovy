/*  Fireplace prototype
 *  Tasmota IR and RGB control, Iris Zigbee power monitoring for status
 *
 *	based on Iris smart plug by - Copyright 2018 Steve White
 *  Added Tasmota IR and RGB control  Gassgs 2022
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *	use this file except in compliance with the License. You may obtain a copy
 *	of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *	License for the specific language governing permissions and limitations
 *	under the License.
 */

def driverVer() { return "2.1" }

metadata {
	definition(name: "Tasmota Fireplace", namespace: "Gassgs", author: "Gary G"){
		capability "Actuator"
		capability "Switch"
        capability "ColorControl"
		capability "Light"
		capability "SwitchLevel"
        capability "MotionSensor"
        capability "Power Meter"
		capability "Sensor"
        capability "Configuration"
        capability "FanControl"
		//capability "Refresh"
        
        command "relay", [[name:"Relay", type: "ENUM",description: "relay", constraints: ["On", "Off",]]]
        command "setHeat", [[name:"Set Heat Mode", type: "ENUM",description: "Set Heat Mode", constraints: ["Off", "Low", "High"]]]
        command "setSpeed", [[name:"Speed*", type: "ENUM", description: "Speed", constraints: ["off","low","high"] ] ]
        command "cycleSpeedDown"
        command "levelUp"
        command "levelDown"
        
        attribute "heat","string"
        attribute "command","string"
        attribute "outlet","string"
        
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite", model: "3200", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite", model: "3200-Sgb", deviceJoinName: "Outlet"
    }
	preferences{
        section("Tasmota"){
            input(name: "ipAddress", type: "string", title: "<b>RGB Device IP Address</b>", displayDuringSetup: true, required: true)
			input(name: "port", type: "number", title: "<b>Port Number</b>", displayDuringSetup: true, required: true, defaultValue: 80)
            input(name: "irIpAddress", type: "string", title: "<b>IR Device IP Address</b>", displayDuringSetup: true, required: true)
		}
		section("Power"){
        		input "intervalMin", "number", title: "<b>Minimum interval (seconds) between power reports:</b>", defaultValue: 5, range: "1..600", required: false
        		input "intervalMax", "number", title: "<b>Maximum interval (seconds) between power reports:</b>", defaultValue: 600, range: "1..600", required: false
       			input "minDeltaV", "enum", title: "<b>Amount of voltage change required to trigger a report:</b>", options: [".5", "1", "5", "10", "15", "25", "50"], defaultValue: "1", required: false
		}
		section("Logging"){
            input name: "logInfo", type: "bool", title: "<b>Enable info logging</b>", defaultValue: true
            input name: "logEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: true
		} 
	}
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def installed(){
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    if (logEnable) runIn(1800, logsOff)
	initialize()
    configure()
}

def updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
	initialize()
    configure()
    if (logEnable) runIn(1800, logsOff)
}

def initialize(){
	state.lastSwitch = 0

	if (logEnable){
		logInfo "Verbose logging has been enabled for the next 30 minutes."
		runIn(1800, logsOff)
	}
}

//===================== Zigbee power reporting====================//

def parse(String description){
    status = device.currentValue ("switch")
    heatStatus = device.currentValue ("heat")
    
	if (logEnable) log.debug "Description is $description"

	def event = zigbee.getEvent(description)
	def msg = zigbee.parseDescriptionAsMap(description)

	if (logEnable) log.debug "Parsed data...  Evt: ${event}, msg: ${msg}"

	// Hubitat does not seem to support power events
	if (event){
		if (event.name == "power"){
			def value = (event.value.parseInt()) / 10
			event = createEvent(name: event.name, value: value, descriptionText: "${device.displayName} power is ${value} watts")
			if (logEnable) log.debug "${device.displayName} power is ${value} watts"
            state.powerOn = (value >= (1.5))
            state.heatPowerOff = (value  <(300))
            state.heatPowerLow = (value >(300)&&value <(900))
            state.heatPowerHigh = (value >( 900))
            if (!state.powerOn && status == "on"){
                if (logInfo) log.info "$device.label - OFF"
                sendEvent(name:"switch",value:"off")
                sendEvent(name:"motion",value:"inactive")
                runInMillis(100,rgbOff)
            }
            if (state.powerOn && status == "off"){
                if (logInfo) log.info "$device.label - ON"
                sendEvent(name:"switch",value:"on")
                sendEvent(name:"motion",value:"active")
                runInMillis(100,rgbOn)
            }
            if(state.heatPowerOff && heatStatus != "off"){
                if (logInfo) log.info "$device.label - Heat is OFF"
                sendEvent(name:"heat",value:"off")
            }
            if (state.heatPowerLow && heatStatus != "low"){
                if (logInfo) log.info "$device.label - Heat is Low"
                sendEvent(name:"heat",value:"low")
            }
            if (state.heatPowerHigh && heatStatus != "high"){
                if (logInfo) log.info "$device.label - Heat is High"
                sendEvent(name:"heat",value:"high")
            }
		}
		else if (event.name == "switch"){
			def descriptionText = event.value == "on" ? "${device.displayName} is On" : "${device.displayName} is Off"
			event = createEvent(name:"outlet", value: event.value, descriptionText: descriptionText)
			
			// Since the switch has reported that it is off it can't be using any power.  Set to zero in case the power report does not arrive, but do not report in event logs.
			if (event.value == "off") sendEvent(name: "power", value: "0", descriptionText: "${device.displayName} power is 0 watts")

			// DEVICE HEALTH MONITOR: Switch state (on/off) should report every 10mins or so, regardless of any state changes.
			// Capture the time of this message
			state.lastSwitch = now()
		}
	}
	// Handle interval-based power reporting
	else if (msg?.cluster == "0B04"){
		// Watts
		if (msg?.attrId == "050B"){
			def value = Integer.parseInt(msg.value, 16) / 10
			event = createEvent(name: "power", value: value, descriptionText: "${device.displayName} power is ${value} watts")
			if (logEnable) log.debug "${device.displayName} power is ${value} watts"
            state.powerOn = (value >= (1.5))
            state.heatPowerOff = (value  <(300))
            state.heatPowerLow = (value >(300)&&value <(900))
            state.heatPowerHigh = (value >( 900))
            if (!state.powerOn && status == "on"){
                if (logInfo) log.info "$device.label - OFF"
                sendEvent(name:"switch",value:"off")
                sendEvent(name:"motion",value:"inactive")
                runInMillis(500,rgbOff)
            }
            if (state.powerOn && status == "off"){
                if (logInfo) log.info "$device.label - ON"
                sendEvent(name:"switch",value:"on")
                sendEvent(name:"motion",value:"active")
                runInMillis(500,rgbOn)
            }
            if(state.heatPowerOff && heatStatus != "off"){
                if (logInfo) log.info "$device.label - Heat is OFF"
                sendEvent(name:"heat",value:"off")
            }
            if (state.heatPowerLow && heatStatus != "low"){
                if (logInfo) log.info "$device.label - Heat is Low"
                sendEvent(name:"heat",value:"low")
            }
            if (state.heatPowerHigh && heatStatus != "high"){
                if (logInfo) log.info "$device.label - Heat is High"
                sendEvent(name:"heat",value:"high")
            }
		}
	}
	else{
		def cluster = zigbee.parse(description)

		if (cluster && cluster.clusterId == 0x0006 && (cluster.command == 0x07 || cluster.command == 0x0B)){
			if (cluster.data[0] == 0x00 || cluster.data[0] == 0x02){
				// DEVICE HEALTH MONITOR: Switch state (on/off) should report every 10mins or so, regardless of any state changes.
				// Capture the time of this message
				state.lastSwitch = now()

				if (cluster.data[0] == 0x00) logDebug "ON/OFF REPORTING CONFIG RESPONSE: " + cluster
				if (cluster.data[0] == 0x02) logDebug "ON/OFF TOGGLE RESPONSE: " + cluster
			}
			else{
				log.warn "ON/OFF REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
				event = null
			}
		}
		else if (cluster && cluster.clusterId == 0x0B04 && cluster.command == 0x07){
			if (cluster.data[0] == 0x00){
				// Get a power meter reading
				runIn(5, "refresh")
				if (logEnable) log.debug "POWER REPORTING CONFIG RESPONSE: " + cluster
			}
			else{
				log.warn "POWER REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
				event = null
			}
		}
		else if (cluster && cluster.clusterId == 0x0003 && cluster.command == 0x04){
			if (logInfo) log.info "LOCATING DEVICE FOR 30 SECONDS"
		}

		else{
			//logWarn "DID NOT PARSE MESSAGE for description : $description"
			if (logEnable) log.debug "${cluster}"
		}
	}
	return event ? createEvent(event) : event
}

def relay(value){
    if (value == "On"){
        relayOn()
    }else{
        relayOff()
    }
}

def relayOn(){
	zigbee.command(0x0006, 0x01)
}

def relayOff(){
	zigbee.command(0x0006, 0x00)
}


def refresh(){
	if (logEnable) log.debug "Refresh called..."
	zigbee.onOffRefresh() + zigbee.electricMeasurementPowerRefresh() + zigbee.readAttribute(0x0B04, 0x0505) + zigbee.readAttribute(0x0B04, 0x0300)
}

def configure(){
	if (logEnable) log.debug  "Configure called..."

	// On/Off reporting of 0 seconds, maximum of 15 minutes if the device does not report any on/off activity
	zigbee.onOffConfig(0, 900) + powerConfig()
}

def powerConfig(){
	// Calculate threshold
	def powerDelta = (Float.parseFloat(minDeltaV ?: "1") * 10)
	
	if (logEnable) log.debug "Configuring power reporting intervals; min: ${intervalMin}, max: ${intervalMax}, delta: ${minDeltaV}, endpointId: ${endpointId}"
	
	def cfg = []

	cfg +=	zigbee.configureReporting(0x0B04, 0x050B, 0x29, (int) (intervalMin ?: 5), (int) (intervalMax ?: 600), (int) powerDelta)	// Wattage report.
	cfg +=	zigbee.configureReporting(0x0B04, 0x0505, 0x21, 30, 900, 1)																// Voltage report
	cfg +=	zigbee.configureReporting(0x0B04, 0x0300, 0x21, 900, 86400, 1)  														// AC Frequency Report
	
	return cfg
}

//===================== Tasmota IR Control====================//

def setHeat(value) {
    heatStatus = device.currentValue("heat")
    if (logEnable) log.info "$device.label Heat command set to $value"
    if (value == "High"){
        if (heatStatus != "high"){
            cmd = '{"Protocol":"NEC","Bits":32,"Data":"0x807F609F","DataLSB":"0x1FE06F9","Repeat":0}'
            sendIr(cmd)
        }else{
            if (logInfo) log.info "$device.label - Heat is already High"
        }
    }
    if (value=="Low"){
        if (heatStatus != "low"){
            cmd = '{"Protocol":"NEC","Bits":32,"Data":"0x807F708F","DataLSB":"0x1FE0EF1","Repeat":0}'
            sendIr(cmd)
        }else{
            if (logInfo) log.info "$device.label - Heat is already Low"
        }
    }
    if (value=="Off"){
        if (heatStatus == "low"){
            cmd = '{"Protocol":"NEC","Bits":32,"Data":"0x807F708F","DataLSB":"0x1FE0EF1","Repeat":0}'
            sendIr(cmd)
        }
        else if (heatStatus == "high"){
            cmd = '{"Protocol":"NEC","Bits":32,"Data":"0x807F609F","DataLSB":"0x1FE06F9","Repeat":0}'
            sendIr(cmd)
        }else{
            if (logInfo) log.info "$device.label - Heat is already Off"
        }
     }
}

def setSpeed(data){
    if (data == "off"){
        setHeat("Off")
    }
    if (data == "low"){
        setHeat("Low")
    }
    if (data == "high"){
        setHeat("High")
    }
}

def cycleSpeed() {

    def speed = device.currentValue"heat";

    switch(speed) {
        case "low":
            speed = "high";
            break;
        case "high":
            speed = "off";
            break;
        case "off":
            speed = "low";
            break;
    }
    setSpeed(speed)
}

def cycleSpeedDown() {

    def speed = device.currentValue"heat";

    switch(speed) {
        case "low":
            speed = "off";
            break;
        case "off":
            speed = "high";
            break;
        case "high":
            speed = "low";
            break;
    }
    setSpeed(speed)
}

def on(){
    status = device.currentValue("switch")
    if (logEnable) log.info "$device.label - Power on command "
    if (status == "off"){
        cmd = '{"Protocol":"NEC","Bits":32,"Data":"0x807F38C7","DataLSB":"0x1FE1CE3","Repeat":0}'
        sendIr(cmd)
        //rgbOn()
    
    }else{
        if (logInfo) log.info "$device.label - Power is already on"
    }
}

def off() {
    status = device.currentValue("switch")
    if (logEnable) log.info "$device.label Power Off Command"
    if (status == "on"){
        cmd = '{"Protocol":"NEC","Bits":32,"Data":"0x807F38C7","DataLSB":"0x1FE1CE3","Repeat":0}'
        sendIr(cmd)
        //rgbOff()
        
    }else{
        if (logInfo) log.info "$device.label - Power is already off"
    }
}

def sendIr(cmd){
        if(settings.irIpAddress){
        if (logEnable) log.debug "Sending IR command Device Status - $cmd"

        try {
           httpGet("http://" + irIpAddress + "/cm?cmnd=irsend%20"+ URLEncoder.encode(cmd, "UTF-8").replaceAll(/\+/,'%20')) { resp ->
           def json = (resp.data)
               if (logEnable) log.debug "${json}"
               if (json.IRSend == "Done"){
                   if (logEnable) log.debug "Command Success response from Device"
               }else{
                   if (logEnable) log.debug "Command -ERROR- response from Device- $json"
               }
           }
        }catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }
    }
}

//===================== Tasmota RGB Control====================//

def rgbOn(){
    setLevel(100)
    //sendCommand("power","on")
}

def rgbOff(){
    setLevel(0)
    //sendCommand("power","off")
}

def setColor(value) {
    status = device.currentValue("switch")
    if (status == "off"){
        on()
    }
    if (logInfo) log.info "$device.label Color set $value"
	if (logEnable) {
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
        if (logEnable) {
           log.warn "Invalid argument for setColor: ${value}"
        }
    }
}

def setHsb(h,s,b){
	if (logEnable) {
        log.debug("setHSB - ${h},${s},${b}")
    }
	myh = h*4
	if( myh > 360 ) { myh = 360 }
	hsbcmd = "${myh},${s},${b}"
	if (logEnable) {
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

        white()
        }
    else {
        sendCommand("hsbcolor", hsbcmd)
    }
}

def setHue(h){
    setHsb(h,state.saturation,state.level)
}

def setSaturation(s){
	setHsb(state.hue,s,state.level)
}

def setLevel(v)
{
    setLevel(v, 0)
}

def setLevel(v, duration){
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

def levelUp(){
    currentLevel = device.currentValue("level") as Integer
    currentStatus = device.currentValue("switch")
    statusOn = (currentStatus == "on")
    if (currentLevel <= 85 && statusOn){
        if (logInfo) log.info "$device.label Level Up"
        newLevel = (currentLevel + 15)
        setLevel(newLevel)
    }
    else if (currentLevel > 85 && currentLevel < 100 && statusOn){
        if (logInfo) log.info "$device.label Level Up"
        newLevel = 100
        setLevel(newLevel)
    }
    else{
        if (logInfo) log.info "$device.label Level Up - not available"
    }
}

def levelDown(){
    currentLevel = device.currentValue("level") as Integer
    currentStatus = device.currentValue("switch")
    statusOn = (currentStatus == "on")
    if (currentLevel >= 25 && statusOn){
        if (logInfo) log.info "$device.label Level Down"
        newLevel = (currentLevel - 15)
        setLevel(newLevel)
    }
    else{
        if (logInfo) log.info "$device.label Level Down - not available"
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

private getEndpointId(){
	new BigInteger(device.endpointId, 16).toString()
}
