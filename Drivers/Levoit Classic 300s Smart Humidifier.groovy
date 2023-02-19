/**
 *  Levoit Classic 300s Smart Humidifier
 *  
 *  VeeSync cloud integration
 * 
 *  Copyright 2022 Gassgs/ Gary Gassmann
 *  Based on the VeeSync Integration writen by *Niklas Gustafsson*
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
 *  V1.0.0  11-27-2022       first run   
 *  V1.1.0  12-01-2022       removed parent child structure
 *  V1.2.0  12-02-2022       bug fixes and improvements
 *  V1.3.0  12-03-2022       Cleanup and update improvements
 *  V1.4.0  12-04-2022       Added fanSpeed options, removed "mode"
 */

def driverVer() { return "1.4" }
import java.security.MessageDigest

metadata {
    definition(
        name: "Levoit Classic300S Humidifier",namespace: "Gassgs",author: "Niklas Gustafsson/Gassgs",description: "Levoit Classic 300S Humidifier Driver",)
        {
            capability "Switch"
            capability "FanControl"
            capability "Actuator"
            capability "Relative Humidity Measurement"
            capability "Sensor"
            
            attribute "autoStop","string";
            attribute "displayOn","string";
            attribute "humiditySetpoint", "number";
            attribute "mistLevel","string";
            attribute "nightLight","string";
            attribute "waterLevel","string";
            
            command "setDisplay", [[name:"Display*", type: "ENUM", description: "Display", constraints: ["on", "off"] ] ]
            command "autoStop", [[name:"Auto Stop*", type: "ENUM", description: "Auto Stop", constraints: ["on", "off"] ] ]
            command "setHumidity",[[name: "Humidity",type: "NUMBER",description:"Desired Humidity Level - 30% -80%",constraints: ["30..80"]]]
            command "nightLight", [[name:"Night Light*", type: "ENUM", description: "Night Light", constraints: ["on","dim","off"] ] ]
            command "setMistLevel", [[name:"Mist Level*", type: "ENUM", description: "Mist Level", constraints: ["1", "2", "3", "4", "5", "6", "7", "8", "9"] ] ]
            command "setSpeed", [[name:"Speed*", type: "ENUM", description: "Speed", constraints: ["auto", "low", "medium-low", "medium", "medium-high", "high", "sleep"] ] ]
            command "update"

        }

    preferences {
        def refreshRate = [:]
        refreshRate << ["Disabled" : "Disabled"]
        refreshRate << ["1 min" : "Refresh every minute"]
        refreshRate << ["5 min" : "Refresh every 5 minutes"]
        refreshRate << ["10 min" : "Refresh every 10 minutes"]
        refreshRate << ["15 min" : "Refresh every 15 minutes"]
        refreshRate << ["30 min" : "Refresh every 30 minutes"]
        input(name: "email", type: "string", title: "<b>Email Address</b>", description: "<b; font-style: italic'>VeSync Account Email Address</b>",required: true);
        input(name: "password", type: "password", title: "<b>Password</b>", description: "<b; font-style: italic'>VeSync Account Password</b>");
        input("refreshInterval", "enum", title: "<b>Refresh Interval</b>",options: refreshRate, defaultValue: "5 min", required: true )
        input("debugOutput", "bool", title: "<b>Enable debug logging?</b>", defaultValue: true, required: false)
        input("infoEnable", "bool", title: "<b>Enable info text logging?</b>", defaultValue: true, required: false)
    }
}

def installed() {
	logDebug "Installed with settings: ${settings}"
    updated();
}

def updated() {
	logDebug "Updated with settings: ${settings}"
    //state.clear()
    unschedule()
    state.DriverVersion=driverVer()
	initialize()
    if (settings.email != null && settings.password != null && state.device == null){
        getDeviceData()
    }
    
     switch(refreshInterval) {
         case "Disabled" :
			unschedule(update)
            logDebug "Update schedule disabled"
            if (infoEnable) log.info "$device.label update schedule disabled"
			break
        case "1 min" :
			runEvery1Minute(update)
            logDebug "Update every minute schedule"
            if (infoEnable) log.info "$device.label update every minute schedule"
			break
		case "5 min" :
			runEvery5Minutes(update)
            logDebug "Update every 5 minutes schedule"
            if (infoEnable) log.info "$device.label update every 5 minutes schedule"
			break
        case "10 min" :
			runEvery10Minutes(update)
            logDebug "Update every 10 minutes schedule"
            if (infoEnable) log.info "$device.label update every 10 minutes schedule"
			break
		case "15 min" :
			runEvery15Minutes(update)
            logDebug "Update every 15 minutes schedule"
            if (infoEnable) log.info "$device.label update every 15 minutes schedule"
			break
		case "30 min" :
			runEvery30Minutes(update)
            logDebug "Update every 30 minutes schedule"
            if (infoEnable) log.info "$device.label update every 30 minutes schedule"
            break
	}
    if (state.device != null){
        runIn(2, update)
    }
    if (settings?.debugOutput) runIn(1800, logDebugOff);
}

def uninstalled() {
	logDebug "Uninstalled Device $device.name"
}

def initialize() {
	logDebug "initializing"
    if (settings.email != null && settings.password != null){
        login()
    }
}

def login()
{
    def logmd5 = MD5(password)

	def params = [
		uri: "https://smartapi.vesync.com/cloud/v1/user/login",
		contentType: "application/json",
        requestContentType: "application/json",
        body: [
            "timeZone": "America/Los_Angeles",
            "acceptLanguage": "en",
            "appVersion": "2.5.1",
            "phoneBrand": "SM N9005",
            "phoneOS": "Android",
            "traceId": "1634265366",
            "email": email,
            "password": logmd5,
            "devToken": "",
            "userType": "1",
            "method": "login"
        ],
		headers: [ 
            "Accept": "application/json",
            "Accept-Encoding": "gzip, deflate, br",
            "Connection": "keep-alive",
            "User-Agent": "Hubitat Elevation", 
            "accept-language": "en",
            "appVersion": "2.5.1",
            "tz": "America/Los_Angeles"
 ]
	]

    logDebug "login: ${params.uri}"

	try
	{
		def result = false
		httpPost(params) { resp ->
			if (checkHttpResponse("login", resp))
			{
                state.token = resp.data.result.token
                state.accountID = resp.data.result.accountID
			}
		}
		return result
	}
	catch (e)
	{
        logDebug e.toString();
		checkHttpResponse("login", e.getResponse())
		return false
	}
}

def getDeviceData() {

	def params = [
		uri: "https://smartapi.vesync.com/cloud/v1/deviceManaged/devices",
		contentType: "application/json",
        requestContentType: "application/json",
        body: [
            "timeZone": "America/Los_Angeles",
            "acceptLanguage": "en",
            "appVersion": "2.5.1",
            "phoneBrand": "SM N9005",
            "phoneOS": "Android",
            "traceId": "1634265366",
            "accountID": state.accountID,
            "token": state.token,
            "method": "devices",
            "pageNo": "1",
            "pageSize": "100"
        ],
		headers: [ 
            "tz": "America/Los_Angeles",
            "Accept": "application/json",
            "Accept-Encoding": "gzip, deflate, br",
            "Connection": "keep-alive",
            "User-Agent": "Hubitat Elevation", 
            "accept-language": "en",
            "appVersion": "2.5.1",
            "accountID": state.accountID,
            "tk": state.token ]
	]
	try
	{
		def result = false
		httpPost(params) { resp ->
			if (checkHttpResponse("getDevices", resp))
			{
                def newList = [:]

				for (device in resp.data.result.list) {
                    logDebug "Device found: ${device.deviceType} / ${device.deviceName} / ${device.macID}"

                    def dtype = device.deviceType;

                    if (dtype == "Classic300S") {
                        setName(device.deviceType as String)
                        updateDataValue("configModule", device.configModule);
                        updateDataValue("cid", device.cid);
                        updateDataValue("uuid", device.uuid)
                    }
                    else{
                        logDebug "Device found ${device.deviceType} Not Supported With This Driver"
                    }
                        
                }
				result = true
			}
		}
		return result
	}
	catch (e)
	{
        logError e.getMessage()
		return false
	}
}

def setName(data){
    device.setName("Levoit " + data as String)
    if (state.device == null){
        device.setLabel(data as String)
    }
    state.device = data as String
}

def on() {
    logDebug "on()"
	handlePower(true)
    update()
}

def off() {
    logDebug "off()"
	handlePower(false)
    update()
}

def nightLight(data) {
    logDebug "nightLight($data)"
	handleNightLight(data)
    update()
}

def setHumidity(data) {
    if (data < 30 || data > 80){
        logDebug "setHumidity($data) is outside of valid range"
    }else{
        logDebug "setHumidity($data)"
        handleHumidity(data)
        update()
    }
}

def cycleSpeed() {
    logDebug "cycleSpeed()"

    def speed = device.currentValue"speed";

    switch(speed) {
        case "low":
            speed = "medium-low";
            break;
        case "medium-low":
            speed = "medium";
            break;
        case "medium":
            speed = "medium-high";
            break;
        case "medium-high":
            speed = "high";
            break;
        case "high":
            speed = "low";
            break;
        case "auto":
            speed = "low";
            break;
        case "sleep":
            speed = "low";
            break;
    }
    setSpeed(speed)
}

def setSpeed(speed) {
    logDebug "setSpeed(${speed})"
    if (speed == "low") {
        handleMistLevel(1)
    }
    else if (speed == "medium-low") {
        handleMistLevel(3)  
    }
    else if (speed == "medium") {
        handleMistLevel(5) 
    }
    else if (speed == "medium-high") {
        handleMistLevel(7)  
    }
    else if (speed == "high") {
        handleMistLevel(9)  
    }
    else if (speed == "auto") {
        setMode(speed)
    }
    else if (speed == "sleep") {
        setMode(speed)
    }
    update()   
}

def setMistLevel(value){
    handleMistLevel(value)
    logDebug "setMistLevel $value"
    update()
}

def setMode(mode) {
    logDebug "setMode(${mode})"
    handleMode(mode)
    update()
}

def setDisplay(value) {
    logDebug "setDisplay(${value})"
    handleDisplayOn(value)
    update()
}

def autoStop(value) {
    logDebug "auto stop (${value})"
    handleAutoStop(value)
    update()
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}

def logError(msg) {
    log.error msg
}

void logDebugOff() {
  if (settings?.debugOutput) device.updateSetting("debugOutput", [type: "bool", value: false]);
}

def handlePower(value) {

    def result = false

    sendBypassRequest([
                data: [ enabled: value, id: 0],
                "method": "setSwitch",
                "source": "APP" ]) { resp ->
			if (checkHttpResponse("handleOn", resp))
			{
                def operation = on ? "ON" : "OFF"
                logDebug "turned ${operation}()"
				result = true
			}
		}
    return result
}

def handleNightLight(data) {
    if (data == "off"){
        data = 0
        }
    else if (data == "dim"){
        data = 50
        }
    else if (data == "on"){
        data = 100
        }

    def result = false

    sendBypassRequest([
                data: [ night_light_brightness: data, id: 0],
                "method": "setNightLightBrightness",
                "source": "APP"
            ]) { resp ->
			if (checkHttpResponse("handleNightLight", resp))
			{
                logDebug "Night Light Setting $data"
				result = true
			}
		}
    return result
}

def handleMistLevel(value) {
    value = value as Integer

    def result = false

    sendBypassRequest([
                data: [level: value,id:0,type:"mist"],
                "method": "setVirtualLevel",
                "source": "APP"
            ]) { resp ->
			if (checkHttpResponse("handleMistLevel", resp))
			{
                logDebug "Mist Level $value"
				result = true
			}
		}
    return result
}

def handleHumidity(value) {

    def result = false

    sendBypassRequest([
                data: [target_humidity: value],
                "method": "setTargetHumidity",
                "source": "APP",
            ]) { resp ->
			if (checkHttpResponse("handleSetpoint", resp))
			{
                logDebug "Humidity Setpoint $value"
				result = true
			}
		}
    return result
}

def handleMode(mode) {

    def result = false

    sendBypassRequest([
                data: [ "mode": mode, id: 0],
                "method": "setHumidityMode",
                "source": "APP"
            ]) { resp ->
			if (checkHttpResponse("handleMode", resp))
			{
                logDebug "Set mode $mode"
				result = true
			}
		}
    return result
}

def handleDisplayOn(value) {
    logDebug "handleDisplayOn()"

    def result = false

    sendBypassRequest([
                data: [ "state": (value == "on")],
                "method": "setDisplay",
                "source": "APP"
            ]) { resp ->
			if (checkHttpResponse("handleDisplayOn", resp))
			{
                logDebug "Set display"
				result = true
			}
		}
    return result
}

def handleAutoStop(value){
    logDebug "handleAutoStop()"

    def result = false

    sendBypassRequest([
                data: [ "enabled": (value == "on")],
                "method": "setAutomaticStop",
                "source": "APP"
            ]) { resp ->
			if (checkHttpResponse("handleAutoStop", resp))
			{
                logDebug "Set Auto Stop $value"
				result = true
			}
		}
    return result
}

def update() {

    logDebug "update()"

    def result = null

    sendBypassRequest([
                "method": "getHumidifierStatus",
                "source": "APP",
                "data": [:]
            ]) { resp ->
			if (checkHttpResponse("update", resp))
			{
                def status = resp.data.result
                if (status == null)
                    logError "No status returned from getHumidifierStatus: ${resp.msg}"
                else
                    result = update(status, null)                
			}
		}
    return result
}

def update(status, data){

    logDebug status
    
    def relay = status.result.enabled ? "On" : "Off"
    def hum = status.result.humidity
    def humSet = status.result.configuration.auto_target_humidity
    def mode = status.result.mode
    def lightStatus = status.result.night_light_brightness
    def speed = status.result.mist_virtual_level

    handleEvent("switch", status.result.enabled ? "on" : "off")
    handleEvent("humidity", status.result.humidity)
    handleEvent("humiditySetpoint", status.result.configuration.auto_target_humidity)
    handleEvent("displayOn", status.result.display)
    handleEvent("autoStop", status.result.configuration.automatic_stop)
    handleEvent("mistLevel", status.result.mist_virtual_level)
    if (mode =="auto"){
        speed = "auto"
        handleEvent("speed", speed)
    }
    else if (mode == "sleep"){
        speed = "sleep"
        handleEvent("speed", speed)
    }
    else if (mode == "manual" && speed == 1){
        speed = "low"
        handleEvent("speed", speed)
    }
    else if (mode == "manual" && speed == 2 || mode == "manual" && speed == 3){
        speed = "medium-low"
        handleEvent("speed", speed)
    }
    else if (mode == "manual" && speed == 4 || mode == "manual" && speed == 5){
        speed = "medium"
        handleEvent("speed", speed)
    }
    else if (mode == "manual" && speed == 6 || mode == "manual" && speed == 7){
        speed = "medium-high"
        handleEvent("speed", speed)
    }
    else if (mode == "manual" && speed == 8 || mode == "manual" && speed == 9){
        speed = "high"
        handleEvent("speed", speed)
    }
    if (lightStatus == 0){
        light = "off"
        handleEvent("nightLight", "$light")
    }
    else if (lightStatus == 50){
        light = "dim"
        handleEvent("nightLight", "$light")
    }
    else if (lightStatus == 100){
        light = "on"
        handleEvent("nightLight", "$light")
    }
    def waterLevel = status.result.water_lacks
    def waterTank = status.result.water_tank_lifted
    if (waterTank == true){
        handleEvent("waterLevel", "tank missing")
    }
    else if (waterLevel == true){
        handleEvent("waterLevel", "low")
    }
    else if (waterLevel == false){
        handleEvent("waterLevel", "ok")
    }
    if (infoEnable) log.info "$device.label - Power - $relay | Mode - $mode | Mist - $speed | Humidity - $hum | Setpoint - $humSet"
}

private void handleEvent(name, val){
    logDebug "handleEvent(${name}, ${val})"
    device.sendEvent(name: name, value: val)
}

def sendBypassRequest(payload, Closure closure)
{
    logDebug "sendBypassRequest(${payload})"

    def params = [
		uri: "https://smartapi.vesync.com/cloud/v2/deviceManaged/bypassV2",
		contentType: "application/json; charset=UTF-8",
        requestContentType: "application/json; charset=UTF-8",
        body: [
            "timeZone": "America/Los_Angeles",
            "acceptLanguage": "en",
            "appVersion": "2.5.1",
            "phoneBrand": "SM N9005",
            "phoneOS": "Android",
            "traceId": "1634265366",
            "cid": device.getDataValue("cid"),
            "configModule": device.getDataValue("configModule"),
            "payload": payload,
            "accountID": getAccountID(),
            "token": getAccountToken(),
            "method": "bypassV2",
            "debugMode": false,
            "deviceRegion": "US"
        ],
		headers: [
            "tz": "America/Los_Angeles",
            "Accept": "application/json",
            "Accept-Encoding": "gzip, deflate, br",
            "Connection": "keep-alive",
            "User-Agent": "Hubitat Elevation",
            "accept-language": "en",
            "appVersion": "2.5.1",
            "accountID": getAccountID(),
            "tk": getAccountToken() ]
	]
	try
	{
		httpPost(params, closure)
		return true
	}
	catch (e)
	{
        logDebug e.getMessage()
		return false
	}
}

def getAccountToken() {
    return state.token
}

def getAccountID() {
    return state.accountID
}

def MD5(s) {
	def digest = MessageDigest.getInstance("MD5")
	new BigInteger(1,digest.digest(s.getBytes())).toString(16).padLeft(32,"0")
} 

def parseJSON(data) {
    def json = data.getText()
    def slurper = new groovy.json.JsonSlurper()
    return slurper.parseText(json)
}

def checkHttpResponse(action, resp) {
    if (resp.status == 200 || resp.status == 201 || resp.status == 204){
		return true
        
    }else if (resp.status == 400 || resp.status == 401 || resp.status == 404 || resp.status == 409 || resp.status == 500){
        log.error "${action}: ${resp.status} - ${resp.getData()}"
		return false
        
	}else{
		log.error "${action}: unexpected HTTP response: ${resp.status}"
		return false
	}
}
