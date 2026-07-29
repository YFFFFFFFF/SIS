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
                .andExpect(jsonPath("$.info.title").value("Intelligent Investment Decision Support System API"))
                .andExpect(jsonPath("$.info.version").value("0.1.0"))
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
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/financing-plans'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/calculation-tasks'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/calculation-tasks/{taskId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/calculation-tasks/{taskId}/results'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/import/excel'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/calculation-tasks/{taskId}/reports'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/reports/{reportId}/download'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/approval/submit'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/approval-instances/{instanceId}/approve'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/approval-instances/{instanceId}/reject'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/lock'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/lock'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/audit-events'].get").exists());
    }

    @Test
    void swaggerUiIsAvailableForInteractiveApiExploration() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}