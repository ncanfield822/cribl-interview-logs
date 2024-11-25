package org.ncanfield.cribl.interview.logreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix="log-reader")
public record LogReaderConfig (String logDir, String friendlyName, Integer defaultLineLimit) {
}