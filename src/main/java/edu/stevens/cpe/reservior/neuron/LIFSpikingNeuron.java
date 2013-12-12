/*******************************************************************************
 *  Copyright 2013 William Koch
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package edu.stevens.cpe.reservior.neuron;

import java.util.Calendar;
import java.util.Random;

import org.apache.log4j.Logger;
import org.bushe.swing.event.EventBus;

import edu.stevens.cpe.reservior.Reservoir;
import edu.stevens.cpe.reservior.SpikeEvent;

/**
 * Parameters obtained from 
 * http://www.cs.rug.nl/biehl/Teaching/NN/iaf.pdf
 * @author wil
 *
 */
public class LIFSpikingNeuron extends IFSpikingNeuron {

	private static Logger loggerFiring = Logger.getLogger("fire");
	private static Logger loggerVolt = Logger.getLogger("volt2");
	
	
	/**
	 * The neuron membrane resistance in Ohms
	 * R
	 */
	private double membraneResistance = 38.3 * Math.pow(10, 6);
	
	/**
	 * The time constant for the RC circuit modeling LIF 
	 */
	private double timeConstant;
	
	/**
	 * 
	 * @param name
	 */
	public LIFSpikingNeuron(String name){
		super(name);
	//	this.timeConstant =  getMembraneCapacitance()*membraneResistance;
	//	loggerVolt.info(getName() + "\t" + (System.nanoTime() - Reservior.startTime) + "\t" + getMembranePotential());

	}
	
	/**
	 * Receive all incoming signals from dendrites and update potential
	 * @param current
	 * @param pulseWidth	The time in nanoseconds
	 */
	 int c=0;
	@Override public synchronized void updateMembranePotential(double current, long pulseWidth){
		//logger.info(c + " update");
		long currentTime = System.nanoTime();
		//Make sure we are not in a refactory period
		if (currentTime >= getTimeRefactoryPeriodComplete()){
			long deltaTime = currentTime - getLastEventTimestamp();
		
			double newMembranePotential = discharge(deltaTime*Math.pow(10,-9)) + charge(pulseWidth*Math.pow(10,-9), current);
			
			loggerVolt.debug( (currentTime- Reservoir.startTime) + "\t" + newMembranePotential);
			logger.debug("FIRE " + c + "\t" + deltaTime + "\t" + newMembranePotential);

			
			if (newMembranePotential > THRESHOLD){
				SpikeEvent packet = new SpikeEvent();
				//Fire
				EventBus.publish(getName(), packet );
				loggerFiring.debug(getName() + "\t" + (currentTime- Reservoir.startTime));
				
				//Reset
				setMembranePotential(getResetValue());
				
				//Set next valid time
				setTimeRefactoryPeriodComplete(currentTime + getRefactoryPeriod());
			} else {
				setMembranePotential(newMembranePotential);
			}
			setLastEventTimestamp(currentTime);
		} else {
			logger.debug(c + " missed pulse");
		}
		c++;
	}
	
	/**
	 * 
	 * @param time Lapse time in which the cap has been discharging in seconds
	 * @return
	 */
	private double discharge(double time){
		return getMembranePotential() * Math.exp(-1.0 * time / timeConstant );
	}
	/**
	 * 
	 * @param time seconds
	 * @param current The current in amps applied to the neuron
	 * @return
	 */
	//FIXME is this equation right?
	@Override protected double charge(double time, double current){
		return membraneResistance * current * (1 - Math.exp(-1.0 * time / timeConstant));
	}
}
