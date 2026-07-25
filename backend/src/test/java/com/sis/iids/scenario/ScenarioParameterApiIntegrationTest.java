package com.sis.iids.scenario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
@Transactional
class ScenarioParameterApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsScenarioAndUpsertsParameterSet() throws Exception {
        Long projectId = createProject();

        String scenarioRequest = """
                {
                  "name": "基准测算方案",
                  "horizonYears": 5,
                  "constructionYears": 1,
                  "remarks": "M1 baseline scenario"
                }
                """;

        String scenarioResponse = mockMvc.perform(post("/api/v1/projects/{projectId}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scenarioRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.name").value("基准测算方案"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long scenarioId = extractId(scenarioResponse);

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

    private Long extractId(String response) {
        return Long.valueOf(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }
}