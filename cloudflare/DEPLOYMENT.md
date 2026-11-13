# Cloudflare deployment — GD Hub

## Published services

- Studio Worker: `gdhub-studio`
- Public URL: https://gdhub-studio.daviluccaff34.workers.dev/
- Catalog Worker: `gdhub-mod-catalog`
- Catalog API: https://gdhub-mod-catalog.daviluccaff34.workers.dev/
- KV binding: `MODS` → namespace `GDHUB_MOD_CATALOG` (`b38f2330e74242b89ab649efafb48108`)
- workers.dev is enabled for both scripts.
- The Studio is a static asset map embedded in a Worker script; no GitHub integration is used.

The Studio deployment was accepted by the Cloudflare API on 2026-11-13 (version `396e6513-71ee-460b-8660-45c5a46d71c8`). The catalog Worker already existed and has its KV binding configured.

## Required administrator secret

The catalog currently has no `ADMIN_TOKEN` secret. Public upload/list/download routes are implemented, but approval endpoints intentionally reject requests until the secret exists.

Configure it without committing it:
1. Open Cloudflare Dashboard → **Workers & Pages** → `gdhub-mod-catalog`.
2. Open **Settings** → **Variables and Secrets**.
3. Add a **Secret** named exactly `ADMIN_TOKEN`.
4. Generate a long random value, save it, and deploy the updated Worker.
5. In the Studio's `catalogo.html`, enter the same value in the admin token field when reviewing pending mods. The page does not save it to localStorage.

Never put this value in GitHub, source files, a public issue, or the Android APK. Do not send it in chat.

## Verification still required

- Open the Studio URL in a regular browser and confirm the home page, Mod Creator, command list and catalog load.
- Submit a valid `.gdhb` package. Confirm it appears as pending and is absent from the public approved list.
- With the secret configured, approve the package. Confirm it appears in the public list and downloads with a valid GDHB header.
- Remove the test package from KV after verification.

The Cloudflare API confirmed both Worker deployments and workers.dev settings. The catalog now exposes `GET /health` (returns KV binding status and a boolean indicating whether ADMIN_TOKEN is configured); the Studio catalog page has a **Testar Worker** button. Direct HTTP requests to workers.dev are blocked in the current tool environment, so browser-level smoke tests are not claimed as complete.
