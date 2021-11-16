/**
 *  Z-Wave Plus Contact Sensor + Motion
 *  
 *  Author: Smartthings
 *    Mod by Gassgs
 *
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
 *  V1.0.0  3-09-2021       Moddified to add motion options
 *  V1.1.0  7-09-2021       Removed unused states  
 *  V1.2.0  8-22-2021       Added Battery change date and count
 *  V1.3.0  11-15-2021      improved format for battery changed data
 */

def driverVer() { return "1.3" }

metadata {
	definition (
		name: "Z-Wave Plus Contact Sensor + Motion", namespace: "Gassgs", author: "SmartThings"
	) {
		capability "Sensor"
		capability "Contact Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Tamper Alert"
         	capability "Motion Sensor"
		capability "Refresh"
        
        	command "batteryChanged"

        fingerprint inClusters: "0x86,0x72"
	fingerprint mfr:"0109", prod:"2001", model:"0106", deviceJoinName: "Monoprice Door/Window Sensor"
        fingerprint mfr:"0109", prod:"2022", model:"2201", deviceJoinName: "Monoprice Recessed Door Sensor"
        fingerprint deviceId: "2201", inClusters:"0x5E,0x86,0x72,0x5A,0x85,0x59,0x73,0x80,0x71,0x84,0x7A,0x98" 
	}
	
	
	preferences {
		input "checkinInterval", "number",title: "Minimum Check-in Interval (Hours)",defaultValue: 4,range: "1..167",displayDuringSetup: true, required: false
		input "reportBatteryEvery", "number", title: "Battery Reporting Interval (Hours)",defaultValue: 6,range: "1..167",displayDuringSetup: true, required: false
		input "enableExternalSensor", "bool", title: "Enable External Sensor?",defaultValue: false,displayDuringSetup: true, required: false
		input "autoClearTamper", "bool", title: "Automatically Clear Tamper?",defaultValue: false,displayDuringSetup: true, required: false
        	input name: "motionEnabled", type: "bool" , title: "Motion Active When Closed? ", defaultValue: false
        	input name: "infoEnable", type: "bool", title: "Enable info text logging", defaultValue: true
		input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
	}
}

def updated() {	
	// This method always gets called twice when preferences are saved.
    if (state.batteryChangedDays != null){
        schedule('0 0 4 * * ?',addDay)
    }
    state.DriverVersion = driverVer()
	if (infoEnable) log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
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
    updated()
}

def addDay(){
    if (state.batteryChangedDays != null){
    state.batteryChangedDays = state.batteryChangedDays + 1
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def configure() {	
	if (logEnable) log.debug "configure()"
	def cmds = []
	
	if (!device.currentValue("contact")) {
		sendEvent(createEventMap("contact", "open", false))
	}
	
	if (!state.isConfigured) {
		  if (logEnable) log.debug "Waiting 1 second because this is the first time being configured"
		// Give inclusion time to finish.
		cmds << "delay 1000"			
	}
	
	initializeCheckin()
		
	cmds += delayBetween([
		wakeUpIntervalSetCmd(checkinIntervalSettingSeconds),
		externalSensorConfigSetCmd(settings?.enableExternalSensor ?: false),
		externalSensorConfigGetCmd(),
		batteryGetCmd()
	], 100)
		
	if (logEnable) log.debug "Sending configuration to device."
	return cmds
}

private initializeCheckin() {
	// Set the Health Check interval so that it can be skipped twice plus 5 minutes.
	def checkInterval = ((checkinIntervalSettingSeconds * 3) + (5 * 60))
	
	sendEvent(name: "checkInterval", value: checkInterval, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
}


private getCheckinIntervalSetting() {
	return (settings?.checkinInterval ?: 6)
}

private getCheckinIntervalSettingSeconds() {
	return (checkinIntervalSetting * 60 * 60)
}
				
def parse(String description) {
	def result = []
	def cmd = zwave.parse(description, commandClassVersions)
	if (cmd) {
		result += zwaveEvent(cmd)
	}
	else {
		if (logEnable) log.debug "Unable to parse description: $description"
	}
	
	return result
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def result = []
	def encapCmd = cmd.encapsulatedCommand(commandClassVersions)
	if (encapCmd) {
		result += zwaveEvent(encapCmd)
	}
	else {
		if (logEnable) log.warn "Unable to extract encapsulated cmd from $cmd"	
	}
	return result
}

private getCommandClassVersions() {
	[
		0x20: 1,  // Basic
		0x59: 1,  // AssociationGrpInfo
		0x5A: 1,  // DeviceResetLocally
		0x5E: 2,  // ZwaveplusInfo
		0x70: 1,  // Configuration
		0x71: 3,  // Alarm v1 or Notification v4
		0x72: 2,  // ManufacturerSpecific*=
		0x73: 1,  // Powerlevel
		0x7A: 2,  // FirmwareUpdateMd
		0x80: 1,  // Battery
		0x84: 2,  // WakeUp
		0x85: 2,  // Association
		0x86: 1,  // Version (2)
		0x98: 1		// Security
	]
}

def zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd) {
	  if (logEnable) log.debug "WakeUpNotification: $cmd"
	def cmds = []
	
	if (canSendConfiguration()) {
		cmds += configure()
	}
	else if (canReportBattery()) {
		cmds << batteryGetCmd()
	}
	else {
		  if (logEnable) log.debug "Skipping battery check because it was already checked within the last $reportEveryHours hours."
	}
	
	if (cmds) {
		cmds << "delay 5000"
	}
	
	cmds << wakeUpNoMoreInfoCmd()
	return response(cmds)
}

private canReportBattery() {
	def reportEveryHours = settings?.reportBatteryEvery ?: 6
	def reportEveryMS = (reportEveryHours * 60 * 60 * 1000)
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
	def val = (cmd.batteryLevel == 0xFF ? 1 : cmd.batteryLevel)
	if (val > 100) {
		val = 100
	}
	else if (val < 1) {
		val = 1
	}
    if (infoEnable) log.info "$device.label battery $val %"
	[
		createEvent(createEventMap("battery", val, null, "%"))
	]
}	

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	if (logEnable) log.debug "ConfigurationReport: $cmd"
	def parameterName
	switch (cmd.parameterNumber) {
		case 1:
			state.enableExternalSensor = (cmd.configurationValue[0] == 0xFF)
			  if (logEnable) logDebug "External Sensor Enabled: ${state.enableExternalSensor}"
			break
		default:	
			parameterName = "Parameter #${cmd.parameterNumber}"
	}		
	if (parameterName) {
		  if (logEnable) log.debug "${parameterName}: ${cmd.configurationValue}"
	} 
	return []
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
	  if (logEnable) log.debug "BasicReport: $cmd"	
	return []
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
	  if (logEnable) log.debug "Basic Set: $cmd"	
	return []
}


def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd) {
	def result = []	
	if (logEnable) log.debug "NotificationReport: $cmd"
	if (cmd.notificationType == 0x06) {
		result += handleContactEvent(cmd.event)
	}
	else if (cmd.notificationType == 0x07) {		
		result += handleTamperEvent(cmd.event)
	}
	return result
}

def zwaveEvent(hubitat.zwave.Command cmd) {
	if (logEnable) log.debug "Unhandled Command: $cmd"
	return []
}

private handleContactEvent(event) {
	def result = []
	def val
	if (event == 0xFF || event == 0x16) {
		val = "open"
        if (motionEnabled)sendEvent(name: "motion", value: "inactive")
        else sendEvent(name: "motion", value: "active")
	}
	else if(event == 0 || event == 0x17) {
		val = "closed"
        if (motionEnabled)sendEvent(name: "motion", value: "active")
        else sendEvent(name: "motion", value: "inactive")
	}
	if (val) {
		result << createEvent(createEventMap("contact", val))
	}
	return result
}

private handleTamperEvent(event) {
	def result = []
	def val
	if (event == 0x03) {
		val = "detected"
	}
	else if (event == 0) {
		if (settings?.autoClearTamper) {
			val = "clear"
		}
		else {
			if (logEnable) log.debug "Tamper is Clear"
		}
	}
	if (val) {
		result << createEvent(createEventMap("tamper", val))
	}
	return result
}

// Resets the tamper attribute to clear and requests the device to be refreshed.
def refresh() {	
	if (device.currentValue("tamper") != "clear") {
		sendEvent(createEventMap("tamper", "clear", false))
	}
	else {
		if (logEnable) log.debug "The configuration and attributes will be refresh the next time the device wakes up.  If you want this to happen immediately, open the back cover of the device, wait until the red light turns solid, and then put the cover back on."
	}
}

def createEventMap(eventName, newVal, displayed=null, unit=null) {
	if (displayed == null) {
		displayed = (device.currentValue(eventName) != newVal)
	}
	if (displayed) {
		if (infoEnable) log.info "$device.label ${eventName.capitalize()} is ${newVal}"
	}
	def eventMap = [
		name: eventName, 
		value: newVal, 
		displayed: displayed,
		isStateChange: true
	]
	if (unit) {
		eventMap.unit = unit
	}
	return eventMap
}

private wakeUpIntervalSetCmd(val) {
	if (logEnable) log.debug "wakeUpIntervalSetCmd(${val})"
	return secureCmd(zwave.wakeUpV2.wakeUpIntervalSet(seconds:val, nodeid:zwaveHubNodeId))
}

private wakeUpNoMoreInfoCmd() {
	return secureCmd(zwave.wakeUpV2.wakeUpNoMoreInformation())
}

private batteryGetCmd() {
	if (logEnable) log.debug "Requesting battery report"
	return secureCmd(zwave.batteryV1.batteryGet())
}

private externalSensorConfigGetCmd() {
	return configGetCmd(1)
}

private externalSensorConfigSetCmd(isEnabled) {
	return configSetCmd(1, 1, (isEnabled ? 0xFF : 0x00))
}

private configSetCmd(paramNumber, valSize, val) {	
	if (logEnable) log.debug "Setting configuration param #${paramNumber} to ${val}"
	return secureCmd(zwave.configurationV1.configurationSet(parameterNumber: paramNumber, size: valSize, configurationValue: [val]))
}

private configGetCmd(paramNumber) {
	if (logEnable) log.debug "Requesting configuration report for param #${paramNumber}"
	return secureCmd(zwave.configurationV1.configurationGet(parameterNumber: paramNumber))
}

private secureCmd(cmd) {
	if (zwaveInfo?.zw?.contains("s") || ("0x98" in device.rawDescription?.split(" "))) {
		return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	}
	else {
		return cmd.format()
	}	
}

private canSendConfiguration() {
	return (!state.isConfigured || state.pendingRefresh != false	|| state.pendingChanges != false)
}

private convertToLocalTimeString(dt) {
	def timeZoneId = location?.timeZone?.ID
	if (timeZoneId) {
		return dt.format("MM/dd/yyyy hh:mm:ss a", TimeZone.getTimeZone(timeZoneId))
	}
	else {
		return "$dt"
	}	
}

private isDuplicateCommand(lastExecuted, allowedMil) {
	!lastExecuted ? false : (lastExecuted + allowedMil > new Date().time) 
}

