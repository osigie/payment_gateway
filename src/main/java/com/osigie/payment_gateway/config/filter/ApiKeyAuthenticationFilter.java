package com.osigie.payment_gateway.config.filter;

import com.osigie.payment_gateway.config.security.ApiAuthenticationToken;
import com.osigie.payment_gateway.config.security.SecurityConfig;
import com.osigie.payment_gateway.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
  private static final String API_KEY_HEADER = "x-api-key";
  private final AuthenticationManager authenticationManager;
  private final HandlerExceptionResolver resolver;
  private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

  @Override
  public boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return Arrays.stream(SecurityConfig.WHITELISTED_ENDPOINTS)
        .anyMatch(pattern -> ANT_PATH_MATCHER.match(pattern, path));
  }

  public ApiKeyAuthenticationFilter(
      AuthenticationManager authenticationManager,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.authenticationManager = authenticationManager;
    this.resolver = resolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {

      String apiKey = request.getHeader(API_KEY_HEADER);

      if (apiKey == null || apiKey.isBlank()) {
        throw new UnauthorizedException("Invalid API Key");
      }

      Authentication authentication =
          this.authenticationManager.authenticate(new ApiAuthenticationToken(apiKey));

      SecurityContextHolder.getContext().setAuthentication(authentication);

      filterChain.doFilter(request, response);

    } catch (Exception ex) {
      resolver.resolveException(request, response, null, ex);
    }
  }
}
