 /**
 //**********Multi Sensor Plus********
 *
 * Average: Temperature, Humidity, and Illuminance   - 
 * Group:  Locks, Contact, Motion, Water, Presence, and Sound Sensors  -
 * Plus  a Virtual Switch  -  All  In One Device
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
 *  Last Update: 1/26/2021
 *
 *  Changes:
 *
 *  V1.0.0  -       1-09-2021       First run
 *  V2.0.0  -       1-11-2021       Improvements
 *  V2.1.0  -       1-26-2021       Code Cleanup
 *  V2.2.0  -       7-05-2021       Added import url
 *
 */

import groovy.transform.Field

definition(
    name: "Multi Sensor Plus",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Average: Temperature, Humidity, and Illuminance  -"+
    "Group:  Locks, Contact, Motion, Water, Presence, and Sound Sensors   -"+
    "Plus  a Virtual Switch  -  All  In One Device",
    category: "",
    importUrl: "https://raw.githubusercontent.com/Gassgs/Hubitat-Apps-and-Drivers/master/Apps/Multi%20Sensor%20Plus/Multi%20Sensor%20Plus%20Parent%20app.groovy",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences{
    page(
        name: "mainPreferences",
        title: "<div style='text-align:center'><b>Multi Sensor Plus</b></div>",
        install: true,
        uninstall: true)
        {
        section{
            app(
                name: "childApps",
                appName: "Multi Sensor Plus Child",
                namespace: "Gassgs",
                title: "Add New Multi Sensor Plus Rule",
                multiple: true
                )
        }
        displayFooter()
    }
}

def displayFooter(){
	section(){
		paragraph "<div style='color:#1A77C9;text-align:center'>Multi Sensor Plus"+
        "<br><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=CUVJA2MQB5TCW&source=url' target"+
        "='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border="+
        "'0' alt='PayPal Logo'></a><br><br>Donations are not necessary but always appreciated! -"+
        "Please click <b>Done</b> to install the parent app</div>"
	}
}

def uninstalled(){
    getChildApps().each { childApp ->
        deleteChildApp(childApp.id)
    }
}
