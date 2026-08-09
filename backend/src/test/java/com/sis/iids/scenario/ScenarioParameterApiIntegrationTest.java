package com.sis.iids.scenario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class ScenarioParameterApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsScenarioAndUpsertsParameterSet() throws Exception {
        Long projectId = createProject();
        Long scenarioId = createScenario(projectId, "Baseline Scenario");

        mockMvc.perform(get("/api/v1/scenarios/{id}", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.horizonYears").value(5))
                .andExpect(jsonPath("$.data.constructionYears").value(1));

        String parameterRequest = """
                {
                  "wacc": 0.10,
                  "waccSource": "manual benchmark",
                  "taxRate": 0.25,
                  "depreciationYears": 5,
                  "residualRate": 0,
                  "loanRatioLimit": 0.70,
                  "pricePerUnit": 140,
                  "unitCost": 40,
                  "annualOutput": 1000,
                  "fixedOperatingCost": 10000,
                  "formulaVersion": "fin-m1-1.0.0"
                }
                """;

        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(parameterRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarioId").value(scenarioId))
                .andExpect(jsonPath("$.data.wacc").value(0.10))
                .andExpect(jsonPath("$.data.depreciationYears").value(5))
                .andExpect(jsonPath("$.data.pricePerUnit").value(140));

        mockMvc.perform(get("/api/v1/scenarios/{id}/parameters", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarioId").value(scenarioId))
                .andExpect(jsonPath("$.data.waccSource").value("manual benchmark"))
                .andExpect(jsonPath("$.data.fixedOperatingCost").value(10000));
    }

    @Test
    void updatesScenarioFieldsAndWritesAuditEvents() throws Exception {
        Long projectId = createProject();
        Long scenarioId = createScenario(projectId, "Original Scenario");

        String updateRequest = """
                {
                  "name": "Updated Scenario",
                  "status": "ACTIVE",
                  "horizonYears": 8,
                  "constructionYears": 2,
                  "remarks": "Updated scenario remarks"
                }
                """;

        mockMvc.perform(put("/api/v1/scenarios/{id}", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(scenarioId))
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.name").value("Updated Scenario"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.horizonYears").value(8))
                .andExpect(jsonPath("$.data.constructionYears").value(2))
                .andExpect(jsonPath("$.data.remarks").value("Updated scenario remarks"));

        mockMvc.perform(get("/api/v1/audit-events")
                        .param("targetType", "SCENARIO")
                        .param("targetId", scenarioId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("SCENARIO_CREATED"))
                .andExpect(jsonPath("$.data[1].action").value("SCENARIO_UPDATED"));
    }

    private Long createProject() throws Exception {
        String request = """
                {
                  "code": "SIS-M1-SCENARIO",
                  "name": "Scenario Host Project",
                  "projectType": "INDUSTRIAL"
                }
                """;
        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private Long createScenario(Long projectId, String name) throws Exception {
        String request = """
                {
                  "name": "%s",
                  "horizonYears": 5,
                  "constructionYears": 1,
                  "remarks": "M1 baseline scenario"
                }
                """.formatted(name);

        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private Long extractId(String response) {
        return Long.valueOf(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }
}
