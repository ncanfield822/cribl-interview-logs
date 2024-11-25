package org.ncanfield.cribl.interview.logreader.models;

import java.util.List;

public record LogAggregateResponse(List<LogReadResponse> serverLogs, List<String> errors) {
}
