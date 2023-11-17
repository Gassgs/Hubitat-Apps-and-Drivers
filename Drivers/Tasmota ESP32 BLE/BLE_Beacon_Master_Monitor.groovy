/**
 *  BLE Beacon Master Monitor Device (optional device to see overall beacon status)
 *  Option for BLE Tracker App
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
 *  V1.0.0  11-17-2023       First run
 *  V1.1.0  11-18-2023       Added state name reference option command
 *
 * 
 */

metadata {
    definition (name: "BLE Beacon Master Monitor", namespace: "Gassgs", author: "Gary G"){
        capability "Actuator"
        capability "Sensor"
        capability "PresenceSensor"
        capability "Beacon"
        
        command "nameBeacon", [[name: "Beacon*", type:"ENUM", constraints:["select beacon","1","2","3","4","5","6","7","8"]], [name: "Name", description: "name reference", type: "STRING"]]
        
        attribute "beacon1","string"
        attribute "beacon2","string"
        attribute "beacon3","string"
        attribute "beacon4","string"
        attribute "beacon5","string"
        attribute "beacon6","string"
        attribute "beacon7","string"
        attribute "beacon8","string"
        attribute "status","string"
    }   
    preferences {
        None
    }
}

def nameBeacon(beacon,name = null){
    if (beacon == "1" && name !=null){
        state."1" = "$name"
    }
    if (beacon == "2" && name !=null){
        state."2" = "$name"
    }
    if (beacon == "3" && name !=null){
        state."3" = "$name"
    }
    if (beacon == "4" && name !=null){
        state."4" = "$name"
    }
    if (beacon == "5" && name !=null){
        state."5" = "$name"
    }
    if (beacon == "6" && name !=null){
        state."6" = "$name"
    }
    if (beacon == "7" && name !=null){
        state."7" = "$name"
    }
    if (beacon == "8" && name !=null){
        state."8" = "$name"
    }
}

def updated() {
    log.info "$device.label - updated..."
}
