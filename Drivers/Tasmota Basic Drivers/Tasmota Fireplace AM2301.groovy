/*
 *  Tasmota - Fireplace 
 *  Athom 4 channel 30a relay module with Push Button and AM2301 Temperature/Humidity sensor
 *
 *  Copyright 2023 by Gassgs
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  Change History:
 *
 *  V1.0.0  4-13-2023       Tasmota Fireplace First run
 *  V1.1.0  4-24-2023       Added dewPoint reading
 *  V1.2.0  4-27-2023       Added re-setting setpoint to 68 if out of range at start up.
 */

def driverVer() { return "1.2" }


metadata
{
	definition(name: "Tasmota Fireplace - AM2301", namespace: "Gassgs", author: "GaryG")
	{
        capability "Actuator"
		capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
		capability "Refresh"
		capability "Sensor"
        capability "Switch"
        
        command "setThermostatMode", [[name:"Thermostat Mode", type: "ENUM",description: "Thermostat Mode", constraints: ["on","heat","off"]]]
        command "setHeatingSetpoint",[[name: "Heat Set Point",type: "NUMBER",description:"Desired Heat Level - 60..80"]]
        command "toggle"
        
        attribute "dewPoint","number"
        attribute "thermostatMode","string"
        attribute "thermostatOperatingState","string"
        attribute "heatingSetpoint","number"
        attribute "wifi","string"
	}

	preferences{
		section{
            input name: "deviceIp",type: "string", title: "<b>Tasmota Device IP Address</b>", required: true
            input name: "hubIp",type: "string", title: "<b>Hubitat Device IP Address</b>", required: true
            input name: "refreshEnable",type: "bool", title: "<b>Enable to Refresh every 30mins</b>", defaultValue: true
            input name: "logInfo", type: "bool", title: "<b>Enable info logging</b>", defaultValue: true
            input name: "logEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: true
		}
	}
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
    initialize()
}

def initialize(){
	if (logEnable){
		log.trace "Verbose logging has been enabled for the next 30 minutes."
		runIn(1800, logsOff)
	}
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
            rule = "ON Power1#state DO webquery http://"+ hubIp + ":39501/ POST SwitchOne%value% ENDON " +
                "ON Power2#state DO webquery http://"+ hubIp + ":39501/ POST SwitchTwo%value% ENDON " +
                "ON Power3#state DO webquery http://"+ hubIp + ":39501/ POST SwitchThree%value% ENDON " +
                "ON Tele-AM2301#Temperature DO webquery http://" + hubIp + ":39501/ POST Temperature%value% ENDON " +
                "ON Tele-AM2301#Humidity DO webquery http://" + hubIp + ":39501/ POST Humidity%value% ENDON " +
                "ON Tele-AM2301#DewPoint DO webquery http://" + hubIp + ":39501/ POST DewPoint%value% ENDON "
                
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
    setpoint = device.currentValue("heatingSetpoint") as Integer
    if (logEnable) log.debug "${json}"
    if (json.contains("SwitchOne")){
        if (logEnable) log.debug "Found the word SwitchOne"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - Power Relay is On"
            sendEvent(name:"switch",value:"on")
            sendEvent(name:"thermostatMode", value: "on")
            if (setpoint < 64 || setpoint > 70){
                setHeatingSetpoint(68)
            }
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - is Off"
            sendEvent(name:"switch",value:"off")
            sendEvent(name:"thermostatMode", value: "off")
            runIn(1,relay2Off)
            runIn(1,relay3Off)
        }
    }
    if (json.contains("SwitchTwo")){
        if (logEnable) log.debug "Found the word SwitchTwo"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - Blower Relay is On"
            sendEvent(name:"thermostatMode", value: "heat")
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - Blower Relay is Off"
            status = device.currentValue("switch")
            if (status == "on"){
                sendEvent(name:"thermostatMode", value: "on")
                runIn(1,relay3Off)
            }else{
                sendEvent(name:"thermostatMode", value: "off")
            }   
        }
    }
    if (json.contains("SwitchThree")){
        if (logEnable) log.debug "Found the word SwitchThree"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - Thermostat Relay is On"
            sendEvent(name:"thermostatOperatingState", value: "heating")
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - Thermostat Relay is Off"
            sendEvent(name:"thermostatOperatingState", value: "idle")
        }
    }
    if (json.contains("Temperature")){
        json = json?.replace("Temperature","") 
        if (logEnable) log.debug "Found the word Temperature"
        if (logInfo) log.info "$device.label - Temperature is $json"
        sendEvent(name:"temperature",value:"$json")
    }
    if (json.contains("Humidity")){
        json = json?.replace("Humidity","") 
        if (logEnable) log.debug "Found the word Humidity"
        if (logInfo) log.info "$device.label - Humidity is $json"
        sendEvent(name:"humidity",value:"$json")
    }
    if (json.contains("DewPoint")){
        json = json?.replace("DewPoint","") 
        if (logEnable) log.debug "Found the word DewPoint"
        if (logInfo) log.info "$device.label - DewPoint is $json"
        sendEvent(name:"dewPoint",value:"$json")
    }
    runIn(1,thermostatState)
}

def on(){
    setThermostatMode("on")
}

def off(){
    setThermostatMode("off")
}

def toggle(){
    status = device.currentValue("switch")
    if (status == "on"){
        off()
    }else{
        on()
    }
}

def setThermostatMode(data){
    if (logInfo) log.info "$device.label Mode set to $data"
    if (data == "heat"){
        relay1On()
        relay2On()
        runIn(1,thermostatState)
    }
    else if (data == "on"){
        relay1On()
        relay2Off()
    }
    else if (data == "off"){
        relay1Off()
        state.heatOn = false
    }
}

def setHeatingSetpoint(value){
    if (value <= 80 && value >= 60){
        if (logInfo) log.info "$device.label - Heating Setpoint set to $value"
        sendEvent(name:"heatingSetpoint",value:"$value")
        runIn(1,thermostatState)
    }else{
        if (logInfo) log.info "$device.label - Heating Setpoint of $value is Out of Range - Not Set"
    }       
}

def thermostatState(){    //evaluate temps to determine heating or idle state
    switchStatus = device.currentValue("thermostatMode")
    if (switchStatus == "heat") {
        currentTemp = device.currentValue("temperature")
        setTemp = device.currentValue("heatingSetpoint")
        tempLow = (setTemp - 1)
        status = device.currentValue("thermostatOperatingState")
        if (status == "heating"){
            state.heatOn = true
        }else{
            state.heatOn = false
        }
        if (setTemp <= currentTemp && state.heatOn){
            if (logInfo) log.info "$device.label Current temp is $currentTemp, Setpoint is $setTemp - changing to idle"
            relay3Off()
        }
        else if (currentTemp <= tempLow && !state.heatOn){
            if (logInfo) log.info "$device.label Current temp is $currentTemp, Setpoint is $setTemp - changing to heating"
            relay3On()
        }
        else{
            if (logInfo) log.info "$device.label Current temp is $currentTemp, Setpoint is $setTemp - Current status is $status , No change needed"
        }
    }
}

def relay1On() {
    if (logEnable) log.debug "Sending On Command to [${settings.deviceIp}]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER1%20On") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER1 == "ON"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def relay1Off() {
    if (logEnable) log.debug "Sending Off Command to [${settings.deviceIp}]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER1%20Off") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER1 == "OFF"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def relay2On() {
    if (logEnable) log.debug "Sending On 2 Command to [${settings.deviceIp}]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER2%20On") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER2 == "ON"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def relay2Off() {
    if (logEnable) log.debug "Sending Off 2 Command to [${settings.deviceIp}]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER2%20Off") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER2 == "OFF"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def relay3On() {
    if (logEnable) log.debug "Sending On 3 Command to [${settings.deviceIp}]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER3%20On") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER3 == "ON"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def relay3Off() {
    if (logEnable) log.debug "Sending Off 3 Command to [${settings.deviceIp}]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER3%20Off") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER3 == "OFF"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def refresh() {
    if(settings.deviceIp){
        if (logEnable) log.debug "Refreshing Device Status - [${settings.deviceIp}]"
        try {
           httpGet("http://" + deviceIp + "/cm?cmnd=status%200") { resp ->
           def json = (resp.data)
            if (logEnable) log.debug "${json}"
               if (json.containsKey("StatusSNS")){
                   if (logEnable) log.debug "AM2301 sensor found"
                   temp = json.StatusSNS.AM2301.Temperature
                   hum = json.StatusSNS.AM2301.Humidity
                   dew = json.StatusSNS.AM2301.DewPoint
                   sendEvent(name:"temperature",value:"${temp}")
                   sendEvent(name:"humidity",value:"${hum}")
                   sendEvent(name:"dewPoint",value:"${dew}")
                   if (logEnable) log.debug "Temperature of $device.label is ${temp} Humidity is ${hum} Dew Point is ${dew}"
                   if (logInfo) log.info "$device.label -Temperature is ${temp}, Humidity is ${hum}%, Dew Point is ${dew}"
               }
               if (json.containsKey("StatusSTS")){
                   if (logEnable) log.debug "PWR status found"
                   status1 = json.StatusSTS.POWER1 as String
                   status2 = json.StatusSTS.POWER2 as String
                   status3 = json.StatusSTS.POWER3 as String
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logEnable) log.debug "Wifi signal strength $signal db"
                   sendEvent(name:"wifi",value:"${signal}db")
                   if (logEnable) log.debug "$device.label $deviceIp - Relay 1 - $status1, Relay 2 - $status2, Relay 3 - $status3"
                   if (logInfo) log.info "$device.label Relay 1 - $status1,  Relay 2 - $status2,  Relay 3 - $status3"
                   if (status3 == "ON"){
                       sendEvent(name:"thermostatOperatingState", value: "heating")
                   }else{
                       sendEvent(name:"thermostatOperatingState", value: "idle")
                   }    
                   if (status1 == "ON" && status2 == "OFF"){
                       sendEvent(name:"switch",value:"on")
                       sendEvent(name:"thermostatMode", value: "on")
                   }
                   else if (status1 == "ON" && status2 == "ON"){
                       sendEvent(name:"switch",value:"on")
                       sendEvent(name:"thermostatMode", value: "heat")
                   }
                   else if (status1 == "OFF"){
                       sendEvent(name:"switch",value:"off")
                       sendEvent(name:"thermostatMode", value: "off")
                   }
               }
           }
        }catch (Exception e) {
            sendEvent(name:"wifi",value:"offline")
            if (logInfo) log.error "$device.label Unable to connect, device is <b>OFFLINE</b>"
            log.warn "Call to on failed: ${e.message}"
        }
    }
    runIn(1,thermostatState)
}

def logsOff(){
    log.warn "debug logging disabled..."
	device.updateSetting("logEnable", [value:"false",type:"bool"])
}
