package com.sis.iids.library;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-16 项目库与知识沉淀 API 集成测试（FR-03-03）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class LibraryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.sis.iids.calculation.CalculationService calculationService;

    @Test
    void tagsRoundTripAndSearch() throws Exception {
        Long projectId = createProject("SIS-R16-TAG");
        mockMvc.perform(put("/api/v1/projects/{id}/tags", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"光伏\", \"重点\", \"光伏\", \"  区域A  \"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0]").value("光伏"));

        mockMvc.perform(get("/api/v1/project-library").param("tag", "光伏"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("SIS-R16-TAG"));

        mockMvc.perform(get("/api/v1/project-library").param("tag", "不存在"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/project-library").param("keyword", "R16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/v1/project-library").param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void reviewWithDeviation() throws Exception {
        Long projectId = createProject("SIS-R16-REV");
        Long scenarioId = createCalculatedScenario(projectId, 140);

        mockMvc.perform(post("/api/v1/projects/{id}/review", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":%d,"actualNpv":80000,"actualIrr":0.22,"actualInvestment":230000,
                                 "actualPaybackYears":3.5,"operationStartDate":"2025-06-01",
                                 "lessons":"售价假设偏乐观，实际投产爬坡慢于预期。"}
                                """.formatted(scenarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plannedNpv", closeTo(88022.58, 1.0)))
                .andExpect(jsonPath("$.data.npvDeviation", notNullValue()))
                .andExpect(jsonPath("$.data.irrDeviation", notNullValue()));

        mockMvc.perform(get("/api/v1/projects/{id}/review", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actualNpv").value(80000))
                .andExpect(jsonPath("$.data.scenarioName").value("复盘对照方案"));

        // 检索 hasReview 标记
        mockMvc.perform(get("/api/v1/project-library").param("keyword", "R16-REV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].hasReview").value(true))
                .andExpect(jsonPath("$.data[0].latestNpv", notNullValue()));
    }

    @Test
    void reviewRejectsScenarioOfOtherProject() throws Exception {
        Long projectA = createProject("SIS-R16-PA");
        Long projectB = createProject("SIS-R16-PB");
        Long scenarioB = createScenarioOnly(projectB, "B 方案");

        mockMvc.perform(post("/api/v1/projects/{id}/review", projectA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenarioId\":" + scenarioB + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewNotFoundReturns400() throws Exception {
        Long projectId = createProject("SIS-R16-NONE");
        mockMvc.perform(get("/api/v1/projects/{id}/review", projectId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ============================================================
    // fixtures
    // ============================================================
    private Long createCalculatedScenario(Long projectId, int price) throws Exception {
        Long scenarioId = createScenarioOnly(projectId, "复盘对照方案");
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                {"wacc":0.10,"taxRate":0.25,"depreciationYears":5,"residualRate":0,"loanRatioLimit":0.70,
                                 "pricePerUnit":%d,"unitCost":40,"annualOutput":1000,"fixedOperatingCost":10000}
                                """).formatted(price)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CONSTRUCTION\",\"name\":\"建设投资\",\"amount\":200000,\"yearNo\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"WORKING_CAPITAL\",\"name\":\"流动资金\",\"amount\":20000,\"yearNo\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/financing-plans", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"EQUITY\",\"ratio\":1,\"amount\":220000,\"interestRate\":0,\"termYears\":0}"))
                .andExpect(status().isOk());
        String taskResp = mockMvc.perform(post("/api/v1/scenarios/{id}/calculation-tasks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"taskType\":\"FULL\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        calculationService.executeTask(extractId(taskResp));
        return scenarioId;
    }

    private Long createScenarioOnly(Long projectId, String name) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractId(resp);
    }

    private Long createProject(String code) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"name\":\"Host\",\"projectType\":\"INDUSTRIAL\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractId(resp);
    }

    private Long extractId(String response) {
        return Long.valueOf(response.replaceAll("(?s).*\\\"id\\\":(\\d+).*", "$1"));
    }
}
