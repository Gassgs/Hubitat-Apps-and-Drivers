/**
 *  ****************  Winter Humidity Control Parent App ****************
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
 *  V1.0.0 -        1-29-2021       First run
 *  V1.1.0 -        2-16-2021       Added two child apps
 */

import groovy.transform.Field

definition(
    name: "Winter Humidity Control",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Calculates target humidity based on an outdoor temperature sensor"+
    " Control a switch or relay when humidity sensors report lower than tartget in the home."+
    " Optional motion activity restriction with humidifier and option to control ceiling fans"+
    " with humidistat control",
    category: "",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences{
    page(
        name: "mainPreferences",
        title: "<div style='text-align:center'><b><big>Winter Humidistat Control</b></big></div>",
        install: true,
        uninstall: true)
        {
        section{
            app(
                name: "childApps",
                appName: "Winter Humidistat Control Child",
                namespace: "Gassgs",
                title: "<b>Add Winter Humidistat Control Rule</b>",
                multiple: true
                )
        }
        section{
            app(
                name: "childApps",
                appName: "Winter Humidifier Control Child",
                namespace: "Gassgs",
                title: "<b>Add Winter Humidifier Control Rule</b>",
                multiple: true
                )
        }
    }
}

def uninstalled(){
    getChildApps().each { childApp ->
        deleteChildApp(childApp.id)
    }
}
