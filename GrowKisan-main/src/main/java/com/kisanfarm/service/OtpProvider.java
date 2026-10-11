package com.kisanfarm.service;

/**
 * Abstraction over an OTP delivery/verification provider.
 * Implementation: {@link TwoFactorOtpProvider} (2Factor.in).
 * Provider secrets live only on the backend, never exposed to the frontend.
 */
public interface OtpProvider {

    String name();

    /**
     * Start a verification for the given mobile. The provider owns and sends the
     * OTP; the backend keeps only the returned reference.
     *
     * @return provider reference (e.g. the 2Factor session id) used later to verify.
     */
    SendResult send(String mobile);

    /** Check whether the supplied code is valid for the mobile. */
    boolean verify(String mobile, String code, SendResult sendContext);

    /**
     * Carries the provider reference (session id) between send and verify.
     * The raw OTP is never stored or returned to the caller.
     */
    record SendResult(String providerRef) {}
}
