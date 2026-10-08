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
  if (!r.ok)
    throw Error(
      r.status === 503
        ? "Configure CAMPAIGN_ADMIN_TOKEN in Railway to enable operator approval and stock updates."
        : r.status === 401
          ? "Check the operator key."
          : "Could not complete that action.",
    );
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
    const [people, stock, reputation] = await Promise.all([
      api(""),
      api("/availability"),
      api("", "GET", undefined, true),
    ]);
    renderReputation(reputation);
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
