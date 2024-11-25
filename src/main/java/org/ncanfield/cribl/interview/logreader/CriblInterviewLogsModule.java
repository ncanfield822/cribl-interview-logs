package org.ncanfield.cribl.interview.logreader;

import org.ncanfield.cribl.interview.logreader.config.FilePathValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CriblInterviewLogsModule {
    @Bean
    public static FilePathValidator configurationPropertiesValidator() {
        return new FilePathValidator();
    }
}
