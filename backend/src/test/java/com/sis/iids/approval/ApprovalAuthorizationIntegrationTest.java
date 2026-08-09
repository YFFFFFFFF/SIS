package com.sis.iids.approval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApprovalAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void investmentAnalystCannotApproveReviewNode() throws Exception {
        String analystToken = login("investment_analyst", "Password123!");
        Long scenarioId = createScenario(analystToken, createProject(analystToken, "SIS-APP-AUTH-REVIEW"));
        Long instanceId = submit(analystToken, scenarioId);

        mockMvc.perform(post("/api/v1/approval-instances/{instanceId}/review/approve", instanceId)
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"self review\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void financeSpecialistCanReviewButCannotFinalApprove() throws Exception {
        String analystToken = login("investment_analyst", "Password123!");
        String financeToken = login("finance_specialist", "Password123!");
        Long scenarioId = createScenario(analystToken, createProject(analystToken, "SIS-APP-AUTH-FIN"));
        Long instanceId = submit(analystToken, scenarioId);

        mockMvc.perform(post("/api/v1/approval-instances/{instanceId}/review/approve", instanceId)
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"finance review ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentNode").value("APPROVAL"));

        mockMvc.perform(post("/api/v1/approval-instances/{instanceId}/approve", instanceId)
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"finance final approval\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void projectManagerCanFinalApprove() throws Exception {
        String analystToken = login("investment_analyst", "Password123!");
        String financeToken = login("finance_specialist", "Password123!");
        String managerToken = login("project_manager", "Password123!");
        Long scenarioId = createScenario(analystToken, createProject(analystToken, "SIS-APP-AUTH-MGR"));
        Long instanceId = submit(analystToken, scenarioId);

        mockMvc.perform(post("/api/v1/approval-instances/{instanceId}/review/approve", instanceId)
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"finance review ok\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/approval-instances/{instanceId}/approve", instanceId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"manager approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    private Long submit(String token, Long scenarioId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/approval/submit", scenarioId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"submit\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
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
                        .content("{\"code\":\"%s\",\"name\":\"Approval Auth Project\",\"projectType\":\"INDUSTRIAL\"}".formatted(code)))
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
                        .content("{\"name\":\"Approval Auth Scenario\",\"horizonYears\":5,\"constructionYears\":1}"))
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