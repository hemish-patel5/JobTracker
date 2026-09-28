import { useCallback, useEffect, useMemo, useState } from 'react'
import { getApplications } from './api/applications'
import type { ApplicationStatus, JobApplication } from './types/jobApplication'
import './App.css'

const statusLabels: Record<ApplicationStatus, string> = {
  SAVED: 'SAVED',
  APPLIED: 'APPLIED',
  ONLINE_ASSESSMENT: 'ONLINE ASSESSMENT',
  INTERVIEW: 'INTERVIEW',
  OFFER: 'OFFER',
  REJECTED: 'REJECTED',
  WITHDRAWN: 'WITHDRAWN',
}

const statuses = Object.keys(statusLabels) as ApplicationStatus[]

function formatDate(date: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    day: 'numeric',
    month: 'short',
  }).format(new Date(`${date}T00:00:00`))
}

function App() {
  const [applications, setApplications] = useState<JobApplication[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<ApplicationStatus | 'ALL'>('ALL')

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

  const filteredApplications = useMemo(() => {
    const term = search.trim().toLowerCase()

    return applications.filter((application) => {
      const matchesStatus = status === 'ALL' || application.status === status
      const matchesSearch =
        !term ||
        [
          application.company,
          application.role,
          application.location,
          application.notes,
        ].some((value) => value.toLowerCase().includes(term))

      return matchesStatus && matchesSearch
    })
  }, [applications, search, status])

  const countByStatus = (applicationStatus: ApplicationStatus) =>
    applications.filter((application) => application.status === applicationStatus)
      .length

  return (
    <main className="tracker">
      <header className="tracker-header">
        <h1>JobTracker</h1>
        <button type="button">+ Add Application</button>
      </header>

      <section className="summary" aria-label="Application summary">
        <span>{applications.length} Total</span>
        <span>{countByStatus('APPLIED')} Applied</span>
        <span>{countByStatus('INTERVIEW')} Interview</span>
        <span>{countByStatus('OFFER')} Offer</span>
      </section>

      <section className="filters" aria-label="Application filters">
        <label>
          <span className="sr-only">Search applications</span>
          <input
            type="search"
            placeholder="Search applications..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </label>
        <label className="status-filter">
          <span>Status:</span>
          <select
            value={status}
            onChange={(event) =>
              setStatus(event.target.value as ApplicationStatus | 'ALL')
            }
          >
            <option value="ALL">[ All ▼ ]</option>
            {statuses.map((applicationStatus) => (
              <option value={applicationStatus} key={applicationStatus}>
                {statusLabels[applicationStatus]}
              </option>
            ))}
          </select>
        </label>
      </section>

      {isLoading && <p className="message">Loading applications…</p>}

      {error && (
        <div className="message error-message" role="alert">
          <span>{error}</span>
          <button type="button" onClick={() => void loadApplications()}>
            Try again
          </button>
        </div>
      )}

      {!isLoading && !error && filteredApplications.length === 0 && (
        <p className="message">No applications found.</p>
      )}

      {!isLoading && !error && filteredApplications.length > 0 && (
        <section className="application-list" aria-label="Job applications">
          {filteredApplications.map((application) => (
            <article className="application-card" key={application.id}>
              <p>{application.company}</p>
              <p>{application.role}</p>
              <div className="application-meta">
                <span>{application.location || 'Not specified'}</span>
                <span>{statusLabels[application.status]}</span>
              </div>
              <p>Applied {formatDate(application.dateApplied)}</p>
            </article>
          ))}
        </section>
      )}
    </main>
  )
}

export default App
