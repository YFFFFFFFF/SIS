package com.sis.iids.calculation;

import com.sis.iids.worker.CalculationWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-02 收尾：设计 §8.2 新增/变更 API 的集成测试。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class CalculationExtendedApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalculationWorker calculationWorker;

    @Test
    void investmentAndCostItemCrudAndSummary() throws Exception {
        Long scenarioId = createScenarioWithParams();

        // 投资分项：新增（含扩展字段）→ 列表 → 修改 → 汇总 → 删除
        String itemResp = mockMvc.perform(post("/api/v1/scenarios/{id}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CONSTRUCTION_BUILDING\",\"name\":\"建筑工程费\",\"amount\":120000,\"yearNo\":0,\"itemCode\":\"C-01\",\"sortOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCode").value("C-01"))
                .andReturn().getResponse().getContentAsString();
        Long itemId = extractId(itemResp);

        mockMvc.perform(post("/api/v1/scenarios/{id}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CONSTRUCTION\",\"name\":\"设备购置及安装费\",\"amount\":80000,\"yearNo\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"WORKING_CAPITAL\",\"name\":\"流动资金\",\"amount\":20000,\"yearNo\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/scenarios/{id}/investment-items", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));

        mockMvc.perform(put("/api/v1/scenarios/{id}/investment-items/{itemId}", scenarioId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CONSTRUCTION_BUILDING\",\"name\":\"建筑工程费(调整)\",\"amount\":125000,\"yearNo\":0,\"itemCode\":\"C-01\",\"sortOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("建筑工程费(调整)"))
                .andExpect(jsonPath("$.data.amount").value(125000.0000));

        mockMvc.perform(get("/api/v1/scenarios/{id}/investment-summary", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.constructionTotal").value(205000.0000))
                .andExpect(jsonPath("$.data.workingCapital").value(20000.0000))
                .andExpect(jsonPath("$.data.totalInvestment").value(225000.0000))
                .andExpect(jsonPath("$.data.balanced").value(true));

        mockMvc.perform(delete("/api/v1/scenarios/{id}/investment-items/{itemId}", scenarioId, itemId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/scenarios/{id}/investment-items", scenarioId))
                .andExpect(jsonPath("$.data", hasSize(2)));

        // 成本分项 CRUD
        String costResp = mockMvc.perform(post("/api/v1/scenarios/{id}/cost-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"RAW_MATERIAL\",\"name\":\"外购原材料及燃料动力\",\"yearNo\":0,\"amount\":50000}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long costId = extractId(costResp);
        mockMvc.perform(get("/api/v1/scenarios/{id}/cost-items", scenarioId))
                .andExpect(jsonPath("$.data", hasSize(1)));
        mockMvc.perform(put("/api/v1/scenarios/{id}/cost-items/{itemId}", scenarioId, costId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"RAW_MATERIAL\",\"name\":\"外购原材料\",\"yearNo\":0,\"amount\":52000}"))
                .andExpect(jsonPath("$.data.amount").value(52000.0000));
        mockMvc.perform(delete("/api/v1/scenarios/{id}/cost-items/{itemId}", scenarioId, costId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/scenarios/{id}/cost-items", scenarioId))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void extendedParameterFieldsRoundTrip() throws Exception {
        Long scenarioId = createScenarioWithParams();
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "wacc": 0.10, "taxRate": 0.25, "depreciationYears": 6, "residualRate": 0.04,
                                  "loanRatioLimit": 0.70, "pricePerUnit": 500, "unitCost": 100, "annualOutput": 5,
                                  "fixedOperatingCost": 200, "formulaVersion": "fin-std-2.0.0",
                                  "depreciationPolicy": "DOUBLE_DECLINING", "amortizationYears": 0, "amortizableAmount": 0,
                                  "repaymentMethod": "EQUAL_PRINCIPAL",
                                  "taxSchedule": "[{\\"fromYear\\":1,\\"toYear\\":3,\\"rate\\":0}]",
                                  "rampUp": "[{\\"year\\":1,\\"loadFactor\\":0.6}]"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.depreciationPolicy").value("DOUBLE_DECLINING"))
                .andExpect(jsonPath("$.data.repaymentMethod").value("EQUAL_PRINCIPAL"));

        mockMvc.perform(get("/api/v1/scenarios/{id}/parameters", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.depreciationPolicy").value("DOUBLE_DECLINING"))
                .andExpect(jsonPath("$.data.taxSchedule").value("[{\"fromYear\":1,\"toYear\":3,\"rate\":0}]"))
                .andExpect(jsonPath("$.data.rampUp").value("[{\"year\":1,\"loadFactor\":0.6}]"));
    }

    @Test
    void statementsProfitFlowAndLoanScheduleAreDerived() throws Exception {
        Long scenarioId = createLoanScenario();

        String taskResp = mockMvc.perform(post("/api/v1/scenarios/{id}/calculation-tasks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskType\":\"FINANCIAL\",\"requestKey\":\"extended-001\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long taskId = extractNestedTaskId(taskResp);
        calculationWorker.runPendingOnce();

        // 三类报表（默认全返 = 3 表 × 8 期 = 24 行）
        mockMvc.perform(get("/api/v1/calculation-tasks/{id}/statements", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(24)));
        // 按类型过滤
        mockMvc.perform(get("/api/v1/calculation-tasks/{id}/statements?type=EQUITY_CASH_FLOW", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(8)))
                .andExpect(jsonPath("$.data[0].statementType").value("EQUITY_CASH_FLOW"));

        // 利润流向分解：8 个节点
        mockMvc.perform(get("/api/v1/calculation-tasks/{id}/profit-flow", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(8)))
                .andExpect(jsonPath("$.data[*].key", containsInAnyOrder(
                        "REVENUE", "OPERATING_TAX_SURTAX", "OPERATING_COST", "DEPRECIATION_AMORTIZATION",
                        "FINANCE_COST", "PROFIT_BEFORE_TAX", "INCOME_TAX", "NET_PROFIT")));

        // 还本付息计划：等额本金，利息随本金递减
        mockMvc.perform(get("/api/v1/calculation-tasks/{id}/loan-schedule", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].openingBalance").exists());
    }

    private Long createScenarioWithParams() throws Exception {
        Long projectId = createProject("SIS-R02-EXT");
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"扩展 API 方案\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long scenarioId = extractId(scenarioResp);
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"wacc":0.10,"taxRate":0.25,"depreciationYears":5,"residualRate":0,"loanRatioLimit":0.70,
                                 "pricePerUnit":140,"unitCost":40,"annualOutput":1000,"fixedOperatingCost":10000}
                                """))
                .andExpect(status().isOk());
        return scenarioId;
    }

    private Long createLoanScenario() throws Exception {
        Long projectId = createProject("SIS-R02-LOAN");
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"贷款方案\",\"horizonYears\":6,\"constructionYears\":2}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long scenarioId = extractId(scenarioResp);
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"wacc":0.10,"taxRate":0.25,"depreciationYears":10,"residualRate":0.05,"loanRatioLimit":0.70,
                                 "pricePerUnit":500,"unitCost":150,"annualOutput":5,"fixedOperatingCost":300,
                                 "repaymentMethod":"EQUAL_PRINCIPAL"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CONSTRUCTION\",\"name\":\"建设投资\",\"amount\":2000000,\"yearNo\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"WORKING_CAPITAL\",\"name\":\"流动资金\",\"amount\":200000,\"yearNo\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/financing-plans", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"EQUITY\",\"ratio\":0.4,\"amount\":880000,\"interestRate\":0,\"termYears\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/financing-plans", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"LOAN\",\"ratio\":0.6,\"amount\":1320000,\"interestRate\":0.08,\"termYears\":5,\"repaymentMethod\":\"EQUAL_PRINCIPAL\",\"graceYears\":1}"))
                .andExpect(status().isOk());
        return scenarioId;
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
        return Long.valueOf(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }

    private Long extractNestedTaskId(String response) {
        return Long.valueOf(response.replaceAll(".*\\\"task\\\":\\{\\\"id\\\":(\\d+).*", "$1"));
    }
}
