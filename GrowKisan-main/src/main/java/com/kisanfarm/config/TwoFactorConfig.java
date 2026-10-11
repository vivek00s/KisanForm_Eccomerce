package com.kisanfarm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Wiring for the 2Factor.in OTP provider. Only active when
 * {@code kisanfarm.otp.provider=twofactor}. Supplies a dedicated
 * {@link RestClient.Builder} pre-configured with connect/read timeouts so a slow
 * or unresponsive provider can never hang request threads.
 */
@Configuration
@ConditionalOnProperty(name = "kisanfarm.otp.provider", havingValue = "twofactor")
public class TwoFactorConfig {

    @Bean("twoFactorRestClientBuilder")
    RestClient.Builder twoFactorRestClientBuilder(
            @Value("${kisanfarm.twofactor.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${kisanfarm.twofactor.read-timeout-ms:10000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder().requestFactory(factory);
    }
}
