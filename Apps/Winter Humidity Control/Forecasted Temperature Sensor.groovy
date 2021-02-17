/**
 *  ****************  Forecasted Temperature Sensor ****************
 *
 * Forecasted Temperature Sensor used to average with outdoor temperature sensors
 * to control humidity. works with open weather maps 3 day forecast 
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
 *  
 */

import groovy.transform.Field

definition(
    name: "Forecasted Temperature Sensor",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Forecasted Temperature Sensor app",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {

	section {

     paragraph(
         title: "Forecasted Temperature Sensor",
         required: false,
    	"<div style='text-align:center'><b>:Forecasted Temperature Sensor:</b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>Forecasted Temperature Sensor options</b></div>"
        )
        input(
            name:"tempDevice",
            type:"capability.temperatureMeasurement",
            title: "<b>-Virtual temperature Sensor-</b>",
            required: true,
            submitOnChange: true
            )
    }
    section{
        input(
            name:"openWeatherMap",
            type:"capability.temperatureMeasurement",
            title: "Open Weater Map Device",
            multiple: false,
            required: true,
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
    subscribe(settings.openWeatherMap, "forecastMorn", forecastMornHandler)
    subscribe(settings.openWeatherMap, "forecastDay", forecastDayHandler)
    subscribe(settings.openWeatherMap, "forecastEve", forecastEveHandler)
    subscribe(settings.openWeatherMap, "forecastNight", forecastNightHandler)
    subscribe(settings.openWeatherMap, "forecastLow", forecastLowHandler)
    subscribe(location, "mode", modeEventHandler)
    logInfo ("subscribed to sensor events")
}

def modeEventHandler(evt){
    mode = evt.value
    state.earlyMorning = (mode == "Early_morning")
    state.day = (mode == "Day")
    state.afternoon = (mode == "Afternoon")
    state.dinner = (mode == "Dinner")
    state.evening = (mode == "Evening")
    state.lateEvening = (mode == "Late_evening")
    state.night = (mode == "Night")
    state.away = (mode == "Away")
}

def forecastDayHandler(evt){
    dayTemp = evt.value
    if (state.earlyMorning){
        settings.tempDevice.setTemperature("$dayTemp")
        logInfo ("mode is early morning setting to day forcasted temperature $dayTemp")
    }
    if (state.day){
        settings.tempDevice.setTemperature("$dayTemp")
         logInfo ("mode is day setting to day forcasted temperature $dayTemp")
    }
}

def forecastEveHandler(evt){
    eveTemp = evt.value
    if (state.afternoon){
        settings.tempDevice.setTemperature("$eveTemp")
        logInfo ("mode is afternoon setting to evening forcasted temperature $eveTemp")
    }
    if (state.dinner){
        settings.tempDevice.setTemperature("$eveTemp")
        logInfo ("mode is dinner setting to evening forcasted temperature $eveTemp")
    }
}

def forecastNightHandler(evt){
    nightTemp = evt.value
    if (state.evening){
        settings.tempDevice.setTemperature("$nightTemp")
        logInfo ("mode is evening setting to night forcasted temperature $nightTemp")
    }
    if (state.lateEvening){
        settings.tempDevice.setTemperature("$nightTemp")
        logInfo ("mode is late evening setting to night forcasted temperature $nightTemp")
    }
}

def forecastMornHandler(evt){
    mornTemp = evt.value
    if (state.night){
        settings.tempDevice.setTemperature("$mornTemp")
        logInfo ("mode is night setting to morning forcasted temperature $mornTemp")
    }
}

def forecastLowHandler(evt){
    lowTemp = evt.value
    if (state.away){
        settings.tempDevice.setTemperature("$lowTemp")
        logInfo ("mode is away setting to low forcasted temperature $lowTemp")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}

