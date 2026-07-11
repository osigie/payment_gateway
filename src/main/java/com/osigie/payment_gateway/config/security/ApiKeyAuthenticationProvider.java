package com.osigie.payment_gateway.config.security;

import com.osigie.payment_gateway.domain.MerchantPrincipal;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.repository.MerchantRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {
    private final MerchantRepository merchantRepository;

    public ApiKeyAuthenticationProvider(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String apiKey = (String) authentication.getCredentials();

//        TODO: create exception for this
        Merchant merchant = merchantRepository.findByApiKey(apiKey).orElseThrow(() -> new RuntimeException("Merchant with apiKey " + apiKey + " not found"));
        MerchantPrincipal principal = new MerchantPrincipal(merchant.getId(), merchant.getName());

        return new ApiAuthenticationToken(principal, Collections.emptyList());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AbstractAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
