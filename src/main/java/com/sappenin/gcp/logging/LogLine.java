package com.sappenin.gcp.logging;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class LogLine {
    private String time;
    private String severity;
    private String logMessage;
}
