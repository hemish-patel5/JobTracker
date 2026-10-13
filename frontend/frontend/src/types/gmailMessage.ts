export interface GmailMessage {
  id: string
  from: string
  subject: string
  snippet: string
  fullBody?: string
  receivedAt: number | null
  outcome: 'SUCCESS' | 'REJECTION'
}
