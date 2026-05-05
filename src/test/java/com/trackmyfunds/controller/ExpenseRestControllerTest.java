package com.trackmyfunds.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackmyfunds.dto.ExpenseRequestDTO;
import com.trackmyfunds.dto.SummaryStatsDTO;
import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import com.trackmyfunds.model.Expense;
import com.trackmyfunds.service.ExpenseService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for the REST API layer.
 * GlobalExceptionHandler is loaded automatically (it is @RestControllerAdvice
 * and scoped to @RestController beans), so 404 and 400 error shapes are
 * tested here end-to-end without a real database.
 */
@WebMvcTest(ExpenseRestController.class)
class ExpenseRestControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean ExpenseService expenseService;
    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Expense sampleExpense;

    @BeforeEach
    void setUp() {
        sampleExpense = Expense.builder()
                .id(1L).title("Metro Card").amount(new BigDecimal("350.00"))
                .category(Category.TRANSPORT).paymentMethod(PaymentMethod.CARD)
                .date(LocalDate.of(2024, 4, 10)).build();
    }

    // ── GET /api/expenses ─────────────────────────────────────────────────────

    @Test
    void getExpenses_returns200_withPageStructure() throws Exception {
        when(expenseService.getFilteredExpenses(any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of(sampleExpense)));

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Metro Card"))
                .andExpect(jsonPath("$.content[0].category").value("TRANSPORT"))
                .andExpect(jsonPath("$.content[0].amount").value(350.00));
    }

    @Test
    void getExpenses_withCategoryFilter_passesFilterToService() throws Exception {
        when(expenseService.getFilteredExpenses(any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/expenses").param("category", "FOOD"))
                .andExpect(status().isOk());

        verify(expenseService).getFilteredExpenses(
                argThat(f -> f.category() == Category.FOOD),
                anyInt(), anyInt(), anyString(), anyString());
    }

    // ── GET /api/expenses/summary ─────────────────────────────────────────────

    @Test
    void getSummary_returns200_withAllStatFields() throws Exception {
        when(expenseService.getSummaryStats(any()))
                .thenReturn(new SummaryStatsDTO(
                        new BigDecimal("1200.00"), 4L,
                        new BigDecimal("300.00"),  new BigDecimal("500.00")));

        mockMvc.perform(get("/api/expenses/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(1200.00))
                .andExpect(jsonPath("$.totalCount").value(4))
                .andExpect(jsonPath("$.averageAmount").value(300.00))
                .andExpect(jsonPath("$.highestExpense").value(500.00));
    }

    // ── GET /api/expenses/{id} ────────────────────────────────────────────────

    @Test
    void getById_knownId_returns200_withExpenseJson() throws Exception {
        when(expenseService.getExpenseById(1L)).thenReturn(sampleExpense);

        mockMvc.perform(get("/api/expenses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Metro Card"))
                .andExpect(jsonPath("$.amount").value(350.00))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.date").value("2024-04-10"));
    }

    @Test
    void getById_unknownId_returns404_withStructuredErrorBody() throws Exception {
        when(expenseService.getExpenseById(99L))
                .thenThrow(new EntityNotFoundException("Expense not found: 99"));

        mockMvc.perform(get("/api/expenses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Expense not found: 99"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").doesNotExist()); // null → omitted by @JsonInclude
    }

    // ── POST /api/expenses ────────────────────────────────────────────────────

    @Test
    void postExpense_validBody_returns201_withCreatedExpense() throws Exception {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(
                "Groceries", new BigDecimal("450.00"), Category.FOOD,
                PaymentMethod.UPI, LocalDate.of(2024, 5, 1), null);

        Expense created = Expense.builder()
                .id(2L).title("Groceries").amount(new BigDecimal("450.00"))
                .category(Category.FOOD).paymentMethod(PaymentMethod.UPI)
                .date(LocalDate.of(2024, 5, 1)).build();
        when(expenseService.createExpense(any())).thenReturn(created);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Groceries"))
                .andExpect(jsonPath("$.category").value("FOOD"));
    }

    @Test
    void postExpense_blankTitleAndNegativeAmount_returns400_withErrorsList() throws Exception {
        String invalidJson = """
                {
                    "title":         "",
                    "amount":        -10,
                    "category":      "FOOD",
                    "paymentMethod": "CARD",
                    "date":          "2024-01-15"
                }""";

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists()); // at least one field error
    }

    @Test
    void postExpense_missingRequiredFields_returns400_withMessageSummary() throws Exception {
        String emptyJson = "{}";

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Validation failed")));
    }

    // ── PUT /api/expenses/{id} ────────────────────────────────────────────────

    @Test
    void putExpense_validBody_returns200_withUpdatedFields() throws Exception {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(
                "Updated Title", new BigDecimal("600.00"), Category.SHOPPING,
                PaymentMethod.CARD, LocalDate.of(2024, 5, 10), "online order");

        Expense updated = Expense.builder()
                .id(1L).title("Updated Title").amount(new BigDecimal("600.00"))
                .category(Category.SHOPPING).paymentMethod(PaymentMethod.CARD)
                .date(LocalDate.of(2024, 5, 10)).build();
        when(expenseService.updateExpense(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.category").value("SHOPPING"));
    }

    @Test
    void putExpense_unknownId_returns404() throws Exception {
        when(expenseService.updateExpense(eq(99L), any()))
                .thenThrow(new EntityNotFoundException("Expense not found: 99"));

        ExpenseRequestDTO dto = new ExpenseRequestDTO(
                "Title", BigDecimal.TEN, Category.OTHER,
                PaymentMethod.CASH, LocalDate.now(), null);

        mockMvc.perform(put("/api/expenses/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── DELETE /api/expenses/{id} ─────────────────────────────────────────────

    @Test
    void deleteExpense_knownId_returns204NoContent() throws Exception {
        doNothing().when(expenseService).deleteExpense(1L);

        mockMvc.perform(delete("/api/expenses/1"))
                .andExpect(status().isNoContent());

        verify(expenseService).deleteExpense(1L);
    }

    @Test
    void deleteExpense_unknownId_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Expense not found: 99"))
                .when(expenseService).deleteExpense(99L);

        mockMvc.perform(delete("/api/expenses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
