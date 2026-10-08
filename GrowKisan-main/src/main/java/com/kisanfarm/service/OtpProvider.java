package com.kisanfarm.service;

/**
 * Abstraction over an OTP delivery/verification provider.
 * Implementations: MockOtpProvider (dev), TwilioOtpProvider (production).
 * Provider secrets live only on the backend, never exposed to the frontend.
 */
public interface OtpProvider {

    String name();

    /**
     * Start a verification for the given mobile.
     * @return provider reference (e.g. Twilio SID) or a locally-generated code
     *         carrier for the mock provider.
     */
    SendResult send(String mobile);

    /** Check whether the supplied code is valid for the mobile. */
    boolean verify(String mobile, String code, SendResult sendContext);

    /** Carries provider-specific data between send and verify. */
    record SendResult(String providerRef, String codeHashOrNull, String devCodeOrNull) {}
}
