/**
 *  ****************  Multi Sensor Plus  Child App  ****************
 *
 *   Average: Temperature, Humidity, and Illuminance   -  
 *   Group:  Locks, Contact, Motion, Water, Presence, and Sound Sensors  - 
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
 *  V1.0.0 -    1-09-2021       First run    Gassgs
 *  V1.1.0 -    1-10-2021       Fixed "size" error     Gassgs
 *  V1.2.0 -    1-11-2021       Improved Motion Sensor Handler   Gassgs
 *  V1.3.0 -    1-12-2021       Improved event sending and revamped device driver    Gassgs
 *  V1.4.0 -    1-12-2021       Added timeouts for presence and sound sensors      Gassgs
 *  V1.5.0 -    1-13-2021       Improved Motion and sound  sensor Handlers   Gassgs
 *  V2.0.0 -    1-14-2021       Improvements,  Revamped Presence for normal sensors and Nest Cameras   Gassgs
 *  V2.1.0 -    1-22-2021       code clean up and improvements
 *  V2.2.0 -    1-23-2021       added initialize values & code improvements
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
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {
    
	section {
       
     paragraph(
         title: "Multi Sensor Plus Child",
        required: false, 
    	"<div style='text-align:center'><b>Average</b>: Temperature, Humidity, and Illuminance"+
         "- <b>Group</b>: Locks, Contact, Motion, Water, Presence, and Sound Sensors"+
         "- <b>Plus</b>: a Virtual Switch"+
         "<b>All In One Device</b></div>"
     	)
        
        input( 
            name:"multiSensor", 
            type:"capability.sensor", 
            title: "<b>Virtual Multi Sensor Device</b>(Create device before adding rules)", 
            required: true
              )
    }
    section {
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
    section {
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
    section {
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
    section {
        input(
            name:"contactSensors", 
            type:"capability.contactSensor",
            title: "<b>Contact</b> Sensors to group (optional)",
            multiple: true
            )
   }
    section {
        input(
            name:"locks",
            type:"capability.lock", 
            title: "<b>Locks</b> to group (optional)",
            multiple: true
            )
   }
    section {
        input(
            name:"waterSensors",
            type:"capability.waterSensor",
            title: "<b>Water</b> Sensors to group (optional)",
            multiple: true
            )
   }
    section {
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
                type:"number" , 
                title: "Activity timeout (in seconds) Started after all sensors become inactive", 
                defaultValue: 0, 
                required: true
                )
        }
   }
    section {
        input (
            name:"soundSensors",
            type:"capability.soundSensor", 
            title: "<b>Sound</b> Sensors to group (optional)",
            multiple: true,
            submitOnChange: true
            )
        if (soundSensors){
            input(
                name:"soundTimeout", 
                type:"number" , 
                title: "Sound timeout (in seconds) Started after all sensors become not detected", 
                defaultValue: 0, 
                required: true
                )
        }
   }
    section {
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
                type:"number" , 
                title: "Timeout (in seconds) Started after all cameras  do not detect a person", 
                defaultValue: 0, 
                required: true
                )
        }
   }
    section{
        input (
            name:"logEnable",
            type:"bool",
            title: "Enable Info logging",
            required: true, 
            defaultValue: false
            )
    }
}

def installed() {
	initialize()
}

def uninstalled() {
	logInfo ("uninstalling app")
}

def updated() {	
    logInfo ("Updated with settings: ${settings}")
	unschedule()
    unsubscribe()
	initialize()
}

def initialize() {
    subscribe(settings.temperatureSensors, "temperature", temperatureSensorsHandler)
    subscribe(settings.humiditySensors, "humidity",humiditySensorsHandler)
    subscribe( settings.illuminanceSensors,"illuminance",illuminanceSensorsHandler)
    subscribe(settings.contactSensors, "contact", contactSensorsHandler)
    subscribe(settings.locks, "lock", lockHandler)
    subscribe(settings.waterSensors, "water", waterSensorHandler)
    subscribe(settings.motionSensors, "motion",  motionSensorHandler)
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
    if (settings.soundSensors){
        getSound()
    }
    if (settings.presenceSensors){
        getPresence()
    }
}

def temperatureSensorsHandler(evt) {
    getTemperature()
}

def averageTemperature() { 
	def total = 0
    def n =settings. temperatureSensors.size()
	settings.temperatureSensors.each {total += it.currentTemperature}
	return (total /n).toDouble().round(1)
}

def getTemperature(){
	def avg = averageTemperature()
	multiSensor.setTemperature(avg)
	logInfo ("Current temperature average is ${averageTemperature()}")
}
  
def humiditySensorsHandler(evt) {
    getHumidity()
}

def averageHumidity() { 
    def total = 0
	def n=  settings.humiditySensors.size()
    settings.humiditySensors.each {total += it.currentHumidity}
	 return (total /n).toDouble().round(1)    
}

def getHumidity(){
	def avg = averageHumidity()
	multiSensor.setHumidity(avg)
	logInfo("Current humidity average is ${averageHumidity()}%")
}

def illuminanceSensorsHandler(evt) {
    getLux()
}

def averageIlluminance() { 
	def total = 0
    def n = settings.illuminanceSensors.size()
	settings.illuminanceSensors.each {total += it.currentIlluminance}
	return (total /n).toDouble().round()
}

def getLux(){
	def avg = averageIlluminance()
	multiSensor.setIlluminance(avg)
	logInfo ("Current lux average is ${averageIlluminance()}")
}

def contactSensorsHandler(evt){
    getContacts()
}

def getContacts(){
    def open = settings.contactSensors.findAll { it?.latestValue("contact") == 'open' }
		if (open) { 
            contactList = "${open}"      
            multiSensor.statusUpdate("contact","open")
            multiSensor.statusUpdate("Contacts",contactList)
            logInfo("contactOpen"+contactList)
        }
    else{
    multiSensor.statusUpdate("contact","closed")
    multiSensor.statusUpdate("Contacts","All Closed")
    logInfo("All Closed")
    }    
}

def lockHandler(evt){ 
    getLocks()
}

def getLocks(){
	def unlocked = settings.locks.findAll { it?.latestValue("lock") == 'unlocked' }
		if (unlocked) { 
            lockList = "${unlocked}"      
            multiSensor.statusUpdate("lock","unlocked") 
            multiSensor.statusUpdate("Locks",lockList)
            logInfo("Unlocked"+contactList)
        }
    else{
    multiSensor.statusUpdate("lock","locked") 
    multiSensor.statusUpdate("Locks","All Locked")
     logInfo("All Locked")
    }    
}

def waterSensorHandler(evt){ 
    getWaterStatus()
}

def getWaterStatus(){
	def wet = settings.waterSensors.findAll { it?.latestValue("water") == 'wet' }
		if (wet) { 
            waterList = "${wet}"      
            multiSensor.statusUpdate("water","wet") 
            multiSensor.statusUpdate("Water_Sensors",waterList)
            logInfo("leakDetected"+waterList)
        }
    else{
    multiSensor.statusUpdate("water","dry") 
    multiSensor.statusUpdate("Water_Sensors","All Dry")
    logInfo("All Dry")
    }
}

def motionSensorHandler(evt){ 
    getMotion()
}

def getMotion(){
	def active = settings.motionSensors.findAll { it?.latestValue("motion") == 'active' }
		if (active) {
		    unschedule(motionInactive)
            motionList = "${active}"      
            multiSensor.statusUpdate("motion","active") 
            multiSensor.statusUpdate("Motion_Sensors",motionList)
            logInfo("motionActive"+motionList)
		    
    }else{
       runIn(motionTimeout,motionInactive)
    }
}
def motionInactive(){
    multiSensor.statusUpdate("motion","inactive")
    multiSensor.statusUpdate("Motion_Sensors","All Inactive")
     logInfo("All Inactive")
}

def soundSensorHandler(evt){ 
    getSound()
}
       
def getSound(){
	def detected = settings.soundSensors.findAll { it?.latestValue("sound") == 'detected' }
		if (detected) { 
		    unschedule(soundNotDetected)
            soundList = "${detected}"      
            multiSensor.statusUpdate("sound","detected") 
            multiSensor.statusUpdate("Sound_Heard",soundList)
            logInfo("soundDetected"+soundList)
        
    }else{
        runIn(soundTimeout,soundNotDetected)
    }
}
def soundNotDetected(){
    multiSensor.statusUpdate("sound","not detected") 
    multiSensor.statusUpdate("Sound_Heard","No Sound Detected")
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
		if (present) { 
		    unschedule(personNotDetected)
            presenceList = "${present}"      
            multiSensor.statusUpdate("presence","present") 
            multiSensor.statusUpdate("Person_Detected",presenceList)
            logInfo("personDetected"+presenceList)
        
    }else{
        runIn(presenceTimeout,personNotDetected)
    }
}
def personNotDetected(){
    multiSensor.statusUpdate("presence","not present") 
    multiSensor.statusUpdate("Person_Detected","No One Present")
     logInfo("No One Present")
}

def presenceHandler2(){
	def present = settings.presenceSensors.findAll { it?.latestValue("presence") == 'present' }
		if (present) { 
            presenceList = "${present}"
            multiSensor.statusUpdate("presence","present") 
            multiSensor.statusUpdate("Home",presenceList)
            logInfo("Home"+presenceList)
        }
    else{
        multiSensor.statusUpdate("presence","not present") 
        multiSensor.statusUpdate("Home","Everyone is  Away")
        logInfo("Everyone is  Away") 
    }
}

void logInfo(String msg) {
	if (settings?.logEnable != false) {
		log.info "$msg"
	}
}
