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
package edu.stevens.cpe.reservior.topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import edu.stevens.cpe.reservior.ReserviorException;
import edu.stevens.cpe.reservior.neuron.Neuron;

public class FullyConnectedNetworkTopology implements NetworkTopology{

	private int rows;
	private int cols;
	public FullyConnectedNetworkTopology(int rows, int cols){
		this.rows = rows;
		this.cols = cols;
	}
	@Override public HashMap<Integer,Double> getConnections(int neuronIndex) throws ReserviorException {
/*
		int index = getIndex(neurons, neuron);
		if (index < 0){
			throw new ReserviorException("Neuron " + neuron.getName() + " not found.");
			
		}
		ArrayList<Neuron> neighbors = new ArrayList<Neuron>();
		int neuronRow = (int)Math.floor((double)index/(double)rows);
		int neuronCol = index%cols;
		
		//If there are neurons to the left add them
		if (neuronCol != 0){
			neighbors.add(neurons[index-1]);
		}
		//If there are neurons to the right add them
		if (neuronCol != (cols-1)){
			neighbors.add(neurons[index+1]);
		}
		//To the top
		if (neuronRow != 0){
			neighbors.add(neurons[index-cols]);
		}
		//To the bottom
		if (neuronRow != (rows-1)){
			neighbors.add(neurons[index+cols]);

		}
		
		return neighbors.toArray(new Neuron[neighbors.size()]);
		*/
		return null;
	}

	private int getIndex(Neuron[] neurons, Neuron neuron){
		int index = -1;
		for (int i=0; i< neurons.length; i++){
			if (neurons[i].toString().equalsIgnoreCase(neuron.toString())){
				index = i;
				break;
			}
		}
		return index;
	}
	
}
