package org.ncanfield.cribl.interview.logreader.models;

import java.util.List;

public record LogReadResponse(String serverName, List<LogFile> logFiles, List<String> errors) {
}
