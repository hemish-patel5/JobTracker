import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  createApplication,
  deleteApplication,
  getApplications,
  type NewJobApplication,
  updateApplication,
} from './api/applications'
import { getGmailMessages, getUpdateMessages } from './api/gmail'
import { API_URL } from './config'
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

function formatEmailDate(timestamp: number | null) {
  if (timestamp === null) return 'Unknown'

  return new Intl.DateTimeFormat('en-NZ', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(timestamp))
}

function connectGmail() {
  window.location.href = `${API_URL}/api/gmail/connect`
}

async function disconnectGmail() {
  const response = await fetch(
    `${API_URL}/api/gmail/disconnect`,
    { method: 'DELETE' },
  )

  if (!response.ok) {
    throw new Error(`Unable to log out of Gmail (${response.status})`)
  }
}

async function isGmailConnected(signal?: AbortSignal) {
  const response = await fetch(`${API_URL}/api/gmail/status`, {
    signal,
  })

  if (!response.ok) {
    throw new Error(`Unable to check Gmail status (${response.status})`)
  }

  const result = (await response.json()) as { connected: boolean }
  return result.connected
}

function App() {
  const [activePage, setActivePage] = useState<
    'applications' | 'emails' | 'updates'
  >('applications')
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
  const [gmailLoading, setGmailLoading] = useState(false)
  const [gmailLoaded, setGmailLoaded] = useState(false)
  const [gmailError, setGmailError] = useState<string | null>(null)
  const [emailSort, setEmailSort] = useState<'latest' | 'oldest'>('latest')
  const [updateMessages, setUpdateMessages] = useState<GmailMessage[]>([])
  const [updatesLoading, setUpdatesLoading] = useState(false)
  const [updatesLoaded, setUpdatesLoaded] = useState(false)
  const [updatesError, setUpdatesError] = useState<string | null>(null)

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
      setGmailLoaded(true)
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

  const loadUpdateMessages = useCallback(async (signal?: AbortSignal) => {
    setUpdatesLoading(true)
    setUpdatesError(null)

    try {
      setUpdateMessages(await getUpdateMessages(signal))
      setUpdatesLoaded(true)
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') {
        return
      }

      setUpdatesError(
        requestError instanceof Error
          ? requestError.message
          : 'Unable to load update emails',
      )
    } finally {
      if (!signal?.aborted) {
        setUpdatesLoading(false)
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
    if (activePage !== 'emails' || gmailLoaded) return

    const controller = new AbortController()
    // Gmail synchronization starts when Job Emails is first opened.
    // oxlint-disable-next-line react/set-state-in-effect
    void loadGmailMessages(controller.signal)

    return () => controller.abort()
  }, [activePage, gmailLoaded, loadGmailMessages])

  useEffect(() => {
    if (activePage !== 'updates' || updatesLoaded) return

    const controller = new AbortController()
    // Update synchronization starts when the Updates screen is first opened.
    // oxlint-disable-next-line react/set-state-in-effect
    void loadUpdateMessages(controller.signal)

    return () => controller.abort()
  }, [activePage, loadUpdateMessages, updatesLoaded])

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

  const sortedGmailMessages = useMemo(
    () => [...gmailMessages].sort((first, second) =>
      emailSort === 'latest'
        ? (second.receivedAt ?? 0) - (first.receivedAt ?? 0)
        : (first.receivedAt ?? 0) - (second.receivedAt ?? 0),
    ),
    [emailSort, gmailMessages],
  )

  const sortedUpdateMessages = useMemo(
    () => [...updateMessages].sort((first, second) =>
      emailSort === 'latest'
        ? (second.receivedAt ?? 0) - (first.receivedAt ?? 0)
        : (first.receivedAt ?? 0) - (second.receivedAt ?? 0),
    ),
    [emailSort, updateMessages],
  )

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
            <button
              type="button"
              className={activePage === 'updates' ? 'active' : undefined}
              aria-pressed={activePage === 'updates'}
              onClick={() => setActivePage('updates')}
            >
              Updates
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
            <div className="email-controls">
              <button
                type="button"
                onClick={() =>
                  setEmailSort((current) =>
                    current === 'latest' ? 'oldest' : 'latest',
                  )
                }
              >
                Sort: {emailSort === 'latest' ? 'Latest' : 'Oldest'}
              </button>
              <button type="button" onClick={() => void loadGmailMessages()}>
                Refresh
              </button>
            </div>
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

          {!gmailLoading && !gmailError && sortedGmailMessages.map((message) => (
            <article className="gmail-message" key={message.id}>
              <p className="gmail-subject">
                <a
                  href={`https://mail.google.com/mail/u/0/#inbox/${message.id}`}
                  target="_blank"
                  rel="noreferrer"
                >
                  {message.subject || '(No subject)'}
                </a>
              </p>
              <p>From: {message.from || 'Unknown sender'}</p>
              <p>Received: {formatEmailDate(message.receivedAt)}</p>
              <p>{message.snippet || 'No message preview available.'}</p>
              <p className="gmail-id">Gmail ID: {message.id}</p>
            </article>
          ))}
        </section>
      )}

      {activePage === 'updates' && (
        <section className="gmail-message-section" aria-label="Update emails">
          <div className="section-heading">
            <h2>Updates</h2>
            <div className="email-controls">
              <button
                type="button"
                onClick={() =>
                  setEmailSort((current) =>
                    current === 'latest' ? 'oldest' : 'latest',
                  )
                }
              >
                Sort: {emailSort === 'latest' ? 'Latest' : 'Oldest'}
              </button>
              <button type="button" onClick={() => void loadUpdateMessages()}>
                Refresh
              </button>
            </div>
          </div>

          {updatesLoading && <p className="message">Loading updates...</p>}

          {!updatesLoading && updatesError && (
            <div className="message error-message" role="alert">
              <span>{updatesError}</span>
              <button type="button" onClick={() => void loadUpdateMessages()}>
                Try again
              </button>
            </div>
          )}

          {!updatesLoading && !updatesError && updatesLoaded &&
            updateMessages.length === 0 && (
              <p className="message">No update emails found.</p>
            )}

          {!updatesLoading && !updatesError && sortedUpdateMessages.map((message) => (
            <article className="gmail-message" key={message.id}>
              <p className="gmail-subject">
                <a
                  href={`https://mail.google.com/mail/u/0/#inbox/${message.id}`}
                  target="_blank"
                  rel="noreferrer"
                >
                  {message.subject || '(No subject)'}
                </a>
              </p>
              <p>From: {message.from || 'Unknown sender'}</p>
              <p>Received: {formatEmailDate(message.receivedAt)}</p>
              <p>
                Outcome:{' '}
                <span
                  className={`email-outcome ${
                    message.outcome === 'REJECTION'
                      ? 'email-outcome-rejection'
                      : 'email-outcome-success'
                  }`}
                >
                  {message.outcome === 'REJECTION' ? 'Rejection' : 'Success'}
                </span>
              </p>
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
