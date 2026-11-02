# BetterTracker

BetterTracker is a simple job-application tracker built with Java, Spring Boot,
and PostgreSQL. It lets you create, search, filter, edit, and delete applications.
It can connect to Gmail and display job-related emails and applications, and can ping your phone with any updates.

## Tech Stack

- Java 21
- React
- Node.js 
- PostgreSQL
- Google OAuth 

## Local setup

Create a `.env` file in the project root:

```env
DB_URL=jdbc:postgresql://localhost:5432/jobtracker
DB_USERNAME=jobtracker_user
DB_PASSWORD=your_database_password
GOOGLE_GMAIL_CLIENT_ID=your_google_client_id
GOOGLE_GMAIL_CLIENT_SECRET=your_google_client_secret
NTFY_TOPIC=your_private_ntfy_topic
```


## Desktop shortcut

On the configured development computer, double-click **JobTracker** on the
desktop. The launcher starts the backend first, waits until it is ready, starts
the frontend, and opens the application in a browser. Keep the launcher terminal
open while using the application.

## Main features

- Job application CRUD and request validation
- Search, filtering, and sorting
- Gmail OAuth connection
- Job-email and application-update screens
- Duplicate Gmail message prevention
- Success/rejection outcome detection for update emails

The `.env` file details and Gmail OAuth tokens need to be added yourself

## Production configuration

Production uses Neon PostgreSQL, a Spring Boot API on Render, and the React
frontend at `https://bettertracker.vercel.app`.

Vercel builds the frontend using the root `vercel.json`. Browser requests use
the same-origin `/api` path, which Vercel proxies to Render. A production
`VITE_API_URL` is therefore not required.

Render builds the backend using the root `Dockerfile`. Configure these runtime
values in the Render environment:

```env
DB_URL=jdbc:postgresql://your-neon-pooler-host/your-database?sslmode=require
DB_USERNAME=your_neon_username
DB_PASSWORD=your_neon_password
GOOGLE_GMAIL_CLIENT_ID=your_google_client_id
GOOGLE_GMAIL_CLIENT_SECRET=your_google_client_secret
GOOGLE_GMAIL_REDIRECT_URI=https://jobtracker-5j2t.onrender.com/api/gmail/oauth2/callback
FRONTEND_URL=https://bettertracker.vercel.app
NTFY_TOPIC=your_private_ntfy_topic
SCHEDULER_SECRET=your_private_scheduler_secret
```

Use Neon's pooled hostname for normal application traffic. The Spring JDBC URL
must start with `jdbc:postgresql://`. Add the exact deployed callback URL to the
Google OAuth client's authorized redirect URIs.

Gmail OAuth credentials are stored in the `gmail_oauth_credentials` PostgreSQL
table rather than Cloud Run's local filesystem. Reconnect Gmail once after the
production deployment to populate that table in Neon.
