# BetterTracker

BetterTracker is a simple job-application tracker built with React, Spring Boot,
and PostgreSQL. It lets you create, search, filter, edit, and delete applications.
It can connect to Gmail and display job-related emails and application
updates.

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

The intended deployment is Neon PostgreSQL, a Spring Boot API on Google Cloud
Run, and the React frontend on Vercel.

Vercel builds the frontend using the root `vercel.json`. Set this Vercel
environment variable to the public Cloud Run service URL:

```env
VITE_API_URL=https://your-cloud-run-service.run.app
```

Cloud Run builds the backend using the root `Dockerfile`. Configure these
runtime values, preferably using Google Secret Manager for secrets:

```env
DB_URL=jdbc:postgresql://your-neon-pooler-host/your-database?sslmode=require
DB_USERNAME=your_neon_username
DB_PASSWORD=your_neon_password
GOOGLE_GMAIL_CLIENT_ID=your_google_client_id
GOOGLE_GMAIL_CLIENT_SECRET=your_google_client_secret
GOOGLE_GMAIL_REDIRECT_URI=https://your-cloud-run-service.run.app/api/gmail/oauth2/callback
FRONTEND_URL=https://your-vercel-project.vercel.app
NTFY_TOPIC=your_private_ntfy_topic
```

Use Neon's pooled hostname for normal application traffic. The Spring JDBC URL
must start with `jdbc:postgresql://`. Add the exact deployed callback URL to the
Google OAuth client's authorized redirect URIs.

Gmail OAuth credentials are stored in the `gmail_oauth_credentials` PostgreSQL
table rather than Cloud Run's local filesystem. Reconnect Gmail once after the
production deployment to populate that table in Neon.
