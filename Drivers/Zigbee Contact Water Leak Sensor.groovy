/*
 * Zigbee Contact Water Leak Sensor
 * for Ecolink contact sensor
 *
 *  Copyright 2018 SmartThings
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
 */
import hubitat.zigbee.clusters.iaszone.ZoneStatus

metadata
{
	definition(name: "Zigbee Contact Water Sensor", namespace: "Gassgs", author: "GaryG")
	{
		capability "Water Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Temperature Measurement"
		capability "Refresh"
		capability "Sensor"
        
        attribute "batteryVoltage","string"

		fingerprint inClusters: "0000,0001,0003,0020,0402,0500,0B05", outClusters: "0019", manufacturer: "Ecolink", model: "4655BC0-R", deviceJoinName: "Ecolink Contact Sensor"
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

			if (descMap?.clusterId == "0001" && descMap.command == "07" && descMap.value)
			{
				logDebug "BATT METRICS - attr: ${descMap?.attrInt}, value: ${descMap?.value}, decValue: ${Integer.parseInt(descMap.value, 16)}, currPercent: ${device.currentState("battery")?.value}, device: ${device.getDataValue("manufacturer")} ${device.getDataValue("model")}"
				List<Map> descMaps = collectAttributes(descMap)
				def battMap = descMaps.find { it.attrInt == 0x0020 }

				if (battMap)
				{
					map = getBatteryResult(Integer.parseInt(battMap.value, 16))
				}
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
		map.descriptionText = temperatureScale == 'C' ? '{{ device.displayName }} was {{ value }}°C' : '{{ device.displayName }} was {{ value }}°F'
		map.translatable = true
	}
	
	else if (map.name == "batteryVoltage")
	{
		map.unit = "V"
		map.descriptionText = "${device.displayName} battery voltage is ${map.value} volts"
	}

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

private Map getBatteryResult(rawValue) {

	def linkText = getLinkText(device)

	def result = [:]

	def volts = rawValue / 10

	if (!(rawValue == 0 || rawValue == 255)) {
		result.name = 'battery'
		result.translatable = true
		result.unit = "%"
		def minVolts =  2.4
		def maxVolts =  2.7
		// Get the current battery percentage as a multiplier 0 - 1
		def curValVolts = Integer.parseInt(device.currentState("battery")?.value ?: "100") / 100.0
		// Find the corresponding voltage from our range
		curValVolts = curValVolts * (maxVolts - minVolts) + minVolts
		// Round to the nearest 10th of a volt
		curValVolts = Math.round(10 * curValVolts) / 10.0
		if (state?.lastVolts == null || state?.lastVolts == volts || device.currentState("battery")?.value == null || Math.abs(curValVolts - volts) > 0.1) {
			def pct = (volts - minVolts) / (maxVolts - minVolts)
			def roundedPct = Math.round(pct * 100)
			if (roundedPct <= 0)
				roundedPct = 1
			result.value = Math.min(100, roundedPct)
		} else {
			result.value = device.currentState("battery").value
		}
		result.descriptionText = "${device.displayName} battery was ${result.value}%"
		state.lastVolts = volts
	}

	return result
}

private Map getMotionResult(value) {
    if (value == "active"){
        sendEvent(name:"water",value:"dry")
        logInfo "$device.label Water Dry"
    }else{
        sendEvent(name:"water",value:"wet")
        logInfo "$device.label Water Wet"
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
		zigbee.temperatureConfig(30, 300) +
		zigbee.configureReporting(0x0405, 0x0000, DataType.UINT16, 30, 3600, 100) 

	return configCmds
}

def installed(){
	state.lastVolts = 0
	
	initialize()
    configure()
}

def updated(){
	initialize()
    configure()
}

def initialize(){
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
