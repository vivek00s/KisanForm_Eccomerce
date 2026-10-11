package com.kisanfarm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link TwoFactorOtpProvider} using mocked 2Factor HTTP
 * responses. No real SMS is sent and no network call leaves the test JVM.
 */
class TwoFactorOtpProviderTest {

    private static final String API_KEY = "test-key";
    private static final String BASE_URL = "https://2factor.in/API/V1";
    private static final String SESSION_ID = "abc-123-session";

    private TwoFactorOtpProvider provider;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        // Bind the mock to the builder BEFORE the provider builds its client, so
        // the provider's RestClient routes every call through the mock server.
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new TwoFactorOtpProvider(API_KEY, BASE_URL, "+91", builder);
    }

    @Test
    void send_parsesSessionIdFromDetails() {
        server.expect(requestTo(BASE_URL + "/" + API_KEY + "/SMS/919999988888/AUTOGEN"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"Status\":\"Success\",\"Details\":\"" + SESSION_ID + "\"}",
                        MediaType.APPLICATION_JSON));

        OtpProvider.SendResult result = provider.send("9999988888");

        assertThat(result.providerRef()).isEqualTo(SESSION_ID);  // only the session id is kept
        server.verify();
    }

    @Test
    void send_tenDigitMobileIsPrefixedWithCountryCode() {
        server.expect(requestTo(BASE_URL + "/" + API_KEY + "/SMS/919876543210/AUTOGEN"))
                .andRespond(withSuccess(
                        "{\"Status\":\"Success\",\"Details\":\"s1\"}", MediaType.APPLICATION_JSON));

        OtpProvider.SendResult result = provider.send("98765 43210");

        assertThat(result.providerRef()).isEqualTo("s1");
        server.verify();
    }

    @Test
    void send_apiReturnsErrorStatus_throwsDeliveryException() {
        server.expect(requestTo(BASE_URL + "/" + API_KEY + "/SMS/919999988888/AUTOGEN"))
                .andRespond(withSuccess(
                        "{\"Status\":\"Error\",\"Details\":\"Invalid API Key\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.send("9999988888"))
                .isInstanceOf(TwoFactorOtpProvider.OtpDeliveryException.class);
        server.verify();
    }

    @Test
    void send_httpError_throwsDeliveryException() {
        server.expect(requestTo(BASE_URL + "/" + API_KEY + "/SMS/919999988888/AUTOGEN"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.send("9999988888"))
                .isInstanceOf(TwoFactorOtpProvider.OtpDeliveryException.class);
        server.verify();
    }

    @Test
    void verify_correctCode_returnsTrue() {
        server.expect(requestTo(BASE_URL + "/" + API_KEY + "/SMS/VERIFY/" + SESSION_ID + "/123456"))
                .andRespond(withSuccess(
                        "{\"Status\":\"Success\",\"Details\":\"OTP Matched\"}",
                        MediaType.APPLICATION_JSON));

        OtpProvider.SendResult ctx = new OtpProvider.SendResult(SESSION_ID);
        boolean ok = provider.verify("9999988888", "123456", ctx);

        assertThat(ok).isTrue();
        server.verify();
    }

    @Test
    void verify_wrongCode_returnsFalse() {
        server.expect(requestTo(BASE_URL + "/" + API_KEY + "/SMS/VERIFY/" + SESSION_ID + "/000000"))
                .andRespond(withSuccess(
                        "{\"Status\":\"Error\",\"Details\":\"OTP Mismatch\"}",
                        MediaType.APPLICATION_JSON));

        OtpProvider.SendResult ctx = new OtpProvider.SendResult(SESSION_ID);
        boolean ok = provider.verify("9999988888", "000000", ctx);

        assertThat(ok).isFalse();
        server.verify();
    }

    @Test
    void verify_transportError_failsClosed() {
        server.expect(requestTo(BASE_URL + "/" + API_KEY + "/SMS/VERIFY/" + SESSION_ID + "/123456"))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));

        OtpProvider.SendResult ctx = new OtpProvider.SendResult(SESSION_ID);
        boolean ok = provider.verify("9999988888", "123456", ctx);

        assertThat(ok).isFalse();
        server.verify();
    }

    @Test
    void verify_withoutSessionId_returnsFalseWithoutCallingApi() {
        // No session id -> cannot verify; must not reach the provider at all.
        OtpProvider.SendResult ctx = new OtpProvider.SendResult(null);
        boolean ok = provider.verify("9999988888", "123456", ctx);

        assertThat(ok).isFalse();
        server.verify(); // no requests expected
    }

    @Test
    void verify_blankCode_returnsFalse() {
        OtpProvider.SendResult ctx = new OtpProvider.SendResult(SESSION_ID);
        assertThat(provider.verify("9999988888", "  ", ctx)).isFalse();
        server.verify();
    }
}
