/**
 *  Bluecharms Beacon Device *BETA*
 *
 *
 *  Copyright 2022 Gassgs
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
 *  V1.0.0  07-25-2022       first run
 *  V1.1.0  08-12-2022       Added commands
 *
 * 
 */

metadata {
    definition (name: "Bluecharms Beacon", namespace: "Gassgs", author: "Gary G"){
        capability "Actuator"
        capability "Sensor"
        capability "Presence Sensor"
        
        command "inRangeThreshold",[[name: "RSSI", description: "number from -99 to -1  suggested -65", type: "NUMBER"]]
        command "outOfRangeThreshold", [[name: "RSSI", description: "number from -99 to -1  suggested -80", type: "NUMBER"]]
        command "inRangeCount", [[name: "Count", description: "number from 1 to 100  suggested 3", type: "NUMBER"]]
        command "outOfRangeCount", [[name: "Count", description: "number from 1 to 100  suggested 3", type: "NUMBER"]]
        command "scanInterval", [[name: "milliseconds", description: "number from 1000 to 100000  suggested 5000", type: "NUMBER"]]
        
        attribute "beacon","string"
    }   
    preferences {
        None
    }
}

def inRangeThreshold(data){
    def beaconId = "$device.deviceNetworkId" as String
    beacon = ("$beaconId"[-1]) as Integer
    parent.inRangeThreshold(beacon,data,beaconId)
    runIn(2,clearStatus)
}

def inRangeCount(data){
    def beaconId = "$device.deviceNetworkId" as String
    beacon = ("$beaconId"[-1]) as Integer
    parent.inRangeCount(beacon,data,beaconId)
    runIn(2,clearStatus)
}

def outOfRangeThreshold(data){
    def beaconId = "$device.deviceNetworkId" as String
    beacon = ("$beaconId"[-1]) as Integer
    parent.outOfRangeThreshold(beacon,data,beaconId)
    runIn(2,clearStatus)
}

def outOfRangeCount(data){
    def beaconId = "$device.deviceNetworkId" as String
    beacon = ("$beaconId"[-1]) as Integer
    parent.outOfRangeCount(beacon,data,beaconId)
    runIn(2,clearStatus)
}

def scanInterval(data){
    def beaconId = "$device.deviceNetworkId" as String
    beacon = ("$beaconId"[-1]) as Integer
    parent.scanInterval(beacon,data,beaconId)
    runIn(2,clearStatus)
}

def clearStatus() {
    sendEvent(name:"status",value: "-")
}

def installed() {
}
