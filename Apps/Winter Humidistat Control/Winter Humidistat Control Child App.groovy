/**
 *  ****************  Winter Humidistat Control Child App ****************
 *
 *  Calculates target humidity based on an outdoor temperature sensor
 *  Control a switch or relay when comparing humidity sensors in the home.
 *  Optional motion activity restriction and option to turn ceiling fans on low when high humidity
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
 */

import groovy.transform.Field

definition(
    name: "Winter Humidistat Control Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Calculates target humidity based on an outdoor temperature sensor"+
    " Control a switch or relay when humidity sensors report lower than tartget in the home."+
    " Optional motion activity restriction and option to turn ceiling fans on when humidity is high humidity",
    parent: "Gassgs:Winter Humidistat Control",
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
    	"<div style='text-align:center'><b>Winter Humidistat Control App</b></div>"
     	)
     paragraph(
        title: "Winter Humidistat Control Child",
        required: true,
    	"<div style='text-align:center'>Humidistat Control Options</div>"
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
            title:"If you set condensation on your windows set to a lower number.",
            defaultValue: 0,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"humiditySensors",
            type:"capability.relativeHumidityMeasurement",
            title: "Humidity sensor(s) to compare to -target humidity-",
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
            title:"Motion sensor or zone to disable humidistat from running (optional)",
            multiple: false,
            required: false,
            submitOnChange: true
        )  
    }
    section{
        input(
            name:"fans",
            type:"capability.fanControl",
            title:"Ceiling fans to control when humidity is higher than -target humidity- (optional)",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        input(
            name:"fanThresholdLow",
            type:"number",
            title:"Fan low threshold -  % above target humidity to turn fans on low",
            multiple: false,
            defaultValue:"1",
            required: true,
            submitOnChange: true
        )
        input(
            name:"fanThresholdMed",
            type:"number",
            title:"Fan medium threshold -  % above target humidity to turn fans on medium-low",
            multiple: false,
            defaultValue:"3",
            required: true,
            submitOnChange: true
        )
        if (fans){
        input(
            name:"fanSwitch",
            type:"capability.switch",
            title:"Switch to enable or disable automatic ceiling fans",
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
    subscribe(settings.temperatureSensors, "temperature", temperatureSensorsHandler)
	subscribe(settings.humiditySensors, "humidity",humiditySensorsHandler)
    subscribe(settings.motionSensor, "motion",  motionHandler)
    subscribe(settings.fanSwitch, "switch",  fanSwitchHandler)
    humidistatHandler()
    logInfo ("subscribed to sensor events")
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

def motionHandler(evt){
    motionStatus = evt.value
    logInfo ("motion status $motionStatus")
    state.motionActive = (motionStatus == "active")
    state.motionInactive = (motionStatus == "inactive")
    humidistatHandler()
}

def fanSwitchHandler(evt){
    switchStatus = evt.value
    logInfo ("Automatic fan switch $switchStatus")
    state.fanSwitchOn = (switchStatus == "on")
    state.fanSwitchOff = (switchStatus == "off")
    humidistatHandler()
}

def humidistatHandler(){
    humidityAvg = averageHumidity()
    target = calculateTarget()
    thresholdLow = (target + fanThresholdLow)
    thresholdMed = (target + fanThresholdMed)
    state.lowHumidity = (humidityAvg < target-1)
    state.goodHumidity = (humidityAvg >= target)
    state.fanOffHumidity = (humidityAvg < thresholdLow)
    state.fanLowHumidity = (humidityAvg >= thresholdLow)
    state.fanMedHumidity = (humidityAvg >= thresholdMed)
    if (settings.motionSensor&&settings.fans){
        humidistatMotionPlusFans()
    }
   else if (settings.motionSensor){
        humidistatMotion()
    }
   else if (settings.fans){
        humidistatFans()
    }
    else{
        if (state.lowHumidity){
        logInfo ("conditions are met turning humidistat ON")
        settings.humidistat.on()
        }
        if (state.goodHumidity){
        logInfo ("Humidity is on target - turning OFF")
        settings.humidistat.off()
        }
    }
}
def humidistatMotion(){
    if (state.lowHumidity&&state.motionInactive){
        logInfo ("conditions are met turning humidistat ON")
        settings.humidistat.on()
    }
    else{
        unschedule(humidistatOn)
        logInfo ("Humidity is on target or motion is active - turning OFF")
        settings.humidistat.off()
    }
}
def humidistatFans(){
    if (state.lowHumidity){
        logInfo ("conditions are met turning humidistat ON")
        settings.humidistat.on()
    }
    if (state.goodHumidity){
        logInfo ("Humidity is on target - turning humidistat OFF")
        settings.humidistat.off()
    }
    if (state.fanOffHumidity&&state.fanSwitchOn){
        logInfo ("Humidity is below target plus threshold - turning Fans OFF")
        settings.fans.setSpeed("off")
    }
    if (state.fanMedHumidity&&state.fanSwitchOn){
        logInfo ("Humidity is above target plus medium-low threshold - turning Fans ON medium-low")
        settings.fans.setSpeed("medium-low")
    }
    else if (state.fanLowHumidity&&state.fanSwitchOn){
        logInfo ("Humidity is above target plus low threshold - turning Fans ON low")
        settings.fans.setSpeed("low")
    }
}

def humidistatMotionPlusFans(){
    if (state.motionInactive){
        humidistatFans()
    }
    else{
        logInfo ("turning humistat and fans off due to motion")
        settings.humidistat.off()
        settings.fans.setSpeed("off")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
