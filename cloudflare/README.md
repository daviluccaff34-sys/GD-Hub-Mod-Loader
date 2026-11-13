# GD Hub mod catalog

This folder contains the public mod-catalog API Worker.

## Current state

- The Android app now has a **Publicar Mods** tab.
- It accepts `.gdhb` files, checks the 16 MiB limit and GDHB magic header, and saves a local draft.
- When the API URL is configured, the app uploads the package to `POST /v1/mods`.
- New uploads are **pending review**. They are not visible or downloadable until an administrator approves them.
- The catalog uses Workers KV instead of R2 so package publishing can work without enabling R2. Each `.gdhb` is limited to 16 MiB, below KV's 25 MiB maximum value size.

## Enable storage

1. The KV namespace and Worker are already created and deployed.
2. In Cloudflare Dashboard, open **Workers & Pages** → `gdhub-mod-catalog` → **Settings** → **Variables and Secrets**.
3. Add a secret named `ADMIN_TOKEN` with a long random value. Keep it private; it is used only for moderation.

The app's GitHub Actions build now embeds the deployed Worker URL. For a local build, set the same URL at build time:

```sh
gradle assembleDebug -PgdhubPublishApi=https://YOUR-WORKER.YOUR-SUBDOMAIN.workers.dev
```

Do not put `ADMIN_TOKEN` in the Android app or repository. Until that secret is added, submissions can be uploaded as pending, but the moderation endpoints remain unavailable.

## API

- `GET /v1/mods` — list approved mods.
- `POST /v1/mods` — multipart upload with fields `name`, `version`, `author`, `description`, and `file`. Uploads are stored as pending.
- `GET /v1/mods/:id/download` — download an approved package.
- `GET /admin/pending` — list pending submissions; requires `Authorization: Bearer ADMIN_TOKEN`.
- `POST /admin/approve/:id` — approve one submission; requires the same admin header.

The Worker validates the GDHB v1 structure, required manifest fields, source entry, UTF-8 paths, duplicate paths, entry count, per-entry limits, and total package size. It never executes DashCode. Only approved packages are listed or downloadable.

## Abuse controls before opening submissions widely

Configure Cloudflare rate limiting for `POST /v1/mods` and review pending submissions regularly. Keep the KV namespace private; downloads are served through the Worker after approval. Do not expose the admin token in the app.