package com.sis.iids.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ProjectApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsProjectAndReadsItBack() throws Exception {
        String response = createProject("SIS-M1-001", "M1 Investment Project");
        String id = response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/v1/projects/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SIS-M1-001"))
                .andExpect(jsonPath("$.data.department").value("Investment Department"));

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("SIS-M1-001"));
    }

    @Test
    void updatesProjectFieldsAndWritesAuditEvents() throws Exception {
        String response = createProject("SIS-M1-UPD", "Original Project");
        String id = response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1");

        String updateRequest = """
                {
                  "name": "Updated Project",
                  "projectType": "INFRASTRUCTURE",
                  "status": "ACTIVE",
                  "department": "Strategy Office",
                  "ownerId": 1001,
                  "tags": "m1,updated",
                  "description": "Updated project description"
                }
                """;

        mockMvc.perform(put("/api/v1/projects/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(Integer.parseInt(id)))
                .andExpect(jsonPath("$.data.code").value("SIS-M1-UPD"))
                .andExpect(jsonPath("$.data.name").value("Updated Project"))
                .andExpect(jsonPath("$.data.projectType").value("INFRASTRUCTURE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.department").value("Strategy Office"))
                .andExpect(jsonPath("$.data.ownerId").value(1001))
                .andExpect(jsonPath("$.data.tags").value("m1,updated"))
                .andExpect(jsonPath("$.data.description").value("Updated project description"));

        mockMvc.perform(get("/api/v1/audit-events")
                        .param("targetType", "PROJECT")
                        .param("targetId", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("PROJECT_CREATED"))
                .andExpect(jsonPath("$.data[1].action").value("PROJECT_UPDATED"));
    }

    private String createProject(String code, String name) throws Exception {
        String request = """
                {
                  "code": "%s",
                  "name": "%s",
                  "projectType": "INDUSTRIAL",
                  "department": "Investment Department",
                  "tags": "m1,calculation",
                  "description": "M1 backend core loop sample"
                }
                """.formatted(code, name);

        return mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
