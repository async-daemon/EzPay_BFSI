package com.ezpay.backend.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated {@link com.ezpay.backend.entity.User} resolved
 * from the request's Bearer token — the Spring MVC equivalent of FastAPI's
 * {@code Depends(get_current_user)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CurrentUser {
}
