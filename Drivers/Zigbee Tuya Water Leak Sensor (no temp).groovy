/*
 *  Zigbee Tuya Water Leak Sensor (no temp)
 *  for Tuya Water Leak sensors
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
 *  V1.0.0  9-24-2021       "no temp version"
 *  V1.1.0  11-15-2021      Improved Battery reporting and change date format
 */

def driverVer() { return "1.1" }

import hubitat.zigbee.clusters.iaszone.ZoneStatus

metadata
{
	definition(name: "Zigbee Tuya Water Leak Sensor(no temp)", namespace: "Gassgs", author: "GaryG")
	{
		capability "Water Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Refresh"
		capability "Sensor"
        
        command "batteryChanged"
        
        attribute "batteryVoltage","string"

		fingerprint inClusters: "0000,0001,0003,0500,EF01", outClusters: "0003,0019", manufacturer: "_TYZB01_sqmd19i1", model: "TS0207", deviceJoinName: "Zigbee Water Sensor"
	}

	preferences{
		section{
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
        }
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

	return result
}

private Map parseIasMessage(String description) {
	ZoneStatus zs = zigbee.parseZoneStatus(description)

	translateZoneStatus(zs)
}

private Map translateZoneStatus(ZoneStatus zs) {
	return (zs.isAlarm1Set() || zs.isAlarm2Set()) ? getResult('wet') : getResult('dry')
}

def getBatteryResult(rawValue) {
	def batteryVolts = $rawValue
	def minVolts = 1.9
	def maxVolts = 2.9
	def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
	def batteryValue = Math.min(100, pct)
    sendEvent("name": "battery", "value": batteryValue, "unit": "%", "displayed": true, isStateChange: true)
    logInfo "$device.label battery $batteryValue%"

	return
}

def batteryEvent(rawValue) {
	def batteryVolts = (rawValue / 10).setScale(2, BigDecimal.ROUND_HALF_UP)
	def minVolts = 19
	def maxVolts = 29
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

private Map getResult(value) {
    if (value == "wet"){
        sendEvent(name:"water",value:"wet")
        logInfo "$device.label Sensor Wet"
    }else{
        sendEvent(name:"water",value:"dry")
        logInfo "$device.label Sensor Dry"
    }
}

def refresh() {
	logInfo "Refreshing Values"
	def refreshCmds = []

	refreshCmds +=
		zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020) +
		zigbee.readAttribute(0x0500, 0x0002) +	// IAS Zone Status
		zigbee.enrollResponse()

	return refreshCmds
}

def configure() {
	logDebug "Configuring Reporting..."
	def configCmds = []

	configCmds +=
		zigbee.batteryConfig()

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
        schedule('0 0 6 * * ?',addDay)
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
