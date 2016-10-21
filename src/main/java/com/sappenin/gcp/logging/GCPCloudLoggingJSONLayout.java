package com.sappenin.gcp.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sappenin.gcp.logging.GCPCloudLoggingJSONLayout.GCPCloudLoggingEvent.GCPCloudLoggingTimestamp;
import org.json.JSONObject;

import java.util.Map;

import static ch.qos.logback.classic.Level.*;

/**
 * Format a LoggingEvent as a single line JSON object
 * <p>
 * <br>https://cloud.google.com/appengine/docs/flexible/java/writing-application-logs
 * <p>
 * <br>From https://cloud.google.com/appengine/articles/logging
 * <quote>
 * Applications using the flexible environment should write custom log files to the VM's log directory at
 * /var/log/app_engine/custom_logs. These files are automatically collected and made available in the Logs Viewer.
 * Custom log files must have the suffix .log or .log.json. If the suffix is .log.json, the logs must be in JSON
 * format with one JSON object per line. If the suffix is .log, log entries are treated as plain text.
 * </quote>
 * <p>
 * Nathan: I can't find a reference to this format on the google pages but I do remember getting the format from some
 * GO code that a googler on the community slack channel referred me to.
 *
 * @see "http://stackoverflow.com/questions/37420400/how-do-i-map-my-java-app-logging-events-to-corresponding-cloud-logging-event-lev"
 */
public class GCPCloudLoggingJSONLayout extends PatternLayout {

    @Override
    public String doLayout(ILoggingEvent event) {
        String formattedMessage = super.doLayout(event);
        return doLayout_internal(formattedMessage, event);
    }

    /* for testing without having to deal wth the complexity of super.doLayout()
     * Uses formattedMessage instead of event.getMessage() */
    String doLayout_internal(String formattedMessage, ILoggingEvent event) {
        GCPCloudLoggingEvent gcpLogEvent = new GCPCloudLoggingEvent(formattedMessage
                , convertTimestampToGCPLogTimestamp(event.getTimeStamp())
                , mapLevelToGCPLevel(event.getLevel())
                , null);
        JSONObject jsonObj = new JSONObject(gcpLogEvent);
        /* Add a newline so that each JSON log entry is on its own line.
         * Note that it is also important that the JSON log entry does not span multiple lines.
         */
        return jsonObj.toString() + "\n";
    }

    static GCPCloudLoggingTimestamp convertTimestampToGCPLogTimestamp(long millisSinceEpoch) {
        int nanos = ((int) (millisSinceEpoch % 1000)) * 1_000_000; // strip out just the milliseconds and convert to nanoseconds
        long seconds = millisSinceEpoch / 1000L; // remove the milliseconds
        return new GCPCloudLoggingTimestamp(seconds, nanos);
    }

    static String mapLevelToGCPLevel(Level level) {
        switch (level.toInt()) {
            case TRACE_INT:
                return "TRACE";
            case DEBUG_INT:
                return "DEBUG";
            case INFO_INT:
                return "INFO";
            case WARN_INT:
                return "WARN";
            case ERROR_INT:
                return "ERROR";
            default:
                return null; /* This should map to no level in GCP Cloud Logging */
        }
    }

    /* Must be public for JSON marshalling logic */
    public static class GCPCloudLoggingEvent {
        private String message;
        private GCPCloudLoggingTimestamp timestamp;
        private String traceId;
        private String severity;

        public GCPCloudLoggingEvent(
                String message, GCPCloudLoggingTimestamp timestamp, String severity,
                String traceId
        ) {
            super();
            this.message = message;
            this.timestamp = timestamp;
            this.traceId = traceId;
            this.severity = severity;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public GCPCloudLoggingTimestamp getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(GCPCloudLoggingTimestamp timestamp) {
            this.timestamp = timestamp;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        /* Must be public for JSON marshalling logic */
        public static class GCPCloudLoggingTimestamp {
            private long seconds;
            private int nanos;

            public GCPCloudLoggingTimestamp(long seconds, int nanos) {
                super();
                this.seconds = seconds;
                this.nanos = nanos;
            }

            public long getSeconds() {
                return seconds;
            }

            public void setSeconds(long seconds) {
                this.seconds = seconds;
            }

            public int getNanos() {
                return nanos;
            }

            public void setNanos(int nanos) {
                this.nanos = nanos;
            }

        }
    }

    @Override
    public Map<String, String> getDefaultConverterMap() {
        return PatternLayout.defaultConverterMap;
    }

}
