import type { JobApplication } from '../types/jobApplication'

export async function getApplications(
  signal?: AbortSignal,
): Promise<JobApplication[]> {
  const response = await fetch('/api/applications', { signal })

  if (!response.ok) {
    throw new Error(`Unable to load applications (${response.status})`)
  }

  return response.json() as Promise<JobApplication[]>
}
