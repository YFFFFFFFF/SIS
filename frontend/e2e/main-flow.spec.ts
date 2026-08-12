import { expect, test, type APIRequestContext } from '@playwright/test'

test('用户可以完成项目、方案、测算和报告主链路', async ({ page, request }) => {
  const code = `E2E-${Date.now()}`
  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill('investment_analyst')
  await page.getByPlaceholder('请输入密码').fill('Password123!')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/dashboard/)

  await page.getByText('项目管理', { exact: true }).click()
  await page.getByLabel('项目编码').fill(code)
  await page.getByLabel('项目名称').fill('E2E 主流程项目')
  await page.getByRole('button', { name: '保存项目' }).click()
  await expect(page.getByText(code, { exact: true })).toBeVisible()

  await page.reload()
  await expect(page.getByText(code, { exact: true })).toBeVisible()

  await page.getByText(code, { exact: true }).click()
  const token = await page.evaluate(() => localStorage.getItem('iids.auth.token'))
  expect(token).toBeTruthy()

  const projects = await api<Array<{ id: number; code: string }>>(request, 'GET', '/projects', token!)
  const project = projects.find(item => item.code === code)
  expect(project).toBeTruthy()

  const scenario = await api<{ id: number; name: string }>(request, 'POST', `/projects/${project!.id}/scenarios`, token!, {
    name: 'E2E 基准方案', horizonYears: 5, constructionYears: 1, remarks: 'Playwright main flow'
  })
  await api(request, 'PUT', `/scenarios/${scenario.id}/parameters`, token!, {
    wacc: 0.1, taxRate: 0.25, depreciationYears: 5, residualRate: 0, loanRatioLimit: 0.7,
    pricePerUnit: 140, unitCost: 40, annualOutput: 1000, fixedOperatingCost: 10000
  })
  await api(request, 'POST', `/scenarios/${scenario.id}/investment-items`, token!, {
    category: 'CONSTRUCTION', name: '建设投资', amount: 200000, yearNo: 0, sortOrder: 1
  })
  await api(request, 'POST', `/scenarios/${scenario.id}/investment-items`, token!, {
    category: 'WORKING_CAPITAL', name: '流动资金', amount: 20000, yearNo: 1, sortOrder: 2
  })
  await api(request, 'POST', `/scenarios/${scenario.id}/financing-plans`, token!, {
    sourceType: 'EQUITY', ratio: 1, amount: 220000, interestRate: 0, termYears: 0,
    repaymentMethod: 'EQUAL_PRINCIPAL', graceYears: 0
  })

  const run = await api<{ task: { id: number } }>(request, 'POST', `/scenarios/${scenario.id}/calculation-tasks`, token!, {
    taskType: 'FULL', requestKey: `e2e-${scenario.id}`
  })
  let taskStatus = ''
  for (let attempt = 0; attempt < 60; attempt++) {
    const task = await api<{ status: string; errorMessage?: string }>(request, 'GET', `/calculation-tasks/${run.task.id}`, token!)
    taskStatus = task.status
    if (taskStatus === 'SUCCESS' || taskStatus === 'FAILED') break
    await page.waitForTimeout(500)
  }
  expect(taskStatus).toBe('SUCCESS')

  const results = await api<{ metrics: Record<string, number>; cashFlowRows: unknown[] }>(
    request, 'GET', `/calculation-tasks/${run.task.id}/results`, token!
  )
  expect(results.metrics.NPV).toBeDefined()
  expect(results.cashFlowRows.length).toBeGreaterThan(0)

  const report = await api<{ id: number; fileType: string }>(
    request, 'POST', `/calculation-tasks/${run.task.id}/reports?format=EXCEL`, token!
  )
  expect(report.fileType).toBe('EXCEL')
  const download = await request.get(`http://127.0.0.1:5173/api/v1/reports/${report.id}/download`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(download.ok()).toBeTruthy()
  expect(download.headers()['content-type']).toContain('spreadsheetml')
  expect((await download.body()).length).toBeGreaterThan(1000)

  await page.getByText('测算方案', { exact: true }).click()
  await page.getByRole('button', { name: '刷新' }).click()
  await expect(page.getByText('E2E 基准方案', { exact: true })).toBeVisible()
})

async function api<T>(
  request: APIRequestContext,
  method: 'GET' | 'POST' | 'PUT',
  path: string,
  token: string,
  data?: unknown
): Promise<T> {
  const response = await request.fetch(`http://127.0.0.1:5173/api/v1${path}`, {
    method,
    headers: { Authorization: `Bearer ${token}` },
    data
  })
  const text = await response.text()
  expect(response.ok(), text).toBeTruthy()
  const body = JSON.parse(text) as { code: string; message: string; data: T }
  expect(body.code, body.message).toBe('SUCCESS')
  return body.data
}
