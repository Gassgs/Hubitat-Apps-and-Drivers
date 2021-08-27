/**
 *  Zemismart Zigbee w/ Soma Tilt 2  Vertical Blind Driver for Hubitat
 *  Set level to control Zemismart Zigbee Rail / Set Tilt level to rotate blinds with Soma Tilt.
 *  Open and close contols both devices properly
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
 This DTH is coded based on iquix's tuya-window-shade DTH.
 https://github.com/iquix/Smartthings/blob/master/devicetypes/iquix/tuya-window-shade.src/tuya-window-shade.groovy
 */

import groovy.json.JsonOutput
import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils

def driverVer() { return "1.0" }

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
        

        //attribute "Direction", "enum", ["Reverse","Forward"]

		fingerprint endpointId: "01", profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_wmcdj3aq", model: "mcdj3aq", deviceJoinName: "Zemismart Zigbee Blind"

    }

	preferences {
        input name: "connectIp",type: "text", title: "Soma Connect IP Address", required: true
        input name: "mac",type: "text", title: "Mac address of Tilt 2 device", required: true
        input name: "Direction", type: "enum", title: "Direction Set", defaultValue: "00", options:["01": "Reverse", "00": "Forward"], displayDuringSetup: true
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
	if (level == 0) {
    	sendEvent(name: "windowShade", value: "closed")
        sendEvent(name: "switch", value: "off")
        setTiltLevel(0)
    } else if (level == 100) {
    	sendEvent(name: "windowShade", value: "open")
        sendEvent(name: "switch", value: "on")
        setTiltLevel(60)
    } else if (level > 0 && level < 40) {
		sendEvent(name: "windowShade", value: "partially open")
        sendEvent(name: "switch", value: "on")
        setTiltLevel(25)
    } else if (level >= 40 && level < 70) {
		sendEvent(name: "windowShade", value: "partially open")
        sendEvent(name: "switch", value: "on")
        setTiltLevel(35)
    } else if (level >= 70 && level < 100) {
		sendEvent(name: "windowShade", value: "partially open")
        sendEvent(name: "switch", value: "on")
        setTiltLevel(45)
    } else {
    	sendEvent(name: "windowShade", value: "unknown")
        //return
    }
    sendEvent(name: "level", value: (level))
    sendEvent(name: "position", value: (level))
}

def close() {
	if(logEnable) log.info "$device.label close()"
	def currentLevel = device.currentValue("level")
    if (currentLevel == 0) {
    	sendEvent(name: "windowShade", value: "closed")
    }
	setLevel(0)
}

def open() {
	if(logEnable) log.info "$device.label open()"
    def currentLevel = device.currentValue("level")
    if (currentLevel == 100) {
    	sendEvent(name: "windowShade", value: "open")
        return
    }
	setLevel(100)
}

def pause() {
	if(logEnable) log.info "$device.label pause()"
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
            if (currentTilt <= 50){
                setTiltLevel(100)
                if (state.somaConnected){
                    sendEvent(name:"windowShade",value:"closing")
                    sendTuyaCommand("0202", "00", "04000000"+HexUtils.integerToHexString(data.intValue(), 1))
                }else{
                    log.warn "Could not set Soma Position, Not changing level"
                }
            }else{
                sendEvent(name:"windowShade",value:"closing")
                sendTuyaCommand("0202", "00", "04000000"+HexUtils.integerToHexString(data.intValue(), 1))
            }     
        }    
    }
    else if (currentLevel > data){
        setTiltLevel(100)
        if (state.somaConnected){
            sendEvent(name:"windowShade", value:"closing")
            sendTuyaCommand("0202", "00", "04000000"+HexUtils.integerToHexString(data.intValue(), 1))
        }else{
            log.warn "Could not set Soma Position, Not changing level"
        }
    }
    else if (currentLevel < data){
        setTiltLevel(100)
        if (state.somaConnected){
            sendEvent(name:"windowShade", value:"opening")
            sendTuyaCommand("0202", "00", "04000000"+HexUtils.integerToHexString(data.intValue(), 1))
        }else{
            log.warn "Could not set Soma Position, Not changing level"
        }
    }
    else if (currentLevel == data) {
        sendEvent(name: "level", value: currentLevel)
    }
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

def stopTilt() {    //Not currently in use
    if (logEnable) log.debug "Sending Stop Command to [${settings.mac}]"
    try {
       httpGet("http://" + connectIp + ":3000/stop_shade/"  + mac) { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.result == "error") {
               if (logEnable) log.debug "Command -ERROR- from SOMA Connect- $json.msg"
           }
           if (json.result == "success") {
               if (logEnable) log.debug "Command Success Response from SOMA Connect"
               runIn(2,refresh)          
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def setTiltLevel(tilt) {
    if (logEnable) log.debug "Sending Set Tilt Command to [${settings.mac}]"
    state.somaConnected = false

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
                sendEvent(name: "tilt", value: value, isStateChange: true)
                state.somaConnected = true
                runIn(timeout,refresh) 
            }
        }
    } catch (Exception e) {
        //log.warn "Call to on failed: ${e.message}"
    }
}

def tiltOpen() {
	def tiltValue = device.currentValue('tilt') as Integer

	if (tiltValue < 100) {
		tiltValue = Math.min(25 * (Math.round(tiltValue / 25) + 1), 100) as Integer
	}
	else {
		tiltValue = 100
	}
	setTiltLevel(tiltValue)
}

def tiltClose() {
	def tiltValue = device.currentValue('tilt') as Integer

	if (tiltValue > 0) {
		tiltValue = Math.max(25 * (Math.round(tiltValue / 25) - 1), 0) as Integer
	}
	else {
		tiltValue = 0
	}
	setTiltLevel(tiltValue)
}

def getTilt() {
    if (logEnable) log.debug "Checking Tilt Position"
    try {
    httpGet("http://" + connectIp + ":3000/get_shade_state/"  + mac) { resp ->
        def json = (resp.data)
        if (logEnable) log.debug "${json}"
        if (json.find{ it.key == "closed_upwards" }){
            shadePos = 50 + (json.position / 2)
        }
        else if (json.position == 0){
            shadePos = 50
        }
        else if (json.position == 100){
            shadePos = 0
        
        }else{
            shadePos = (100 - json.position) /2
        }
        sendEvent(name: "tilt", value: shadePos)
        if (logEnable) log.debug  "Tilt Position set to ${shadePos}"
		}
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
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
    }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def refresh() {
    getBattery()
    getTilt()
}
