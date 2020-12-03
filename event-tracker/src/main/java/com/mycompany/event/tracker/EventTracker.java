/*
 * Copyright 2020 Rocco Varela. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycompany.event.tracker;

import java.util.HashMap;
import java.util.Map;

/** The EventTracker class provides an simple API for tracking {@link Event} signals. */
public class EventTracker {
  // map of unique events
  private Map<String, Event> eventMap;

  public EventTracker() {
    eventMap = new HashMap<>();
  }

  /**
   * Increments the counter of the specified event. A new {@link Event} will be created if it does
   * not exist.
   *
   * @param eventId The string ID of the event to update
   * @return The updated signal count of the event
   */
  public Long signalEvent(String eventId) {
    Event event = eventMap.get(eventId);
    if (event == null) {
      event = new Event(eventId);
      eventMap.put(eventId, event);
    }
    return event.signalEvent();
  }

  /**
   * Retrieves the number of signals recorded for the specified {@link Event} over the requested
   * lookback window.
   *
   * @param eventId Name of the event to query
   * @param timeWindowInSecs Length of time in seconds until the current time
   * @return The event count within the specified window, null if the event does not exist
   */
  public Long getRecentEventCount(String eventId, int timeWindowInSecs) {
    Event event = eventMap.get(eventId);
    return (event == null) ? null : event.getRecentEventCount(timeWindowInSecs);
  }
}
