package za.co.finsight.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import za.co.finsight.config.FinsightProperties;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    static final String HEADER = "X-API-Key";

    private static final List<String> PUBLIC_PATHS =
            List.of(
                    "/actuator/health",
                    "/v3/api-docs",
                    "/swagger-ui");

    private final byte[] expectedKey;

    public ApiKeyAuthFilter(FinsightProperties properties) {
        this.expectedKey =
                properties.security()
                        .apiKey()
                        .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        return PUBLIC_PATHS.stream()
                .anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {


        String provided = request.getHeader(HEADER);

        if (provided != null
                && MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expectedKey)) {

            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
            {
              "type":"about:blank",
              "title":"Unauthorized",
              "status":401,
              "detail":"Missing or invalid API key"
            }
            """);
    }
}
