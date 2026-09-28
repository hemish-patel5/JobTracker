import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  createApplication,
  deleteApplication,
  getApplications,
  type NewJobApplication,
  updateApplication,
} from './api/applications'
import { getGmailMessages } from './api/gmail'
import type { ApplicationStatus, JobApplication } from './types/jobApplication'
import type { GmailMessage } from './types/gmailMessage'
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

const emptyApplication: NewJobApplication = {
  company: '',
  role: '',
  location: '',
  status: 'APPLIED',
  dateApplied: new Date().toLocaleDateString('en-CA'),
  jobUrl: '',
  notes: '',
}

function formatDate(date: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    day: 'numeric',
    month: 'short',
  }).format(new Date(`${date}T00:00:00`))
}

function connectGmail() {
  window.location.href = 'http://localhost:8080/api/gmail/connect'
}

async function disconnectGmail() {
  const response = await fetch(
    'http://localhost:8080/api/gmail/disconnect',
    { method: 'DELETE' },
  )

  if (!response.ok) {
    throw new Error(`Unable to log out of Gmail (${response.status})`)
  }
}

async function isGmailConnected(signal?: AbortSignal) {
  const response = await fetch('http://localhost:8080/api/gmail/status', {
    signal,
  })

  if (!response.ok) {
    throw new Error(`Unable to check Gmail status (${response.status})`)
  }

  const result = (await response.json()) as { connected: boolean }
  return result.connected
}

function App() {
  const [activePage, setActivePage] = useState<'applications' | 'emails'>(
    'applications',
  )
  const [applications, setApplications] = useState<JobApplication[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<ApplicationStatus | 'ALL'>('ALL')
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editingApplicationId, setEditingApplicationId] = useState<number | null>(
    null,
  )
  const [newApplication, setNewApplication] =
    useState<NewJobApplication>(emptyApplication)
  const [isSaving, setIsSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [gmailConnected, setGmailConnected] = useState(false)
  const [gmailMessages, setGmailMessages] = useState<GmailMessage[]>([])
  const [gmailLoading, setGmailLoading] = useState(true)
  const [gmailError, setGmailError] = useState<string | null>(null)

  const loadApplications = useCallback(async (signal?: AbortSignal) => {
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

  const retryLoadingApplications = () => {
    setIsLoading(true)
    setError(null)
    void loadApplications()
  }

  const loadGmailMessages = useCallback(async (signal?: AbortSignal) => {
    setGmailLoading(true)
    setGmailError(null)

    try {
      setGmailMessages(await getGmailMessages(signal))
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') {
        return
      }

      setGmailError(
        requestError instanceof Error
          ? requestError.message
          : 'Unable to load Gmail messages',
      )
    } finally {
      if (!signal?.aborted) {
        setGmailLoading(false)
      }
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    // Fetching API data is the external synchronization performed by this effect.
    // oxlint-disable-next-line react/set-state-in-effect
    void loadApplications(controller.signal)

    return () => controller.abort()
  }, [loadApplications])

  useEffect(() => {
    const controller = new AbortController()

    void isGmailConnected(controller.signal)
      .then(setGmailConnected)
      .catch((requestError: unknown) => {
        if (
          requestError instanceof DOMException &&
          requestError.name === 'AbortError'
        ) {
          return
        }

        console.error(requestError)
      })

    return () => controller.abort()
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    // Gmail synchronization is the external operation performed by this effect.
    // oxlint-disable-next-line react/set-state-in-effect
    void loadGmailMessages(controller.signal)

    return () => controller.abort()
  }, [loadGmailMessages])

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

  const updateField = <K extends keyof NewJobApplication>(
    field: K,
    value: NewJobApplication[K],
  ) => {
    setNewApplication((current) => ({ ...current, [field]: value }))
  }

  const closeForm = () => {
    if (isSaving) return
    setIsFormOpen(false)
    setFormError(null)
    setEditingApplicationId(null)
    setNewApplication(emptyApplication)
  }

  const openAddForm = () => {
    setEditingApplicationId(null)
    setNewApplication(emptyApplication)
    setFormError(null)
    setIsFormOpen(true)
  }

  const openEditForm = (application: JobApplication) => {
    setEditingApplicationId(application.id)
    setNewApplication({
      company: application.company,
      role: application.role,
      location: application.location,
      status: application.status,
      dateApplied: application.dateApplied,
      jobUrl: application.jobUrl,
      notes: application.notes,
    })
    setFormError(null)
    setIsFormOpen(true)
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setIsSaving(true)
    setFormError(null)

    try {
      if (editingApplicationId === null) {
        const createdApplication = await createApplication(newApplication)
        setApplications((current) => [...current, createdApplication])
      } else {
        const updatedApplication = await updateApplication(
          editingApplicationId,
          newApplication,
        )
        setApplications((current) =>
          current.map((application) =>
            application.id === editingApplicationId
              ? updatedApplication
              : application,
          ),
        )
      }

      setNewApplication(emptyApplication)
      setEditingApplicationId(null)
      setIsFormOpen(false)
    } catch (requestError) {
      setFormError(
        requestError instanceof Error
          ? requestError.message
          : 'Unable to add application',
      )
    } finally {
      setIsSaving(false)
    }
  }

  const handleDelete = async (application: JobApplication) => {
    const confirmed = window.confirm(
      `Delete ${application.company} - ${application.role}?`,
    )

    if (!confirmed) return

    try {
      await deleteApplication(application.id)
      setApplications((current) =>
        current.filter((item) => item.id !== application.id),
      )
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : 'Unable to delete application',
      )
    }
  }

  const handleGmailLogout = async () => {
    try {
      await disconnectGmail()
      setGmailConnected(false)
      window.alert('Gmail disconnected')
    } catch (requestError) {
      window.alert(
        requestError instanceof Error
          ? requestError.message
          : 'Unable to log out of Gmail',
      )
    }
  }

  return (
    <main className="tracker">
      <header className="tracker-header">
        <div className="brand-navigation">
          <h1>BetterTracker</h1>
          <nav className="page-navigation" aria-label="Main navigation">
            <button
              type="button"
              className={activePage === 'applications' ? 'active' : undefined}
              aria-pressed={activePage === 'applications'}
              onClick={() => setActivePage('applications')}
            >
              Applications
            </button>
            <button
              type="button"
              className={activePage === 'emails' ? 'active' : undefined}
              aria-pressed={activePage === 'emails'}
              onClick={() => setActivePage('emails')}
            >
              Job Emails
            </button>
          </nav>
        </div>
        <div className="header-actions">
          {activePage === 'applications' && (
            <button type="button" onClick={openAddForm}>
              + Add Application
            </button>
          )}
          <div className="gmail-actions">
            <button
              type="button"
              onClick={connectGmail}
              disabled={gmailConnected}
              title={gmailConnected ? 'Gmail is already connected' : undefined}
            >
              Log In
            </button>
            <button type="button" onClick={() => void handleGmailLogout()}>
              Log Out
            </button>
          </div>
        </div>
      </header>

      {activePage === 'applications' && (
        <>
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

          {isLoading && <p className="message">Loading applications...</p>}

          {error && (
            <div className="message error-message" role="alert">
              <span>{error}</span>
              <button type="button" onClick={retryLoadingApplications}>
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
                  <div className="application-actions">
                    <button type="button" onClick={() => openEditForm(application)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      onClick={() => void handleDelete(application)}
                    >
                      Delete
                    </button>
                  </div>
                </article>
              ))}
            </section>
          )}
        </>
      )}

      {activePage === 'emails' && (
        <section className="gmail-message-section" aria-label="Gmail messages">
          <div className="section-heading">
            <h2>Job Emails</h2>
            <button type="button" onClick={() => void loadGmailMessages()}>
              Refresh
            </button>
          </div>

          {gmailLoading && <p className="message">Loading Gmail messages...</p>}

          {!gmailLoading && gmailError && (
            <div className="message error-message" role="alert">
              <span>{gmailError}</span>
              <button type="button" onClick={() => void loadGmailMessages()}>
                Try again
              </button>
            </div>
          )}

          {!gmailLoading && !gmailError && gmailMessages.length === 0 && (
            <p className="message">No job-related Gmail messages found.</p>
          )}

          {!gmailLoading && !gmailError && gmailMessages.map((message) => (
            <article className="gmail-message" key={message.id}>
              <p className="gmail-subject">
                {message.subject || '(No subject)'}
              </p>
              <p>From: {message.from || 'Unknown sender'}</p>
              <p>{message.snippet || 'No message preview available.'}</p>
              <p className="gmail-id">Gmail ID: {message.id}</p>
            </article>
          ))}
        </section>
      )}

      {isFormOpen && (
        <div className="modal-backdrop" role="presentation" onMouseDown={closeForm}>
          <section
            className="application-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="application-form-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <h2 id="application-form-title">
              {editingApplicationId === null
                ? 'Add Application'
                : 'Edit Application'}
            </h2>
            <form onSubmit={handleSubmit}>
              <label>
                Company
                <input
                  required
                  autoFocus
                  value={newApplication.company}
                  onChange={(event) => updateField('company', event.target.value)}
                />
              </label>
              <label>
                Role
                <input
                  required
                  value={newApplication.role}
                  onChange={(event) => updateField('role', event.target.value)}
                />
              </label>
              <label>
                Location
                <input
                  value={newApplication.location}
                  onChange={(event) => updateField('location', event.target.value)}
                />
              </label>
              <label>
                Status
                <select
                  value={newApplication.status}
                  onChange={(event) =>
                    updateField('status', event.target.value as ApplicationStatus)
                  }
                >
                  {statuses.map((applicationStatus) => (
                    <option value={applicationStatus} key={applicationStatus}>
                      {statusLabels[applicationStatus]}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Date Applied
                <input
                  required
                  type="date"
                  value={newApplication.dateApplied}
                  onChange={(event) =>
                    updateField('dateApplied', event.target.value)
                  }
                />
              </label>
              <label>
                Job URL
                <input
                  type="url"
                  value={newApplication.jobUrl}
                  onChange={(event) => updateField('jobUrl', event.target.value)}
                />
              </label>
              <label>
                Notes
                <textarea
                  rows={3}
                  value={newApplication.notes}
                  onChange={(event) => updateField('notes', event.target.value)}
                />
              </label>

              {formError && <p className="form-error">{formError}</p>}

              <div className="form-actions">
                <button type="button" onClick={closeForm} disabled={isSaving}>
                  Cancel
                </button>
                <button type="submit" disabled={isSaving}>
                  {isSaving
                    ? 'Saving...'
                    : editingApplicationId === null
                      ? 'Add Application'
                      : 'Save Changes'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </main>
  )
}

export default App
