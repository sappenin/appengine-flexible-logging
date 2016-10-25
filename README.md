# appengine-flexible-logging
A Google Appengine Flexible Cloud Logging adapter for SLF4J.

## Overview
Google Appengine Flexible [(Non-Standard) environments](https://cloud.google.com/appengine/docs/flexible/java/dev-java-only) don't work well out-of-the-box with Google Stackdriver Logging (formerly Cloud Logging).  For appengine apps that run in a non-standard Java container, it is necessary to add some plumbing to your application in order for logs to display properly in the GCP logging console.

This code is an adaptation of code posted on StackOverflow [here](http://stackoverflow.com/questions/37420400/how-do-i-map-my-java-app-logging-events-to-corresponding-cloud-logging-event-lev).

## Installation
This code relies upon [logback](http://logback.qos.ch/) and [slf4j](http://www.slf4j.org/) in order to work properly.  

First, update your dependencies to include the following (this example assumes Maven):

```
...
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
...

...
  <dependency>
    <groupId>com.sappenin.logging</groupId>
    <artifactId>appengine-flexible-logging</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </dependency>
...

```
