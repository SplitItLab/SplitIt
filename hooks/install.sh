#!/usr/bin/env bash
set -e

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GIT_DIR=$(git rev-parse --git-dir)
TARGET="$GIT_DIR/hooks/pre-commit"

echo "🔗  Installing git hooks..."

chmod +x "$HOOKS_DIR/pre-commit"
cp "$HOOKS_DIR/pre-commit" "$TARGET"
chmod +x "$TARGET"

echo "✅  pre-commit instalado en → $TARGET"
echo ""