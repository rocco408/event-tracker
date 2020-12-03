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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * The Event class encapsulates an event type, largely defined the {@link Event#eventId}. This class
 * uses the Caffeine caching library for maintaining a time-windowed in-memory cache used for
 * tracking event signals over a 5-minute window. When new {@link Event} object is created an atomic
 * counter is created that maintains the most recent count of signals received. A {@link Cache} is
 * created, entries older than 5-minutes are evicted automatically. Lastly, a background cache
 * update thread is initiated that atomically updates the cache every second with the latest signal
 * counter.
 */
class Event {
  static final Logger logger = LoggerFactory.getLogger(Event.class);
  public static final int MAX_SECONDS_ALLOWED = 300;
  private String eventId;
  private boolean autoUpdateCache;
  private AtomicLong counter;
  private ExecutorService executor;
  private Cache<Integer, Long> cache; // key: current time in secs, value: event count

  Event(String eventId) {
    this.eventId = eventId;
    counter = new AtomicLong();
    cache = Caffeine.newBuilder().expireAfterWrite(MAX_SECONDS_ALLOWED, TimeUnit.SECONDS).build();
    startCacheUpdater();
    logger.info("Created new event:\n" + toString());
  }

  @VisibleForTesting
  void setCounter(long value) {
    counter.set(value);
  }

  /** Increment the signal counter and return the updated value. */
  long incrementCounter() {
    return counter.incrementAndGet();
  }

  long getCount() {
    return counter.get();
  }

  @VisibleForTesting
  long getEstimatedCacheSize() {
    return cache.estimatedSize();
  }

  /** Helper method for {@link Event#signalEvent()} and to enable flexible testing. */
  long signalEvent(Integer timestampInSecs) {
    long counter = incrementCounter();
    cache.put(timestampInSecs, counter);
    logger.debug(
        "Recorded new signal with timestamp '{}' and count '{}' ", timestampInSecs, counter);
    return counter;
  }

  /**
   * Atomically increments the signal counter for this event.
   *
   * @return The new signal counter value.
   */
  public long signalEvent() {
    return signalEvent(Math.toIntExact(System.currentTimeMillis() / 1000));
  }

  /** Helper method for {@link Event#getRecentEventCount(int)} and to enable flexible testing. */
  Long getRecentEventCount(int currentTimeInSecs, int timeWindowInSecs) {
    assert (timeWindowInSecs <= MAX_SECONDS_ALLOWED)
        : String.format(
            "The max window size in secs is %d, you provided %d",
            MAX_SECONDS_ALLOWED, timeWindowInSecs);
    assert (timeWindowInSecs > 0)
        : String.format(
            "The time window size must be greater than 0, you provided %d", timeWindowInSecs);
    assert (currentTimeInSecs > 0)
        : String.format(
            "The current timestamp in secs must be greater than 0, the current time is %d",
            currentTimeInSecs);

    int startTime = currentTimeInSecs - timeWindowInSecs;
    Long startCount = cache.get(startTime, k -> null);
    if (startCount == null)
      throw new IllegalStateException(
          String.format(
              "Event count for timestamp '%d' not found. "
                  + "Current timestamp is '%d', supplied lookback "
                  + "window in secs is '%d', event state is: %s",
              startCount, currentTimeInSecs, timeWindowInSecs, this));

    Long currentCount = cache.get(currentTimeInSecs, k -> incrementCounter());
    assert currentCount != null
        : String.format(
            "The count for the current time '%d' is null, this should not happen",
            currentTimeInSecs);
    return currentCount - startCount;
  }

  /**
   * Retrieves the number of signals received for this {@link Event} over the specified lookback
   * window.
   *
   * @param timeWindowInSecs The number of seconds from the current time that defines the lookback
   *     window.
   * @return The number signals that occurred during this time frame.
   */
  public Long getRecentEventCount(int timeWindowInSecs) {
    return getRecentEventCount(
        Math.toIntExact(System.currentTimeMillis() / 1000), timeWindowInSecs);
  }

  /**
   * Starts a background thread that atomically updates the {@link Event#cache} every second with
   * the latest value of {@link Event#counter}.
   */
  void startCacheUpdater() {
    logger.info("Starting the cache updater");
    autoUpdateCache = true;
    Runnable r =
        () -> {
          while (autoUpdateCache) {
            try {
              // updating every half-second decreases the likelihood of not recording the count for
              // a given second calculated from System.currentTimeMillis(), this rarely happens
              // during GC pauses
              Thread.sleep(500);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            int currentTimeInSecs = Math.toIntExact(System.currentTimeMillis() / 1000);
            // if currentTimeInSecs is cached, do nothing; otherwise create and cache with
            // getCount() as the value, and return
            cache.get(currentTimeInSecs, k -> getCount());
          }
        };
    executor =
        Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Event-Cache-Updater-%d")
                .build());
    executor.submit(r);
  }

  @VisibleForTesting
  void stopCacheUpdater() {
    logger.info("Stopping the cache updater");
    autoUpdateCache = false;
    executor.shutdown();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("eventId: %s\n", eventId));
    sb.append(String.format("counter: %s\n", counter));
    sb.append(String.format("cache:\n"));
    cache.asMap().entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .forEachOrdered(
            e -> sb.append(String.format("key: %d, value: %d\n", e.getKey(), e.getValue())));
    return sb.toString();
  }

  @VisibleForTesting
  public boolean isAutoUpdateCacheOn() {
    return autoUpdateCache;
  }
}
