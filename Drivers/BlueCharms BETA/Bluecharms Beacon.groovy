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
 *
 *  
 * 
 */

metadata {
    definition (name: "Bluecharms Beacon", namespace: "Gassgs", author: "Gary G"){
        capability "Actuator"
        capability "Sensor"
        capability "Motion Sensor"
        capability "Presence Sensor"
        
        attribute "beacon","string"
    }   
    preferences {
        None
    }
}

def installed() {
}
