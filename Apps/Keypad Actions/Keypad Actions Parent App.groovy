/**
 *  ****************  Keypad Actions Parent App ****************
 *
 *  Use 4 digit codes to run actions
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
 *  V1.0.0  -       3-04-2021       First run
 *
 */

import groovy.transform.Field

definition(
    name: "Keypad Actions",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Run actions triggered by 4 digit codes",
    category: "",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences{
    page(
        name: "mainPreferences",
        title: "<div style='text-align:center'><b><big>Keypad Actions</big></b></div>",
        install: true,
        uninstall: true)
        {
        section{
            app(
                name: "childApps",
                appName: "Keypad Actions Child",
                namespace: "Gassgs",
                title: "<b>Add New Keypad Action</b>",
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
