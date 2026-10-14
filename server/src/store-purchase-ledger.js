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
      const notificationIds = await this.ctx.storage.get("subscription:notification-ids:v1");
      return json(200, {
        subscription: subscription ?? null,
        notificationIds: Array.isArray(notificationIds) ? notificationIds : [],
      });
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
    if (url.pathname === "/subscription/sync") {
      return this.#syncSubscription(body);
    }
    if (url.pathname === "/subscription/notification/seen") {
      return this.#markNotificationSeen(body);
    }
    if (url.pathname === "/reserve") {
      return this.#reserve(body);
    }
    if (url.pathname === "/complete") {
      return this.#complete(body);
    }
    return json(404, { error: "NOT_FOUND" });
  }

  async #delete(body) {
    if (!isAccountId(body?.accountId)) return json(400, { error: "INVALID_PURCHASE" });
    const existing = await this.ctx.storage.get("purchase:v1");
    const subscription = await this.ctx.storage.get("subscription:v1");
    if (
      (existing && existing.accountId !== body.accountId) ||
      (subscription && subscription.accountId !== body.accountId)
    ) {
      return json(409, { error: "PURCHASE_ACCOUNT_MISMATCH" });
    }
    if (existing) await this.ctx.storage.delete("purchase:v1");
    if (subscription) {
      await this.ctx.storage.delete("subscription:v1");
      await this.ctx.storage.delete("subscription:notification-ids:v1");
    }
    return json(200, { ok: true, deleted: Boolean(existing || subscription) });
  }

  async #syncSubscription(body) {
    if (
      !isAccountId(body?.accountId) ||
      !isOwnerAccountId(body?.ownerAccountId) ||
      body?.productId !== "paws_plus" ||
      ![
        "SUBSCRIPTION_STATE_PENDING",
        "SUBSCRIPTION_STATE_ACTIVE",
        "SUBSCRIPTION_STATE_PAUSED",
        "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
        "SUBSCRIPTION_STATE_ON_HOLD",
        "SUBSCRIPTION_STATE_CANCELED",
        "SUBSCRIPTION_STATE_EXPIRED",
        "SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED",
      ].includes(body?.subscriptionState) ||
      !Number.isSafeInteger(body?.expiryTimeMs) ||
      body.expiryTimeMs <= 0 ||
      typeof body?.autoRenewEnabled !== "boolean" ||
      !isAccountId(body?.purchaseTokenHash)
    ) {
      return json(400, { error: "INVALID_SUBSCRIPTION" });
    }

    const existing = await this.ctx.storage.get("subscription:v1");
    if (existing && (
      existing.accountId !== body.accountId ||
      existing.ownerAccountId !== body.ownerAccountId ||
      existing.productId !== body.productId ||
      existing.purchaseTokenHash !== body.purchaseTokenHash
    )) {
      return json(409, { error: "SUBSCRIPTION_ACCOUNT_MISMATCH" });
    }
    const changed = !existing ||
      existing.subscriptionState !== body.subscriptionState ||
      existing.expiryTimeMs !== body.expiryTimeMs ||
      existing.autoRenewEnabled !== body.autoRenewEnabled;
    const subscription = {
      accountId: body.accountId,
      ownerAccountId: body.ownerAccountId.toLowerCase(),
      productId: body.productId,
      subscriptionState: body.subscriptionState,
      expiryTimeMs: body.expiryTimeMs,
      autoRenewEnabled: body.autoRenewEnabled,
      purchaseTokenHash: body.purchaseTokenHash,
      updatedAt: changed ? Date.now() : Number(existing.updatedAt ?? Date.now()),
    };
    if (changed) await this.ctx.storage.put("subscription:v1", subscription);
    return json(200, { created: !existing, changed, replay: !changed, subscription });
  }

  async #markNotificationSeen(body) {
    const messageId = String(body?.messageId ?? "");
    if (!/^[A-Za-z0-9_-]{1,200}$/.test(messageId)) {
      return json(400, { error: "INVALID_NOTIFICATION_ID" });
    }
    const stored = await this.ctx.storage.get("subscription:notification-ids:v1");
    const ids = Array.isArray(stored) ? stored : [];
    if (ids.includes(messageId)) return json(200, { ok: true, duplicate: true });
    const next = [...ids, messageId].slice(-100);
    await this.ctx.storage.put("subscription:notification-ids:v1", next);
    return json(200, { ok: true, duplicate: false });
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

function isOwnerAccountId(value) {
  return typeof value === "string" &&
    /^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/i.test(value);
}

function json(status, body, headers = {}) {
  return Response.json(body, {
    status,
    headers: {
      "cache-control": "no-store",
      ...headers,
    },
  });
}
