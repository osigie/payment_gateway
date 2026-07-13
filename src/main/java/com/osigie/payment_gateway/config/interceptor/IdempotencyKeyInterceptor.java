package com.osigie.payment_gateway.config.interceptor;


import com.osigie.payment_gateway.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class IdempotencyKeyInterceptor implements HandlerInterceptor {
    private static final String IDEMPOTENCY_KEY = "IDEMPOTENCY_KEY";


    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        //match all post method for now
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        String key = request.getHeader("x-idempotency-key");

        if (key == null || key.isBlank()) {
            throw new BadRequestException("x-idempotency-key is null or blank");
        }

        request.setAttribute(IDEMPOTENCY_KEY, key);

        return true;
    }

}
