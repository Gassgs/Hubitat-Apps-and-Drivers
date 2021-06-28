/**
 *  Neato (Connect)
 *
 *  Copyright 2016,2017,2018,2019,2020 Alex Lee Yuk Cheung
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  VERSION HISTORY
 *	V1.0 Hubitat
 */

 
definition(
    name: "Neato (Connect)",
    namespace: "alyc100",
    author: "Alex Lee Yuk Cheung",
    description: "Integration to Neato Robotics Connected Series robot vacuums",
    category: "",
    iconUrl: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/neato_icon.png",
    iconX2Url: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/neato_icon.png",
    iconX3Url: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/neato_icon.png",
    oauth: true,
    singleInstance: true)

{
	appSetting "clientId"
	appSetting "clientSecret"
}


preferences {
	page(name: "auth", title: "Neato", nextPage:"", content:"authPage", uninstall: true, install:true)
    page(name: "selectDevicePAGE")
}

mappings {
    //path("/oauthInitialize") {action: [GET: "oauthInitUrl"]}
    path("/callback") {action: [GET: "oauthCallback"]}
}

def authPage() {
    log.debug "authPage()"

	if(!atomicState.accessToken) { //this is to access token for 3rd party to make a call to connect app
		atomicState.accessToken = createAccessToken()
	}

	def description
	def uninstallAllowed = false
	def oauthTokenProvided = false

	if(atomicState.authToken) {
		description = "You are connected."
		uninstallAllowed = true
		oauthTokenProvided = true
	} else {
		description = "Click to enter Neato Credentials"
	}

	def redirectUrl = buildRedirectUrl
	log.debug "RedirectUrl = ${redirectUrl}"
	// get rid of next button until the user is actually auth'd
	if (!oauthTokenProvided) {
		return dynamicPage(name: "auth", title: "Login", nextPage: "", uninstall:uninstallAllowed) {
        	section { headerSECTION() }
			section() {
				paragraph "Tap below to log in to the Neato service and authorize Hubitat access."
				href url:redirectUrl, style:"external", required:true, title:"Neato Account Authorization", description:description
			}
		}
    } else {
		updateDevices()
        //Disable push option if contact book is enabled
   	 	if (location.contactBookEnabled) {
    		settings.sendPush = false
    	}
        
        dynamicPage(name: "auth", uninstall: true, install: true) {
        	section { headerSECTION() }
            
			section ("Choose your Neato Botvacs:") {
				href("selectDevicePAGE", title: null, description: devicesSelected() ? "Devices:" + getDevicesSelectedString() : "Tap to select your Neato Botvacs", state: devicesSelected())
        	}
            if (devicesSelected() == "complete") {
               
        		def botvacList = ""
           	}
            section() {
				paragraph "Tap below to re-authenticate to the Neato service and reauthorize Hubitat access."
				href url:redirectUrl, style:"external", required:false, title:"Neato Account Authorization", description:description
			}
            section() {
				input(
            name:"botvac",
            type:"capability.switch",
            title: "Botvac",
            multiple: false,
            required: false,
            submitOnChange: true
              )
            }
            section{
        input(
            name:"logEnable",
            type:"bool",
            title: "Enable debug logging",
            required: true,
            defaultValue: false
            )
            }
        }
	}
}

def selectDevicePAGE() {
	updateDevices()
	dynamicPage(name: "selectDevicePAGE", title: "Devices", uninstall: false, install: false) {
    	section { headerSECTION() }
    	section() {
			paragraph "Tap below to see the list of Neato Botvacs available in your Neato account and select the ones you want to connect to Hubitat."
    		input "selectedBotvacs", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/devicetypes/alyc100/neato_botvac_image.png", required:false, title:"Select Neato Devices \n(${state.botvacDevices.size() ?: 0} found)", multiple:true, options:state.botvacDevices
        }
    }
}

def headerSECTION() {
	return paragraph ("${textVersion()}")
} 

def oauthInitUrl() {
	log.debug "oauthInitUrl with callback: ${callbackUrl}"

	atomicState.oauthInitState = buildStateUrl

	def oauthParams = [
			response_type: "code",
			scope: "public_profile control_robots maps",
			client_id: clientId(),
			state: atomicState.oauthInitState,
			redirect_uri: callbackUrl
	]

	return "${apiEndpoint}/oauth2/authorize?${toQueryString(oauthParams)}"
}

// The toQueryString implementation simply gathers everything in the passed in map and converts them to a string joined with the "&" character.
String toQueryString(Map m) {
        return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def oauthCallback() {
	log.debug "callback()>> params: $params, params.code ${params.code}"

	def code = params.code
	def oauthState = params.state

	if (oauthState == atomicState.oauthInitState) {
		def tokenParams = [
			grant_type: "authorization_code",
			code      : code,
			client_id : clientId(),
            client_secret: clientSecret(),
			redirect_uri: callbackUrl
		]

		def tokenUrl = "https://beehive.neatocloud.com/oauth2/token?${toQueryString(tokenParams)}"

		httpPost(uri: tokenUrl) { resp ->
			atomicState.refreshToken = resp.data.refresh_token
			atomicState.authToken = resp.data.access_token
		}

		if (atomicState.authToken) {
			oauthSuccess()
		} else {
			oauthFailure()
		}

	} else {
		log.error "callback() failed oauthState != atomicState.oauthInitState"
	}

}

// Example success method
def oauthSuccess() {
	def message = """
        <p>Your Neato Account is now connected to Hubitat!</p>
        <p>Close this window to continue setup.</p>
    """
	displayMessageAsHtml(message)
}

def oauthFailure() {
	def message = """
        <p>The connection could not be established!</p>
        <p>Close this window to go back to the app.</p>
    """
	displayMessageAsHtml(message)
}

def displayMessageAsHtml(message) {
    def redirectHtml = ""
	if (redirectUrl) { redirectHtml = """<meta http-equiv="refresh" content="3; url=${redirectUrl}" />""" }

	def html = """
		<!DOCTYPE html>
		<html>
		<head>
		<meta name="viewport" content="width=640">
		<title>Hubitat & Neato connection</title>
		<style type="text/css">
				@font-face {
						font-family: 'Swiss 721 W01 Thin';
						src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
						src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
						font-weight: normal;
						font-style: normal;
				}
				@font-face {
						font-family: 'Swiss 721 W01 Light';
						src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
						src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
						font-weight: normal;
						font-style: normal;
				}
				.container {
						width: 90%;
						padding: 4%;
						/*background: #eee;*/
						text-align: center;
				}
				img {
						vertical-align: middle;
				}
				p {
						font-size: 2.2em;
						font-family: 'Swiss 721 W01 Thin';
						text-align: center;
						color: #666666;
						padding: 0 40px;
						margin-bottom: 0;
				}
				span {
						font-family: 'Swiss 721 W01 Light';
				}
		</style>
		</head>
		<body>
				<div class="container">
						<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
						<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
						<img src="https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/neato_icon.png" alt="neato icon" width="205" />
						${message}
				</div>
		</body>
		</html>
		"""
	render contentType: 'text/html', data: html
}

private refreshAuthToken() {
	log.debug "refreshing auth token"

	if(!atomicState.refreshToken) {
		log.warn "Can not refresh OAuth token since there is no refreshToken stored"
	} else {
		def refreshParams = [
			method: 'POST',
			uri   : "https://beehive.neatocloud.com",
			path  : "/oauth2/token",
			query : [grant_type: 'refresh_token', refresh_token: "${atomicState.refreshToken}"],
		]

		def notificationMessage = "Neato is disconnected from Hubitat, because the access credential changed or was lost. Please go to the Neato (Connect) SmartApp and re-enter your account login credentials."
		//changed to httpPost
		try {
			def jsonMap
			httpPost(refreshParams) { resp ->
				if(resp.status == 200) {
					log.debug "Token refreshed...calling saved RestAction now!"
					saveTokenAndResumeAction(resp.data)
			    }
            }
		} catch (groovyx.net.http.HttpResponseException e) {
			log.error "refreshAuthToken() >> Error: e.statusCode ${e.statusCode}"
			def reAttemptPeriod = 300 // in sec
			if (e.statusCode != 401) { // this issue might comes from exceed 20sec app execution, connectivity issue etc.
				runIn(reAttemptPeriod, "refreshAuthToken")
			} else if (e.statusCode == 401) { // unauthorized
            	if (!atomicState.reAttempt) atomicState.reAttempt = 0
				atomicState.reAttempt = atomicState.reAttempt + 1
				log.warn "reAttempt refreshAuthToken to try = ${atomicState.reAttempt}"
				if (atomicState.reAttempt <= 3) {
					runIn(reAttemptPeriod, "refreshAuthToken")
				} else {
					messageHandler(notificationMessage, true)
                    atomicState.authToken = null
					atomicState.reAttempt = 0
                    
				}
			}
		}
	}
}

private void saveTokenAndResumeAction(json) {
    log.debug "saveTokenAndResumeAction: token response json: $json"
    if (json) {
        atomicState.refreshToken = json?.refresh_token
        atomicState.authToken = json?.access_token
        if (atomicState.action) {
            log.debug "got refresh token, executing next action: ${atomicState.action}"
            "${atomicState.action}"()
        }
    } else {
        log.warn "did not get response body from refresh token response"
    }
    atomicState.action = ""
}

void installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

void updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

void initialize() {
	unschedule()
    addBotvacs()
}
void uninstalled() {
	log.info("Uninstalling, removing child devices...")
	unschedule()
	removeChildDevices(getChildDevices())
}

def updateDevices() {
	log.debug "Executing 'updateDevices'"
	if (!state.devices) {
		state.devices = [:]
	}
	def devices = devicesList()
    state.botvacDevices = [:]
    state.secretKeys = [:]
    def selectors = []
	devices.each { device -> 
    	if (device.serial != null) {
        	selectors.add("${device.serial}")
            state.secretKeys["${device.serial}"] = device.secret_key
			state.botvacDevices["${device.serial}"] = "Neato Botvac - " + device.name
      	}
	}    
    log.debug "selectors: $selectors"
    //Remove devices if does not exist on the Neato platform
    getChildDevices().findAll { !selectors.contains("${it.deviceNetworkId}") }.each {
		log.info("Deleting ${it.deviceNetworkId}")
        try {
			deleteChildDevice(it.deviceNetworkId)
        } catch (hubitat.exception.NotFoundException e) {
        	log.info("Could not find ${it.deviceNetworkId}. Assuming manually deleted.")
        } catch (hubitat.exception.ConflictException ce) {
        	log.info("Device ${it.deviceNetworkId} in use. Please manually delete.")
        }
	} 
    if (selectedBotvacs) {
    	selectedBotvacs.retainAll(selectors as Object[])
    }
}

def addBotvacs() {
	log.debug "Executing 'addBotvacs'"
	updateDevices()

	selectedBotvacs.each { device ->
    	
        def childDevice = getChildDevice(device)
        
        
        if (!childDevice) { 
    		log.info("Adding Neato Botvac device ${device}: ${state.botvacDevices[device]}")
            
        	def data = [
                name: state.botvacDevices[device],
				label: state.botvacDevices[device]
			]
            //childDevice = addChildDevice(app.namespace, "Neato Botvac Connected Series", device, null, data)
            childDevice = addChildDevice("alyc100","Neato Botvac Connected Series", device, null, data)
            childDevice.refresh()
           
			log.debug "Created ${state.botvacDevices[device]} with id: ${device}"
		} else {
			log.debug "found ${state.botvacDevices[device]} with id ${device} already exists"
            
            
		}
	}
}

def getSecretKey(deviceSerial) {
	return state.secretKeys[deviceSerial]
}

private removeChildDevices(devices) {
	devices.each {
		deleteChildDevice(it.deviceNetworkId) // 'it' is default
	}
}

def devicesList() {
	logErrors([]) {
		def resp = beehiveGET("/users/me/robots")
        def notificationMessage = "Neato is disconnected from Hubitat, because the access credential changed or was lost. Please go to the Neato (Connect) SmartApp and re-enter your account login credentials."
		if (resp.status == 200) {
			return resp.data
		} else if (resp.status == 401) {
        	atomicState.action = "updateDevices"
        	if (!atomicState.reAttempt) atomicState.reAttempt = 0
        	atomicState.reAttempt = atomicState.reAttempt + 1
			log.warn "reAttempt refreshAuthToken to try = ${atomicState.reAttempt}"
			if (atomicState.reAttempt <= 3) {
				runIn(reAttemptPeriod, "refreshAuthToken")
			} else {
				messageHandler(notificationMessage, true)
                atomicState.authToken = null
				atomicState.reAttempt = 0
			}
        }
        else {
        	log.error("Non-200 from device list call. ${resp.status} ${resp.data}")
            runIn(reAttemptPeriod, "refreshAuthToken")
			return []
		}
	}
}

def devicesSelected() {
	return (selectedBotvacs) ? "complete" : null
}

def getDevicesSelectedString() {
	updateDevices()
	def listString = ""
	selectedBotvacs.each { childDevice -> 
        if (null != state.botvacDevices) {
        	listString += "\n• " + state.botvacDevices[childDevice]
        }
    }
    return listString
}

//Beehive API Access
def beehiveGET(path, body = [:]) {
	try {
        log.debug("Beginning API GET: ${beehiveURL(path)}, ${beehiveRequestHeaders()}")

        httpGet(uri: beehiveURL(path), contentType: 'application/json', headers: beehiveRequestHeaders()) {response ->
			logResponse(response)
			return response
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

Map beehiveRequestHeaders() {
	return [
        'Accept': 'application/vnd.neato.nucleo.v1',
        'Content-Type': 'application/*+json',
        'X-Agent': '0.11.3-142',
        'Authorization': "Bearer ${atomicState.authToken}"
    ]
}

def logResponse(response) {
	//log.info("Status: ${response.status}")
	//log.info("Body: ${response.data}")
    //move poll parsing from device to app.......then push changes to device\\
    def resp = (response.data)
    def status = (response.status)
    logDebug ("${resp}")
    logDebug ("${status}")
    def result = resp
    //def binFullFlag = false
    if (status != 200) {
    	if (result.find{ it.key == "message" }){
        	switch (result.message) {
            	case "Could not find robot_serial for specified vendor_name":
                	statusMsg += 'Robot serial and/or secret is not correct.\n'
                break;
            }
        }
		log.error("Unexpected result in poll(): [${resp}] ${status}")
        settings.botvac.statusUpdate("status","error")
        settings.botvac.statusUpdate("network","not connected")
		logDebug ("Not Connected To Neato")
	}
    else { 
        if (result.find{ it.key == "cleaning" }){
            batteryLevel = result.details.charge as String
            logDebug ("Battery level ${batteryLevel}")
            settings.botvac?.statusUpdate("battery",batteryLevel)      
        }
        if (result.find{ it.key == "action" }){
        	logDebug ("action key looking for 9 or 4" )
            if (result.action == 4 || result.action == 9){
            state.returningToDock = true
                logDebug ("returningToDock = true" )
            }else{
                state.returningToDock = false
                logDebug ("returningToDock = false" )
            }     
        }
        if (result.find{ it.key == "state" }){
        	settings.botvac.statusUpdate("network","connected")
        	//state 1 - Ready to clean
        	//state 2 - Cleaning
        	//state 3 - Paused
       		//state 4 - Error
            switch (result.state) {
        		case "1":
                    settings.botvac.statusUpdate("status","stopped")
                    settings.botvac.statusUpdate("switch","off")
                    logDebug ("switch status should be off - Stopped")
				break;
				case "2":
                if (state.returningToDock){
                    settings.botvac.statusUpdate("status","returning to dock")
                	settings.botvac.statusUpdate("switch","on")
                    logDebug ("switch should be on - returning to dock")
                }else{
                    settings.botvac.statusUpdate("status","running")
                	settings.botvac.statusUpdate("switch","on")
                    logDebug ("switch should be on - running")
                }   
				break;
            	case "3":
					settings.botvac.statusUpdate("status","paused")
                	settings.botvac.statusUpdate("switch","on")
                    logDebug ("Vacuum should be paused")
                break;
            	case "4":
					settings.botvac.statusUpdate("status","error")
                    logDebug ("Vacuum Error??")
				break;
            	default:
                    settings.botvac.statusUpdate("status","unknown")
				break;
        	}
        }
         if (result.find{ it.key == "error" }){
             errorCode = result.error as String
             if (errorCode == null){
                 logDebug ("No errors")
                 settings.botvac.statusUpdate("error","clear")
             }else{
                 logDebug ("Error is -  $errorCode")
                 settings.botvac.statusUpdate("error",errorCode)
             }
        }
        if (result.find{ it.key == "details" }){
        	if (result.details.isDocked) {
                logDebug ("Vacuum now Docked")
                settings.botvac.statusUpdate("status","docked")
            } else {
                logDebug ("Botvac Not Docked") 
            }
            settings.botvac.statusUpdate("charging",result.details.isCharging as String)
        }
        //need to see how to handle this
        /*if (binFullFlag) {
            settings.botvac.statusUpdate("bin","full")
        } else {
            settings.botvac?.statusUpdate("bin","empty")
        } */
    }
}

def logErrors(options = [errorReturn: null, logObject: log], Closure c) {
	try {
		return c()
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error("got error: ${e}, body: ${e.getResponse().getData()}")
		return options.errorReturn
	} catch (java.net.SocketTimeoutException e) {
		log.warn "Connection timed out, not much we can do here"
		return options.errorReturn
	}
}

def getChildName()           { return "Neato BotVac" }
def getServerUrl()           { return "https://cloud.hubitat.com" }
def getShardUrl()            { return getApiServerUrl() }
def getCallbackUrl()         { return "https://cloud.hubitat.com/oauth/stateredirect" }
def getBuildRedirectUrl()    { return oauthInitUrl() }
def getBuildStateUrl()       { return "${getHubUID()}/apps/${app.id}/callback?access_token=${state.accessToken}" }
def getApiEndpoint()         { return "https://apps.neatorobotics.com" }
def getSmartThingsClientId() { return appSettings?.clientId }
def beehiveURL(path = '/') 	 { return "https://beehive.neatocloud.com${path}" }
private def textVersion() {
    def text = "Neato (Connect)\nHubitat Version: 1.0"
}

private def textCopyright() {
    def text = "Copyright © 2016-2020 Alex Lee Yuk Cheung"
}

def clientId() {
	if(!appSettings?.clientId) {
		return "4f21ab200ecacf56759e7b2654124f5945630e4249823dee6c0ae56bb7adc1de"
	} else {
		return appSettings?.clientId
	}
}

def clientSecret() {
	if(!appSettings?.clientSecret) {
		return "c4b91d782c86ff6ad714fc6176bf06e4f83aafbc5c68621a8d6c7d21403516e5"
	} else {
		return appSettings?.clientSecret
	}
}

void logDebug(String msg){
	if (settings?.logEnable != false){
		log.debug "$msg"
	}
}
