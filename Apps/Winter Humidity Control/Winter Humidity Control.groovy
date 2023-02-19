/**
 *  ****************  Winter Humidity Control App ****************
 *
 *  Calculates target humidity based on an outdoor temperature sensor
 *  Control a switch or relay when comparing humidity sensors in the home.
 *  Option to control ceiling fans when humidity is high.
 *  Set humidity setpoint on smart humidifier
 *   
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
 *  Last Update: 12/29/2022
 *
 *  Changes:
 *
 *  V1.0.0  -        1-29-2021      First run
 *  V1.2.0  -        1-31-2021      Improvements
 *  V1.3.0  -        2-06-2021      added low and medium low options for fans
 *  V1.4.0  -        2-08-2021      handler improvements
 *  V1.5.0  -        2-16-2021      handler logic improvements and added humidity timeout
 *  V1.6.0  -        11-10-2021     Format and logging improvements
 *  V1.7.0  -        11-14-2021     fixed initializing values
 *  V1.8.0  -        12-28-2022     removed parent/child structure. All features now in one app.
 *  V1.9.0  -        12-29-2022     Added set humidity setpoint for smart humidifier, also night light and display settings by mode
 */

import groovy.transform.Field

definition(
    name: "Winter Humidity Control",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Calculates target humidity based on an outdoor temperature sensor"+
    " Control a switch or relay when humidity sensors report lower than tartget in the home."+
    " Options to control ceiling fans on when humidity is high - supports humidity setpoint",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Winter Humidity Control",
        required: true,
    	"<div style='text-align:center'><big><b>Winter Humidity Control</b></big></div>"
     	)
     paragraph(
        title: "Humidistat Control Options",
        required: true,
    	"<div style='text-align:center'><b>Humidistat Control Options</b></div>"
     	)
        input(
            name:"humidistat",
            type:"capability.switch",
            title: "<b>Humidistat</b> (relay, switch, or outlet) to control",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        input(
            name:"thermostat",
            type:"capability.thermostat",
            title: "<b>Thermostat</b> Main household HVAC thermostat",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        input(
            name:"humidityTimeout",
            type:"number",
            title: "<b>Timeout</b> number of minutes to add humidity while HVAC is heating",
            defaultValue: 10,
            required: true,
            submitOnChange: true
              )
        input(
            name:"temperatureSensors",
            type:"capability.temperatureMeasurement",
            title: "<b>Outdoor temperature sensor(s)</b> that will calculate  -target humidity-",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        if(temperatureSensors){
            paragraph "<b>Current target humidity is ${calculateTarget()}% with an outdoor temperature of ${averageTemperature()}</b>"
        }
        input(
            name:"humiditySensors",
            type:"capability.relativeHumidityMeasurement",
            title: "<b>Indoor humidity sensor(s)</b> to compare to -target humidity-",
            required: true,
            multiple: true,
            submitOnChange: true
            )
        if(humiditySensors){
            paragraph "<b>Current humidity average is ${averageHumidity()}%</b>"
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
        paragraph(
        title: "Smart Humidifier Control Options",
        required: true,
    	"<div style='text-align:center'><b>Smart Humidifier Control Options</b></div>"
     	)
        input(
            name:"smartHumidifier",
            type:"capability.relativeHumidityMeasurement",
            title:"<b>Smart Humidiier</b> to control and set humidity setpoint",
            multiple: false,
            required: true,
            submitOnChange: true
        )
        if(temperatureSensors){
        paragraph(
            "<b>Current target humidity is ${calculateTarget2()}% with an outdoor temperature of ${averageTemperature()}</b>"
            )
        }
        if(smartHumidifier){
            def smartHum = settings.smartHumidifier.currentValue("humidity")
        paragraph(
            "<b>Current humidity of smart humidifier is $smartHum %</b>"
            )
        }
        input(
            name:"smartHumidifierOffset",
            type:"decimal",
            title:"Target offset for smart humidifier setpoint",
            defaultValue: 0,
            submitOnChange: true
        )
        input(
            name:"displayMode",
            type:"mode",
            title:"<b>Mode(s)</b> for Display On",
            multiple: true,
            submitOnChange: true
        )
        input(
            name:"nightLightMode",
            type:"mode",
            title:"<b>Mode(s)</b> for Night Light On",
            multiple: true,
            submitOnChange: true
        )
    }
    section{
        paragraph(
        title: "Ceiling Fan Control Options",
        required: true,
    	"<div style='text-align:center'><b>Ceiling Fan Control Options</b></div>"
     	)
        input(
            name:"fans",
            type:"capability.fanControl",
            title:"<b>Ceiling fans</b> to control when humidity is higher than -target humidity-",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        input(
            name:"fanThresholdLow",
            type:"number",
            title:"Fan threshold - % above target humidity to turn fans on low",
            range: "*..*",
            multiple: false,
            defaultValue:"0",
            required: true,
            submitOnChange: true
        )
        input(
            name:"fanThresholdMed",
            type:"number",
            title:"Fan threshold - % above target humidity to turn fans on medium",
            multiple: false,
            defaultValue:"2",
            required: true,
            submitOnChange: true
        )
        input(
            name:"fanThresholdNight",
            type:"number",
            title:"Fan threshold for Night Mode - % above target humidity to turn fans on medium",
            multiple: false,
            defaultValue:"4",
            required: true,
            submitOnChange: true
        )
        input(
            name:"fanSwitch",
            type:"capability.switch",
            title:"<b>Switch</b> to enable or disable automatic ceiling fans",
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
    logInfo ("$app.label Updated with settings: ${settings}")
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
    if (fanSwitch){
        fanSwitchStatus = settings.fanSwitch.currentValue("switch")
        state.fanSwitchOn = (fanSwitchStatus == "on")
        state.fanSwitchOff = (fanSwitchStatus == "off")
    }
    if (humidistat){
        switchStatus = settings.humidistat.currentValue("switch")
        state.humidistatSwitchOn = (switchStatus == "on")
        state.humidistatSwitchOff = (switchStatus == "off")
    }
    mode = location.currentMode as String
    state.earlyMorning = (mode == "Early_morning")
    state.day = (mode == "Day")
    state.afternoon = (mode == "Afternoon")
    state.dinner = (mode == "Dinner")
    state.evening = (mode == "Evening")
    state.lateEvening = (mode == "Late_evening")
    state.night = (mode == "Night")
    state.away = (mode == "Away")
    modeEventHandler(mode)
    humidistatHandler()
    setHumiditySetpoint()
    logInfo ("$app.label subscribed to sensor events")
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
    state.nightMode = (mode == "Night")
    state.lateEveningMode = (mode == "Late_evening")
    logInfo ("$app.label mode status $mode")
    displayModeList = displayMode as String
    nightLightModeList = nightLightMode as String
    if (displayModeList.contains ("$mode")){
        settings.smartHumidifier.setDisplay("on")
    }else{
        settings.smartHumidifier.setDisplay("off")
    }
    if (nightLightModeList.contains ("$mode")){
        settings.smartHumidifier.nightLight("dim")
    }else{
        settings.smartHumidifier.nightLight("off")
    }   
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
	logInfo ("$app.label Current temperature average is ${averageTemperature()}")
    humidistatHandler()
    setHumiditySetpoint()
}

def calculateTarget() {
	def double outdoorTemp = averageTemperature()
	def target = Math.max(Math.min((double)frostCorrection+27.7+0.535*outdoorTemp - 0.00409 * Math.pow(outdoorTemp, 2), (double)maxRh), (double)minRh)
    logInfo ( "$app.label Calculated a target humidity of ${target.round(1)}% for the main floor with an outdoor temperature of ${outdoorTemp}")
    return Math.round(target*10)/10
}

def calculateTarget2() {
	def double outdoorTemp = averageTemperature()
	def target = Math.max(Math.min((double)smartHumidifierOffset+27.7+0.535*outdoorTemp - 0.00409 * Math.pow(outdoorTemp, 2), (double)maxRh), (double)minRh)
    logInfo ( "$app.label Calculated a target humidity of ${target.round(1)}% for the basement with an outdoor temperature of ${outdoorTemp}")
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
	logInfo ("$app.label Current humidity average is ${averageHumidity()}")
    humidistatHandler()
}

def fanSwitchHandler(evt){
    switchStatus = evt.value
    logInfo ("$app.label Automatic fan switch $switchStatus")
    state.fanSwitchOn = (switchStatus == "on")
    state.fanSwitchOff = (switchStatus == "off")
    humidistatHandler()
}

def humidistatSwitchHandler(evt){
    humidistatSwitch = evt.value
    logInfo ("$app.label Humidistat $humidistatSwitch")
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
    if (state.nightMode && state.fanSwitchOn || state.lateEveningMode && state.fanSwitchOn){
        logInfo ("$app.label running late evening / night humidistat fan handler")
        humidistatNoFans()
        humidistatNightFans()
    }
    else if (state.fanSwitchOn){
        logInfo ("$app.label running day humidistat fan handler")
        humidistatNoFans()
        humidistatFans()
    }
    else if (state.fanSwitchOff){
        logInfo ("$app.label Auto fan switch off running humidistat handler")
        humidistatNoFans()
    }
}

def humidistatNoFans(){
    if (state.lowHumidity&&state.humidistatSwitchOff){
        logInfo ("$app.label conditions are met setting humidistat to ready state")
        state.humidistatReady = true
    }
    else if (state.lowHumidity&&state.humidistatSwitchOn){
        logInfo ("$app.label conditions are met humidistat is already ON")
    }
    else if (state.goodHumidity&&state.humidistatSwitchOn){
        logInfo ("$app.label Humidity is on target - turning humidistat OFF")
        state.humidistatReady = false
        settings.humidistat.off()
    }
    else if (state.goodHumidity&&state.humidistatSwitchOff){
        logInfo ("$app.label Humidity is on target humidistat is already OFF")
        state.humidistatReady = false
    }
}

def humidistatFans(){
    if (state.fanOffHumidity){
        logInfo ("$app.label Humidity is below target plus threshold - turning Fans OFF")
        settings.fans.setSpeed("off")
    }
    if (state.fanLowHumidity){
        logInfo ("$app.label Humidity is above target plus low threshold - turning Fans ON low")
        settings.fans.setSpeed("low")
    }
    if (state.fanMedHumidity){
        logInfo ("$app.label Humidity is above target plus medium-low threshold - turning Fans ON medium")
        settings.fans.setSpeed("medium")
    }   
}

def humidistatNightFans(){
    if (state.fanOffHumidity){
        logInfo ("$app.label Humidity is below target plus threshold - turning Fans OFF")
        settings.fans.setSpeed("off")
    }
    if (state.fanLowNightHumidity){
        logInfo ("$app.label Humidity is above target plus low for Night threshold - turning Fans ON low")
        settings.fans.setSpeed("low")
    }
    if (state.fanMedNightHumidity){
        logInfo ("$app.label Humidity is above target plus medium Night threshold - turning Fans ON medium")
        settings.fans.setSpeed("medium")
    }   
}

def thermostatOperatingStateHandler(evt){
    status = evt.value
    logInfo ("$app.label thermostat operating state $status")
    state.idle = (status == "idle")
    state.heating = (status == "heating")
    if (state.heating&&state.humidistatReady){
        logInfo ("$app.label HVAC running with humidistat on")
        settings.humidistat.on()
        runIn(humidityTimeout*60,humidistatOff)
    }
    else if (state.heating&&state.humidistatSwitchOff){
        logInfo ("$app.label HVAC running, humidity not needed")
    }
}

def humidistatOff(){
    logInfo ("$app.label humidity timeout expired, turning humidistat off")
    settings.humidistat.off()
}

def setHumiditySetpoint(){
    value = calculateTarget2()
    settings.smartHumidifier.setHumidity(value)
    logInfo ("$app.label setting humidity setpoint $value")
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
