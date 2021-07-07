/**
 *  Copyright 2017 SmartThings
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
 */
import groovy.json.JsonOutput


metadata {
    definition (name: "Zemismart Z-Wave Window Shade Curtain", namespace: "smartthings/Gassgs", author: "SmartThings", ocfDeviceType: "oic.d.blind") {
        capability "Window Shade"
        capability "Actuator"
        capability "Sensor"
        capability "Switch Level" 
        capability "Switch "   
        capability "Change Level"   

preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "logInfoEnable", type: "bool", title: "Enable text info logging", defaultValue: true
    }
}

        fingerprint type: "0x1107", cc: "0x5E,0x26", deviceJoinName: "Window Shade"
        fingerprint type: "0x9A00", cc: "0x5E,0x26", deviceJoinName: "Window Shade"
    }

    simulator {
        status "open":  "command: 2603, payload: FF"
        status "closed": "command: 2603, payload: 00"
        status "10%": "command: 2603, payload: 0A"
        status "66%": "command: 2603, payload: 42"
        status "99%": "command: 2603, payload: 63"
        status "battery 100%": "command: 8003, payload: 64"
        status "battery low": "command: 8003, payload: FF"

        // reply messages
        reply "2001FF,delay 1000,2602": "command: 2603, payload: 10 FF FE"
        reply "200100,delay 1000,2602": "command: 2603, payload: 60 00 FE"
        reply "200142,delay 1000,2602": "command: 2603, payload: 10 42 FE"
        reply "200163,delay 1000,2602": "command: 2603, payload: 10 63 FE"
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def parse(String description) {
    def result = null
    //if (description =~ /command: 2603, payload: ([0-9A-Fa-f]{6})/)
    // TODO: Workaround manual parsing of v4 multilevel report
    def cmd = zwave.parse(description, [0x20: 1, 0x26: 3])  // TODO: switch to SwitchMultilevel v4 and use target value
    if (cmd) {
        result = zwaveEvent(cmd)
    }
    if(logEnable) log.debug "Parsed '$description' to ${result.inspect()}"
    return result
}

def getCheckInterval() {
    // These are battery-powered devices, and it's not very critical
    // to know whether they're online or not – 12 hrs
    4 * 60 * 60
}

def installed() {
    sendEvent(name: "checkInterval", value: checkInterval, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    sendEvent(name: "supportedWindowShadeCommands", value: JsonOutput.toJson(["open", "close", "pause"]), displayed: false)
    response(refresh())
}

def updated() {
    if (device.latestValue("checkInterval") != checkInterval) {
        sendEvent(name: "checkInterval", value: checkInterval, displayed: false)
    }
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    handleLevelReport(cmd)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    handleLevelReport(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
    handleLevelReport(cmd)
}

private handleLevelReport(hubitat.zwave.Command cmd) {
    def descriptionText = null
    def shadeValue = null

    def level = cmd.value as Integer
    if (level >= 99) {
        level = 100
        shadeValue = "open"
        sendEvent(name: "switch", value: "on")
    } else if (level <= 0) {
        level = 0  // unlike dimmer switches, the level isn't saved when closed
        shadeValue = "closed"
        sendEvent(name: "switch", value: "off")
    } else {
        shadeValue = "partially open"
        sendEvent(name: "switch", value: "on")
        descriptionText = "${device.displayName} shade is ${level}% open"
    }
    def levelEvent = createEvent(name: "level", value: level, unit: "%", displayed: false)
    sendEvent(name: "position", value: level, unit: "%") 
    def stateEvent = createEvent(name: "windowShade", value: shadeValue, descriptionText: descriptionText, isStateChange: levelEvent.isStateChange)

    def result = [stateEvent, levelEvent]
    result
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelStopLevelChange cmd) {
    [ createEvent(name: "windowShade", value: "partially open", displayed: false, descriptionText: "$device.displayName shade stopped"),
      response(zwave.switchMultilevelV1.switchMultilevelGet().format()) ]
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
    updateDataValue("MSR", msr)
    if (cmd.manufacturerName) {
        updateDataValue("manufacturer", cmd.manufacturerName)
    }
    createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    if(logEnable) log.debug "unhandled $cmd"
    return []
}

def open() {
    if(logEnable) log.debug "open()"
      if(logInfoEnable)log.info "$device.label open()"
    /*delayBetween([
            zwave.basicV1.basicSet(value: 0xFF).format(),
            zwave.switchMultilevelV1.switchMultilevelGet().format()
    ], 1000)*/
    zwave.basicV1.basicSet(value: 99).format()
}

def close() {
    if(logEnable) log.debug "close()"
    if(logInfoEnable) log.info "$device.label close()"
    /*delayBetween([
            zwave.basicV1.basicSet(value: 0x00).format(),
            zwave.switchMultilevelV1.switchMultilevelGet().format()
    ], 1000)*/
    zwave.basicV1.basicSet(value: 0).format()
}

def on() {
	open() 
}

def off() {
    close()
}

def setLevel(value, duration = null) {
    if(logEnable)  log.debug "setLevel(${value.inspect()})"
    if(logInfoEnable) log.info "$device.label setLevel(${value.inspect()})"
    Integer level = value as Integer
    if (level < 0) level = 0
    if (level > 99) level = 99
    delayBetween([
            zwave.basicV1.basicSet(value: level).format(),
            zwave.switchMultilevelV1.switchMultilevelGet().format()
    ])
}


def pause() {
    if(logEnable) log.debug "pause()"
    stop()
}

def stop() {
    if(logEnable) log.debug "stop()"
    if(logInfoEnable) log.info "$device.label stop()"
    zwave.switchMultilevelV3.switchMultilevelStopLevelChange().format()
}

def stopLevelChange(){
             stop()
}
def startLevelChange(direction) {
    if (direction == "up") {
        open()
    } else {
       close()
    }
}
def setPosition(value) {
    setLevel(value)
    
}

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

def refresh() {
    if(logEnable) log.debug "refresh()"
    delayBetween([
            zwave.switchMultilevelV1.switchMultilevelGet().format(),
    ], 1500)
}
