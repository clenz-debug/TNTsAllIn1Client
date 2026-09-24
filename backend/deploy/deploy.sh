#!/usr/bin/env bash
# Builds the backend locally and deploys it to the cape server as the unprivileged user from the
# `kleinaberfein` SSH config entry. Idempotent: safe to re-run for every update. Nothing here needs
# root - the server owner already set up Apache (~/www -> https://nxlc.de/, /app/ -> 127.0.0.1:1025)
# and enabled lingering for the user.
set -euo pipefail

HOST="${DEPLOY_HOST:-kleinaberfein}"
NODE_VERSION="v22.23.3"
NODE_DIST="node-${NODE_VERSION}-linux-x64"
cd "$(dirname "$0")/.."

npm run build

# 1. Node.js in ~/.local/node (pinned, checksum-verified) - only if that exact version is missing.
ssh "$HOST" bash -s <<EOF
set -euo pipefail
if [ "\$(~/.local/node/bin/node --version 2>/dev/null)" != "${NODE_VERSION}" ]; then
  tmp=\$(mktemp -d)
  cd "\$tmp"
  curl -fsSLO "https://nodejs.org/dist/${NODE_VERSION}/${NODE_DIST}.tar.xz"
  curl -fsSL "https://nodejs.org/dist/${NODE_VERSION}/SHASUMS256.txt" | grep " ${NODE_DIST}.tar.xz\$" | sha256sum -c -
  rm -rf ~/.local/node && mkdir -p ~/.local
  tar -xJf "${NODE_DIST}.tar.xz" && mv "${NODE_DIST}" ~/.local/node
  cd / && rm -rf "\$tmp"
fi
~/.local/node/bin/node --version
mkdir -p ~/tntcapes-backend ~/www/tntcapes ~/.config/systemd/user
EOF

# 2. Service code (no runtime dependencies - dist/ is all it needs).
scp -q package.json "$HOST":tntcapes-backend/package.json
ssh "$HOST" 'rm -rf ~/tntcapes-backend/dist'
scp -q -r dist "$HOST":tntcapes-backend/dist

# 3. Apache override for the public cape folder + the user service.
scp -q deploy/htaccess-tntcapes "$HOST":www/tntcapes/.htaccess
scp -q deploy/tntcapes.service "$HOST":.config/systemd/user/tntcapes.service
ssh "$HOST" 'systemctl --user daemon-reload && systemctl --user enable tntcapes.service >/dev/null 2>&1 && systemctl --user restart tntcapes.service && sleep 1 && systemctl --user is-active tntcapes.service && curl -fsS http://127.0.0.1:1025/health'
echo
echo "Deployed. Public health check:"
curl -fsS https://nxlc.de/app/health
echo
