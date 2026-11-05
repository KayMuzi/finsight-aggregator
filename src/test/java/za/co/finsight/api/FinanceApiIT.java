package za.co.finsight.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanceApiIT {

    private static final String KEY = "X-API-Key";
    private static final String API_KEY = "test-api-key-0123456789";
    private static final String CUST = "CUST-1001";
    private static final String OCT_FROM = "2026-10-01T00:00:00Z";
    private static final String OCT_TO = "2026-11-01T00:00:00Z";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void rejectsRequestsWithoutApiKey() throws Exception {
        mvc.perform(get("/api/v1/categories")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/categories").header(KEY, "wrong")).andExpect(status().isUnauthorized());
    }

    @Test
    void syncAggregatesAllSourcesAndFlagsCrossSourceDuplicates() throws Exception {
        sync(CUST).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.sources.length()").value(3));

        mvc.perform(get("/api/v1/customers/{c}/transactions", CUST)
                        .param("includeDuplicates", "true")
                        .param("category", "GROCERIES")
                        .param("from", OCT_FROM).param("to", OCT_TO)
                        .header(KEY, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.status=='DUPLICATE')]").exists());
    }

    @Test
    void syncIsIdempotent() throws Exception {
        sync(CUST).andExpect(status().isOk());
        sync(CUST).andExpect(status().isOk())
                .andExpect(jsonPath("$.inserted").value(0))
                .andExpect(jsonPath("$.alreadyKnown").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void failingSourceYieldsPartialSyncWithoutAbortingOthers() throws Exception {
        sync("CUST-2002").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.sources[?(@.source=='WALLET')].status").value(hasItem("FAILED")))
                .andExpect(jsonPath("$.sources[?(@.source=='BANK')].status").value(hasItem("OK")));
    }

    @Test
    void budgetIsSetAndTransactionsAreEvaluatedAgainstIt() throws Exception {
        sync(CUST);
        String body = """
                {"totalLimit": 30000.00,
                 "categoryLimits": [
                   {"category": "GROCERIES", "amount": 5000.00},
                   {"category": "DINING", "amount": 1500.00}]}
                """;
        mvc.perform(put("/api/v1/customers/{c}/budgets/{m}", CUST, "2026-10")
                        .header(KEY, API_KEY).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-10"));

        mvc.perform(get("/api/v1/customers/{c}/budgets/{m}/health", CUST, "2026-10").header(KEY, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[?(@.category=='GROCERIES')].health").value(hasItem("ON_TRACK")));

        mvc.perform(get("/api/v1/customers/{c}/transactions", CUST)
                        .param("budgetStatus", "OVER_BUDGET").header(KEY, API_KEY))
                .andExpect(status().isOk());
    }

    @Test
    void budgetRejectsCategoriesThatAreNotBudgetable() throws Exception {
        String body = """
                {"categoryLimits": [{"category": "SAVINGS", "amount": 1000.00}]}
                """;
        mvc.perform(put("/api/v1/customers/{c}/budgets/{m}", CUST, "2026-09")
                        .header(KEY, API_KEY).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void budgetValidationErrorsAreReportedPerField() throws Exception {
        String body = """
                {"categoryLimits": [{"category": "GROCERIES", "amount": -5}]}
                """;
        mvc.perform(put("/api/v1/customers/{c}/budgets/{m}", CUST, "2026-09")
                        .header(KEY, API_KEY).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void deletingBudgetReturnsNoContentThenNotFound() throws Exception {
        String body = "{\"categoryLimits\": [{\"category\": \"DINING\", \"amount\": 800.00}]}";
        mvc.perform(put("/api/v1/customers/{c}/budgets/{m}", "CUST-1002", "2026-10")
                .header(KEY, API_KEY).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/customers/{c}/budgets/{m}", "CUST-1002", "2026-10").header(KEY, API_KEY))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/customers/{c}/budgets/{m}", "CUST-1002", "2026-10").header(KEY, API_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidMonthIsRejected() throws Exception {
        mvc.perform(get("/api/v1/customers/{c}/budgets/{m}", CUST, "2026-13").header(KEY, API_KEY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recategorisingTransactionSetsUserSourceAndKeepsItOnResync() throws Exception {
        sync(CUST);
        MvcResult found = mvc.perform(get("/api/v1/customers/{c}/transactions", CUST)
                        .param("merchant", "Steers").header(KEY, API_KEY))
                .andExpect(status().isOk()).andReturn();
        long id = mapper.readTree(found.getResponse().getContentAsString()).get("content").get(0).get("id").asLong();

        mvc.perform(patch("/api/v1/customers/{c}/transactions/{id}/category", CUST, id)
                        .header(KEY, API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"SHOPPING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("SHOPPING"))
                .andExpect(jsonPath("$.categorySource").value("USER"));

        sync(CUST).andExpect(status().isOk());
        mvc.perform(get("/api/v1/customers/{c}/transactions/{id}", CUST, id).header(KEY, API_KEY))
                .andExpect(jsonPath("$.category").value("SHOPPING"));
    }

    @Test
    void summaryReportsIncomeAndDuplicatesExcluded() throws Exception {
        sync(CUST);
        mvc.perform(get("/api/v1/customers/{c}/summary/{m}", CUST, "2026-10").header(KEY, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(32000.0))
                .andExpect(jsonPath("$.currency").value("ZAR"))
                .andExpect(jsonPath("$.duplicatesExcluded").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.topCategories").isArray())
                .andExpect(jsonPath("$.bySource.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void analyticsEndpointsReturnAggregates() throws Exception {
        sync(CUST);
        mvc.perform(get("/api/v1/customers/{c}/analytics/categories/{m}", CUST, "2026-10").header(KEY, API_KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categories").isArray());
        mvc.perform(get("/api/v1/customers/{c}/analytics/merchants/{m}", CUST, "2026-10")
                        .param("limit", "5").header(KEY, API_KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/v1/customers/{c}/analytics/daily/{m}", CUST, "2026-10").header(KEY, API_KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/v1/customers/{c}/analytics/trend", CUST)
                        .param("from", "2026-09").param("to", "2026-10").header(KEY, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].yearMonth").value("2026-09"));
    }

    @Test
    void trendRejectsReversedRange() throws Exception {
        mvc.perform(get("/api/v1/customers/{c}/analytics/trend", CUST)
                        .param("from", "2026-10").param("to", "2026-09").header(KEY, API_KEY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void categoriesEndpointListsBudgetability() throws Exception {
        mvc.perform(get("/api/v1/categories").header(KEY, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.category=='SAVINGS')].budgetable").value(hasItem(false)))
                .andExpect(jsonPath("$[?(@.category=='GROCERIES')].budgetable").value(hasItem(true)));
    }

    @Test
    void unknownTransactionReturnsNotFound() throws Exception {
        mvc.perform(get("/api/v1/customers/{c}/transactions/{id}", CUST, 999999999L).header(KEY, API_KEY))
                .andExpect(status().isNotFound());
    }

    private ResultActions sync(String customerId) throws Exception {
        return mvc.perform(post("/api/v1/customers/{c}/sync", customerId).header(KEY, API_KEY));
    }
}
