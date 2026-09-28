import type { GmailMessage } from '../types/gmailMessage'

export async function getGmailMessages(
  signal?: AbortSignal,
): Promise<GmailMessage[]> {
  const response = await fetch('http://localhost:8080/api/gmail/messages', {
    signal,
  })

  if (!response.ok) {
    throw new Error(`Unable to load Gmail messages (${response.status})`)
  }

  return response.json() as Promise<GmailMessage[]>
}
