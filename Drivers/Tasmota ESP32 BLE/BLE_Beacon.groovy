/**
 *  Tasmota BLE Beacon - Child Device
 *
 *
 *  Copyright 2023 Gassgs
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
 * 
 *  V1.0.0  10-23-2023       first run
 *  V1.1.0  11-03-2023       Added command for use with BLE Tracker App
 *
 * 
 */

metadata {
    definition (name: "BLE Beacon", namespace: "Gassgs", author: "Gary G"){
        capability "Actuator"
        capability "Sensor"
        capability "PresenceSensor"
        capability "Beacon"
        
        command "beacon", [[name:"Set beacon", type: "ENUM",description: "Set beacon", constraints: ["detected", "not detected"]]]
        
        attribute "beacon","string"
    }   
    preferences {
        input name: "infoEnable", type: "bool", title: "<b>Enable Info Text logging</b>", defaultValue: true
    }
}

def beacon(value){
    if (value == "detected"){
        if (device.currentValue("beacon") == "not detected" || device.currentValue("beacon") == null){
            if (infoEnable) log.info "$device.label Detected - Present"
            sendEvent(name:"beacon",value:"detected")
            sendEvent(name:"presence",value:"present")
        }
    }
    else if (value == "not detected"){
        if (device.currentValue("beacon") == "detected" || device.currentValue("beacon") == null){
            if (infoEnable) log.info "$device.label Not Detected - Not Present"
            sendEvent(name:"beacon",value:"not detected")
            sendEvent(name:"presence",value:"not present")
        }
    }
}

def installed() {
}
