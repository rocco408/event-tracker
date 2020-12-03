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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class AbstractEventTest extends RandomizedTest {

  /**
   * Creates an {@link Event} with some random ID and calls {@link
   * AbstractEventTest#getRecentEventCount(Event, int)}
   */
  protected void getRecentEventCount(int numSimulatedSecs) throws InterruptedException {
    Event event = new Event(randomRealisticUnicodeOfLength(100));
    getRecentEventCount(event, numSimulatedSecs);
  }

  /**
   * Test Setup: 1. create event 2. wait for event to record first count of 0 3. send N signals 4.
   * verify recent event count
   *
   * @param event A preconfigured Event object to test.
   * @param numSimulatedSecs Number of seconds and signals to simulate in the event cache
   * @throws InterruptedException
   */
  protected void getRecentEventCount(Event event, int numSimulatedSecs)
      throws InterruptedException {
    // wait for the event cache to record its first count
    assertTrue(waitForEventCacheWindowBuild(event, 1L, 3000));

    int startTimeInSecs = Math.toIntExact(System.currentTimeMillis() / 1000);
    int nextTimeInSecs = startTimeInSecs;
    int numSimulatedSignals = numSimulatedSecs;
    for (int i = 0; i < numSimulatedSecs; i++) {
      // signal several events with artificial timestamps
      event.signalEvent(nextTimeInSecs++);
    }

    Long count = event.getRecentEventCount(nextTimeInSecs, numSimulatedSecs);
    assertNotNull(count);
    assertEquals(numSimulatedSignals, count.longValue());
  }

  /**
   * Waits for the Event cache window to grow to the specified size
   *
   * @param event Event that is building its cache
   * @param timeWindowInSecs The target size of the Event cache window
   * @param timeoutInMillis How long to wait before giving up
   * @return boolean indicating whether the cache window grew to the desired size
   * @throws InterruptedException
   */
  protected boolean waitForEventCacheWindowBuild(
      Event event, long timeWindowInSecs, long timeoutInMillis) throws InterruptedException {
    long start = System.currentTimeMillis();

    while (true) {
      if (event.getEstimatedCacheSize() >= timeWindowInSecs) {
        return true;
      } else if ((System.currentTimeMillis() - start) > timeoutInMillis) {
        return false;
      } else {
        Thread.sleep(1000);
      }
    }
  }

  protected boolean waitForEventCacheCountToFlatline(
      Event event, int desiredConsecutiveRepeatCounts, long timeoutInMillis)
      throws InterruptedException {
    long start = System.currentTimeMillis();
    long previousCacheCount = event.getEstimatedCacheSize();
    int numTimesCacheCountRemainedConstant = 0;
    while (true) {
      long currentCacheCount = event.getEstimatedCacheSize();
      if (currentCacheCount == previousCacheCount) {
        numTimesCacheCountRemainedConstant++;
        if (numTimesCacheCountRemainedConstant == desiredConsecutiveRepeatCounts) return true;
      } else if ((System.currentTimeMillis() - start) > timeoutInMillis) {
        return false;
      } else {
        previousCacheCount = currentCacheCount;
        numTimesCacheCountRemainedConstant = 0;
        Thread.sleep(1000);
      }
    }
  }

  protected boolean waitForEventCacheCountToFlatline(
      EventTracker eventTracker,
      String eventId,
      int desiredConsecutiveRepeatCounts,
      long timeoutInMillis)
      throws InterruptedException {
    long start = System.currentTimeMillis();
    long previousCacheCount = eventTracker.getRecentEventCount(eventId, 1);
    int numTimesCacheCountRemainedConstant = 0;
    while (true) {
      long currentCacheCount = eventTracker.getRecentEventCount(eventId, 1);
      if (currentCacheCount == previousCacheCount) {
        numTimesCacheCountRemainedConstant++;
        if (numTimesCacheCountRemainedConstant == desiredConsecutiveRepeatCounts) return true;
      } else if ((System.currentTimeMillis() - start) > timeoutInMillis) {
        return false;
      } else {
        previousCacheCount = currentCacheCount;
        numTimesCacheCountRemainedConstant = 0;
        Thread.sleep(1000);
      }
    }
  }
}
