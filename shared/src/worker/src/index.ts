const MODRINTH_TOKEN_URL = "https://api.modrinth.com/_internal/oauth/token";
const MAX_BODY_BYTES = 4096;
const USER_AGENT = "Asterion/1.0 (+github.com/tomatopotato17265/Asterion)";
const IOS_BUNDLE_ID = "dev.tomatopotato.asterion.Asterion";
const ANDROID_PACKAGE = "dev.tomatopotato.asterion";
const ANDROID_SHA256_FINGERPRINTS = [
  "94:00:BE:30:81:87:14:CC:1E:8B:46:D0:19:1A:BA:A4:9E:04:10:98:1F:F1:7B:8A:58:CD:71:58:82:52:9D:81",
];

interface Env {
  CLIENT_ID: string;
  CLIENT_SECRET: string;
  APPLE_TEAM_ID: string;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}

function assetlinks(): Response {
  return json([
    {
      relation: ["delegate_permission/common.handle_all_urls"],
      target: {
        namespace: "android_app",
        package_name: ANDROID_PACKAGE,
        sha256_cert_fingerprints: ANDROID_SHA256_FINGERPRINTS,
      },
    },
  ]);
}

function appleAppSiteAssociation(env: Env): Response {
  if (!env.APPLE_TEAM_ID) return json({ error: "server_misconfigured" }, 500);
  const appID = `${env.APPLE_TEAM_ID}.${IOS_BUNDLE_ID}`;
  return json({
    applinks: {
      details: [{ appIDs: [appID], components: [{ "/": "/callback*" }] }],
    },
    webcredentials: { apps: [appID] },
  });
}

const CALLBACK_PAGE = `<!doctype html><html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Asterion</title></head>
<body style="font:16px/1.5 system-ui,sans-serif;margin:3rem;text-align:center;color:#111">
<p>Signing you in&hellip;</p><p>You can return to Asterion.</p></body></html>`;

async function exchangeToken(request: Request, env: Env): Promise<Response> {
  if (!env.CLIENT_ID || !env.CLIENT_SECRET) {
    return json({ error: "server_misconfigured" }, 500);
  }

  const raw = await request.text();
  if (raw.length > MAX_BODY_BYTES) return json({ error: "payload_too_large" }, 413);

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
      "content-type": upstream.headers.get("content-type") ?? "application/json",
    },
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const { pathname } = new URL(request.url);
    const isGet = request.method === "GET";

    if (isGet && pathname === "/") return json({ service: "asterion", ok: true });

    if (isGet && pathname === "/.well-known/assetlinks.json") return assetlinks();
    if (isGet && pathname === "/.well-known/apple-app-site-association") {
      return appleAppSiteAssociation(env);
    }

    if (pathname === "/callback") {
      return new Response(CALLBACK_PAGE, {
        headers: { "content-type": "text/html; charset=utf-8" },
      });
    }

    if (pathname === "/token") {
      if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);
      return exchangeToken(request, env);
    }

    return json({ error: "not_found" }, 404);
  },
};
