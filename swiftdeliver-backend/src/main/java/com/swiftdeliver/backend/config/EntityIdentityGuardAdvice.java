package com.swiftdeliver.backend.config;

import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Many endpoints bind a JPA entity straight from the request body. If the client may choose the
 * entity's {@code id}, a plain "create" silently becomes an overwrite of somebody else's row (a
 * customer could take over another customer's order just by POSTing with that order's id).
 *
 * <p>So the identity always comes from the URL, never from the body:
 * <ul>
 *   <li>POST: the {@code id} is cleared, a new row is always inserted;</li>
 *   <li>PUT/PATCH on {@code /{id}}: the body's {@code id} is replaced by the path's.</li>
 * </ul>
 * References to other entities ({@code "vendorCompany": {"id": 3}}) are untouched.
 */
@ControllerAdvice
public class EntityIdentityGuardAdvice extends RequestBodyAdviceAdapter {

    private static final Logger log = LoggerFactory.getLogger(EntityIdentityGuardAdvice.class);

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return targetType instanceof Class<?> type && type.isAnnotationPresent(Entity.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return body;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        Method setId = findSetId(body.getClass());
        if (setId == null) {
            return body;
        }
        try {
            switch (request.getMethod()) {
                case "POST" -> setId.invoke(body, new Object[]{null});
                case "PUT", "PATCH" -> {
                    Map<String, String> vars = (Map<String, String>) request.getAttribute(
                            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                    String pathId = vars == null ? null : vars.get("id");
                    if (pathId != null) {
                        setId.invoke(body, Long.valueOf(pathId));
                    }
                }
                default -> { }
            }
        } catch (ReflectiveOperationException | NumberFormatException e) {
            log.warn("Could not normalise the id of a {} body: {}", body.getClass().getSimpleName(), e.getMessage());
        }
        return body;
    }

    private static Method findSetId(Class<?> type) {
        try {
            return type.getMethod("setId", Long.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
