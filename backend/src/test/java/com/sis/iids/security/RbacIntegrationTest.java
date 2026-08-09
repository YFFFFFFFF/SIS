package com.sis.iids.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousRequestToProtectedApiReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void analystCannotAccessAdminEndpoint() throws Exception {
        String token = login("analyst", "Password123!");

        mockMvc.perform(get("/api/v1/admin/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        String token = login("admin", "Password123!");

        mockMvc.perform(get("/api/v1/admin/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("pong"));
    }

    @Test
    void seededPrdRolesCanLogin() throws Exception {
        login("investment_analyst", "Password123!");
        login("finance_specialist", "Password123!");
        login("technical_engineer", "Password123!");
        login("project_manager", "Password123!");
        login("admin", "Password123!");
    }

    @Test
    void financeSpecialistCannotCreateProject() throws Exception {
        String token = login("finance_specialist", "Password123!");

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SIS-RBAC-FIN\",\"name\":\"Forbidden Project\",\"projectType\":\"INDUSTRIAL\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void financeSpecialistCanUpdateScenarioParameters() throws Exception {
        String analystToken = login("investment_analyst", "Password123!");
        String financeToken = login("finance_specialist", "Password123!");
        Long scenarioId = createScenario(analystToken, createProject(analystToken, "SIS-RBAC-PARAM"));

        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "wacc":0.08,
                                  "waccSource":"finance desk",
                                  "taxRate":0.25,
                                  "depreciationYears":10,
                                  "residualRate":0.05,
                                  "loanRatioLimit":0.7,
                                  "pricePerUnit":100,
                                  "unitCost":60,
                                  "annualOutput":10000,
                                  "fixedOperatingCost":100000,
                                  "formulaVersion":"fin-m1-test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waccSource").value("finance desk"));
    }

    @Test
    void technicalEngineerCanCreateInvestmentItemButCannotUpdateFinancialParameters() throws Exception {
        String analystToken = login("investment_analyst", "Password123!");
        String engineerToken = login("technical_engineer", "Password123!");
        Long scenarioId = createScenario(analystToken, createProject(analystToken, "SIS-RBAC-TECH"));

        mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/investment-items", scenarioId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CONSTRUCTION\",\"name\":\"equipment\",\"amount\":1000000,\"yearNo\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("equipment"));

        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "wacc":0.08,
                                  "taxRate":0.25,
                                  "depreciationYears":10,
                                  "residualRate":0.05,
                                  "loanRatioLimit":0.7,
                                  "pricePerUnit":100,
                                  "unitCost":60,
                                  "annualOutput":10000,
                                  "fixedOperatingCost":100000
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private String login(String username, String password) throws Exception {
        String request = "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return response.replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private Long createProject(String token, String code) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"name\":\"RBAC Project\",\"projectType\":\"INDUSTRIAL\"}".formatted(code)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private Long createScenario(String token, Long projectId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/scenarios", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"RBAC Scenario\",\"horizonYears\":5,\"constructionYears\":1}"))
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