/**
 *  Virtual Laundry Machine
 *
 *  Virtual Driver for Washing Maching or Dryer Status
 *
 *  Copyright 2021 Gassgs/ Gary Gassmann
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
 *    V1.0  1-20-2021   -       first run - Gassgs  
 *    V1.1  2-13-2021   -       removed acceleration cap, not needed
 *    V1.2  7-1-2021    -       improved update method
 *    V1.3  10-8-2021   -       cleanup
 */

metadata {
    definition (name: "Virtual Laundry Machine", namespace: "Gassgs", author: "Gary G") {
        capability "Actuator"
        capability "Switch"
        capability "Sensor"

        command"start"
        command"stop"

        attribute"status","string"
    }
}
        

//doesn't function, device is for monitoring only
def on(){
    log.info "on pushed, doesn't do anything"
}
//used to turn off notifications and reset to "off"
def off(){
    sendEvent(name:"switch",value:"off")
    log.info "off pushed, setting to off"
}
//no function. device is for monitoring only
def start(){
    log.info "start pushed, doesn't do anything"
}
//use only to reset the machine to idle if needed
def stop(){
    sendEvent(name:"status",value:"idle")
    log.info "stop pushed, status changed to idle"
}

