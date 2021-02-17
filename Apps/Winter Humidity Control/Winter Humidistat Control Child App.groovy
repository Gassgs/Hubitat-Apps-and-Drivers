/**
 *  ****************  Winter Humidistat Control Child App ****************
 *
 *  Calculates target humidity based on an outdoor temperature sensor
 *  Control a switch or relay when comparing humidity sensors in the home.
 *  Option to control ceiling fans when humidity is high.
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
 *  V1.3.0  -        2-06-2021      added low and medium low options for fans
 *  V1.4.0  -        2-08-2021      handler improvements
 *  V1.5.0  -        2-16-2021      handler logic improvements and added humidity timeout
 */

import groovy.transform.Field

definition(
    name: "Winter Humidistat Control Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Calculates target humidity based on an outdoor temperature sensor"+
    " Control a switch or relay when humidity sensors report lower than tartget in the home."+
    " Options to control ceiling fans on when humidity is high",
    parent: "Gassgs:Winter Humidity Control",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Winter Humidistat Control Child",
        required: true,
    	"<div style='text-align:center'><big><b>Winter Humidistat Control App</b></big></div>"
     	)
     paragraph(
        title: "Winter Humidistat Control Child",
        required: true,
    	"<div style='text-align:center'><b>Humidistat Control Options</b></div>"
     	)
        input(
            name:"humidistat",
            type:"capability.switch",
            title: "Humidistat (relay, switch, or outlet) to control",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        input(
            name:"thermostat",
            type:"capability.thermostat",
            title: "Main household HVAC thermostat",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        input(
            name:"humidityTimeout",
            type:"number",
            title: "Number of minutes to add humidity while HVAC is heating",
            defaultValue: 10,
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
            name:"fans",
            type:"capability.fanControl",
            title:"Ceiling fans to control when humidity is higher than -target humidity-",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        input(
            name:"fanThresholdLow",
            type:"number",
            title:"Fan threshold - % above target humidity to turn fans on low",
            multiple: false,
            defaultValue:"0",
            required: true,
            submitOnChange: true
        )
        input(
            name:"fanThresholdMed",
            type:"number",
            title:"Fan threshold - % above target humidity to turn fans on medium-low",
            multiple: false,
            defaultValue:"2",
            required: true,
            submitOnChange: true
        )
        input(
            name:"fanThresholdNight",
            type:"number",
            title:"Fan threshold for Night Mode - % above target humidity to turn fans on medium-low",
            multiple: false,
            defaultValue:"4",
            required: true,
            submitOnChange: true
        )
        input(
            name:"fanSwitch",
            type:"capability.switch",
            title:"Switch to enable or disable automatic ceiling fans",
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
    subscribe(settings.temperatureSensors, "temperature", temperatureSensorsHandler)
	subscribe(settings.humiditySensors, "humidity",humiditySensorsHandler)
    subscribe(settings.humidistat, "switch",  humidistatSwitchHandler)
    subscribe(settings.thermostat, "thermostatOperatingState", thermostatOperatingStateHandler)
    subscribe(settings.fanSwitch, "switch",  fanSwitchHandler)
    subscribe(location, "mode", modeEventHandler)
    humidistatHandler()
    logInfo ("subscribed to sensor events")
}

def modeEventHandler(evt){
    mode = evt.value
    state.nightMode = (mode == "Night")
    logInfo ("mode status $mode")
    humidistatHandler()
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
    humidistatHandler()
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
    humidistatHandler()
}

def fanSwitchHandler(evt){
    switchStatus = evt.value
    logInfo ("Automatic fan switch $switchStatus")
    state.fanSwitchOn = (switchStatus == "on")
    state.fanSwitchOff = (switchStatus == "off")
    humidistatHandler()
}

def humidistatSwitchHandler(evt){
    humidistatSwitch = evt.value
    logInfo ("Humidistat $humidistatSwitch")
    state.humidistatSwitchOn = (humidistatSwitch == "on")
    state.humidistatSwitchOff = (humidistatSwitch == "off")
}

def humidistatHandler(){
    humidityAvg = averageHumidity()
    target = calculateTarget()
    thresholdLow = (target + fanThresholdLow)
    thresholdMed = (target + fanThresholdMed)
    thresholdNight = (target + fanThresholdNight)
    state.lowHumidity = (humidityAvg < target-1)
    state.goodHumidity = (humidityAvg >= target)
    state.fanOffHumidity = (humidityAvg < thresholdLow)
    state.fanLowHumidity = (humidityAvg >= thresholdLow&&humidityAvg < thresholdMed)
    state.fanLowNightHumidity = (humidityAvg >= thresholdLow&&humidityAvg < thresholdNight)
    state.fanMedHumidity = (humidityAvg >= thresholdMed)
    state.fanMedNightHumidity = (humidityAvg >= thresholdNight)
    if (state.nightMode&&state.fanSwitchOn){
        logInfo ("running night hunidistat fan handler")
        humidistatNoFans()
        humidistatNightFans()
    }
    else if (state.fanSwitchOn){
        logInfo ("running day hunidistat fan handler")
        humidistatNoFans()
        humidistatFans()
    }
    else if (state.fanSwitchOff){
        logInfo ("Auto fan switch off running humidistat handler")
        humidistatNoFans()
    }
}

def humidistatNoFans(){
    if (state.lowHumidity&&state.humidistatSwitchOff){
        logInfo ("conditions are met setting humidistat to ready state")
        state.humidistatReady = true
    }
    else if (state.lowHumidity&&state.humidistatSwitchOn){
        logInfo ("conditions are met humidistat is already ON")
    }
    else if (state.goodHumidity&&state.humidistatSwitchOn){
        logInfo ("Humidity is on target - turning humidistat OFF")
        state.humidistatReady = false
        settings.humidistat.off()
    }
    else if (state.goodHumidity&&state.humidistatSwitchOff){
        logInfo ("Humidity is on target humidistat is already OFF")
        state.humidistatReady = false
    }
}

def humidistatFans(){
    if (state.fanOffHumidity){
        logInfo ("Humidity is below target plus threshold - turning Fans OFF")
        settings.fans.setSpeed("off")
    }
    if (state.fanLowHumidity){
        logInfo ("Humidity is above target plus low threshold - turning Fans ON low")
        settings.fans.setSpeed("low")
    }
    if (state.fanMedHumidity){
        logInfo ("Humidity is above target plus medium-low threshold - turning Fans ON medium-low")
        settings.fans.setSpeed("medium-low")
    }   
}

def humidistatNightFans(){
    if (state.fanOffHumidity){
        logInfo ("Humidity is below target plus threshold - turning Fans OFF")
        settings.fans.setSpeed("off")
    }
    if (state.fanLowNightHumidity){
        logInfo ("Humidity is above target plus low threshold - turning Fans ON low")
        settings.fans.setSpeed("low")
    }
    if (state.fanMedNightHumidity){
        logInfo ("Humidity is above target plus medium-low Night threshold - turning Fans ON medium-low")
        settings.fans.setSpeed("medium-low")
    }   
}

def thermostatOperatingStateHandler(evt){
    status = evt.value
    logInfo ("thermostat operating state $status")
    state.idle = (status == "idle")
    state.heating = (status == "heating")
    if (state.heating&&state.humidistatReady){
        logInfo ("HVAC running with humidistat on")
        settings.humidistat.on()
        runIn(humidityTimeout*60,humidistatOff)
    }
    else if (state.heating&&state.humidistatSwitchOff){
        logInfo ("HVAC running, humidity not needed")
    }
}

def humidistatOff(){
    logInfo ("humidity timeout expired, turning humidistat off")
    settings.humidistat.off()
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
