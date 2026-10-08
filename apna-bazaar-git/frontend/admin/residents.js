const status = document.querySelector("#status"),
  community = document.querySelector("#community");
let offerings = [];
function el(tag, text) {
  const n = document.createElement(tag);
  if (text) n.textContent = text;
  return n;
}
async function api(path, method = "GET", body, reputation = false) {
  const r = await fetch(
    `/api/${community.value}/admin/${reputation ? "reputation" : "residents"}${path}`,
    {
      method,
      headers: {
        Authorization:
          "Bearer " + document.querySelector("#operator-key").value,
        "Content-Type": "application/json",
      },
      body: body ? JSON.stringify(body) : undefined,
    },
  );
  if (!r.ok) {
    const error = await r.json().catch(() => ({}));
    throw Error(
      error.error ||
        (r.status === 503
          ? "Configure CAMPAIGN_ADMIN_TOKEN in Railway to enable the operator workspace."
          : r.status === 401
            ? "Check the operator key."
            : "Could not complete that action."),
    );
  }
  const text = await r.text();
  return text ? JSON.parse(text) : null;
}
function controls(card, id, path, options, current) {
  const select = el("select");
  select.setAttribute("aria-label", "Status");
  options.forEach((value) => {
    const o = el("option", value.replaceAll("_", " "));
    o.value = value;
    select.append(o);
  });
  select.value = current;
  const b = el("button", "Apply update");
  b.onclick = async () => {
    b.disabled = true;
    try {
      if (
        path.includes("review") &&
        select.value === "verified" &&
        !confirm(
          "Have you checked this resident’s name and flat against community records?",
        )
      )
        return;
      await api(path, "POST", { status: select.value });
      status.textContent = "Updated.";
      await load();
    } catch (e) {
      status.textContent = e.message;
    } finally {
      b.disabled = false;
    }
  };
  card.append(select, b);
}
async function load() {
  status.textContent = "Loading…";
  try {
    const [people, stock, reputation, invitations, plans] = await Promise.all([
      api(""),
      api("/availability"),
      api("", "GET", undefined, true),
      api("/invitations"),
      api("/plans"),
    ]);
    renderReputation(reputation);
    renderInvitations(invitations);
    renderPlans(plans);
    const grid = document.querySelector("#residents");
    grid.replaceChildren();
    people.forEach((p) => {
      const c = el("article");
      c.className = "activity-card";
      c.append(
        el("h3", p.name + " · " + p.flat_number),
        el("p", "Review: " + p.verification),
      );
      controls(
        c,
        p.id,
        `/${p.id}/review`,
        ["pending", "verified", "rejected"],
        p.verification,
      );
      grid.append(c);
    });
    if (!people.length) grid.append(el("p", "No profiles to review yet."));
    offerings = stock;
    renderStock();
    status.textContent = "Operator workspace ready.";
  } catch (e) {
    status.textContent = e.message;
  }
}
function renderStock() {
  const grid = document.querySelector("#stock");
  grid.replaceChildren();
  const term = document.querySelector("#stock-search").value.toLowerCase();
  offerings
    .filter((o) => (o.name + " " + o.shop_name).toLowerCase().includes(term))
    .forEach((o) => {
      const c = el("article");
      c.className = "activity-card";
      c.append(
        el("h3", o.shop_name),
        el("p", o.name),
        el(
          "p",
          o.availability_updated_at
            ? "Last updated " +
                new Date(o.availability_updated_at).toLocaleString()
            : "Not confirmed yet",
        ),
      );
      controls(
        c,
        o.id,
        `/availability/${o.id}`,
        ["unconfirmed", "available", "sold_out", "preorder"],
        o.live_status,
      );
      grid.append(c);
    });
}
document.querySelector("#operator-form").onsubmit = (e) => {
  e.preventDefault();
  load();
};
document.querySelector("#stock-search").oninput = renderStock;

function renderReputation(data) {
  const grid = document.querySelector("#provider-checks");
  grid.replaceChildren();
  for (const p of data.providers) {
    const card = el("article");
    card.className = "activity-card";
    card.append(
      el("h3", p.shop_name || p.name),
      el(
        "p",
        `${p.name} · ${p.flat_number || "Flat not listed"} · ${p.whatsapp_number || "No contact"}`,
      ),
      el(
        "p",
        p.is_verified
          ? `Provider checked · ${new Date(p.verification_checked_at).toLocaleString()}`
          : "Not checked",
      ),
    );
    const form = el("form"),
      checks = {};
    for (const [name, label] of [
      ["identityChecked", "Identity checked"],
      ["flatChecked", "Flat association checked"],
      ["contactChecked", "Contact checked"],
    ]) {
      const l = el("label"),
        input = el("input");
      input.type = "checkbox";
      checks[name] = input;
      l.append(input, document.createTextNode(" " + label));
      form.append(l);
    }
    const evidenceLabel = el("label", "Private evidence / revocation reason"),
      evidence = el("textarea");
    evidence.maxLength = 1000;
    evidence.required = true;
    evidence.rows = 3;
    evidenceLabel.append(evidence);
    form.append(evidenceLabel);
    const save = el(
      "button",
      p.is_verified ? "Recheck provider" : "Mark provider checked",
    );
    save.type = "submit";
    save.disabled = /demo/i.test(p.name || "");
    if (save.disabled)
      card.append(el("p", "Fictional demo providers cannot be verified."));
    form.append(save);
    async function submit(verified) {
      await api(
        `/${p.id}/verify`,
        "POST",
        {
          verified,
          identityChecked: checks.identityChecked.checked,
          flatChecked: checks.flatChecked.checked,
          contactChecked: checks.contactChecked.checked,
          evidence: evidence.value,
        },
        true,
      );
      await load();
    }
    form.onsubmit = async (e) => {
      e.preventDefault();
      save.disabled = true;
      try {
        await submit(true);
      } catch (e) {
        status.textContent = e.message;
        save.disabled = false;
      }
    };
    if (p.is_verified) {
      const revoke = el("button", "Revoke check");
      revoke.type = "button";
      revoke.onclick = async () => {
        if (!evidence.value.trim()) {
          status.textContent = "Enter a revocation reason.";
          return;
        }
        revoke.disabled = true;
        try {
          await submit(false);
        } catch (e) {
          status.textContent = e.message;
          revoke.disabled = false;
        }
      };
      form.append(revoke);
    }
    card.append(form);
    grid.append(card);
  }
  const reviews = document.querySelector("#provider-reviews");
  reviews.replaceChildren();
  if (!data.reviews.length) reviews.append(el("p", "No resident reviews yet."));
  for (const r of data.reviews) {
    const card = el("article");
    card.className = "activity-card";
    card.append(
      el("h3", r.shop_name),
      el("p", `${r.stars} stars · ${r.hidden ? "Hidden" : "Visible"}`),
      el("p", r.body || "Rating only"),
    );
    const label = el("label", "Content-policy reason"),
      reason = el("input");
    reason.maxLength = 1000;
    label.append(reason);
    const action = el("button", r.hidden ? "Restore review" : "Hide review");
    action.onclick = async () => {
      if (!reason.value.trim()) {
        status.textContent = "Enter a moderation reason.";
        return;
      }
      action.disabled = true;
      try {
        await api(
          `/${r.provider_id}/moderate`,
          "POST",
          {
            residentId: r.resident_id,
            hidden: !r.hidden,
            reason: reason.value,
          },
          true,
        );
        await load();
      } catch (e) {
        status.textContent = e.message;
        action.disabled = false;
      }
    };
    card.append(label, action);
    reviews.append(card);
  }
  const feedback = document.querySelector("#connection-feedback");
  feedback.replaceChildren();
  if (!data.feedback.length)
    feedback.append(el("p", "No contact feedback yet."));
  for (const f of data.feedback) {
    const c = el("article");
    c.className = "activity-card";
    c.append(
      el("h3", f.shop_name),
      el("p", `${f.outcome.replaceAll("_", " ")} · ${f.count}`),
    );
    feedback.append(c);
  }
}

function renderInvitations(rows) {
  const root = document.querySelector("#invitation-history");
  root.replaceChildren();
  for (const inv of rows) {
    const c = el("article");
    c.className = "activity-card";
    c.append(
      el("h3", inv.flat_number),
      el(
        "p",
        inv.revoked_at
          ? "Revoked"
          : inv.consumed_at
            ? "Used"
            : new Date(inv.expires_at) < new Date()
              ? "Expired"
              : "Ready · expires " +
                new Date(inv.expires_at).toLocaleDateString("en-IN", {
                  timeZone: "Asia/Kolkata",
                }),
      ),
    );
    if (!inv.consumed_at && !inv.revoked_at) {
      const b = el("button", "Revoke unused code");
      b.onclick = async () => {
        b.disabled = true;
        try {
          await api(`/invitations/${inv.id}/revoke`, "POST");
          await load();
        } catch (e) {
          status.textContent = e.message;
          b.disabled = false;
        }
      };
      c.append(b);
    }
    root.append(c);
  }
}
document.querySelector("#invitation-form").onsubmit = async (e) => {
  e.preventDefault();
  const b = e.submitter;
  b.disabled = true;
  try {
    const rows = await api("/invitations", "POST", {
      flats: document
        .querySelector("#invite-flats")
        .value.split(/\r?\n/)
        .filter((x) => x.trim()),
      validDays: Number(document.querySelector("#invite-days").value),
      privateDelivery: document.querySelector("#invite-private").checked,
    });
    const root = document.querySelector("#fresh-invitations");
    root.replaceChildren(
      el("p", "Shown once. Save privately; the server retains only hashes."),
    );
    for (const inv of rows) {
      const line = el(
        "p",
        inv.flat +
          " · " +
          inv.code +
          " · expires " +
          new Date(inv.expiresAt).toLocaleDateString("en-IN", {
            timeZone: "Asia/Kolkata",
          }),
      );
      root.append(line);
    }
    const download = el("button", "Download private operator CSV");
    download.type = "button";
    download.onclick = () => {
      const quote = (x) =>
        '"' +
        String(x)
          .replace(/^[=+@-]/, "'$&")
          .replaceAll('"', '""') +
        '"';
      const text =
        "flat,code,expires_at\r\n" +
        rows
          .map((x) => [x.flat, x.code, x.expiresAt].map(quote).join(","))
          .join("\r\n");
      const url = URL.createObjectURL(
        new Blob([text], { type: "text/csv;charset=utf-8" }),
      );
      const a = el("a");
      a.href = url;
      a.download = "heyhood-private-flat-invitations.csv";
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    };
    root.append(download);
    const clear = el("button", "Clear codes from this screen");
    clear.type = "button";
    clear.onclick = () => root.replaceChildren();
    root.append(clear);
    await load();
  } catch (err) {
    status.textContent = err.message;
  } finally {
    b.disabled = false;
  }
};
function renderPlans(plans) {
  const root = document.querySelector("#operator-plans");
  root.replaceChildren();
  for (const p of plans) {
    const c = el("article");
    c.className = "activity-card";
    c.append(
      el("h3", p.title),
      el(
        "p",
        `${p.is_demo ? "Demo · " : ""}${p.status} · ${p.interested}/${p.minimum_interested} interested · ${p.attending} attending · ${p.waitlisted} waitlisted`,
      ),
    );
    if (!p.is_demo && !p.ended && p.status !== "cancelled") {
      const label = el("label", "Confirmation details / cancellation reason"),
        note = el("textarea");
      note.maxLength = 500;
      label.append(note);
      c.append(label);
      for (const action of p.status === "gathering"
        ? ["confirm", "cancel"]
        : ["cancel"]) {
        const b = el(
          "button",
          action === "confirm" ? "Confirm plan" : "Cancel plan",
        );
        b.onclick = async () => {
          b.disabled = true;
          try {
            await api(`/plans/${p.id}/manage`, "POST", {
              action,
              note: note.value,
            });
            await load();
          } catch (e) {
            status.textContent = e.message;
          } finally {
            b.disabled = false;
          }
        };
        c.append(b);
      }
    }
    root.append(c);
  }
}
document.querySelector("#operator-plan-form").onsubmit = async (e) => {
  e.preventDefault();
  const b = e.submitter;
  b.disabled = true;
  try {
    await api("/plans", "POST", {
      title: document.querySelector("#op-plan-title").value,
      description: document.querySelector("#op-plan-description").value,
      category: document.querySelector("#op-plan-category").value,
      location: document.querySelector("#op-plan-location").value,
      startsAt: new Date(
        document.querySelector("#op-plan-time").value + "+05:30",
      ).toISOString(),
      minimum: Number(document.querySelector("#op-plan-minimum").value),
      capacity: document.querySelector("#op-plan-capacity").value
        ? Number(document.querySelector("#op-plan-capacity").value)
        : null,
      publish: document.querySelector("#op-plan-publish").checked,
    });
    e.target.reset();
    await load();
  } catch (err) {
    status.textContent = err.message;
  } finally {
    b.disabled = false;
  }
};
