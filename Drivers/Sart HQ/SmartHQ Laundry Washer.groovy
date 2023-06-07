/*

Copyright 2022 - tomw

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------

Change history:

0.9.6 - gassgs - Washer specific driver
0.9.5 - tomw - Improved Washer/Dryer support (in SmartHQ Laundry driver)
0.9.2 - tomw - Make child logging follow system device setting
0.9.0 - tomw - Initial release

*/

metadata 
{
    definition(name: "SmartHQ Laundry Washer", namespace: "tomw", author: "tomw") 
    {
        capability "Actuator"
        capability "ContactSensor"
        capability "Refresh"
        
        command "setWasherCycle", [[name: "Cycle*", type:"ENUM", description:"Cycle mode to set", constraints:["Normal","Whites","Delicates","Jeans","Active Wear","Towels /Sheets","Bulky Items","Quick Wash","Sanitize With Oxi","Drain and spin"]],[name: "Stain Removal*", type:"ENUM", description:"Stain Option", constraints:["off","blood","grass","dirt","tomato","beverages","oily"]]]
        command "start"
        command "stop"
        
        attribute "delayRemaining", "number"
        attribute "doorLock", "enum", ["unlocked", "locked"]
        attribute "doorStatus", "enum", ["open", "closed"]
        attribute "machineState", "string"
        attribute "smartDispenseTankStatus", "string"
        attribute "timeRemaining", "number"
        attribute "controlPanel", "enum", ["unlocked", "locked"]
        attribute "cycleName", "string"
        attribute "soilLevel", "string"
        attribute "tempLevel", "string"
        attribute "spinOption", "string"
        attribute "rinseOption", "string"
        attribute "tangleControl", "string"
        attribute "deepFill", "string"
        attribute "autoSoak", "string"
    }
}

#include tomw.smarthqHelpers

def refresh()
{
    refreshAppliance()
}

def parse(item)
{
    if(!item)
    {
        return
    }
    
    logDebug(item)
    
    switch(item.erd?.toLowerCase())
    {
        case LAUNDRY_DOOR:
            parseDoorStatusByte(item.value, doorNormallyOpen)
            break
        
        case LAUNDRY_TIME_REMAINING:
            def timeRemMins = (decodeErdInt(hubitat.helper.HexUtils.hexStringToByteArray(item.value)) / 60).toInteger()
            sendEvent(name: "timeRemaining", value: timeRemMins)
            break
        
        case LAUNDRY_CYCLE:
            cycle = decodeErdInt(hubitat.helper.HexUtils.hexStringToByteArray(item.value))
            parseCycleState(item.value)
            break
        
        case LAUNDRY_MACHINE_STATE:
            parseMachineState(item.value)
            break
        
        case LAUNDRY_CONTROL_PANEL_LOCK:
            parseControlPanel(item.value)
            break
        
        case LAUNDRY_WASHER_DOOR_LOCK:
            parseDoorLock(item.value)
            break
        
        case LAUNDRY_DELAY_TIME_REMAINING:
            def timeRemMins = decodeErdInt(hubitat.helper.HexUtils.hexStringToByteArray(item.value))
            sendEvent(name: "delayRemaining", value: timeRemMins)
            break
        
        case LAUNDRY_WASHER_SMART_DISPENSE_TANK_STATUS:
            parseSmartDispenseTankStatus(item.value)
            break
        
        case LAUNDRY_WASHER_SOIL_LEVEL:
            parseSoilLevel(item.value)
            break
        
        case LAUNDRY_WASHER_WASHTEMP_LEVEL:
            parseTempLevel(item.value)
            break
        
        case LAUNDRY_WASHER_SPINTIME_LEVEL:
            parseSpinOption(item.value)
            break
        
        case LAUNDRY_WASHER_RINSE_OPTION:
            parseRinseOption(item.value)
            break
        
        case LAUNDRY_WASHER_TANGLECONTROL:
            parseTangleControl(item.value)
            break
        
        case LAUNDRY_WASHER_DEEPFILL:
            parseDeepFill(item.value)
            break
        
        case LAUNDRY_WASHER_AUTOSOAK:
            parseAutoSoak(item.value)
            break
    }
}

def parseMachineState(value)
{
    def bytes = hubitat.helper.HexUtils.hexStringToByteArray(value)    
    sendEvent(name: "machineState", value: machineStates[decodeErdInt(subBytes(bytes, 0, 1))] ?: "unknown")
}

def parseCycleState(value)
{
    def bytes = hubitat.helper.HexUtils.hexStringToByteArray(value)
    sendEvent(name: "cycleName", value: cycleNames[decodeErdInt(subBytes(bytes, 0, 1))] ?: "unknown")
}

def parseDoorLock(value)
{
    def doorLock = decodeErdBool(hubitat.helper.HexUtils.hexStringToByteArray(value)?.getAt(0)) ? "locked" : "unlocked"
    sendEvent(name: "doorLock", value: doorLock)    
}

def parseTangleControl(value)
{
    def status = decodeErdBool(hubitat.helper.HexUtils.hexStringToByteArray(value)?.getAt(0)) ? "on" : "off"
    sendEvent(name: "tangleControl", value: status)
}

def parseDeepFill(value)
{
    def status = decodeErdBool(hubitat.helper.HexUtils.hexStringToByteArray(value)?.getAt(0)) ? "on" : "off"
    sendEvent(name: "deepFill", value: status)
}

def parseSmartDispenseTankStatus(value)
{
    def bytes = hubitat.helper.HexUtils.hexStringToByteArray(value)    
    sendEvent(name: "smartDispenseTankStatus", value: smartDispenseTankStatuses[decodeErdInt(subBytes(bytes, 0, 1))] ?: "unknown")
}

def parseControlPanel(value)
{
    value = value as String
    if (value == "01" || value == "00"){
        sendEvent(name: "controlPanel", value: "unlocked")  
    }
    else if (value == "02"){
        sendEvent(name: "controlPanel", value: "locked")
    }
    else{
        sendEvent(name: "controlPanel", value: "unknown")
    }
}

def parseSoilLevel(value)
{
    def bytes = hubitat.helper.HexUtils.hexStringToByteArray(value)    
    sendEvent(name: "soilLevel", value: soilLevel[decodeErdInt(subBytes(bytes, 0, 1))] ?: "unknown")
}

def parseTempLevel(value)
{
    def bytes = hubitat.helper.HexUtils.hexStringToByteArray(value)    
    sendEvent(name: "tempLevel", value: tempLevel[decodeErdInt(subBytes(bytes, 0, 1))] ?: "unknown")
}

def parseSpinOption(value)
{
    def bytes = hubitat.helper.HexUtils.hexStringToByteArray(value)    
    sendEvent(name: "spinOption", value: spinOption[decodeErdInt(subBytes(bytes, 0, 1))] ?: "unknown")
}

def parseRinseOption(value)
{
    def bytes = hubitat.helper.HexUtils.hexStringToByteArray(value)    
    sendEvent(name: "rinseOption", value: rinseOption[decodeErdInt(subBytes(bytes, 0, 1))] ?: "unknown")
}

def parseAutoSoak(value)
{
    def bytes = hubitat.helper.HexUtils.hexStringToByteArray(value)    
    sendEvent(name: "autoSoak", value: autoSoakTime[decodeErdInt(subBytes(bytes, 0, 1))] ?: "unknown")
}


//////////////Commands/////////
def start()
{
    def ctrlMap = buildAppCtrlSetter(buildDevDetails(), buildCmdDetails("washer-start-cycle"))
    
    parent?.sendWssMap(ctrlMap)
}

def stop()
{
    def ctrlMap = buildAppCtrlSetter(buildDevDetails(), buildCmdDetails("washer-stop-cycle"))
    
    parent?.sendWssMap(ctrlMap)
}

def setWasherCycle(cycle,data)
{
    value = cycle as String 
    if (value == "Sanitize With Oxi" || value == "Drain and spin" || value == "Towels /Sheets"){
        body = [[name: "cycle", value: cycle]]
    }else{
        body = [[name: "cycle", value: cycle],[name: "stain-removal-guide", value: data]]
    }
    def ctrlMap = buildAppCtrlSetter(buildDevDetails(), buildCmdDetails("washer-set-state", body))
    
    parent?.sendWssMap(ctrlMap)
}

import groovy.transform.Field

@Field Map machineStates = 
    [
        0: "idle",
        1: "standby",
        2: "running",
        3: "paused",
        4: "end_of_cycle",
        5: "dsm_delay_run",
        6: "delay_run",
        7: "delay_pause",
        8: "drain_timeout",
        9: "commissioning",
        10: "bulk_flush",
        128: "clean_speak"
    ]

@Field Map smartDispenseTankStatuses =
    [
        2: "full",
        1: "low",
        0: "unknown"
    ]

@Field Map cycleNames = 
    [
        0: "Not Defined",
        1: "Basket Clean",
        2: "Drain and Spin",
        3: "Quick Rinse",
        4: "Bulky Items",
        5: "Sanitize",
        6: "Towels /Sheets",
        7: "Steam Refresh",
        8: "Normal/Mixed load",
        9: "Whites",
        10: "Dark Colors",
        11: "Jeans",
        12: "Hand Wash",
        13: "Delicates",
        14: "Speed Wash",
        15: "Heavy Duty",
        16: "Allergen",
        17: "Power Clean",
        18: "Rinse and Spin",
        19: "Single Item Wash",
        20: "Colors",
        21: "Cold Wash",
        22: "Water On Demand",
        23: "Tub Clean",
        24: "Casuals with Steam",
        25: "Stain Wash with Steam",
        26: "Deep Clean",
        27: "Bulky Bedding",
        28: "Normal",
        29: "Quick Wash",
        30: "Sanitize with Oxi",
        31: "Self Clean",
        32: "Towels",
        33: "Soak",
        34: "Wool",
        35: "Ultra Fresh Vent",
        36: "Sanitize + Allergen",
        37: "Spin Only",
        38: "Everyday",
        39: "Soft Toys",
        40: "Sneakers",
        41: "Synthetics",
        42: "Silk",
        43: "Denim",
        44: "Drum Clean",
        45: "Sheets",
        46: "Quick 15",
        47: "Quick 30",
        48: "Easy Iron",
        49: "Sports",
        50: "Eco 40-60",
        51: "20°C",
        52: "Warm Wash",
        53: "Hot Wash",
        54: "Swim Wear",
        55: "Eco",
        56: "Express",
        57: "Mix",
        58: "Quick Cycle",
        128: "Cottons",
        129: "Easy Care",
        130: "Active Wear",
        131: "Timed Dry",
        132: "DeWrinkle",
        133: "Quick/Air Fluff",
        134: "Steam Refresh",
        135: "Steam Dewrinkle",
        136: "Speed Dry",
        137: "Mixed",
        138: "Quick dry",
        139: "Casuals",
        140: "Warm up",
        141: "Energy Saver",
        142: "Antibacterial",
        143: "Rack Dry",
        144: "Baby Care",
        145: "Auto Dry",
        146: "Auto Extra Dry",
        147: "Perm Press",
        148: "Washer Link",
        149: "Auto Damp Dry",
        150: "Smart Vent",
        151: "Pre Iron",
        152: "Hygiene",
        153: "Cool Air",
        154: "Outdoor",
        155: "Ultra Delicate",
        156: "Scent",
        157: "Sanitize Steam",
        158: "Durable",
        159: "Shoes",
        160: "Shirts",
        161: "Refresh",
        162: "Freshen",
        163: "Eco Cool",
        164: "Rinse + Dry",
        165: "Leather",
        166: "Outerwear",
        167: "Mixed Refresh",
        168: "Shirts Refresh",
        169: "Delicate Refresh",
        170: "Sanitise Refresh",
        255: "Reset"
    ]

@Field Map soilLevel =
    [
        0: "off",
        1: "extra light",
        2: "light",
        3: "normal",
        4: "heavy",
        5: "extra heavy"
    ]

@Field Map tempLevel =
    [
        0: "tapCold",
        1: "cold",
        2: "warm",
        3: "hot",
        4: "extra hot",
        5: "invalid",
        16: "tap cold",
        17: "cold",
        18: "cool",
        19: "colors",
        20: "warm",
        21: "hot",
        22: "extra hot"
    ]
@Field Map spinOption =
    [
        0: "no spin",
        1: "low",
        2: "medium",
        3: "high",
        4: "extra high",
        5: "invalid",
        6: "max"
    ]
@Field Map rinseOption =
    [
        0: "single",
        1: "deep rinse",
        2: "extra",
        3: "max"
    ]

@Field Map autoSoakTime =
    [
        0: "off",
        1: "15 min",
        2: "30 min",
        3: "1 hr",
        4: "2 hr"
    ]

@Field LAUNDRY_MACHINE_STATE = "0x2000"
@Field LAUNDRY_SUB_CYCLE = "0x2001"
@Field LAUNDRY_END_OF_CYCLE = "0x2002"
@Field LAUNDRY_TIME_REMAINING = "0x2007"
@Field LAUNDRY_WASHER_TANK_STATUS = "0x2008"
@Field LAUNDRY_WASHER_TANK_SELECTED = "0x2009"
@Field LAUNDRY_DELAY_TIME_REMAINING = "0x2010"
@Field LAUNDRY_DOOR = "0x2012"
@Field LAUNDRY_WASHER_DOOR_LOCK = "0x2013"
@Field LAUNDRY_CYCLE = "0x200a"
@Field LAUNDRY_DRYER_DRYNESS_LEVEL = "0x201a"
@Field LAUNDRY_DRYER_TUMBLE_STATUS = "0x201b"
@Field LAUNDRY_DRYER_LEVEL_SENSOR_DISABLED = "0x201c"
@Field LAUNDRY_UNKNOWN201D = "0x201d"
@Field LAUNDRY_WASHER_SOIL_LEVEL = "0x2015"
@Field LAUNDRY_WASHER_WASHTEMP_LEVEL = "0x2016"
@Field LAUNDRY_WASHER_SPINTIME_LEVEL = "0x2017"
@Field LAUNDRY_WASHER_RINSE_OPTION = "0x2018"
@Field LAUNDRY_DRYER_TEMPERATURE_OPTION = "0x2019"
@Field LAUNDRY_DRYER_SHEET_USAGE_CONFIGURATION = "0x2022"
@Field LAUNDRY_DRYER_SHEET_INVENTORY = "0x2023"
@Field LAUNDRY_REMOTE_DELAY_CONTROL = "0x2038"
@Field LAUNDRY_REMOTE_STATUS = "0x2039"
@Field LAUNDRY_WASHER_SMART_DISPENSE_TANK_STATUS = "0x203c"
@Field LAUNDRY_WASHER_SMART_DISPENSE = "0x203d"
@Field LAUNDRY_WASHER_UNKNOWN203E = "0x203e"
@Field LAUNDRY_REMOTE_POWER_CONTROL = "0x2040"
@Field LAUNDRY_UNKNOWN2041 = "0x2041"
@Field LAUNDRY_DRYER_UNKNOWN2045 = "0x2045"
@Field LAUNDRY_DRYER_UNKNOWN2046 = "0x2046"
@Field LAUNDRY_DRYER_UNKNOWN2047 = "0x2047"
@Field LAUNDRY_DRYER_UNKNOWN2049 = "0x2049"
@Field LAUNDRY_DRYER_DAMP_ALERT = "0x204a"
@Field LAUNDRY_DRYER_UNKNOWN204C = "0x204c"
@Field LAUNDRY_DRYER_DRYNESSNEW_LEVEL = "0x204d"
@Field LAUNDRY_DRYER_UNKNOWN204F = "0x204f"
@Field LAUNDRY_DRYER_TEMPERATURENEW_OPTION = "0x2050"
@Field LAUNDRY_DRYER_UNKNOWN2051 = "0x2051"
@Field LAUNDRY_DRYER_ECODRY_STATUS = "0x2052"
@Field LAUNDRY_DRYER_TUMBLENEW_STATUS = "0x2053"
@Field LAUNDRY_WASHER_UNKNOWN2054 = "0x2054"
@Field LAUNDRY_WASHER_TIMESAVER = "0x2055"
@Field LAUNDRY_WASHER_UNKNOWN2057 = "0x2057"
@Field LAUNDRY_WASHER_POWERSTEAM = "0x2058"
@Field LAUNDRY_WASHER_PREWASH = "0x205b"
@Field LAUNDRY_DRYER_UNKNOWN205D = "0x205d"
@Field LAUNDRY_DRYER_REDUCE_STATIC = "0x205e"
@Field LAUNDRY_DRYER_UNKNOWN205F = "0x205f"
@Field LAUNDRY_WASHER_UNKNOWN2060 = "0x2060"
@Field LAUNDRY_WASHER_TUMBLECARE = "0x2061"
@Field LAUNDRY_WASHER_UNKNOWN2069 = "0x2069"
@Field LAUNDRY_DRYER_WASHERLINK_CYCLE = "0x206b"
@Field LAUNDRY_DRYER_WASHERLINK_STATUS = "0x206c"
@Field LAUNDRY_DRYER_WASHERLINK_CONTROL = "0x206e"
@Field LAUNDRY_DRYER_UNKNOWN206F = "0x206f"
@Field LAUNDRY_WASHER_UNKNOWN2070 = "0x2070"
@Field LAUNDRY_WASHER_UNKNOWN2072 = "0x2072"
@Field LAUNDRY_CONTROL_PANEL_LOCK = "0x2083"
@Field LAUNDRY_WASHER_TANGLECONTROL = "0x20a4"
@Field LAUNDRY_WASHER_AUTOSOAK = "0x20ab"
@Field LAUNDRY_WASHER_DEEPFILL = "0x20ad"
