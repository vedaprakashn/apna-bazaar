const community = document.querySelector("#community"),
  params = new URLSearchParams(location.search);
const selected =
  params.get("community") || sessionStorage.getItem("heyhood-community");
if (["tridasa", "sayuk"].includes(selected)) community.value = selected;
let view = params.get("view") || "overview",
  activity,
  needId,
  replying,
  posting = false;
function el(tag, cls, text) {
  const n = document.createElement(tag);
  n.className = cls || "";
  if (text != null) n.textContent = text;
  return n;
}
function when(value) {
  return (
    new Date(value).toLocaleString("en-IN", {
      timeZone: "Asia/Kolkata",
      dateStyle: "medium",
      timeStyle: "short",
    }) + " IST"
  );
}
function button(text, action) {
  const b = el("button", "", text);
  b.type = "button";
  b.onclick = async () => {
    b.disabled = true;
    try {
      await action();
      await load();
    } catch (e) {
      heyhoodNotice(e.message);
    } finally {
      b.disabled = false;
    }
  };
  return b;
}
function card(title, body, status) {
  const c = el("article", "activity-card");
  if (status) c.append(el("span", "status-pill", status));
  c.append(el("h3", "", title));
  if (body) c.append(el("p", "", body));
  return c;
}
function changeView(next) {
  view = next;
  for (const v of ["overview", "requests", "profile"])
    document.querySelector(`#${v}-view`).hidden = v !== next;
  document
    .querySelectorAll("[data-view]")
    .forEach((b) =>
      b.setAttribute("aria-pressed", String(b.dataset.view === next)),
    );
}
document
  .querySelectorAll("[data-view]")
  .forEach((b) => (b.onclick = () => changeView(b.dataset.view)));
async function load() {
  document.querySelector("#status").textContent = "Loading your hood…";
  if (view === "overview")
    heyhoodSkeletons(document.querySelector("#overview"));
  try {
    const r = await fetch(`/api/${community.value}/resident/requests`);
    if (!r.ok) throw Error("Requests are taking a moment. Try again.");
    const needs = await r.json();
    renderNeeds(needs);
    if (heyhoodResident()) {
      activity = await heyhoodResidentFetch("activity");
      renderActivity(activity);
      const p = activity.profile;
      document.querySelector("#name").value = p.name;
      document.querySelector("#flat").value = p.flat_number;
      document.querySelector("#share-name").checked = p.share_name;
      document.querySelector("#share-flat").checked = p.share_flat;
      document.querySelector("#verification").textContent =
        p.verification === "verified"
          ? "Community operator approved ✓"
          : "Verification: " +
            p.verification +
            ". Your name and flat need operator review.";
    } else {
      const empty = card(
        "Your finds deserve a home.",
        "Create an optional profile to save favourites, post a need and track responses. Browsing stays open.",
      );
      empty.append(
        button("Set up my profile", async () => changeView("profile")),
      );
      document.querySelector("#overview").replaceChildren(empty);
    }
    document.querySelector("#status").textContent = "";
  } catch (e) {
    document
      .querySelectorAll("#overview .loading-skeleton")
      .forEach((n) => n.remove());
    const status = document.querySelector("#status");
    status.textContent = e.message + " ";
    status.append(button("Retry", load));
  }
}
function renderNeeds(needs) {
  const grid = document.querySelector("#requests");
  grid.replaceChildren();
  if (!needs.length)
    grid.append(
      el(
        "p",
        "empty-card",
        "No open requests yet. Be the first to put a need in your hood.",
      ),
    );
  for (const n of needs) {
    const c = card(
      n.title,
      n.body,
      n.verification === "verified"
        ? "Verified resident"
        : "Resident · " + n.verification,
    );
    c.append(
      el(
        "p",
        "muted",
        `${n.name}${n.flat_number ? " · Flat " + n.flat_number : ""} · Open until ${when(n.expires_at)}`,
      ),
    );
    c.append(
      button("I can help ↗", async () => {
        if (!heyhoodResident()) {
          changeView("profile");
          heyhoodNotice("Create your profile to respond.");
          return;
        }
        replying = n.id;
        document.querySelector("#reply-title").textContent = n.title;
        document.querySelector("#reply-body").value = "";
        document.querySelector("#reply-status").textContent = "";
        document.querySelector("#reply-sheet").showModal();
      }),
    );
    grid.append(c);
  }
}
function renderActivity(a) {
  const root = document.querySelector("#overview");
  root.replaceChildren();
  const profile = card(
    "Your resident profile",
    `${a.profile.name} · ${a.profile.flat_number}`,
    a.profile.verification === "verified"
      ? "Operator approved ✓"
      : "Verification " + a.profile.verification,
  );
  profile.append(button("Edit visibility", async () => changeView("profile")));
  root.append(profile);
  function section(title, items, render) {
    root.append(el("h2", "", title));
    if (!items.length)
      root.append(
        el(
          "p",
          "empty-card",
          "Nothing here yet. Your next find will fit right in.",
        ),
      );
    else {
      const grid = el("div", "activity-grid");
      items.forEach((item) => grid.append(render(item)));
      root.append(grid);
    }
  }
  section("Saved & followed", a.saved, (s) => {
    const c = card(s.title, null, s.following ? "Following" : "Saved");
    const url =
      s.kind === "provider"
        ? `../provider/index.html?community=${community.value}&id=${s.entity_id}`
        : `../${s.kind === "ride" ? "rides" : "plans"}/index.html?community=${community.value}`;
    const link = el("a", "", "Open ↗");
    link.href = url;
    c.append(
      link,
      button("Remove", () =>
        heyhoodResidentFetch(`saved/${s.kind}/${s.entity_id}`, {
          method: "DELETE",
        }),
      ),
    );
    return c;
  });
  section("Updates from followed shops", a.updates, (u) => {
    const c = card(u.shop_name, u.name, u.live_status.replaceAll("_", " "));
    c.append(el("p", "muted", "Updated " + when(u.availability_updated_at)));
    return c;
  });
  section("My requests", a.requests, (n) => {
    const c = card(n.title, n.body, n.status);
    c.append(el("p", "muted", "Expires " + when(n.expires_at)));
    if (n.status !== "closed")
      c.append(
        button("Close request", () =>
          heyhoodResidentFetch(`requests/${n.id}/close`, { method: "POST" }),
        ),
      );
    return c;
  });
  section("Responses & arrangements", a.responses, (s) => {
    const c = card(s.title, s.body, s.status);
    c.append(
      el(
        "p",
        "muted",
        `${s.name}${s.flat_number ? " · " + s.flat_number : ""} · ${s.verification}`,
      ),
    );
    if (s.mine && s.status === "offered")
      c.append(
        button("Accept this response", async () => {
          if (
            !confirm(
              "Accept this response? You still need to agree the details with your neighbour.",
            )
          )
            return;
          await heyhoodResidentFetch(`requests/${s.request_id}/accept`, {
            method: "POST",
            body: JSON.stringify({ responseId: s.id }),
          });
        }),
      );
    if (s.status === "accepted")
      c.append(
        el(
          "p",
          "muted",
          "Matched by the requester. Agree timing and arrangements before proceeding.",
        ),
      );
    return c;
  });
  section("My ride posts", a.rides, (r) => {
    const c = card(r.destination, when(r.departure_at), r.status);
    if (r.recurrence_until)
      c.append(
        el(
          "p",
          "",
          `Repeats until ${r.recurrence_until} · ${r.exchange_terms}`,
        ),
      );
    if (r.status === "open")
      c.append(
        button("Close post", async () => {
          const response = await fetch(
            `/api/${community.value}/rides/${r.id}/close`,
            {
              method: "POST",
              headers: heyhoodResidentHeaders(),
              body: JSON.stringify({ visitorId: heyhoodResident().id }),
            },
          );
          if (!response.ok) throw Error("Could not close that ride.");
        }),
      );
    return c;
  });
  section("My Hood Plans", a.plans, (p) => {
    const c = card(
      p.title,
      when(p.starts_at),
      p.choice === "in" ? "I’m in" : "Passed",
    );
    const link = el("a", "", "View plan ↗");
    link.href = `../plans/index.html?community=${community.value}`;
    c.append(link);
    return c;
  });
}
document.querySelector("#profile-form").onsubmit = async (e) => {
  e.preventDefault();
  const submit = e.submitter;
  submit.disabled = true;
  const status = document.querySelector("#profile-status");
  status.textContent = "Saving…";
  try {
    const current = heyhoodResident();
    const data = {
      name: document.querySelector("#name").value,
      flatNumber: document.querySelector("#flat").value,
      shareName: document.querySelector("#share-name").checked,
      shareFlat: document.querySelector("#share-flat").checked,
      consent: document.querySelector("#profile-consent").checked,
    };
    if (!current) {
      let id =
        localStorage.getItem("heyhood-ride-visitor") ||
        localStorage.getItem("heyhood-plan-visitor");
      if (
        !id ||
        Object.keys(localStorage)
          .filter((k) => k.startsWith("heyhood-resident:"))
          .some((k) => {
            try {
              return JSON.parse(localStorage.getItem(k)).id === id;
            } catch {
              return false;
            }
          })
      )
        id = crypto.randomUUID();
      data.id = id;
      data.legacyPlanVisitorId = localStorage.getItem("heyhood-plan-visitor");
      data.legacyRideVisitorId = localStorage.getItem("heyhood-ride-visitor");
      data.legacyTokens = {};
      for (const key of Object.keys(localStorage).filter((k) =>
        k.startsWith("heyhood-resident:"),
      )) {
        try {
          const prior = JSON.parse(localStorage.getItem(key));
          data.legacyTokens[prior.id] = prior.token;
        } catch {}
      }
    }
    const result = await heyhoodResidentFetch("profile", {
      method: current ? "PUT" : "POST",
      body: JSON.stringify(data),
    });
    const p = result.profile || result;
    heyhoodStoreResident({ ...p, token: result.token || current.token });
    status.textContent = "Saved. Verification remains " + p.verification + ".";
    await load();
    if (params.get("compose")) openNeed();
    else if (params.get("return")) {
      const path = params.get("return");
      if (/^\/(chatbot|provider|rides|plans)\/index\.html(?:\?|$)/.test(path))
        location.href = path;
    }
  } catch (e) {
    status.textContent = e.message;
  } finally {
    submit.disabled = false;
  }
};
function openNeed() {
  if (!heyhoodResident()) {
    changeView("profile");
    heyhoodNotice("Create your profile first. Your request draft is retained.");
    return;
  }
  needId = crypto.randomUUID();
  const draft =
    params.get("compose") ||
    sessionStorage.getItem(`heyhood-need-draft:${community.value}`) ||
    "";
  document.querySelector("#need-title").value = draft.slice(0, 120);
  document.querySelector("#need-body").value = draft;
  const date = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);
  document.querySelector("#need-expiry").value = date;
  document.querySelector("#need-expiry").min = new Date(Date.now() + 86400000)
    .toISOString()
    .slice(0, 10);
  document.querySelector("#need-publish").checked = false;
  document.querySelector("#need-status").textContent = "";
  document.querySelector("#need-sheet").showModal();
}
document.querySelector("#new-need").onclick = openNeed;
document.querySelector("#need-body").oninput = (e) =>
  sessionStorage.setItem(
    `heyhood-need-draft:${community.value}`,
    e.target.value,
  );
document.querySelectorAll("[data-close]").forEach(
  (b) =>
    (b.onclick = () => {
      if (!posting) document.getElementById(b.dataset.close).close();
    }),
);
document.querySelectorAll("dialog").forEach((d) =>
  d.addEventListener("cancel", (e) => {
    if (posting) e.preventDefault();
  }),
);
document.querySelector("#need-form").onsubmit = async (e) => {
  e.preventDefault();
  posting = true;
  e.submitter.disabled = true;
  try {
    await heyhoodResidentFetch("requests", {
      method: "POST",
      body: JSON.stringify({
        id: needId,
        title: document.querySelector("#need-title").value,
        body: document.querySelector("#need-body").value,
        expiresAt: new Date(
          document.querySelector("#need-expiry").value + "T23:59:59+05:30",
        ).toISOString(),
        publish: document.querySelector("#need-publish").checked,
      }),
    });
    document.querySelector("#need-sheet").close();
    sessionStorage.removeItem(`heyhood-need-draft:${community.value}`);
    changeView("overview");
    await load();
    heyhoodNotice("Your need is in the hood. No broadcast sent.");
  } catch (error) {
    document.querySelector("#need-status").textContent = error.message;
  } finally {
    posting = false;
    e.submitter.disabled = false;
  }
};
document.querySelector("#reply-form").onsubmit = async (e) => {
  e.preventDefault();
  posting = true;
  e.submitter.disabled = true;
  try {
    await heyhoodResidentFetch(`requests/${replying}/responses`, {
      method: "POST",
      body: JSON.stringify({
        body: document.querySelector("#reply-body").value,
      }),
    });
    document.querySelector("#reply-sheet").close();
    await load();
    heyhoodNotice("Response sent. Track it in My activity.");
  } catch (error) {
    document.querySelector("#reply-status").textContent = error.message;
  } finally {
    posting = false;
    e.submitter.disabled = false;
  }
};
changeView(view);
load().then(() => {
  if (params.get("compose") && heyhoodResident()) openNeed();
  if (params.get("request")) changeView("requests");
});
