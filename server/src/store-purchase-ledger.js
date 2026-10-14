export class StorePurchaseLedger {
  constructor(ctx) {
    this.ctx = ctx;
    this.tail = Promise.resolve();
  }

  fetch(request) {
    const run = this.tail.then(
      () => this.#handle(request),
      () => this.#handle(request),
    );
    this.tail = run.catch(() => {});
    return run;
  }

  async #handle(request) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/status") {
      const purchase = await this.ctx.storage.get("purchase:v1");
      return json(200, { purchase: purchase ?? null });
    }
    if (request.method === "GET" && url.pathname === "/subscription/status") {
      const subscription = await this.ctx.storage.get("subscription:v1");
      return json(200, { subscription: subscription ?? null });
    }

    if (request.method !== "POST") {
      return json(405, { error: "METHOD_NOT_ALLOWED" }, { allow: "GET, POST" });
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return json(400, { error: "INVALID_JSON" });
    }

    if (url.pathname === "/delete") {
      return this.#delete(body);
    }
    if (url.pathname === "/subscription/register") return this.#registerSubscription(body);
    if (url.pathname === "/subscription/delete") return this.#deleteSubscription(body);
    if (url.pathname === "/reserve") {
      return this.#reserve(body);
    }
    if (url.pathname === "/complete") {
      return this.#complete(body);
    }
    return json(404, { error: "NOT_FOUND" });
  }

  async #registerSubscription(body) {
    if (!isSubscriptionAccountId(body?.accountId) || !isAccountId(body?.billingAccountId) ||
        body?.productId !== "paws_plus" || !isHash(body?.tokenHash)) {
      return json(400, { error: "INVALID_SUBSCRIPTION" });
    }
    const existing = await this.ctx.storage.get("subscription:v1");
    if (existing?.deleted === true) return json(410, { error: "SUBSCRIPTION_TOKEN_REVOKED" });
    if (existing && (existing.accountId !== body.accountId ||
        existing.billingAccountId !== body.billingAccountId ||
        existing.productId !== body.productId || existing.tokenHash !== body.tokenHash)) {
      return json(409, { error: "SUBSCRIPTION_ACCOUNT_MISMATCH" });
    }
    const subscription = existing ?? {
      accountId: body.accountId, billingAccountId: body.billingAccountId,
      productId: body.productId, tokenHash: body.tokenHash, deleted: false, registeredAt: Date.now(),
    };
    await this.ctx.storage.put("subscription:v1", subscription);
    return json(200, { ok: true, subscription });
  }

  async #deleteSubscription(body) {
    if (!isAccountId(body?.accountId)) return json(400, { error: "INVALID_SUBSCRIPTION" });
    const existing = await this.ctx.storage.get("subscription:v1");
    if (!existing) return json(200, { ok: true, deleted: false });
    if (existing.billingAccountId !== body.accountId) return json(409, { error: "SUBSCRIPTION_ACCOUNT_MISMATCH" });
    if (existing.deleted === true) return json(200, { ok: true, deleted: true, replay: true });
    await this.ctx.storage.put("subscription:v1", {
      accountId: existing.accountId, billingAccountId: existing.billingAccountId, productId: existing.productId,
      tokenHash: existing.tokenHash, deleted: true, deletedAt: Date.now(),
    });
    return json(200, { ok: true, deleted: true, replay: false });
  }

  async #delete(body) {
    if (!isAccountId(body?.accountId)) return json(400, { error: "INVALID_PURCHASE" });
    const existing = await this.ctx.storage.get("purchase:v1");
    if (!existing) return json(200, { ok: true, deleted: false });
    if (existing.accountId !== body.accountId) return json(409, { error: "PURCHASE_ACCOUNT_MISMATCH" });
    await this.ctx.storage.delete("purchase:v1");
    return json(200, { ok: true, deleted: true });
  }

  async #reserve(body) {
    if (
      !isAccountId(body?.accountId) ||
      typeof body?.productId !== "string" ||
      !Number.isSafeInteger(body?.gemAmount) ||
      body.gemAmount < 0 ||
      typeof body?.removeAds !== "boolean"
    ) {
      return json(400, { error: "INVALID_PURCHASE" });
    }

    const existing = await this.ctx.storage.get("purchase:v1");
    if (existing) {
      if (
        existing.productId !== body.productId ||
        existing.accountId !== body.accountId
      ) {
        return json(409, { error: "PURCHASE_ALREADY_REDEEMED" });
      }
      return json(200, { created: false, purchase: existing });
    }

    const purchase = {
      accountId: body.accountId,
      productId: body.productId,
      gemAmount: body.gemAmount,
      removeAds: body.removeAds,
      state: "RESERVED",
    };
    await this.ctx.storage.put("purchase:v1", purchase);
    return json(200, { created: true, purchase });
  }

  async #complete(body) {
    if (
      !isAccountId(body?.accountId) ||
      typeof body?.productId !== "string"
    ) {
      return json(400, { error: "INVALID_PURCHASE" });
    }

    const existing = await this.ctx.storage.get("purchase:v1");
    if (
      !existing ||
      existing.productId !== body.productId ||
      existing.accountId !== body.accountId
    ) {
      return json(409, { error: "PURCHASE_RESERVATION_NOT_FOUND" });
    }

    const purchase = { ...existing, state: "FULFILLED" };
    await this.ctx.storage.put("purchase:v1", purchase);
    return json(200, { purchase });
  }
}

function isAccountId(value) {
  return typeof value === "string" && /^[a-f0-9]{64}$/.test(value);
}
function isSubscriptionAccountId(value) {
  return typeof value === "string" && /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}
function isHash(value) { return typeof value === "string" && /^[a-f0-9]{64}$/.test(value); }

function json(status, body, headers = {}) {
  return Response.json(body, {
    status,
    headers: {
      "cache-control": "no-store",
      ...headers,
    },
  });
}
