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
 *  Last Update: 10/31/2021
 *
 *  Changes:
 *
 *  V1.0.0 -        1-24-2021       First run
 *  V2.0.0 -        1-26-2021       Fixed all logic to sync attributes
 *  V2.1.0 -        2-04-2021       added outdoor temperature
 *  V2.2.0 -        6-18-2021       Fixed Low and High Sync issue
 *  V2.3.0 -        8-07-2021       Removed Color change (tasmota controlled RGB now)
 *  V2.4.0 -        10-31-2021      Rewrite Handlers to improve response time
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
    	"<div style='text-align:center'><b><big>: Bedroom Fireplace :</big></b></div>"
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
    section{
        input(
            name:"temperatureSensors",
            type:"capability.temperatureMeasurement",
            title:"Outdoor temperature sensor to determine if heat should turn on",
            multiple: true,
            submitOnChange: true
            )
        if(temperatureSensors){
            paragraph "<b>Current temperature  is ${averageTemperature()}</b>"
        input(
            name:"tempThreshold",
            type:"number",
            title:"Temperature threshold to turn heat on",
            multiple: false,
            required: true,
            submitOnChange: true
            )
        }
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
    subscribe(settings.fireplace, "switch", switchHandler)
    subscribe(settings.fireplace, "heat", fireplaceHeatHandler)
    subscribe(settings.powerMeter,"power", powerHandler)
    logInfo ("subscribed to Events")
}

def averageTemperature(){
	def total = 0
    def n = settings.temperatureSensors.size()
	settings.temperatureSensors.each {total += it.currentTemperature}
	return (total /n).toDouble().round(1)
}

def getTemperature(){
	def avg = averageTemperature()
	logInfo ("$app.label Current temperature average is ${averageTemperature()}")
        if (tempThreshold>avg){
            logInfo ("$app.label Outdoor Temp below threshold turning heat on low")
            setHeatLow()
    }
}

def switchHandler(evt){
    switchStatus = evt.value
    logInfo ("$app.label switch $switchStatus")
    state.switchOn = (switchStatus == "on")
    state.switchOff = (switchStatus == "off")
    if (state.switchOn && state.powerOff){
        turnFireplaceOn()
        runIn(1,getTemperature)
    }
    else if (state.switchOff && state.powerOn){
        turnFireplaceOff()
    }
}

def fireplaceHeatHandler(evt){
    heatLevel = evt.value
    logInfo ("$app.label fireplace heat $heatLevel")
    state.fireplaceHeatHigh = (heatLevel == "high")
    state.fireplaceHeatLow = (heatLevel == "low")
    state.fireplaceHeatOff = (heatLevel == "off")
    if (state.fireplaceHeatHigh){
        setHeatHigh()
    }
    else if (state.fireplaceHeatLow){
        setHeatLow()
    }
    else if (state.fireplaceHeatOff){
        setHeatOff()
    }
    runIn(2,powerSync)
}


def powerHandler(evt){
    meterValue = evt.value.toDouble()
    state.powerOff = (meterValue < (1.5))
    state.powerOn = (meterValue >= (1.5))
    state.heatPowerOff = (meterValue  <(300))
    state.heatPowerLow = (meterValue >(300)&&meterValue <(900))
    state.heatPowerHigh = (meterValue >( 900))
    if (state.powerOff && state.switchOn){
        runIn(2,powerSync)
    }
    if (state.powerOn && state.switchOff){
        runIn(2,powerSync)
    }
    if(state.heatPowerOff && ! state.fireplaceHeatOff){
        runIn(2,powerSync)
    }
    if (state.heatPowerLow && ! state.fireplaceHeatLow){
        runIn(2,powerSync)
    }
    if (state.heatPowerHigh && ! state.fireplaceHeatHigh){
        runIn(2,powerSync)
    }    
}

def powerSync(){
    if (state.powerOff && state.switchOn){
        settings.fireplace.off()
    }
    if (state.powerOn && state.switchOff){
        settings.fireplace.on()
        getTemperature()
    }
    if(state.heatPowerOff && ! state.fireplaceHeatOff){
        settings.fireplace.setHeat("Off")
    }
    if (state.heatPowerLow && ! state.fireplaceHeatLow){
        settings.fireplace.setHeat("Low")
    }
    if (state.heatPowerHigh && ! state.fireplaceHeatHigh){
        settings.fireplace.setHeat("High")
    }
} 

def turnFireplaceOn(){
    logInfo ("$app.label turning ON")
    settings.harmonyHub.deviceCommand("pwr","65347561")
}

def turnFireplaceOff(){
    settings.harmonyHub.deviceCommand("pwr","65347561")
    logInfo ("$app.label turning OFF")
}

def setHeatOff(){
    if (state.heatPowerLow){
        settings.harmonyHub.deviceCommand("low","65347561")
    }
    else if  (state.heatPowerHigh){
        settings.harmonyHub.deviceCommand("high","65347561")
    }else{
        logInfo ("$app.label Heat already set to off. no command needs to be sent")
    }
}

def setHeatLow(){
    if (! state.heatPowerLow){
        settings.harmonyHub.deviceCommand("low","65347561")
    }else{
        logInfo ("$app.label Heat already set to low. no command needs to be sent")
    }
}

def setHeatHigh(){
    if (! state.heatPowerHigh){
        settings.harmonyHub.deviceCommand("high","65347561")
    }else{
        logInfo ("$app.label Heat already set to high. no command needs to be sent")
    }
}

void logInfo(String Msg){
    if (settings?.logEnable !=false){
        log.info "$Msg"
    }
}
