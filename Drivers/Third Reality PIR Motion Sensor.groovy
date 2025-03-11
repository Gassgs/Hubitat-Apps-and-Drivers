/*
 *  Third Reality Motion Sensor  model -3RMS16BZ
 *
 *
 *  Copyright 2018 SmartThings / Modified by Gassgs for Third Reality Sensor
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
 *  V1.0.0  07-10-2024      First run, battery and motion reporting.
 *  V1.1.0  07-12-2024      Added battery change date data.
 *  V1.2.0  07-15-2024      Added timeout and Led brightness settings, compatible with latest fiirmware Ver 43 (1233-D3A1-00000043)
 *  V1.3.0  07-22-2024      Cleanup and bug fixes
 *  V1.4.0  07-27-2024      Quick fix for battery reporting -200
 */

def driverVer() { return "1.4" }

import hubitat.zigbee.clusters.iaszone.ZoneStatus
import groovy.transform.Field

metadata
{
	definition(name: "ThirdReality Motion Sensor", namespace: "Gassgs", author: "GaryG")
	{
		capability "Motion Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Refresh"
		capability "Sensor"
        
        command "setLedBrightness", [[name: "Led", type:"ENUM", constraints:["blue","red"]], [name: "Level", description: "number 0...100", type: "NUMBER"]]
        command "setTimeout",[[name: "Set Timeout",type: "NUMBER",description:"Motion timeout in seconds - 0...9999sec"]]
      
        attribute "timeout","string"
        attribute "blueLed","string"
        attribute "redLed","string"

        fingerprint inClusters:"0000,0001,0500", outClusters:"0019", manufacturer:"Third Reality, Inc",  model:"3RMS16BZ", deviceJoinName: "Third Reality Motion Sensor", controllerType: "ZGB"}
    
	preferences{
		section{
            input "enableInfo", "bool", title: "Enable info logging?", defaultValue: true, required: false
			input "enableDebug", "bool", title: "Enable debug logging?", defaultValue: false, required: false
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

def parse(String description){
	logDebug "Msg: Description is $description"
	Map map = zigbee.getEvent(description)
	if (!map){
		if (description?.startsWith('zone status')){
			map = parseIasMessage(description)
		}
		else{
			Map descMap = zigbee.parseDescriptionAsMap(description)
            logDebug "Parsed Map $descMap"
            if (descMap.cluster == "0001" && descMap.attrId == "0020"){
				batteryEvent(Integer.parseInt(descMap.value,16))
			}
            else if (descMap.cluster == "0500"){  //Zone status on refresh
                status = (descMap?.value)
                if (status == "0001"){
                    getMotionResult('active')
                    logDebug "Motion Active on Refresh"
                }
                else if (status == "0000"){
                    getMotionResult('inactive')
                    logDebug "Motion Inactive on Refresh"
                }
            }
            else if (descMap.clusterId == "FF01"){  //timeout
                if (state.over){
                    timeoutEvent(descMap?.data[4]+descMap?.data[5] as int)
                }else{
                    timeoutEvent(descMap?.data[4] as int)
                }
			}
            else if (descMap.clusterId == "FF00"){  //Led Level
                led = (descMap?.data-descMap?.data[4]-descMap?.data[5] as String)
                if (led.contains("2")){
                    if (state.over){
                        blueLedEvent(descMap?.data[4]+descMap?.data[5] as int)
                    }else{
                        blueLedEvent(descMap?.data[4] as int)
                    }
                }
                else{
                    if (state.over){
                        redLedEvent(descMap?.data[4]+descMap?.data[5] as int)
                    }else{
                        redLedEvent(descMap?.data[4] as int)
                    }
                }
			}
			else if (descMap?.clusterId == "0500" && descMap.attrInt == 0x0002){
				def zs = new ZoneStatus(zigbee.convertToInt(descMap.value, 16))
				map = translateZoneStatus(zs)
			}
            else if (descMap?.clusterId == "0001" && descMap.command == "07" && descMap.data[0] == "00"){
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
	else if (map.name == "batteryVoltage"){
		map.unit = "V"
		map.descriptionText = "${device.displayName} battery voltage is ${map.value} volts"
        logDebug "$device.label battery voltage $map.value"
        getBatteryResult(map.value)
	}
    else if (map.name == "battery"){
        logDebug "$device.label battery $map.value Logged - not sent to attribute to avoid conflicting reports"
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


def getBatteryResult(rawValue){
    if (rawValue == 0.0){
        batteryValue = 100
    }
    else{
        def minVolts = 2.0
        def maxVolts = 3.0
        def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
        batteryValue = Math.min(100, pct)
    }
    sendEvent("name": "battery", "value": batteryValue, "unit": "%", "displayed": true, isStateChange: true)
    logInfo "$device.label battery $batteryValue%"

	return
}

def batteryEvent(rawValue) {
    logDebug "$device.label $rawValue for battery percent battery event"
	def batteryVolts = (rawValue / 10).setScale(2, BigDecimal.ROUND_HALF_UP)
	def minVolts = 20
	def maxVolts = 30
	def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
	def batteryValue = Math.min(100, pct)
	if (batteryValue > 0){
        log.trace "WTF"
		sendEvent("name": "battery", "value": batteryValue, "unit": "%", "displayed": true, isStateChange: true)
		logInfo "$device.displayName battery changed to $batteryValue%"
		logdebug "$device.displayName voltage changed to $batteryVolts volts"
	}
    
	return
}

private Map getMotionResult(value){
    status = device.currentValue "motion"
    if (value == "active"){
        sendEvent(name:"motion",value:"active")
        if (status != "active"){  //added to reduce duplicate logging entries
            logInfo "$device.label Motion Active"
        }
    }else{
        sendEvent(name:"motion",value:"inactive")
        if (status != "inactive"){ //added to reduce duplicate logging entries
            logInfo "$device.label Motion Inactive"
        }
    }
}

def timeoutEvent(data){
    log.info "$device.label Timeout set to $data"
    sendEvent(name:"timeout",value:"$data")
}

def blueLedEvent(data){
    log.info "$device.label Blue Led set to $data"
    if (data == 0){
        sendEvent(name:"blueLed",value:"off")
    }else{
        sendEvent(name:"blueLed",value:"$data")
    }   
}

def redLedEvent(data){
    log.info "$device.label Red Led set to $data"
    if (data == 0){
        sendEvent(name:"redLed",value:"off")
    }else{
        sendEvent(name:"redLed",value:"$data")
    }
}

def refresh() {
	logInfo "Refreshing Values"
	def refreshCmds = []

	refreshCmds +=
		zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020) +
        zigbee.readAttribute(0x0500, 0x0002) 
		zigbee.enrollResponse()

	return refreshCmds
}

def configure() {
	logInfo "$device.label Configuring Reporting..."
	def configCmds = []

	configCmds +=
		zigbee.batteryConfig()

	return configCmds
}

def setTimeout(data) {
    if (data > 99){state.over = true}else{state.over = false}
    if (data <=10000){
        logDebug "$device.label cmd = Timeout set to ${data}"
        return zigbee.writeAttribute(0xff01, 0x0001, 0x00, data as int, [:], delay=200)
    }
    else{
        log.warn "$device.label cmd = Timeout can not be set to ${value}, value is out of range"
    }
}

def setLedBrightness(){
    log.warm "$device.label no values selected"
}

def setLedBrightness(color,level = null){
    if (level > 99){state.over = true}else{state.over = false}
    if (level != null  && level <= 100){
        logDebug "$device.label cmd =  ${color} LED set to ${level}"
        if (color == "blue"){
            return zigbee.writeAttribute(0xff00, 0x0002, 0x00, level as int, [:], delay=200)
        }
        else if (color == "red"){
            return zigbee.writeAttribute(0xff00, 0x0000, 0x00, level as int, [:], delay=200)
        }
    }
    else{
        log.warn "$device.label ${color} LED not set, ${level} is out of range"
    }
}

def installed(){
    configure()
}

def updated(){
    state.DriverVersion = driverVer()
	initialize()
    configure()
}

def initialize(){
	if (enableDebug){
		logInfo "Verbose logging has been enabled for the next 30 minutes."
		runIn(1800, logsOff)
	}
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
    logInfo "debug logging disabled..."
	device.updateSetting("enableDebug", [value:"false",type:"bool"])
}
