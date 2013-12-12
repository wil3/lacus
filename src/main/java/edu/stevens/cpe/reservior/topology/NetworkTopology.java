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

import java.util.HashMap;

import edu.stevens.cpe.reservior.ReserviorException;
import edu.stevens.cpe.reservior.neuron.Neuron;

public interface NetworkTopology {

	/**
	 * 
	 * @param neurons
	 * @param neuron
	 * @return	The neuron and the assocaited weight used to attach to it.
	 * @throws ReserviorException
	 */
	public HashMap<Integer,Double> getConnections(int neuronIndex) throws ReserviorException ;
	
}
