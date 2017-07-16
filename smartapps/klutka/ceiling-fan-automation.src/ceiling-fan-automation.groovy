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
        input "triggerStates", "enum", title: "Select your trigger operating states", multiple: true, required: false, options:
          [["heating": "heating"], ["cooling": "cooling"], ["fan only": "fan only"], ["idle": "idle"]]
	}
    section("Fan Setup") {
    	input "fans", "capability.switch", required: true, multiple: true, title: "Select your Fans or Fan Switches"
    }
    section("Sleep Period") {
    	input "sleepTimeStart", "time", title: "Starting: ", required: false
        input "sleepTimeEnd", "time", title: "Ending: ", required: false
    }
    
}

def installed() {
	subscribeToEvents()
    evaluateAutomation()
}

def updated() {
	unsubscribe()
	subscribeToEvents()
    evaluateAutomation()
}

def subscribeToEvents() {	
	if (thermostat) 
        subscribe(thermostat, "thermostatOperatingState", operatingStateHandler)
	
	evaluateAutomation()
}

def operatingStateHandler(evt) {
	log.debug("The thermostat operating state changed.")
    evaluateAutomation()
}

//called on a schedule to determine if sleep mode requires fans to shut off
def sleepEnforcement(fromTime, toTime) {
	if (isSleepTime(fromTime, toTime)) {
    	switchesOff()
    }
}

//core function to evaluate if fans should be automated
private evaluateAutomation() {	    
	
    if (fansRequired()) {
    	if (!isSleepTime(sleepTimeStart, sleepTimeEnd)) {
        	switchesOn()
            
            //if a sleep preference exists monitor for sleep time
            if (sleepPreference(sleepTimeStart, sleepTimeEnd)) {
            	runEvery15Minutes(sleepEnforcement(sleepTimeStart, sleepTimeEnd))
            }
        }        
    }
    else {
    	switchesOff()
	}
}

//Returns if operating state requires fans to come on
private fansRequired () {
	def currentOperatingState
    def matchingOperatingState = false
    
    if (!thermostat) {
    	log.trace("A thermostat poll will be requested")
    	thermostat.poll()
    }
    
	currentOperatingState = thermostat.currentState("thermostatOperatingState")?.value
    log.debug("Evaluation operating state: ${currentOperatingState}.") 
    
    //evaluate if an operating state requiring fans is present
    triggerStates.each { 
        if (it == currentOperatingState) {                       	
            matchingOperatingState = true
        }            
    }
        
    //no fans are required
    return matchingOperatingState
}

//Returns if a sleep preference was set
private sleepPreference(fromTime, toTime) {
	//evaluate if sleep rules need to be observed
    if (fromTime != null && toTime != null)
    	return true
    else
    	return false
}

//Evaluate if sleep time needs to be observed
private isSleepTime(fromTime, toTime) {
	if (sleepPreference(fromTime, toTime)) {
    	log.trace("A sleep preference is present")
		return timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
    }
    else {
    	return false
    }
}

//Turns on all fans
private switchesOn() {	
	fans.each {
    	it.on()
    }
}

//Turns off all fans
private switchesOff() {
	fans.each {
    	it.off()
    }
}