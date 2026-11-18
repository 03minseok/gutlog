export const WEIGHT_PATH = '/mypage/weight'

/** 추이 기간 (오늘 포함 n일) */
export const WEIGHT_PERIODS = [
  { value: '7', label: '7일' },
  { value: '30', label: '30일' },
  { value: '90', label: '90일' },
] as const

export type WeightPeriod = (typeof WEIGHT_PERIODS)[number]['value']

export const DEFAULT_WEIGHT_PERIOD: WeightPeriod = '30'
