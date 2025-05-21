package com.vik.herald;

import com.vik.herald.clients.RestClient;
import com.vik.herald.config.RestClientConfigProperties;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

@Disabled
@SpringBootTest(
    classes = {HeraldApplication.class, HeraldApplicationTests.TestConfig.class},
    webEnvironment = WebEnvironment.NONE,
    properties = {
        "spring.main.lazy-initialization=true",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false"
    }
)
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    KafkaAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.main.web-application-type=reactive",
    "herald.services.http-bin.activate-at-startup=true"
})
@ContextConfiguration(classes = HeraldApplicationTests.TestConfig.class)
class HeraldApplicationTests {

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public RestClientConfigProperties restClientConfigProperties() {
			return new RestClientConfigProperties();
		}
	}

	@MockBean
	private RestClient restClient;

	@Test
	void contextLoads() {
		// This test will verify that the Spring context loads successfully
	}

}
