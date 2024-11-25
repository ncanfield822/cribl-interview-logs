package org.ncanfield.cribl.interview.logreader.models;

import java.util.List;

public record LogFile(String fileName, String filePath, List<String> logLines, String error) {
}
