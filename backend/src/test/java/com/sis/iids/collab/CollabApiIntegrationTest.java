package com.sis.iids.collab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Autowired
    private ScenarioPresenceRepository presenceRepository;

    @Autowired
    private ScenarioCommentRepository commentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @WithMockUser(username = "comment_author", roles = "INVESTMENT_ANALYST")
    void commentAuthorCanDeleteOwnComment() throws Exception {
        Long scenarioId = createScenario("SIS-R15-C2");
        String response = mockMvc.perform(post("/api/v1/scenarios/{id}/comments", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"待删除评论\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long commentId = extractId(response);

        mockMvc.perform(delete("/api/v1/scenarios/{id}/comments/{commentId}", scenarioId, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        mockMvc.perform(get("/api/v1/scenarios/{id}/comments", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/v1/scenarios/{id}/changes", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].changeType").value("COMMENT_DELETED"));
    }

    @Test
    @WithMockUser(username = "other_user", roles = "INVESTMENT_ANALYST")
    void nonAuthorCannotDeleteComment() throws Exception {
        Long scenarioId = createScenario("SIS-R15-C3");
        ScenarioComment comment = new ScenarioComment();
        comment.setScenarioId(scenarioId);
        comment.setContent("他人评论");
        comment.setAuthorName("comment_author");
        comment = commentRepository.save(comment);

        mockMvc.perform(delete("/api/v1/scenarios/{id}/comments/{commentId}", scenarioId, comment.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("只能删除自己发表的评论"));
    }

    @Test
    void leaveRemovesPresenceAndListingPurgesExpiredRows() throws Exception {
        Long scenarioId = createScenario("SIS-R15-P2");
        mockMvc.perform(post("/api/v1/scenarios/{id}/presence", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":11,\"userName\":\"active_user\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/scenarios/{id}/presence/{userId}", scenarioId, 11))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(post("/api/v1/scenarios/{id}/presence", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":12,\"userName\":\"stale_user\"}"))
                .andExpect(status().isOk());
        jdbcTemplate.update("update scenario_presence set last_seen_at = ? where scenario_id = ? and user_id = ?",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusMinutes(5)), scenarioId, 12L);

        mockMvc.perform(get("/api/v1/scenarios/{id}/presence", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
        org.assertj.core.api.Assertions.assertThat(presenceRepository.findByScenarioIdAndUserId(scenarioId, 12L)).isEmpty();
    }

    @Test
    void sseStreamStartsAsync() throws Exception {
        Long scenarioId = createScenario("SIS-R15-S1");
        String ticketResponse = mockMvc.perform(post("/api/v1/scenarios/{id}/collab/tickets", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String ticket = ticketResponse.replaceAll("(?s).*?\\\"ticket\\\":\\\"([^\\\"]+)\\\".*", "$1");
        mockMvc.perform(get("/api/v1/scenarios/{id}/collab/stream", scenarioId).param("ticket", ticket))
                .andExpect(request().asyncStarted())
                .andReturn();
    }

    @Test
    void sseTicketIsRequiredAndSingleUse() throws Exception {
        Long scenarioId = createScenario("SIS-R15-S2");
        mockMvc.perform(get("/api/v1/scenarios/{id}/collab/stream", scenarioId).param("ticket", "invalid"))
                .andExpect(status().isUnauthorized());
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
