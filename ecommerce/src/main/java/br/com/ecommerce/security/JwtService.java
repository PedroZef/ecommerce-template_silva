package br.com.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String DEV_FALLBACK_SECRET =
            "dGhpcy1pcy1hLXNlY3VyZS1rZXktZm9yLWp3dC1zaWduaW5nLXBsZWFzZS1jaGFuZ2UtaXQtaW4tcHJvZHVjdGlvbg==";

    private final String secretKey;
    private final long jwtExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration:86400000}") long jwtExpiration,
            Environment environment
    ) {
        // Fail-fast: fora do perfil dev, o fallback padrão da chave NÃO pode ser usado.
        // Se JWT_SECRET não for fornecida no ambiente, a aplicação nem deve subir.
        if (DEV_FALLBACK_SECRET.equals(secretKey) && !environment.matchesProfiles("dev")) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurada no ambiente! Defina a variável de ambiente JWT_SECRET "
                            + "(Base64 com no mínimo 256 bits) antes de iniciar a aplicação fora do perfil 'dev'.");
        }
        this.secretKey = secretKey;
        this.jwtExpiration = jwtExpiration;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        extraClaims.put("roles", userDetails.getAuthorities().stream()
                .map(Object::toString)
                .toList());
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    @SuppressWarnings("unchecked")
    public List<SimpleGrantedAuthority> extractRoles(String token) {
        List<String> roles = extractClaim(token, claims -> claims.get("roles", List.class));
        if (roles == null) return List.of();
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
