/**
 *  ****************  Harmony Controlled Fireplace ****************
 *
 * Virtual Fireplace Harmony controlled.
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
 *  Last Update: 1/24/2021
 *
 *  Changes:
 *
 *  V1.0.0 -        1-24-2021       First run
 *  V2.0.0 -        1-26-2021       Fixed all logic to sync attributes
 */

import groovy.transform.Field

definition(
    name: "Harmony Controlled Fireplace",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Harmony Controlled Fireplace app",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {

	section {

     paragraph(
         title: "Harmony IR Controlled Fireplacce",
         required: false,
    	"<div style='text-align:center'><b>: Bedroom Fireplace :</b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>Power and Control options</b></div>"
        )
        input(
            name:"fireplace",
            type:"capability.switch",
            title: " Virtual Fireplace Device   <b>-Bedroom Fireplace-</b>",
            required: true
            )
    }
    section{
        input(
            name:"powerMeter",
            type:"capability.powerMeter",
            title: "Power reporting device conected to Fireplace",
            multiple: false,
            submitOnChange: true
            )
    }
    section{
        input(
            name:"harmonyHub",
            type:"capability.switch",
            title:"Harmony hub that sends the IR commands",
            multiple: false,
            submitOnChange: true
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
    subscribe(settings.fireplace, "switch.on", fireplaceSwitchOnHandler)
    subscribe(settings.fireplace, "switch.off", fireplaceSwitchOffHandler)
    subscribe(settings.fireplace, "heat.off", fireplaceHeatOffHandler)
    subscribe(settings.fireplace, "heat.low", fireplaceHeatLowHandler)
    subscribe(settings.fireplace, "heat.high", fireplaceHeatHighHandler)
    subscribe(settings.fireplace, "color.on", fireplaceColorHandler)
    subscribe(settings.powerMeter,"power", powerHandler)
    logInfo ("subscribed to Events")
}

def fireplaceSwitchOnHandler(evt){
    fireplaceSwitch = evt.value
    logInfo ("fireplace switch $fireplaceSwitch")
    state.fireplaceSwitchOn = (fireplaceSwitch == "on")
    turnFireplaceOn()
}

def fireplaceSwitchOffHandler(evt){
    fireplaceSwitch = evt.value
    logInfo ("fireplace switch $fireplaceSwitch")
    state.fireplaceSwitchOff = (fireplaceSwitch == "off")
    turnFireplaceOff()
}

def fireplaceHeatHighHandler(evt){
    highHeat = evt.value
    logInfo ("fireplace heat $highHeat")
    state.fireplaceSetHeatHigh = (highHeat == "high")
    setHeatHigh()
}

def fireplaceHeatLowHandler(evt){
    lowHeat = evt.value
    logInfo ("fireplace heat $lowHeat")
    state.fireplaceSetHeatHigh = (lowHeat == "low")
    setHeatLow()
}

def fireplaceHeatOffHandler(evt){
    offHeat = evt.value
    logInfo ("fireplace heat $offHeat")
    state.fireplaceSetHeatOff = (offHeat == "off")
    setHeatOff()
}

def powerHandler(evt){
    meterValue = evt.value.toDouble()
    state.fireplaceOff = (meterValue <( 3))
    state.fireplaceOn = (meterValue >( 4))
    state.fireplaceHeatOff = (meterValue  <( 20))
    state.fireplaceHeatLow = (meterValue >(300)&&meterValue <(900))
    state.fireplaceHeatHigh = (meterValue >( 900))
    powerSync()
}

def powerSync(){
    if (state.fireplaceOff){
        settings.fireplace.off()
    }
    if (state.fireplaceOn){
        settings.fireplace.on()
    }
    if(state.fireplaceHeatOff){
        settings.fireplace.setHeat("Off")
    }
    if (state.fireplaceHeatLow){
        settings.fireplace.setHeat("Low")
    }
    if (state.fireplaceHeatHigh){
        settings.fireplace.setHeat("High")
    }
}

def turnFireplaceOn(){
    if (state.fireplaceOff){
        settings.harmonyHub.deviceCommand("pwr","65347561")
    }
}

def turnFireplaceOff(){
    if (state.fireplaceOn){
        settings.harmonyHub.deviceCommand("pwr","65347561")   
    }
}

def setHeatOff(){
    if (state.fireplaceHeatLow){
        settings.harmonyHub.deviceCommand("low","65347561")
    }
    else if  (state.fireplaceHeatHigh){
        settings.harmonyHub.deviceCommand("high","65347561")
    }
}

def setHeatLow(){
    if (state.fireplaceHeatLow==false){
        settings.harmonyHub.deviceCommand("low","65347561")
    }
}

def setHeatHigh(){
    if (state.fireplaceHeatHigh==false){
        settings.harmonyHub.deviceCommand("high","65347561")
    }
}

def fireplaceColorHandler(evt){
    changeColor = evt.value
    logInfo ("fireplace change color $changeColor")
    state.fireplaceChangeColorOn = (changeColor == "on")
    changeColor()
}

def changeColor(){
        settings.harmonyHub.deviceCommand("clr","65347561")
}


void logInfo(String Msg){
    if (settings?.logEnable !=false){
        log.info "$Msg"
    }
}
