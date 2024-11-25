package org.ncanfield.cribl.interview.logreader;

import org.ncanfield.cribl.interview.logreader.config.LogReaderConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LogReaderConfig.class)
public class CriblInterviewLogsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CriblInterviewLogsApplication.class, args);
	}

}
