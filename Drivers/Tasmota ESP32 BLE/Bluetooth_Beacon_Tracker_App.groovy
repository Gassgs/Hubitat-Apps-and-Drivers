/**
 *  ****************  Bluetooth Beacon Tracker  ****************
 *
 * Track upto 8 beacons from multiple Tasmota ESP32 BLE devices
 * Use with Tasmota ESP32-C3 BLE Tracking Devices and BLE Beacon/BLE Tasker Beacon combined virtual presence devices
 *
 *
 *  Copyright 2023 Gassgs / Gary Gassmann
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
 *  Changes:
 *
 *  V1.0.0      -       11-02-2023       First run, test new beacons and additional ESP32's
 *  V2.0.0      -       11-03-2023       Expanded to support 8 beacon devices
 */

import groovy.transform.Field

definition(
    name: "Bluetooth Beacon Tracker ",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Bluetooth Beacon Tracker App",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "BLE Beacon Tracker App",
        required: true,
    	"<div style='text-align:center'><big><b>BLE Beacon Tracker App</b></div></big>"
     	)
     paragraph(
        title: "Tasmota ESP32 Tracker Devices",
        required: true,
    	"<b><div style='text-align:center'>Tasmota ESP32 Tracker Devices Options</div></b>"
     	)
        input(
            name:"ESP32",
            type:"capability.beacon",
            title: "<b>Tasmota ESP32 Devices to Monitor</b>",
            multiple: true,
            required: true,
            submitOnChange: true
        )
        input(
            name:"buffer",
            type:"number",
            title: "<b>Beacon Timeout Buffer</b><i> *in seconds </i>",
            required: true,
            defaultValue: 10,
            submitOnChange: true
        )
    }
    section{
        paragraph(
        title: "Master Beacon Options",
        required: true,
    	"<div style='text-align:center'><b>Master Presence Device Options</b></div>"
     	)
        input(
            name:"master1",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 1</b>",
            required: false,
            submitOnChange: true
        )
        input(
            name:"master2",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 2</b>",
            required: false,
            submitOnChange: true
        )
        input(
            name:"master3",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 3</b>",
            required: false,
            submitOnChange: true
        )
        input(
            name:"master4",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 4</b>",
            required: false,
            submitOnChange: true
        )
        input(
            name:"master5",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 5</b>",
            required: false,
            submitOnChange: true
        )
        input(
            name:"master6",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 6</b>",
            required: false,
            submitOnChange: true
        )
        input(
            name:"master7",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 7</b>",
            required: false,
            submitOnChange: true
        )
        input(
            name:"master8",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 8</b>",
            required: false,
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
    if (master1){subscribe(ESP32, "beacon1", beacon1Handler)}
    if (master2){subscribe(ESP32, "beacon2", beacon2Handler)}
    if (master3){subscribe(ESP32, "beacon3", beacon3Handler)}
    if (master4){subscribe(ESP32, "beacon4", beacon4Handler)}
    if (master5){subscribe(ESP32, "beacon5", beacon5Handler)}
    if (master6){subscribe(ESP32, "beacon6", beacon6Handler)}
    if (master7){subscribe(ESP32, "beacon7", beacon6Handler)}
    if (master8){subscribe(ESP32, "beacon8", beacon6Handler)}
    logInfo ("subscribed to sensor events")
}

////////////////////////////////////////////////////////////////////
def beacon1Handler(evt){
    getBeacon1()
}
def getBeacon1(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon1") == 'detected' }
    statusAway = settings.master1.currentValue("beacon") == 'not detected'
    if (detected){
        unschedule(beacon1Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 1"+beaconList)
        if (statusAway){
            settings.master1.beacon("detected")
        }
    }else{
        runIn(buffer,beacon1Inactive)
    }
}
def beacon1Inactive(){
    settings.master1.beacon("not detected")
    logInfo("$app.label Beacon 1 Not Detected")
}

/////////////////////////////////////////////////////////////////////
def beacon2Handler(evt){
    getBeacon2()
}
def getBeacon2(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon2") == 'detected' }
    statusAway = settings.master2.currentValue("beacon") == 'not detected'
    if (detected){
        unschedule(beacon2Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 2"+beaconList)
        if (statusAway){
            settings.master2.beacon("detected")
        }
    }else{
       runIn(buffer,beacon2Inactive)
    }
}
def beacon2Inactive(){
    settings.master2.beacon("not detected")
    logInfo("$app.label Beacon 2 Not Detected")
}

/////////////////////////////////////////////////////////////////////
def beacon3Handler(evt){
    getBeacon3()
}
def getBeacon3(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon3") == 'detected' }
    statusAway = settings.master3.currentValue("beacon") == 'not detected'
    if (detected){
        unschedule(beacon3Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 3"+beaconList)
        if (statusAway){
            settings.master3.beacon("detected")
        }
    }else{
       runIn(buffer,beacon3Inactive)
    }
}
def beacon3Inactive(){
    settings.master3.beacon("not detected")
    logInfo("$app.label Beacon 3 Not Detected")
}

/////////////////////////////////////////////////////////////////////
def beacon4Handler(evt){
    getBeacon4()
}
def getBeacon4(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon4") == 'detected' }
    statusAway = settings.master4.currentValue("beacon") == 'not detected'
    if (detected){
        unschedule(beacon4Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 4"+beaconList)
        if (statusAway){
            settings.master4.beacon("detected")
        }
    }else{
       runIn(buffer,beacon4Inactive)
    }
}
def beacon4Inactive(){
    settings.master4.beacon("not detected")
    logInfo("$app.label Beacon 4 Not Detected")
}

////////////////////////////////////////////////////////////////////
def beacon5Handler(evt){
    getBeacon5()
}
def getBeacon5(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon5") == 'detected' }
    statusAway = settings.master5.currentValue("beacon") == 'not detected'
    if (detected){
        unschedule(beacon5Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 5"+beaconList)
        if (statusAway){
            settings.master5.beacon("detected")
        }
    }else{
       runIn(buffer,beacon5Inactive)
    }
}
def beacon5Inactive(){
    settings.master5.beacon("not detected")
    logInfo("$app.label Beacon 5 Not Detected")
}

////////////////////////////////////////////////////////////////////
def beacon6Handler(evt){
    getBeacon6()
}
def getBeacon6(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon6") == 'detected' }
    statusAway = settings.master6.currentValue("beacon") == 'not detected'
    if (detected){
        unschedule(beacon6Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 6"+beaconList)
        if (statusAway){
            settings.master6.beacon("detected")
        }
    }else{
       runIn(buffer,beacon6Inactive)
    }
}
def beacon6Inactive(){
    settings.master6.beacon("not detected")
    logInfo("$app.label Beacon 6 Not Detected")
}

////////////////////////////////////////////////////////////////////
def beacon7Handler(evt){
    getBeacon7()
}
def getBeacon7(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon7") == 'detected' }
    statusAway = settings.master7.currentValue("beacon") == 'not detected'
    if (detected){
        unschedule(beacon7Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 7"+beaconList)
        if (statusAway){
            settings.master7.beacon("detected")
        }
    }else{
       runIn(buffer,beacon7Inactive)
    }
}
def beacon7Inactive(){
    settings.master7.beacon("not detected")
    logInfo("$app.label Beacon 7 Not Detected")
}

////////////////////////////////////////////////////////////////////
def beacon8Handler(evt){
    getBeacon8()
}
def getBeacon8(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon8") == 'detected' }
    statusAway = settings.master8.currentValue("beacon") == 'not detected'
    if (detected){
        unschedule(beacon8Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 8"+beaconList)
        if (statusAway){
            settings.master8.beacon("detected")
        }
    }else{
       runIn(buffer,beacon8Inactive)
    }
}
def beacon8Inactive(){
    settings.master8.beacon("not detected")
    logInfo("$app.label Beacon 8 Not Detected")
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
