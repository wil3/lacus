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

import java.util.HashMap;
import java.util.Iterator;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import edu.stevens.cpe.math.MLMatrixUtils;
import edu.stevens.cpe.reservior.neuron.Neuron;

public abstract class NeuronNetwork implements Subscriber {
	
	public abstract Neuron[] getNeurons();

	/**
	 * Get the internal state of the reservoir 
	 * Each row is a neuron with the corresponding spike trains. Neurons are ordered in the 
	 * order in which they were created.
	 * @return
	 */
	public DoubleMatrix2D getInternalFiringTimes(){
		
		//FIXME we can not cast to a specific nueron type if we are using generics
		//May need to have classes implementing different types of reservoirs
		DoubleFactory2D factory = DoubleFactory2D.dense;
		Neuron n1 = getNeurons()[0];
		DoubleMatrix2D allSpikes = MLMatrixUtils.convertArrayTo2DMatrix( n1.getFiringTimes());
		//Here the neurons are in order
		for (int i=1; i < getNeurons().length; i++){
			Neuron n = getNeurons()[i];
			allSpikes = factory.appendRows(allSpikes, MLMatrixUtils.convertArrayTo2DMatrix(n.getFiringTimes()));
		}
		return allSpikes;
	}

	public DoubleMatrix2D getInternalChargeHistory(){
		
		//FIXME we can not cast to a specific nueron type if we are using generics
		//May need to have classes implementing different types of reservoirs
		DoubleFactory2D factory = DoubleFactory2D.dense;
		Neuron n1 = getNeurons()[0];
		DoubleMatrix2D allSpikes = MLMatrixUtils.convertArrayTo2DMatrix( n1.getStateHistory());
		//Here the neurons are in order
		for (int i=1; i<getNeurons().length; i++){
			Neuron n = getNeurons()[i];
			allSpikes = factory.appendRows(allSpikes, MLMatrixUtils.convertArrayTo2DMatrix(n.getStateHistory()));
		}
		return allSpikes;
	}
	public void manipulateWeightsByType(String type, double scale, double shift) {
		for (int j=0; j<getNeurons().length; j++){
			 HashMap<String, Double> weights = getNeurons()[j].getWeightsByType(type);
			//iterate through and scale
			Iterator<String> it = weights.keySet().iterator();
			while (it.hasNext()){
				String key = it.next();
				getNeurons()[j].getWeights().put(key, (weights.get(key)* scale) + shift);
			}
		}
	}
}
