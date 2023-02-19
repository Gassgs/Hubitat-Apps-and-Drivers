/**
 *  Kitchen Stove-Range Hood
 *  Zigbee Sengled bulb for light and Sonoff Basic Tasmota switch for fan 
 *
 *	Copyright 2022 Gassgs
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 * *  Change History:
 *
 *  V1.0.0  12-14-2022       first run
 *  V1.1.0  12-15-2022       added fan control for google home
 *  V1.2.0  12-16-2022       added change level capability
 *
 */
def driverVer() { return "1.2" }
import groovy.transform.Field

metadata {
	definition (name: "Tasmota Kitchen Stove Hood", namespace: "Gassgs", author: "Gary G",runLocally: true, executeCommandsLocally: true, genericHandler: "Zigbee") {
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Switch Level"
        capability "Change Level"
		capability "Light"
        capability "Fan Control"
        
        command "setSpeed", [[name:"Speed*", type: "ENUM", description: "Speed", constraints: ["on", "off"] ] ]
        command "toggle"
        
        attribute "wifi","string"
	}
}

preferences {
        input name: "transitionTime", type: "enum", description: "", title: "<b>Transition time</b>", options: [[400:"400ms"], [500:"500ms"],[1000:"1s"],[1500:"1.5s"],[2000:"2s"],[5000:"5s"]], defaultValue: 400
		input name: "sonoffIP",type: "text", title: "<b>Sonoff Switch IP Address</b>", required: true
        input name: "refreshEnable",type: "bool", title: "<b>Enable to Refresh every 30mins</b>", defaultValue: true
        input name: "infoEnable", type: "bool", title: "<b>Enable info logging</b>", defaultValue: true
        input name: "logEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: true
        
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    if (refreshEnable){
        runEvery30Minutes(refreshTasmota)
        if (logEnable) log.debug "refresh tasmota every 30 minutes scheduled"
    }else{
        unschedule(refreshTasmota)
        if (logEnable) log.debug "refresh tasmota schedule canceled"
	}
    if (logEnable) runIn(1800, logsOff)
}

def parse(String description) {
	if (logEnable) log.debug "description is $description"

	def event = zigbee.getEvent(description)
    if (event.name=="switch"){
        if (infoEnable) log.info "$device.label $event"
    }
	if (event) {
		if (event.name=="level" && event.value==0) {}
		else {
			sendEvent(event)
		}
	} else {
		def descMap = zigbee.parseDescriptionAsMap(description)
		if (descMap && descMap.clusterInt == 0x0006 && descMap.commandInt == 0x07) {
			if (descMap.data[0] == "00") {
				if (logEnable) log.debug "ON/OFF REPORTING CONFIG RESPONSE: " + cluster
				sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
			} else {
				log.warn "ON/OFF REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
			}
		} else if (isSengled() && descMap && descMap.clusterInt == 0x0008 && descMap.attrInt == 0x0000) {
			// This is being done because the sengled element touch/classic incorrectly uses the value 0xFF for the max level.
			// Per the ZCL spec for the UINT8 data type 0xFF is an invalid value, and 0xFE should be the max.  Here we
			// manually handle the invalid attribute value since it will be ignored by getEvent as an invalid value.
			// We also set the level of the bulb to 0xFE so future level reports will be 0xFE until it is changed by
			// something else.
			if (descMap.value.toUpperCase() == "FF") {
				descMap.value = "FE"
			}
			sendHubCommand(zigbee.command(zigbee.LEVEL_CONTROL_CLUSTER, 0x00, "FE0000").collect { new hubitat.device.HubAction(it) }, 0)
			sendEvent(zigbee.getEventFromAttrData(descMap.clusterInt, descMap.attrInt, descMap.encoding, descMap.value))
		} else {
			if (logEnable) log.warn "DID NOT PARSE MESSAGE for description : $description"
			if (logEnable) log.debug "${descMap}"
		}
	}
}

def off() {
	zigbee.off()
}

def on() {
	zigbee.on()
}

def toggle(){
    status = device.currentValue("switch")
    if (status == "on"){
        off()
    }else{
        on()
    }
}

def startLevelChange(direction){
    Integer upDown = direction == "down" ? 1 : 0
    Integer unitsPerSecond = 100
    return "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 1 { 0x${intTo8bitUnsignedHex(upDown)} 0x${intTo16bitUnsignedHex(unitsPerSecond)} }"
}

def stopLevelChange(){
    return [
            "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 3 {}}","delay 250",
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 {}"
    ]
}

def setLevel(value) {
    setLevel(value,(transitionTime?.toBigDecimal() ?: 400) / 400)
}

def setLevel(value,rate) {
    rate = rate.toBigDecimal()
    def scaledRate = (rate * 10).toInteger()
    def cmd = []
    def isOn = device.currentValue("switch") == "on"
    value = (value.toInteger() * 2.55).toInteger()
    if (isOn){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 4 {0x${intTo8bitUnsignedHex(value)} 0x${intTo16bitUnsignedHex(scaledRate)}}",
                "delay ${(rate * 1000) + 400}",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 4 {0x${intTo8bitUnsignedHex(value)} 0x0100}", "delay 275",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}", "delay 275",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 {}"
        ]
    }
    return cmd
}

def refresh() {
	zigbee.onOffRefresh() + zigbee.levelRefresh()
    refreshTasmota()
}

def installed() {
    refresh()
}

def isSengled() {
	device.getDataValue("manufacturer") == "sengled"
}

def configure() {
	log.debug "Configuring Reporting and Bindings."
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

	// OnOff minReportTime 0 seconds, maxReportTime 5 min. Reporting interval if no activity
	zigbee.onOffConfig(0, 300) + zigbee.levelConfig()
}

def setSpeed(data){
    if (data == "on"){
        fanOn()
    }else{
        fanOff()
    }
}

def cycleSpeed(){
    status = device.currentValue("speed")
    if (status == "on"){
        fanOff()
    }else{
        fanOn()
    }
}

def fanOn() {
    if (logEnable) log.debug "Sending On Command to [${settings.sonoffIP}]"
    try {
        httpGet("http://" + sonoffIP + "/cm?cmnd=POWER%20On") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER == "ON"){
                if (logEnable) log.debug "Command Success response from Device"
                runIn(1,refreshTasmota)
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def fanOff() {
    if (logEnable) log.debug "Sending Off Command to [${settings.sonoffIP}]"
    try {
        httpGet("http://" + sonoffIP + "/cm?cmnd=POWER%20Off") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER == "OFF"){
                if (logEnable) log.debug "Command Success response from Device"
                runIn(1,refreshTasmota)
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def refreshTasmota(){
        if(settings.sonoffIP){
        if (logEnable) log.debug "Refreshing Device Status - [${settings.sonoffIP}]"
        try {
           httpGet("http://" + sonoffIP + "/cm?cmnd=status%200") { resp ->
           def json = (resp.data)
            if (logEnable) log.debug "${json}"
               if (json.containsKey("StatusSTS")){
                   status = json.StatusSTS.POWER as String
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logEnable) log.debug "Wifi signal strength $signal db"
                   sendEvent(name:"wifi",value:"${signal}db")
                   if (logEnable) log.debug "$device.label $sonoffIP - Fan- $status Wifi signal strength $signal db"
                   if (infoEnable) log.info "$device.label Fan- $status - Wifi signal strength $signal db"
                   if (status == "ON"){
                       sendEvent(name:"speed",value:"on")
                   }else{
                       sendEvent(name:"speed",value:"off")
                   }
               }
           }
    } catch (Exception e) {
            sendEvent(name:"wifi",value:"offline")
            log.warn "Call to on failed: ${e.message}"
        }
    }
}

//---------------------------------
def intTo16bitUnsignedHex(value) {
    def hexStr = zigbee.convertToHexString(value.toInteger(),4)
    return new String(hexStr.substring(2, 4) + hexStr.substring(0, 2))
}

def intTo8bitUnsignedHex(value) {
    return zigbee.convertToHexString(value.toInteger(), 2)
}
