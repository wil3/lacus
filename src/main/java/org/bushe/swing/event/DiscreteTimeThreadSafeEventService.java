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
package org.bushe.swing.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bushe.swing.event.Logger.Level;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import edu.stevens.cpe.reservior.ReservoirNetwork;
import edu.stevens.cpe.reservior.SpikeEvent;

public class DiscreteTimeThreadSafeEventService extends ThreadSafeEventService{
	
	/**
	 * Allow for multiple values for single key. Values are stored in an ArrayList, when getting an item a list is returned.
	 * The key is the discrete time. The value is an ArrayList of BufferedEventObjects
	 */
	 ListMultimap<Long,BufferedEventObject> eventBuffer = ArrayListMultimap.create();
	public DiscreteTimeThreadSafeEventService(){
		super();
	}
	 @Override protected void publish(final Object event, final String topic, final Object eventObj,
	           final List subscribers, final List vetoSubscribers, StackTraceElement[] callingStack) {

		
	
		 
		 //Continue with normal operation
		 SpikeEvent spikeEvent = (SpikeEvent)eventObj;
		 long eventTime;
		 if (spikeEvent != null){
			 eventTime = spikeEvent.getTime();
		 } else {
			 return;
		 }
		 if (eventTime != ReservoirNetwork.getClock()){
			 BufferedEventObject obj = new BufferedEventObject();
			 obj.setEventObj(eventObj);
			 obj.setTopic(topic);
			 eventBuffer.put(eventTime, obj);
			 
		 } else {
			 //First this is first, try and see if there are any buffered events for the current time
			 List<BufferedEventObject> buff = eventBuffer.get(ReservoirNetwork.getClock());
			 if (buff.size() != 0){
				 //remove from the list since you just re-published it, need to make a copy because the current
				 //buff is a reference to the list 
				 List<BufferedEventObject> eventsToRepublish = new ArrayList<BufferedEventObject>(buff);
				 eventBuffer.removeAll(ReservoirNetwork.getClock());
				 for (BufferedEventObject obj : eventsToRepublish){
					// eventBuffer.remove(CLOCK, obj);
					 publish(obj.getTopic(), obj.getEventObj());
				 }

			 }
			 
			 if (event == null && topic == null) {
		         throw new IllegalArgumentException("Can't publish to null topic/event.");
		      }
	
		      setStatus(PublicationStatus.Initiated, event, topic, eventObj);
		      //topic or event
	
		      //Check all veto subscribers, if any veto, then don't publish or cache
		 
		      setStatus(PublicationStatus.Queued, event, topic, eventObj);
		    
	
		      addEventToCache(event, topic, eventObj);
	
		      if (subscribers == null || subscribers.isEmpty()) {
		         if (LOG.isLoggable(Level.DEBUG)) {
		            LOG.debug("No subscribers for event or topic. Event:" + event + ", Topic:" + topic);
		         }
		      } else {
		         if (LOG.isLoggable(Level.DEBUG)) {
		            LOG.debug("Publishing to subscribers:" + subscribers);
		         }
		         setStatus(PublicationStatus.Publishing, event, topic, eventObj);
		         for (int i = 0; i < subscribers.size(); i++) {
		            Object eh = subscribers.get(i);
		            if (event != null) {
		               EventSubscriber eventSubscriber = (EventSubscriber) eh;
		               long start = System.currentTimeMillis();
		               try {
		                  eventSubscriber.onEvent(event);
		                 // checkTimeLimit(start, event, eventSubscriber, null);
		               } catch (Throwable e) {
		                 // checkTimeLimit(start, event, eventSubscriber, null);
		                  handleException(event, e, callingStack, eventSubscriber);
		               }
		            } else {
		               EventTopicSubscriber eventTopicSubscriber = (EventTopicSubscriber) eh;
		               try {
		                  eventTopicSubscriber.onEvent(topic, eventObj);
		               } catch (Throwable e) {
		                  onEventException(topic, eventObj, e, callingStack, eventTopicSubscriber);
		               }
		            }
		         }
		      }
		      setStatus(PublicationStatus.Completed, event, topic, eventObj);   
		      
		 }
	   }

		/**
		 * Any values left in the buffer execute them
		 */
		public void flush(){
			long next = 0;
			while (( next = getNextTime()) != 0){
				//Move clock to next time 
				ReservoirNetwork.setClock(next);
				List<BufferedEventObject> buff = eventBuffer.get(next);
				 if (buff != null){
					 //remove from the list since you just re-published it and the
					 // first things its going to do is see if there is anything buffered
					 eventBuffer.removeAll(next);
					 for (BufferedEventObject obj : buff){
						 publish(obj.getTopic(), obj.getEventObj());
					 }

				 }
			}
		}
		private long getNextTime(){
			long nextTime = 0;
			//Start at the current time 
			Set<Long> times = eventBuffer.keySet();
			ArrayList<Long> timeList = new ArrayList<Long>(times);
			Collections.sort(timeList);
			if (timeList.size() != 0){
				nextTime = timeList.get(0);
			}
			return nextTime;
		}
		/**
		 * Buffer this event to be used later
		 * @author wil
		 *
		 */
		private class BufferedEventObject {
			private String topic;
			private Object eventObj;
			/**
			 * @return the eventObj
			 */
			public Object getEventObj() {
				return eventObj;
			}
			/**
			 * @param eventObj the eventObj to set
			 */
			public void setEventObj(Object eventObj) {
				this.eventObj = eventObj;
			}
			/**
			 * @return the topic
			 */
			public String getTopic() {
				return topic;
			}
			/**
			 * @param topic the topic to set
			 */
			public void setTopic(String topic) {
				this.topic = topic;
			}
			
			@Override public String toString(){
				 SpikeEvent spikeEvent = (SpikeEvent)eventObj;
				 long eventTime = spikeEvent.getTime();
				return "Topic=" + topic + " Time="+ eventTime;
			}
		}
}
