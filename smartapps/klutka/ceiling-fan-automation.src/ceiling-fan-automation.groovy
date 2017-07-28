/**
 *  Copyright 2016 Justin Klutka
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
 *  Ceiling Fan Automation
 *
 *  Author: Justin Klutka
 */
 
 /*
 	Version History:
    
    1.0 - 5/29/2016 - Basic version release.  You may specifiy a thermostat and a set of fans
    1.1 - 11/23/2016 - Migrated to a new repo and removed the testing field for snooze actions
    1.2 - 7/13/2017 - Added Sleep time window to avoid fan coming on when it is unwanted. Code refactor.
    1.3 - 7/16/2017 - Logic bug fix
    1.4 - 7/24/2017 - Fixed a bug with the sleep time monitor logic.
    
    
    Future Plans:
    - Add conditional preferences. Ex: Mode of the house, time of day, day of week, if certain people are home
 */

definition(
    name: "Ceiling Fan Automation",
    namespace: "klutka",
    author: "Justin Klutka",
    description: "Designed to coordinate device operating state for a thermostat to a set of switches.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light24-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light24-icn@2x.png"
)

preferences() {
	section("Thermostat Setup") {
		input "thermostat", "capability.thermostat", required: true, title: "Select your thermostat"
        input "triggerStates", "enum", title: "Select your trigger operating states", multiple: true, required: true, options:
          [["heating": "heating"], ["cooling": "cooling"], ["fan only": "fan only"], ["idle": "idle"]]
	}
    section("Fan Setup") {
    	input "fans", "capability.switch", required: true, multiple: true, title: "Select your Fans or Fan Switches"
    }
    section("Sleep Period") {
    	input "sleepTimeStart", "time", title: "Starting:", required: false
        input "sleepTimeEnd", "time", title: "Ending:", required: false
    }
    
}

def installed() {
	subscribeToEvents()
}

def updated() {
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {	
	if (thermostat) {
        subscribe(thermostat, "thermostatOperatingState", operatingStateHandler)
	}
    
	if (fans) {
    	subscribe(fans, "switch.on", switchOnHandler)
        subscribe(fans, "switch.off", switchOffHandler)
	}
    
	evaluateAutomation()
}

def operatingStateHandler(evt) {
	log.debug("The thermostat operating state changed at ${new Date()}.")    
    evaluateAutomation()
}

def switchOnHandler(evt) {

}

def switchOffHandler(evt) {
	
}

//core function to evaluate if fans should be automated
def evaluateAutomation() {	    
	log.debug("Evaluation of Automation happened at ${new Date()}.")
    
    if (isSleepTime()) {
    	log.debug("Enforce sleep mode at ${new Date()}.")
    	switchesOff()
        return
    }
    
    if (fansRequired()) {
        switchesOn()  
    }
    else {
    	switchesOff()
	}
    
    //if a sleep preference exists monitor for sleep time
    if (sleepTimeStart != null && sleepTimeEnd != null) {
        log.debug("A 15 minute sleep check was scheduled at ${new Date()}.")
        runEvery15Minutes(evaluateAutomation)
    }    
}

//Returns if operating state requires fans to come on
def fansRequired () {
	def currentOperatingState
    
    if (!thermostat) {
    	log.debug("A thermostat poll will be requested because the thermostat was null.")
    	thermostat.poll()
    }
    
	currentOperatingState = thermostat.currentState("thermostatOperatingState")?.value
    log.debug("Current operating state: ${currentOperatingState.toString()}.") 
        
    //evaluate if an operating state requiring fans is present
    log.debug("Seeking operating state: ${triggerStates.toString()}.") 
    if (triggerStates.contains(currentOperatingState.toString())) {                       	
        return true
    }  
    else {
        //no fans are required
        return false
    }
}

//Evaluate if sleep time needs to be observed
def isSleepTime() {
    if (sleepTimeStart != null && sleepTimeEnd != null) {
        return timeOfDayIsBetween(sleepTimeStart, sleepTimeEnd, new Date(), location.timeZone)      
    }
    else {
    	return false
	}
}

//Turns on all fans
private switchesOn() {	
	log.debug("Fans powered on at ${new Date()}.")
	fans.each {
		it.on()
    }
}

//Turns off all fans
private switchesOff() {
	log.debug("Fans powered off at ${new Date()}.")
	fans.each {
		it.off()
    }
}