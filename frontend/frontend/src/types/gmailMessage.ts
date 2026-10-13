export interface GmailMessage {
  id: string
  from: string
  subject: string
  snippet: string
  fullBody?: string
  receivedAt: number | null
  outcome: 'SUCCESS' | 'REJECTION'
  classification?:
    | 'REJECTED'
    | 'OFFER'
    | 'INTERVIEW'
    | 'ONLINE_ASSESSMENT'
    | 'PROGRESSED'
    | 'APPLICATION_RECEIVED'
    | 'PENDING'
    | 'ROLE_CLOSED'
    | 'WITHDRAWN'
    | 'REVIEW_REQUIRED'
    | 'OTHER'
  matchedPhrase?: string | null
}
