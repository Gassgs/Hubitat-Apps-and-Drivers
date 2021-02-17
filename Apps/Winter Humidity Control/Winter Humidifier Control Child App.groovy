/**
 *  ****************  Winter Humidifier Control Child App ****************
 *
 *  Calculates target humidity based on an outdoor temperature sensor
 *  Control a switch or relay when comparing humidity sensors in the home.
 *  Option for motion activity restriction.
 *   
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
 *  V1.0.0  -        1-29-2021      First run
 *  V1.2.0  -        1-31-2021      Improvements
 *  V1.3.0  -        2-08-2021      handler improvements
 *  V1.4.0  -        2-16-2021      handler logic improvements
 */

import groovy.transform.Field

definition(
    name: "Winter Humidifier Control Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Calculates target humidity based on an outdoor temperature sensor"+
    " Control a switch or relay when humidity sensors report lower than tartget in the home."+
    " Optional motion activity restriction.",
    parent: "Gassgs:Winter Humidity Control",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Winter Humidifier Control Child",
        required: true,
    	"<div style='text-align:center'><b><big>Winter Humidifier Control App</b></big></div>"
     	)
     paragraph(
        title: "Winter Humidifier Control Child",
        required: true,
    	"<div style='text-align:center'><b>Humidifier Control Options</b></div>"
     	)
        input(
            name:"humidifier",
            type:"capability.switch",
            title: "Humidifier (relay, switch, or outlet) to control",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        input(
            name:"temperatureSensors",
            type:"capability.temperatureMeasurement",
            title: "Outdoor temperature sensor(s) that will calculate  -target humidity-",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        if(temperatureSensors){
            paragraph "<b>Current target humidity is ${calculateTarget()}% with an outdoor temperature of ${averageTemperature()}</b>"
        }
        input(
            name:"minRh",
            type:"decimal",
            title: "The minimum target relative humidity allowed",
            defaultValue: 20,
            submitOnChange: true
        )
        input(
            name:"maxRh",
            type:"decimal",
            title:"The maximum target relative humidity allowed",
            defaultValue: 50,
            submitOnChange: true
        )
        input(
            name:"frostCorrection",
            type:"decimal",
            title:"Target offset, If you see condensation on your windows set to a lower number",
            defaultValue: -3,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"humiditySensors",
            type:"capability.relativeHumidityMeasurement",
            title: "Indoor humidity sensor(s) to compare to -target humidity-",
            required: true,
            multiple: true,
            submitOnChange: true
            )
        if(humiditySensors){
            paragraph "<b>Current humidity average is ${averageHumidity()}%</b>"
        }
    }
    section{
        input(
            name:"motionSensor",
            type:"capability.motionSensor",
            title:"Motion sensor or zone to disable humidistat from running",
            multiple: false,
            required: true,
            submitOnChange: true
        )  
    }
    section{
        input(
            name:"timeout",
            type:"number",
            title:"Timeout in seconds after motion becomes inactive",
            required: true,
            defaultValue: 0,
            submitOnChange: true
        )  
    }
    section{
        paragraph(
        title: "Mode information",
        required: true,
    	"<div style='text-align:center'><b>*Humidifer will not run in late evening, night, and away modes*</b></div>"
     	)
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
    subscribe(settings.temperatureSensors, "temperature", temperatureSensorsHandler)
	subscribe(settings.humiditySensors, "humidity",humiditySensorsHandler)
    subscribe(settings.motionSensor, "motion",  motionHandler)
    subscribe(settings.humidifier, "switch",  humidifierSwitchHandler)
    subscribe(location, "mode", modeEventHandler)
    humidifierHandler()
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
    logInfo ("mode status $mode")
    if (state.lateEvening||state.night||state.away){
        state.modeOk = false
        logInfo ("mode not ok turning humidifer Off")
        checkModeMotion()
    }else{
        state.modeOk = true
        logInfo ("mode ok checking humidifer status")
        humidifierHandler()
    }
}

def temperatureSensorsHandler(evt){
    getTemperature()
}

def averageTemperature(){
	def total = 0
    def n = settings.temperatureSensors.size()
	settings.temperatureSensors.each {total += it.currentTemperature}
	return (total /n).toDouble().round(1)
}

def getTemperature(){
	def avg = averageTemperature()
	logInfo ("Current temperature average is ${averageTemperature()}")
    humidifierHandler()
}

def calculateTarget() {
	def double outdoorTemp = averageTemperature()
	def target = Math.max(Math.min((double)frostCorrection+27.7+0.535*outdoorTemp - 0.00409 * Math.pow(outdoorTemp, 2), (double)maxRh), (double)minRh)
    logInfo ( "Calculated a target humidity of ${target.round(1)}% with an outdoor temperature of ${outdoorTemp}")
    return Math.round(target*10)/10
}

def humiditySensorsHandler(evt){
    getHumidity()
}

def averageHumidity(){
	def total = 0
    def n = settings.humiditySensors.size()
	settings.humiditySensors.each {total += it.currentHumidity}
	return (total /n).toDouble().round(1)
}

def getHumidity(){
	def humidityAvg = averageHumidity()
	logInfo ("Current humidity average is ${averageHumidity()}")
    humidifierHandler()
}

def motionHandler(evt){
    getMotion()
}
def getMotion(){
	def active = settings.motionSensor.findAll { it?.latestValue("motion") == 'active' }
		if (active){
		    unschedule(motionInactive)
            checkModeMotion()
            state.motionInactive = false
            motionList = "${active}"
            logInfo("motionActive"+motionList)
        }
    else{
       runIn(timeout,motionInactive)
    }
}
def motionInactive(){
    state.motionInactive = true
    logInfo("All Inactive")
    humidifierHandler()
}

def checkModeMotion(){
    if (state.humidifierSwitchOn){
        logInfo("turning humidifer off due to mode or motion")
        settings.humidifier.off()
    }
    else{
        logInfo("humidifer already off")
    }
}

def humidifierSwitchHandler(evt){
    switchStatus = evt.value
    logInfo ("humidifier switch $evt.value")
    state.humidifierSwitchOn = (evt.value == "on")
    state.humidifierSwitchOff = (evt.value == "off")
}

def humidifierHandler(){
    humidityAvg = averageHumidity()
    target = calculateTarget()
    state.lowHumidity = (humidityAvg < target-1)
    state.goodHumidity = (humidityAvg >= target)
    if (state.lowHumidity&&state.humidifierSwitchOff){
        logInfo ("humidity below threshold checking to turn on")
        humidifierOn()
    }
    else if (state.lowHumidity&&state.humidifierSwitchOn){
        logInfo ("humidity below threshold humidifier already on")
    }
    else if (state.goodhumidity&&state.humidifierSwitchOn){
        logInfo ("humidity Ok turning humidifier off")
        settings.humidifier.off()
    }
    else{
        logInfo ("humidity Ok humidifier already off")
    }
}

def humidifierOn(){
    if (state.motionInactive&&state.modeOk){
        logInfo ("mode and motion ok, turn hmidifier on")
        settings.humidifier.on()
    }
    else{
        logInfo ("mode or motion preventing humidifier from turning on")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
