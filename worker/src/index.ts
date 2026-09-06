const MODRINTH_TOKEN_URL = "https://api.modrinth.com/_internal/oauth/token";
const MAX_BODY_BYTES = 4096;
const USER_AGENT = "Asterion/1.0 (+github.com/tomatopotato17265/Asterion)";

interface Env {
  CLIENT_ID: string;
  CLIENT_SECRET: string;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/") {
      return json({ service: "asterion", ok: true });
    }
    if (url.pathname !== "/token") {
      return json({ error: "not_found" }, 404);
    }
    if (request.method !== "POST") {
      return json({ error: "method_not_allowed" }, 405);
    }
    if (!env.CLIENT_ID || !env.CLIENT_SECRET) {
      return json({ error: "server_misconfigured" }, 500);
    }

    const raw = await request.text();
    if (raw.length > MAX_BODY_BYTES) {
      return json({ error: "payload_too_large" }, 413);
    }

    let parsed: { code?: unknown; redirect_uri?: unknown };
    try {
      parsed = JSON.parse(raw);
    } catch {
      return json({ error: "invalid_json" }, 400);
    }

    const code = parsed.code;
    const redirectUri = parsed.redirect_uri;
    if (typeof code !== "string" || typeof redirectUri !== "string") {
      return json({ error: "missing_code_or_redirect_uri" }, 400);
    }

    const form = new URLSearchParams();
    form.set("grant_type", "authorization_code");
    form.set("code", code);
    form.set("redirect_uri", redirectUri);
    form.set("client_id", env.CLIENT_ID);

    const upstream = await fetch(MODRINTH_TOKEN_URL, {
      method: "POST",
      headers: {
        "content-type": "application/x-www-form-urlencoded",
        accept: "application/json",
        "user-agent": USER_AGENT,
        authorization: env.CLIENT_SECRET,
      },
      body: form.toString(),
    });

    const responseBody = await upstream.text();
    return new Response(responseBody, {
      status: upstream.status,
      headers: {
        "content-type":
          upstream.headers.get("content-type") ?? "application/json",
      },
    });
  },
};
