package com.trackmyfunds.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        int           status,
        String        message,
        LocalDateTime timestamp,
        List<String>  errors          // non-null only on validation failures
) {}
