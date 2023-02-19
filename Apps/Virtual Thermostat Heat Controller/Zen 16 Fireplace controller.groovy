/**
 *  ****************  Zooz Zen 16 Fireplace Controller ****************
 *
 * Virtual Thermostat Fireplace Heat Controller
 *
 *
 *  Copyright 2022 Gassgs / Gary Gassmann
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
 *  Last Update: 12/23/2022
 *
 *  Changes:
 *
 *  V1.0.0 -        2-04-2021       First run
 *  V1.1.0 -        2-19-2021       Handler and logging improvements
 *  V1.2.0 -        12-23-2022      Redo and Improvements for fireplace modes
 *  V1.3.0 -        12-24-2022      Added set mode based on outdoor temp
 *  V1.4.0 -        12-25-2022      Removed temp option, driver now built around zigbee temp/humidity sensor
 */

import groovy.transform.Field

definition(
    name: "Zooz Zen 16 Fireplace Controller ",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Zooz Zen 16 Fireplace Controller",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {

	section {

     paragraph(
         title: "Zooz Zen 16 Fireplace Controller ",
         required: false,
    	"<div style='text-align:center'><b><big>: Zooz Zen 16 Fireplace Controller :</big></b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>Zen 16 Fireplace Controller Options</b></div>"
        )
        input(
            name:"thermostat",
            type:"capability.temperatureMeasurement",
            title: "<b>Virtual Fireplace Device</b>",
            required: true,
            multiple: false,
            )
    }
    section{
        input(
            name:"powerSwitch",
            type:"capability.switch",
            title: "<b>Switch or relay</b> for - ON mode sync",
            multiple: false,
            submitOnChange: true
            )
        input(
            name:"heatElement",
            type:"capability.switch",
            title:"<b>Switch or relay</b> for - HEAT mode sync",
            multiple: false,
            submitOnChange: true
            )
        input(
            name:"heatSwitch",
            type:"capability.switch",
            title:"<b>Switch or relay</b> for - Thermostat Operating mode sync",
            multiple: false,
            submitOnChange: true
            )
    }
    section{
        input(
            name:"outdoorTemperatureSensors",
            type:"capability.temperatureMeasurement",
            title:"<b>Outdoor temperature sensors</b> to determine if heat element should turn on",
            multiple: true,
            submitOnChange: true
            )
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
    subscribe(settings.thermostat, "thermostatOperatingState", thermostatOperatingStateHandler)
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
         logInfo ("Outdoor Temp below threshold, turning on 'Heat' Mode")
         settings.thermostat.setThermostatMode("heat")          
    }else{
         logInfo ("Outdoor Temp above threshold, not turning on 'On' Mode")
         settings.thermostat.setThermostatMode("on")
    }
}

def powerSwitchHandler(evt){
    pwrSwitch = evt.value
    logInfo ("Switch $pwrSwitch")
    if (pwrSwitch == "on" && state.offMode){
        logInfo ("Switch On, Checking Outdoor Temmperature")
        getOutdoorTemperature()
    }
    else if (pwrSwitch == "off" && !state.offMode){
        logInfo ("Switch Off, setting mode to Off")
        settings.thermostat.setThermostatMode("off")
        settings.heatElement.off()
    }
}

def thermostatModeHandler(evt){
    mode = evt.value
    logInfo ("Thermostat Mode $mode")
    state.onMode = (mode == "on")
    state.heatMode = (mode == "heat")
    state.offMode = (mode == "off")
    if (state.onMode){
        logInfo ("On Mode, turning On Switch")
        settings.powerSwitch.on()
        settings.heatElement.off()
    } 
    else if (state.heatMode){
        logInfo ("Heat Mode, turning On Heat Element")
        settings.powerSwitch.on()
        settings.heatElement.on()
    }
    else if (state.offMode){
        logInfo ("Off Mode, turning Off switch")
        settings.powerSwitch.off()
        settings.heatElement.off()
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
