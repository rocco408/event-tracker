# Event Tracker

## Overview
The Event Tracker is a small library which helps to track the number of events that happened during a specified window of time. 

This library supports two operations:

- A client should be able to signal that a single event happened (e.g. a webpage was served, a sensor recorded data, etc.)

- A client should be able to request the number of events that happened over a user-specified amount of time until current time (limited to a 5 minute lookback window)

The Event Tracker only keeps track of event types and counts, no additional information is provided.

## Adding Event Tracker to your build

### Building

This project requires Java 11+ and Gradle 6.7.1+.

Building the project will compile, test, and build jars.

`./gradlew build`

If you see the following error, make sure you're using Java 11+, in IntelliJ navigate to Preferences -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JVM and make sure Java 11 or higher is selected.
```shell script
Execution failed for task ':event-tracker:verifyGoogleJavaFormat'.
> com/google/googlejavaformat/java/JavaFormatterOptions$Style has been compiled by a more recent version of the Java Runtime (class file version 55.0), this version of the Java Runtime only recognizes class file versions up to 52.0
```

If you see code formatting conflicts run the `googleJavaFormat` task to properly format all `*.java` files in the project.
See the Code Formatting section below for more details.
```shell script
$ ./gradlew goJF
```

The jars will be found in `event-tracker/build/libs/`.

Simply place `event-tracker/build/libs/event-tracker-0.1.0-all.jar` in your client's classpath.


### Testing

Tests can be executed through the IDE (IntelliJ IDEA was used in development) or from the command line.

Here is how to run all tests
```
./gradlew test

```

Here is how to run all `EventTracker` tests
```
./gradlew :event-tracker:test --tests "com.mycompany.event.tracker.EventTrackerTest*"
```

Here is how to a single tests
```
./gradlew :event-tracker:test --tests "com.mycompany.event.tracker.EventTrackerTest.testSignalEvent*"
```
                                       
The test results can be found in `event-tracker/build/reports/tests/test/index.html`.


## Quick Start

The API for tracking events is simple and accessed through an `EventTracker` object. 

### Signal that an event has taken place

Start by initializing an `EventTracker` object.

```java
EventTracker tracker = new EventTracker();
```

To signal that an event has taken place simply call the `signalEvent` method and provide the name of the event. The
EventTracker will update a counter for that event while keeping track of the timestamp of the incoming signal. The
event may be new or an existing event.

```java
tracker.signalEvent("https://www2.purpleair.com/");
```

### Request the number of events within a time window

A client may request the number of events within a window of time preceding the current time. The client may specify the
time window in seconds. The `getRecentEventCount` method takes an event ID and time window size (prior to and including 
the current time).

Here is an example requesting event counts over the last 20 seconds.
```java
long numEvents = tracker.getRecentEventCount("https://www2.purpleair.com/", 20);
```

## Contributing

### Code Formatting

We use the [Google Java Style](https://github.com/google/google-java-format). This link has instructions for adding the plugin to your IDE.

We use the [Google Java format gradle plugin](https://github.com/sherter/google-java-format-gradle-plugin) for auto-formatting and verifying.

To format manually, run the `googleJavaFormat` task to properly format all `*.java` files in the project
```shell script
$ ./gradlew goJF
```

To verify proper code format run the `verifyGoogleJavaFormat` task
```shell script
$ ./gradlew verGJF
```

### Requirements

Java 11+

Gradle 6.7.1+


## Contributors

- Rocco Varela
