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
 *  V1.5.0      -       11-03-2023       Expanded to support 8 beacon devices
 *  V1.7.0      -       11-12-2023       Added current tracker status per device
 *  V2.0.0      -       11-17-2023       Added master monitor device option, cleanup and method improvements
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
        if(master1){
            paragraph "${beacon1Status()}"
        }
        input(
            name:"master2",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 2</b>",
            required: false,
            submitOnChange: true
        )
        if(master2){
            paragraph "${beacon2Status()}"
        }
        input(
            name:"master3",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 3</b>",
            required: false,
            submitOnChange: true
        )
        if(master3){
            paragraph "${beacon3Status()}"
        }
        input(
            name:"master4",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 4</b>",
            required: false,
            submitOnChange: true
        )
        if(master4){
            paragraph "${beacon4Status()}"
        }
        input(
            name:"master5",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 5</b>",
            required: false,
            submitOnChange: true
        )
        if(master5){
            paragraph "${beacon5Status()}"
        }
        input(
            name:"master6",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 6</b>",
            required: false,
            submitOnChange: true
        )
        if(master6){
            paragraph "${beacon6Status()}"
        }
        input(
            name:"master7",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 7</b>",
            required: false,
            submitOnChange: true
        )
        if(master7){
            paragraph "${beacon7Status()}"
        }
        input(
            name:"master8",
            type:"capability.beacon",
            title:"<b>Master Presence Device for Beacon 8</b>",
            required: false,
            submitOnChange: true
        )
        if(master8){
            paragraph "${beacon8Status()}"
        }
        input(
            name:"masterMonitor",
            type:"capability.beacon",
            title:"<b>Master Presence Device for All Beacons</b> <i> Optional </i>",
            required: false,
            submitOnChange: true
        )
        if(masterMonitor){
            paragraph "<i> Bluetooth Trackers are - </i><b> ${monitorStatus()} <b>"
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
    if (master1){
        subscribe(ESP32, "beacon1", beacon1Handler)
        getBeacon1()
    }
    if (master2){
        subscribe(ESP32, "beacon2", beacon2Handler)
        getBeacon2()
    }
    if (master3){
        subscribe(ESP32, "beacon3", beacon3Handler)
        getBeacon3()
    }
    if (master4){
        subscribe(ESP32, "beacon4", beacon4Handler)
        getBeacon4()
    }
    if (master5){
        subscribe(ESP32, "beacon5", beacon5Handler)
        getBeacon5()
    }
    if (master6){
        subscribe(ESP32, "beacon6", beacon6Handler)
        getBeacon6()
    }
    if (master7){
        subscribe(ESP32, "beacon7", beacon7Handler)
        getBeacon7()
    }
    if (master8){
        subscribe(ESP32, "beacon8", beacon8Handler)
        getBeacon8()
    }
    if (masterMonitor){
        subscribe(ESP32, "wifi", statusHandler)
        getStatus()
    }
    logInfo ("subscribed to sensor events")
}

////////////////////////////////////////////////////////////////////
def beacon1Handler(evt){
    getBeacon1()
}
def getBeacon1(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon1") == 'detected' }
    statusAway = settings.master1.currentValue("beacon") != 'detected'
    status1Away = settings.masterMonitor.currentValue("beacon1") != 'present'
    if (detected){
        unschedule(beacon1Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 1"+beaconList)
        if (statusAway){
            settings.master1.beacon("detected")
        }
        if (masterMonitor && status1Away){
            sendEvent(masterMonitor,[name:"beacon1",value:"present"])
        }
    }else{
        runIn(buffer,beacon1Inactive)
    }
}
def beacon1Inactive(){
    settings.master1.beacon("not detected")
    logInfo("$app.label Beacon 1 Not Detected")
    if (masterMonitor){
        sendEvent(masterMonitor,[name:"beacon1",value:"not present"])
    }
}

def beacon1Status(){
    def value = settings.ESP32.findAll { it?.latestValue("beacon1") == 'detected' }
    return value as String
}

/////////////////////////////////////////////////////////////////////
def beacon2Handler(evt){
    getBeacon2()
}
def getBeacon2(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon2") == 'detected' }
    statusAway = settings.master2.currentValue("beacon") != 'detected'
    status2Away = settings.masterMonitor.currentValue("beacon2") != 'present'
    if (detected){
        unschedule(beacon2Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 2"+beaconList)
        if (statusAway){
            settings.master2.beacon("detected")
        }
        if (masterMonitor && status2Away){
            sendEvent(masterMonitor,[name:"beacon2",value:"present"])
        }
    }else{
       runIn(buffer,beacon2Inactive)
    }
}
def beacon2Inactive(){
    settings.master2.beacon("not detected")
    logInfo("$app.label Beacon 2 Not Detected")
    if (masterMonitor){
        sendEvent(masterMonitor,[name:"beacon2",value:"not present"])
    }
}

def beacon2Status(){
    def value = settings.ESP32.findAll { it?.latestValue("beacon2") == 'detected' }
    return value as String
}

/////////////////////////////////////////////////////////////////////
def beacon3Handler(evt){
    getBeacon3()
}
def getBeacon3(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon3") == 'detected' }
    statusAway = settings.master3.currentValue("beacon") != 'detected'
    status3Away = settings.masterMonitor.currentValue("beacon3") != 'present'
    if (detected){
        unschedule(beacon3Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 3"+beaconList)
        if (statusAway){
            settings.master3.beacon("detected")
        }
        if (masterMonitor && status3Away){
            sendEvent(masterMonitor,[name:"beacon3",value:"present"])
        }
    }else{
       runIn(buffer,beacon3Inactive)
    }
}
def beacon3Inactive(){
    settings.master3.beacon("not detected")
    logInfo("$app.label Beacon 3 Not Detected")
    if (masterMonitor){
        sendEvent(masterMonitor,[name:"beacon3",value:"not present"])
    }
}

def beacon3Status(){
    def value = settings.ESP32.findAll { it?.latestValue("beacon3") == 'detected' }
    return value as String
}

/////////////////////////////////////////////////////////////////////
def beacon4Handler(evt){
    getBeacon4()
}
def getBeacon4(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon4") == 'detected' }
    statusAway = settings.master4.currentValue("beacon") != 'detected'
    status4Away = settings.masterMonitor.currentValue("beacon4") != 'present'
    if (detected){
        unschedule(beacon4Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 4"+beaconList)
        if (statusAway){
            settings.master4.beacon("detected")
        }
        if (masterMonitor && status4Away){
            sendEvent(masterMonitor,[name:"beacon4",value:"present"])
        }
    }else{
       runIn(buffer,beacon4Inactive)
    }
}
def beacon4Inactive(){
    settings.master4.beacon("not detected")
    logInfo("$app.label Beacon 4 Not Detected")
    if (masterMonitor){
        sendEvent(masterMonitor,[name:"beacon4",value:"not present"])
    }
}

def beacon4Status(){
    def value = settings.ESP32.findAll { it?.latestValue("beacon4") == 'detected' }
    return value as String
}

////////////////////////////////////////////////////////////////////
def beacon5Handler(evt){
    getBeacon5()
}
def getBeacon5(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon5") == 'detected' }
    statusAway = settings.master5.currentValue("beacon") != 'detected'
    status5Away = settings.masterMonitor.currentValue("beacon5") != 'present'
    if (detected){
        unschedule(beacon5Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 5"+beaconList)
        if (statusAway){
            settings.master5.beacon("detected")
        }
        if (masterMonitor && status5Away){
            sendEvent(masterMonitor,[name:"beacon5",value:"present"])
        }
    }else{
       runIn(buffer,beacon5Inactive)
    }
}
def beacon5Inactive(){
    settings.master5.beacon("not detected")
    logInfo("$app.label Beacon 5 Not Detected")
    if (masterMonitor){
        sendEvent(masterMonitor,[name:"beacon5",value:"not present"])
    }
}

def beacon5Status(){
    def value = settings.ESP32.findAll { it?.latestValue("beacon5") == 'detected' }
    return value as String
}

////////////////////////////////////////////////////////////////////
def beacon6Handler(evt){
    getBeacon6()
}
def getBeacon6(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon6") == 'detected' }
    statusAway = settings.master6.currentValue("beacon") != 'detected'
    status6Away = settings.masterMonitor.currentValue("beacon6") != 'present'
    if (detected){
        unschedule(beacon6Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 6"+beaconList)
        if (statusAway){
            settings.master6.beacon("detected")
        }
        if (masterMonitor && status6Away){
            sendEvent(masterMonitor,[name:"beacon6",value:"present"])
        }
    }else{
       runIn(buffer,beacon6Inactive)
    }
}
def beacon6Inactive(){
    settings.master6.beacon("not detected")
    logInfo("$app.label Beacon 6 Not Detected")
    if (masterMonitor){
        sendEvent(masterMonitor,[name:"beacon6",value:"not present"])
    }
}

def beacon6Status(){
    def value = settings.ESP32.findAll { it?.latestValue("beacon6") == 'detected' }
    return value as String
}

////////////////////////////////////////////////////////////////////
def beacon7Handler(evt){
    getBeacon7()
}
def getBeacon7(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon7") == 'detected' }
    statusAway = settings.master7.currentValue("beacon") != 'detected'
    status7Away = settings.masterMonitor.currentValue("beacon7") != 'present'
    if (detected){
        unschedule(beacon7Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 7"+beaconList)
        if (statusAway){
            settings.master7.beacon("detected")
        }
        if (masterMonitor && status7Away){
            sendEvent(masterMonitor,[name:"beacon7",value:"present"])
        }
    }else{
       runIn(buffer,beacon7Inactive)
    }
}
def beacon7Inactive(){
    settings.master7.beacon("not detected")
    logInfo("$app.label Beacon 7 Not Detected")
    if (masterMonitor){
        sendEvent(masterMonitor,[name:"beacon7",value:"not present"])
    }
}

def beacon7Status(){
    def value = settings.ESP32.findAll { it?.latestValue("beacon7") == 'detected' }
    return value as String
}

////////////////////////////////////////////////////////////////////
def beacon8Handler(evt){
    getBeacon8()
}
def getBeacon8(){
	def detected = settings.ESP32.findAll { it?.latestValue("beacon8") == 'detected' }
    statusAway = settings.master8.currentValue("beacon") != 'detected'
    status8Away = settings.masterMonitor.currentValue("beacon8") != 'present'
    if (detected){
        unschedule(beacon8Inactive)
        beaconList = "${detected}"
        logInfo("$app.label Beacon 8"+beaconList)
        if (statusAway){
            settings.master8.beacon("detected")
        }
        if (masterMonitor && status8Away){
            sendEvent(masterMonitor,[name:"beacon8",value:"present"])
        }
    }else{
       runIn(buffer,beacon8Inactive)
    }
}
def beacon8Inactive(){
    settings.master8.beacon("not detected")
    logInfo("$app.label Beacon 8 Not Detected")
    if (masterMonitor){
        sendEvent(masterMonitor,[name:"beacon8",value:"not present"])
    }
}

def beacon8Status(){
    def value = settings.ESP32.findAll { it?.latestValue("beacon8") == 'detected' }
    return value as String
}

//////////////////////////////////////////////////////////////////////////
def statusHandler(evt){
    getStatus()
}
def getStatus(){
	def offline = settings.ESP32.findAll { it?.latestValue("wifi") == 'offline'}
    if (offline){
        statusList = "${offline}"
        log.warn "$app.label Tracker Offline - $stausList"
        sendEvent(masterMonitor,[name:"status",value:"offline"])
    }else{
        sendEvent(masterMonitor,[name:"status",value:"online"])
    }
}

def monitorStatus(){
    def value = settings.ESP32.findAll { it?.latestValue("wifi") == 'offline'}
    statusList = "${value}"
    if (offline){
        status = "offline"
    }else{
        status = "online"
    return status +" - " +statusList as String
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
