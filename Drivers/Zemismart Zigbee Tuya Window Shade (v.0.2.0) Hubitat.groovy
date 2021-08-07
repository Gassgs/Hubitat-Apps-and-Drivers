/**
 *  Zemismart Zigbee/Tuya Window Shade (v.0.2.0) Hubitat
 *	Copyright 2020 iquix
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

def driverVer() { return "2.1" }

metadata {
	definition(name: "Zemismart Zigbee/Tuya Window Shade", namespace: "ShinJjang,GG", author: "ShinJjang-iquix-Gassgs", ocfDeviceType: "oic.d.blind", vid: "generic-shade") {
		capability "Actuator"
		capability "Window Shade"
		capability "Switch Level"
        capability"ChangeLevel"
		capability "Sensor"
        capability"Switch"


        attribute "OCcommand", "enum", ["Replace","Original"]
        attribute "stapp", "enum", ["Reverse","Forward"]
        attribute "remote", "enum", ["Reverse","Forward"]

	
		fingerprint  profileId:"0104",inClusters:"0000,0003,0004,0005,0006",outClusters:"0019",manufacturer:"_TYST11_wmcdj3aq",model:"mcdj3aq",deviceJoinName:"Zemismart Zigbee Blind"
        fingerprint  profileId:"0104",inClusters:"0000,0003,0004,0005,0006",outClusters:"0019",manufacturer:"_TYST11_cowvfni3",model:"owvfni3\u0000",deviceJoinName: "Zemismart Zigbee Blind"
        fingerprint  profileId:"0104",inClusters:"0000,000A,0004,0005,EF00",outClusters:"0019",manufacturer:"_TZE200_cowvfni3",model:"TS0601",deviceJoinName: "Zemismart Zigbee Blind"
	
	}

	preferences {
        input name: "Direction", type: "enum", title: "Direction Set", options:["01": "Reverse", "00": "Forward"], required: true, displayDuringSetup: true
        input name: "OCcommand", type: "enum", title: "Replace Open and Close commands", options:["2": "Replace", "0": "Original"], required: true, displayDuringSetup: true
        input name: "stapp", type: "enum", title: "app opening,closing Change", options:["2": "Reverse", "0": "Forward"], required: true, displayDuringSetup: true
        input name: "remote", type: "enum", title: "RC opening,closing Change", options:["1": "Reverse", "0": "Forward"], required: true, displayDuringSetup: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "logInfoEnable", type: "bool", title: "Enable text info logging", defaultValue: true
	}
}

private getCLUSTER_TUYA() { 0xEF00 }
private getSETDATA() { 0x00 }

// Parse incoming device messages to generate events
def parse(String description) {
	if (description?.startsWith('catchall:') || description?.startsWith('read attr -')) {
		Map descMap = zigbee.parseDescriptionAsMap(description)        
		if (descMap?.clusterInt==CLUSTER_TUYA) {
        	if(logEnable)log.debug descMap
			if ( descMap?.command == "01" || descMap?.command == "02" ) {
				def dp = zigbee.convertHexToInt(descMap?.data[3]+descMap?.data[2])
                if(logEnable)log.debug "dp = " + dp
				switch (dp) {
					case 1025: 
                        def parData = descMap.data[6] as int
                        if(parData != 1){
                        def stappVal = (stapp ?:"0") as int
                        def data = Math.abs(parData - stappVal)
						//sendEvent([name:"windowShade", value: (data == 0 ? "opening":"closing"), displayed: true])
                        if(logEnable) log.debug "App control=" + (data == 0 ? "opening":"closing")
                        }
                    	break
					case 1031: // 0x04 0x07: Confirm opening/closing/stopping (triggered from remote)
                        def parData = descMap.data[6] as int
                        def remoteVal = remote as int
                        def data = Math.abs(parData - remoteVal)
						sendEvent([name:"windowShade", value: (data == 0 ? "opening":"closing"), displayed: true])
                        if(logEnable)log.debug "Remote control =" + (data == 0 ? "opening":"closing")
                    	break
					case 514: // 0x02 0x02: Started moving to position (triggered from Zigbee)
                    	def setLevel = zigbee.convertHexToInt(descMap.data[9])
                        def lastLevel = device.currentValue("level")
						//sendEvent([name:"windowShade", value: (setLevel > lastLevel ? "opening":"closing"), displayed: true])
                        if(logEnable)log.debug "Zigbee control =" + (setLevel > lastLevel ? "opening":"closing")
                        break
					case 515: // 0x02 0x03: Arrived at position
                    	def pos = zigbee.convertHexToInt(descMap.data[9])
                    	if(logEnable)log.debug "arrived at position :"+pos
                    	if (pos > 0 && pos <100) {
                        	sendEvent(name: "windowShade", value: "partially open")
                            sendEvent(name:"switch", value:"on")
                        } else {
                        	sendEvent([name:"windowShade", value: (pos == 100 ? "open":"closed"), displayed: true])
                            sendEvent([name:"switch", value: (pos == 100 ? "on":"off"), displayed: true])
                        }
                        sendEvent(name: "level", value: (pos))
                        sendEvent(name: "position", value:(pos)) 
                        break
				}
			}
		}
	}
} 
def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def installed() {
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(3600, logsOff)
	state.DriverVersion=driverVer()
}

def open() {
    if(logInfoEnable)log.info "$device.label open()"
    if(logEnable)log.debug "open()"
    setLevel(100)
}

def close() {
    if(logInfoEnable)log.info "$device.label close()"
    if(logEnable)log.debug "close()"
    setLevel(0)
}

def on(){
    open()
}

def off(){
    close()
}    

def pause() {
	if(logEnable)log.debug "pause()"
    if(logInfoEnable)log.info "$device.label pause()"
	sendTuyaCommand("0104", "00", "0101")
}

def setLevel(data, rate = null) {
    if(logEnable) log.debug "setLevel("+data+")"
    if(logInfoEnable) log.info "$device.label setLevel("+data+")"
    def currentLevel = device.currentValue("level")
    if (currentLevel > data){
        sendEvent(name:"windowShade",value:"closing")
    }
    else if (currentLevel < data){
        sendEvent(name:"windowShade", value:"opening")
    }
    else if (currentLevel == data) {
    sendEvent(name: "level", value: currentLevel)
    sendEvent(name: "position", value: currentLevel)
        
    }
    sendTuyaCommand("0202", "00", "04000000"+HexUtils.integerToHexString(data.intValue(), 1))
}

def setPosition(data){ 
    if(logEnable) log.debug "setPos to("+data+")"
    if(logInfoEnable) log.info "$device.label setPos to("+data+")"
    setLevel(data, null)
}

def stopLevelChange(){
    pause()
}

def startLevelChange(direction) {
    if (direction == "up") {
        open()
    } else {
       close()
    }
}

def stopPositionChange(){
    pause()
}

def startPositionChange(direction) {
    if (direction == "open") {
        open()
    } else {
       close()
    }
}

def refresh() {
	zigbee.readAttribute(CLUSTER_TUYA, 0x00, )
}


def updated() {
	log.debug "val(${Direction}),valC(${OCcommand}),valR(${remote})"
	DirectionSet(Direction ?:"00")
     log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
	state.DriverVersion=driverVer()
}	

def DirectionSet(Dval) {
    if (logEnable) log.debug "Dset(${Dval})"
    sendTuyaCommand("05040001", Dval, "")
}

private sendTuyaCommand(dp, fn, data) {
	if (logEnable) log.debug "${zigbee.convertToHexString(rand(256), 2)}=${dp},${fn},${data}"
	zigbee.command(CLUSTER_TUYA, SETDATA, "00" + zigbee.convertToHexString(rand(256), 2) + dp + fn + data)
}

private rand(n) {
	return (new Random().nextInt(n))
}
