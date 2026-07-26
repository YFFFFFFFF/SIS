package com.sis.iids.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class AuditEventApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void recordsAuditEventWhenCalculationCompletes() throws Exception {
        Long projectId = createProject();
        Long scenarioId = createScenario(projectId);
        upsertParameters(scenarioId);
        createInvestmentItem(scenarioId, "CONSTRUCTION", "建设投资", 200000, 0);
        createInvestmentItem(scenarioId, "WORKING_CAPITAL", "流动资金", 20000, 1);
        createFinancingPlan(scenarioId);

        String taskResponse = mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/calculation-tasks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskType\":\"FINANCIAL\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long taskId = extractNestedTaskId(taskResponse);

        mockMvc.perform(get("/api/v1/audit-events")
                        .param("targetType", "CALCULATION_TASK")
                        .param("targetId", taskId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].action").value("CALCULATION_COMPLETED"))
                .andExpect(jsonPath("$.data[0].targetType").value("CALCULATION_TASK"))
                .andExpect(jsonPath("$.data[0].targetId").value(taskId.toString()));
    }

    private Long createProject() throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SIS-M1-AUDIT\",\"name\":\"Audit Host Project\",\"projectType\":\"INDUSTRIAL\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractId(response);
    }

    private Long createScenario(Long projectId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"审计测算方案\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractId(response);
    }

    private void upsertParameters(Long scenarioId) throws Exception {
        String request = """
                {"wacc":0.10,"taxRate":0.25,"depreciationYears":5,"residualRate":0,"loanRatioLimit":0.70,
                 "pricePerUnit":140,"unitCost":40,"annualOutput":1000,"fixedOperatingCost":10000,
                 "formulaVersion":"fin-m1-1.0.0"}
                """;
        mockMvc.perform(put("/api/v1/scenarios/{scenarioId}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }

    private void createInvestmentItem(Long scenarioId, String category, String name, int amount, int yearNo) throws Exception {
        String request = "{\"category\":\"%s\",\"name\":\"%s\",\"amount\":%d,\"yearNo\":%d}"
                .formatted(category, name, amount, yearNo);
        mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }

    private void createFinancingPlan(Long scenarioId) throws Exception {
        mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/financing-plans", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"EQUITY\",\"ratio\":1.0,\"amount\":220000,\"interestRate\":0,\"termYears\":0}"))
                .andExpect(status().isOk());
    }

    private Long extractId(String response) {
        return Long.valueOf(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }

    private Long extractNestedTaskId(String response) {
        return Long.valueOf(response.replaceAll(".*\\\"task\\\":\\{\\\"id\\\":(\\d+).*", "$1"));
    }
}