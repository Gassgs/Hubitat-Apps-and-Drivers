/**
 *  Zemismart Zigbee w/ Soma Tilt 2  Vertical Blind Driver for Hubitat
 *
 *  Set level to control Zemismart Zigbee Rail / Set Tilt level to rotate blinds with Soma Tilt 2.
 *  Open and close controls both devices in sync
 *	Copyright 2021 Gassgs
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *  This DTH is coded based on iquix's tuya-window-shade DTH.
 *  https://github.com/iquix/Smartthings/blob/master/devicetypes/iquix/tuya-window-shade.src/tuya-window-shade.groovy
 *  Change Log:
 *
 *  V1.0.0  8-20-2021       First version
 *  V1.1.0  8-22-2021       Bug fixes and improvements
 *  V1.2.0  8-25-2021       Added Soma Connected Check
 *  V1.3.0  8-26-2021       Added Tilt limits to prevent overtightening.
 *  V1.4.0  8-28-2021       Added short delays when opening from closed, as a safety precaution.
 *  V1.5.0  8-30-2021       Added range limiting for set tilt based on position and tilt open/close commands.
 */

import groovy.json.JsonOutput
import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils

def driverVer() { return "1.5" }

metadata {
	definition(name: "Zigbee + Soma Tilt Vertical Blinds", namespace: "Gassgs", author: "Gary G") {
		capability "Actuator"
		capability "Window Shade"
        capability "WindowBlind"
        capability "Change Level"
		capability "Switch Level"
        capability "Switch"
        capability "Refresh"
        capability "Battery"
        capability "Sensor"
        
        command "tiltOpen"
        command "tiltClose"

		fingerprint endpointId: "01", profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_wmcdj3aq", model: "mcdj3aq", deviceJoinName: "Zemismart Zigbee Blind"

    }

	preferences {
        input name: "connectIp",type: "text", title: "Soma Connect IP Address", required: true
        input name: "mac",type: "text", title: "Mac address of Tilt 2 device", required: true
        input name: "Direction", type: "enum", title: "Direction Set", defaultValue: "00", options:["01": "Reverse", "00": "Forward"], displayDuringSetup: true
        input "logInfoEnable", "bool", title: "Enable info text logging", required: true, defaultValue: true
	    input "logEnable", "bool", title: "Enable debug logging", required: true, defaultValue: true
    }
}

private getCLUSTER_TUYA() { 0xEF00 }
private getSETDATA() { 0x00 }

// Parse incoming device messages to generate events
def parse(String description) {
	if (description?.startsWith('catchall:') || description?.startsWith('read attr -')) {
		Map descMap = zigbee.parseDescriptionAsMap(description)
        //if(logEnable)  "Pasred Map $descMap"
		if (descMap?.clusterInt==CLUSTER_TUYA) {
			if ( descMap?.command == "01" || descMap?.command == "02" ) {
				def dp = zigbee.convertHexToInt(descMap?.data[3]+descMap?.data[2])
                if(logEnable) log.debug "dp = " + dp
				switch (dp) {
					case 1025: // 0x04 0x01: Confirm opening/closing/stopping (triggered from Zigbee)
                    	def data = descMap.data[6]
                    	if (descMap.data[6] == "00") {
                        	if(logEnable) log.debug "parsed opening"
                            levelEventMoving(100)
                        }
                        else if (descMap.data[6] == "02") {
                        	if(logEnable) log.debug "parsed closing"
                            levelEventMoving(0)
                        }
                        else { if (logEnable) log.debug "parsed else case $dp open/close/stop zigbee $data"}
                    	break;

					case 1031: // 0x04 0x07: Confirm opening/closing/stopping (triggered from remote)
                    	def data = descMap.data[6]
                    	if (descMap.data[6] == "01") {
                        	log.trace "remote closing"
                            levelEventMoving(0)
                        }
                        else if (descMap.data[6] == "00") {
                        	log.trace "remote opening"
                            levelEventMoving(100)
                        }
                        else {if (logEnable) log.debug "parsed else case $dp open/close/stop remote $data"}
                    	break;

					case 514: // 0x02 0x02: Started moving to position (triggered from Zigbee)
                    	def pos = zigbee.convertHexToInt(descMap.data[9])
						if(logEnable) log.debug "moving to position :"+pos
                        levelEventMoving(pos)
                        break;

					case 515: // 0x03: Arrived at position
                    	def pos = zigbee.convertHexToInt(descMap.data[9])
                        if(logEnable) log.debug description
                    	log.info "$device.label arrived at position :"+pos
                    	levelEventArrived(pos)
                        break;

                    log.warn "UN-handled CLUSTER_TUYA case  $dp $descMap"
				}
			}

		}
        else {
            //log.warn "UN-Pasred Map $descMap"
        }
	}
}

private levelEventMoving(currentLevel) {
	def lastLevel = device.currentValue("level")
	if(logEnable) log.debug "levelEventMoving - currentLevel: ${currentLevel} lastLevel: ${lastLevel}"
	if (lastLevel == "undefined" || currentLevel == lastLevel) { //Ignore invalid reports
		if (logEnable) log.debug "Ignore invalid reports"
	} else {
		if (lastLevel < currentLevel) {
			sendEvent([name:"windowShade", value: "opening"])
		} else if (lastLevel > currentLevel) {
			sendEvent([name:"windowShade", value: "closing"])
		}
    }
}

private levelEventArrived(level) {
    sendEvent(name: "level", value: (level))
    sendEvent(name: "position", value: (level))
	if (level == 0) {
    	sendEvent(name: "windowShade", value: "closed")
        sendEvent(name: "switch", value: "off")
        runInMillis(250,closeTilt)
    } else if (level == 100) {
    	sendEvent(name: "windowShade", value: "open")
        sendEvent(name: "switch", value: "on")
        runInMillis(250,closeTilt)
    } else if (level > 0 && level < 100) {
		sendEvent(name: "windowShade", value: "partially open")
        sendEvent(name: "switch", value: "on")
        runInMillis(250,closeTilt)
    } else {
    	sendEvent(name: "windowShade", value: "unknown")
        //return
    }
}

def closeTilt(){
    setTiltLevel(0)
}

def close() {
	if(logInfoEnable) log.info "$device.label close()"
	def currentLevel = device.currentValue("level")
    if (currentLevel == 0) {
    	sendEvent(name: "windowShade", value: "closed")
    }
	setLevel(0)
}

def open() {
	if(logInfoEnable) log.info "$device.label open()"
    def currentLevel = device.currentValue("level")
    if (currentLevel == 100) {
    	sendEvent(name: "windowShade", value: "open")
        return
    }
	setLevel(100)
}

def pause() {
	if(logInfoEnable) log.info "$device.label pause()"
	sendTuyaCommand("0104", "00", "0101")

}

def setLevel(data, rate = null) {
    if(logEnable) log.debug "setLevel("+data+")"
    if(logInfoEnable) log.info "$device.label setLevel("+data+")"
    def currentLevel = device.currentValue("level")
    def currentTilt = device.currentValue("tilt")
    if (data == 0){
        if (currentLevel == 0 && currentTilt != 0){
            sendEvent(name: "windowShade", value: "closed")
            setTiltLevel(0)
        }
        else if (currentLevel == 0 && currentTilt == 0){
            sendEvent(name: "windowShade", value: "closed")
        }
        else{
            setTiltLevel(100)
            if (state.somaConnected){
                sendEvent(name:"windowShade",value:"closing")
                sendTuyaCommand("0202", "00", "04000000"+HexUtils.integerToHexString(data.intValue(), 1))
            }else{
                log.warn "Could not set Soma Tilt Position, Not changing level"
            }   
        }    
    }
    else if (currentLevel == 0 && data > 0){
        state.openValue = data
        setTiltLevel(100)
        if (state.somaConnected){
            sendEvent(name:"windowShade", value:"opening")
            runIn(1,openFromClosed)
        }else{
            log.warn "Could not set Soma Tilt Position, Not changing level"
        }
    }
    else if (currentLevel > data){
        setTiltLevel(100)
        if (state.somaConnected){
            sendEvent(name:"windowShade", value:"closing")
            sendTuyaCommand("0202", "00", "04000000"+HexUtils.integerToHexString(data.intValue(), 1))
        }else{
            log.warn "Could not set Soma Tilt Position, Not changing level"
        }
    }
    else if (currentLevel < data){
        state.openValue = data
        setTiltLevel(100)
        if (state.somaConnected){
            sendEvent(name:"windowShade", value:"opening")
            runIn(1,openFromClosed)
        }else{
            log.warn "Could not set Soma Tilt Position, Not changing level"
        }
    }
    else if (currentLevel == data) {
        sendEvent(name: "level", value: currentLevel)
    }
}

def openFromClosed(data){
    data = state.openValue
    if (logInfoEnable) log.info "$device.label Opened to $data % with delayed start"
    sendTuyaCommand("0202", "00", "04000000"+HexUtils.integerToHexString(data.intValue(), 1))
}
                  

def stopLevelChange(){
    pause()
}

def startLevelChange(direction) {
    if (direction == "up") {
        open()
    }else{
        close()
    }
}

def setPosition(data){ 
    if(logEnable) log.debug "setPos to("+data+")"
    setLevel(data, null)
}   

def stopPositionChange() {
    pause()
}
    
def startPositionChange(direction) {
    if (direction == "open") {
        open()
    }else{
        close()
    }
}

def installed() {
	sendEvent(name: "supportedWindowShadeCommands", value: JsonOutput.toJson(["open", "close", "pause"]), displayed: false)
}

def updated() {
	def val = Direction
    sendEvent([name:"Direction", value: (val == "00" ? "Forward" : "Reverse")])
	DirectionSet(val)
    state.DriverVersion=driverVer()
    refresh()
    runEvery30Minutes(refresh)
}

def DirectionSet(Dval) {
	if(logEnable) log.info "Direction set ${Dval} "
    sendTuyaCommand("05040001", Dval, "")
}

private sendTuyaCommand(dp, fn, data) {
	if(logEnable) log.trace "send tuya command ${dp},${fn},${data}"
	zigbee.command(CLUSTER_TUYA, SETDATA, "00" + zigbee.convertToHexString(rand(256), 2) + dp + fn + data)
}

private rand(n) {
	return (new Random().nextInt(n))
}

def on () {
    open()
}

def off () {
    close()
}

def setTiltLevel(tilt) {
    tiltValue = tilt
    state.somaConnected = false
    if (logInfoEnable) log.info "$device.label Requested Tilt Level is $tilt %"
    currentLevel = device.currentValue("level")
    if (currentLevel == 100){
        tilt = tilt * 0.50 + 50
        if (logInfoEnable) log.info "$device.label New adjusted Tilt Level is $tilt %"
    }
    else if (currentLevel >= 90 && currentLevel < 100){
        tilt = tilt * 0.55 + 45
        if (logInfoEnable) log.info "$device.label New adjusted Tilt Level is $tilt %"
    }
    else if (currentLevel >= 80 && currentLevel < 90){
        tilt = tilt * 0.60 + 40
        if (logInfoEnable) log.info "$device.label New adjusted Tilt Level is $tilt %"
    }
    else if (currentLevel >= 70 && currentLevel < 80){
        tilt = tilt * 0.65 + 35
        if (logInfoEnable) log.info "$device.label New adjusted Tilt Level is $tilt %"
    }
    else if (currentLevel >= 60 && currentLevel < 70){
        tilt = tilt * 0.70 + 30
        if (logInfoEnable) log.info "$device.label New adjusted Tilt Level is $tilt %"
    }
    else if (currentLevel >= 50 && currentLevel < 60){
        tilt = tilt * 0.75 + 25
        if (logInfoEnable) log.info "$device.label New adjusted Tilt Level is $tilt %"
    }
    else if (currentLevel >= 40 && currentLevel < 50){
        tilt = tilt * 0.80 + 20
        if (logInfoEnable) log.info "$device.label New adjusted Tilt Level is $tilt %"
    }
    else if (currentLevel >= 30 && currentLevel < 40){
        tilt = tilt * 0.85 + 15
        if (logInfoEnable) log.info "$device.label New adjusted Tilt Level is $tilt %"
    }
    else if (currentLevel >= 20 && currentLevel < 30){
        tilt = tilt * 0.90 + 10
        if (logInfoEnable) log.info "$device.label New adjusted Tilt Level is $tilt %"
    }
    else if (currentLevel >= 10 && currentLevel < 20){
        tilt = tilt * 0.95 + 5
        if (logInfoEnable) log.info "$device.label New adjusted Tilt Level is $tilt %"
    }
    else if (currentLevel >= 0 && currentLevel < 10){
        if (logInfoEnable) log.info "$device.label Tilt Level not adjusted  $tilt %"
    }
    if (logEnable) log.debug "Sending Set Tilt Command to [${settings.mac}]"
    value = tilt.toInteger()
    if (value <= 50){
        position = 100 - (value *2)
        direction = 0
    }
    else if (value > 50){
        position = (value - 50) *2
        direction = 1
    }
    try {
        httpGet("http://" + connectIp + ":3000/set_shade_position/"  + mac + "/" + position + "?close_upwards=" + direction) { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.result == "error") {
                if (logEnable) log.debug "Command -ERROR- from SOMA Connect- $json.msg"
            }
            if (json.result == "success") {
                if (logEnable) log.debug "Command Success Response from SOMA Connect"
                sendEvent(name: "tilt", value: tiltValue, isStateChange: true)
                if (logInfoEnable) log.info "$device.label Tilt level set to $value %"
                state.somaConnected = true
            }
        }
    } catch (Exception e) {
        //log.warn "Call to on failed: ${e.message}"
    }
}

def tiltOpen(){
    tilt = device.currentValue("tilt")
    if (tilt <50){
        setTiltLevel(50)
    }else{
        setTiltLevel(100)
    }
}

def tiltClose(){
    tilt = device.currentValue("tilt")
    if (tilt >50){
        setTiltLevel(50)
    }else{
        setTiltLevel(0)
    }
}

def getBattery() {
    if (logEnable) log.debug "Checking Battery Level"
    try {
    httpGet("http://" + connectIp + ":3000/get_battery_level/"  + mac) { resp ->
        def json = (resp.data)
        if (logEnable) log.debug  "${json}"
        def batteryPercent = json.battery_percentage
        sendEvent(name: "battery", value: batteryPercent)
        if (logEnable) log.debug  "Battery level set to ${batteryPercent}"
        if (logInfoEnable) log.info  "$device.label Battery level is ${batteryPercent} %"
    }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def refresh() {
    getBattery()
}
