/**
 *  ****************  Summer Humidity Control App ****************
 *
 *  Adjusts ceiling fan speeds based on indoor humidity
 *  Control Dehumidifier based on Mode and Weather
 *
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
 *  Last Update: 12/2/22
 *
 *  Changes:
 *
 *  V1.0.0  -       2-14-2021       First run
 *  V1.1.0  -       2-15-2021       added logging
 *  V1.2.0  -       5-13-2022       Fixed turning fan speed off instead of fan light off
 *  V1.3.0  -       5-31-2022       Changed App Name Added Control for VacPlus Tasmota dehumidifier.
 *  V1.4.0  -       6-02-2022       Dehumidifier control based on weather and Mode, Added more logging
 *  V1.5.0  -       12-2-2022       Fix for new ionizer commands
 *  V1.6.0  -       12-25-2022      Name change simplified
 */

import groovy.transform.Field

definition(
    name: "Summer Humidity Control",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Adjusts ceiling fan speeds based on indoor humidity and Controls Dehumidifier based on Humidity, Mode, and Weather",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Summer Humidity Control",
        required: true,
    	"<div style='text-align:center'><b><big>Summer Humidity Control</big></b></div>"
     	)
     paragraph(
        title: "Ceiling Fan Control Options",
        required: true,
    	"<div style='text-align:center'><b>Ceiling Fan Control Options</b></div>"
     	)
    }
    section{
        input(
            name:"fans",
            type:"capability.fanControl",
            title:"<b>Ceiling fans</b> to control",
            multiple: true,
            required: true,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"switch",
            type:"capability.switch",
            title:"<b>Switch</b> to enable or disable automatic ceiling fans (optional)",
            multiple: false,
            required: false,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"humiditySensors",
            type:"capability.relativeHumidityMeasurement",
            title: "<b>Humidity sensors</b> to use for indoor humidity level",
            required: true,
            multiple: true,
            submitOnChange: true
            )
        if(humiditySensors){
            paragraph "<b>Current humidity average is ${averageHumidity()}%</b>"
        }
    }
    section{
        paragraph(
        title: "Recomended Humidity Levels",
        required: true,
    	"<div style='text-align:center'><b>Summer Recomended Humidity Levels</b></div>"
     	)
         paragraph(
        title: "Recomended Humidity Levels",
        required: true,
    	"<div style='text-align:center'><b>Ideal Humidity Levels 45% - 55%</b></div>"
     	)
         paragraph(
        title: "Recomended Humidity Levels",
        required: true,
    	"<div style='text-align:center'><b>High Humidity Levels 55% - 80%</b></div>"
             )
        input(
            name:"fanOffThreshold",
            type:"number",
            title:"Fan Off threshold -  % humidity to turn fans off",
            multiple: false,
            defaultValue:"45",
            required: true,
            submitOnChange: true
        )
        input(
            name:"fanLowThreshold",
            type:"number",
            title:"Fan Low threshold -  % humidity to keep fans on low",
            multiple: false,
            defaultValue:"55",
            required: true,
            submitOnChange: true
        )
        input(
            name:"fanNightThreshold",
            type:"number",
            title:"Fan Low threshold for Night  -  % humidity to turn keep fans on low for night",
            multiple: false,
            defaultValue:"65",
            required: true,
            submitOnChange: true
        )
        input(
            name:"humidityThreshold",
            type:"number",
            title:"% humidity to tigger next fan speed",
            multiple: false,
            defaultValue:"5",
            required: true,
            submitOnChange: true
        )
    }
    section{
        paragraph(
            title: "Summer Ceiling Fan and Dehumidifier Control App",
            required: true,
    	    "<div style='text-align:center'><b>Dehumidifier Control Options</b></div>"
     	)
        input(
            name:"dehumidifier",
            type:"capability.switch",
            title:"<b>Dehumidifier</b> to control",
            multiple: true,
            required: true,
            submitOnChange: true
        )
        input(
            name:"ionizerModes",
            type:"mode",
            title: "<b>Modes</b> to turn on Ionizer",
            required: true,
            multiple: true,
            submitOnChange: true
            )
        input(
            name:"sleepModes",
            type:"mode",
            title: "<b>Modes</b> to turn on Sleep Mode",
            required: true,
            multiple: true,
            submitOnChange: true
            )
        input(
            name:"weather",
            type:"capability.temperatureMeasurement",
            title:"<b>Weather Service</b> to pull current conditions from",
            multiple: false,
            required: true,
            submitOnChange: true
        )
        if(weather){
            paragraph "Current Weather = <b>${weatherDisplay()}</b>, Humidity = <b>${weatherHumDisplay()}</b>, Temperature = <b>${weatherTempDisplay()}</b>"
        }
        input(
            name:"rainEnable",
            type:"bool",
            title: "<b>Enable</b> to turn on raining mode anytime 'rain' is mentioned in the current weather",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        input(
            name:"basementHumiditySensors",
            type:"capability.relativeHumidityMeasurement",
            title: "<b>Humidity sensors</b> to use for basement humidity level",
            required: true,
            multiple: true,
            submitOnChange: true
            )
        if(basementHumiditySensors){
            paragraph "<b>Current humidity average is ${averageBasementHumidity()}%</b>"
        }
        input(
            name:"basementHumidityThreshold",
            type:"number",
            title:"% humidity to turn off Dehumidifier",
            multiple: false,
            defaultValue:"44",
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
	subscribe(settings.humiditySensors, "humidity",humiditySensorsHandler)
    subscribe(settings.switch, "switch",  switchHandler)
    subscribe(location, "mode", modeEventHandler)
    subscribe(settings.dehumidifier, "switch",  dehumidifierHandler)
    subscribe(settings.weather, "weather",  weatherHandler)
    subscribe(settings.basementHumiditySensors, "humidity", basementHumiditySensorsHandler)
    getHumidity()
    logInfo ("subscribed to sensor events")
}

def weatherDisplay(){
	def currentWeather = settings.weather.currentValue ("weather")
	return currentWeather
}
def weatherHumDisplay(){
	def currentHumWeather = settings.weather.currentValue ("humidity")
	return currentHumWeather
}
def weatherTempDisplay(){
	def currentTempWeather = settings.weather.currentValue ("temperature")
	return currentTempWeather
}

def weatherHandler(evt){
    currentWeather = evt.value
    logInfo ("$app.label Current weather is $currentWeather")
    if (currentWeather.contains("rain")){
        state.rain = true
        logInfo ("$app.label Current weather is Rainy")
    }else{
        state.rain = false
    }
    if (rainEnable){
        if (state.rain && !state.sleep){
            logInfo ("$app.label setting Dehumidifier Mode to Raining")
            settings.dehumidifier.setMode("raining")
        }
        else if (!state.rain && !state.sleep){
            logInfo ("$app.label setting Dehumidifier Mode to Standard")
            settings.dehumidifier.setMode("standard")
        }
    }
}

def dehumidifierHandler(evt){
    status = evt.value
    if (status == "on"){
        state.dehumidifierOn = true
    }else{
        state.dehumidifierOn = false
    }
}

def basementHumiditySensorsHandler(evt){
    getBasementHumidity()
}

def averageBasementHumidity(){
	def total = 0
    def n = settings.basementHumiditySensors.size()
	settings.basementHumiditySensors.each {total += it.currentHumidity}
	return (total /n).toDouble().round(1)
}

def getBasementHumidity(){
	def humidityAvg = averageHumidity()
	logInfo ("Current basement humidity average is ${averageHumidity()}")
    basementHumidityHandler()
}

def basementHumidityHandler(){
    humidityAvg = averageBasementHumidity()
    if (humidityAvg <= settings.basementHumidityThreshold){
        if (state.dehumidifierOn){
            logInfo ("Current basement humidity average is low, turning off dehumidifier")
            settings.dehumidifier.off()
        }else{
            logInfo ("Current basement humidity average is low, dehumidifier is already off")
        }
    }else{
        if (!state.dehumidifierOn){
            logInfo ("Current basement humidity average is high, turning on dehumidifier")
            settings.dehumidifier.on()
        }else{
            logInfo ("Current basement humidity average is high, dehumidifier is already on")
        }
    }
}

def modeEventHandler(evt){
    mode = evt.value as String
    state.night = (mode == "Night")
    ionizerList = ionizerModes as String
    sleepList = sleepModes as String
    if (ionizerList.contains ("$mode")){
        logInfo ("ionizer modes - $ionizerModes  current mode -  $mode")
        state.ionizer = true
        logInfo ("$app.label setting Dehumidifier Ionizer to ON")
        settings.dehumidifier.ionizer(on)
    }else{
        logInfo ("ionizer modes - $ionizerModes  current mode -  $mode")
        state.ionizer = false
        logInfo ("$app.label setting Dehumidifier Ionizer to OFF")
        settings.dehumidifier.ionizer(off)
    }
    if (sleepList.contains ("$mode")){
        logInfo ("sleep modes - $ionizerModes  current mode -  $mode")
        state.sleep = true
        logInfo ("$app.label setting Dehumidifier Mode to Sleep")
        settings.dehumidifier.setMode("sleep")
    }else{
        logInfo ("sleep modes - $ionizerModes  current mode -  $mode")
        state.sleep = false
        if (rainEnable){
            if (state.rain){
                logInfo ("$app.label setting Dehumidifier Mode to Raining")
                settings.dehumidifier.setMode("raining")
            }else{
                logInfo ("$app.label setting Dehumidifier Mode to Standard")
                settings.dehumidifier.setMode("standard")
            }
        }else{
            logInfo ("$app.label setting Dehumidifier Mode to Standard")
            settings.dehumidifer.setMode("standard")
        }
    }
    humidityHandler()
    logInfo ("mode status $mode")
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
    humidityHandler()
}

def switchHandler(evt){
    switchStatus = evt.value
    logInfo ("Automatic fan switch $switchStatus")
    state.switchOn = (switchStatus == "on")
    state.switchOff = (switchStatus == "off")
    humidityHandler()
}

def humidityHandler(){
    if(settings.switch){
        logInfo ("Checking Switch settings")
        humiditySwitchHandler()
    }
    else{
        humidityNoSwitchHandler()
    }
}
def humidityNoSwitchHandler(){
    if (state.night){
        logInfo ("Running Night Humidity Fans")
        humidityNight()
    }
    else{
        logInfo ("Running Day Humidity Fans")
        humidityDay()
    }
}

def humiditySwitchHandler(){
    if (state.switchOn&&state.night){
        logInfo ("Running Night Humidity Fans")
        humidityNight()
    }
    else if (state.switchOn){
        logInfo ("Running Day Humidity Fans")
        humidityDay()
    }
    else if (state.switchOff){
        logInfo ("Switch Off not running Humidity Fans")
    }
}

def humidityDay(){
    humidityAvg = averageHumidity()
    state.fansOff = (humidityAvg <= fanOffThreshold)
    state.fansLow = (humidityAvg > fanOffThreshold&&humidityAvg<=fanLowThreshold)
    state.fansMediumLow = (humidityAvg > fanLowThreshold&&humidityAvg <= fanLowThreshold+humidityThreshold)
    state.fansMedium = (humidityAvg > fanLowThreshold+humidityThreshold&&humidityAvg <= fanLowThreshold+humidityThreshold*2)
    state.fansAuto = (humidityAvg > fanLowThreshold+humidityThreshold*2&&humidityAvg <= fanLowThreshold+humidityThreshold*3)
    state.fansHigh = (humidityAvg > fanLowThreshold+humidityThreshold*3)
    if (state.fansOff){
        logInfo ("Turning Fans Off")
        settings.fans.setSpeed("off")
    }
    if (state.fansLow){
        logInfo ("Setting Fans to Low")
        settings.fans.setSpeed("low")
    }
    if (state.fansMediumLow){
        logInfo ("Setting Fans to Medium")
        settings.fans.setSpeed("medium")
    }
    if (state.fansMedium){
        logInfo ("Setting Fans to Medium-High")
        settings.fans.setSpeed("medium-high")
    }
    if (state.fansAuto){
        logInfo ("Setting Fans to Auto")
        settings.fans.setSpeed("auto")
    }
    if (state.fansHigh){
        logInfo ("Setting Fans to High")
        settings.fans.setSpeed("high")
    }
}

def humidityNight(){
    humidityAvg = averageHumidity()
    state.fansOff = (humidityAvg <= fanOffThreshold)
    state.fansLow = (humidityAvg > fanOffThreshold&&humidityAvg <= fanNightThreshold)
    state.fansMediumLow = (humidityAvg > fanNightThreshold&&humidityAvg <= fanNightThreshold+humidityThreshold)
    state.fansMedium = (humidityAvg > fanNightThreshold+humidityThreshold&&humidityAvg <= fanNightThreshold+humidityThreshold*2)
    state.fansAuto = (humidityAvg > fanNightThreshold+humidityThreshold*2&&humidityAvg <= fanNightThreshold+humidityThreshold*3)
    state.fansHigh = (humidityAvg > fanNightThreshold+humidityThreshold*3)
    if (state.fansOff){
        logInfo ("Turning Fans Off")
        settings.fans.setSpeed("off")
    }
    if (state.fansLow){
        logInfo ("Setting Fans to Low")
        settings.fans.setSpeed("low")
    }
    if (state.fansMediumLow){
        logInfo ("Setting Fans to Medium")
        settings.fans.setSpeed("medium")
    }
    if (state.fansMedium){
        logInfo ("Setting Fans to Medium-High")
        settings.fans.setSpeed("medium-high")
    }
    if (state.fansAuto){
        logInfo ("Setting Fans to Auto")
        settings.fans.setSpeed("auto")
    }
    if (state.fansHigh){
        logInfo ("Setting Fans to High")
        settings.fans.setSpeed("high")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
