package com.sis.iids.audit;

import com.sis.iids.worker.CalculationWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class AuditEventApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalculationWorker calculationWorker;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditService auditService;

    @Test
    void chainLinksNewEventsAndVerifiesIntact() throws Exception {
        // 写入两条事件，校验链式哈希挂接
        auditService.record("TEST_ACTION_A", "TEST", "1", null, "after-a");
        auditService.record("TEST_ACTION_B", "TEST", "2", "before-b", "after-b");

        java.util.List<AuditEvent> events = auditEventRepository.findAllByOrderByIdAsc().stream()
                .filter(e -> e.getHash() != null && e.getAction().startsWith("TEST_ACTION_"))
                .toList();
        org.assertj.core.api.Assertions.assertThat(events).hasSizeGreaterThanOrEqualTo(2);
        AuditEvent first = events.get(0);
        AuditEvent second = events.get(1);
        org.assertj.core.api.Assertions.assertThat(first.getPrevHash()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(second.getPrevHash()).isEqualTo(first.getHash());
        org.assertj.core.api.Assertions.assertThat(second.getHash()).hasSize(64);

        mockMvc.perform(get("/api/v1/audit-events/chain/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intact").value(true))
                .andExpect(jsonPath("$.data.brokenCount").value(0))
                .andExpect(jsonPath("$.data.linkedEvents").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void verifyDetectsTamperedEvent() throws Exception {
        auditService.record("TEST_TAMPER", "TEST", "9", null, "original");
        AuditEvent event = auditEventRepository.findAllByOrderByIdAsc().stream()
                .filter(e -> "TEST_TAMPER".equals(e.getAction()))
                .findFirst().orElseThrow();
        // 模拟直接改库篡改：绕过 Service 修改 after_value（hash 未同步更新）
        event.setAfterValue("tampered");
        auditEventRepository.save(event);

        mockMvc.perform(get("/api/v1/audit-events/chain/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intact").value(false))
                .andExpect(jsonPath("$.data.brokenCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.brokenEventIds", org.hamcrest.Matchers.hasItem(event.getId().intValue())));
    }

    @Test
    void recordsAuditEventWhenCalculationCompletes() throws Exception {
        Long projectId = createProject();
        Long scenarioId = createScenario(projectId);
        upsertParameters(scenarioId);
        createInvestmentItem(scenarioId, "CONSTRUCTION", "Construction Investment", 200000, 0);
        createInvestmentItem(scenarioId, "WORKING_CAPITAL", "Working Capital", 20000, 1);
        createFinancingPlan(scenarioId);

        String taskResponse = mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/calculation-tasks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskType\":\"FINANCIAL\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long taskId = extractNestedTaskId(taskResponse);
        calculationWorker.runPendingOnce();

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
                        .content("{\"name\":\"Audit Scenario\",\"horizonYears\":5,\"constructionYears\":1}"))
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
        return Long.valueOf(response.replaceAll(".*\\\"data\\\":\\{\\\"id\\\":(\\d+).*", "$1"));
    }

    private Long extractNestedTaskId(String response) {
        return Long.valueOf(response.replaceAll(".*\\\"task\\\":\\{\\\"id\\\":(\\d+).*", "$1"));
    }
}