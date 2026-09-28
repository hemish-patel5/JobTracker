export type ApplicationStatus =
  | 'SAVED'
  | 'APPLIED'
  | 'ONLINE_ASSESSMENT'
  | 'INTERVIEW'
  | 'OFFER'
  | 'REJECTED'
  | 'WITHDRAWN'

export interface JobApplication {
  id: number
  company: string
  role: string
  location: string
  status: ApplicationStatus
  dateApplied: string
  jobUrl: string
  notes: string
}
