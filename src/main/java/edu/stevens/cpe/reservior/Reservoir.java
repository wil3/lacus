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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.log4j.Logger;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventServiceExistsException;
import org.bushe.swing.event.EventServiceLocator;
import org.bushe.swing.event.DiscreteTimeThreadSafeEventService;
import org.bushe.swing.event.ThreadSafeEventService;
import org.encog.engine.network.activation.ActivationFunction;

import edu.stevens.cpe.reservior.neuron.Neuron;
import edu.stevens.cpe.reservior.topology.NetworkTopology;

/**
 * @author wil
 *
 * @param <T>
 */
public class Reservoir <T extends Neuron> extends NeuronNetwork  {
	public static Logger logger = Logger.getLogger(Reservoir.class);
	public static long startTime = 0;
	private Class<T> neuronClass;
	private  NetworkTopology topology;
	private int neuronCount = 0;
	
	/**
	 * To be used to know what the next Id should be for dynamically adding neurons to the reservoir
	 */
	private int lastId =0;
	/**
	 * Flat network
	 */
	private T[] neurons;
	
	/**
	 * Uniform distribution for weights connected to inputs
	 */
	private Random random = new Random(System.nanoTime());

	 
	public Reservoir(int neuronCount, NetworkTopology topology, Class<T> clazz) throws ReserviorException{
		startTime = System.nanoTime();
		this.neuronCount = neuronCount; 
		this.topology = topology;	
		this.neuronClass = clazz;
		
		try {
			
			createNeurons();
			//TODO move me to network
			System.setProperty("org.bushe.swing.event.eventBusClass", "org.bushe.swing.event.DiscreteTimeThreadSafeEventService");
			EventServiceLocator.setEventService("DiscreteTimeThreadSafeEventService", new DiscreteTimeThreadSafeEventService());
			initConnections();

		} catch (IllegalArgumentException e) {
			throw new ReserviorException(e);
		} catch (SecurityException e) {
			throw new ReserviorException(e);
		} catch (InstantiationException e) {
			throw new ReserviorException(e);
		} catch (IllegalAccessException e) {
			throw new ReserviorException(e);
		} catch (InvocationTargetException e) {
			throw new ReserviorException(e);
		} catch (NoSuchMethodException e) {
			throw new ReserviorException(e);
		} catch (EventServiceExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	/**
	 * @return the neurons
	 */
	public Neuron[] getNeurons() {
		return neurons;
	}

	/**
	 * @return the neuronCount
	 */
	public int getNeuronCount() {
		return neuronCount;
	}

	public T getNeuronById(String id){
		T neuron = null;
		for (int i=0; i<neurons.length; i++){
			if (neurons[i].getName().equalsIgnoreCase(id)){
				neuron = neurons[i];
				break;
			}
		}
		return neuron;
	}
	
	/**
	 * Initialize all the neuron
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	private void createNeurons() throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException{
		neurons=	(T[])Array.newInstance(neuronClass,neuronCount);

		for (int i=0; i<neuronCount; i++){
			neurons[i] =	neuronClass.getConstructor(Integer.class).newInstance(i);			
		}
	}
	
	/**
	 * Subscribe each neuron to events from its neighbors
	 * @throws ReserviorException 
	 */
	private void initConnections() throws ReserviorException{
		
		for (int i=0; i<neurons.length; i++){
			HashMap<Integer,Double> indexMapping = topology.getConnections(i);
			//Using the indexes obtain the actual neurons
			HashMap<String,Double> connections = new HashMap<String,Double>();
			Iterator<Integer> it = indexMapping.keySet().iterator();
			while (it.hasNext()){
				Integer index = it.next();
				connections.put(neurons[index].getName(), indexMapping.get(index));
			}
			neurons[i].subscribe(connections);

		}
	}

	/**
	 * Subscribe all neurons in the reservoir to the specified sourcce
	 * @param publisherName
	 */
	public void subscribeAll(String source){
		
	}

	/**
	 * Reset all of the neurons to there original state and reset the clock
	 */
	public void reset(){
		//Clear the EventBus
		
		//reset all neurons
		for (int i=0; i<neurons.length; i++){
			neurons[i].reset();
		}
		//reset clock
		ReservoirNetwork.resetClock();
	}

	/**
	 * @param neurons the neurons to set
	 */
	public void setNeurons(T[] neurons) {
		this.neurons = neurons;
	}

	/**
	 * @param neuronCount the neuronCount to set
	 */
	public void setNeuronCount(int neuronCount) {
		this.neuronCount = neuronCount;
	}

	@Override
	public void subscribe(String source) {		
		for (int j=0; j<getNeurons().length; j++){
			getNeurons()[j].subscribe(source, random.nextDouble());
		}
	}

	@Override
	public void subscribe(String source, double weight) {
		
	}

	/**
	 * Subscribe every neuron in the reservoir to these sources.
	 */
	@Override public void subscribe(String[] sources) {
		for (int i=0; i<sources.length;i++){
			subscribe(sources[i]);
		}
	}

	@Override
	public void subscribe(HashMap<String, Double> sources) {
		
	}

	@Override
	public void unsubscribe(String source) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unsubscribe(String source, double weight) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unsubscribe(String[] sources) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unsubscribe(HashMap<String, Double> sources) {
		// TODO Auto-generated method stub
		
	}
	
	
}
