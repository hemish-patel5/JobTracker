import { useCallback, useEffect, useState } from 'react'
import { getApplications } from './api/applications'
import type { ApplicationStatus, JobApplication } from './types/jobApplication'
import './App.css'

const statusLabels: Record<ApplicationStatus, string> = {
  SAVED: 'Saved',
  APPLIED: 'Applied',
  ONLINE_ASSESSMENT: 'Online assessment',
  INTERVIEW: 'Interview',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
}

function formatDate(date: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(new Date(`${date}T00:00:00`))
}

function App() {
  const [applications, setApplications] = useState<JobApplication[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadApplications = useCallback(async (signal?: AbortSignal) => {
    setIsLoading(true)
    setError(null)

    try {
      setApplications(await getApplications(signal))
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') {
        return
      }

      setError(
        requestError instanceof Error
          ? requestError.message
          : 'Unable to load applications',
      )
    } finally {
      if (!signal?.aborted) {
        setIsLoading(false)
      }
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    void loadApplications(controller.signal)

    return () => controller.abort()
  }, [loadApplications])

  return (
    <main className="app-shell">
      <header className="page-header">
        <div>
          <p className="eyebrow">Job Tracker</p>
          <h1>Applications</h1>
          <p className="subtitle">Keep every opportunity in one clear view.</p>
        </div>
        <div className="application-count" aria-label="Application count">
          <strong>{applications.length}</strong>
          <span>{applications.length === 1 ? 'application' : 'applications'}</span>
        </div>
      </header>

      {isLoading && <p className="message">Loading applications…</p>}

      {error && (
        <div className="message error-message" role="alert">
          <span>{error}</span>
          <button type="button" onClick={() => void loadApplications()}>
            Try again
          </button>
        </div>
      )}

      {!isLoading && !error && applications.length === 0 && (
        <div className="message empty-state">
          <h2>No applications yet</h2>
          <p>Add an application through the API and it will appear here.</p>
        </div>
      )}

      {!isLoading && !error && applications.length > 0 && (
        <section className="application-grid" aria-label="Job applications">
          {applications.map((application) => (
            <article className="application-card" key={application.id}>
              <div className="card-heading">
                <div>
                  <p className="company">{application.company}</p>
                  <h2>{application.role}</h2>
                </div>
                <span
                  className={`status status-${application.status.toLowerCase()}`}
                >
                  {statusLabels[application.status]}
                </span>
              </div>

              <dl className="application-details">
                <div>
                  <dt>Location</dt>
                  <dd>{application.location || 'Not specified'}</dd>
                </div>
                <div>
                  <dt>Applied</dt>
                  <dd>{formatDate(application.dateApplied)}</dd>
                </div>
              </dl>

              {application.notes && <p className="notes">{application.notes}</p>}

              {application.jobUrl && (
                <a
                  className="job-link"
                  href={application.jobUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  View job posting <span aria-hidden="true">↗</span>
                </a>
              )}
            </article>
          ))}
        </section>
      )}
    </main>
  )
}

export default App
