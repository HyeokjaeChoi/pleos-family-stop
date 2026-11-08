#!/usr/bin/env bash
# Gate check (#45): feature/app code must use :core:designsystem tokens, never hardcoded color/size/theme.
# Usage: scripts/check-design-tokens.sh   (exit 1 on any hit)
set -u
cd "$(dirname "$0")/.."
pat='Color\(0x|[0-9]\.sp\b|[0-9]\.dp\b|fontSize *=|isSystemInDarkTheme|MaterialTheme|androidx\.compose\.material3\.(Button|OutlinedButton|FilterChip|OutlinedTextField|Card)\b'
hits=$(grep -rnE "$pat" --include='*.kt' feature/*/src/main app/src/main || true)
if [ -n "$hits" ]; then
  echo "Hardcoded design values found. Use App.colors / App.type / App.space / App.radius / App.icon and the designsystem components:"
  echo "$hits"
  exit 1
fi
echo "design-token check: OK"
