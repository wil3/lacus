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
package edu.stevens.cpe.reservior;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bushe.swing.event.DiscreteTimeThreadSafeEventService;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventServiceExistsException;
import org.bushe.swing.event.EventServiceLocator;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;

import edu.stevens.cpe.math.MLMatrixUtils;
import edu.stevens.cpe.reservior.layers.Input;
import edu.stevens.cpe.reservior.layers.ReadoutFunction;
import edu.stevens.cpe.reservior.neuron.IFSpikingNeuron;
import edu.stevens.cpe.reservior.neuron.Neuron;
import edu.stevens.cpe.reservior.topology.NetworkTopology;

/**
 * Input-driven publish-subscribe architechured spiking reservoir network
 * 
 * Wrapper class containing the input, reservoir and output layers
 * 
 * Decision to base neurons as objects rather than matrices provides the flexibility to treat each neuron as an agent.
 * Treating each neuron as an agent defines the base framework to expand this research for investigation self organization, communication,
 * and additional intellegence to individual neurons.  
 *  
 * 
 * @author wil
 *
 */
public class ReservoirNetwork <T extends Neuron> {
	
	
	private volatile static long CLK = 0;
	private static Object clkLock = new Object();
	
	/**
	 * Lock for updating next ID
	 */
	private static Object idLock = new Object();
	/**
	 * To be used for neurons being dynamically added so they have a unique ID
	 */
	public static volatile int NEXT_ID = 0;
	
	private Reservoir<T> reservoir;
	private Input inputLayer;
	
	
	/**
	 * 
	 * @param inputs
	 * @param N number of neurons in the reservoir
	 * @param useBias Whether to use a bias or not
	 * @param topology 
	 * @param clazz The class of the neurons
	 * @throws ReserviorException
	 */
	public ReservoirNetwork(int inputs, int N, boolean useBias, NetworkTopology topology, Class<T> clazz) throws ReserviorException{
		this.reservoir = new Reservoir<T>(N, topology, clazz);
		this.inputLayer = new Input(reservoir, inputs, useBias);
		
		//After initialization the next avaible ID will be the length of the number of neurons
		setNEXT_ID(N);
/*
		System.setProperty("org.bushe.swing.event.eventBusClass", "org.bushe.swing.event.DiscreteTimeThreadSafeEventService");
		try {
			EventServiceLocator.setEventService("DiscreteTimeThreadSafeEventService", new DiscreteTimeThreadSafeEventService());
		} catch (EventServiceExistsException e) {
			e.printStackTrace();
		}*/
	}
	/**
	 * For static data
	 * @param input
	 * @throws ReserviorException
	 */
	public void input(MLData input) throws ReserviorException  {
	    //Input driven, Spike trains will act as clock
		
		inputLayer.setStaticInput(input);
		
		//Handle any left over spikes in the buffer
		DiscreteTimeThreadSafeEventService ev = (DiscreteTimeThreadSafeEventService)EventBus.getGlobalEventService();
		ev.flush();
	}
	/**
	 * For dynamic data
	 * @param input
	 * @throws ReserviorException 
	 */
	public void input(double [][] input) throws ReserviorException{
		inputLayer.setDynamicInput(input);
		
		//Handle any left over spikes in the buffer
		DiscreteTimeThreadSafeEventService ev = (DiscreteTimeThreadSafeEventService)EventBus.getGlobalEventService();
		ev.flush();
	}
	/**
	 * @return the nEXT_ID
	 */
	public static int generateID() {
	
		int id = NEXT_ID;
		setNEXT_ID(id+1);
		return id;
	}

	/**
	 * @return the reservior
	 */
	public Reservoir<T> getReservior() {
		return reservoir;
	}

	/**
	 * @return the inputLayer
	 */
	public Input getInputLayer() {
		return inputLayer;
	}
	
	/**
	 * @param reservior the reservior to set
	 */
	public void setReservior(Reservoir<T> reservior) {
		this.reservoir = reservior;
	}
/*
	public void setInput (String [] spikeTrains) throws ReserviorException{
		
		//Spike trains will act as clock
		inputLayer.setInput(spikeTrains);
		
		//Handle any left over spikes in the buffer
		DiscreteTimeThreadSafeEventService ev = (DiscreteTimeThreadSafeEventService)EventBus.getGlobalEventService();
		ev.flush();
			
		//return readout.getOutput();
	
	}
*/
	/**
	 * @param nEXT_ID the nEXT_ID to set
	 */
	public static void setNEXT_ID(int nextID) {
		synchronized (idLock) {
			NEXT_ID = nextID;
		}
	}
	/**
	 * Get the internal state of the reservoir 
	 * Each row is a neuron with the corresponding spike trains. Neurons are ordered in the 
	 * order in which they were created.
	 * @return
	 
	public DoubleMatrix2D getInternalFiringTimes(){
		
		//FIXME we can not cast to a specific nueron type if we are using generics
		//May need to have classes implementing different types of reservoirs
		DoubleFactory2D factory = DoubleFactory2D.dense;
		Neuron n1 = getReservior().getNeurons()[0];
		DoubleMatrix2D allSpikes = MLMatrixUtils.convertArrayTo2DMatrix( n1.getFiringTimes());
		//Here the neurons are in order
		for (int i=1; i<getReservior().getNeurons().length; i++){
			Neuron n = getReservior().getNeurons()[i];
			allSpikes = factory.appendRows(allSpikes, MLMatrixUtils.convertArrayTo2DMatrix(n.getFiringTimes()));
		}
		return allSpikes;
	}

	public DoubleMatrix2D getInternalChargeHistory(){
		
		//FIXME we can not cast to a specific nueron type if we are using generics
		//May need to have classes implementing different types of reservoirs
		DoubleFactory2D factory = DoubleFactory2D.dense;
		Neuron n1 = getReservior().getNeurons()[0];
		DoubleMatrix2D allSpikes = MLMatrixUtils.convertArrayTo2DMatrix( n1.getStateHistory());
		//Here the neurons are in order
		for (int i=1; i<getReservior().getNeurons().length; i++){
			Neuron n = getReservior().getNeurons()[i];
			allSpikes = factory.appendRows(allSpikes, MLMatrixUtils.convertArrayTo2DMatrix(n.getStateHistory()));
		}
		return allSpikes;
	}
*/
	
	
	/**
	 * @param inputLayer the inputLayer to set
	 */
	public void setInputLayer(Input inputLayer) {
		this.inputLayer = inputLayer;
	}
	public static void setClock(long time){
		synchronized (clkLock) {
			CLK= time;
		}
	}
	public static void incClock(){
		synchronized (clkLock) {
			CLK++;
		}
	}
	public static long getClock(){
		return CLK;
	}
	public static void resetClock(){
		synchronized (clkLock) {
			CLK = 0;
		}
	}
	
	public void save(File dest){
		
	}
	public void load(File dest){
		
	}
	/**
	 * Do clean up, release subscribers becasue we are strongly subscribed
	 */
	public void shutdown(){
		//Unsubscribe from neurons in 
		for (int i=0; i< reservoir.getNeurons().length; i++){
			String topic = reservoir.getNeurons()[i].getName();
			List subscribers = EventBus.getSubscribers(topic);
			for (int j=0; j<subscribers.size(); j++){
				EventBus.unsubscribe(topic, subscribers.get(j));
			}
		}
		
		//Unsubscribe from the input
		for (int i=0; i<inputLayer.getPublishers().length; i++){
			List subscribers = EventBus.getSubscribers(inputLayer.getPublishers()[i]);
			for (int j=0; j<subscribers.size(); j++){
				EventBus.unsubscribe(inputLayer.getPublishers()[i], subscribers.get(j));
			}
		}
		
	}
}
