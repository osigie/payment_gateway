package com.osigie.payment_gateway.config.security;

import com.osigie.payment_gateway.domain.MerchantPrincipal;
import java.util.Collection;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class ApiAuthenticationToken extends AbstractAuthenticationToken {
  private final Object principal;
  private final String apiKey;

  //    authentication token
  public ApiAuthenticationToken(
      MerchantPrincipal principal, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.apiKey = null;
    setAuthenticated(true);
  }

  //    unauthenticated token
  public ApiAuthenticationToken(String apiKey) {
    super((Collection<? extends GrantedAuthority>) null);
    this.principal = null;
    this.apiKey = apiKey;
    setAuthenticated(false);
  }

  @Override
  public @Nullable Object getCredentials() {
    return apiKey;
  }

  @Override
  public @Nullable Object getPrincipal() {
    return principal;
  }
}
