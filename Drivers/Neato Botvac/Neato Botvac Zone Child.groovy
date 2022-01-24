/**
 *  Neato Botvac Connected Series - D7 Zone Child Device
 *
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
 */
def version() {"v1.0"}

metadata {
	definition (name: "Neato Botvac Zone Child", namespace: "alyc100", author: "Gassgs") {
		    capability "Actuator"
        capability "Switch"
        capability "Sensor" 
        
        command "start"
   }
}
preferences{
    input("pwrMode", "enum", options: ["turbo", "eco"], title: "Power Mode", required:true, defaultValue: "turbo")
    input("navMode", "enum", options: ["standard", "extraCare","deep"], title: "Navigation Mode", required:true, defaultValue: "standard" )
    input(name:"logEnable",type:"bool",title: "Enable Info logging",required: true, defaultValue: true)
}

def on(){
    if (logEnable) log.info "$parent.device.label starting $device.label Id# $state.zoneId"
    sendEvent(name:"switch",value:"on")
    roomId = state.zoneId
    if (pwrMode == "turbo"){
        modeParam = 2
    }else{
        modeParam = 1
    }
    if (navMode == "standard"){
        navParam = 1
    }
    else if (navMode == "extraCare"){
        navParam = 2
    }
    else if (navMode == "deep"){
        navParam = 3
        modeParam = 2
    }
	parent.nucleoPOST("/messages", '{"reqId":"1", "cmd":"startCleaning", "params":{"category": "4", "mode":' + modeParam +' , "navigationMode":' + navParam +', "boundaryId":"'+roomId+'"}}')
    runIn(2,off)
    runIn(3,refresh)
}

def off(){
    sendEvent(name:"switch",value:"off")	
}

def start() {
    on()
}

def setId(data){
    state.zoneId = data as String
    log.trace "setId  - $device.label : $state.zoneId"
}

def refresh(){
    parent.refresh()
}

def installed() {
 	initialize()
}

def updated() {
 	initialize()
}

def initialize() {
    state.version = version()
}
