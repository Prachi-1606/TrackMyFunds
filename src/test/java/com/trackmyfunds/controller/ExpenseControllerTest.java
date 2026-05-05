package com.trackmyfunds.controller;

import com.trackmyfunds.dto.DashboardDTO;
import com.trackmyfunds.dto.ExpenseRequestDTO;
import com.trackmyfunds.dto.SummaryStatsDTO;
import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import com.trackmyfunds.model.Expense;
import com.trackmyfunds.service.CsvExportService;
import com.trackmyfunds.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for the MVC controller layer only.
 * All service calls are mocked; Thymeleaf templates are fully rendered.
 *
 * @MockBean(JpaMetamodelMappingContext.class) is required because AppConfig
 * enables JPA auditing — @WebMvcTest loads @Configuration beans but lacks the
 * JPA layer, so the auditing context bean must be provided as a mock.
 */
@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean ExpenseService  expenseService;
    @MockBean CsvExportService csvExportService;
    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Expense sampleExpense;

    @BeforeEach
    void setUp() {
        sampleExpense = Expense.builder()
                .id(1L).title("Lunch").amount(new BigDecimal("150.00"))
                .category(Category.FOOD).paymentMethod(PaymentMethod.CARD)
                .date(LocalDate.of(2024, 3, 15)).build();

        // Default stubs used by list and dashboard endpoints
        when(expenseService.getFilteredExpenses(any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of()));
        when(expenseService.getSummaryStats(any()))
                .thenReturn(new SummaryStatsDTO(BigDecimal.ZERO, 0L, BigDecimal.ZERO, BigDecimal.ZERO));
        when(expenseService.getDashboardData())
                .thenReturn(new DashboardDTO(Map.of(), Map.of(), List.of()));
    }

    // ── GET /expenses ─────────────────────────────────────────────────────────

    @Test
    void getExpenses_returns200_withRequiredModelAttributes() throws Exception {
        mockMvc.perform(get("/expenses"))
                .andExpect(status().isOk())
                .andExpect(view().name("expenses/list"))
                .andExpect(model().attributeExists(
                        "expenses", "stats", "categories", "paymentMethods",
                        "sortBy", "sortDir", "size", "currentPage"));
    }

    @Test
    void getExpenses_withCategoryParam_passesFilterToService() throws Exception {
        mockMvc.perform(get("/expenses").param("category", "FOOD"))
                .andExpect(status().isOk());

        verify(expenseService).getFilteredExpenses(
                argThat(f -> f.category() == Category.FOOD),
                anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void getExpenses_defaultPaginationParams_arePassedCorrectly() throws Exception {
        mockMvc.perform(get("/expenses"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sortBy",  "date"))
                .andExpect(model().attribute("sortDir", "desc"))
                .andExpect(model().attribute("size",    10));
    }

    // ── GET /dashboard ────────────────────────────────────────────────────────

    @Test
    void getDashboard_returns200_withAllChartModelAttributes() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists(
                        "categoryLabels", "categoryValues",
                        "monthLabels",    "monthValues",
                        "topExpenses"));
    }

    // ── GET /expenses/new ─────────────────────────────────────────────────────

    @Test
    void getNewForm_returns200_withEmptyDTOAndIsEditFalse() throws Exception {
        mockMvc.perform(get("/expenses/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("expenses/form"))
                .andExpect(model().attribute("isEdit", false))
                .andExpect(model().attributeExists("expense", "categories", "paymentMethods"));
    }

    // ── POST /expenses ────────────────────────────────────────────────────────

    @Test
    void postExpenses_validInput_redirectsToList() throws Exception {
        when(expenseService.createExpense(any())).thenReturn(sampleExpense);

        mockMvc.perform(post("/expenses")
                        .param("title",         "Lunch")
                        .param("amount",        "150.00")
                        .param("category",      "FOOD")
                        .param("paymentMethod", "CARD")
                        .param("date",          "2024-03-15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/expenses"));

        verify(expenseService).createExpense(any(ExpenseRequestDTO.class));
    }

    @Test
    void postExpenses_blankTitle_redisplaysFormWithFieldError() throws Exception {
        mockMvc.perform(post("/expenses")
                        .param("title",         "")   // violates @NotBlank
                        .param("amount",        "150.00")
                        .param("category",      "FOOD")
                        .param("paymentMethod", "CARD")
                        .param("date",          "2024-03-15"))
                .andExpect(status().isOk())
                .andExpect(view().name("expenses/form"))
                .andExpect(model().attributeHasFieldErrors("expense", "title"));
    }

    @Test
    void postExpenses_missingAmount_redisplaysFormWithFieldError() throws Exception {
        mockMvc.perform(post("/expenses")
                        .param("title",         "Valid Title")
                        .param("category",      "FOOD")
                        .param("paymentMethod", "CARD")
                        .param("date",          "2024-03-15"))
                // amount not provided — @NotNull fires
                .andExpect(status().isOk())
                .andExpect(view().name("expenses/form"))
                .andExpect(model().attributeHasFieldErrors("expense", "amount"));
    }

    // ── GET /expenses/{id}/edit ───────────────────────────────────────────────

    @Test
    void getEditForm_returns200_withPopulatedExpenseAndIsEditTrue() throws Exception {
        when(expenseService.getExpenseById(1L)).thenReturn(sampleExpense);

        mockMvc.perform(get("/expenses/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("expenses/form"))
                .andExpect(model().attribute("isEdit",    true))
                .andExpect(model().attribute("expenseId", 1L));
    }

    // ── POST /expenses/{id} ───────────────────────────────────────────────────

    @Test
    void postUpdateExpense_validInput_redirectsToList() throws Exception {
        when(expenseService.updateExpense(eq(1L), any())).thenReturn(sampleExpense);

        mockMvc.perform(post("/expenses/1")
                        .param("title",         "Updated Lunch")
                        .param("amount",        "200.00")
                        .param("category",      "FOOD")
                        .param("paymentMethod", "UPI")
                        .param("date",          "2024-04-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/expenses"));

        verify(expenseService).updateExpense(eq(1L), any(ExpenseRequestDTO.class));
    }

    // ── POST /expenses/{id}/delete ────────────────────────────────────────────

    @Test
    void postDeleteExpense_redirectsToList_andInvokesService() throws Exception {
        mockMvc.perform(post("/expenses/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/expenses"));

        verify(expenseService).deleteExpense(1L);
    }
}
