package com.ezpay.backend.web;

import com.ezpay.backend.entity.AuthenticationSession;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.exception.ApiException;
import com.ezpay.backend.repository.AuthenticationSessionRepository;
import com.ezpay.backend.repository.UserRepository;
import com.ezpay.backend.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtService jwtService;
    private final AuthenticationSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public CurrentUserArgumentResolver(
            JwtService jwtService,
            AuthenticationSessionRepository sessionRepository,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String token = extractBearerToken(request);
        Claims claims = jwtService.decode(token);

        String sessionToken = claims.get("sid", String.class);
        AuthenticationSession session = sessionRepository.findBySessionTokenAndValidIsTrue(sessionToken)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired, please log in again"));

        User user = userRepository.findById(claims.getSubject())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        return user;
    }

    public static String extractBearerToken(HttpServletRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return header.substring(7).trim();
    }
}
