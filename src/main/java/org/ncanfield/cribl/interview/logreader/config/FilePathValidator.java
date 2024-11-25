package org.ncanfield.cribl.interview.logreader.config;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.io.File;

public class FilePathValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return LogReaderConfig.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "logDir", "config.required");

        LogReaderConfig logReaderConfig = (LogReaderConfig) target;
        if (logReaderConfig.logDir() != null) {
            File logDir = new File(logReaderConfig.logDir());
            if (!logDir.isAbsolute()) {
                errors.rejectValue("logDir", "file.absolutePathRequired", "The logDir must be an absolute path");
            } else if (!logDir.exists()) {
                errors.rejectValue("logDir", "file.directoryRequired", "The logDir must exist");
            } else if (!logDir.isDirectory()) {
                errors.rejectValue("logDir", "file.directoryRequired", "The logDir must be a directory path");
            } else if (!logDir.canRead()) {
                errors.rejectValue("logDir", "file.cannotRead", "The logDir must be readable by this process");
            }
        }

    }
}
