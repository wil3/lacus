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

/**
 * Any group of multiple nodes implementing publish subscribe architechures
 * @author wil
 *
 */
public interface Subscriber {

	/**
	 * Subscribe to a single source. The weight chosen will be the default implementation for the agent.
	 * @param source
	 */
	public void subscribe(String source);
	/**
	 * 
	 * @param source
	 * @param weight
	 */
	public void subscribe(String source, double weight);

	/**
	 * Subscribe to multiple sources.
	 * @param sources
	 */
	public void subscribe(String [] sources);
	/**
	 * Mapping defining the source names and the corresponding weight that should be used.
	 * @param sources
	 */
	public void subscribe(HashMap<String,Double> sources);
	/**
	 * Subscribe to a single source. The weight chosen will be the default implementation for the agent.
	 * @param source
	 */
	public void unsubscribe(String source);
	/**
	 * 
	 * @param source
	 * @param weight
	 */
	public void unsubscribe(String source, double weight);

	/**
	 * Subscribe to multiple sources.
	 * @param sources
	 */
	public void unsubscribe(String [] sources);
	/**
	 * Mapping defining the source names and the corresponding weight that should be used.
	 * @param sources
	 */
	public void unsubscribe(HashMap<String,Double> sources);
}
