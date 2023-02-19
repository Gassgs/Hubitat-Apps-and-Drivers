/**
 *  Tasmota Kauf SR10 RGB Switch
 *  
 *
 *  Copyright 2023 Gassgs/ Gary Gassmann
 *
 *
 *  Based on the Hubitat community driver httpGetSwitch 
 *  https://raw.githubusercontent.com/hubitat/HubitatPublic/master/examples/drivers/httpGetSwitch.groovy
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *  V1.0.0  07-02-2021       first run   
 *  V1.1.0  07-17-2021       Refresh schedule improvements
 *  V1.2.0  08-15-2021       Added option to pause refreshing
 *  V1.3.0  10-09-2021       Fixed pause refreshing option
 *  V1.4.0  05-18-2022       Added motion capability
 *  V1.5.0  06-01-2022       Adding rule integration for syned updates, Many changes and improvments
 *  V1.6.0  06-03-2022       Addding child devices for dual switch
 *  V1.7.0  06-22-2022       Addding relay controls from parent device
 *  V1.8.0  06-28-2022       Removed "offline, status" moved to wifi atribute and general cleanup and improvments
 *  V1.9.0  06-28-2022       New device support Kauf SR10 RGB Wall switch
 */

def driverVer() { return "1.9" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Tasmota RGB Switch", namespace: "Gassgs", author: "Gary G") {
        capability "Actuator"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
        capability "Light"
        capability "SwitchLevel"
        capability "ColorControl"
        capability "Sensor"
        
        command "rgbOn"
        command "rgbOff"
        command "flash"
        
        attribute "rgb","string"
        attribute "wifi","string"
        
    }
}
    preferences {
        input name: "deviceIp",type: "string", title: "<b>Tasmota Device IP Address</b>", required: true
        input name: "hubIp",type: "string", title: "<b>Hubitat Device IP Address</b>", required: true
        input name: "refreshEnable",type: "bool", title: "<b>Enable to Refresh every 30mins</b>", defaultValue: true
        input name: "logInfo", type: "bool", title: "<b>Enable info logging</b>", defaultValue: true
        input name: "logEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: true
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    if (refreshEnable){
        runEvery30Minutes(refresh)
        if (logEnable) log.debug "refresh every 30 minutes scheduled"
    }else{
        unschedule(refresh)
        if (logEnable) log.debug "refresh schedule canceled"
	}
    deviceSetup()
    syncSetup()
    if (logEnable) runIn(1800, logsOff)
}

def deviceSetup(){
    if (deviceIp){
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=STATUS%200") { resp ->
                def json = (resp.data)
                if (json){
                    if (logEnable) log.debug "${json}"
                    def macAddress = (json.StatusNET.Mac)
                    def mac = macAddress.replace(":","")
                    state.dni = mac as String
                    if (logEnable) log.debug "Command Success response from Device"
                    if (logEnable) log.debug "Mac Address $macAddress  to DNI $mac"
                    setDeviceNetworkId()
                    def name = (json.Status.DeviceName)
                    if (logEnable) log.debug "Device Name set to $name"
                    device.name = "$name"
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }
    }
}

void setDeviceNetworkId(){
    if (state.dni != null && state.dni != device.deviceNetworkId) {
       device.deviceNetworkId = state.dni
       if (logEnable) log.debug "${state.dni as String} set as Device Network ID"
    }
}

def syncSetup(){
        if (hubIp){
            rule = "ON Power1#state DO webquery http://"+ hubIp + ":39501/ POST SwitchOne%value% ENDON "  +
            "ON Power2#state DO webquery http://"+ hubIp + ":39501/ POST SwitchTwo%value% ENDON "
            
            ruleNow = rule.replaceAll("%","%25").replaceAll("#","%23").replaceAll("/","%2F").replaceAll(":","%3A").replaceAll(" ","%20")
            if (logEnable) log.debug "$ruleNow"              
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=RULE3%20${ruleNow}") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logEnable) log.debug "Command Success response from Device - Setup Rule 3"
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }
    }
    runIn(2,turnOnRule)
}

def turnOnRule(){
     try {
         httpGet("http://" + deviceIp + "/cm?cmnd=RULE3%20ON") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logEnable) log.debug "Command Success response from Device - Rule 3 activated"
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
    refresh()
}

def parse(LanMessage){
    if (logEnable) log.debug "data is ${LanMessage}"
    def msg = parseLanMessage(LanMessage)
    def json = msg.body
    if (logEnable) log.debug "${json}"
    if (json.contains("SwitchOne")){         
        if (logEnable) log.debug "Found the word SwitchOne"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - Switch is On"
            sendEvent(name:"switch",value:"on")
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - Switch is Off"
            sendEvent(name:"switch",value:"off")
        }
    }
    if (json.contains("SwitchTwo")){
        if (logEnable) log.debug "Found the word SwitchTwo"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - RGB is On"
            sendEvent(name:"rgb",value:"on")
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - RGB is Off"
            sendEvent(name:"rgb",value:"off")
        }
    }
}
    
def on(value) {
    if (logEnable) log.debug "Sending On Command to [${settings.deviceIp} Switch $value]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER" + "$value" + "%20On") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER1 == "ON" || json.POWER2 == "ON"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def off(value) {
    if (logEnable) log.debug "Sending Off Command to [${settings.deviceIp} Switch $value]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER" + "$value" + "%20Off") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER1 == "OFF" || json.POWER2 == "OFF"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def setColor(value) {
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

private def sendCommand(String command, payload) {
    sendCommand(command, payload.toString())
}

private def sendCommand(String command, String payload) {
    port = 80 as Number
	if (lodEnable) log.debug "sendCommand(${command}:${payload}) to device at $deviceIp:$port"

	if (!deviceIp) {
		if (logEnable) {
            log.warn "aborting. ip address or port of device not set"
        }
		return null;
	}
	def hosthex = convertIPtoHex(deviceIp)
	def porthex = convertPortToHex(port)

	def path = "/cm"
	if (payload){
		path += "?cmnd=${command}%20${payload}"
	}
	else{
		path += "?cmnd=${command}"
	}
	def result = new hubitat.device.HubAction(
		method: "GET",
		path: path,
		headers: [
			HOST: "${deviceIp}:${port}"
		]
	)
    return result
}

private String convertIPtoHex(deviceIp) { 
	String hex = deviceIp.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format('%04x', port.toInteger())
	return hexport
}

def on(){
    on(1)
}

def off(){
    off(1)
}

def rgbOn(){
    if (state.flashing){
        stopFlash()
    }
    on(2)
}

def rgbOff(){
    if (state.flashing){
        stopFlash()
    }
    off(2)
}

def flash(){
    if (state.flashing){
        stopFlash()
        if (state.restore){
            rgbOn()
        }
        else{
           rgbOff()
        }
    }
    else{
        state.flashing = true
        if (logInfo) log.info "$device.label Flashing Started"
        currentStatus = device.currentValue("rgb")
        if (currentStatus == "on"){
            state.restore = true
            flashOff()
        }
        else{
            state.restore = false
            flashOn()
        }
    }  
}

def flashOn(){
    on(2)
    runInMillis(750,flashOff)
}

def flashOff(){
    off(2)
    runInMillis(750,flashOn)  
}

def stopFlash(){
    if (logInfo) log.info "$device.label Flashing Ended"
    unschedule(flashOn)
    unschedule(flashOff)
    state.flashing = false
}

def refresh() {
    if(settings.deviceIp){
        if (logEnable) log.debug "Refreshing Device Status - [${settings.deviceIp}]"
        try {
           httpGet("http://" + deviceIp + "/cm?cmnd=status%2011") { resp ->
           def json = (resp.data)
            if (logEnable) log.debug "${json}"
               if (json.containsKey("StatusSTS")){
                   if (logEnable) log.debug "PWR status found"
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logEnable) log.debug "Wifi signal strength $signal db"
                   sendEvent(name:"wifi",value:"${signal}db")
                   status1 = json.StatusSTS.POWER1 as String
                   if (logEnable) log.debug "POWER1: $status1"
                   if (logInfo) log.info "$device.label - Switch is $status1"
                   sendEvent(name: "switch", value: "$status1".toLowerCase())
                   }
                   status2 = json.StatusSTS.POWER2 as String
                   if (logEnable) log.debug "POWER2: $status2"
                   if (logInfo) log.info "$device.label - RGB is $status2"
                   sendEvent(name: "rgb", value: "$status2".toLowerCase())
           }
        }catch (Exception e) {
            sendEvent(name:"wifi",value:"offline")
            log.warn "Call to on failed: ${e.message}"
        }
    }
} 

def installed() {
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    if (logEnable) runIn(1800, logsOff)
}

def uninstalled() {
    deleteChildren()
}

