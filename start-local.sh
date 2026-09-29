#!/usr/bin/env bash

set -u

PROJECT_DIR="/home/hemish/Desktop/JavaProjects/JobTracker"
FRONTEND_DIR="$PROJECT_DIR/frontend/frontend"
FRONTEND_URL="http://localhost:5173"
BACKEND_URL="http://localhost:8080"
BACKEND_PID=""
FRONTEND_PID=""

port_is_open() {
    timeout 1 bash -c "</dev/tcp/127.0.0.1/$1" >/dev/null 2>&1
}

cleanup() {
    printf '\nStopping JobTracker...\n'
    if [[ -n "$FRONTEND_PID" ]] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi
    if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
    wait 2>/dev/null || true
}

trap cleanup EXIT INT TERM

printf 'Starting JobTracker locally...\n'

if port_is_open 8080; then
    printf 'Backend is already running on port 8080.\n'
else
    printf 'Starting Spring Boot backend...\n'
    (
        cd "$PROJECT_DIR" || exit 1
        exec ./mvnw spring-boot:run
    ) &
    BACKEND_PID=$!
fi

if port_is_open 5173; then
    printf 'Frontend is already running on port 5173.\n'
else
    if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
        printf 'Installing frontend packages (first launch only)...\n'
        (cd "$FRONTEND_DIR" && npm install) || exit 1
    fi

    printf 'Starting React frontend...\n'
    (
        cd "$FRONTEND_DIR" || exit 1
        exec npm run dev
    ) &
    FRONTEND_PID=$!
fi

printf 'Waiting for the website'
for _ in {1..60}; do
    if curl --silent --fail --max-time 1 "$FRONTEND_URL" >/dev/null 2>&1; then
        printf '\nOpening %s\n' "$FRONTEND_URL"
        xdg-open "$FRONTEND_URL" >/dev/null 2>&1 &
        if [[ -z "$BACKEND_PID" && -z "$FRONTEND_PID" ]]; then
            printf 'JobTracker was already running, so no new processes were started.\n'
            exit 0
        fi
        printf 'JobTracker is running. Keep this window open.\n'
        printf 'Press Ctrl+C or close this window to stop it.\n\n'
        wait
        exit $?
    fi

    if [[ -n "$BACKEND_PID" ]] && ! kill -0 "$BACKEND_PID" 2>/dev/null; then
        printf '\nThe backend stopped before startup completed. Check the error above.\n'
        read -r -p 'Press Enter to close...'
        exit 1
    fi
    if [[ -n "$FRONTEND_PID" ]] && ! kill -0 "$FRONTEND_PID" 2>/dev/null; then
        printf '\nThe frontend stopped before startup completed. Check the error above.\n'
        read -r -p 'Press Enter to close...'
        exit 1
    fi

    printf '.'
    sleep 1
done

printf '\nThe frontend did not become ready within 60 seconds.\n'
read -r -p 'Press Enter to close...'
exit 1
