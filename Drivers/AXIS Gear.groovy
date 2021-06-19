/**
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
	definition (name: "AXIS Gear", namespace: "AXIS Gear", author: "AXIS Labs", importURL: "https://raw.githubusercontent.com/cofin/hubitat-axis-gear-driver/main/AxisGear.groovy----") {
		capability "Actuator"
		capability "Battery"
		capability "Configuration"
		capability "Refresh"
		capability "WindowShade"
        capability "SwitchLevel"
        capability "Switch"
        capability "Refresh"

        command "ShadesUp"
        command "ShadesDown"

		fingerprint profileID: "0104", manufacturer: "AXIS", model: "Gear", deviceJoinName: "AXIS Gear"
		fingerprint profileId: "0104", deviceId: "0202", inClusters: "0000, 0003, 0006, 0008, 0102, 0020, 0001", outClusters: "0019", manufacturer: "AXIS", model: "Gear", deviceJoinName: "AXIS Gear"
	}

    preferences() {
        section(""){
            input "refreshSch", "bool", title: "Refresh Schedule", description: "Enable to refresh every 15mins to keep shades in sync", defaultValue:false, required: true
            input "logEnable", "bool", title: "Enable Debug logging", required: true, defaultValue: true
            input "logInfoEnable", "bool", title: "Enable text info logging", required: true, defaultValue: true
        }
    }
}
private getCLUSTER_BASIC() {0x0000}
private getCLUSTER_WINDOW_COVERING() { 0x0102 }
private getCLUSTER_POWER() {0x0001}
private getCLUSTER_ON_OFF() {0x0006}
private getCLUSTER_LEVEL() {0x0008}

private getCOMMAND_OPEN() { 0x00 }
private getCOMMAND_CLOSE() { 0x01 }
private getCOMMAND_PAUSE() { 0x02 }
private getCOMMAND_STOP() { 0x03 }
private getCOMMAND_GOTO_LIFT_PERCENTAGE() { 0x05 }
private getATTRIBUTE_POSITION_LIFT() { 0x0008 }
private getATTRIBUTE_CURRENT_LEVEL() { 0x0000 }
private getATTRIBUTE_ON_OFF_STATE() { 0x0000 }
private getCOMMAND_MOVE_LEVEL_ONOFF() { 0x04 }
private getBATTERY_PERCENTAGE_REMAINING() { 0x0021 }
private getATTRIBUTE_BUILD_ID() {0x4000}


private List<Map> collectAttributes(Map descMap) {
	List<Map> descMaps = new ArrayList<Map>()

	descMaps.add(descMap)

	if (descMap.additionalAttrs) {
		descMaps.addAll(descMap.additionalAttrs)
	}
	return descMaps
}

def updated(){
    if (infoLogEnable)  log.info "Updated"
    unschedule()
    if (refreshSch)
    runEvery15Minutes(refresh)
    if (infoLogEnable)  log.info "15 min Refresh Schedule Started"
}

def installed() {
	if(logEnable) log.debug "installed"
	sendEvent(name: "supportedWindowShadeCommands", value: JsonOutput.toJson(["open", "close", "stop"]))
}

// Parse incoming device messages to generate events
def parse(String description) {
	if(logEnable) log.debug "description:- ${description}"
	if (description?.startsWith("read attr -")) {
		Map descMap = zigbee.parseDescriptionAsMap(description)
		/* if (isBindingTableMessage(description)) {
			parseBindingTableMessage(description)
		} else */ if (supportsLiftPercentage() && descMap?.clusterInt == CLUSTER_WINDOW_COVERING && descMap.value) {
			if(logEnable) log.debug "attr: ${descMap?.attrInt}, value: ${descMap?.value}, descValue: ${Integer.parseInt(descMap.value, 16)}, ${device.getDataValue("model")}"
			List<Map> descMaps = collectAttributes(descMap)
			def liftmap = descMaps.find { it.attrInt == ATTRIBUTE_POSITION_LIFT }
			if (liftmap && liftmap.value) {
				def newLevel = zigbee.convertHexToInt(liftmap.value)
				if (shouldInvertLiftPercentage()) {
					// some devices report % level of being closed (instead of % level of being opened)
					// inverting that logic is needed here to avoid a code duplication
					newLevel = 100 - newLevel
				}
                if(logEnable) log.debug "sending to levelEventHandler() with supportsLiftPercentage()"
				levelEventHandler(newLevel)
			}
		} else if (/*!supportsLiftPercentage() && */descMap?.clusterInt == zigbee.LEVEL_CONTROL_CLUSTER && descMap.value) {
            if(logEnable) log.debug "sending to levelEventHandler()"
			def valueInt = Math.round((zigbee.convertHexToInt(descMap.value)) / 255 * 100)

			levelEventHandler(valueInt)
		} else if (reportsBatteryPercentage() && descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && zigbee.convertHexToInt(descMap?.attrId) == BATTERY_PERCENTAGE_REMAINING && descMap.value) {
			def batteryLevel = zigbee.convertHexToInt(descMap.value)
			batteryPercentageEventHandler(batteryLevel)
		}
        else if (isAxisGear() && descMap?.clusterInt == CLUSTER_BASIC && zigbee.convertHexToInt(descMap?.attrId) == ATTRIBUTE_BUILD_ID && descMap.value) {
			if(logEnable) log.debug "sending to softwareBuildEventHandler()"
            String softwareBuild = descMap.value
			softwareBuildEventHandler(softwareBuild)
		}
        else if (isAxisGear() && descMap?.clusterInt == CLUSTER_ON_OFF && zigbee.convertHexToInt(descMap?.attrId) == ATTRIBUTE_ON_OFF_STATE && descMap.value) {
            if(logEnable) log.debug "sending to onOffEventHandler()"

            def onOff = zigbee.convertHexToInt(descMap.value)
			onOffEventHandler(onOff)
		}
        else {
		    if(logEnable) log.debug "No Handler found for event: clusterInt == ${descMap?.clusterInt}, attrId == ${zigbee.convertHexToInt(descMap?.attrId)}, value = ${descMap.value}"
	    }
	}
}

def softwareBuildEventHandler(softwareBuild) {
	if(logEnable) log.debug "softwareBuildEventHandler - found software build ID ${softwareBuild.substring(softwareBuild.length() - 4)}"
    state.softwareBuild = softwareBuild
    state.currentVersion = softwareBuild.substring(softwareBuild.length() - 4)
}
def onOffEventHandler(onOff) {
	if(logEnable) log.debug "onOffEventHandlerf - on/off state $onOff"
    state.switch = onOff
}

//Custom command to increment blind position by 25 %
def ShadesUp(){
	def shadeValue = device.latestValue("level") as Integer ?: 0 
    
    if (shadeValue < 100){
      	shadeValue = Math.min(25 * (Math.round(shadeValue / 25) + 1), 100) as Integer
    }else { 
    	shadeValue = 100
	}
    //sendEvent(name:"level", value:shadeValue, displayed:true)
    setLevel(shadeValue)
}

//Custom command to decrement blind position by 25 %
def ShadesDown(){
	def shadeValue = device.latestValue("level") as Integer ?: 0 
    
    if (shadeValue > 0){
      	shadeValue = Math.max(25 * (Math.round(shadeValue / 25) - 1), 0) as Integer
    }else { 
    	shadeValue = 0
	}
    //sendEvent(name:"level", value:shadeValue, displayed:true)
    setLevel(shadeValue)
}

def levelEventHandler(currentLevel) {

	def lastLevel = state.level
	if(logEnable) log.debug "levelEventHandler - currentLevel: ${currentLevel} lastLevel: ${lastLevel}"
	if (lastLevel == "undefined" || currentLevel == lastLevel) { //Ignore invalid reports
		if(logEnable) log.debug "skipping - no level change required"
        if (!lastLevel) {
            if(logEnable) log.debug "defaulting null level value"
            state.level = currentLevel
        }
	} else {
		sendEvent(name: "level", value: currentLevel)
        sendEvent(name: "position", value: currentLevel)
        state.level = currentLevel
		if (currentLevel == 0 || currentLevel == 100) {
			sendEvent(name: "windowShade", value: currentLevel == 0 ? "closed" : "open")
            sendEvent(name: "switch", value: currentLevel == 0 ? "off" : "on")
            state.windowShade = currentLevel == 0 ? "closed" : "open"
		} else {
			if (lastLevel < currentLevel) {
				sendEvent([name:"windowShade", value: "opening"])
                state.windowShade = "opening"
			} else if (lastLevel > currentLevel) {
				sendEvent([name:"windowShade", value: "closing"])
                state.windowShade = "closing"
			}
            else if (lastLevel == currentLevel) {
				sendEvent([name:"windowShade", value: "partially open"])
                state.windowShade = "partially open"
			}
			runIn(10, "updateFinalState", [overwrite:true])
		}
	}
}

def updateFinalState() {
	//def level = device.currentValue("level")
    def level = state.level
	if(logEnable) log.debug "updateFinalState: ${level}"
	if (level > 0 && level < 100) {
		sendEvent(name: "windowShade", value: "partially open")
        sendEvent(name: "switch", value: "on")
        state.windowShade = "partially open"
	}
}

def batteryPercentageEventHandler(batteryLevel) {
	if (batteryLevel != null) {
		batteryLevel = Math.min(100, Math.max(0, batteryLevel))
		sendEvent([name: "battery", value: batteryLevel, unit: "%", descriptionText: "{{ device.displayName }} battery was {{ value }}%"])
        state.battery = batteryLevel
	}
}

def close() {
	if(logEnable) log.info "close()"
    if(logInfoEnable) log.info "close()"
	
    sendEvent(name: "windowShade", value: "closing")
    zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_CLOSE)
    runIn(10, "updateFinalState", [overwrite:true])
    
    setLevel(0)
}

def open() {
	if(logEnable) log.info "open()"
    if(logInfoEnable) log.info "open()"

    sendEvent(name: "windowShade", value: "opening")
    zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_OPEN)
    runIn(10, "updateFinalState", [overwrite:true])
    
    setLevel(100)    
}

def on(){
    open()
}

def off(){
    close()
}

def setLevel(data, rate = null) {
    if(logInfoEnable) log.info "setLevel ${data}%"
	def cmd
    def level = data as Integer
	if (supportsLiftPercentage()) {
		if (shouldInvertLiftPercentage()) {
			// some devices keeps % level of being closed (instead of % level of being opened)
			// inverting that logic is needed here
			levelParam = 100 - level
		}
		cmd = zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_GOTO_LIFT_PERCENTAGE, zigbee.convertToHexString(levelParam, 2))
	} else {
		cmd = zigbee.command(zigbee.LEVEL_CONTROL_CLUSTER, COMMAND_MOVE_LEVEL_ONOFF, zigbee.convertToHexString(Math.round(levelParam * 255 / 100), 2))
	}
    levelEventHandler(level)    
    /*runIn(30, "updateFinalState", [overwrite:true])*/
	cmd
}

def updateLiftState() {
    if (supportsLiftPercentage()) {
		cmds = zigbee.readAttribute(CLUSTER_WINDOW_COVERING, ATTRIBUTE_POSITION_LIFT)
	} else {
		cmds = zigbee.readAttribute(zigbee.LEVEL_CONTROL_CLUSTER, ATTRIBUTE_CURRENT_LEVEL)
	}
    updateFinalState()
    return cmds

}

def setPosition(data) {
    if(logEnable) log.info "setPosition ${data}%"
	setLevel(data)
}
// AXIS does not repond fast enough to use pause
def pause() {
    stop()
}
// AXIS does not repond fast enough to use stop 
def stop() {
	if(logEnable) log.debug "stop()"
    if(logInfoEnable) log.info "stop()"
	zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_PAUSE)
}

//added for new window shade commands in hubitat
def stopPositionChange(){
    stop()
}
def startPositionChange(direction) {
    if (direction == "open") {
        open()
    } else {
       close()
    }
}
/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	return refresh()
}

def refresh() {
	if(logEnable) log.info "refresh()"
	def cmds
	if (supportsLiftPercentage()) {
		cmds = zigbee.readAttribute(CLUSTER_WINDOW_COVERING, ATTRIBUTE_POSITION_LIFT)
        cmds += zigbee.readAttribute(zigbee.LEVEL_CONTROL_CLUSTER, ATTRIBUTE_CURRENT_LEVEL)
	} else {
		cmds = zigbee.readAttribute(zigbee.LEVEL_CONTROL_CLUSTER, ATTRIBUTE_CURRENT_LEVEL)
	}
    if (isAxisGear()) {
        cmds += zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, BATTERY_PERCENTAGE_REMAINING)
		cmds += zigbee.readAttribute(CLUSTER_BASIC, ATTRIBUTE_BUILD_ID)
    }
	return cmds
}

def configure() {
	// Device-Watch allows 2 check-in misses from device + ping (plus 2 min lag time)
	if(logEnable) log.info "configure()"
    state.currentVersion = 0

	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	if(logEnable) log.debug "Configuring Reporting and Bindings."

	def cmds
	if (supportsLiftPercentage()) {
		cmds = zigbee.configureReporting(CLUSTER_WINDOW_COVERING, ATTRIBUTE_POSITION_LIFT, 0x20, 1, 3600, 0x00)
        cmds += zigbee.levelConfig()
	} else {
		cmds = zigbee.levelConfig()
	}

	if (usesLocalGroupBinding()) {
		cmds += readDeviceBindingTable()
	}

	if (reportsBatteryPercentage()) {
		cmds += zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, BATTERY_PERCENTAGE_REMAINING,  0x20, 30, 21600, 0x01)
	}

    if (isAxisGear()) {
        cmds += zigbee.configureReporting(CLUSTER_LEVEL, ATTRIBUTE_CURRENT_LEVEL, 0x20, 1, 3600, 0x00)
    }

	return refresh() + cmds
}

def usesLocalGroupBinding() {
	isAxisGear()
}

private def parseBindingTableMessage(description) {
	Integer groupAddr = getGroupAddrFromBindingTable(description)
	if (groupAddr) {
		List cmds = addHubToGroup(groupAddr)
		cmds?.collect { new hubitat.device.HubAction(it) }
	}
}

private Integer getGroupAddrFromBindingTable(description) {
	if(logEnable) log.info "Parsing binding table - '$description'"
	def btr = zigbee.parseBindingTableResponse(description)
	def groupEntry = btr?.table_entries?.find { it.dstAddrMode == 1 }
	if(logEnable) log.info "Found ${groupEntry}"
	!groupEntry?.dstAddr ?: Integer.parseInt(groupEntry.dstAddr, 16)
}

private List addHubToGroup(Integer groupAddr) {
	["st cmd 0x0000 0x01 ${CLUSTER_GROUPS} 0x00 {${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr,4))} 00}", "delay 200"]
}

private List readDeviceBindingTable() {
	["zdo mgmt-bind 0x${device.deviceNetworkId} 0", "delay 200"]
}

def supportsLiftPercentage() {
	isAxisGear()
}

def shouldInvertLiftPercentage() {
	return isAxisGear()
}

def reportsBatteryPercentage() {
	return isAxisGear()
}

def isAxisGear() {
	device.getDataValue("model") == "Gear"
}
