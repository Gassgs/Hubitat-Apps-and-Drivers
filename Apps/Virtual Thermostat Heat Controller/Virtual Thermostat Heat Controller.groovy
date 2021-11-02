/**
 *  ****************  Virtual Thermostat Heat Controller ****************
 *
 * Virtual Thermostat Heat Controller.
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
 *  V1.0.0 -        2-04-2021       First run
 *  V1.1.0 -        2-19-2021       Handler and logging improvements
 */

import groovy.transform.Field

definition(
    name: "Virtual Thermostat Heat Controller",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Virtual Thermostat Heat Controller",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {

	section {

     paragraph(
         title: "Virtual Thermostat Heat Controller",
         required: false,
    	"<div style='text-align:center'><b><big>: Virtual Thermostat Heat Controller :</big></b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>Virtual Thermostat Heat Controller Options</b></div>"
        )
        input(
            name:"thermostat",
            type:"capability.thermostat",
            title: "<b>Virtual thermostat Device</b>",
            required: true,
            multiple: false,
            )
    }
    section{
        input(
            name:"temperatureSensors",
            type:"capability.temperatureMeasurement",
            title:"<b>Temperature sensors</b> for current temperature",
            multiple: true,
            submitOnChange: true
            )
        if(temperatureSensors){
            paragraph "<b>Current temperature  is ${averageTemperature()}</b>"
        }
    }
    section{
        input(
            name:"powerSwitch",
            type:"capability.switch",
            title: "<b>Switch or relay</b> for -power on/heat mode- power off/off mode sync",
            multiple: false,
            submitOnChange: true
            )
    }
    section{
        input(
            name:"heatingEnable",
            type:"bool",
            title: "Enable for heating control options",
            defaultValue: false,
            submitOnChange: true
            )
    }
    section{
        if(heatingEnable){
        input(
            name:"heatSwitch",
            type:"capability.switch",
            title:"<b>Switch or relay</b> that responds to thermostat operating mode -heating & idle (optional)",
            multiple: false,
            submitOnChange: true
            )
        input(
            name:"heatElement",
            type:"capability.switch",
            title:"<b>Switch or relay</b> that controls heating element, enable based on outdoor temp (optional)",
            multiple: false,
            submitOnChange: true
            )
        }
        if(heatElement){
        input(
            name:"outdoorTemperatureSensors",
            type:"capability.temperatureMeasurement",
            title:"<b>Outdoor temperature sensors</b> to determine if heat element should turn on",
            multiple: true,
            submitOnChange: true
            )
        }
        if(outdoorTemperatureSensors){
            paragraph "<b>Current outdoor temperature  is ${averageOutdoorTemperature()}</b>"
        input(
            name:"tempThreshold",
            type:"number",
            title:"Temperature threshold to turn heat element on",
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
    subscribe(settings.powerSwitch, "switch", powerSwitchHandler)
    subscribe(settings.thermostat, "thermostatMode", thermostatModeHandler)
    subscribe(settings.temperatureSensors, "temperature", temperatureHandler)
    getTemperature()
    logInfo ("subscribed to Events")
    if(heatSwitch){
        subscribe(settings.thermostat, "thermostatOperatingState", thermostatOperatingStateHandler)
        }
}

def powerSwitchHandler(evt){
    pwrSwitch = evt.value
    logInfo ("switch $pwrSwitch")
    state.powerSwitchOn = (pwrSwitch == "on")
    state.powerSwitchOff = (pwrSwitch == "off")
    if (settings.heatElement){
        powerHeatElement()
    }
    else{
        powerSwitch()
    }
}

def powerSwitch(){
    if (state.powerSwitchOn&&state.offMode){
        logInfo ("Switch On, setting mode to Heat")
        settings.thermostat.setThermostatMode("heat")
    }
    else if (state.powerSwitchOn&&state.heatMode){
        logInfo ("Switch On, mode is already set to Heat")
    }
    else if (state.powerSwitchOff&&state.heatMode){
        logInfo ("Switch Off, setting mode to Off")
        settings.thermostat.setThermostatMode("off")  
    }
    else if (state.powerSwitchOff&&state.offMode){
        logInfo ("Switch Off, mode is already set to Off") 
    }
}

def powerHeatElement(){
    if (state.powerSwitchOn&&state.offMode){
        logInfo ("Switch On, setting mode to Heat, checking outdoor temperature")
        settings.thermostat.setThermostatMode("heat")
        getOutdoorTemperature()
    }
    else if (state.powerSwitchOn&&state.heatMode){
        logInfo ("Switch On, mode is already set to Heat")
    }
    else if (state.powerSwitchOff&&state.heatMode){
        logInfo ("Switch Off, setting mode to Off, turning off heat element")
        settings.thermostat.setThermostatMode("off")
        settings.heatElement.off()
    }
    else if (state.powerSwitchOff&&state.offMode){
        logInfo ("Switch Off, mode is already set to Off")
    }
}

def averageTemperature(){
	def total = 0
    def n = settings.temperatureSensors.size()
	settings.temperatureSensors.each {total += it.currentTemperature}
	return (total /n).toDouble().round(1)
}

def temperatureHandler(evt){
    getTemperature()
}

def getTemperature(){
	def avg = averageTemperature()
	logInfo ("Current temperature average is ${averageTemperature()}")
    settings.thermostat.setTemperature(avg)
}

def averageOutdoorTemperature(){
	def total = 0
    def n = settings.outdoorTemperatureSensors.size()
	settings.outdoorTemperatureSensors.each {total += it.currentTemperature}
	return (total /n).toDouble().round(1)
}

def getOutdoorTemperature(){
	def avg = averageOutdoorTemperature()
	logInfo ("Current outdoor temperature average is ${averageOutdoorTemperature()}")
     if (tempThreshold>avg){
            logInfo ("outdoor Temp below threshold, turning heat element on")
            settings.heatElement.on()
    }
    else{
        logInfo ("outdoor Temp above threshold, not turning heat element on")
    }
}

def thermostatModeHandler(evt){
    mode = evt.value
    logInfo ("Thermostat Mode $mode")
    state.heatMode = (mode == "heat")
    state.offMode = (mode == "off")
    if (settings.heatElement){
        statModeHeatElement()
    }
    else{
        statMode()
    }
}

def statMode(){
    if (state.heatMode&&state.powerSwitchOff){
        logInfo ("Heat Mode, turning On switch")
        settings.powerSwitch.on()
    }
    else if (state.heatMode&&state.powerSwitchOn){
        logInfo ("Heat Mode, switch already On")
    }
    else if (state.offMode&&state.powerSwitchOn){
        logInfo ("Off Mode, turning Off switch")
        settings.powerSwitch.off()
    }
    else if (state.offMode&&state.powerSwitchOff){
        logInfo ("Off Mode, switch already Off")
    }
}

def statModeHeatElement(){
    if (state.heatMode&&state.powerSwitchOff){
        logInfo ("Heat Mode, turning On switch, checking outdoor temperature")
        settings.powerSwitch.on()
        getOutdoorTemperature()
    }
    else if (state.heatMode&&state.powerSwitchOn){
        logInfo ("Heat Mode, switch already On")
    }
    else if (state.offMode&&state.powerSwitchOn){
        logInfo ("Off Mode, turning Off switch and heat element")
        settings.powerSwitch.off()
        settings.heatElement.off()
    }
    else if (state.offMode&&state.powerSwitchOff){
        logInfo ("Off Mode, switch already Off")
    }
}

def thermostatOperatingStateHandler(evt){
    status = evt.value
    logInfo ("thermostat operating state $status")
    state.idle = (status == "idle")
    state.heating = (status == "heating")
    if (state.heating){
        logInfo ("thermostat operating state $status turning relay On")
        settings.heatSwitch.on()
    }
    if (state.idle){
        logInfo ("thermostat operating state $status turning relay Off")
        settings.heatSwitch.off()
    }
}

void logInfo(String Msg){
    if (settings?.logEnable !=false){
        log.info "$Msg"
    }
}
