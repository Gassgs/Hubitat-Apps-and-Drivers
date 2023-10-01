/*
 *  Linptech / Moes 24Ghz Presence Sensor ES1 driver 
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
 *  V1.0.0  9-26-2023       Modifying Tuya 4in1 driver by "Krassimir Kossev" to support only Linptech/Moes 24Ghz Presence Sensor ES1
 *  V1.1.0  9-27-2023       Fixed lux reporting and parsing, clean up / todo - distance reporting option
 *  V1.2.0  9-30-2023       Added distance reporting
 *  V1.3.0  10-1-2023       Added fade time option and states for preferences
 */

import hubitat.zigbee.clusters.iaszone.ZoneStatus

metadata
{
	definition(name: "Linptech 24Ghz Presence Sensor ES1", namespace: "Gassgs", author: "Krassimir Kossev", importUrl: "https://raw.githubusercontent.com/Gassgs/Hubitat-Apps-and-Drivers/master/Drivers/Linptech%2024Ghz%20Presence%20Sensor%20ES1.groovy", singleThreaded: true )
	{
	capability "Motion Sensor"
        capability "IlluminanceMeasurement"
	capability "Configuration"
	capability "Refresh"
	capability "Sensor"
        
        attribute "distance", "number" 
          
	fingerprint inClusters: "0000,0003,0004,0005,E002,4000,EF00,0500", outClusters: "0019,000A", manufacturer: "_TZ3218_awarhusb", model: "TS0225", deviceJoinName: "LINPTECH 24Ghz Human Presence Detector"
	}

	preferences{
		section{
            def distanceLimit = [:]
            distanceLimit << [1.5 : "1.5 meters"]
            distanceLimit << [2.25 : "2.25 meters"]
            distanceLimit << [3.0 : "3.0 meters"]
            distanceLimit << [3.75 : "3.75 meters"]
            distanceLimit << [4.5 : "4.5 meters"]
            distanceLimit << [5.25 : "5.25 meters"]
            distanceLimit << [6.0 : "6.0 meters"]
            input "motionDetectionDistance", "enum", title: "<b>Motion Detection Distance</b>", options: distanceLimit, defaultValue: 6.0
            def detectLevel = [:]
            detectLevel << [5 : "high"]
            detectLevel << [4 : "medium high"]
            detectLevel << [3 : "medium"]
            detectLevel << [2 : "medium low"]
            detectLevel << [1 : "low"]
            input "motionDetectionSensitivity", "enum", title: "<b>Motion Detection Sensitivity</b>", options: detectLevel, defaultValue: 5
            def staticLevel = [:]
            staticLevel << [5 : "high"]
            staticLevel << [4 : "medium high"]
            staticLevel << [3 : "medium"]
            staticLevel << [2 : "medium low"]
            staticLevel << [1 : "low"]
            input "staticDetectionSensitivity", "enum", title: "<b>Static Detection Sensitivity</b>", options: staticLevel, defaultValue: 5
            input "luxThreshold", "number", title: "<b>Lux threshold</b>", description: "<i>Range (0..999)</i>", range: "0..999", defaultValue: 5
            input "fadeTime", "decimal", title: "<b>Fade time, in seconds</b>", description: "<i>Range (10..999)</i>", range: "10..999", defaultValue: 10
            input "enableDistance", "bool", title: "<b>Enable Distance Reporting?</b>", defaultValue: false, required: false, multiple: false
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
    if (value == 1){state.motion = "low"}
    else if (value == 2){state.motion = "medium-low"}
    else if (value == 3){state.motion = "medium"}
    else if (value == 4){state.motion = "medium-high"}
    else if (value == 5){state.motion = "high"}
    else{state.motion = "unknown"}
}

def staticSensitivity( descMap ) {
    def value = zigbee.convertHexToInt(descMap.value) 
    logDebug "Cluster ${descMap.cluster} Attribute ${descMap.attrId} value is ${value} (0x${descMap.value})"
    if (value == 1){state.static = "low"}
    else if (value == 2){state.static = "medium-low"}
    else if (value == 3){state.static = "medium"}
    else if (value == 4){state.static = "medium-high"}
    else if (value == 5){state.static = "high"}
    else{state.static = "unknown"}
}

def distanceLimit( descMap ) {
    def value = zigbee.convertHexToInt(descMap.value) 
    logDebug "Cluster ${descMap.cluster} Attribute ${descMap.attrId} value is ${value} (0x${descMap.value})"
    state.distance = (value/100) + " m" 
}

def processDistance( descMap ) {
    def value = zigbee.convertHexToInt(descMap.value) 
    logDebug "Cluster ${descMap.cluster} Attribute ${descMap.attrId} value is ${value} (0x${descMap.value})"
    logInfo "$device.label distance is ${value/100} m"
    sendEvent(name : "distance", value : value/100, unit : "m")                
}

def updatePreferences(){
    ArrayList<String> cmds = []
    
    cmds += tuyaBlackMagic()
    cmds += setRadarFadingTime(settings?.fadeTime ?: 10)
    cmds += setMotionDetectionDistance( settings?.motionDetectionDistance )
    cmds += setMotionDetectionSensitivity( settings?.motionDetectionSensitivity )
    cmds += setStaticDetectionSensitivity( settings?.staticDetectionSensitivity ) 
    
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

def setMotionDetectionDistance( data ) {
    def val = data as float
    def value = Math.round(val * 100)
    logDebug "$device.label set MotionDetectionDistance to ${val}m (raw ${value})"
    return zigbee.writeAttribute(0xE002, 0xE00B, 0x20, value as int, [:], delay=200)
}

def setMotionDetectionSensitivity( val ) {
    def value = val as int
    logDebug "$device.label set MotionDetectionSensitivity to ${value}"
    return zigbee.writeAttribute(0xE002, 0xE004, 0x20, value as int, [:], delay=200)
}

def setStaticDetectionSensitivity( val ) {
    def value = val as int
    logDebug "$device.label set StaticDetectionSensitivity to ${value}"
    return zigbee.writeAttribute(0xE002, 0xE005, 0x20, value as int, [:], delay=200)
}

def setRadarFadingTime( val ){
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
    if (!enableDistance){
        device.deleteCurrentState('distance')
    }
}

def refresh() {
    logInfo "Refreshing Values"
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
