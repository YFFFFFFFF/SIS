package com.sis.iids.approval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class ApprovalApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApprovalInstanceRepository approvalInstanceRepository;

    @Autowired
    private ApprovalRecordRepository approvalRecordRepository;

    @Test
    void submitScenarioCreatesApprovalInstanceAtReviewNode() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-APP-SUBMIT"));

        String response = submit(scenarioId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.scenarioId").value(scenarioId))
                .andExpect(jsonPath("$.data.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.data.currentNode").value("REVIEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long instanceId = extractId(response);

        ApprovalInstance instance = approvalInstanceRepository.findById(instanceId).orElseThrow();
        assertThat(instance.getStatus()).isEqualTo(ApprovalStatus.IN_REVIEW);
        assertThat(instance.getCurrentNode()).isEqualTo("REVIEW");
    }

    @Test
    void reviewerApprovesAndMovesToApprovalNode() throws Exception {
        Long instanceId = extractId(submit(createScenario(createProject("SIS-M1-APP-REVIEW")))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/approval-instances/{instanceId}/review/approve", instanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"review ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_APPROVAL"))
                .andExpect(jsonPath("$.data.currentNode").value("APPROVAL"));

        ApprovalInstance instance = approvalInstanceRepository.findById(instanceId).orElseThrow();
        assertThat(instance.getStatus()).isEqualTo(ApprovalStatus.IN_APPROVAL);
        List<ApprovalRecord> records = approvalRecordRepository.findByInstanceIdOrderByOperatedAtAsc(instanceId);
        assertThat(records).anySatisfy(record -> {
            assertThat(record.getNodeCode()).isEqualTo("REVIEW");
            assertThat(record.getDecision()).isEqualTo(ApprovalDecision.APPROVE);
            assertThat(record.getCommentText()).isEqualTo("review ok");
        });
    }

    @Test
    void approverApprovesAndInstanceBecomesApproved() throws Exception {
        Long instanceId = extractId(submit(createScenario(createProject("SIS-M1-APP-APPROVE")))
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(post("/api/v1/approval-instances/{instanceId}/review/approve", instanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"review ok\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/approval-instances/{instanceId}/approve", instanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.currentNode").value("APPROVED"));

        ApprovalInstance instance = approvalInstanceRepository.findById(instanceId).orElseThrow();
        assertThat(instance.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void rejectRecordsDecisionAndTerminalStatus() throws Exception {
        Long instanceId = extractId(submit(createScenario(createProject("SIS-M1-APP-REJECT")))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/approval-instances/{instanceId}/reject", instanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"insufficient data\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.currentNode").value("REJECTED"));

        List<ApprovalRecord> records = approvalRecordRepository.findByInstanceIdOrderByOperatedAtAsc(instanceId);
        assertThat(records).anySatisfy(record -> {
            assertThat(record.getDecision()).isEqualTo(ApprovalDecision.REJECT);
            assertThat(record.getCommentText()).isEqualTo("insufficient data");
        });
    }

    private org.springframework.test.web.servlet.ResultActions submit(Long scenarioId) throws Exception {
        return mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/approval/submit", scenarioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"submit for approval\"}"));
    }

    private Long createProject(String code) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"name\":\"Approval Host Project\",\"projectType\":\"INDUSTRIAL\"}".formatted(code)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private Long createScenario(Long projectId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Approval Scenario\",\"horizonYears\":5,\"constructionYears\":1}"))
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