import { API_URL } from '../config'
import type { GmailMessage } from '../types/gmailMessage'

export async function getGmailMessages(
  signal?: AbortSignal,
): Promise<GmailMessage[]> {
  const response = await fetch(`${API_URL}/api/gmail/messages`, {
    signal,
  })

  if (!response.ok) {
    throw new Error(`Unable to load Gmail messages (${response.status})`)
  }

  return response.json() as Promise<GmailMessage[]>
}

export async function getUpdateMessages(
  signal?: AbortSignal,
): Promise<GmailMessage[]> {
  const response = await fetch(`${API_URL}/api/gmail/updates`, {
    signal,
  })

  if (!response.ok) {
    throw new Error(`Unable to load update emails (${response.status})`)
  }

  return response.json() as Promise<GmailMessage[]>
}
