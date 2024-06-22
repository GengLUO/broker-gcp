package be.kuleuven.dsgt4.auth;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;
import java.util.concurrent.TimeUnit;

//Reference: https://github.com/auth0/jwks-rsa-java?tab=readme-ov-file
@Configuration
public class JwkConfiguration {

    private static final String JWKS_URI = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    @Bean
    public JwkProvider jwkProvider() {
        try {
            //The provider can be configured to cache JWKs to avoid unnecessary network requests, as well as only fetch the JWKs within a defined rate limit:
            return new JwkProviderBuilder(new URL(JWKS_URI))
                    .cached(10, 24, TimeUnit.HOURS) // Cache up to 10 keys, refresh every 24 hours
                    .rateLimited(10, 1, TimeUnit.MINUTES) // Allow 10 requests per minute
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build JwkProvider", e);
        }
    }
}
