import type { JobApplication } from '../types/jobApplication'

export type NewJobApplication = Omit<JobApplication, 'id'>

export async function getApplications(
  signal?: AbortSignal,
): Promise<JobApplication[]> {
  const response = await fetch('/api/applications', { signal })

  if (!response.ok) {
    throw new Error(`Unable to load applications (${response.status})`)
  }

  return response.json() as Promise<JobApplication[]>
}

export async function createApplication(
  application: NewJobApplication,
): Promise<JobApplication> {
  const response = await fetch('/api/applications', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(application),
  })

  if (!response.ok) {
    throw new Error(`Unable to add application (${response.status})`)
  }

  return response.json() as Promise<JobApplication>
}

export async function updateApplication(
  id: number,
  application: NewJobApplication,
): Promise<JobApplication> {
  const response = await fetch(`/api/applications/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(application),
  })

  if (!response.ok) {
    throw new Error(`Unable to update application (${response.status})`)
  }

  return response.json() as Promise<JobApplication>
}

export async function deleteApplication(id: number): Promise<void> {
  const response = await fetch(`/api/applications/${id}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    throw new Error(`Unable to delete application (${response.status})`)
  }
}
