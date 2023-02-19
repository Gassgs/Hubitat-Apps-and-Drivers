/**
 *  King Of Fans Zigbee Fan Controller (Adapted for Hubitat)
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *
 */
def version() {"v2.0"}

metadata {
	definition (name: "KOF Fan Controller", namespace: "Gassgs", author: "GaryG") {
		capability "Actuator"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"
        capability "Light"
        capability "Sensor"
        capability "Fan Control"

        command "cycleSpeedDown"
        command "setSpeed", [[name:"Speed*", type: "ENUM", description: "Speed", constraints: ["off", "on", "low", "medium", "medium-high", "high", "auto"] ] ]
        command "setFanLevel",["number"]
        
        attribute "fanLevel", "string"
        attribute "fan", "string"
        
        

	fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0202", outClusters: "0003, 0019", model: "HDC52EastwindFan"
    }
    
    preferences {
        input name: "logInfo", type: "bool", title: "<b>Enable info logging</b>", defaultValue: true
        input name: "logEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: true
        input name: "childEnable", type: "bool", title: "<b>Enable Child Light Component</b>", defaultValue: false
    }
}

/*
//////////////////Adjusted Levels to make dimming smooth//////////
              needed to adjust for commands and parse returns
20 <= 5
25 == 10
30 == 15
35 == 20
40 == 25
45 == 30
50 == 40
55 == 50
60 == 60
65 == 70
70 == 80
75 == 90
80 >= 100
*/

def parse(String description) {
	if (logEnable) log.debug "Parse description $description"
    def event = zigbee.getEvent(description)
    if (event) {
    	if (logEnable) log.debug "Status report received from controller: [Light ${event.name} is ${event.value}]"
        def childDevice = getChildDevices()?.find {it.data.componentLabel == "Light"}
        if(event.value == "on" || event.value == "off"){
            sendEvent(event)
            if(childDevice) childDevice.sendEvent(event)
            if(!childDevice){
                if (logInfo) log.info "$device.label ${event}"
            }   
        }
        else if(event.value != "on" && event.value != "off"){
            level = event.value
            if (level <= 20){
                newLevel = 5
            }else if (level > 20 && level <= 25){
                newLevel = 10
            }else if (level > 25 && level <= 30){
                newLevel = 15
            }else if (level > 30 && level <= 35){
                newLevel = 20
            }else if (level > 35 && level <= 40){
                newLevel = 25
            }else if (level > 40 && level <= 45){
                newLevel = 30
            }else if (level > 45 && level <= 50){
                newLevel = 40
            }else if (level > 50 && level <= 55){
                newLevel = 50
            }else if (level > 55 && level <= 60){
                newLevel = 60
            }else if (level > 60 && level <= 65){
                newLevel = 70
            }else if (level > 65 && level <= 70){
                newLevel = 80
            }else if (level > 70 && level <= 75){
                newLevel = 90
            }else if (level >= 80){
                newLevel = 100
            }
            sendEvent(name: "level", value: newLevel)
            if(childDevice) childDevice.sendEvent(name: "level", value: newLevel)
        }
    }
	else {
		def map = [:]
		if (description?.startsWith("read attr -")) {
			def descMap = zigbee.parseDescriptionAsMap(description)
			if (descMap.cluster == "0202" && descMap.attrId == "0000") {
				map.name = "speed"
				newSpeed = descMap.value.toInteger()
                if (newSpeed == 01){
                map.value = "low"
                    sendEvent(name:"fanLevel",value:"25")
                    sendEvent(name:"fan",value:"on")
                }
                else if (newSpeed == 02){
                map.value = "medium"
                    sendEvent(name:"fanLevel",value:"50")
                    sendEvent(name:"fan",value:"on")
                }
                else if (newSpeed == 03){
                map.value = "medium-high"
                    sendEvent(name:"fanLevel",value:"75")
                    sendEvent(name:"fan",value:"on")
                }
                else if (newSpeed == 04){
                map.value = "high"
                    sendEvent(name:"fanLevel",value:"100")
                    sendEvent(name:"fan",value:"on")
                }
                else if (newSpeed == 00){
                map.value = "off"
                    sendEvent(name:"fanLevel",value:"0")
                    sendEvent(name:"fan",value:"off")
                }
                else if (newSpeed == 06){
                map.value = "auto"
                    sendEvent(name:"fan",value:"on")
                }
                if (logInfo) log.info "$device.label Status report received from controller: Fan Speed is ${map.value}"
			}
		}
		def result = null
        if (map) {
			result = createEvent(map)
		}
		return result
   	}
}

def getFanName() {
	[
    0:"Off",
    1:"Low",
    2:"Medium",
    3:"Medium High",
	4:"High",
    5:"Off",
    6:"Auto",
	]
}

def installed() {
	if (logInfo) log.info "Installing"
	initialize()
}

def updated() {
	if (logInfo) log.info "Updating"
	initialize()
    if (childEnable){
        createLightChild()
    }else{
        deleteChildren()
    }
    configure()
    if (logEnable) runIn(1800, logsOff)
}

def initialize() {
	if (logInfo) log.info "Initializing"
    state.version = version()
}

def createLightChild() {
    def childDevice = getChildDevices()?.find {it.data.componentLabel == "Light"}
    if (!childDevice) {
        if (logInfo) log.info "$device.label Creating Light Child"
		childDevice = addChildDevice("Gassgs", "KOF Fan Light Child", "${device.deviceNetworkId}-Light",[label: "${device.displayName} Light", isComponent: true, componentLabel: "Light"])
    }
	else {
        if (logInfo) log.info "$device.label Light Child already exists"
	}
}

def deleteChildren() {
	if (logInfo) log.info "Deleting children"
	def children = getChildDevices()
    children.each {child->
  		deleteChildDevice(child.deviceNetworkId)
    }
}

def configure() {
	if (logInfo) log.info "Configuring Reporting and Bindings."
    state.version = version()
	return 	zigbee.configureReporting(0x0006, 0x0000, 0x10, 1, 600, null)+
			zigbee.configureReporting(0x0008, 0x0000, 0x20, 1, 600)+
			zigbee.configureReporting(0x0202, 0x0000, 0x30, 1, 600, null)
}

def off() {
	zigbee.off()
}

def on() {
	zigbee.on()
}

def setLevel(val, duration = null) {
    val = val.toInteger()
    if (val == 0){
        newVal = 0
    }else if (val > 0 && val <= 5){
        newVal = 20
    }else if (val > 5 && val <= 10){
        newVal = 25
    }else if (val > 10 && val <= 15){
        newVal = 30
    }else if (val > 15 && val <= 20){
        newVal = 35
    }else if (val > 20 && val <= 25){
        newVal = 40
    }else if (val > 25 && val <= 30){
        newVal = 45
    }else if (val > 30 && val <= 40){
        newVal = 50
    }else if (val > 40 && val <= 50){
        newVal = 55
    }else if (val > 50 && val <= 60){
        newVal = 60
    }else if (val > 60 && val <= 70){
        newVal = 65
    }else if (val > 70 && val <= 80){
        newVal = 70
    }else if (val > 80 && val <= 90){
        newVal = 75
    }else if (val > 90){
        newVal = 100
    }
    return zigbee.setLevel(newVal.toInteger())
}

def setSpeed(value){
    if (value == "off"){
        fanOff()
    }
    else if (value == "on"){
        fanSpeedResumeLast()
    }
    else if (value == "low"){
        fanSpeed1()
    }
    else if (value == "medium-low"){
        fanSpeed1()
    }
    else if (value == "medium"){
        fanSpeed2()
    }
    else if (value == "medium-high"){
        fanSpeed3()
    }
    else if (value == "high"){
        fanSpeed4()
    }
    else if (value == "auto"){
        fanSpeedBreeze()
    }   
}

def setFanLevel(data){
    value = data as Integer
    if (value == 0){
        fanOff()
    }
    else if( value > 0 && value <= 25 ){
        fanSpeed1()
    }
    else if( value > 25 && value <= 50 ){
        fanSpeed2()
    }
    else if( value > 50 && value <= 75 ){
        fanSpeed3()
    }
    else if( value > 75 && value <= 100 ){
        fanSpeed4()
    }
}

def fanOff() {
	if (logInfo) log.info "$device.label Turning Fan Off"
    currentSpeed = device.currentValue("speed")
    if (currentSpeed != "off"){
        state.lastSpeed = device.currentValue("speed")
        if (logEnable) log.debug "last level is ${state.lastSpeed}"
    }
    return zigbee.writeAttribute(0x0202, 0x0000, 0x30, 00)
}

def fanSpeedResumeLast() {
    def data = "$state.lastSpeed"
    if (logInfo) log.info "$device.label Resuming Previous Fan Speed" 
    if (logEnable) log.debug "on at $data last speed"
    return setSpeed("$data")    
}

def fanSpeedBreeze() {
	if (logInfo) log.info "$device.label Setting Fan to Breeze Mode"
    return zigbee.writeAttribute(0x0202, 0x0000, 0x30, 06)
}

def fanSpeed1() {
    if (logInfo) log.info "$device.label Setting Fan Speed to Low"
    return zigbee.writeAttribute(0x0202, 0x0000, 0x30, 01)
}

def fanSpeed2() {
    if (logInfo) log.info "$device.label Setting Fan Speed to Medium"
    return zigbee.writeAttribute(0x0202, 0x0000, 0x30, 02)    
}

def fanSpeed3() {
    if (logInfo) log.info "$device.label Setting Fan Speed to Medium-High"
    return zigbee.writeAttribute(0x0202, 0x0000, 0x30, 03)    
}

def fanSpeed4() {
    if (logInfo) log.info "$device.label Setting Fan Speed to High"
    return zigbee.writeAttribute(0x0202, 0x0000, 0x30, 04)
}

def cycleSpeed(){
    def currentSpeed = device.currentValue("speed")
    if (currentSpeed == "off" || currentSpeed == "high" || currentSpeed == "auto"){
        fanSpeed1()
    }
    else if (currentSpeed == "low"){
        fanSpeed2()
    }
    else if (currentSpeed == "medium"){
        fanSpeed3()
    }
    else if (currentSpeed == "medium-high"){
        fanSpeed4()
    }
}

def cycleSpeedDown(){
    def currentSpeed = device.currentValue("speed")
    if (currentSpeed == "off" || currentSpeed == "low" || currentSpeed == "auto"){
        fanSpeed4()
    }
    else if (currentSpeed == "high"){
        fanSpeed3()
    }
    else if (currentSpeed == "medium-high"){
        fanSpeed2()
    }
    else if (currentSpeed == "medium"){
        fanSpeed1()
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
	device.updateSetting("logEnable", [value:"false",type:"bool"])
}

def refresh() {
    return zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.readAttribute(0x0202, 0x0000)
}
