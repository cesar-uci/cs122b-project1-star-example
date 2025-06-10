package uci122b.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {
    // IMPORTANT: In a real application, this secret key should be loaded from a secure configuration file, not hardcoded.
    private static final String SECRET_KEY_STRING = "your-super-secret-key-that-is-long-enough-for-hs256";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_TIME_MS = 86400000; // 24 hours in milliseconds

    /**
     * Generates a JWT for a given user email.
     *
     * @param email The email of the user to include in the token.
     * @return A signed JWT string.
     */
    public static String generateToken(String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME_MS);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Validates a JWT and extracts the user email (subject).
     *
     * @param token The JWT string to validate.
     * @return The user email if the token is valid, otherwise null.
     */
    public static String validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);

            // Check if the token is expired
            if (claimsJws.getBody().getExpiration().before(new Date())) {
                System.err.println("JWT has expired for subject: " + claimsJws.getBody().getSubject());
                return null;
            }

            return claimsJws.getBody().getSubject();
        } catch (Exception e) {
            System.err.println("JWT validation failed: " + e.getMessage());
            return null; // Token is invalid (e.g., signature mismatch, malformed)
        }
    }
}
