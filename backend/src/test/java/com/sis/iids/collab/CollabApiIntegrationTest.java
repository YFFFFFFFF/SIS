package com.sis.iids.collab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-15 协同编辑 API 集成测试（FR-04-02）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class CollabApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void commentWithMentionsAndChangeTimeline() throws Exception {
        Long scenarioId = createScenario("SIS-R15-C1");

        mockMvc.perform(post("/api/v1/scenarios/{id}/comments", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"请 @finance_specialist 复核下售价假设，@project_manager 关注\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.mentions", containsString("finance_specialist")))
                .andExpect(jsonPath("$.data.mentions", containsString("project_manager")));

        mockMvc.perform(get("/api/v1/scenarios/{id}/comments", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 评论自动产生变更时间线条目（版本递增）
        mockMvc.perform(get("/api/v1/scenarios/{id}/changes", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].versionNo").value(1))
                .andExpect(jsonPath("$.data[0].changeType").value("COMMENT_ADDED"));

        mockMvc.perform(post("/api/v1/scenarios/{id}/comments", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"第二条评论\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/scenarios/{id}/changes", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].versionNo").value(2));
    }

    @Test
    void presenceHeartbeatAndListing() throws Exception {
        Long scenarioId = createScenario("SIS-R15-P1");

        mockMvc.perform(post("/api/v1/scenarios/{id}/presence", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"userName\":\"investment_analyst\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].userName").value("investment_analyst"));

        // 同用户再次心跳 → 仍然 1 人在线（去重）
        mockMvc.perform(post("/api/v1/scenarios/{id}/presence", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"userName\":\"investment_analyst\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(post("/api/v1/scenarios/{id}/presence", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"userName\":\"finance_specialist\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        mockMvc.perform(get("/api/v1/scenarios/{id}/presence", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void sseStreamStartsAsync() throws Exception {
        Long scenarioId = createScenario("SIS-R15-S1");
        mockMvc.perform(get("/api/v1/scenarios/{id}/collab/stream", scenarioId))
                .andExpect(request().asyncStarted())
                .andReturn();
    }

    @Test
    void rejectsCommentOnMissingScenario() throws Exception {
        mockMvc.perform(post("/api/v1/scenarios/{id}/comments", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private Long createScenario(String code) throws Exception {
        Long projectId = createProject(code);
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"协同场景\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractId(scenarioResp);
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
