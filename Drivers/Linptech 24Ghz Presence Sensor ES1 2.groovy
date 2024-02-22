/*
 *  Linptech / Moes 24Ghz Presence Sensor ES1 driver 2.0
 *
 *  Driver made possible by the work of Krassimir Kossev
 *  Code pulled from methods developed by "Krassimir Kossev" in the Tuya 4in1 driver
 *  https://raw.githubusercontent.com/kkossev/Hubitat/development/Drivers/Tuya%20Multi%20Sensor%204%20In%201/Tuya%20Multi%20Sensor%204%20In%201.groovy
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
 *  V1.0.0  09-26-2023       Modifying Tuya 4in1 driver by "Krassimir Kossev" to support only Linptech/Moes 24Ghz Presence Sensor ES1
 *  V1.1.0  09-27-2023       Fixed lux reporting and parsing, clean up / todo - distance reporting option
 *  V1.2.0  09-30-2023       Added distance reporting
 *  V1.3.0  10-01-2023       Added fade time option and states for preferences
 *  V1.4.0  10-03-2023       Addjust fade time range to match Tuya hub settings
 *  V1.5.0  02-03-2024       Added addtional info logging
 *  V1.6.0  02-14-2024       Added fade Time and existance time attributes
 *  V1.7.0  02-18-2024       Changed commands to replace preference settings, added actuator capability
 *  V1.8.0  02-19-2024       Added Device Health Check (testing)
 *  V1.9.0  02-20-2024       Fix for existance time = 1 and changed atribute to number
 *  V2.0.0  02-21-2024       Changed health check method for lower hub resource usage, code cleanup
 */

def driverVer() { return "2.0" }

import hubitat.zigbee.clusters.iaszone.ZoneStatus

metadata
{
	definition(name: "Linptech 24Ghz Presence Sensor ES1 2.0", namespace: "Gassgs", author: "Krassimir Kossev", importUrl: "https://raw.githubusercontent.com/Gassgs/Hubitat-Apps-and-Drivers/master/Drivers/Linptech%2024Ghz%20Presence%20Sensor%20ES1.groovy", singleThreaded: true )
	{
		capability "Motion Sensor"
		capability "IlluminanceMeasurement"
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		
		command "setMotionSensitivity", [[name:"Set Motion Sensitivity", type: "ENUM",description: "Motion Detection Sensitivity", constraints: ["low","medium-low","medium","medium-high","high"],defaultValue: "high"]]
		command "setStaticSensitivity", [[name:"Set Static Sensitivity", type: "ENUM",description: "Static Detection Sensitivity", constraints: ["low","medium-low","medium","medium-high","high"],defaultValue: "high"]]
		command "setDetectionDistance", [[name:"Set Detection Distance", type: "ENUM",description: "Detection Distance in Meters", constraints: [1.5,2.25,3.0,3.75,4.5,5.25,6.0],defaultValue: 6.0]]
		command "setFadeTime", [[name:"Set Fade Time", type: "NUMBER",description: "Fade Timeout in Seconds", constraints: "0..10000",defaultValue: "10"]]
		
		attribute "distance", "number"
		attribute "motionSensitivity", "string"
		attribute "staticSensitivity", "string"
		attribute "distanceLimit", "string"
		attribute "existanceTime", "number"
		attribute "fadeTime", "number"
		attribute "status", "string"

	fingerprint inClusters: "0000,0003,0004,0005,E002,4000,EF00,0500", outClusters: "0019,000A", manufacturer: "_TZ3218_awarhusb", model: "TS0225", deviceJoinName: "LINPTECH 24Ghz Human Presence Detector"
	}

	preferences{
		section{
			input "luxThreshold", "number", title: "<b>Lux threshold</b>", description: "<i>Range (0..999)</i>", range: "0..999", defaultValue: 5
			input "enableDistance", "bool", title: "<b>Enable Distance Reporting?</b>", defaultValue: false, required: false, multiple: false
			input "healthCheckEnabled", "bool", title: "<b>Enable Health Check</b>", defaultValue: false, required: false
			if(healthCheckEnabled){
				def pingRate = [:]
				pingRate << ["5 min" : "5 minutes"]
				pingRate << ["10 min" : "10 minutes"]
				pingRate << ["15 min" : "15 minutes"]
				pingRate << ["30 min" : "30 minutes"]
				pingRate << ["60 min" : "60 minutes"]
				input("healthCheckInterval", "enum", title: "<b>Health Check Interval</b>",options: pingRate, defaultValue: "15 min", required: true )
			}
			input "enableInfo", "bool", title: "<b>Enable info logging?</b>", defaultValue: true, required: false, multiple: false
			input "enableDebug", "bool", title: "<b>Enable debug logging?</b>", defaultValue: false, required: false, multiple: false
		}
	}
}

Map parseDescriptionAsMap( String description )
{
    def descMap = [:]
    try {
        descMap = zigbee.parseDescriptionAsMap(description)
        return descMap
    }
    catch (e1) {
        logDebug "exception ${e1} caught while processing parseDescriptionAsMap <b>myParseDescriptionAsMap</b> description:  ${description}"
        descMap = [:]
        try {
            descMap += description.replaceAll('\\[|\\]', '').split(',').collectEntries { entry ->
                def pair = entry.split(':')
                [(pair.first().trim()): pair.last().trim()]
            }
            if (descMap.value != null) {
                descMap.value = zigbee.swapOctets(descMap.value)
            }
        }
        catch (e2) {
            logWarn "exception ${e2} caught while parsing using an alternative method <b>myParseDescriptionAsMap</b> description:  ${description}"
            return [:]
        }
        logDebug "alternative method parsing success: descMap=${descMap}"
    }
    return descMap
}

def parse(String description) {
    Map descMap = [:]
    logDebug "parse: zone status: $description"
    if (description?.startsWith('zone status')){
        logDebug "parse: zone status: $description"
        parseIasMessage(description)
    }
    else if (description?.startsWith('read attr -')){
        try  {
            descMap = parseDescriptionAsMap(description)
        }
        catch (e) {
            logWarn "exception caught while processing description ${description}"
            return
        }
        if (descMap.cluster == "0400" && descMap.attrId == "0000") {
            def rawLux = Integer.parseInt(descMap.value,16)
            illuminanceEvent( rawLux )
        }
        else if (descMap.cluster  == "E002" && descMap.attrId == "E00A") {
            if (enableDistance){
                processDistance( descMap )
            }
        }
        else if (descMap.cluster  == "E002" && descMap.attrId == "E001") {
                existanceTime( descMap )
        }
        else if (descMap.cluster  == "E002" && descMap.attrId == "E004") {
            motionSensitivity( descMap )
        }
        else if (descMap.cluster  == "E002" && descMap.attrId == "E005") {
            staticSensitivity( descMap )
        }
        else if (descMap.cluster  == "E002" && descMap.attrId == "E00B") {
            distanceLimit( descMap )
        }
    }
    else if (description?.startsWith('catchall')){
        try  {
            descMap = parseDescriptionAsMap(description)
            logDebug "$descMap"
        }
        catch (e) {
            logWarn "exception caught while processing description ${description}"
            return
        }
        if (descMap.command == "06") {
            fadeTime( descMap )
        }
    }
    if (healthCheckEnabled) {
        if (device.currentValue("status") != "online"){
            unschedule(healthExpired)
            sendEvent(name: "status", value:  "online")
            logInfo ("$device.label Online")
		}
    }
}

def healthCheck() {
    runIn(30,healthExpired)
    runIn(2,healthPing)
}

def healthPing() {
    val = device.currentValue("distanceLimit")
    setDetectionDistance( val )
}

def healthExpired() {
	sendEvent(name: "status", value:  "offline")
	logError "$device.label - Offline"
}

private Map parseIasMessage(String description) {
	ZoneStatus zs = zigbee.parseZoneStatus(description)
	translateZoneStatus(zs)
}

private Map translateZoneStatus(ZoneStatus zs) {
	return (zs.isAlarm1Set() || zs.isAlarm2Set()) ? getMotionResult('active') : getMotionResult('inactive')
}

private Map getMotionResult(value){
    status = device.currentValue("motion")
    if (value == "active"){
        if (status != "active"){
            sendEvent(name:"motion",value:"active")
            logInfo "$device.label Motion Active"
        }
    }else{
        if (status != "inactive"){
            sendEvent(name:"motion",value:"inactive")
            logInfo "$device.label Motion Inactive"
        }
    }
}

def illuminanceEvent( rawLux ) {
	def lux = rawLux > 0 ? Math.round(Math.pow(10,(rawLux/10000))) : 0
    illuminanceEventLux( lux as Integer) 
}

def illuminanceEventLux( lux ) {
    Integer illumCorrected = Math.round((lux * ((settings?.illuminanceCoeff ?: 1.00) as float)))
    Integer delta = Math.abs(safeToInt(device.currentValue("illuminance")) - (illumCorrected as int))
    if (device.currentValue("illuminance", true) == null || (delta >= safeToInt(settings?.luxThreshold))) {
        sendEvent("name": "illuminance", "value": illumCorrected, "unit": "lx", "type": "physical", "descriptionText": "Illuminance is ${lux} Lux")
        logInfo "$device.label Illuminance is ${illumCorrected} Lux"
    }
    else {
        logDebug "ignored illuminance event ${illumCorrected} lx : the change of ${delta} lx is less than the ${safeToInt(settings?.luxThreshold)} lux threshold!"
    }
}

def motionSensitivity( descMap ) {
    def value = zigbee.convertHexToInt(descMap.value) 
    logDebug "Cluster ${descMap.cluster} Attribute ${descMap.attrId} value is ${value} (0x${descMap.value})"
    if (value == 1){motionValue = "low"}
    else if (value == 2){motionValue = "medium-low"}
    else if (value == 3){motionValue = "medium"}
    else if (value == 4){motionValue = "medium-high"}
    else if (value == 5){motionValue = "high"}
    else{motionValue = "unknown"}
    logInfo "$device.label Motion Sensitivity - $motionValue"
    sendEvent(name: "motionSensitivity",value:"$motionValue")
}

def staticSensitivity( descMap ) {
    def value = zigbee.convertHexToInt(descMap.value) 
    logDebug "Cluster ${descMap.cluster} Attribute ${descMap.attrId} value is ${value} (0x${descMap.value})"
    if (value == 1){staticValue = "low"}
    else if (value == 2){staticValue = "medium-low"}
    else if (value == 3){staticValue = "medium"}
    else if (value == 4){staticValue = "medium-high"}
    else if (value == 5){staticValue = "high"}
    else{staticValue = "unknown"}
    logInfo "$device.label Static Sensitivity - $staticValue"
    sendEvent(name: "staticSensitivity",value:"$staticValue")
}

def distanceLimit( descMap ) {
    def value = zigbee.convertHexToInt(descMap.value) 
    logDebug "Cluster ${descMap.cluster} Attribute ${descMap.attrId} value is ${value} (0x${descMap.value})"
    distanceValue = (value/100)
    newDistance = distanceValue as String
    currentDistance = device.currentValue("distanceLimit")
    if (newDistance != currentDistance){
        logInfo "$device.label Distance Detection Limit - $distanceValue meters"
        sendEvent(name: "distanceLimit",value:"$distanceValue")
    }
}

def processDistance( descMap ) {
    def value = zigbee.convertHexToInt(descMap.value) 
    logDebug "Cluster ${descMap.cluster} Attribute ${descMap.attrId} value is ${value} (0x${descMap.value})"
    logInfo "$device.label distance is ${value/100} m"
    sendEvent(name : "distance", value : value/100, unit : "m")                
}

def existanceTime( descMap ){
    currentExistanceTime = device.currentValue("existanceTime")
    def value = zigbee.convertHexToInt(descMap.value)
    if (value as Number != currentExistanceTime){
        logInfo "$device.label Existance Time - $value minutes"
        sendEvent(name : "existanceTime", value : "$value")
    }
}

def fadeTime( descMap ) {
    def value = zigbee.convertHexToInt(descMap?.data[9])
    logInfo "$device.label Fade Time - $value seconds"
    sendEvent(name : "fadeTime", value : "$value")
}

def updatePreferences(){
    ArrayList<String> cmds = []
    
    cmds += tuyaBlackMagic()
    if (device.currentValue("fadeTime") == null) {cmds += setFadeTime(10)}
    if (device.currentValue("distanceLimit") == null) {cmds += setDetectionDistance(6.0)}
    if (device.currentValue("motionSensitivity") == null) {cmds += setMotionSensitivity( "high" )}
    if (device.currentValue("staticSensitivity") == null) {cmds += setStaticSensitivity("high")}
    
    if (cmds != null) {
        logDebug "$device.label sending the changed AdvancedOptions"
        sendZigbeeCommands( cmds )
    }
}

def tuyaBlackMagic() {
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0000, [0x0004, 0x000, 0x0001, 0x0005, 0x0007, 0xfffe], [:], delay=200)
    cmds += zigbee.writeAttribute(0x0000, 0xffde, 0x20, 0x13, [:], delay=200)
    return  cmds
}

def setDetectionDistance( data ) {
    def val = data as float
    def value = Math.round(val * 100)
    logDebug "$device.label set MotionDetectionDistance to ${val}m (raw ${value})"
    return zigbee.writeAttribute(0xE002, 0xE00B, 0x20, value as int, [:], delay=200)
}

def setMotionSensitivity( data ) {
    if (data == "low"){val = 1}
    else if (data == "medium-low"){val = 2}
    else if (data == "medium"){val = 3}
    else if (data == "medium-high"){val = 4}
    else if (data == "high"){val = 5}
    def value = val as int
    logDebug "$device.label set MotionDetectionSensitivity to ${value}"
    return zigbee.writeAttribute(0xE002, 0xE004, 0x20, value as int, [:], delay=200)
}

def setStaticSensitivity( data ) {
    if (data == "low"){val = 1}
    else if (data == "medium-low"){val = 2}
    else if (data == "medium"){val = 3}
    else if (data == "medium-high"){val = 4}
    else if (data == "high"){val = 5}
    def value = val as int
    logDebug "$device.label set StaticDetectionSensitivity to ${value}"
    return zigbee.writeAttribute(0xE002, 0xE005, 0x20, value as int, [:], delay=200)
}

def setFadeTime( val ){
    def value = val as int
    logDebug "$device.label set fade time to ${value} seconds"
    return sendTuyaCommand( "65","02", zigbee.convertToHexString(value, 8))
}

private sendTuyaCommand(dp, dp_type, fncmd) {
    ArrayList<String> cmds = []
    int tuyaCmd = 0x04
    cmds += zigbee.command(0xEF00, tuyaCmd, PACKET_ID + dp + dp_type + zigbee.convertToHexString((int)(fncmd.length()/2), 4) + fncmd )
    logDebug "${device.displayName} <b>sendTuyaCommand</b> = ${cmds}"
    return cmds
}

private getPACKET_ID() {
    state.packetID = ((state.packetID ?: 0) + 1 ) % 65536
    return zigbee.convertToHexString(state.packetID, 4)
}

Integer safeToInt(val, Integer defaultVal=0) {
	return "${val}"?.isInteger() ? "${val}".toInteger() : defaultVal
}

Double safeToDouble(val, Double defaultVal=0.0) {
	return "${val}"?.isDouble() ? "${val}".toDouble() : defaultVal
}

void sendZigbeeCommands(ArrayList<String> cmd) {
    logDebug "<b>sendZigbeeCommands</b> (cmd=$cmd)"
    hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
    cmd.each {
            allActions.add(new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE))
    }
    sendHubCommand(allActions)
}

def configure() {
    logDebug "Configuring Reporting..."
    ArrayList<String> cmds = []
        cmds += "delay 200"
        cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0402 {${device.zigbeeId}} {}"
        cmds += "delay 200"
        cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0405 {${device.zigbeeId}} {}"
        cmds += "delay 200"
        cmds += "zdo bind 0x${device.deviceNetworkId} 0x03 0x01 0x0400 {${device.zigbeeId}} {}"
    
    sendZigbeeCommands(cmds)    
}

def installed(){
    configure()
    updatePreferences()
}

def updated(){
	initialize()
    configure()
    updatePreferences()
}

def initialize(){
	if (enableDebug){
		logInfo "Verbose logging has been enabled for the next 30 minutes."
		runIn(1800, logsOff)
	}
    state.DriverVersion=driverVer()
    if (!enableDistance){
        device.deleteCurrentState('distance')
    }
    if (healthCheckEnabled){
        
        switch(healthCheckInterval) {
		case "5 min" :
		runEvery5Minutes(healthCheck)
		logDebug "Health Check every 5 minutes schedule"
		logInfo "$device.label Health Check every 5 minutes schedule"
		break
        	case "10 min" :
		runEvery10Minutes(healthCheck)
		logDebug "Health Check every 10 minutes schedule"
		logInfo "$device.label Health Check every 10 minutes schedule"
		break
		case "15 min" :
		runEvery15Minutes(healthCheck)
		logDebug "Health Check every 15 minutes schedule"
		logInfo "$device.label Health Check every 15 minutes schedule"
		break
		case "30 min" :
		runEvery30Minutes(healthCheck)
		logDebug "Health Check every 30 minutes schedule"
		logInfo "$device.label Health Check every 30 minutes schedule"
		break
         	case "60 min" :
		runEvery1Hour(healthCheck)
		logDebug "Health Check every 60 minutes schedule"
		logInfo "$device.label Health Check every 60 minutes schedule"
		break
        }
    }
    if (!healthCheckEnabled){
        device.deleteCurrentState('status')
    }   
}

def refresh() {
    logInfo "$device.label - Refreshing Values"
    if (healthCheckEnabled){
        sendEvent(name: "status", value:  "checking")
        healthCheck()
    }
    ArrayList<String> cmds = []
    IAS_ATTRIBUTES.each { key, value ->
        cmds += zigbee.readAttribute(0x0500, key, [:], delay=200)
    cmds += zigbee.command(0xEF00, 0x03)
    }

    sendZigbeeCommands( cmds ) 
}    

private logError(msgOut){
    log.error msgOut
}

private logWarn(msgOut){
    log.warn msgOut
}

private logDebug(msgOut){
	if (settings.enableDebug){
		log.debug msgOut
	}
}

private logInfo(msgOut){
    if (settings.enableInfo){
        log.info msgOut
    }
}

def logsOff(){
    logWarn "debug logging disabled..."
	device.updateSetting("enableDebug", [value:"false",type:"bool"])
}
