package com.sis.iids.collaboration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class EditLockApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EditLockRepository editLockRepository;

    @Test
    void userCanAcquireLockForScenario() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-LOCK-ACQUIRE"));

        mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/lock", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockRequest(101, "Analyst A", 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.scenarioId").value(scenarioId))
                .andExpect(jsonPath("$.data.holderId").value(101))
                .andExpect(jsonPath("$.data.holderName").value("Analyst A"))
                .andExpect(jsonPath("$.data.expireAt", notNullValue()));

        EditLock lock = editLockRepository.findByScenarioId(scenarioId).orElseThrow();
        assertThat(lock.getHolderId()).isEqualTo(101L);
        assertThat(lock.getHolderName()).isEqualTo("Analyst A");
        assertThat(lock.getExpireAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void secondUserCannotAcquireActiveLock() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-LOCK-CONFLICT"));
        acquire(scenarioId, 101, "Analyst A", 30);

        mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/lock", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockRequest(202, "Analyst B", 30)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("测算方案正在被编辑，锁持有人：Analyst A"));
    }

    @Test
    void holderCanReleaseLock() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-LOCK-RELEASE"));
        acquire(scenarioId, 101, "Analyst A", 30);

        mockMvc.perform(delete("/api/v1/scenarios/{scenarioId}/lock", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"holderId\":101}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarioId").value(scenarioId))
                .andExpect(jsonPath("$.data.released").value(true));

        assertThat(editLockRepository.findByScenarioId(scenarioId)).isEmpty();
    }

    @Test
    void expiredLockCanBeReplaced() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-LOCK-EXPIRED"));
        EditLock expired = new EditLock();
        expired.setScenarioId(scenarioId);
        expired.setHolderId(101L);
        expired.setHolderName("Analyst A");
        expired.setExpireAt(LocalDateTime.now().minusMinutes(1));
        editLockRepository.save(expired);

        mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/lock", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockRequest(202, "Analyst B", 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.holderId").value(202))
                .andExpect(jsonPath("$.data.holderName").value("Analyst B"));

        EditLock lock = editLockRepository.findByScenarioId(scenarioId).orElseThrow();
        assertThat(lock.getHolderId()).isEqualTo(202L);
        assertThat(lock.getHolderName()).isEqualTo("Analyst B");
    }

    private void acquire(Long scenarioId, long holderId, String holderName, int ttlMinutes) throws Exception {
        mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/lock", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockRequest(holderId, holderName, ttlMinutes)))
                .andExpect(status().isOk());
    }

    private String lockRequest(long holderId, String holderName, int ttlMinutes) {
        return "{\"holderId\":%d,\"holderName\":\"%s\",\"ttlMinutes\":%d}"
                .formatted(holderId, holderName, ttlMinutes);
    }

    private Long createProject(String code) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"name\":\"Lock Host Project\",\"projectType\":\"INDUSTRIAL\"}".formatted(code)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private Long createScenario(Long projectId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lock Scenario\",\"horizonYears\":5,\"constructionYears\":1}"))
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