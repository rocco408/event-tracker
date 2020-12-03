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
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** EventTest tests all internal functionality provided by {@link Event}. */
public class EventTest extends AbstractEventTest {
  static final Logger logger = LoggerFactory.getLogger(EventTest.class);

  /** Tests the count incrementing mechanism, the count should increase with every call. */
  @Test
  public void testIncrementCounter() {
    Event event = new Event(randomRealisticUnicodeOfLength(100));
    assertEquals(1L, event.incrementCounter());
    assertEquals(2L, event.incrementCounter());
    assertEquals(3L, event.incrementCounter());
  }

  /**
   * Tests that the count extraction method works, getCount() should not modify the count but simply
   * return it.
   */
  @Test
  public void testGetCount() {
    Event event = new Event(randomRealisticUnicodeOfLength(100));
    assertEquals(0L, event.getCount());
    assertEquals(1L, event.incrementCounter());
    assertEquals(1L, event.getCount());
  }

  /**
   * Tests that the event signalling mechanism is working, the event count should increase with
   * every signal.
   */
  @Test
  public void testSignalEvent() {
    Event event = new Event(randomRealisticUnicodeOfLength(100));
    assertEquals(0L, event.getCount());
    assertEquals(1L, event.signalEvent());
    assertEquals(2L, event.signalEvent());
    assertEquals(3L, event.signalEvent());
  }

  /** Tests that a proper error is thrown when an invalid time window below 1 is used. */
  @Test
  public void testGetRecentEventCountWithInvalidTimeWindow() {
    Event event = new Event(randomRealisticUnicodeOfLength(100));
    String expectedErrorMessage =
        String.format("The time window size must be greater than 0, you provided 0");
    try {
      event.getRecentEventCount(1, 0);
      fail(
          String.format(
              "Expected getRecentEventCount(1, 0) to throw an AssertionError containing the message '%s'",
              expectedErrorMessage));
    } catch (AssertionError ex) {
      assertEquals(expectedErrorMessage, ex.getMessage());
    }
  }

  /**
   * Tests that a proper error is thrown when attempting to get an event count with an invalid
   * timestamp.
   */
  @Test
  public void testGetRecentEventCountWithInvalidCurrentTime() {
    Event event = new Event(randomRealisticUnicodeOfLength(100));
    String expectedErrorMessage =
        String.format(
            "The current timestamp in secs must be greater than 0, the current time is 0");
    try {
      event.getRecentEventCount(0, 1);
      fail(
          String.format(
              "Expected getRecentEventCount(0, 1) to throw an AssertionError containing the message '%s'",
              expectedErrorMessage));
    } catch (AssertionError ex) {
      assertEquals(expectedErrorMessage, ex.getMessage());
    }
  }

  /** Tests that accurate counts are retrieved when the signal count crosses the Long.MAX_VALUE */
  @Test
  public void testGetRecentEventCountWithSignalCountWrapping() throws InterruptedException {
    Event event = new Event(randomRealisticUnicodeOfLength(100));
    event.setCounter(Long.MAX_VALUE);
    getRecentEventCount(event, 1);
  }

  /**
   * Tests getting the event count above zero and below the max window size.
   *
   * @throws InterruptedException
   */
  @Test
  public void testGetRecentEventCountBelowMaxWindow() throws InterruptedException {
    getRecentEventCount(1);
    getRecentEventCount(Event.MAX_SECONDS_ALLOWED / 2);
    getRecentEventCount(Event.MAX_SECONDS_ALLOWED - 1);
  }

  /**
   * Tests querying at the boundary condition where the specified time window equals the max allowed
   * window size.
   *
   * @throws InterruptedException
   */
  @Test
  public void testGetRecentEventCountAtMaxWindow() throws InterruptedException {
    getRecentEventCount(Event.MAX_SECONDS_ALLOWED);
  }

  /**
   * Tests that the correct error is thrown if a user attempts to query beyond the allowed window
   * size.
   */
  @Test
  public void testGetRecentEventCountBeyondMaxWindow() {
    String expectedErrorMessage =
        String.format(
            "The max window size in secs is %d, you provided %d",
            Event.MAX_SECONDS_ALLOWED, Event.MAX_SECONDS_ALLOWED + 1);
    try {
      getRecentEventCount(Event.MAX_SECONDS_ALLOWED + 1);
      fail(
          String.format(
              "Expected getRecentEventCount(Event.MAX_SECONDS_ALLOWED + 1) to throw an AssertionError containing the message '%s'",
              expectedErrorMessage));
    } catch (AssertionError ex) {
      assertEquals(expectedErrorMessage, ex.getMessage());
    } catch (InterruptedException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * This tests that the event caching mechanism can handle simulatneous writes (signals) and reads
   * (get counts).
   *
   * @throws InterruptedException
   */
  @Test
  public void testSimultaneousSignallingAndCounts() throws InterruptedException {
    Event event = new Event(randomRealisticUnicodeOfLength(100));
    int signallingThreadWaitInMillis = 1000;
    int countingThreadWaitInMillis = 5000;

    // wait for the event cache to record its first count
    assertTrue(waitForEventCacheWindowBuild(event, 1L, 3000));

    Thread signallingThread = createSignallingThread(event, signallingThreadWaitInMillis);
    signallingThread.start();

    Thread.sleep(countingThreadWaitInMillis);

    // verify counts are increasing and accessible
    for (int i = 0; i < 3; i++) {
      long currentCount = event.getRecentEventCount(3);
      logger.debug("getting event count over last 3s: {}", currentCount);
      assertTrue(currentCount > 0);
      Thread.sleep(countingThreadWaitInMillis);
    }
    signallingThread.interrupt();
  }

  /** Simply create a thread for sending signals to the event */
  private Thread createSignallingThread(Event event, int signallingThreadWaitInMillis) {
    return new Thread(
        () -> {
          while (true) {
            event.signalEvent();
            try {
              Thread.sleep(signallingThreadWaitInMillis);
            } catch (InterruptedException e) {
              logger.info("signalling thread was interrupted");
            }
          }
        });
  }

  /**
   * Tests that stopping the event cache updater stops automatically updating the cache, and
   * restarting the updater simply appends to the existing cache.
   *
   * @throws InterruptedException
   */
  @Test
  public void testStartStopCacheUpdater() throws InterruptedException {
    Event event = new Event(randomRealisticUnicodeOfLength(100));
    assertTrue(event.isAutoUpdateCacheOn());

    // wait for the event cache to record its first count
    assertTrue(waitForEventCacheWindowBuild(event, 1L, 3000));

    event.stopCacheUpdater();
    long estimatedCacheSizeAfterStopping = event.getEstimatedCacheSize();

    assertFalse(event.isAutoUpdateCacheOn());
    assertTrue(waitForEventCacheCountToFlatline(event, 3, 5000));

    event.startCacheUpdater();
    Thread.sleep(3000);
    long estimatedCacheSizeAfterRestarting = event.getEstimatedCacheSize();

    assertTrue(event.isAutoUpdateCacheOn());
    assertTrue(estimatedCacheSizeAfterRestarting > estimatedCacheSizeAfterStopping);
  }
}
