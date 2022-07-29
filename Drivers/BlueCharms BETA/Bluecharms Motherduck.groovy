/**
 *  Bluecharms Motherduck Beacon tracking Parent device Driver *BETA*
 *  
 *
 *  Copyright 2022 Gassgs/ Gary Gassmann
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
 *  example webhook....Need to update beacon # and "On" or "Off"
 * 
 *      http://"+ hubIp + ":39501/ POST Beacon1-On
 *
 *
 *  Change History:
 *
 *  V1.0.0  07-25-2022       first run   
 *
 */

def driverVer() { return "1.0" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Bluecharms Motherduck", namespace: "Gassgs", author: "Gary G") {
        capability "Actuator"
        capability "Sensor"
        
        attribute "beacon1","string"
        attribute "beacon2","string"
        attribute "beacon3","string"
    }
}
    preferences {
        input name: "deviceMac",type: "string", title: "Motherduck Device MAC Address", required: true
        input( "beaconCount","enum", options:["none","1","2","3"], title: "Number of Beacon Child Devices (optional)", defaultValue: "none")
        input name: "logInfo", type: "bool", title: "Enable info logging", defaultValue: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    setDeviceNetworkId()
    if (logEnable) runIn(1800, logsOff)
}

void setDeviceNetworkId(){
    if (deviceMac != null && device.deviceNetworkId != state.dni as String) {
        def macAddress = (deviceMac as String)
        def mac = macAddress.replace(":","").replace("-","")
        state.dni = mac as String
        device.deviceNetworkId = state.dni
        if (logEnable) log.debug "${state.dni as String} set as Device Network ID"
    }
    runIn(1,createChild)  
}

def createChild() {
    def childDevice = getChildDevices()?.find {it.data.componentLabel == "Beacon"}
    if (settings.beaconCount == "none"){
        if (childDevice) {
            if (logEnable) log.debug "$device.label beacon count changed to none, child devices will be removed"
            deleteChildren()
        }
    }
    else if (settings.beaconCount != "none"){
        if (!childDevice) {
            if (logEnable) log.debug "$device.label child device(s) added"
            if (settings.beaconCount == "1"){
                for (i in 1) {
                    childDevice = addChildDevice ("Gassgs", "Bluecharms Beacon", "${device.deviceNetworkId}-${i}",[label: "Beacon ${i}", isComponent: false, componentLabel: "Beacon"])
                }
            }
            else if (settings.beaconCount == "2"){
                for (i in 1..2) {
                    childDevice = addChildDevice ("Gassgs", "Bluecharms Beacon", "${device.deviceNetworkId}-${i}",[label: "Beacon ${i}", isComponent: false, componentLabel: "Beacon"])
                }
            }
            else if (settings.beaconCount == "3"){
                for (i in 1..3) {
                    childDevice = addChildDevice ("Gassgs", "Bluecharms Beacon", "${device.deviceNetworkId}-${i}",[label: "Beacon ${i}", isComponent: false, componentLabel: "Beacon"])
                }
            }
        }else{
            if (logInfo) log.info "$device.label Child (children) already exist"
            if (logEnable) log.debug "$device.label Child (children) already exist"
        }
	}
}

def deleteChildren() {
    if (logEnable) log.debug "Deleting child devices"
	if (logInfo) log.info "Deleting children"
	def children = getChildDevices()
    children.each {child->
  		deleteChildDevice(child.deviceNetworkId)
    }
}

def parse(LanMessage){
    if (logEnable) log.debug "data is ${LanMessage}"
    def msg = parseLanMessage(LanMessage)
    def json = msg.body
    if (logEnable) log.debug "${json}"
    if (json.contains("Beacon1")){
        def childDevice1 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-1"}          
        if (logEnable) log.debug "Found the word Beacon1"
        if (json.contains("On")){
            if (logEnable) log.debug "Found the value On"
            if (logInfo) log.info "$device.label - Beacon 1 is Detected"
            sendEvent(name:"beacon1",value:"detected")
            if (childDevice1) {
                childDevice1.sendEvent(name: "presence", value:"present")
                childDevice1.sendEvent(name: "motion", value:"active")
                childDevice1.sendEvent(name: "beacon", value:"detected")
            }
        }
        else if (json.contains("Off")){
            if (logEnable) log.debug "Found the value Off"
            if (logInfo) log.info "$device.label - Beacon 1 is Not Detected"
            sendEvent(name:"beacon1",value:"not detected")
            if (childDevice1) {
                childDevice1.sendEvent(name: "presence", value:"not present")
                childDevice1.sendEvent(name: "motion", value:"inactive")
                childDevice1.sendEvent(name: "beacon", value:"not detected")
            }
        }
    }
    if (json.contains("Beacon2")){
        def childDevice2 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-2"}          
        if (logEnable) log.debug "Found the word Beacon2"
        if (json.contains("On")){
            if (logEnable) log.debug "Found the value On"
            if (logInfo) log.info "$device.label - Beacon 2 is Detected"
            sendEvent(name:"beacon2",value:"detected")
            if (childDevice2) {
                childDevice2.sendEvent(name: "presence", value:"present")
                childDevice2.sendEvent(name: "motion", value:"active")
                childDevice2.sendEvent(name: "beacon", value:"detected")
            }
        }
        else if (json.contains("Off")){
            if (logEnable) log.debug "Found the value Off"
            if (logInfo) log.info "$device.label - Beacon 2 is Not Detected"
            sendEvent(name:"beacon2",value:"not detected")
            if (childDevice2) {
                childDevice2.sendEvent(name: "presence", value:"not present")
                childDevice2.sendEvent(name: "motion", value:"inactive")
                childDevice2.sendEvent(name: "beacon", value:"not detected")
            }
        }
    }
    if (json.contains("Beacon3")){
        def childDevice3 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-3"}          
        if (logEnable) log.debug "Found the word Beacon3"
        if (json.contains("On")){
            if (logEnable) log.debug "Found the value On"
            if (logInfo) log.info "$device.label - Beacon 3 is Detected"
            sendEvent(name:"beacon3",value:"detected")
            if (childDevice3) {
                childDevice3.sendEvent(name: "presence", value:"present")
                childDevice3.sendEvent(name: "motion", value:"active")
                childDevice3.sendEvent(name: "beacon", value:"detected")
            }
        }
        else if (json.contains("Off")){
            if (logEnable) log.debug "Found the value Off"
            if (logInfo) log.info "$device.label - Beacon 3 is Not Detected"
            sendEvent(name:"beacon3",value:"not detected")
            if (childDevice3) {
                childDevice3.sendEvent(name: "presence", value:"not present")
                childDevice3.sendEvent(name: "motion", value:"inactive")
                childDevice3.sendEvent(name: "beacon", value:"not detected")
            }
        }
    }
}

def installed() {
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    if (logEnable) runIn(1800, logsOff)
}

def uninstalled() {
    deleteChildren()
}

