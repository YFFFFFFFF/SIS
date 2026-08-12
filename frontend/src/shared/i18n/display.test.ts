import { describe, expect, it } from 'vitest'
import { approvalStatusName, displayValue, metricName, roleName } from './display'

describe('display mappings', () => {
  it('translates core business codes', () => {
    expect(metricName('NPV')).toBe('净现值')
    expect(approvalStatusName('IN_REVIEW')).toBe('财务复核中')
    expect(roleName('PROJECT_MANAGER')).toBe('项目管理者')
  })

  it('keeps unknown values visible and handles empty values', () => {
    expect(displayValue({}, 'CUSTOM_CODE')).toBe('CUSTOM_CODE')
    expect(displayValue({}, null)).toBe('-')
  })
})
