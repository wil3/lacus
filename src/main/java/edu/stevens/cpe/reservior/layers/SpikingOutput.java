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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.bushe.swing.event.EventBus;
import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.util.arrayutil.NormalizationAction;
import org.encog.util.arrayutil.NormalizedField;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

import edu.stevens.cpe.math.MLMatrixUtils;
import edu.stevens.cpe.reservior.NeuronNetwork;
import edu.stevens.cpe.reservior.Reservoir;
import edu.stevens.cpe.reservior.Subscriber;
import edu.stevens.cpe.reservior.neuron.IFSpikingNeuron;
import edu.stevens.cpe.reservior.neuron.LIFSpikingNeuron;
import edu.stevens.cpe.reservior.neuron.Neuron;

public class SpikingOutput  <T extends Neuron> extends NeuronNetwork implements ReadoutFunction {
	private static Logger logger = Logger.getLogger(SpikingOutput.class);

	private static final String ID_PREFIX = "Y_";
	public static double DEFAULT_WEIGHT = 0;
	private Reservoir<?> reservior;
	private T[] nodes;
	private Class<T> neuronClass;
	private double[] lastPolledVoltages;
	private final int stabilizedIterations = 5;
	private int numberOutputs;
	/**
	 * Uniform distribution for weights connected to inputs
	 */
	private Random random = new Random(System.nanoTime());

	/**
	 * 
	 * @param reservior The reservoir to connect too
	 * @param numberOutputs Number of outputs
	 * @param clazz The class of the output neurons
	 */
	public SpikingOutput(Reservoir<?> reservior, int numberOutputs, Class<T> clazz){
		this.numberOutputs = numberOutputs;
		this.reservior = reservior;
		this.neuronClass = clazz;

		try {
			createOutputNodes();
			addOutConnections();
			addFeedbackConnections();
			
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	
	}
	
	private void createOutputNodes() throws InstantiationException, IllegalAccessException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException{
		nodes=	(T[])Array.newInstance(neuronClass,getNumberOutputs());
	//	neurons = new [numberOutputs];
		lastPolledVoltages = new double[getNumberOutputs()];
		for (int i=0; i<getNumberOutputs(); i++){
//			neurons[i] =	neuronClass.getConstructor(String.class).newInstance(ID_PREFIX + (reservior.getNeurons().length+i) + "");	
			nodes[i] =	neuronClass.getConstructor(String.class, Boolean.class).newInstance(ID_PREFIX + i + "",true);			

			lastPolledVoltages[i] = 0;
		}
	}
	/**
	 * Subscribe to all spike events from all the neurons
	 */
	private void addOutConnections(){
		HashMap<String,Double> defaults = defaultConnections();
		for (int i=0; i<nodes.length; i++){			
			//Have each output neuron connect to every single neuron in the reservoir with a default weight
			nodes[i].subscribe(defaults);
		}
	}
	/**
	 * Feedback connections.
	 * Go through the neurons in the reservoir and add an addition dendrite subscribed to the input node
	 */
	private void addFeedbackConnections(){
		for (int i=0; i<nodes.length; i++){
			for (int j=0; j<reservior.getNeurons().length; j++){
				reservior.getNeurons()[j].subscribe(nodes[i].getName(), random.nextDouble());
			}
		}
	}
	/**
	 * Because these connnections will be trained init them to static value
	 * @return
	 */
	private HashMap<String,Double> defaultConnections(){
		HashMap<String,Double> connections = new HashMap<String,Double>();

		for (int i=0; i<reservior.getNeurons().length; i++){
			connections.put(reservior.getNeurons()[i].getName(), DEFAULT_WEIGHT);
		}
		return connections;
		
	}
	public boolean isOutputsStabilzed(){
		logger.info("Polling: " );
		for (int i=0; i<nodes.length; i++){
			if (nodes[i].getMembranePotential() == lastPolledVoltages[i]){
				
			} 
		}
		return false;
	}
	
	public DoubleMatrix2D getState(){
	
		return MLMatrixUtils.convertArrayTo2DMatrix((nodes[0]).getFiringTimes());
	}

	/**
	 * 
	 * @return An array containing all of the times in which spikes occured in discrete time
	 */
	public MLData getOutput2() {
		final MLData output = new BasicMLData(nodes.length);
		logger.debug("Output Spike Trains:");
		for (int i=0; i<nodes.length; i++){
			double [] spikes = nodes[i].getFiringTimes();
			logger.info(nodes[i].toString() + " = " + Arrays.toString(spikes));
		}
		return output;
	}
	@Override
	public MLData getOutput() {
		
		//waitTillStabilized();
		
		final MLData output = new BasicMLData(nodes.length);
		NormalizedField norm = new NormalizedField(NormalizationAction.Normalize, 
				null,IFSpikingNeuron.THRESHOLD,0,1,0);
		
		//now stabilized
		for (int i=0; i<nodes.length; i++){
			//output.setData(i,norm.normalize(nodes[i].getMembranePotential()));
			output.setData(i,nodes[i].getMembranePotential());

		}
		
		return output;
	}


	/**
	 * @return the neurons
	 */
	public T[] getNeurons() {
		return nodes;
	}

	/**
	 * @param neurons the neurons to set
	 */
	public void setNeurons(T[] neurons) {
		this.nodes = neurons;
	}
	/*
	 * public void setWeightsByType(String type, double [] newWeights){
		int index = 0;
		for (int i=0; i<getNeurons().length; i++){
			HashMap<String, Double> weights = getNeurons()[i].getWeights();
			
			List<String> keys = new ArrayList<String>(weights.keySet());
			Collections.sort(keys);
			
			for (int j=0; j<keys.size(); j++){
				if (keys.get(j).startsWith(type)){
					weights.put(keys.get(j), newWeights[index]);
				}
			}
		}
		
	}
	 */
	public void setWeightsByType(String type, double [] newWeights){
		int index = 0;
		for (int i=0; i<nodes.length; i++){
			HashMap<String, Double> weights = nodes[i].getWeights();
			
			List<String> keys = new ArrayList<String>(weights.keySet());
			Collections.sort(keys);
			
			for (int j=0; j<keys.size(); j++){
				if (keys.get(j).startsWith(type)){
					weights.put(keys.get(j), newWeights[index]);
					index++;
				}
			}
		}
		
	}
	public void setWeights(double [] newWeights){
		int index = 0;
		for (int i=0; i<getNeurons().length; i++){
			HashMap<String, Double> weights = getNeurons()[i].getWeights();
			Iterator<String> it = weights.keySet().iterator();
			while (it.hasNext()){
				String id = it.next();
				weights.put(id, newWeights[index]);
				index++;
			}
		}
	}
	@Override public String toString(){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		for (int i=0; i<getNeurons().length; i++){
			HashMap<String, Double> weights = getNeurons()[i].getWeights();
			Iterator<String> it = weights.keySet().iterator();
			while (it.hasNext()){
				String id = it.next();
				double weight = weights.get(id);
				ps.print("{id=" + id + ",w=" + weight + "},");
			}
		}
		return baos.toString();
	}

	@Override
	public void reset() {
		//reset all neurons
		for (int i=0; i<nodes.length; i++){
			nodes[i].reset();
		}
	}

	@Override
	public void subscribe(String source) {
		for (int j=0; j<getNeurons().length; j++){
			getNeurons()[j].subscribe(source, random.nextDouble());
		}		
	}

	@Override
	public void subscribe(String source, double weight) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void subscribe(String[] sources) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void subscribe(HashMap<String, Double> sources) {
		// TODO Auto-generated method stub
		
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

	@Override
	public int getNumberOutputs() {
		return numberOutputs;
	}

	@Override
	public void setNumberOutputs(int numberOutputs) {
		this.numberOutputs = numberOutputs;
	}

	@Override
	public void manipulateFeedbackConnections(double scale, double shift) {
		for (int j=0; j<reservior.getNeurons().length; j++){
			 HashMap<String, Double> weights = reservior.getNeurons()[j].getWeightsByType(ID_PREFIX);
			//iterate through and scale
			Iterator<String> it = weights.keySet().iterator();
			while (it.hasNext()){
				String key = it.next();
				reservior.getNeurons()[j].getWeights().put(key, weights.get(key)* scale + shift);
			}
		}
	}
	

	@Override
	public void shiftFeedbackConnections(double shift) {
		// TODO Auto-generated method stub
		
	}
	

}
