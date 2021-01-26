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
 *    V1.0  1-20-2021         -first run - Gassgs  
 *    
 * 
 */

metadata {
    definition (name: "Virtual Laundry Machine", namespace: "Gassgs", author: "Gary G") {
        capability"Actuator"
        capability "Switch"
        capability "Sensor"
        capability "AccelerationSensor"

        
        command "update",[[name:"status",type:"STRING"],[name:"text",type:"STRING"]]
        command"start"
        command"stop"

       
      
        attribute"status","string"
        attribute"notification","string"
        attribute"acceleration","string"
    }
}
        

//doesn't function, device is for monitoring only
def on(){
}
//used to turn off notifications and reset to "off" and "idle"
def off(){
    sendEvent(name:"switch",value:"off")
}

def update(String status,String value) {
    textValue=value
    statusValue=status
    sendEvent(name:statusValue, value: textValue) 
}
//no functions below. device is for monitoring only
def start(){
    log.info "start pushed, doesn't do anything"
}

def stop(){
     log.info "stop pushed, doesn't do anything"
}

