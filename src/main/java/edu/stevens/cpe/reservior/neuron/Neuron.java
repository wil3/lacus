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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventTopicSubscriber;
import org.encog.engine.network.activation.ActivationFunction;

import edu.stevens.cpe.reservior.ReservoirNetwork;
import edu.stevens.cpe.reservior.SpikeEvent;
import edu.stevens.cpe.reservior.Subscriber;
/**
 * To try and limit the subscribes, all signals are going to be mapped to the neuron and weights will be handled inside the neuron
 * 
 * This abstract class provdies the basic publish subscribe communication helper methods for any subclass. 
 * Depending on the implementation of this class abstract methods must be overriden.
 * @author wil
 *
 */
public abstract class Neuron  implements EventTopicSubscriber<SpikeEvent>, Runnable, Subscriber {
	
	public static Logger logger = Logger.getLogger(Neuron.class);
	//private Dendrite[] dendrites;
	//private ArrayList<Dendrite> dendrites = new ArrayList<Dendrite>();
	/**
	 * Synaptic strength
	 */
	public final static  double DEFAULT_WEIGHT = 1.0;

	/**
	 * Default prefix for the name
	 */
	public final static String NAME_PREFIX = "X_";

	/**
	 * The unique Id to identify this neurons so others may subscribe to it. 
	 */
	//FIXME Need to determine if we need separate name and id or if the name will be the ID
	private final String name;
	
	private ArrayList<Neuron> targetNeurons = new ArrayList<Neuron>();
	
	/**
	 * Mapping for weight to target identified by the target neuron index
	 */
	//FIXME Change this to a double [] with length == # neurons in reservoir
	private HashMap<String, Double> weights = new HashMap<String,Double>();
	/**
	 * The time that the last event occurred in continulous time in nanoseconds
	 */
	private volatile long lastEventTimestamp;
	
	
	private volatile long lastEventdiscreteTimestamp;

	/**
	 * Uniform distribution for weights connected to inputs
	 */
	private Random random = new Random(System.nanoTime());

	/**
	 * http://www.mindcreators.com/neuronbasics.htm
	 * membrane potential around -70mV.
	 */
	private double restingMembranePotential = -70;
	/**
	 * The current level of signals received from other neurons
	 */
	private volatile double membranePotential = 0.0;
		
	/**
	 * The index among the other neurons from when it was created.
	 * This is needed for being able to map back the neuron in arrays and matrices.
	 */
	private final int ID;

	public Neuron(){
		this(ReservoirNetwork.generateID()); //create an ID from last known ID in network
	}
	public Neuron(Integer id){
		this.ID = id;
		this.name = NAME_PREFIX + id;
	}
	public Neuron(String name){
		this(ReservoirNetwork.generateID(),name);
	}
	public Neuron(Integer id, String name){
		this.ID = id;
		this.name = name;
	}
	/**
	 * Depending on the number of dendrites have been assigned in the resevior which we are connected to
	 * 
	
	public void growAllDendrites(){
		//dendrites = new Dendrite[targetNeurons.length];
		for (int i=0; i<targetNeurons.length; i++){
			//dendrites[i] = new Dendrite(this, targetNeurons[i].getName());
		//	dendrites.add(new Dendrite(this, targetNeurons[i].getName()));

		}
	} */
	
	/**
	 * {@inheritDoc}
	 */
	@Override public void subscribe(String source){
		subscribe(source, random.nextDouble());
	}
	/**
	 * Subscribe to a particular source with the specified weight. Allows 
	 * for external stimulation.
	 * @param targetNeuron
	 */
	@Override public void subscribe(String source, double weight){
		//Add to global list of target neurons
		//this.targetNeurons.add(targetNeuron);
		
		//dendrites.add(new Dendrite(this, targetNeuron.getName()));
		//int targetID = targetNeuron.getID();
		
		weights.put(source,weight);
		
		//
		//Must be strongly typed to prevent from being garbaged collected when there are many subscriptions
		//
		//TODO what is we are already subscribed what happens?
		EventBus.subscribeStrongly(source, this);
		
	}
	@Override public void subscribe(String [] sources){
		for (int i=0; i<sources.length; i++){
			subscribe(sources[i]);
		}
	}

	/**
	 * Subscribe to all the neurons and set the corresponding connection strength
	 * @param neurons
	 */
	//TODO all I really need here are the neurons names, do we ever need an instance of the
	//actual neuron?
	@Override public void subscribe(HashMap<String,Double> sources){
		//Due to the nature of the eventbus we need to have the addAllTargetNeurons and addTargetNeurons decoupled a bit

		//this.targetNeurons.addAll(Arrays.asList(neurons));

		Iterator<String> it = sources.keySet().iterator();
		while (it.hasNext()){
			String sourceName = it.next();
			subscribe(sourceName, sources.get(sourceName));
		}
	}
	
	/**
	 * @return the iD
	 */
	public int getID() {
		return ID;
	}
	/*public Dendrite[] getDendrites() {
		return dendrites;
	}
	public void setDendrites(Dendrite[] dendrites) {
		this.dendrites = dendrites;
	}*/
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/*
	public Neuron[] getConnectedNeurons() {
		return targetNeurons;
	}
	public void setConnectedNeurons(Neuron[] connectedNeurons) {
		this.targetNeurons = connectedNeurons;
	}
	*/
	/**
	 * @return the membranePotential
	 */
	public double getMembranePotential() {
		return membranePotential;
	}
	/**
	 * @param membranePotential the membranePotential to set
	 */
	public void setMembranePotential(double membranePotential) {
		this.membranePotential = membranePotential;
	}
	/**
	 * @return the lastEventTimestamp
	 */
	public long getLastEventTimestamp() {
		return lastEventTimestamp;
	}
	/**
	 * @return the lastEventdiscreteTimestamp
	 */
	public long getLastEventdiscreteTimestamp() {
		return lastEventdiscreteTimestamp;
	}
	public synchronized double getCurrentMembranePotential(){
				
		return membranePotential;
	
	}
	/**
	 * @return the weights
	 */
	public HashMap<String, Double> getWeights() {
		return weights;
	}
	/**
	 * Spike from another neuron occured
	 */
	//static int c = 0;
	@Override public void onEvent(String topic, SpikeEvent data) {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		ps.printf("%4s:%1.9f \t => \t %s \t t= %d \t %1.9f",topic, data.getAmplitude(), name, data.getTime(),getMembranePotential());
		//logger.debug(baos.toString());
		
		setLastEventdiscreteTimestamp(data.getTime());
		updateMembranePotential(data.getAmplitude()*weights.get(topic), data.getPulseWidth());
	
	}
	/**
	 * Not currently used may when dynamically removing creating
	 * @param targetNeuron
	 */
	public void removeTargetNeuron(Neuron targetNeuron){
		this.targetNeurons.remove(targetNeuron);
	}
	/**
	 * Override this
	 */
	public abstract void reset();
	
	/**
	 * The state history contains information of what the current value of the neuron is.
	 * @return
	 */
	public abstract double [] getStateHistory();
	
	/**
	 * When the neurons is stimulated and reaches a threshold. For sigmoid, tanh neurons this may be when sum is > -1|1, for spiking neurons this
	 * may be when sum is > threshold.
	 * @return
	 */
	public abstract double [] getFiringTimes();

	/**
	 * @param lastEventTimestamp the lastEventTimestamp to set
	 */
	public void setLastEventTimestamp(long lastEventTimestamp) {
		this.lastEventTimestamp = lastEventTimestamp;
	}

	/**
	 * @param weights the weights to set
	 */
	public void setWeights(HashMap<String, Double> weights) {
		this.weights = weights;
	}
	public void setWeightsByType(String type, double [] newWeights){
		int index = 0;			
		List<String> keys = new ArrayList<String>(weights.keySet());
		Collections.sort(keys);
		
		for (int j=0; j<keys.size(); j++){
			if (keys.get(j).startsWith(type)){
				weights.put(keys.get(j), newWeights[index]);
				index++;
			}
		}
	}
	public HashMap<String, Double> getWeightsByType(String type){
		 HashMap<String, Double> w = new HashMap<String,Double>();
		List<String> keys = new ArrayList<String>(weights.keySet());		
		for (int j=0; j<keys.size(); j++){
			if (keys.get(j).startsWith(type)){
				w.put(keys.get(j), weights.get(keys.get(j)));
			}
		}
		return w;
	}
	/**
	 * @param lastEventdiscreteTimestamp the lastEventdiscreteTimestamp to set
	 */
	public void setLastEventdiscreteTimestamp(long lastEventdiscreteTimestamp) {
		this.lastEventdiscreteTimestamp = lastEventdiscreteTimestamp;
	}
	
	@Override public String toString(){
		return "{name:" + getName() + " charge:" + getMembranePotential() + "}";
	}
	
	/**
	 * Subclasses must implement this method. 
	 * Receive all incoming signals from dendrites and update potential
	 */
	public abstract void updateMembranePotential(double current, long time);
	
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
