#!/usr/bin/env bash
# Push helper that reads credentials from ../.env
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." >/dev/null 2>&1 && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
    set -a
    # shellcheck disable=SC1091
    source .env
    set +a
fi

: "${GITHUB_USER:?Set GITHUB_USER in .env}"
: "${GITHUB_TOKEN:?Set GITHUB_TOKEN in .env}"
: "${GITHUB_REPO:?Set GITHUB_REPO in .env}"

REMOTE_URL="https://${GITHUB_USER}:${GITHUB_TOKEN}@github.com/${GITHUB_REPO}.git"

if ! git rev-parse --git-dir >/dev/null 2>&1; then
    git init
    git checkout -B main
fi

if ! git remote get-url origin >/dev/null 2>&1; then
    git remote add origin "$REMOTE_URL"
else
    git remote set-url origin "$REMOTE_URL"
fi

# Author identity for CI commits (only needed once).
if [[ -z "$(git config user.email || true)" ]]; then
    git config user.email "${GITHUB_USER}@users.noreply.github.com"
    git config user.name "${GITHUB_USER}"
fi

BRANCH="${1:-main}"
git checkout -B "$BRANCH"

git add -A
if ! git diff --cached --quiet; then
    MSG="${COMMIT_MESSAGE:-Auto commit from push.sh}"
    git commit -m "$MSG"
fi

git push -u origin "$BRANCH"
