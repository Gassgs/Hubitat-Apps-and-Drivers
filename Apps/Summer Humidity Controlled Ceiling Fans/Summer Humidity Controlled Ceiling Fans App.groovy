/**
 *  ****************  Summer Humidity Controlled Ceiling Fans App ****************
 *
 *  Adjusts ceiling fan speeds based on indoor humidity

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
 *  Last Update: 2/14/2021
 *
 *  Changes:
 *
 *  V1.0.0  -       2-14-2021       First run
 *  V1.1.0  -       2-15-2021       added logging
 */

import groovy.transform.Field

definition(
    name: "Summer Humidity Controlled Ceiling Fans",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Adjusts ceiling fan speeds based on indoor humidity",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Summer Humidity Controlled Ceiling Fans",
        required: true,
    	"<div style='text-align:center'><b>Summer Humidity Controlled Ceiling Fans</b></div>"
     	)
     paragraph(
        title: "Summer Humidity Controlled Ceiling Fans",
        required: true,
    	"<div style='text-align:center'>Summer Humidity Controlled Ceiling Fans Options</div>"
     	)
    }
    section{
        input(
            name:"fans",
            type:"capability.fanControl",
            title:"Ceiling fans to control",
            multiple: true,
            required: true,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"switch",
            type:"capability.switch",
            title:"Switch to enable or disable automatic ceiling fans (optional)",
            multiple: false,
            required: false,
            submitOnChange: true
        )
        }
    section{
        input(
            name:"humiditySensors",
            type:"capability.relativeHumidityMeasurement",
            title: "Humidity sensor(s) to use for indoor humidity level",
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
    getHumidity()
    logInfo ("subscribed to sensor events")
}

def modeEventHandler(evt){
    mode = evt.value
    state.night = (mode == "Night")
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
        settings.fans.off()
    }
    if (state.fansLow){
        logInfo ("Setting Fans to Low")
        settings.fans.setSpeed("low")
    }
    if (state.fansMediumLow){
        logInfo ("Setting Fans to Medium-Low")
        settings.fans.setSpeed("medium-low")
    }
    if (state.fansMedium){
        logInfo ("Setting Fans to Medium")
        settings.fans.setSpeed("medium")
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
        settings.fans.off()
    }
    if (state.fansLow){
        logInfo ("Setting Fans to Low")
        settings.fans.setSpeed("low")
    }
    if (state.fansMediumLow){
        logInfo ("Setting Fans to Medium-Low")
        settings.fans.setSpeed("medium-low")
    }
    if (state.fansMedium){
        logInfo ("Setting Fans to Medium")
        settings.fans.setSpeed("medium")
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
