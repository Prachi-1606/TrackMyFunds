package com.trackmyfunds.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * MVC-scoped advice that intercepts {@link GeminiApiException} and renders a
 * friendly HTML page rather than the default Spring error stack trace.
 * <p>
 * Restricted to {@code @Controller} beans so it doesn't shadow the
 * {@code @RestControllerAdvice} for the JSON API; the few {@code @ResponseBody}
 * methods inside controllers will still receive the rendered HTML, which their
 * {@code .catch()} handlers already treat as a non-OK response.
 */
@ControllerAdvice(annotations = Controller.class)
public class AiExceptionHandler {

    @ExceptionHandler(GeminiApiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleGeminiFailure(GeminiApiException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/ai-error";
    }
}
