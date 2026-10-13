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
  const [expandedEmailIds, setExpandedEmailIds] = useState<Set<string>>(
    () => new Set(),
  )
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
    if (
      (activePage !== 'updates' && activePage !== 'emails') ||
      updatesLoaded
    ) return

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

  const successCount = updateMessages.filter(
    (message) => message.outcome === 'SUCCESS',
  ).length
  const rejectionCount = updateMessages.filter(
    (message) => message.outcome === 'REJECTION',
  ).length
  const latestEmailTimestamp = gmailMessages.reduce<number | null>(
    (latest, message) =>
      message.receivedAt !== null && (latest === null || message.receivedAt > latest)
        ? message.receivedAt
        : latest,
    null,
  )

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

  const toggleEmailBody = (messageId: string) => {
    setExpandedEmailIds((current) => {
      const next = new Set(current)

      if (next.has(messageId)) {
        next.delete(messageId)
      } else {
        next.add(messageId)
      }

      return next
    })
  }

  return (
    <div className="tracker">
      <header className="tracker-header">
        <div className="brand-navigation">
          <button
            type="button"
            className="brand"
            onClick={() => setActivePage('applications')}
            aria-label="Open applications"
          >
            <span className="brand-mark" aria-hidden="true">▣</span>
            <span>BetterTracker</span>
          </button>
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
          <button type="button" className="primary-button" onClick={openAddForm}>
            Add Application
          </button>
          <span className="header-divider" aria-hidden="true" />
          <div className="session-actions">
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

      <main className="page-content">
        {activePage === 'applications' && (
          <div className="page-stack">
            <section className="page-intro">
              <div>
                <h1>Applications</h1>
              </div>
            </section>

            <section className="summary" aria-label="Application summary">
              <article className="metric-card">
                <span>Total</span>
                <strong>{applications.length}</strong>
              </article>
              <article className="metric-card">
                <span>Applied</span>
                <strong>{countByStatus('APPLIED')}</strong>
              </article>
              <article className="metric-card">
                <span>Interview</span>
                <strong>{countByStatus('INTERVIEW')}</strong>
              </article>
              <article className="metric-card metric-card-dark">
                <span>Offer</span>
                <strong>{countByStatus('OFFER')}</strong>
              </article>
            </section>

            <section className="filters panel" aria-label="Application filters">
              <label className="search-field">
              <span className="sr-only">Search applications</span>
              <input
                type="search"
                placeholder="Search applications..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
              </label>
              <label className="status-filter">
                <span>Status</span>
                <select
                  value={status}
                  onChange={(event) =>
                    setStatus(event.target.value as ApplicationStatus | 'ALL')
                  }
                >
                  <option value="ALL">All</option>
                  {statuses.map((applicationStatus) => (
                    <option value={applicationStatus} key={applicationStatus}>
                      {statusLabels[applicationStatus]}
                    </option>
                  ))}
                </select>
              </label>
            </section>

            {isLoading && <p className="message panel">Loading applications...</p>}

            {error && (
              <div className="message panel error-message" role="alert">
                <span>{error}</span>
                <button type="button" onClick={retryLoadingApplications}>Try again</button>
              </div>
            )}

            {!isLoading && !error && filteredApplications.length === 0 && (
              <div className="empty-state panel">
                <strong>No matching applications</strong>
                <span>Try another keyword or status.</span>
              </div>
            )}

            {!isLoading && !error && filteredApplications.length > 0 && (
              <section className="application-list" aria-label="Job applications">
                {filteredApplications.map((application) => (
                  <article className="application-card panel" key={application.id}>
                    <div className="application-copy">
                      <div className="card-title-row">
                        <h2>{application.company}</h2>
                        <span className={`status-badge status-${application.status.toLowerCase()}`}>
                          {statusLabels[application.status]}
                        </span>
                      </div>
                      <p className="application-role">{application.role}</p>
                      <p className="application-meta">
                        <span>{application.location || 'Not specified'}</span>
                        <span aria-hidden="true">/</span>
                        <span>Applied {formatDate(application.dateApplied)}</span>
                      </p>
                    </div>
                    <div className="application-actions">
                      <button type="button" onClick={() => openEditForm(application)}>Edit</button>
                      <button type="button" onClick={() => void handleDelete(application)}>Delete</button>
                    </div>
                  </article>
                ))}
                <p className="result-count">
                  Showing {filteredApplications.length} of {applications.length} tracked applications
                </p>
              </section>
            )}
          </div>
        )}

        {activePage === 'emails' && (
          <div className="page-stack email-page">
            <section className="page-intro">
              <div>
                <h1>Job Emails</h1>
              </div>
              <span className="outlined-pill">{gmailMessages.length} messages</span>
            </section>

            <section className="email-overview" aria-label="Email overview">
              <div><span>Gmail API</span><strong>{gmailConnected ? 'Connected' : 'Not connected'}</strong></div>
              <div><span>Total job emails</span><strong>{gmailMessages.length} active</strong></div>
              <div><span>Application updates</span><strong>{updateMessages.length} reviews</strong></div>
              <div><span>Latest email</span><strong>{latestEmailTimestamp ? formatEmailDate(latestEmailTimestamp) : 'Not loaded'}</strong></div>
            </section>

            <section className="section-heading panel">
              <div>
                <h2>Inbox</h2>
                <span>{gmailMessages.length} total</span>
              </div>
              <div className="email-controls">
                <button type="button" onClick={() => setEmailSort((current) => current === 'latest' ? 'oldest' : 'latest')}>
                  Sort: {emailSort === 'latest' ? 'Latest' : 'Oldest'}
                </button>
                <button type="button" onClick={() => void loadGmailMessages()}>Refresh</button>
              </div>
            </section>

            {gmailLoading && <p className="message panel">Loading Gmail messages...</p>}
            {!gmailLoading && gmailError && (
              <div className="message panel error-message" role="alert">
                <span>{gmailError}</span>
                <button type="button" onClick={() => void loadGmailMessages()}>Try again</button>
              </div>
            )}
            {!gmailLoading && !gmailError && gmailMessages.length === 0 && (
              <div className="empty-state panel"><strong>No job emails found</strong><span>Refresh after Gmail is connected.</span></div>
            )}
            {!gmailLoading && !gmailError && (
              <section className="email-list" aria-label="Gmail messages">
                {sortedGmailMessages.map((message) => {
                  const gmailUrl = `https://mail.google.com/mail/u/0/#inbox/${message.id}`
                  const isExpanded = expandedEmailIds.has(message.id)
                  const hasFullBody = Boolean(message.fullBody?.trim())
                  return (
                    <article className="gmail-message panel" key={message.id}>
                      <div className="email-card-top">
                        <div className="email-copy">
                          <h2 className="gmail-subject"><a href={gmailUrl} target="_blank" rel="noreferrer">{message.subject || '(No subject)'}</a></h2>
                          <p className="email-meta"><span><b>From:</b> {message.from || 'Unknown sender'}</span><span>Received: {formatEmailDate(message.receivedAt)}</span></p>
                        </div>
                      </div>
                      <p className={`email-snippet ${isExpanded ? 'email-body-expanded' : ''}`}>
                        {isExpanded && hasFullBody
                          ? message.fullBody
                          : message.snippet || 'No message preview available.'}
                      </p>
                      <div className="email-card-footer">
                        <p className="gmail-id">Gmail ID: <code>{message.id}</code></p>
                        <div className="email-card-actions">
                          {hasFullBody && (
                            <button
                              type="button"
                              className="small-button"
                              aria-expanded={isExpanded}
                              onClick={() => toggleEmailBody(message.id)}
                            >
                              {isExpanded ? 'Show less' : 'Show more'}
                            </button>
                          )}
                          <a className="small-button" href={gmailUrl} target="_blank" rel="noreferrer">Open in Gmail</a>
                        </div>
                      </div>
                    </article>
                  )
                })}
              </section>
            )}
          </div>
        )}

        {activePage === 'updates' && (
          <div className="page-stack updates-page">
            <section className="sync-card panel">
              <div>
                <div className="card-title-row"><h1>Sync Feed</h1></div>
              </div>
              <div className="outcome-counts">
                <span className="outcome-count outcome-success">Success <b>{successCount}</b></span>
                <span className="outcome-count outcome-rejection">Rejection <b>{rejectionCount}</b></span>
              </div>
            </section>

            <section className="section-heading panel">
              <div><h2>Updates</h2></div>
              <div className="email-controls">
                <button type="button" onClick={() => setEmailSort((current) => current === 'latest' ? 'oldest' : 'latest')}>
                  Sort: {emailSort === 'latest' ? 'Latest' : 'Oldest'}
                </button>
                <button type="button" onClick={() => void loadUpdateMessages()}>Refresh</button>
              </div>
            </section>

            {updatesLoading && <p className="message panel">Loading updates...</p>}
            {!updatesLoading && updatesError && (
              <div className="message panel error-message" role="alert">
                <span>{updatesError}</span>
                <button type="button" onClick={() => void loadUpdateMessages()}>Try again</button>
              </div>
            )}
            {!updatesLoading && !updatesError && updatesLoaded && updateMessages.length === 0 && (
              <div className="empty-state panel"><strong>No update emails found</strong><span>New application updates will appear here.</span></div>
            )}
            {!updatesLoading && !updatesError && (
              <section className="update-list" aria-label="Update emails">
                {sortedUpdateMessages.map((message) => (
                  <article className="update-card panel" key={message.id}>
                    <div className="update-copy">
                      <div className="card-title-row">
                        <h2 className="gmail-subject"><a href={`https://mail.google.com/mail/u/0/#inbox/${message.id}`} target="_blank" rel="noreferrer">{message.subject || '(No subject)'}</a></h2>
                        <span className={`email-outcome ${message.outcome === 'REJECTION' ? 'email-outcome-rejection' : 'email-outcome-success'}`}>
                          {message.outcome === 'REJECTION' ? 'Rejection' : 'Success'}
                        </span>
                      </div>
                      <p className="email-meta"><span>From: <b>{message.from || 'Unknown sender'}</b></span><span>Received: <b>{formatEmailDate(message.receivedAt)}</b></span></p>
                    </div>
                    <p className="gmail-id id-chip"><span>ID:</span> {message.id}</p>
                  </article>
                ))}
                {updateMessages.length > 0 && <p className="result-count">Showing all {updateMessages.length} parsed mailbox updates</p>}
              </section>
            )}
          </div>
        )}
      </main>

      <footer className="site-footer">
        <span><b>BetterTracker</b> © 2026</span>
        <span>Built for high-velocity job pipelines.</span>
      </footer>

      {isFormOpen && (
        <div className="modal-backdrop" role="presentation" onMouseDown={closeForm}>
          <section className="application-modal panel" role="dialog" aria-modal="true" aria-labelledby="application-form-title" onMouseDown={(event) => event.stopPropagation()}>
            <div className="modal-heading">
              <div><p className="eyebrow">Application details</p><h2 id="application-form-title">{editingApplicationId === null ? 'Add Application' : 'Edit Application'}</h2></div>
              <button type="button" className="close-button" onClick={closeForm} aria-label="Close">×</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <label>Company<input required autoFocus value={newApplication.company} onChange={(event) => updateField('company', event.target.value)} /></label>
                <label>Role<input required value={newApplication.role} onChange={(event) => updateField('role', event.target.value)} /></label>
                <label>Location<input value={newApplication.location} onChange={(event) => updateField('location', event.target.value)} /></label>
                <label>Status<select value={newApplication.status} onChange={(event) => updateField('status', event.target.value as ApplicationStatus)}>{statuses.map((applicationStatus) => <option value={applicationStatus} key={applicationStatus}>{statusLabels[applicationStatus]}</option>)}</select></label>
                <label>Date Applied<input required type="date" value={newApplication.dateApplied} onChange={(event) => updateField('dateApplied', event.target.value)} /></label>
                <label>Job URL<input type="url" value={newApplication.jobUrl} onChange={(event) => updateField('jobUrl', event.target.value)} /></label>
              </div>
              <label>Notes<textarea rows={3} value={newApplication.notes} onChange={(event) => updateField('notes', event.target.value)} /></label>
              {formError && <p className="form-error">{formError}</p>}
              <div className="form-actions">
                <button type="button" onClick={closeForm} disabled={isSaving}>Cancel</button>
                <button type="submit" className="primary-button" disabled={isSaving}>{isSaving ? 'Saving...' : editingApplicationId === null ? 'Add Application' : 'Save Changes'}</button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  )
}

export default App
