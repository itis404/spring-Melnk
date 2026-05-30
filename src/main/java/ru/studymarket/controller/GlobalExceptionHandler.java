package ru.studymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import ru.studymarket.dto.ApiError;
import ru.studymarket.exception.ForbiddenOperationException;
import ru.studymarket.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object notFound(ResourceNotFoundException exception, HttpServletRequest request, Model model) {
        log.warn("Resource not found: {}", exception.getMessage());
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, model, Map.of());
    }

    @ExceptionHandler({ForbiddenOperationException.class, AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Object forbidden(Exception exception, HttpServletRequest request, Model model) {
        log.warn("Forbidden operation: {}", exception.getMessage());
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), request, model, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "Bad Request",
                "Проверьте поля запроса", request.getRequestURI(), fields));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object generic(Exception exception, HttpServletRequest request, Model model) {
        log.error("Unhandled application error", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Что-то пошло не так. Мы уже записали ошибку в лог.", request, model, Map.of());
    }

    private Object error(HttpStatus status,
                         String message,
                         HttpServletRequest request,
                         Model model,
                         Map<String, String> validationErrors) {
        if (wantsJson(request)) {
            return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                    message, request.getRequestURI(), validationErrors));
        }
        model.addAttribute("status", status.value());
        model.addAttribute("error", status.getReasonPhrase());
        model.addAttribute("message", message);
        return new ModelAndView("error/custom", model.asMap(), status);
    }

    private boolean wantsJson(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/")
                || uri.startsWith("/ajax/")
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }
}
