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
package edu.stevens.cpe.reservior.layers;

import java.util.Arrays;
import java.util.Random;

import org.apache.log4j.Logger;
import org.bushe.swing.event.DiscreteTimeThreadSafeEventService;
import org.bushe.swing.event.EventBus;
import org.encog.ml.data.MLData;

import edu.stevens.cpe.reservior.PulseGenerator;
import edu.stevens.cpe.reservior.ReserviorException;
import edu.stevens.cpe.reservior.Reservoir;
import edu.stevens.cpe.reservior.ReservoirNetwork;
import edu.stevens.cpe.reservior.SpikeEvent;
import edu.stevens.cpe.reservior.neuron.Neuron;

/**
 * Weights connected to inputs are uniform distributed
 * 
 * The inputs act as the clock to the reservoir
 * @author wil
 *
 */
public class Input {
	
	public static Logger logger = Logger.getLogger(Input.class);
	/**
	 * The reservoir to connect too
	 */
	private Reservoir<?> reservior;
	/**
	 * Prefix given to the ID of the neuron used for topic subscription
	 */
	private static final String ID_PREFIX = "in_";
	/**
	 * 
	 */
	private static final String ID_BIAS = "B";
	/**
	 * Number of inputs
	 */
	private int numberInputs;
	/**
	 * Uniform distribution for weights connected to inputs
	 */
	private Random random = new Random(System.nanoTime());
	
	/**
	 * 
	 */
	private String [] publishers;
	/**
	 * 
	 */
	private boolean useBias = false;

	/**
	 * 
	 * @param reservior The reservoir to connect to
	 * @param numberInputs Number of inputs 
	 * @param useBias
	 */
	public Input(Reservoir<?> reservior,int numberInputs, boolean useBias){
		this.reservior = reservior;
		this.numberInputs = numberInputs;
		this.useBias = useBias;
		
		this.publishers = new String[numberInputs];
		for (int i=0; i<publishers.length; i++){
			publishers[i] =ID_PREFIX + i; 
		}
		//Subscribe all the neurons in the reservoir to these inputs
		reservior.subscribe(publishers);
		if (useBias){
			reservior.subscribe(ID_BIAS);
		}
	}
	
	/**
	 * Set inputs as a sequene of spike trains
	 * @param spikeTrains
	 * @throws ReserviorException
	
	public void setInput(String [] spikeTrains) throws ReserviorException{
		//input.getData()
		if (spikeTrains.length != nodes.length){
			throw new ReserviorException("There are " + nodes.length + " input nodes and the training set has " +spikeTrains.length + " input values.");
		}
		
		//There is a node for each data input
		for (int i=0; i<nodes.length; i++){
			//Going to need to convert the analog signal into a sequence of pulse or 
			PulseGenerator.pulseSpikeTrain(nodes[i].getName(),spikeTrains[i]);
			//EventBus.publish(nodes[i].getName(),  new SpikeEvent(input.getData()[i], SpikeEvent.DEFAULT_PULSE_WIDTH));
	
		}
	}
	 */
	
	/**
	 * @return the publishers
	 */
	public String[] getPublishers() {
		return publishers;
	}

	/**
	 * Mimic a parallel system. Input as doubles. 
	 * @param input
	 * @throws ReserviorException
	 */
	public void setStaticInput(MLData input) throws ReserviorException{

		if (input.getData().length != publishers.length){
			throw new ReserviorException("There are " + publishers.length + " input nodes and the training set has " + input.getData().length + " input values.");
		}
		//Make a copy of the input array so it does not modify the original
		double [] workableData = Arrays.copyOf(input.getData(), input.getData().length);

		for (int i=0; i<63; i++){ // 63=# bits in double
			for (int j=0; j<publishers.length; j++){
				long bits = Double.doubleToRawLongBits(workableData[j]);
				long leastSignificant = bits & 0x0000001; 
	
				double amp = (leastSignificant != 0) ? SpikeEvent.DEFAULT_SPIKE_HEIGHT : 0;
				SpikeEvent ev = new SpikeEvent(amp, SpikeEvent.DEFAULT_PULSE_WIDTH);
				ev.setTime(i);
				EventBus.publish(publishers[j],  ev);
				
				bits = bits >> 1; //shift out the bit that was just published
				workableData[j] =  Double.longBitsToDouble(bits) ;
			}
			//Fire off the bias
			if (useBias){
				SpikeEvent ev = new SpikeEvent(SpikeEvent.DEFAULT_SPIKE_HEIGHT, SpikeEvent.DEFAULT_PULSE_WIDTH);
				ev.setTime(i);
				EventBus.publish(ID_BIAS,  ev);
			}
			
			ReservoirNetwork.incClock();
		//	logger.info("it: " + i + " clk: " + DiscreteTimeThreadSafeEventService.getClock());
		}
	}
	public void setDynamicInput(double [][] input) throws ReserviorException{
		if (input.length != publishers.length){
			throw new ReserviorException("There are " + publishers.length + " input nodes and the training set has " + input.length + " input values.");
		}
		int sampleSize = input[0].length;
		for (int i=0; i<sampleSize; i++){ //for every sample
			for (int j=0; j<publishers.length; j++){ // for each input signal
				SpikeEvent ev = new SpikeEvent(input[j][i]*SpikeEvent.DEFAULT_SPIKE_HEIGHT, SpikeEvent.DEFAULT_PULSE_WIDTH);
				ev.setTime(i);
				EventBus.publish(publishers[j],  ev);
			}
			System.out.print(i+" ");
			ReservoirNetwork.incClock();
		}
		System.out.println("");
	}
	/**
	 * Set inputs as a sequene of spike trains
	 * @param spikeTrains
	 * @throws ReserviorException
	
	public void setInput(String [] spikeTrains) throws ReserviorException{
		//input.getData()
		if (spikeTrains.length != nodes.length){
			throw new ReserviorException("There are " + nodes.length + " input nodes and the training set has " +spikeTrains.length + " input values.");
		}
		
		//There is a node for each data input
		for (int i=0; i<nodes.length; i++){
			//Going to need to convert the analog signal into a sequence of pulse or 
			PulseGenerator.pulseSpikeTrain(nodes[i].getName(),spikeTrains[i]);
			//EventBus.publish(nodes[i].getName(),  new SpikeEvent(input.getData()[i], SpikeEvent.DEFAULT_PULSE_WIDTH));

		}
	}
	 */

	/**
	 * @param publishers the publishers to set
	 */
	public void setPublishers(String[] publishers) {
		this.publishers = publishers;
	}

}
