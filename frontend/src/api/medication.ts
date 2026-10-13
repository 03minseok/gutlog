import { api, unwrap } from './client'
import type { ApiResponse } from './types'

// 백엔드 enum과 같은 값
export type DosageForm =
  | 'TABLET'
  | 'CAPSULE'
  | 'GRANULE'
  | 'ORAL_LIQUID'
  | 'SUPPOSITORY'
  | 'ENEMA'
  | 'SC_INJECTION'
  | 'IV_INJECTION'
export type MedicineCategory =
  | 'AMINOSALICYLATE'
  | 'CORTICOSTEROID'
  | 'IMMUNOMODULATOR'
  | 'BIOLOGIC'
  | 'JAK_INHIBITOR'
  | 'ANTI_TUBERCULOSIS'
  | 'OTHER'
export type ScheduleType = 'DAILY' | 'INTERVAL' | 'AS_NEEDED'

/** 약 마스터 (제품 + 제형 하나) */
export interface Medicine {
  id: number
  /** 성분명 */
  genericName: string
  /** 제품명 */
  brandName: string
  dosageForm: DosageForm
  /** 복용량 단위: 정, 캡슐, 포, mL, 개, 회 */
  doseUnit: string
  category: MedicineCategory
  /** 비활성 약은 검색·새 등록에서 빠지지만, 이미 복용 중인 사용자에게는 보인다. */
  active: boolean
}

/** 내 복용 약 */
export interface UserMedicine {
  id: number
  medicine: Medicine
  scheduleType: ScheduleType
  /** DAILY만 (1~6) */
  dailyDoseCount: number | null
  /** INTERVAL만 (2~180일) */
  intervalDays: number | null
  /** 1회 복용량. 단위는 medicine.doseUnit */
  doseAmount: number | null
  doseMemo: string | null
}

/**
 * 복용 약 등록·수정 요청. 수정은 전체 교체이고 medicineId는 보내지 않는다.
 * 복용 방식에 맞지 않는 값(예: AS_NEEDED에 dailyDoseCount)은 서버가 400으로 거절하므로 null로 보낸다.
 */
export interface UserMedicineRequest {
  scheduleType: ScheduleType
  dailyDoseCount: number | null
  intervalDays: number | null
  doseAmount: number | null
  doseMemo: string | null
}

export interface DailyMedicationItem {
  userMedicineId: number
  medicine: Medicine
  scheduleType: ScheduleType
  dailyDoseCount: number | null
  intervalDays: number | null
  doseAmount: number | null
  doseMemo: string | null
  /** 지금 끊은 상태. 체크·취소할 수 없다. */
  stopped: boolean
  /** 그날 체크한 회차. 하루 복용 횟수를 줄였다면 범위 밖 회차도 들어 있다. */
  takenDoses: number[]
  /** INTERVAL만: 조회 날짜까지의 마지막 투여일 / 다음 예정일 ('yyyy-MM-dd') */
  lastTakenDate: string | null
  nextDueDate: string | null
}

export interface DailyMedication {
  date: string
  /** 홈 "오늘 복약 n/m": 그날 복용 중이던 DAILY 약의 회차만 센다. */
  dailyTakenCount: number
  dailyTotalCount: number
  /** 그날 복용 중이던 약 + 그날 기록이 있는 끊은 약 */
  medicines: DailyMedicationItem[]
}

export interface MedicationToggleResult {
  userMedicineId: number
  takenDate: string
  doseNumber: number
  /** 바뀐 뒤의 상태 */
  taken: boolean
}

/** 관리자 약 추가·수정 요청 */
export interface MedicineRequest {
  genericName: string
  brandName: string
  dosageForm: DosageForm
  category: MedicineCategory
}

export const medicationApi = {
  /** 활성 약 검색. keyword가 없으면 활성 약 전체 */
  searchMedicines: (keyword?: string) =>
    unwrap(api.get<ApiResponse<Medicine[]>>('/medicines', { params: keyword ? { keyword } : undefined })),

  listMyMedicines: () => unwrap(api.get<ApiResponse<UserMedicine[]>>('/my-medicines')),

  createMyMedicine: (body: UserMedicineRequest & { medicineId: number }) =>
    unwrap(api.post<ApiResponse<UserMedicine>>('/my-medicines', body)),

  updateMyMedicine: (id: number, body: UserMedicineRequest) =>
    unwrap(api.put<ApiResponse<UserMedicine>>(`/my-medicines/${id}`, body)),

  /** 복용 중단 (소프트 딜리트). 지난 복용 기록은 남는다. */
  removeMyMedicine: (id: number) => unwrap(api.delete<ApiResponse<null>>(`/my-medicines/${id}`)),

  daily: (date: string) => unwrap(api.get<ApiResponse<DailyMedication>>('/record/medication/daily', { params: { date } })),

  /**
   * 체크 토글. 서버가 지금 상태를 보고 체크·취소를 정한다.
   * n회차 체크는 n-1회차가 체크돼 있어야 하고, 취소는 n+1회차가 체크돼 있지 않아야 한다.
   */
  toggle: (userMedicineId: number, date: string, doseNumber: number) =>
    unwrap(api.post<ApiResponse<MedicationToggleResult>>(`/record/medication/${userMedicineId}/${date}/${doseNumber}/toggle`)),
}

/** /api/admin/** — ADMIN만. 일반 사용자는 403 */
export const adminMedicineApi = {
  /** 비활성 포함 전체 */
  list: () => unwrap(api.get<ApiResponse<Medicine[]>>('/admin/medicines')),

  create: (body: MedicineRequest) => unwrap(api.post<ApiResponse<Medicine>>('/admin/medicines', body)),

  update: (id: number, body: MedicineRequest) => unwrap(api.put<ApiResponse<Medicine>>(`/admin/medicines/${id}`, body)),

  deactivate: (id: number) => unwrap(api.patch<ApiResponse<Medicine>>(`/admin/medicines/${id}/deactivate`)),

  activate: (id: number) => unwrap(api.patch<ApiResponse<Medicine>>(`/admin/medicines/${id}/activate`)),
}
