/**
 * ECharts 品牌调色板（DESIGN.md token 衍生）。
 * 主色 #005eba 起，向外扩展同色系 + 语义色（成功/警告/危险）。
 * 各图表组件 setOption 时通过 `color: CHART_PALETTE` 统一引用，避免散落硬编码。
 */
export const CHART_PALETTE = [
  '#005eba', // 主色
  '#337ecb', // primary-light-3
  '#669ed8', // primary-light-5
  '#16a34a', // 成功/收益
  '#d97706', // 警告
  '#dc2626', // 危险/亏损
  '#0891b2', // 青（辅助）
  '#7c3aed'  // 紫（辅助）
] as const

/** 盈亏/正负语义色：正=绿，负=红（财务图表惯用）。 */
export const CHART_POSITIVE = '#16a34a'
export const CHART_NEGATIVE = '#dc2626'
/** 中性网格/坐标轴色。 */
export const CHART_AXIS = '#9ca3af'
