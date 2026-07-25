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
        String request = """
                {
                  "code": "SIS-M1-001",
                  "name": "智能投资测算一期",
                  "projectType": "INDUSTRIAL",
                  "department": "投资发展部",
                  "tags": "m1,测算",
                  "description": "M1 backend core loop sample"
                }
                """;

        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.code").value("SIS-M1-001"))
                .andExpect(jsonPath("$.data.name").value("智能投资测算一期"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/v1/projects/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SIS-M1-001"))
                .andExpect(jsonPath("$.data.department").value("投资发展部"));

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("SIS-M1-001"));
    }
}