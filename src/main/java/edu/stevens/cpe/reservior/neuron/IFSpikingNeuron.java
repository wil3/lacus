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

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.bushe.swing.event.DiscreteTimeThreadSafeEventService;
import org.bushe.swing.event.EventBus;

import edu.stevens.cpe.reservior.Reservoir;
import edu.stevens.cpe.reservior.ReservoirNetwork;
import edu.stevens.cpe.reservior.SpikeEvent;

/**
 * Parameters obtained from 
 * http://www.cs.rug.nl/biehl/Teaching/NN/iaf.pdf
 * 
 * Need to model this less like a real neuron. Hold on to basical princial that this neuron must be stimulated X times before it fires thus retaining some sort of memory
 * @author wil
 *
 */
public class IFSpikingNeuron extends Neuron {

	public final static String ID_PREFIX = "IF_";
	//public final static int DEFAULT_TIME_REFACTORY_PERIOD_COMPLETE = -1;

	/**
	 * This value determines how many times the neuron must be stimulated until it reaches its threshold
	 * A smaller value will retain less memory as a larger value but will fire more frequently.
	 */
	public static double MEMORY_CAPACITY_COEF = 3;
	/**
	 * The times in which this neuron has fired
	 */
	private final ArrayList<Long> spikeTimes = new ArrayList<Long>();



	private final HashMap<Long,Double> chargeTimes = new HashMap<Long,Double>();

	/**
	 * If a neuron fires then the action potential is the same regardless of the amount of excitation received from the inputs.
	 * The membrane potential voltage threshold in mV
	 */
	public static double THRESHOLD = 1;//16;
	
	/**
	 * Membrane capactiance in Ferrads
	 * C
	 */
	public static final double membraneCapacitance = 0.207 * Math.pow(10,-9);

	
	/**
	 * Value once the neuron fires the voltage membrane is reset to this value in mV.
	 */
	private final double resetValue = 0;
	
	/**
	 * Time in which the neuron is given time to rest in nanoseconds
	 * t_ref
	 */
	public static long REFACTORY_PEROID = 1;//3 * (long)Math.pow(10, 6);
	

	/**
	 * Used to determine if in refactory period in nanoseconds
	 */
	private long timeRefactoryPeriodComplete = -1;
	
	public IFSpikingNeuron(Integer id){
		super(id, ID_PREFIX + id);
	}
	public IFSpikingNeuron(String name){
		super(name);
	}
	public IFSpikingNeuron(String name, Boolean shouldReset){
		super(name);
		this.shouldReset = shouldReset;
	}
	/**
	 * 
	 * @param name
	 */
	public IFSpikingNeuron(Integer id, String name){
		super(id, name);

	}
	
	/**
	 * Receive all incoming signals from dendrites and update potential
	 * @param current
	 * @param pulseWidth	The time in nanoseconds
	 */
	 int c=0;
	/**
	 * 
	 * @param time seconds
	 * @param current The current in amps applied to the neuron
	 * @return
	 */
	protected double charge(double time, double current){
		//in descrete time, time = 1
		//return (current * time / membraneCapacitance) + getMembranePotential();
		return (current * time / MEMORY_CAPACITY_COEF) + getMembranePotential();

	}

	Boolean shouldReset = true;
	/**
	 * This method must be overriden to provide the response to when a spike event is received
	 */
	@Override public synchronized void updateMembranePotential(double current, long pulseWidth){
		//logger.info(c + " update");
		long currentTime = System.nanoTime();
		//Make sure we are not in a refactory period
		if (getLastEventdiscreteTimestamp() >= timeRefactoryPeriodComplete){
			//long deltaTime = currentTime - getLastEventTimestamp();
		
			//charge at a constant current for 1 second
			//double newMembranePotential = charge(pulseWidth*Math.pow(10,-9), current);
			double newMembranePotential = charge(pulseWidth, current);

			if (newMembranePotential > THRESHOLD){
				SpikeEvent packet = new SpikeEvent();
				//Inc the time series, time driven by spikes. Descrete times are updated in the onEvent method
				packet.setTime(getLastEventdiscreteTimestamp()+1);
				
				//Fire
				EventBus.publish(getName(), packet );
				spikeTimes.add(getLastEventdiscreteTimestamp());

				logger.debug(getName() + "\t=>\t\tt=" + getLastEventdiscreteTimestamp());
				//Reset the membran potential
				
				//FIXME
				//Temp for output neurons
				if (shouldReset){
					setMembranePotential(resetValue);
				}
				
				//Set next valid time
				this.timeRefactoryPeriodComplete  = getLastEventdiscreteTimestamp()+REFACTORY_PEROID;//currentTime + refactoryPeriod;
			} else {
				setMembranePotential(newMembranePotential);
			}
			setLastEventTimestamp(currentTime);
		} else {
			logger.debug(c + " missed pulse");
		}
		chargeTimes.put(getLastEventdiscreteTimestamp(),getMembranePotential());
		c++;
	}
	
	@Override public synchronized double getCurrentMembranePotential(){
		
		return getMembranePotential();

	}
	

	/**
	 * @return the resetValue
	 */
	public double getResetValue() {
		return resetValue;
	}

	/**
	 * @return the refactoryPeriod
	 */
	public long getRefactoryPeriod() {
		return REFACTORY_PEROID;
	}

	/**
	 * @return the timeRefactoryPeriodComplete
	 */
	public long getTimeRefactoryPeriodComplete() {
		return timeRefactoryPeriodComplete;
	}

	/**
	 * @param timeRefactoryPeriodComplete the timeRefactoryPeriodComplete to set
	 */
	public void setTimeRefactoryPeriodComplete(long timeRefactoryPeriodComplete) {
		this.timeRefactoryPeriodComplete = timeRefactoryPeriodComplete;
	}
	
	/**
	 * Reset the membrane potential to the reset state. Remove all history of spike times
	 */
	@Override public void reset(){
		setMembranePotential(resetValue);
		spikeTimes.clear();
		this.timeRefactoryPeriodComplete = -1;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	@Override public double [] getStateHistory(){
		int maxTime = (int) ReservoirNetwork.getClock();
		//TODO fix this casting
		double [] charges = new double[maxTime];
		double lastCharge = 0;
		for (int i=0; i<maxTime; i++){
			Object charge = chargeTimes.get((long)i);
			double val = (charge == null) ? -1 :  chargeTimes.get((long)i);
			charges[i] = val;//chargeTimes.get((long)i);//(charge != null) ? (Double)charge : lastCharge;
		}
		return charges;
	}
	@Override public double [] getFiringTimes(){
		int maxTime = (int) ReservoirNetwork.getClock();
		//TODO fix this casting
		double [] spikes = new double[maxTime];
		for (int i=0; i<maxTime; i++){
			spikes[i] = spikeTimes.contains(Long.valueOf(i)) ? 1 : 0;
		}
		return spikes;
	}
}
