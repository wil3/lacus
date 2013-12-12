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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import edu.stevens.cpe.reservior.neuron.Neuron;

public class CycleNetworkTopology implements NetworkTopology{

	

	@Override
	public HashMap<Integer,Double> getConnections(int neuronIndex) {
		/*
		Neuron[] neighbors = new Neuron[]{};
		if (neurons.length > 1){
			
			int index = Arrays.binarySearch(neurons, neuron, new Comparator<Neuron>() {
			    @Override
			    public int compare(Neuron n1, Neuron n2) {
			    	int compare = 0;
			    	if (!n1.getName().equalsIgnoreCase(n2.getName())){
			    		compare = 1;
			    	}
			        return compare;
			    }
			});

			
			if (index >= 0){
				if (neurons.length == 2 ){
					neighbors = new Neuron[]{neurons[(neurons.length-1)-index]};

				} else {
					neighbors = new Neuron[2];
					
					int pre_index = (index == 0) ? (neurons.length - 1) : (index -1);
					neighbors[0] = neurons[pre_index];
					
					int next_index = ((neurons.length-1) == index) ? 0 : (index + 1);
					neighbors[1] = neurons[next_index];
				}
			}
		}
		return neighbors;
		*/
		return null;
	}
	
}
