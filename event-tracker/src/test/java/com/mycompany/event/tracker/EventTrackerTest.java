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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** EventTrackerTest tests the public API exposed through {@link EventTracker}. */
public class EventTrackerTest extends AbstractEventTest {

  /** */
  @Test
  public void testSignalEvent() {
    EventTracker eventTracker = new EventTracker();
    String eventId = randomRealisticUnicodeOfLength(100);
    long count = eventTracker.signalEvent(eventId);
    assertEquals(1L, count);
    count = eventTracker.signalEvent(eventId);
    assertEquals(2L, count);
  }

  @Test
  public void testGetRecentEventCount() throws InterruptedException {
    EventTracker eventTracker = new EventTracker();
    String eventId = randomRealisticUnicodeOfLength(100);
    int numIntervals = 5;
    long count = 0;
    for (int i = 0; i < numIntervals; i++) {
      count = eventTracker.signalEvent(eventId);
      Thread.sleep(1500);
    }
    assertEquals(5L, count);

    // expect count increase from near the beginning
    assertTrue(eventTracker.getRecentEventCount(eventId, numIntervals - 1) > 1);
    assertTrue(waitForEventCacheCountToFlatline(eventTracker, eventId, 3, 5000));
  }
}
