/**  AXIS GEAR
 * 
 *  Ported from SmartThingsPublic - axis-gear-st.groovy by Gassgs, GaryG
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 */

import groovy.json.JsonOutput

metadata {
	definition (name: "AXIS GEAR", namespace: "AXIS", author: "AXIS Labs/Gassgs") {
		capability "Window Shade"
		capability "Switch Level"
		capability "Battery"
		capability "Refresh"
		capability "Actuator"
		capability "Configuration"
		capability "Switch"

		command "ShadesUp"
		command "ShadesDown"
        
        fingerprint profileID: "0104", manufacturer: "AXIS", model: "Gear", deviceJoinName: "AXIS Gear"
		fingerprint profileId: "0104", deviceId: "0202", inClusters: "0000, 0003, 0006, 0008, 0102, 0020, 0001", outClusters: "0019", manufacturer: "AXIS", model: "Gear", deviceJoinName: "AXIS Gear"
	}

    preferences() {
        section(""){
            input "refreshInt", "number", title: "Max time in seconds it takes to Open or Close", required: true, defaultValue: 15
            input "logEnable", "bool", title: "Enable Debug logging", required: true, defaultValue: true
            input "infoEnable", "bool", title: "Enable text info logging", required: true, defaultValue: true
        }
    }
}

//Declare Clusters
private getCLUSTER_BASIC() {0x0000}
private getBASIC_ATTR_SWBUILDID() {0x4000}

private getCLUSTER_POWER() {0x0001}
private getPOWER_ATTR_BATTERY() {0x0021}

private getCLUSTER_ONOFF() {0x0006}
private getONOFF_ATTR_ONOFFSTATE() {0x0000}

private getCLUSTER_LEVEL() {0x0008}
private getLEVEL_ATTR_LEVEL() {0x0000}
private getLEVEL_CMD_STOP() {0x03}

private getCLUSTER_WINDOWCOVERING() {0x0102}
private getWINDOWCOVERING_ATTR_LIFTPERCENTAGE() {0x0008}
private getWINDOWCOVERING_CMD_OPEN() {0x00}
private getWINDOWCOVERING_CMD_CLOSE() {0x01}
private getWINDOWCOVERING_CMD_STOP() {0x02}
private getWINDOWCOVERING_CMD_GOTOLIFTPERCENTAGE() {0x05}

private getMIN_WINDOW_COVERING_VERSION() {1093}


def updated(){
    if (infoEnable) log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def getLastShadeLevel() {
	device.currentState("position") ? device.currentValue("position") : (device.currentState("level") ? device.currentValue("level") : 0) // Try shadeLevel, if not use level, if not 0
}

//Custom command to increment blind position by 25 %
def ShadesUp() {
	def shadeValue = lastShadeLevel as Integer

	if (shadeValue < 100) {
		shadeValue = Math.min(25 * (Math.round(shadeValue / 25) + 1), 100) as Integer
	}
	else {
		shadeValue = 100
	}
	setLevel(shadeValue)
}

//Custom command to decrement blind position by 25 %
def ShadesDown() {
	def shadeValue = lastShadeLevel as Integer

	if (shadeValue > 0) {
		shadeValue = Math.max(25 * (Math.round(shadeValue / 25) - 1), 0) as Integer
	}
	else {
		shadeValue = 0
	}
	setLevel(shadeValue)
}

def stop() {
	if (infoEnable) log.info "$device.label stop()"
    return zigbee.command(CLUSTER_WINDOWCOVERING, WINDOWCOVERING_CMD_STOP)
    return zigbee.command(CLUSTER_LEVEL, LEVEL_CMD_STOP)
}

def stopPositionChange() {
	stop()
}

def on() {
	sendEvent(name: "switch", value: "on")
	open()
}

def off() {
	sendEvent(name: "switch", value: "off")
	close()
}

//Command to set the blind position (%) and log the event
def setPosition(value) {
	setLevel(value)
}

def setLevel(value,duration = null) {
	if (infoEnable) log.info "$device.label set Level / Position ($value)"
	Integer currentLevel = state.level

	def i = value as Integer
	sendEvent(name:"level", value: value, unit:"%", displayed: false)
	sendEvent(name:"position", value: value, unit:"%", displayed:true)
	if (i > currentLevel) {
		sendEvent(name: "windowShade", value: "opening")
	}
	else if (i < currentLevel) {
		sendEvent(name: "windowShade", value: "closing")
	}
    runIn(refreshInt,refresh)
    return zigbee.setLevel(i)
}

def open() {
	if (infoEnable) log.info "$device.label open()"
	setLevel("100")
}

def close() {
	if (infoEnable) log.info "$device.label close()"
	setLevel("0")
}

def setWindowShade(value) {
	if ((value>0)&&(value<99)) {
		sendEvent(name: "windowShade", value: "partially open", displayed:true)
        sendEvent(name: "switch", value: "on", displayed:true)
	}
	else if (value >= 99) {
		sendEvent(name: "windowShade", value: "open", displayed:true)
        sendEvent(name: "switch", value: "on", displayed:true)
	}
	else {
		sendEvent(name: "windowShade", value: "closed", displayed:true)
        sendEvent(name: "switch", value: "off", displayed:true)
	}
}

def startPositionChange(direction) {
    if (direction == "open") {
        open()
    } else {
       close()
    }
}

def refresh() {
	if (logEnable) log.debug "parse() refresh"
	def cmds_refresh = null
    cmds_refresh = zigbee.readAttribute(CLUSTER_LEVEL, LEVEL_ATTR_LEVEL)

	cmds_refresh = cmds_refresh +
					zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY)

	if (infoEnable) log.info "$device.label refreshing"

	return cmds_refresh
}

//configure reporting
def configure() {
	if (logEnable) log.debug "Configuring Reporting and Bindings."
	sendEvent(name: "checkInterval", value: (2 * 60 * 60 + 10 * 60), displayed: true, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

	def attrs_refresh = zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_SWBUILDID) +
						zigbee.readAttribute(CLUSTER_WINDOWCOVERING, WINDOWCOVERING_ATTR_LIFTPERCENTAGE) +
						zigbee.readAttribute(CLUSTER_ONOFF, ONOFF_ATTR_ONOFFSTATE) +
						zigbee.readAttribute(CLUSTER_LEVEL, LEVEL_ATTR_LEVEL) +
						zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY)

	def cmds = zigbee.configureReporting(CLUSTER_WINDOWCOVERING, WINDOWCOVERING_ATTR_LIFTPERCENTAGE, 0x20, 1, 3600, 0x01) +
               zigbee.configureReporting(CLUSTER_ONOFF, ONOFF_ATTR_ONOFFSTATE, 0x10, 1, 3600, null) +
               zigbee.configureReporting(CLUSTER_LEVEL, LEVEL_ATTR_LEVEL, 0x20, 1, 3600, 0x01) +
               zigbee.configureReporting(CLUSTER_POWER, POWER_ATTR_BATTERY, 0x20, 1, 3600, 0x01)
                       

	if (infoEnable) log.info "$device.label -configure-"
	return attrs_refresh + cmds
}

def parse(String description) {
	if (logEnable) log.debug "parse() --- description: $description"

	Map map = [:]

	if (device.currentValue("level") == null && device.currentValue("level") != null) {
		sendEvent(name: "position", value: device.currentValue("level"), unit: "%")
	}

	def event = zigbee.getEvent(description)
	if (event && description?.startsWith('on/off')) {
		if (logEnable) log.debug "$device.label sendEvent(event)"
		sendEvent(event)
	}

	else if ((description?.startsWith('read attr -')) || (description?.startsWith('attr report -'))) {
		map = parseReportAttributeMessage(description)
		def result = map ? createEvent(map) : null

		if (map.name == "level") {
			result = [result, createEvent([name: "position", value: map.value, unit: map.unit])]
		}

		if (logEnable && result != null) log.debug "$device.label parse() --- returned: $result"
		return result
	}
}

private Map parseReportAttributeMessage(String description) {
	Map descMap = zigbee.parseDescriptionAsMap(description)
	Map resultMap = [:]
	if (descMap.clusterInt == CLUSTER_POWER && descMap.attrInt == POWER_ATTR_BATTERY) {
		resultMap.name = "battery"
		def batteryValue = Math.round((Integer.parseInt(descMap.value, 16))/2)
		if (logEnable) log.debug "parseDescriptionAsMap() --- Battery: $batteryValue"
		if ((batteryValue >= 0)&&(batteryValue <= 100)) {
			resultMap.value = batteryValue
		}
		else {
			resultMap.value = 0
		}
	}
	else if (descMap.clusterInt == CLUSTER_WINDOWCOVERING && descMap.attrInt == WINDOWCOVERING_ATTR_LIFTPERCENTAGE && state.currentVersion >= MIN_WINDOW_COVERING_VERSION) {
		resultMap.name = "level"
		def levelValue = 100 - Math.round(Integer.parseInt(descMap.value, 16))
		resultMap.value = levelValue
		state.level = levelValue
		resultMap.unit = "%"
		resultMap.displayed = false
		setWindowShade(levelValue)
	}
	else if (descMap.clusterInt == CLUSTER_LEVEL && descMap.attrInt == LEVEL_ATTR_LEVEL) {
		def currentLevel = state.level

		resultMap.name = "level"
		def levelValue = Math.round(Integer.parseInt(descMap.value, 16))
		def levelValuePercent = Math.round((levelValue/255)*100)
		resultMap.value = levelValuePercent
		state.level = levelValuePercent
		resultMap.unit = "%"
		resultMap.displayed = false
        setWindowShade(levelValuePercent)
	}
	else if (descMap?.clusterInt == CLUSTER_BASIC && zigbee.convertHexToInt(descMap?.attrId) == BASIC_ATTR_SWBUILDID && descMap.value) {
		def versionString = descMap.value
        if (logEnable) log.debug  "$device.label SW Build # $descMap.value"

		def current = versionString.substring(versionString.length() - 4)
        state.version = current
        state.build = "$descMap.value"
        if (logEnable) log.debug "$device.label Version # $current"
	}
	return resultMap
}
