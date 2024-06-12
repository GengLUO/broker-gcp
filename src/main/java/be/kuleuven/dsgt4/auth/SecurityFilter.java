package be.kuleuven.dsgt4.auth;

import be.kuleuven.dsgt4.broker.domain.User;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7); // Remove "Bearer " prefix
            System.out.println("Authorization Header: " + authorizationHeader);
            System.out.println("Extracted Token: " + token);
            try {
                JwkProvider provider = new JwkConfiguration().jwkProvider();
                DecodedJWT jwt = JWT.decode(token);
                Jwk jwk = provider.get(jwt.getKeyId());

                Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
                JWTVerifier verifier = JWT.require(algorithm)
                        .withIssuer("https://securetoken.google.com/broker-da44b")
                        .build(); // Reusable verifier instance
                DecodedJWT verifiedJwt = verifier.verify(token);
                System.out.println("JWT Verified. Subject: " + verifiedJwt.getSubject());

                String email = jwt.getClaim("email").asString();
//          TODO: handle users with multiple roles
                List<String> roles = jwt.getClaim("roles").asList(String.class);
                String role = roles != null && !roles.isEmpty() ? roles.get(0) : "user" ;
                User user = new User(email, role);
                SecurityContext context = SecurityContextHolder.getContext();
                context.setAuthentication(new FirebaseAuthentication(user));
            } catch (Exception e) {
                System.err.println("Failed to verify token: " + e.getMessage());
                e.printStackTrace();
            }
// (level 1) decode Identity Token and assign correct email and role
//            DecodedJWT jwt = JWT.decode(token); // Decode without verification
//
//            String email = jwt.getClaim("email").asString();
////          TODO: handle users with multiple roles
//            List<String> roles = jwt.getClaim("roles").asList(String.class);
//            String role = roles != null && !roles.isEmpty() ? roles.get(0) : "user" ;
//            User user = new User(email, role);
//            SecurityContext context = SecurityContextHolder.getContext();
//            context.setAuthentication(new FirebaseAuthentication(user));

//(level 2) verify Identity Token
//            try {
//                FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(token);
//                String uid = firebaseToken.getUid();
//                String email = firebaseToken.getEmail();
//
//                // Extract roles from the custom claims
//                List<String> roles = (List<String>) firebaseToken.getClaims().get("roles");
//                String role = roles != null && !roles.isEmpty() ? roles.get(0) : "user";
//
//                User user = new User(email, role);
//                SecurityContext context = SecurityContextHolder.getContext();
//                context.setAuthentication(new FirebaseAuthentication(user));
//            } catch (FirebaseAuthException e) {
//                throw new RuntimeException(e);
//            }

        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith("/api");
    }

    private static class FirebaseAuthentication implements Authentication {
        private final User user;

        FirebaseAuthentication(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            if (user.isManager()) {
                return List.of(new SimpleGrantedAuthority("manager"));
            } else {
                return new ArrayList<>();
            }
        }
//        TODO:Extension
//        public Collection<? extends GrantedAuthority> getAuthorities() {
//            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
//            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
//            return authorities;
//        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public User getPrincipal() {
            return this.user;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean b) throws IllegalArgumentException {

        }

        @Override
        public String getName() {
            return null;
        }
    }
}

