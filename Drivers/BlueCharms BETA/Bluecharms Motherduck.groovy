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
 * 
 *
 *  Change History:
 *
 *  V1.0.0  07-25-2022       first run
 *  V1.1.0  08-12-2022       Added child devices
 *  V1.2.0  08-13-2022       Added settings commands 
 *
 *
 */

def driverVer() { return "1.2" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Bluecharms Motherduck", namespace: "Gassgs", author: "Gary G") {
        capability "Actuator"
        capability "Sensor"
        
        command "setMacAddress", [[name: "Beacon*", type:"ENUM", constraints:["select beacon","1", "2", "3"]], [name: "MAC",description: "dd:dd:dd:dd:dd  -must start with dd and have 17 characters total including 5 colon marks", type: "STRING"]]
        command "inRangeThreshold", [[name: "Beacon*", type:"ENUM", constraints:["select beacon","1", "2", "3"]], [name: "RSSI", description: "number from -99 to -1  suggested -65", type: "NUMBER"]]
        command "outOfRangeThreshold", [[name: "Beacon*", type:"ENUM", constraints:["select beacon","1", "2", "3"]], [name: "RSSI", description: "number from -99 to -1  suggested -80", type: "NUMBER"]]
        command "inRangeCount", [[name: "Beacon*", type:"ENUM", constraints:["select beacon","1", "2", "3"]], [name: "Count", description: "number from 1 to 100  suggested 3", type: "NUMBER"]]
        command "outOfRangeCount", [[name: "Beacon*", type:"ENUM", constraints:["select beacon","1", "2", "3"]], [name: "Count", description: "number from 1 to 100  suggested 3", type: "NUMBER"]]
        command "scanInterval", [[name: "Beacon*", type:"ENUM", constraints:["select beacon","1", "2", "3"]], [name: "milliseconds", description: "number from 1000 to 100000  suggested 5000", type: "NUMBER"]]
        
        attribute "beacon1","string"
        attribute "beacon2","string"
        attribute "beacon3","string"
    }
}
    preferences {
        input name: "deviceIp",type: "string", title: "Motherduck Device IP Address", required: true
        input name: "deviceMac",type: "string", title: "Motherduck Device MAC Address", required: false
        input name: "hubIp",type: "string", title: "Hubitat Hub IP Address", required: true
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
    if (deviceMac != null){
        def macAddress = (deviceMac as String)
        def mac = macAddress.replace(":","").replace("-","")
        state.dni = mac as String
        if (deviceMac != null && device.deviceNetworkId != state.dni as String) {
            device.deviceNetworkId = state.dni
            if (logEnable) log.debug "${state.dni as String} set as Device Network ID"
        }
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
    else if (settings.beaconCount != "none" && deviceMac != null){
        if (!childDevice) {
            if (logEnable) log.debug "$device.label child device(s) added"
            if (settings.beaconCount == "1"){
                for (i in 1) {
                    childDevice = addChildDevice ("Gassgs", "Bluecharms Beacon", "${device.deviceNetworkId}-${i}",[label: "Beacon ${i}",name: "BlueCharms Beacon ${i}", isComponent: false, componentLabel: "Beacon"])
                }
            }
            else if (settings.beaconCount == "2"){
                for (i in 1..2) {
                    childDevice = addChildDevice ("Gassgs", "Bluecharms Beacon", "${device.deviceNetworkId}-${i}",[label: "Beacon ${i}", name: "BlueCharms Beacon ${i}", isComponent: false, componentLabel: "Beacon"])
                }
            }
            else if (settings.beaconCount == "3"){
                for (i in 1..3) {
                    childDevice = addChildDevice ("Gassgs", "Bluecharms Beacon", "${device.deviceNetworkId}-${i}",[label: "Beacon ${i}", name: "BlueCharms Beacon ${i}", isComponent: false, componentLabel: "Beacon"])
                }
            }
        }else{
            if (logInfo) log.info "$device.label Child (children) already exist or MAC not set"
            if (logEnable) log.debug "$device.label Child (children) already exist or MAC not set"
        }
	}
    setWebhooks1In()
}

def deleteChildren() {
    if (logEnable) log.debug "Deleting child devices"
	if (logInfo) log.info "Deleting children"
	def children = getChildDevices()
    children.each {child->
  		deleteChildDevice(child.deviceNetworkId)
    }
}

def setWebhooks1In(){
    try {
         httpGet("http://" + deviceIp + "/get?WebHook_1_InRange=http://"+ hubIp + ":39501/Beacon1-On") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logEnable) log.trace "Webhook 1 In Range set to - $json"
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
    runIn(1,setWebhooks1Out)
}

def setWebhooks1Out(){
    try {
         httpGet("http://" + deviceIp + "/get?WebHook_1_OutRange=http://"+ hubIp + ":39501/Beacon1-Off") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logEnable) log.trace "Webhook 1 Out of Range set to -$json"
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
    runIn(2,setWebhooks2In)
}

def setWebhooks2In(){
    try {
         httpGet("http://" + deviceIp + "/get?WebHook_2_InRange=http://"+ hubIp + ":39501/Beacon2-On") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logEnable) log.trace "Webhook 2 In Range set to - $json"
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
    runIn(1,setWebhooks2Out)
}

def setWebhooks2Out(){
    try {
         httpGet("http://" + deviceIp + "/get?WebHook_2_OutRange=http://"+ hubIp + ":39501/Beacon2-Off") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logEnable) log.trace "Webhook 2 Out of Range set to -$json"
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
    runIn(2,setWebhooks3In)
}

def setWebhooks3In(){
    try {
         httpGet("http://" + deviceIp + "/get?WebHook_3_InRange=http://"+ hubIp + ":39501/Beacon3-On") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logEnable) log.trace "Webhook 3 In Range set to - $json"
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
    runIn(1,setWebhooks3Out)
}

def setWebhooks3Out(){
    try {
         httpGet("http://" + deviceIp + "/get?WebHook_3_OutRange=http://"+ hubIp + ":39501/Beacon3-Off") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logEnable) log.trace "Webhook 3 Out of Range set to -$json"
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
}
    

def parse(LanMessage){
    if (logEnable) log.debug "data is ${LanMessage}"
    def msg = parseLanMessage(LanMessage)
    def json = msg.header
    if (logEnable) log.trace "${json}"
    if (json.contains("Beacon1")){
        def childDevice1 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-1"}          
        if (logEnable) log.debug "Found the word Beacon1"
        if (json.contains("On")){
            if (logEnable) log.debug "Found the value On"
            if (logInfo) log.info "$device.label - Beacon 1 is Detected"
            sendEvent(name:"beacon1",value:"detected")
            if (childDevice1) {
                childDevice1.sendEvent(name:"beacon",value:"detected")
                childDevice1.sendEvent(name:"presence",value:"present")
            }
        }
        else if (json.contains("Off")){
            if (logEnable) log.debug "Found the value Off"
            if (logInfo) log.info "$device.label - Beacon 1 is Not Detected"
            sendEvent(name:"beacon1",value:"not detected")
            if (childDevice1) {
                childDevice1.sendEvent(name:"beacon",value:"not detected")
                childDevice1.sendEvent(name:"presence",value:"not present")
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
                childDevice2.sendEvent(name:"beacon",value:"detected")
                childDevice2.sendEvent(name:"presence",value:"present")
            }
        }
        else if (json.contains("Off")){
            if (logEnable) log.debug "Found the value Off"
            if (logInfo) log.info "$device.label - Beacon 2 is Not Detected"
            sendEvent(name:"beacon2",value:"not detected")
            if (childDevice2) {
                childDevice2.sendEvent(name:"beacon",value:"not detected")
                childDevice2.sendEvent(name:"presence",value:"not present")
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
                childDevice3.sendEvent(name:"beacon",value:"detected")
                childDevice3.sendEvent(name:"presence",value:"present")
            }
        }
        else if (json.contains("Off")){
            if (logEnable) log.debug "Found the value Off"
            if (logInfo) log.info "$device.label - Beacon 3 is Not Detected"
            sendEvent(name:"beacon3",value:"not detected")
            if (childDevice3) {
                childDevice3.sendEvent(name:"beacon",value:"not detected")
                childDevice3.sendEvent(name:"presence",value:"not present")
            }
        }
    }
}

def inRangeThreshold(beacon,data,id = null){
    if (logEnable && id != null) log.debug "Sent from child device - $id"
    def childDevice = childDevices.find{it.deviceNetworkId == "$id"}
    if (data < -99 || data > -1 || beacon == "select beacon"){
        if (logEnable) log.debug "$data is not a valid value"
        sendEvent(name:"status",value: "! *Value not valid* !")
        if (childDevice){
            childDevice.sendEvent(name:"status",value: "! *Value not valid* !")
        }
        runIn(2,clearStatus)
    }else{ 
        try {
            httpGet("http://" + deviceIp + "/get?Beacon"+beacon+"MinInRangeRSSI="+data+"") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logEnable) log.trace "Response -$json"
                    sendEvent(name:"status",value: "value updated")
                    if (childDevice){
                        childDevice.sendEvent(name:"status",value: "value updated")
                    }
                    runIn(2,clearStatus)
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
             }
         } catch (Exception e) {
             log.warn "Call to on failed: ${e.message}"
        }
    }
}

def inRangeCount(beacon,data,id = null){
    if (logEnable && id != null) log.debug "Sent from child device - $id"
    def childDevice = childDevices.find{it.deviceNetworkId == "$id"}
    if (data > 100 || data < 1 || beacon == "select beacon"){
        if (logEnable) log.debug "$data is not a valid value"
        sendEvent(name:"status",value: "! *Value not valid* !")
        if (childDevice){
            childDevice.sendEvent(name:"status",value: "! *Value not valid* !")
        }
        runIn(2,clearStatus)
    }else{ 
        try {
            httpGet("http://" + deviceIp + "/get?consecutiveInRange"+beacon+"Goal="+data+"") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logEnable) log.trace "Response -$json"
                    sendEvent(name:"status",value: "value updated")
                    if (childDevice){
                        childDevice.sendEvent(name:"status",value: "value updated")
                    }
                    runIn(2,clearStatus)
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
             }
         } catch (Exception e) {
             log.warn "Call to on failed: ${e.message}"
        }
    }
}

def outOfRangeThreshold(beacon,data,id = null){
    if (logEnable && id != null) log.debug "Sent from child device - $id"
    def childDevice = childDevices.find{it.deviceNetworkId == "$id"}
    if (data < -99 || data > -1 || beacon == "select beacon"){
        if (logEnable) log.debug "$data is not a valid value"
        sendEvent(name:"status",value: "! *Value not valid* !")
        if (childDevice){
            childDevice.sendEvent(name:"status",value: "! *Value not valid* !")
        }
        runIn(2,clearStatus)
    }else{ 
        try {
            httpGet("http://" + deviceIp + "/get?Beacon"+beacon+"MaxOutRangeRSSI="+data+"") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logEnable) log.trace "Response -$json"
                    sendEvent(name:"status",value: "value updated")
                    if (childDevice){
                        childDevice.sendEvent(name:"status",value: "value updated")
                    }
                    runIn(2,clearStatus)
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
             }
         } catch (Exception e) {
             log.warn "Call to on failed: ${e.message}"
        }
    }
}

def outOfRangeCount(beacon,data,id = null){
    if (logEnable && id != null) log.debug "Sent from child device - $id"
    def childDevice = childDevices.find{it.deviceNetworkId == "$id"}
    if (data > 100 || data < 1 || beacon == "select beacon"){
        if (logEnable) log.debug "$data is not a valid value"
        sendEvent(name:"status",value: "! *Value not valid* !")
        if (childDevice){
            childDevice.sendEvent(name:"status",value: "! *Value not valid* !")
        }
        runIn(2,clearStatus)
    }else{ 
        try {
            httpGet("http://" + deviceIp + "/get?consecutiveOutRange"+beacon+"Goal="+data+"") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logEnable) log.trace "Response -$json"
                    sendEvent(name:"status",value: "value updated")
                    if (childDevice){
                        childDevice.sendEvent(name:"status",value: "value updated")
                    }
                    runIn(2,clearStatus)
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
             }
         } catch (Exception e) {
             log.warn "Call to on failed: ${e.message}"
        }
    }
}

def scanInterval(beacon,data,id = null){
    if (logEnable && id != null) log.debug "Sent from child device - $id"
    def childDevice = childDevices.find{it.deviceNetworkId == "$id"}
    if (data > 100000 || data < 1000 || beacon == "select beacon"){
        if (logEnable) log.debug "$data is not a valid value"
        sendEvent(name:"status",value: "! *Value not valid* !")
        if (childDevice){
            childDevice.sendEvent(name:"status",value: "! *Value not valid* !")
        }
        runIn(2,clearStatus)
    }else{ 
        try {
            httpGet("http://" + deviceIp + "/get?missedScanIntervalBeacon"+beacon+"="+data+"") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logEnable) log.trace "Response -$json"
                    sendEvent(name:"status",value: "value updated")
                    if (childDevice){
                        childDevice.sendEvent(name:"status",value: "value updated")
                    }
                    runIn(2,clearStatus)
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
             }
         } catch (Exception e) {
             log.warn "Call to on failed: ${e.message}"
        }
    }
}

def setMacAddress(beacon,data){
    if (logEnable)  log.debug "Set mac address - $data"
    if (data != null && beacon != "select beacon"){
        try {
            httpGet("http://" + deviceIp + "/get?Beacon_"+beacon+"="+data+"") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logEnable) log.trace "Response -$json"
                    sendEvent(name:"status",value: "Updating mac address")
                    runIn(3,clearStatus)
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
             }
         } catch (Exception e) {
             log.warn "Call to on failed: ${e.message}"
        }
    }
}

def clearStatus() {
    sendEvent(name:"status",value: "-")
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

