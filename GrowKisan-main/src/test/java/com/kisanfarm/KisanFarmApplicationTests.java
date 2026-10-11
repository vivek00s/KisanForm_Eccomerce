package com.kisanfarm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test that the Spring context loads. Supplies a dummy 2Factor API key so
 * {@link com.kisanfarm.service.TwoFactorOtpProvider} passes its startup check
 * without a real secret; no real OTP request is ever made here. The main
 * datasource config (MySQL) is used as-is, so this test requires a reachable DB.
 */
@SpringBootTest(properties = {
        "kisanfarm.twofactor.api-key=test-dummy-key"
})
class KisanFarmApplicationTests {

    @Test
    void contextLoads() {
    }
}
