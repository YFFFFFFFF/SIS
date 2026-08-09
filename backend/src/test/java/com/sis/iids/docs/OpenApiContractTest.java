package com.sis.iids.docs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOpenApiDocumentWithM1MetadataAndBearerSecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("智能投资测算与决策支持系统 API"))
                .andExpect(jsonPath("$.info.version").value("0.2.0"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void openApiDocumentMarksLoginAsPublicOperation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security", empty()));
    }

    @Test
    void openApiDocumentIncludesCoreM1ApiMethods() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{projectId}/scenarios'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{projectId}/scenarios'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{id}/parameters'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{id}/parameters'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/investment-items'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/investment-items'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/investment-items/{itemId}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/investment-items/{itemId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/investment-summary'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/cost-items'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/cost-items'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/cost-items/{itemId}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/cost-items/{itemId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/calculation-tasks/{taskId}/statements'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/calculation-tasks/{taskId}/profit-flow'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/calculation-tasks/{taskId}/loan-schedule'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/sensitivity'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/sensitivity'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/sensitivity-runs/{runId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/reverse-runs'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/reverse-runs'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/reverse-runs/{runId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/break-even'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/monte-carlo-runs'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/monte-carlo-runs'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/monte-carlo-runs/{runId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/risk-rules'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/risk-rules'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/risk-rules/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/risk-rules/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/risk-alerts/evaluate'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/risk-alerts'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/risk-alerts/{id}/ack'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/portfolio-runs'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/portfolio-runs/{runId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/approval-flows'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/approval-flows'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/approval-flows/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/approval-flows/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/approval-instances/{instanceId}/timeline'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/comments'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/comments'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/changes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/presence'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/presence'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/collab/stream'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/collab/fields'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/field-locks'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/field-locks'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/field-locks/release'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/field-locks/force-release'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/project-library'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{projectId}/tags'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{projectId}/tags'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{projectId}/review'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{projectId}/review'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{projectId}/ai/operation-records'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{projectId}/ai/operation-records'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/ai/param-recommendation'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/ai/score'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects/{projectId}/comparison'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/financing-plans'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/calculation-tasks'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/calculation-tasks/{taskId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/calculation-tasks/{taskId}/results'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/import/excel'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/calculation-tasks/{taskId}/reports'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/calculation-tasks/{taskId}/reports'].post.parameters[?(@.name=='format')]").exists())
                .andExpect(jsonPath("$.paths['/api/v1/reports/{reportId}/download'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/approval/submit'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/approval-instances/{instanceId}/approve'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/approval-instances/{instanceId}/reject'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/lock'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/lock'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/audit-events'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/audit-events/chain/verify'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/dashboard/summary'].get").exists());
    }

    @Test
    void swaggerUiIsAvailableForInteractiveApiExploration() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}