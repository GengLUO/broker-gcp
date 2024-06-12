//package be.kuleuven.dsgt4.auth;
//
//import com.auth0.jwk.JwkProvider;
//import com.auth0.jwt.JWT;
//import com.auth0.jwt.exceptions.JWTVerificationException;
//import com.auth0.jwt.interfaces.DecodedJWT;
//import com.auth0.jwt.interfaces.JWTVerifier;
//import com.auth0.jwt.algorithms.Algorithm;
//import com.auth0.jwk.Jwk;
//import com.google.gson.Gson;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.security.KeyFactory;
//import java.security.interfaces.RSAPublicKey;
//import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;
//import java.util.Map;
//
//public class TokenVerifier {
//
//    private static final String GOOGLE_PUBLIC_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";
//
//    private static Map<String, String> getGooglePublicKeys() throws Exception {
//        HttpClient client = HttpClient.newHttpClient();
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(new URI(GOOGLE_PUBLIC_KEYS_URL))
//                .build();
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//        return new Gson().fromJson(response.body(), Map.class);
//    }
//
//    private static RSAPublicKey extractPublicKey(String publicKeyStr) throws Exception {
//        String publicKeyPEM = publicKeyStr.replace("-----BEGIN PUBLIC KEY-----", "")
//                .replaceAll(System.lineSeparator(), "")
//                .replace("-----END PUBLIC KEY-----", "");
//
//        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
//        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
//    }
//
//    private static DecodedJWT verifyToken(String token, RSAPublicKey publicKey) throws Exception {
//        try {
//            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
//            JWTVerifier verifier = JWT.require(algorithm)
//                    .withIssuer("https://securetoken.google.com/broker-da44b")
//                    .build(); // Reusable verifier instance
//            return verifier.verify(token);
//        } catch (JWTVerificationException exception) {
//            throw new Exception("Token verification failed.", exception);
//        }
//    }
//
////    public DecodedJWT verifyToken(String token) throws Exception {
////        DecodedJWT jwt = JWT.decode(token);
////        Jwk jwk = jwkProvider.get(jwt.getKeyId());
////
////        Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
////        JWTVerifier verifier = JWT.require(algorithm)
////                .withIssuer("https://securetoken.google.com/demo-distributed-systems-kul")
////                .build();
////
////        return verifier.verify(token);
////    }
//}