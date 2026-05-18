package com.trackmyfunds.exception;

/**
 * Thrown for unrecoverable failures around the Gemini API — e.g. missing API
 * key, malformed configuration. Routine network/parsing errors are still
 * swallowed inside {@code GeminiService} so individual AI features can degrade
 * gracefully with a fallback string.
 *
 * <p>Caught by {@code AiExceptionHandler} which renders a friendly error page.
 */
public class GeminiApiException extends RuntimeException {

    public GeminiApiException(String message) {
        super(message);
    }

    public GeminiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
