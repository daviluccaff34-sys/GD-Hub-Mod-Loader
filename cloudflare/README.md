# GD Hub mod catalog

This folder contains the public mod-catalog API Worker.

## Current state

- The Android app now has a **Publicar Mods** tab.
- It accepts `.gdhb` files, checks the 16 MiB limit and GDHB magic header, and saves a local draft.
- When the API URL is configured, the app uploads the package to `POST /v1/mods`.
- New uploads are **pending review**. They are not visible or downloadable until an administrator approves them.
- The connected Cloudflare account currently reports that R2 has not been enabled. The Worker cannot store package files until R2 is enabled and a bucket is bound.

## Enable storage

1. Open the Cloudflare Dashboard.
2. Open **R2 Object Storage** and complete its first-time enablement.
3. Create a bucket named `gdhub-mods`.
4. Create/deploy a Worker using `mod-catalog-worker.js`.
5. Add an R2 binding named `MODS_BUCKET` pointing to `gdhub-mods`.
6. Add a Worker secret named `ADMIN_TOKEN` with a long random value. Keep it private; it is used only for moderation.
7. Deploy the Worker and note its HTTPS `workers.dev` URL.

The app uses the Worker base URL at build time:

```sh
gradle assembleDebug -PgdhubPublishApi=https://YOUR-WORKER.YOUR-SUBDOMAIN.workers.dev
```

Do not put `ADMIN_TOKEN` in the Android app or repository.

## API

- `GET /v1/mods` — list approved mods.
- `POST /v1/mods` — multipart upload with fields `name`, `version`, `author`, `description`, and `file`. Uploads are stored as pending.
- `GET /v1/mods/:id/download` — download an approved package.
- `GET /admin/pending` — list pending submissions; requires `Authorization: Bearer ADMIN_TOKEN`.
- `POST /admin/approve/:id` — approve one submission; requires the same admin header.

The Worker validates the GDHB v1 structure, required manifest fields, source entry, UTF-8 paths, duplicate paths, entry count, per-entry limits, and total package size. It never executes DashCode. Only approved packages are listed or downloadable.

## Abuse controls before opening submissions widely

Configure Cloudflare rate limiting for `POST /v1/mods` and review pending submissions regularly. Keep the R2 bucket private; downloads are served through the Worker after approval. Do not expose the admin token in the app.