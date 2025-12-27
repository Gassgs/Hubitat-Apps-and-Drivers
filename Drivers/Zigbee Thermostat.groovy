/** Zen or Lux Kono Zigbee Thermostat Driver w/ Added Tasmota relay for humidistat control
 *  Copyright 2018 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *  Updated & Customized for Hubitat - 3/22/2023  Gassgs --  updated for Zen thermostat 11/12/25 Gassgs
 * 
 *  Change History:
 *
 *  V1.0.0  03-22-2023       Port from Smart things, Added humidity & humidity setpoint (pushed from app)  
 *  V1.5.0  03-24-2023       Added fan default timer, added tracking filter change date and days
 *  V1.7.0  03-27-2023       Code cleanup and improved info logging
 *  V1.8.0  04-12-2023       Added tasmota ip relay device for humidistat option
 *  V1.9.0  04-22-2023       Improved filter handler and  attributes for Google Home
 *  V2.0.0  04-29-2023       Added Battery changed information tracking
 *  V2.1.0  05-30-2025       Added networkStatus attibute and changed "wifi" to "rssi" w/ capability 'Signal Strength'
 *  V2.2.0  11-08-2025       Added checks for humidity changes, updates and name change for Zen thermostat
 *  V2.3.0  11-22-2025       Added zigbee refresh on scheduled "ping"  
 */

def driverVer() { return "2.3" }

import groovy.json.JsonOutput
import hubitat.zigbee.zcl.DataType

metadata {
	definition (name: "Zen & Lux Kono Zigbee Thermostat", namespace: "Gassgs", author: "SmartThings", mnmn: "SmartThings", genericHandler: "Zigbee") {
		capability "Actuator"
		capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
		capability "Thermostat"
		capability "Thermostat Mode"
		capability "Thermostat Fan Mode"
		capability "Thermostat Cooling Setpoint"
		capability "Thermostat Heating Setpoint"
		capability "Thermostat Operating State"
		capability "Configuration"
		capability "Battery"
        capability "Power Source"
		capability "Refresh"
		capability "Sensor"
        capability "Signal Strength"
        
        attribute "filter","string"
        attribute "filterLife","number"
        attribute "humidistat","string"
        attribute "humiditySetpoint", "number"
        attribute "networkStatus","string"
        
        command "batteryChanged"
        command "filterChanged"
        command "ping"
        command "setHumidistatRelay", [[name:"Humidistat Relay", type: "ENUM",description: "Relay", constraints: ["on","off"]]]
        command "setHumiditySetpoint",[[name: "Humidity Setpoint",type: "NUMBER",description:"Target Humidity Level - 20% -50%",constraints: ["20..70"]]]
        command "setHumidity",[[name: "Humidity",type: "NUMBER",description:"Average Humidity Level"]]
        command "setThermostatMode", [[name:"Thermostat Mode", type: "ENUM",description: "Thermostat Mode", constraints: ["off","heat","cool","auto"]]]
        command "setThermostatFanMode", [[name:"Thermostat Fan Mode", type: "ENUM",description: "Thermostat Fan Mode", constraints: ["on","auto"]]]

		//Manually set driver
	}
}
    preferences{
        def refreshRate = [:]
        refreshRate << ["disabled" : "Disabled"]
	    refreshRate << ["5 min" : "Ping every 5 minutes"]
        refreshRate << ["10 min" : "Ping every 10 minutes"]
	    refreshRate << ["15 min" : "Ping every 15 minutes"]
	    refreshRate << ["30 min" : "Ping every 30 minutes"]
        input name: "fanTime", type: "number", title: "<b>Fan Run Time</b> *minutes", defaultValue: 5, required: true
        input name: "humidityTime", type: "number", title: "<b>Humidistat Run Time</b> *minutes", defaultValue: 10, required: true
        input name: "filterLifeTime", type: "number", title: "<b>Filter Life Time</b> *days", defaultValue: 90, required: true
        input name: "ipAddress", type: "string", title: "<b>Tasmota Relay Ip Address</b>", required: false
        input("pingRate", "enum", title: "<b>Tasmota Ping Interval</b>",options: refreshRate, defaultValue: "15 min", required: true )
        input name: "logInfo", type: "bool", title: "<b>Enable info logging</b>", defaultValue: true
        input name: "logEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: true
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    
        switch(pingRate) {
		case "5 min" :
			runEvery5Minutes(ping)
            if (logEnable) log.debug "Ping every 5 minutes schedule"
            if (logInfo) log.info "$device.label Ping every 5 minutes schedule"
			break
        case "10 min" :
			runEvery10Minutes(ping)
            if (logEnable) log.debug "Ping every 10 minutes schedule"
            if (logInfo) log.info "$device.label Ping every 10 minutes schedule"
			break
		case "15 min" :
			runEvery15Minutes(ping)
            if (logEnable) log.debug "Ping every 15 minutes schedule"
            if (logInfo) log.info "$device.label Ping every 15 minutes schedule"
			break
		case "30 min" :
			runEvery30Minutes(ping)
            if (logEnable) log.debug "Ping every 30 minutes schedule"
            if (logInfo) log.info "$device.label Ping every 30 minutes schedule"
            break
        }
    initialize()
}

def initialize(){
    if (state.filterChangedDays != null && state.batteryChangedDays != null){
        schedule('0 0 12 * * ?',addDay)
        checkFilterLife()
    }
	if (logEnable){
		log.info "Verbose logging has been enabled for the next 30 minutes."
		runIn(1800, logsOff)
	}
    ping()
}

////////Custom additions and tasmota humidistat relay code >>>

def setHumidistatRelay(data){
    if (data == "on"){
        relayOn()
    }
    else if (data == "off"){
        relayOff()
    }
    else{
        log.warn "$device.label $data is not valid"
    }
}

def relayOn() {
    if (logEnable) log.debug "Sending On Command to [${settings.ipAddress}]"
    try {
        httpGet("http://" + ipAddress + "/cm?cmnd=POWER%20On") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER == "ON"){
                if (logEnable) log.debug "Command Success response from Device"
                runIn(1,ping)
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def relayOff() {
    if (logEnable) log.debug "Sending Off Command to [${settings.ipAddress}]"
    try {
        httpGet("http://" + ipAddress + "/cm?cmnd=POWER%20Off") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER == "OFF"){
                if (logEnable) log.debug "Command Success response from Device"
                runIn(1,ping)
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def ping(){
        if(settings.ipAddress){
        if (logEnable) log.debug "Refreshing Device Status - [${settings.ipAddress}]"
        try {
           httpGet("http://" + ipAddress + "/cm?cmnd=status%200") { resp ->
           def json = (resp.data)
            if (logEnable) log.debug "${json}"
               if (json.containsKey("StatusSTS")){
                   status = json.StatusSTS.POWER as String
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logEnable) log.debug "Wifi signal strength $signal db"
                   sendEvent(name:"rssi",value:"${signal}")
                   sendEvent(name:"networkStatus",value:"online")
                   if (logEnable) log.debug "$device.label $ipAddress - $status Wifi signal strength $signal db"
                   if (logInfo) log.info  "$device.label Humidistat $status - Wifi signal strength $signal db"
                   if (status == "ON"){
                       sendEvent(name:"humidistat",value:"on")
                   }else{
                       sendEvent(name:"humidistat",value:"off")
                   }
               }
           }
    } catch (Exception e) {
            sendEvent(name:"networkStatus",value:"offline")
            if (logInfo) log.error "$device.label Unable to connect, device is <b>OFFLINE</b>"
            log.warn "Call to on failed: ${e.message}"
        }
    }
    refresh()
}

def setHumidity(data) {
    oldHum = device.currentValue("humidity")
    newHum = data as Number
    if (oldHum != newHum){
        if (logInfo) log.info "$device.label - Humidity Average is $data %"
        sendEvent(name:"humidity",value:"$data")
        runIn(1,checkHumidity)
    }   
}

def setHumiditySetpoint(data) {
    oldHum = device.currentValue("humiditySetpoint")
    newHum = data as Number
    if (data < 20 || data > 70){
        logDebug "setHumidity($data) is outside of valid range"
    }else{
        if (oldHum != newHum){
            if (logInfo) log.info "$device.label - Humidity Setpoint is $data %"
            sendEvent(name:"humiditySetpoint",value:"$data")
            runIn(1,checkHumidity)
        }
    }
}

def checkHumidity(){
    humidityAvg = device.currentValue("humidity")
    target = device.currentValue("humiditySetpoint")
    relayOn = device.currentValue("humidistat") == "on"
    if (humidityAvg < target-1){
        state.lowHumidity = true
        if (logInfo) log.info "$device.label -HUMIDITY LOW- Target Humidity is $target% Average Humidity is $humidityAvg%"
    }
    else{
        unschedule(relayOff)
        state.lowHumidity = false
        if (logInfo) log.info "$device.label - Target Humidity is $target% Average Humidity is $humidityAvg%"
        if (relayOn){
            relayOff()
        }
    }   
}

def addDay(){
    if (state.batteryChangedDays != null){
        state.batteryChangedDays = state.batteryChangedDays + 1
    }
    if (state.filterChangedDays != null){
        state.filterChangedDays = state.filterChangedDays + 1
        runIn(1,checkFilterLife)
    }
}

def checkFilterLife(){
    filterLife = new BigDecimal((filterLifeTime - state.filterChangedDays as Integer) / filterLifeTime *100).setScale(0,BigDecimal.ROUND_HALF_UP)
    if (logInfo) log.info "$device.label Filter Life percentage $filterLife %"
    sendEvent(name:"filterLife",value:"$filterLife")
    if (filterLife >= 66){
        sendEvent(name:"filter",value:"new")
    }
    else if (filterLife < 66 && filterLife >= 33){
        sendEvent(name:"filter",value:"good")
    }
    else if (filterLife < 33 && filterLife >= 1){
        sendEvent(name:"filter",value:"replace soon")
    }
    else{
        sendEvent(name:"filter",value:"replace now")
    }
}

def filterChanged(){
    now = new Date()
    dateFormat = new java.text.SimpleDateFormat("EE MMM d YYYY")
    timeFormat = new java.text.SimpleDateFormat("h:mm a")

    newDate = dateFormat.format(now)
    newTime = timeFormat.format(now)
    
    timeStamp = newDate + " " + newTime as String
    
    state.filterChanged = "$timeStamp"
    state.filterChangedDays = 0
    sendEvent(name:"filter",value:state.filterChangedDays as Number)
    initialize()
}

def batteryChanged(){
    now = new Date()
    dateFormat = new java.text.SimpleDateFormat("EE MMM d YYYY")
    timeFormat = new java.text.SimpleDateFormat("h:mm a")

    newDate = dateFormat.format(now)
    newTime = timeFormat.format(now)
    
    timeStamp = newDate + " " + newTime as String
    
    state.batteryChanged = "$timeStamp"
    state.batteryChangedDays = 0
    initialize()
}

/////////Kono/Zen Zigbee Driver code >>>

def parse(String description) {
	def map = zigbee.getEvent(description)
	def result
    
    result = parseAttrMessage(description)
	if (logEnable) log.debug "Description ${description} parsed to ${result}"

	return result
}

private parseAttrMessage(description) {
	def descMap = zigbee.parseDescriptionAsMap(description)
	def result = []
	List attrData = [[cluster: descMap.clusterInt, attribute: descMap.attrInt, value: descMap.value]]

	if (logEnable) log.debug "Desc Map: $descMap"

	descMap.additionalAttrs.each {
		attrData << [cluster: descMap.clusterInt, attribute: it.attrInt, value: it.value]
	}
	attrData.findAll( {it.value != null} ).each {
		def map = [:]
		if (it.cluster == THERMOSTAT_CLUSTER) {
			if (it.attribute == LOCAL_TEMPERATURE) {
				if (logEnable) log.debug "TEMP"
				map.name = "temperature"
				map.value = getTemperature(it.value)
				map.unit = temperatureScale
                if (logInfo) log.info "$device.label - Temperature is $map.value $map.unit"
			} else if (it.attribute == COOLING_SETPOINT) {
				if (logEnable) log.debug "COOLING SETPOINT"
				map.name = "coolingSetpoint"
				map.value = getTemperature(it.value)
				map.unit = temperatureScale
                if (logInfo) log.info "$device.label - Cooling Setpoint is $map.value $map.unit"
			} else if (it.attribute == HEATING_SETPOINT) {
				if (logEnable) log.debug "HEATING SETPOINT"
				map.name = "heatingSetpoint"
				map.value = getTemperature(it.value)
				map.unit = temperatureScale
                if (logInfo) log.info "$device.label - Heating Setpoint is $map.value $map.unit"
			} else if (it.attribute == THERMOSTAT_MODE || it.attribute == THERMOSTAT_RUNNING_MODE) {
				if (logEnable) log.debug "MODE"
				map.name = "thermostatMode"
				map.value = THERMOSTAT_MODE_MAP[it.value]
				map.data = [supportedThermostatModes: state.supportedThermostatModes]
                if (logInfo) log.info "$device.label - Mode - $map.value"
			} else if (it.attribute == THERMOSTAT_RUNNING_STATE) {
				if (logEnable) log.debug "RUNNING STATE"
				def intValue = hexToInt(it.value) as int
				map.name = "thermostatOperatingState"
				if (intValue & 0x01) {
					map.value = "heating"
                    humidistatOff = device.currentValue("humidistat") == "off"
                    if (state.lowHumidity && humidistatOff){
                        relayOn()
                        if (logInfo) log.info "$device.label - Operating State - $map.value with Humidity for $humidityTime minutes"
                        runIn(humidityTime*60,relayOff)
                    }
				} else if (intValue & 0x02) {
					map.value = "cooling"
				} else if (intValue & 0x04) {
					map.value = "fan only"
				} else {
					map.value = "idle"
				}
                if (logInfo) log.info "$device.label - Operating State - $map.value"
			} else if (it.attribute == CONTROL_SEQUENCE_OF_OPERATION) {
				if (logEnable) log.debug "CONTROL SEQUENCE OF OPERATION"
				state.supportedThermostatModes = CONTROL_SEQUENCE_OF_OPERATION_MAP[it.value]
				map.name = "supportedThermostatModes"
				map.value = JsonOutput.toJson(CONTROL_SEQUENCE_OF_OPERATION_MAP[it.value])
			}
			else if (it.attribute == THERMOSTAT_SYSTEM_CONFIG) {
				if (logEnable) log.debug "THERMOSTAT SYSTEM CONFIG"
				def intValue = hexToInt(it.value) as int
				def cooling = 		 intValue & 0b00000011
				def heating = 		(intValue & 0b00001100) >>> 2
				def heatingType = 	(intValue & 0b00010000) >>> 4
				def supportedModes = ["off"]

				if (cooling != 0x03) {
					supportedModes << "cool"
				}
				if (heating != 0x03) {
					supportedModes << "heat"
				}
				if (!isLuxKONOZ() && supportedModes.contains("cool") && supportedModes.contains("heat")) {
					supportedModes << "auto"
				}
				if ((heating == 0x01 || heating == 0x02) && heatingType == 1) {
					supportedModes << "emergency heat"
				}
				if (logEnable) log.debug "supported modes: $supportedModes"
				state.supportedThermostatModes = supportedModes
				map.name = "supportedThermostatModes"
				map.value = JsonOutput.toJson(supportedModes)
			}
		} else if (it.cluster == FAN_CONTROL_CLUSTER) {
			if (it.attribute == FAN_MODE) {
				if (logEnable) log.debug "FAN MODE"
				map.name = "thermostatFanMode"
				map.value = FAN_MODE_MAP[it.value]
                if (map.value == "on"){
                    runIn(fanTime*60,fanAuto)
                }
                else if (map.value == "auto"){
                    unschedule(fanAuto)
                }
				map.data = [supportedThermostatFanModes: state.supportedFanModes]
                if (logInfo) log.info "$device.label - Fan Mode is $map.value"
			} else if (it.attribute == FAN_MODE_SEQUENCE) {
				if (logEnable) log.debug "FAN MODE SEQUENCE"
				map.name = "supportedThermostatFanModes"
				map.value = JsonOutput.toJson(FAN_MODE_SEQUENCE_MAP[it.value])
				state.supportedFanModes = FAN_MODE_SEQUENCE_MAP[it.value]
			}
		} else if (it.cluster == zigbee.POWER_CONFIGURATION_CLUSTER) {
            if (it.attribute == BATTERY_VOLTAGE) {
				map = getBatteryPercentage(Integer.parseInt(it.value, 16))
			} else if (it.attribute == BATTERY_PERCENTAGE_REMAINING) {
				map.name = "battery"
				map.value = Math.min(100, Integer.parseInt(it.value, 16))
			} else if (it.attribute == BATTERY_ALARM_STATE) {
				map = getPowerSource(it.value)
			}
		}
		if (map) {
			result << createEvent(map)
		}
	}
	return result
}

def installed() {
	//sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    state.supportedThermostatModes = ["off", "heat", "cool", "auto"]
    state.supportedFanModes = ["on", "auto"]
    sendEvent(name: "supportedThermostatFanModes", value: JsonOutput.toJson(state.supportedFanModes), displayed: false)
	sendEvent(name: "supportedThermostatModes", value: JsonOutput.toJson(state.supportedThermostatModes), displayed: false)
}

def refresh() {
	// THERMOSTAT_SYSTEM_CONFIG is an optional attribute. It we add other thermostats we need to determine if they support this and behave accordingly.
	return zigbee.readAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_SYSTEM_CONFIG) +
			zigbee.readAttribute(FAN_CONTROL_CLUSTER, FAN_MODE_SEQUENCE) +
			zigbee.readAttribute(THERMOSTAT_CLUSTER, LOCAL_TEMPERATURE) +
			zigbee.readAttribute(THERMOSTAT_CLUSTER, COOLING_SETPOINT) +
			zigbee.readAttribute(THERMOSTAT_CLUSTER, HEATING_SETPOINT) +
			zigbee.readAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE) +
			zigbee.readAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_RUNNING_STATE) +
			zigbee.readAttribute(FAN_CONTROL_CLUSTER, FAN_MODE) +
			zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, BATTERY_ALARM_STATE) +
			getBatteryRemainingCommand()
}

def getBatteryRemainingCommand() {
    zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, BATTERY_VOLTAGE)
}

def configure() {
    installed()
	//def startValues = zigbee.writeAttribute(THERMOSTAT_CLUSTER, HEATING_SETPOINT, DataType.INT16, 0x07D0) + zigbee.writeAttribute(THERMOSTAT_CLUSTER, COOLING_SETPOINT, DataType.INT16, 0x0A28)
	//return startValues + zigbee.batteryConfig() + refresh()
    return zigbee.batteryConfig() + refresh()
}

def getBatteryPercentage(rawValue) {
	def result = [:]

	result.name = "battery"

	if (rawValue == 0) {
		sendEvent(name: "powerSource", value: "mains", descriptionText: "${device.displayName} is connected to mains")
		result.value = 100
		result.descriptionText = "${device.displayName} is powered by external source."
	} else {
		def volts = rawValue / 10
		def minVolts = voltageRange.minVolts
		def maxVolts = voltageRange.maxVolts
		def pct = (volts - minVolts) / (maxVolts - minVolts)
		def roundedPct = Math.round(pct * 100)
		if (roundedPct < 0) {
			roundedPct = 0
		}
		result.value = Math.min(100, roundedPct)
	}
    if (logInfo) log.info "$device.label - Battery - $result.value %" 
	return result
}

def getVoltageRange() {
    [minVolts: 5, maxVolts: 6.5]
}

def getTemperature(value) {
	if (value != null) {
		def celsius = Integer.parseInt(value, 16) / 100
		if (temperatureScale == "C") {
			return celsius.toDouble().round(1)
		} else {
			return Math.round(celsiusToFahrenheit(celsius))
		}
	}
}

def getPowerSource(value) {
	def result = [name: "powerSource"]
	switch (value) {
		case "40000000":
			result.value = "battery"
			result.descriptionText = "${device.displayName} is powered by batteries"
			break
        case "00000000":
			result.value = "battery"
			result.descriptionText = "${device.displayName} is powered by batteries"
			break
		default:
			result.value = "mains"
			result.descriptionText = "${device.displayName} is connected to mains"
			break
	}
	return result
}

def setThermostatMode(mode) {
	if (logEnable) log.debug "set mode $mode (supported ${state.supportedThermostatModes})"
	if (state.supportedThermostatModes?.contains(mode)) {
		switch (mode) {
			case "heat":
				heat()
				break
			case "cool":
				cool()
				break
			case "auto":
				auto()
				break
			case "emergency heat":
				emergencyHeat()
				break
			case "off":
				off()
				break
		}
	} else {
		if (logEnable) log.debug "Unsupported mode $mode"
	}
}

def setThermostatFanMode(mode) {
	if (mode == "auto") {
        fanAuto()
    }
    else if (mode == "on"){
        fanOn()
	} else {
		if (logEnable) log.debug "Unsupported fan mode $mode"
	}
}

def fanCirculate(){
    fanOn()
}

def off() {
	return zigbee.writeAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE, DataType.ENUM8, THERMOSTAT_MODE_OFF) +
			zigbee.readAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE)
}

def auto() {
	return zigbee.writeAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE, DataType.ENUM8, THERMOSTAT_MODE_AUTO) +
			zigbee.readAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE)
}

def cool() {
	return zigbee.writeAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE, DataType.ENUM8, THERMOSTAT_MODE_COOL) +
			zigbee.readAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE)
}

def heat() {
	return zigbee.writeAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE, DataType.ENUM8, THERMOSTAT_MODE_HEAT) +
			zigbee.readAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE)
}

//def emergencyHeat() {
	//return zigbee.writeAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE, DataType.ENUM8, THERMOSTAT_MODE_EMERGENCY_HEAT) +
			//zigbee.readAttribute(THERMOSTAT_CLUSTER, THERMOSTAT_MODE)
//}

def fanAuto() {
	return zigbee.writeAttribute(FAN_CONTROL_CLUSTER, FAN_MODE, DataType.ENUM8, FAN_MODE_AUTO) +
			zigbee.readAttribute(FAN_CONTROL_CLUSTER, FAN_MODE)
}

def fanOn() {
	return zigbee.writeAttribute(FAN_CONTROL_CLUSTER, FAN_MODE, DataType.ENUM8, FAN_MODE_ON) +
			zigbee.readAttribute(FAN_CONTROL_CLUSTER, FAN_MODE)
}

private setSetpoint(degrees, setpointAttr, degreesMin, degreesMax) {
	if (degrees != null && setpointAttr != null && degreesMin != null && degreesMax != null) {
		def normalized = Math.min(degreesMax as Double, Math.max(degrees as Double, degreesMin as Double))
		def celsius = (temperatureScale == "C") ? normalized : fahrenheitToCelsius(normalized)
		celsius = (celsius as Double).round(2)

		return zigbee.writeAttribute(THERMOSTAT_CLUSTER, setpointAttr, DataType.INT16, hex(celsius * 100)) +
				zigbee.readAttribute(THERMOSTAT_CLUSTER, setpointAttr)
	}
}

def setCoolingSetpoint(degrees) {
	setSetpoint(degrees, COOLING_SETPOINT, coolingSetpointRange[0], coolingSetpointRange[1])
}

def setHeatingSetpoint(degrees) {
	setSetpoint(degrees, HEATING_SETPOINT, heatingSetpointRange[0], heatingSetpointRange[1])
}

private hex(value) {
	return new BigInteger(Math.round(value).toString()).toString(16)
}

private hexToInt(value) {
	new BigInteger(value, 16)
}

private boolean isZen() {
	device.getDataValue("model") == "Zen-01"
}

private boolean isLuxKONOZ() {
	device.getDataValue("model") == "KONOZ"
}

private boolean isDanfossAlly() {
	device.getDataValue("model") == "eTRV0100"
}

private boolean isPOPP() {
	device.getDataValue("model") == "eT093WRO"
}

// TODO: Get these from the thermostat; for now they are set to match the UI metadata
def getCoolingSetpointRange() {
	(getTemperatureScale() == "C") ? [10, 35] : [50, 95]
}
def getHeatingSetpointRange() {
	if (isDanfossAlly() || isPOPP()) {
		(getTemperatureScale() == "C") ? [4, 35] : [39, 95]
	} else {
		(getTemperatureScale() == "C") ? [7.22, 32.22] : [45, 90]
	}
}

private getTHERMOSTAT_CLUSTER() { 0x0201 }
private getLOCAL_TEMPERATURE() { 0x0000 }
private getTHERMOSTAT_SYSTEM_CONFIG() { 0x0009 } // Optional attribute
private getCOOLING_SETPOINT() { 0x0011 }
private getHEATING_SETPOINT() { 0x0012 }
private getMIN_HEAT_SETPOINT_LIMIT() { 0x0015 }
private getMAX_HEAT_SETPOINT_LIMIT() { 0x0016 }
private getTHERMOSTAT_RUNNING_MODE() { 0x001E }
private getCONTROL_SEQUENCE_OF_OPERATION() { 0x001B } // Mandatory attribute
private getCONTROL_SEQUENCE_OF_OPERATION_MAP() {
	[
		"00":["off", "cool"],
		"01":["off", "cool"],
		// 0x02, 0x03, 0x04, and 0x05 don't actually guarentee emergency heat; to learn this, one would
		// try THERMOSTAT_SYSTEM_CONFIG (optional), which we default to for the LUX KONOz since it supports THERMOSTAT_SYSTEM_CONFIG
		"02":["off", "heat", "emergency heat"],
		"03":["off", "heat", "emergency heat"],
		"04":["off", "heat", "auto", "cool", "emergency heat"],
		"05":["off", "heat", "auto", "cool", "emergency heat"]
	]
}
private getTHERMOSTAT_MODE() { 0x001C }
private getTHERMOSTAT_MODE_OFF() { 0x00 }
private getTHERMOSTAT_MODE_AUTO() { 0x01 }
private getTHERMOSTAT_MODE_COOL() { 0x03 }
private getTHERMOSTAT_MODE_HEAT() { 0x04 }
private getTHERMOSTAT_MODE_EMERGENCY_HEAT() { 0x05 }
private getTHERMOSTAT_MODE_MAP() {
	[
		"00":"off",
		"01":"auto",
		"03":"cool",
		"04":"heat",
		"05":"emergency heat"
	]
}
private getTHERMOSTAT_RUNNING_STATE() { 0x0029 }
private getSETPOINT_RAISE_LOWER_CMD() { 0x00 }
private getFAN_CONTROL_CLUSTER() { 0x0202 }
private getFAN_MODE() { 0x0000 }
private getFAN_MODE_SEQUENCE() { 0x0001 }
private getFAN_MODE_SEQUENCE_MAP() {
	[
		"00":["low", "medium", "high"],
		"01":["low", "high"],
		"02":["low", "medium", "high", "auto"],
		"03":["low", "high", "auto"],
		"04":["on", "auto"],
	]
}
private getFAN_MODE_ON() { 0x04 }
private getFAN_MODE_AUTO() { 0x05 }
private getFAN_MODE_MAP() {
	[
		"04":"on",
		"05":"auto"
	]
}
private getBATTERY_VOLTAGE() { 0x0020 }
private getBATTERY_PERCENTAGE_REMAINING() { 0x0021 }
private getBATTERY_ALARM_STATE() { 0x003E }
