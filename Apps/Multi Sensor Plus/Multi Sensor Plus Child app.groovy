/**
 *  ****************  Multi Sensor Plus  Child App  ****************
 *
 *   Average: Temperature, Humidity, and Illuminance   -
 *   Group:  Locks, Contact, Motion, Water, Presence,Smoke, CO and Sound Sensors  -
 *   Plus  a Virtual Switch  -  All  In One Device
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
 *  Last Update: 1/09/2021
 *
 *  Changes:
 *
 *  V1.0.0 -    1-09-2021       First run
 *  V1.1.0 -    1-10-2021       Fixed "size" error
 *  V1.2.0 -    1-11-2021       Improved Motion Sensor Handler
 *  V1.3.0 -    1-12-2021       Improved event sending and revamped device driver
 *  V1.4.0 -    1-12-2021       Added timeouts for presence and sound sensors
 *  V1.5.0 -    1-13-2021       Improved Motion and sound  sensor Handlers
 *  V2.0.0 -    1-14-2021       Improvements,  Revamped Presence for normal sensors and Nest Cameras
 *  V2.1.0 -    1-22-2021       code clean up and improvements
 *  V2.2.0 -    1-23-2021       added initialize values & code improvements
 *  V2.3.0 -    1-24-2021       reworked avg and code cleanup - Release
 *  V2.4.0 -    7-05-2021       reworked status updating method.
 *  V2.5.0 -    11-17-2023      Added waterLeak attribute for google home.
 *  V2.6.0 -    11-19-2023      Added smoke and CO attribute for google home community app compatibilty.
 */

import groovy.transform.Field

definition(
    name: "Multi Sensor Plus Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Average: Temperature, Humidity, and Illuminance"+
    "-  Group: Locks, Contact, Motion, Water, Presence, and Sound Sensors"+
    "-  Plus  a Virtual Switch  -  All  In One Device",
    parent: "Gassgs:Multi Sensor Plus",
    category: "Utilities",
    importUrl: "https://raw.githubusercontent.com/Gassgs/Hubitat-Apps-and-Drivers/master/Apps/Multi%20Sensor%20Plus/Multi%20Sensor%20Plus%20Child%20app.groovy",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Multi Sensor Plus App",
        required: true,
    	"<div style='text-align:center'><big><b>Multi Sensor Plus App</b></div></big>"
     	)
       
     paragraph(
         title: "Multi Sensor Plus Child",
        required: false,
    	"<div style='text-align:center'><b>Average</b>: Temperature, Humidity, and Illuminance"+
         "- <b>Group</b>: Locks, Contact, Motion, Water, Presence, and Sound Sensors"+
         "- <b>Plus</b>: a Virtual Switch <b>All In One Device</b></div>"
     	)
        
        input(
            name:"multiSensor",
            type:"capability.sensor",
            title: "<b>Virtual Multi Sensor Device</b>(Create device before adding rules)",
            required: true
              )
    }
    section{
        input(
            name:"temperatureSensors",
            type:"capability.temperatureMeasurement",
            title: "<b>Temperature</b> Sensors to average (optional)",
            multiple: true,
            submitOnChange: true
            )
        if(temperatureSensors){
            paragraph "Current average is ${averageTemperature()}"
        }
   }
    section{
        input(
            name:"humiditySensors",
            type:"capability.relativeHumidityMeasurement",
            title: "<b>Humidity</b> Sensors to average (optional)",
            multiple: true,
            submitOnChange: true
            )
        if(humiditySensors){
            paragraph "Current average is ${averageHumidity()}%"
        }
   }
    section{
        input(
            name:"illuminanceSensors",
            type:"capability.illuminanceMeasurement",
            title: "<b>Lux</b> Sensors to average (optional)",
            multiple: true,
            submitOnChange: true
            )
        if(illuminanceSensors){
            paragraph "Current average is ${averageIlluminance()}"
        }
   }
    section{
        input(
            name:"contactSensors",
            type:"capability.contactSensor",
            title: "<b>Contact</b> Sensors to group (optional)",
            multiple: true
            )
   }
    section{
        input(
            name:"locks",
            type:"capability.lock",
            title: "<b>Locks</b> to group (optional)",
            multiple: true
            )
   }
    section{
        input(
            name:"waterSensors",
            type:"capability.waterSensor",
            title: "<b>Water</b> Sensors to group (optional)",
            multiple: true
            )
   }
    section{
        input(
            name:"motionSensors",
            type:"capability.motionSensor",
            title: "<b>Motion</b> Sensors to group (optional)",
            multiple: true,
            submitOnChange: true
            )
        if (motionSensors){
            input(
                name:"motionTimeout",
                type:"number",
                title: "Activity timeout (in seconds) Started after all sensors become inactive",
                defaultValue: 0,
                required: true
                )
        }
   }
    section{
        input(
            name:"smokeSensors",
            type:"capability.smokeDetector",
            title: "<b>Smoke</b> Sensors to group (optional)",
            multiple: true,
            submitOnChange: true
            )
   }
    section{
        input(
            name:"coSensors",
            type:"capability.carbonMonoxideDetector",
            title: "<b>CarbonMonoxide</b> Sensors to group (optional)",
            multiple: true,
            submitOnChange: true
            )
   }
    section{
        input(
            name:"soundSensors",
            type:"capability.soundSensor",
            title: "<b>Sound</b> Sensors to group (optional)",
            multiple: true,
            submitOnChange: true
            )
        if (soundSensors){
            input(
                name:"soundTimeout",
                type:"number",
                title: "Sound timeout (in seconds) Started after all sensors become not detected",
                defaultValue: 0,
                required: true
                )
        }
   }
    section{
        input(
            name:"presenceSensors",
            type:"capability.presenceSensor",
            title: "<b>Presence</b> Sensors to group (optional)",
            multiple: true
            )
        input(
            name:"nestEnable",
            type:"bool",
            title: "Enable this option for use with Nest Cameras instead of typical presence devices",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        if(nestEnable){
            input(
                name:"presenceTimeout",
                type:"number",
                title: "Timeout (in seconds) Started after all cameras  do not detect a person",
                defaultValue: 0,
                required: true
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
    subscribe(settings.illuminanceSensors,"illuminance",illuminanceSensorsHandler)
	subscribe(settings.contactSensors, "contact", contactSensorsHandler)
    subscribe(settings.locks, "lock", lockHandler)
    subscribe(settings.waterSensors, "water", waterSensorHandler)
    subscribe(settings.motionSensors, "motion",  motionSensorHandler)
    subscribe(settings.smokeSensors, "smoke", smokeSensorHandler)
    subscribe(settings.coSensors, "carbonMonoxide", coSensorHandler)
    subscribe(settings.soundSensors, "sound", soundSensorHandler)
    subscribe(settings.presenceSensors, "presence", presenceSensorHandler)
    loadValues()
    logInfo ("subscribed to sensor events")
    }

def loadValues(){
    if (settings.temperatureSensors){
        getTemperature()
    }
    if (settings.humiditySensors){
        getHumidity()
    }
    if (settings.illuminanceSensors){
        getLux()
    }
    if (settings.contactSensors){
        getContacts()
    }
    if (settings.locks){
        getLocks()
    }
    if (settings.waterSensors){
        getWaterStatus()
    }
    if (settings.motionSensors){
        getMotion()
    }
    if (settings.coSensors){
        getCo()
    }
    if (settings.smokeSensors){
        getSmoke()
    }
    if (settings.soundSensors){
        getSound()
    }
    if (settings.presenceSensors){
        getPresence()
    }
}

def roundedAverage(list, transform, precision){
    return (list.sum(transform).toDouble() / list.size()).round(precision)
}

def temperatureSensorsHandler(evt){
    getTemperature()
}

def averageTemperature(){
    return roundedAverage(settings.temperatureSensors, {it.currentTemperature}, 1)
}

def getTemperature(){
	def avg = averageTemperature()
    sendEvent(multiSensor,[name:"temperature",value:"$avg"])
	logInfo ("Current temperature average is ${averageTemperature()}")
}

def humiditySensorsHandler(evt){
    getHumidity()
}

def averageHumidity(){
    return roundedAverage(settings.humiditySensors, {it.currentHumidity}, 1)
}

def getHumidity(){
	def avg = averageHumidity()
    sendEvent(multiSensor,[name:"humidity",value:"$avg"])
	logInfo("Current humidity average is ${averageHumidity()}%")
}

def illuminanceSensorsHandler(evt){
    getLux()
}

def averageIlluminance(){
	def total = 0
    def n = settings.illuminanceSensors.size()
	settings.illuminanceSensors.each {total += it.currentIlluminance}
	return (total /n).toDouble().round()
}

def getLux(){
	def avg = averageIlluminance()
    sendEvent(multiSensor,[name:"illuminance",value:"$avg"])
	logInfo ("Current lux average is ${averageIlluminance()}")
}

def contactSensorsHandler(evt){
    getContacts()
}

def getContacts(){
    def open = settings.contactSensors.findAll { it?.latestValue("contact") == 'open' }
		if (open){
            contactList = "${open}"
            sendEvent(multiSensor,[name:"contact",value:"open"])
            sendEvent(multiSensor,[name:"Contacts",value:contactList])
            logInfo("contactOpen"+contactList)
        }
    else{
        sendEvent(multiSensor,[name:"contact",value:"closed"])
        sendEvent(multiSensor,[name:"Contacts",value:"All Closed"])
        logInfo("All Closed")
    }
}

def lockHandler(evt){
    getLocks()
}

def getLocks(){
	def unlocked = settings.locks.findAll { it?.latestValue("lock") == 'unlocked' }
		if (unlocked){
            lockList = "${unlocked}"
            sendEvent(multiSensor,[name:"lock",value:"unlocked"])
            sendEvent(multiSensor,[name:"Locks",value:lockList])
            logInfo("Unlocked"+lockList)
        }
    else{
        sendEvent(multiSensor,[name:"lock",value:"locked"])
        sendEvent(multiSensor,[name:"Locks",value:"All Locked"])
        logInfo("All Locked")
    }
}

def waterSensorHandler(evt){
    getWaterStatus()
}

def getWaterStatus(){
	def wet = settings.waterSensors.findAll { it?.latestValue("water") == 'wet' }
		if (wet){
            waterList = "${wet}"
            sendEvent(multiSensor,[name:"water",value:"wet"])
            sendEvent(multiSensor,[name:"waterLeak",value:"leak"])
            sendEvent(multiSensor,[name:"Water_Sensors",value:waterList])
            logInfo("leakDetected"+waterList)
        }
    else{
        sendEvent(multiSensor,[name:"water",value:"dry"])
        sendEvent(multiSensor,[name:"waterLeak",value:"no leak"])
        sendEvent(multiSensor,[name:"Water_Sensors",value:"All Dry"])
        logInfo("All Dry")
    }
}

def motionSensorHandler(evt){
    getMotion()
}

def getMotion(){
	def active = settings.motionSensors.findAll { it?.latestValue("motion") == 'active' }
		if (active){
		    unschedule(motionInactive)
            motionList = "${active}"
            sendEvent(multiSensor,[name:"motion",value:"active"])
            sendEvent(multiSensor,[name:"Motion_Sensors",value:motionList])
            logInfo("motionActive"+motionList)
        }
    else{
       runIn(motionTimeout,motionInactive)
    }
}
def motionInactive(){
    sendEvent(multiSensor,[name:"motion",value:"inactive"])
    sendEvent(multiSensor,[name:"Motion_Sensors",value:"All Inactive"])
    logInfo("All Inactive")
}

def smokeSensorsHandler(evt){
    getSmoke()
}

def getSmoke(){
    def detected = settings.smokeSensors.findAll { it?.latestValue("smoke") == 'detected' }
		if (detected){
            smokeList = "${detected}"
            sendEvent(multiSensor,[name:"smoke",value:"smoke detected"])
            sendEvent(multiSensor,[name:"Smoke_Sensors",value:smokeList])
            logInfo("smoke detected "+smokeList)
        }
    else{
        sendEvent(multiSensor,[name:"smoke",value:"no smoke detected"])
        sendEvent(multiSensor,[name:"Smoke_Sensors",value:"All Clear"])
        logInfo("Smoke All Clear")
    }
}

def coSensorsHandler(evt){
    getCo()
}

def getCo(){
    def detected = settings.coSensors.findAll { it?.latestValue("carbonMonoxide") == 'detected' }
		if (detected){
            coList = "${detected}"
            sendEvent(multiSensor,[name:"carbonMonoxide",value:"carbon monoxide detected"])
            sendEvent(multiSensor,[name:"CarbonMonoxide_Sensors",value:coList])
            logInfo("CO detected"+coList)
        }
    else{
        sendEvent(multiSensor,[name:"carbonMonoxide",value:"no carbon monoxide detected"])
        sendEvent(multiSensor,[name:"CarbonMonoxide_Sensors",value:"All Clear"])
        logInfo("CO All Clear")
    }
}

def soundSensorHandler(evt){
    getSound()
}

def getSound(){
	def detected = settings.soundSensors.findAll { it?.latestValue("sound") == 'detected' }
		if (detected){
		    unschedule(soundNotDetected)
            soundList = "${detected}"
            sendEvent(multiSensor,[name:"sound",value:"detected"])
            sendEvent(multiSensor,[name:"Sound_Heard",value:soundList])
            logInfo("soundDetected"+soundList)
        }
    else{
        runIn(soundTimeout,soundNotDetected)
    }
}
def soundNotDetected(){
    sendEvent(multiSensor,[name:"sound",value:"not detected"])
    sendEvent(multiSensor,[name:"Sound_Heard",value:"No Sound Detected"])
    logInfo("No Sound Detected")
}

def presenceSensorHandler(evt){
    getPresence()
}

def getPresence(){
    if (nestEnable){
        presenceHandler1()
        }
    else{
        presenceHandler2()
    }
}

def presenceHandler1(){
	def present = settings.presenceSensors.findAll { it?.latestValue("presence") == 'present' }
		if (present){
		    unschedule(personNotDetected)
            presenceList = "${present}"
            sendEvent(multiSensor,[name:"presence",value:"present"])
            sendEvent(multiSensor,[name:"Person_Detected",value:presenceList])
            logInfo("personDetected"+presenceList)
        }
    else{
        runIn(presenceTimeout,personNotDetected)
    }
}
def personNotDetected(){
    sendEvent(multiSensor,[name:"presence",value:"not present"])
    sendEvent(multiSensor,[name:"Person_Detected",value:"No One Present"])
    logInfo("No One Present")
}

def presenceHandler2(){
	def present = settings.presenceSensors.findAll { it?.latestValue("presence") == 'present' }
		if (present){
            presenceList = "${present}"
            sendEvent(multiSensor,[name:"presence",value:"present"])
            sendEvent(multiSensor,[name:"Home",value:presenceList])
            logInfo("Home"+presenceList)
        }
    else{
        sendEvent(multiSensor,[name:"presence",value:"not present"])
        sendEvent(multiSensor,[name:"Home",value:"Everyone is Away"])
        logInfo("Everyone is  Away")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
