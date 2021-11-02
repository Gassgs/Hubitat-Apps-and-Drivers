/**
 *  ****************  Harmony Controlled TV ****************
 *
 * Harmony controlled TV. Power state based on power meter
 *
 *
 *  Copyright 2021 Gassgs / Gary Gassmann
 *
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *-------------------------------------------------------------------------------------------------------------------
 *
 *
 *-------------------------------------------------------------------------------------------------------------------
 *
 *  Last Update: 10/30/2021
 *
 *  Changes:
 *
 *  V1.0.0 -        8-10-2021       First run
 *  V1.1.0 -        10-30-2021      Clean up
 *  V1.2.0 -        10-31-2021      Handler rewrite and improvements
 *
 */

import groovy.transform.Field

definition(
    name: "Harmony Controlled TV",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Harmony Controlled TV app",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {

	section {

     paragraph(
         title: "Harmony IR Controlled TV",
         required: false,
    	"<div style='text-align:center'><b><big>: Harmony IR Controlled TV :</big></b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>Power and Control options</b></div>"
        )
        input(
            name:"tv",
            type:"capability.switch",
            title: "<b> - Harmony Controlled TV Device - </b>",
            required: true
            )
    }
    section{
        input(
            name:"powerMeter",
            type:"capability.powerMeter",
            title: "Power reporting device conected to TV",
            multiple: false,
            required: true,
            submitOnChange: true
            )
        input(
            name:"powerThreshold",
            type:"number",
            title: "Power reporting threshold to determine if TV is on or off",
            multiple: false,
            defaultValue: 15,
            required: true,
            submitOnChange: true
            )
    }
    section{
        input(
            name:"harmonyHub",
            type:"capability.switch",
            title:"Harmony hub that sends the IR commands",
            multiple: false,
            required: true,
            submitOnChange: true
            )
        input(
            name:"deviceId",
            type:"string",
            title:"Harmony device ID #",
            multiple: false,
            required: true,
            submitOnChange: true
            )
        input(
            name:"pwr",
            type:"string",
            title:"Power on/off command",
            multiple: false,
            required: true,
            submitOnChange: true
            )
        input(
            name:"volUp",
            type:"string",
            title:"Volume Up command",
            multiple: false,
            required: true,
            submitOnChange: true
            )
        input(
            name:"volDown",
            type:"string",
            title:"Volume Down command",
            multiple: false,
            required: true,
            submitOnChange: true
            )
        input(
            name:"hdmi1",
            type:"string",
            title:"HDMI 1 command - *optional",
            multiple: false,
            required: false,
            submitOnChange: true
            )
        input(
            name:"hdmi2",
            type:"string",
            title:"HDMI 2 command - *optional",
            multiple: false,
            required: false,
            submitOnChange: true
            )
    }
    section{
        input(
            name:"logEnable",
            type:"bool",
            title: "Enable Info logging",
            required: true,
            defaultValue: false
            )
    }
}
    
def installed(){
    initialize()
}

def uninstalled(){
    logInfo ("uninstalling app")
}

def updated(){
    logInfo ("Updated with settings: ${settings}")
    unschedule()
    unsubscribe()
    initialize()
}

def initialize(){
    logInfo ("Settings: ${settings}")
    subscribe(settings.tv, "command", commandHandler)
    subscribe(settings.powerMeter,"power", powerHandler)
    logInfo ("subscribed to Events")
}

def commandHandler(evt){
    logInfo ("$app.label command $evt.value")
    cmd = evt.value
    if (cmd == "on"){
        turnTvOn()
    }
    if (cmd == "off"){
        turnTvOff()
    }
    if (cmd == "hdmi1"){
        if (settings.hdmi1){
        settings.harmonyHub.deviceCommand(hdmi1,deviceId)
        }
    }
    if (cmd == "hdmi2"){
        if (settings.hdmi2){
        settings.harmonyHub.deviceCommand(hdmi2,deviceId)
        }
    }
    if (cmd == "up"){
        settings.harmonyHub.deviceCommand(volUp,deviceId)
    }
    if (cmd == "down"){
        settings.harmonyHub.deviceCommand(volDown,deviceId)
    } 
}

def powerHandler(evt){
    meterValue = evt.value.toDouble()
    power = powerThreshold as Integer
    state.tvOff = (meterValue < power)
    state.tvOn = (meterValue >= power)
    if (state.tvOff){
        sendEvent(settings.tv,[name:"switch",value:"off"])
    }
    if (state.tvOn){
        sendEvent(settings.tv,[name:"switch",value:"on"])
    }
}

def turnTvOn(){
    if (state.tvOff){
        settings.harmonyHub.deviceCommand(pwr,deviceId)
    }
}

def turnTvOff(){
    if (state.tvOn){
        settings.harmonyHub.deviceCommand(pwr,deviceId)   
    }
}

void logInfo(String Msg){
    if (settings?.logEnable !=false){
        log.info "$Msg"
    }
}
