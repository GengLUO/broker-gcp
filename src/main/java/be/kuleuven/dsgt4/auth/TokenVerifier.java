package be.kuleuven.dsgt4.auth;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwk.Jwk;
import java.security.interfaces.RSAPublicKey;

public class TokenVerifier {

    private final JwkProvider jwkProvider;

    public TokenVerifier(JwkProvider jwkProvider) {
        this.jwkProvider = jwkProvider;
    }

    public DecodedJWT verifyToken(String token) throws Exception {
        DecodedJWT jwt = JWT.decode(token);
        Jwk jwk = jwkProvider.get(jwt.getKeyId());

        Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("https://securetoken.google.com/demo-distributed-systems-kul")
                .build();

        return verifier.verify(token);
    }
}