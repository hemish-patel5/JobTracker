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
DB_USERNAME=jobtracker_user
DB_PASSWORD=your_database_password
GOOGLE_GMAIL_CLIENT_ID=your_google_client_id
GOOGLE_GMAIL_CLIENT_SECRET=your_google_client_secret
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
