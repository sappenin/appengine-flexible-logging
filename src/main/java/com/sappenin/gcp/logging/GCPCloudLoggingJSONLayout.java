package com.sappenin.gcp.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

import static ch.qos.logback.classic.ClassicConstants.REQUEST_USER_AGENT_MDC_KEY;
import static ch.qos.logback.classic.Level.*;
import static com.sappenin.gcp.logging.Constants.*;
import static com.sappenin.gcp.logging.MDCInsertingServletFilter.REQUEST_HEADER_PREFIX;

/**
 * Format a LoggingEvent as a single line JSON object.
 * <p>
 * <br>https://cloud.google.com/appengine/docs/flexible/java/writing-application-logs
 * <p>
 * <br>From https://cloud.google.com/appengine/articles/logging <quote> Applications using the flexible environment
 * should write custom log files to the VM's log directory at /var/log/app_engine/custom_logs. These files are
 * automatically collected and made available in the Logs Viewer. Custom log files must have the suffix .log or
 * .log.json. If the suffix is .log.json, the logs must be in JSON format with one JSON object per line. If the suffix
 * is .log, log entries are treated as plain text. </quote>
 * <p>
 * This code was patterned off of the following StackOverflow <a href="http://stackoverflow.com/questions/37420400/how-do-i-map-my-java-app-logging-events-to-corresponding-cloud-logging-event-lev">question</a>.
 */
public class GCPCloudLoggingJSONLayout extends PatternLayout {

    @Override
    public String doLayout(ILoggingEvent event) {
        String formattedMessage = super.doLayout(event);
        return doLayout_internal(formattedMessage, event);
    }

    /* for testing without having to deal wth the complexity of super.doLayout()
     * Uses formattedMessage instead of event.getMessage() */
    String doLayout_internal(final String formattedMessage, final ILoggingEvent event) {
        final GCPCloudLoggingEvent gcpLogEvent = new GCPCloudLoggingEvent(
                // message
                formattedMessage,
                // timstamps
                convertTimestampToGCPLogTimestamp(event.getTimeStamp()),
                // severity
                mapLevelToGCPLevel(event.getLevel()),
                // Appengine Request Id
                event.getMDCPropertyMap().get(REQUEST_ID_IDENTIFIER)
        );

        // The main object that will be logged...
        JSONObject jsonLogObj = new JSONObject(gcpLogEvent);

        // Add all MDC values that start with REQUEST_HEADER_PREFIX into the RequestHeaders object...
        final JSONObject jsonRequestHeaders = new JSONObject();
        Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();
        if (mdcContextMap == null) {
            MDC.put(REQUEST_ID_IDENTIFIER, UUID.randomUUID().toString().replace("-", ""));
            mdcContextMap = MDC.getCopyOfContextMap();
        }

        for (final String mdcKey : mdcContextMap.keySet()) {
            if (mdcKey != null && mdcKey.startsWith(REQUEST_HEADER_PREFIX)) {
                jsonRequestHeaders.put(mdcKey.replace(REQUEST_HEADER_PREFIX, ""), mdcContextMap.get(mdcKey));
            }
        }
        jsonLogObj.put("requestHeaders", jsonRequestHeaders);

        // Special fields for RequestLog (https://cloud.google.com/logging/docs/api/ref/rest/v1beta3/RequestLog)
        //jsonLogObj.put("@type", "type.googleapis.com/google.appengine.logging.v1.RequestLog");
        //jsonLogObj.put("resource", MDC.get(ClassicConstants.REQUEST_REQUEST_URI));
        //jsonLogObj.put(METHOD, MDC.get(ClassicConstants.REQUEST_METHOD));
        //jsonLogObj.put(HTTP_VERSION, MDC.get(HTTP_VERSION));
        //jsonLogObj.put(RESOURCE, MDC.get(RESOURCE));

        jsonLogObj.put(USER_AGENT, MDC.get(REQUEST_USER_AGENT_MDC_KEY));
//        jsonLogObj.put("startTime", convertTimestampToGCPLogTimestamp(DateTime.now(DateTimeZone.UTC).getMillis()));
//        jsonLogObj.put("endTime", convertTimestampToGCPLogTimestamp(DateTime.now(DateTimeZone.UTC).getMillis()));

        // Response Info...
        //jsonLogObj.put(STATUS, MDC.get(STATUS));

        //jsonLogObj.put("startTime", MDC.get(START_TIME));
        //jsonLogObj.put("endTime", MDC.get(END_TIME));

//        List<LogLine> lines = new LinkedList<>();
//        final LogLineBuilder logLineBuilder = LogLine.builder();
//        logLineBuilder.severity("ERROR");
//        logLineBuilder.logMessage("This was not supposed to happen!");
//        logLineBuilder.time(DateTime.now(DateTimeZone.UTC).toString());
//        lines.add(logLineBuilder.build());
//        jsonLogObj.put("line", lines);

        /* Add a newline so that each JSON log entry is on its own line.
         * Note that it is also important that the JSON log entry does not span multiple lines.
         */
        return jsonLogObj.toString() + "\n";
    }

    @Override
    public Map<String, String> getDefaultConverterMap() {
        return PatternLayout.defaultConverterMap;
    }

    private String convertTimestampToGCPLogTimestamp(long millisSinceEpoch) {
        return new DateTime(millisSinceEpoch).toString("YYYY-MM-DD'T'HH:mm:ss.SSSSSS'Z'");
    }

    private String mapLevelToGCPLevel(Level level) {
        switch (level.toInt()) {
            case TRACE_INT:
                return "REQUEST_ID_IDENTIFIER";
            case DEBUG_INT:
                return "DEBUG";
            case INFO_INT:
                return "INFO";
            case WARN_INT:
                return "WARN";
            case ERROR_INT:
                return "ERROR";
            default:
                return "ERROR"; // Map to ERROR so that we can rectify...
        }
    }

    /**
     * A class that models a single line of output that can be consumed by the GCP agent piping log data to StackDriver.
     * Must be public for JSON marshalling logic
     */
    public static class GCPCloudLoggingEvent {
        private String message;
        // See https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#timestamp
        private String timestamp;
        private String requestId;
        private String severity;

        /**
         * Required-args Constructor.
         *
         * @param message
         * @param timestamp
         * @param severity
         * @param requestId
         */
        public GCPCloudLoggingEvent(String message, String timestamp, String severity, String requestId) {
            this.message = message;
            this.timestamp = timestamp;
            this.severity = severity;
            this.requestId = requestId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String rfc3339Timestamp) {
            this.timestamp = rfc3339Timestamp;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }


}
