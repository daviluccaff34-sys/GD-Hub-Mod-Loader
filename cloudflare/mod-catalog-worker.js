const MAX_PACKAGE_BYTES = 16 * 1024 * 1024;
const MAX_ENTRY_BYTES = 4 * 1024 * 1024;
const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
  "Access-Control-Max-Age": "86400"
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...CORS, "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" }
  });
}

function clean(value, max) {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

function validManifest(manifest) {
  return manifest && typeof manifest === "object" &&
    ["id", "name", "version"].every(k => typeof manifest[k] === "string" && manifest[k].trim().length > 0);
}

function validateGdhb(bytes) {
  if (bytes.byteLength < 8 || bytes.byteLength > MAX_PACKAGE_BYTES) throw new Error("Tamanho GDHB inválido.");
  const view = new DataView(bytes);
  const magic = new TextDecoder().decode(new Uint8Array(bytes, 0, 4));
  if (magic !== "GDHB") throw new Error("Cabeçalho GDHB inválido.");
  const version = view.getUint16(4, true);
  const count = view.getUint16(6, true);
  if (version !== 1 || count < 1 || count > 128) throw new Error("Versão ou quantidade de arquivos não suportada.");
  let offset = 8;
  const paths = new Set();
  let manifestText = null;
  let sourceFound = false;
  const decoder = new TextDecoder("utf-8", { fatal: true });
  for (let i = 0; i < count; i++) {
    if (offset + 6 > bytes.byteLength) throw new Error("Pacote truncado.");
    const pathLength = view.getUint16(offset, true);
    const dataLength = view.getUint32(offset + 2, true);
    offset += 6;
    if (pathLength < 1 || pathLength > 240 || dataLength > MAX_ENTRY_BYTES || offset + pathLength + dataLength > bytes.byteLength) {
      throw new Error("Entrada GDHB fora dos limites.");
    }
    const path = decoder.decode(new Uint8Array(bytes, offset, pathLength));
    offset += pathLength;
    if (path.startsWith("/") || path.includes("\\") || path.split("/").some(p => !p || p === "." || p === "..") || paths.has(path)) {
      throw new Error("Caminho GDHB inválido ou duplicado.");
    }
    paths.add(path);
    const data = new Uint8Array(bytes, offset, dataLength);
    if (path === "manifest.json") manifestText = decoder.decode(data);
    if (path === "src/main.dash" && dataLength > 0) {
      decoder.decode(data);
      sourceFound = true;
    }
    offset += dataLength;
  }
  if (offset !== bytes.byteLength || manifestText === null || !sourceFound) throw new Error("Manifesto ou src/main.dash ausente, ou dados extras encontrados.");
  let manifest;
  try { manifest = JSON.parse(manifestText); } catch { throw new Error("manifest.json inválido."); }
  if (!validManifest(manifest)) throw new Error("Manifesto precisa conter id, name e version.");
  return manifest;
}

async function listByStatus(kv, status) {
  const mods = [];
  let cursor;
  do {
    const page = await kv.list({ prefix: "meta:", limit: 1000, cursor });
    for (const key of page.keys) {
      const meta = await kv.get(key.name, { type: "json" });
      if (meta && meta.status === status) mods.push(meta);
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor && mods.length < 500);
  return mods;
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: CORS });
    if (!env.MODS) return json({ error: "KV binding MODS is not configured." }, 503);
    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";

    if (request.method === "GET" && path === "/v1/mods") {
      const mods = await listByStatus(env.MODS, "approved");
      mods.sort((a, b) => String(b.publishedAt).localeCompare(String(a.publishedAt)));
      return json({ mods: mods.slice(0, 500).map(m => ({
        id: m.id, name: m.name, version: m.version, author: m.author,
        description: m.description, fileSize: m.fileSize, publishedAt: m.publishedAt
      })) });
    }

    if (request.method === "POST" && path === "/v1/mods") {
      const length = Number(request.headers.get("content-length") || 0);
      if (length > MAX_PACKAGE_BYTES + 128 * 1024) return json({ error: "Upload excede o limite." }, 413);
      let form;
      try { form = await request.formData(); } catch { return json({ error: "Formulário multipart inválido." }, 400); }
      const file = form.get("file");
      if (!file || typeof file.arrayBuffer !== "function" || file.size < 8 || file.size > MAX_PACKAGE_BYTES) {
        return json({ error: "Envie um arquivo .gdhb de até 16 MiB." }, 400);
      }
      const name = clean(form.get("name"), 64);
      const version = clean(form.get("version"), 32);
      const author = clean(form.get("author"), 48);
      const description = clean(form.get("description"), 500);
      if (!name || !version || !author || !description) return json({ error: "Preencha nome, versão, autor e descrição." }, 400);
      const bytes = await file.arrayBuffer();
      let manifest;
      try { manifest = validateGdhb(bytes); } catch (error) { return json({ error: error.message }, 400); }
      const id = crypto.randomUUID();
      const publishedAt = new Date().toISOString();
      const metadata = {
        id, name, version, author, description, status: "pending",
        fileName: clean(file.name, 100) || "mod.gdhb",
        fileSize: file.size, publishedAt,
        manifestId: manifest.id, manifestName: manifest.name, manifestVersion: manifest.version
      };
      // KV supports binary ArrayBuffer values; GDHB's 16 MiB cap stays below KV's 25 MiB value limit.
      await env.MODS.put("file:" + id, bytes);
      await env.MODS.put("meta:" + id, JSON.stringify(metadata));
      return json({ id, status: "pending", message: "Mod enviado para revisão." }, 201);
    }

    const downloadMatch = path.match(/^\/v1\/mods\/([0-9a-f-]+)\/download$/i);
    if (request.method === "GET" && downloadMatch) {
      const id = downloadMatch[1];
      const meta = await env.MODS.get("meta:" + id, { type: "json" });
      if (!meta || meta.status !== "approved") return json({ error: "Mod não encontrado ou ainda não aprovado." }, 404);
      const bytes = await env.MODS.get("file:" + id, { type: "arrayBuffer" });
      if (!bytes) return json({ error: "Arquivo do mod não encontrado." }, 404);
      const headers = new Headers(CORS);
      headers.set("Content-Type", "application/octet-stream");
      headers.set("Content-Disposition", 'attachment; filename="' + id + '.gdhb"');
      headers.set("Cache-Control", "public, max-age=300");
      return new Response(bytes, { headers });
    }

    const admin = request.headers.get("Authorization") || "";
    if (!env.ADMIN_TOKEN || admin !== "Bearer " + env.ADMIN_TOKEN) return json({ error: "Não encontrado." }, 404);

    if (request.method === "GET" && path === "/admin/pending") {
      return json({ pending: await listByStatus(env.MODS, "pending") });
    }

    const approveMatch = path.match(/^\/admin\/approve\/([0-9a-f-]+)$/i);
    if (request.method === "POST" && approveMatch) {
      const id = approveMatch[1];
      const meta = await env.MODS.get("meta:" + id, { type: "json" });
      if (!meta) return json({ error: "Mod não encontrado." }, 404);
      meta.status = "approved";
      meta.approvedAt = new Date().toISOString();
      await env.MODS.put("meta:" + id, JSON.stringify(meta));
      return json({ id, status: "approved" });
    }

    return json({ error: "Rota não encontrada." }, 404);
  }
};
