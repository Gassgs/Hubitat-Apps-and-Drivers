/*
 *   Zigbee Motion Sensor
 *   for Iris V2 and Samsung motion sensors
 *
 *  Copyright 2018 SmartThings / Modified by Gassgs
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
 *  V1.0.0  2-15-2021       Moddified for hubitat
 *  V1.1.0  8-03-2021       "fixed" Battery reporting
 *  V1.2.0  8-22-2021       Added Battery change date and count
 *  V1.3.0  11-15-2021      improved format for battery changed data
 */

def driverVer() { return "1.3" }

import hubitat.zigbee.clusters.iaszone.ZoneStatus

metadata
{
	definition(name: "Zigbee Motion Sensor", namespace: "Gassgs", author: "GaryG")
	{
		capability "Motion Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Temperature Measurement"
		capability "Refresh"
		capability "Sensor"
        
        command "batteryChanged"
        
        attribute "batteryVoltage","string"
        
		fingerprint inClusters: "0000,0001,0003,0020,0402,0500,0B05", outClusters: "0003,0019", manufacturer: "Samjin", model: "2018 Motion", deviceJoinName: "Samsung Motion Sensor"
        fingerprint inClusters: "0000,0001,0003,0020,0402,0500,0B05", outClusters: "0019", manufacturer: "CentraLite", model: "3326-L", deviceJoinName: "Iris 3326-L Sensor"
	}

	preferences{
		section{
			input "tempOffset", "number", title: "Temperature Offset", range: "*..*", displayDuringSetup: false, type: "paragraph", element: "paragraph"
            input "enableInfo", "bool", title: "Enable info logging?", defaultValue: true, required: false, multiple: false
			input "enableDebug", "bool", title: "Enable debug logging?", defaultValue: false, required: false, multiple: false
		}
	}
}


private List<Map> collectAttributes(Map descMap)
{
	List<Map> descMaps = new ArrayList<Map>()

	descMaps.add(descMap)

	if (descMap.additionalAttrs)
	{
		descMaps.addAll(descMap.additionalAttrs)
	}

	return  descMaps
}

def parse(String description)
{
	logDebug "Msg: Description is $description"
	Map map = zigbee.getEvent(description)

	if (!map)
	{
		if (description?.startsWith('zone status'))
		{
			map = parseIasMessage(description)
		}
		else
		{
			Map descMap = zigbee.parseDescriptionAsMap(description)

			if (descMap.cluster == "0001" && descMap.attrId == "0020") {
				batteryEvent(Integer.parseInt(descMap.value,16))
			}
			else if (descMap?.clusterId == "0500" && descMap.attrInt == 0x0002)
			{
				def zs = new ZoneStatus(zigbee.convertToInt(descMap.value, 16))
				map = translateZoneStatus(zs)
			}
            
			else if (descMap?.clusterId == "0001" && descMap.command == "07" && descMap.data[0] == "00")
			{
				if (descMap.data[0] == "00")
				{
					logDebug "== BATTERY REPORTING CONFIG RESPONSE: success =="
				}
				else
				{
					logError "!!! BATTERY REPORTING CONFIG FAILED: Error code: ${descMap.data[0]} !!!"
				}
			}

			else if (descMap?.clusterId == "0402" && descMap.command == "07" && descMap.data[0] == "00")
			{
				if (descMap.data[0] == "00")
				{
					logDebug "== TEMPERATURE REPORTING CONFIG RESPONSE: success =="
				}
				else
				{
					logError "!!! TEMPERATURE REPORTING CONFIG FAILED: Error code: ${descMap.data[0]} !!!"
				}
			}
			else if (descMap?.clusterId == "0500" && descMap.attrInt == 0x0500 && descMap?.value)
			{
				map = translateZoneStatus(new ZoneStatus(zigbee.convertToInt(descMap?.value)))
			}
		}
    } else if (map.name == "temperature") {
		if (tempOffset) {
			map.value = new BigDecimal((map.value as float) + (tempOffset as float)).setScale(1, BigDecimal.ROUND_HALF_UP)
		}
		map.descriptionText = temperatureScale == 'C' ? "${device.displayName} was ${map.value}°C" : "${device.displayName} was ${map.value}°F"
		map.translatable = true
        logInfo "$device.label Temperature $map.value °F"
	}
	
	else if (map.name == "batteryVoltage")
	{
		map.unit = "V"
		map.descriptionText = "${device.displayName} battery voltage is ${map.value} volts"
        logInfo "$device.label battery voltage $map.value"
        getBatteryResult(map.value)
	}
    
    logDebug "Parse returned $map"

	def result = map ? createEvent(map) : [:]

	if (description?.startsWith('enroll request')){
		List cmds = zigbee.enrollResponse()
		logDebug "enroll response: ${cmds}"
		result = cmds?.collect { new hubitat.device.HubAction(it) }
	}
	return result
}

private Map parseIasMessage(String description) {
	ZoneStatus zs = zigbee.parseZoneStatus(description)

	translateZoneStatus(zs)
}

private Map translateZoneStatus(ZoneStatus zs) {
	return (zs.isAlarm1Set() || zs.isAlarm2Set()) ? getMotionResult('active') : getMotionResult('inactive')
}

def getBatteryResult(rawValue) {
	def batteryVolts = $rawValue
	def minVolts = 2.0
	def maxVolts = 3.0
	def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
	def batteryValue = Math.min(100, pct)
    sendEvent("name": "battery", "value": batteryValue, "unit": "%", "displayed": true, isStateChange: true)
    logInfo "$device.label battery $batteryValue%"

	return
}

def batteryEvent(rawValue) {
	def batteryVolts = (rawValue / 10).setScale(2, BigDecimal.ROUND_HALF_UP)
	def minVolts = 20
	def maxVolts = 30
	def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
	def batteryValue = Math.min(100, pct)
	if (batteryValue > 0){
		sendEvent("name": "battery", "value": batteryValue, "unit": "%", "displayed": true, isStateChange: true)
		sendEvent("name": "batteryVoltage", "value": batteryVolts, "unit": "volts", "displayed": true, isStateChange: true)
		if (infoLogging) log.info "$device.displayName battery changed to $batteryValue%"
		if (infoLogging) log.info "$device.displayName voltage changed to $batteryVolts volts"
	}

	return
}

private Map getMotionResult(value) {
    if (value == "active"){
        sendEvent(name:"motion",value:"active")
        logInfo "$device.label Motion Active"
    }else{
        sendEvent(name:"motion",value:"inactive")
        logInfo "$device.label Motion Inactive"
    }
}

def refresh() {
	logInfo "Refreshing Values"
	def refreshCmds = []

	refreshCmds +=
		zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020) +
		zigbee.readAttribute(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, 0x0000) +
		zigbee.readAttribute(0x0500, 0x0002) +	// IAS Zone Status
		zigbee.enrollResponse()

	return refreshCmds
}

def configure() {
	logDebug "Configuring Reporting..."
	def configCmds = []

	configCmds +=
		zigbee.batteryConfig() +
		zigbee.temperatureConfig(30, 900)

	return configCmds
}

def batteryChanged(){
    now = new Date()
    dateFormat = new java.text.SimpleDateFormat("EE MMM d YYYY")
    timeFormat = new java.text.SimpleDateFormat("h:mm a")

    newDate = dateFormat.format(now)
    newTime = timeFormat.format(now)
    
    timeStamp = newDate + " " + newTime as String
    
    state.batteryChanged = "$timeStamp"
    state.batteryChangedDays = 0
    initialize()
}

def addDay(){
    if (state.batteryChangedDays != null){
    state.batteryChangedDays = state.batteryChangedDays + 1
    }
}

def installed(){
    batteryChanged()
    configure()
}

def updated(){
    state.DriverVersion = driverVer()
	initialize()
    configure()
}

def initialize(){
    if (state.batteryChangedDays != null){
        schedule('0 0 5 * * ?',addDay)
    }
	if (enableTrace || enableDebug){
		logInfo "Verbose logging has been enabled for the next 30 minutes."
		runIn(1800, logsOff)
	}
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
