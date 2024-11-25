package org.ncanfield.cribl.interview.logreader.config;

import org.junit.jupiter.api.Test;
import org.ncanfield.cribl.interview.logreader.endpoints.LogReader;
import org.springframework.validation.Errors;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FilePathValidatorTest {
    private static final String TEST_RESOURCE_PATH = new File("src/test/resources").getAbsolutePath();

    @Test
    public void supportsOnlyLogReaderConfig() {
        FilePathValidator validator = new FilePathValidator();
        assertTrue(validator.supports(LogReaderConfig.class));
        assertFalse(validator.supports(LogReader.class));
    }

    @Test
    public void validateRejectsMissingDir() {
        LogReaderConfig config = new LogReaderConfig(null, "", 100, List.of("self"));
        FilePathValidator validator = new FilePathValidator();
        Errors errors = validator.validateObject(config);
        assertEquals(1, errors.getAllErrors().size());
        assertEquals("Field error in object 'LogReaderConfig' on field 'logDir': rejected value [null]; codes " +
                        "[config.required]; arguments []; default message [null]",
                errors.getAllErrors().get(0).toString());
    }

    @Test
    public void validateRejectsRelativeDir() {
        LogReaderConfig config = new LogReaderConfig("./test", "", 100, List.of("self"));
        FilePathValidator validator = new FilePathValidator();
        Errors errors = validator.validateObject(config);
        assertEquals(1, errors.getAllErrors().size());
        assertEquals("Field error in object 'LogReaderConfig' on field 'logDir': rejected value [./test]; codes " +
                        "[file.absolutePathRequired]; arguments []; default message [The logDir must be an absolute path]",
                errors.getAllErrors().get(0).toString());
    }

    @Test
    public void validateRejectsNonExistantDir() {
        LogReaderConfig config = new LogReaderConfig(TEST_RESOURCE_PATH + "/nowaythisreallyexists", "", 100, List.of("self"));
        FilePathValidator validator = new FilePathValidator();
        Errors errors = validator.validateObject(config);
        assertEquals(1, errors.getAllErrors().size());
        //The actual path will vary depending on system, just check the right error message appears
        assertTrue(errors.getAllErrors().get(0).toString().contains("[The logDir must exist]"));
    }

    @Test
    public void validateRejectsNonDir() {
        LogReaderConfig config = new LogReaderConfig(TEST_RESOURCE_PATH + "/emptyFile.txt", "", 100, List.of("self"));
        FilePathValidator validator = new FilePathValidator();
        Errors errors = validator.validateObject(config);
        assertEquals(1, errors.getAllErrors().size());
        //The actual path will vary depending on system, just check the right error message appears
        assertTrue(errors.getAllErrors().get(0).toString().contains("[The logDir must be a directory path]"));
    }
}
