/**
 *  ****************  Motion Lighting Simple Child App ****************
 *
 *  Simple motion lighting app. simple child for on/off switches
 *  disable app from running with switch,lux and mode options.
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
 *  Last Update: 2/08/2021
 *
 *  Changes:
 *
 *  V1.0.0  -       2-08-2021       First run
 *  V1.1.0  -       2-09-2021       improvements
 *  V1.2.0  -       2-11-2021       new name for parent app
 */

import groovy.transform.Field

definition(
    name: "Motion Lighting Simple Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Motion Lighting Simple Child",
    parent: "Gassgs:Motion Automation",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Motion Lighting Simple Child",
        required: true,
    	"<div style='text-align:center'><b>Motion Lighting Simple</b></div>"
     	)
     paragraph(
        title: "Motion Lighting Simple Child",
        required: true,
    	"<div style='text-align:center'>Motion Lighting Simple Options</div>"
     	)
        input(
            name:"light",
            type:"capability.switch",
            title: "Light(s) to control",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"activeMotionSensors",
            type:"capability.motionSensor",
            title: "Motion sensors for On",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"inactiveMotionSensors",
            type:"capability.motionSensor",
            title: "Motion sensors or zone for Off",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"timeout",
            type:"number",
            title: "Motion timeout in seconds before inactive action",
            required: true,
            defaultValue:"0",
            submitOnChange: true
              )
    }
    section{
        input(
            name:"switch",
            type:"capability.switch",
            title:"Switch to disable automatic lighting (optional)",
            multiple: false,
            required: false,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"luxSensors",
            type:"capability.illuminanceMeasurement",
            title:"Lux sensor(s) to disable automatic lighting (optional)",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        if(luxSensors){
            paragraph "Current Lux average is ${averageIlluminance()}"
        }
        if (luxSensors){ 
        input(
            name:"luxThreshold",
            type:"number",
            title:"Lux needs to be below this level for actions to run",
            required: false,
            defaultValue:"200",
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
    subscribe(settings.activeMotionSensors, "motion",  activeMotionHandler)
    subscribe(settings.inactiveMotionSensors, "motion",  inactiveMotionHandler)
    subscribe(settings.switch, "switch",  switchHandler)
    subscribe(settings.luxSensors, "illuminance", illuminanceSensorsHandler)
    if (luxSensors){
        getLux()
    }
    logInfo ("subscribed to sensor events")
}

def activeMotionHandler(evt){
    motionStatus = evt.value
    logInfo ("motion status $motionStatus")
    state.motionActive = (motionStatus == "active")
    if (state.motionActive){
        motionActive()
    }
}
def motionActive(){
    if (settings.switch&&settings.luxSensors){
        logInfo ("checking switch and lux values")
        checkSwitchLuxOnLevel()
    }
    else if(settings.switch){
        logInfo ("checking switch value")
        checkSwitchOnLevel()
    }
    else if (settings.luxSensors){
        logInfo ("checking lux value")
        checkLuxOnLevel()
    }
    else{
        lightsOnLevel()
    }
}

def lightsOnLevel(){
    logInfo ("Motion Active Turning Light On")
    settings.light.on()


}
def inactiveMotionHandler(evt){
	def active = settings.inactiveMotionSensors.findAll { it?.latestValue("motion") == 'active' }
		if (active){
		    unschedule(motionInactive)
            motionList = "${active}"
            logInfo("motionActive"+motionList)
        }
    else{
       runIn(timeout,motionInactive)
    }
}
def motionInactive(){
    logInfo("All Inactive")
    if (settings.switch&&settings.luxSensors){
        logInfo ("checking switch and lux values")
        checkSwitchLuxOffLevel()
    }
    else if(settings.switch){
        logInfo ("checking switch value")
        checkSwitchOffLevel()
    }
    else if (settings.luxSensors){
        logInfo ("checking lux value")
        checkLuxOffLevel()
    }
    else{
        lightsOffLevel()
    }
}

def lightsOffLevel(){
    logInfo ("Motion inactive Turning Light Off")
    settings.light.off()
}

def switchHandler(evt){
    switchStatus = evt.value
    logInfo ("motion lighting switch $switchStatus")
    state.switchOk = (switchStatus == "on")
}

def illuminanceSensorsHandler(evt){
    getLux()
}

def averageIlluminance(){
	def total = 0
    def n = settings.luxSensors.size()
	settings.luxSensors.each {total += it.currentIlluminance}
	return (total /n).toDouble().round()
}

def getLux(){
	def avg = averageIlluminance()
	logInfo ("Current lux average is ${averageIlluminance()}")
    state.luxOk = avg < luxThreshold
}

def checkSwitchLuxOnLevel(){
    if(state.switchOk&&state.luxOk){
        logInfo ("Switch OK and Lux OK")
        lightsOnLevel()
    }
    else{
        logInfo ("switch or lux value false stopping")
    }
}
def checkSwitchLuxOffLevel(){
    if(state.switchOk&&state.luxOk){
        logInfo ("Switch OK and Lux OK")
        lightsOffLevel()
    }
    else{
        logInfo ("switch or lux value false stopping")
    }
}

def checkSwitchOnLevel(){
    if(state.switchOk){
        logInfo ("Switch OK")
        lightsOnLevel()
    }
    else{
        logInfo ("switch value false stopping")
    }
}
def checkSwitchOffLevel(){
    if(state.switchOk){
        logInfo ("Switch OK")
        lightsOffLevel()
    }
    else{
        logInfo ("switch value false stopping")
    }
}

def checkLuxOnLevel(){
    if(state.luxOk){
        logInfo ("Lux OK")
        lightsOnLevel()
    }
    else{
        logInfo ("lux value false stopping")
    }
}
def checkLuxOffLevel(){
    if(state.luxOk){
        logInfo ("Lux OK")
        lightsOffLevel()
    }
    else{
        logInfo ("lux value false stopping")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
