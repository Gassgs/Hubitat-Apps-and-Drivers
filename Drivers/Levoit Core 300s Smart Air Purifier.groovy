/**
 *  Levoit Core 300s Smart Air Purifier
 *  
 *  Veesync cloud integration
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
 *  V1.1.0  12-01-2022       removing parent child structure
 *  V1.2.0  12-02-2022       bug fixes and improvements
 *  V1.3.0  12-03-2022       Cleanup and update improvements
 *  V1.4.0  12-04-2022       Added auto and sleep into fanSpeed options, removed "mode"
 */

def driverVer() { return "1.4" }
import java.security.MessageDigest

metadata {
    definition(
        name: "Levoit Core300S Air Purifier",namespace: "Gassgs",author: "Niklas Gustafsson/Gassgs",description: "Levoit Core 300S Air Purifier Driver",)
        {
            capability "Switch"
            capability "FanControl"
            capability "Actuator"
            capability "AirQuality"
            capability "Sensor"

            attribute "filter", "number";
            attribute "auto_mode", "string";
            attribute "room_size", "string";
            attribute "aqiStatus", "string";
            attribute "pmValue", "number";
            attribute "childLockOn", "string";
            attribute "displayOn","string";

            command "setChildLock", [[name:"Child Lock*", type: "ENUM", description: "Child Lock", constraints: ["on", "off"] ] ]
            command "setDisplay", [[name:"Display*", type: "ENUM", description: "Display", constraints: ["on", "off"] ] ]
            command "setSpeed", [[name:"Speed*", type: "ENUM", description: "Speed", constraints: ["auto", "low", "medium", "high", "sleep"] ] ]
            command "setAutoMode",[[name:"Mode*", type: "ENUM", description: "Mode", constraints: ["default", "quiet", "efficient"]],[name:"Room Size", type: "NUMBER", description: "Room size in square feet"]]
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
        input(name: "email", type: "string", title: "<b>Email Address</b>", description: "<b; font-style: italic'>VeSync Account Email Address</b>", required: true);
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

                    if (dtype == "Core300S") {
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

def cycleSpeed() {
    logDebug "cycleSpeed()"

    def speed = device.currentValue"speed";

    switch(speed) {
        case "low":
            speed = "medium";
            break;
        case "medium":
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
        speed = 1
        handleSpeed(speed)   
    }
    else if (speed == "medium") {
        speed = 2
        handleSpeed(speed)   
    }
    else if (speed == "high") {
        speed = 3
        handleSpeed(speed)
    }
    else if (speed == "auto") {
        setMode(speed)
    }
    else if (speed == "sleep") {
        setMode(speed)
    }
    update()   
}

def setMode(mode) {
    logDebug "setMode(${mode})"
    handleMode(mode)
    update() 
}

def setAutoMode(mode) {
    setAutoMode(mode, 100);
}

def setAutoMode(mode, roomSize) {
    logDebug "setAutoMode(${mode}, ${roomSize})"
    
    if (mode == "efficient") {
        handleAutoMode(mode, roomSize);
    	handleEvent("room_size", roomSize)
    }
    else {
        handleAutoMode(mode);
    }
    handleMode("auto");
    update()
}

def setDisplay(displayOn) {
    logDebug "setDisplay(${displayOn})"
    handleDisplayOn(displayOn)
    update() 
}

def setChildLock(value) {
    logDebug "set Child Lock(${value})"
    handleChildLock(value)
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

def handlePower(on) {

    def result = false

    sendBypassRequest([
                data: [ enabled: on, id: 0 ],
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

def handleSpeed(speed) {

    def result = false

    sendBypassRequest([
                data: [ level: speed , id: 0, type: "wind" ],
                "method": "setLevel",
                "source": "APP"
            ]) { resp ->
			if (checkHttpResponse("handleSpeed", resp))
			{
                logDebug "Set speed"
				result = true
			}
		}
    return result
}

def handleMode(mode) {

    def result = false

    sendBypassRequest([
                data: [ "mode": mode ],
                "method": "setPurifierMode",
                "source": "APP"
            ]) { resp ->
			if (checkHttpResponse("handleMode", resp))
			{
                logDebug "Set mode"
				result = true
			}
		}
    return result
}

def handleAutoMode(mode) {

    def result = false

    sendBypassRequest([
                data: [ "type": mode ],
                "method": "setAutoPreference",
                "source": "APP"
            ]) { resp ->
			if (checkHttpResponse("handleMode", resp))
			{
                logDebug "Set mode"
				result = true
			}
		}
    return result
}

def handleAutoMode(mode, size) {

    def result = false

    sendBypassRequest([
                data: [ "type": mode, "room_size": size ],
                "method": "setAutoPreference",
                "source": "APP"
            ]) { resp ->
			if (checkHttpResponse("handleMode", resp))
			{
                logDebug "Set mode"
				result = true
			}
		}
    return result
}

def handleDisplayOn(displayOn) 
{
    logDebug "handleDisplayOn()"

    def result = false

    sendBypassRequest([
                data: [ "state": (displayOn == "on")],
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

def handleChildLock(value) 
{
    logDebug "handleChildLock()"

    def result = false

    sendBypassRequest([
                data: [ "child_lock": (value == "on")],
                "method": "setChildLock",
                "source": "APP"
            ]) { resp ->
			if (checkHttpResponse("handleChildLock", resp))
			{
                logDebug "Set Child Lock $value"
				result = true
			}
		}
    return result
}

def update() {

    logDebug "update()"

    def result = null

    sendBypassRequest([
                "method": "getPurifierStatus",
                "source": "APP",
                "data": [:]
            ]) { resp ->
			if (checkHttpResponse("update", resp))
			{
                def status = resp.data.result
                if (status == null)
                    logError "No status returned from getPurifierStatus: ${resp.msg}"
                else
                    result = update(status, null)                
			}
		}
    return result
}

def update(status, data)
{
    logDebug status
    
    def relay = status.result.enabled ? "On" : "Off"
    def aq = status.result.air_quality_value
    def mode = status.result.mode
    def filter = status.result.filter_life
    def speed = status.result.level

    handleEvent("switch", status.result.enabled ? "on" : "off")
    handleEvent("auto_mode", status.result.configuration.auto_preference.type)
    handleEvent("displayOn", status.result.display)
    handleEvent("childLockOn", status.result.child_lock)
    handleEvent("room_size", status.result.configuration.auto_preference.room_size)
    handleEvent("filter", status.result.filter_life)
    handleEvent("pmValue", status.result.air_quality_value)
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
    else if (mode == "manual" && speed == 2){
        speed = "medium"
        handleEvent("speed", speed)
    }
    else if (mode == "manual" && speed == 3){
        speed = "high"
        handleEvent("speed", speed)
    }
    if (infoEnable) log.info "$device.label - Power - $relay | Mode - $mode | PM2.5 - $aq &micro;g/m&sup3 | Filter - ${filter}%"

    updateAQIandFilter(status.result.air_quality_value.toString(),status.result.filter_life)
}

private void handleEvent(name, val)
{
    logDebug "handleEvent(${name}, ${val})"
    device.sendEvent(name: name, value: val)
}

private void updateAQIandFilter(String val, filter) {

    logDebug "updateAQI(${val})"

    //
    // Conversions based on https://en.wikipedia.org/wiki/Air_quality_index
    //
    BigDecimal pm = val.toBigDecimal();

    BigDecimal aqi;

    if (state.prevPM == null || state.prevPM != pm || state.prevFilter == null || state.prevFilter != filter) {

        state.prevPM = pm;
        state.prevFilter = filter;

        if      (pm <  12.1) aqi = convertRange(pm,   0.0,  12.0,   0,  50);
        else if (pm <  35.5) aqi = convertRange(pm,  12.1,  35.4,  51, 100);
        else if (pm <  55.5) aqi = convertRange(pm,  35.5,  55.4, 101, 150);
        else if (pm < 150.5) aqi = convertRange(pm,  55.5, 150.4, 151, 200);
        else if (pm < 250.5) aqi = convertRange(pm, 150.5, 250.4, 201, 300);
        else if (pm < 350.5) aqi = convertRange(pm, 250.5, 350.4, 301, 400);
        else                 aqi = convertRange(pm, 350.5, 500.4, 401, 500);

        handleEvent("airQualityIndex", aqi);

        String danger;
        String color;

        if      (aqi <  51) { danger = "good";                           color = "7e0023"; }
        else if (aqi < 101) { danger = "moderate";                       color = "fff300"; }
        else if (aqi < 151) { danger = "unhealthy for sensitive groups"; color = "f18b00"; }
        else if (aqi < 201) { danger = "unhealthy";                      color = "e53210"; }
        else if (aqi < 301) { danger = "very unhealthy";                 color = "b567a4"; }
        else if (aqi < 401) { danger = "hazardous";                      color = "7e0023"; }
        else {                danger = "hazardous";                      color = "7e0023"; }

        //handleEvent("aqiColor", color)   *not currently used*
        handleEvent("aqiStatus", danger)

        //def html = "AQI: ${aqi} | PM2.5: ${pm} &micro;g/m&sup3 | Filter: ${filter}%"    *not currently used*
    }
}

private BigDecimal convertRange(BigDecimal val, BigDecimal inMin, BigDecimal inMax, BigDecimal outMin, BigDecimal outMax, Boolean returnInt = true) {
  // Let make sure ranges are correct
  assert (inMin <= inMax);
  assert (outMin <= outMax);

  // Restrain input value
  if (val < inMin) val = inMin;
  else if (val > inMax) val = inMax;

  val = ((val - inMin) * (outMax - outMin)) / (inMax - inMin) + outMin;
  if (returnInt) {
    // If integer is required we use the Float round because the BigDecimal one is not supported/not working on Hubitat
    val = val.toFloat().round().toBigDecimal();
  }

  return (val);
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
	if (resp.status == 200 || resp.status == 201 || resp.status == 204)
		return true
	else if (resp.status == 400 || resp.status == 401 || resp.status == 404 || resp.status == 409 || resp.status == 500)
	{
		log.error "${action}: ${resp.status} - ${resp.getData()}"
		return false
	}
	else
	{
		log.error "${action}: unexpected HTTP response: ${resp.status}"
		return false
	}
}
