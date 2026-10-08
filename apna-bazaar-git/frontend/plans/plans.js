const community = document.querySelector("#community"),
  grid = document.querySelector("#plans"),
  status = document.querySelector("#status");
let visitor =
  heyhoodResident()?.id || localStorage.getItem("heyhood-plan-visitor");
if (
  !visitor ||
  !/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(
    visitor,
  )
) {
  visitor = crypto.randomUUID();
  localStorage.setItem("heyhood-plan-visitor", visitor);
}
const selected =
  new URLSearchParams(location.search).get("community") ||
  sessionStorage.getItem("heyhood-community");
if (["tridasa", "sayuk"].includes(selected)) community.value = selected;
let plans = [],
  filter = "all",
  controller;
function el(tag, cls, text) {
  const n = document.createElement(tag);
  n.className = cls || "";
  if (text != null) n.textContent = text;
  return n;
}
function render() {
  grid.replaceChildren();
  const shown = plans.filter((p) => filter === "all" || p.my_vote === "in");
  status.textContent = shown.length
    ? `${shown.length} plans · ${community.selectedOptions[0].textContent}`
    : filter === "mine"
      ? "Nothing here yet. Tap I’m in on a plan you like."
      : "No plans right now. Check back soon.";
  for (const p of shown) {
    const card = el("article", "plan-card");
    card.dataset.id = p.id;
    card.append(heyhoodSaveButton("plan", p.id));
    const date = new Date(p.starts_at),
      day = new Intl.DateTimeFormat("en-IN", {
        timeZone: "Asia/Kolkata",
        day: "numeric",
        month: "short",
      }).format(date);
    const top = el("div", "plan-top");
    top.append(
      el("span", "plan-category", p.category),
      el("span", "plan-date", day),
    );
    card.append(
      top,
      el("h2", "", p.title),
      el("p", "plan-description", p.description),
    );
    card.append(
      el("p", "plan-location", `⌖ ${p.location}`),
      el(
        "p",
        "plan-time",
        new Intl.DateTimeFormat("en-IN", {
          timeZone: "Asia/Kolkata",
          weekday: "long",
          hour: "numeric",
          minute: "2-digit",
        }).format(date) + " IST",
      ),
    );
    const count = Number(p.interested),
      minimum = Number(p.minimum_interested),
      reached = count >= minimum;
    const meta = el("div", "plan-count");
    meta.append(
      el("strong", "", `${count} / ${minimum}`),
      el("span", "", "interested"),
    );
    card.append(meta);
    const progress = el("progress", "plan-progress");
    progress.max = minimum;
    progress.value = Math.min(count, minimum);
    progress.setAttribute("aria-label", `${count} of ${minimum} interested`);
    card.append(progress);
    const closed = p.ended || p.status !== "gathering";
    card.append(
      el(
        "p",
        "plan-state",
        p.status === "cancelled"
          ? "Cancelled"
          : p.ended
            ? "Interest closed"
            : p.status === "confirmed"
              ? "Organiser confirmed"
              : reached
                ? "Interest goal reached · Awaiting organiser confirmation"
                : `${minimum - count} more needed to reach the interest goal`,
      ),
    );
    const actions = el("div", "plan-actions");
    for (const [choice, label] of [
      ["in", p.my_vote === "in" ? "I’m in ✓" : "I’m in"],
      ["pass", p.my_vote === "pass" ? "Meh! ✓" : "Meh!"],
    ]) {
      const button = el(
        "button",
        choice === "in" ? "plan-in" : "plan-pass",
        label,
      );
      button.setAttribute("aria-pressed", String(p.my_vote === choice));
      button.disabled = closed;
      button.onclick = () =>
        vote(p, p.my_vote === choice ? "clear" : choice, card);
      actions.append(button);
    }
    card.append(actions);
    card.append(
      el(
        "small",
        "plan-disclaimer",
        p.is_demo
          ? "Demo plan · not a confirmed event"
          : "Interest only · confirmation pending",
      ),
    );
    grid.append(card);
  }
}
async function vote(p, choice, card) {
  const slug = community.value;
  card.querySelectorAll("button").forEach((b) => (b.disabled = true));
  try {
    const r = await fetch(`/api/${slug}/plans/${p.id}/vote`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ visitorId: visitor, choice }),
    });
    if (!r.ok) {
      const e = await r.json();
      throw Error(e.error || "Couldn’t save your vote. Please retry.");
    }
    const updated = await r.json();
    if (slug !== community.value) return;
    plans = updated;
    render();
    status.textContent =
      choice === "in"
        ? "You’re in! Interest saved. This isn’t a confirmed booking."
        : choice === "pass"
          ? "No worries. You can change your mind anytime."
          : "Vote removed.";
  } catch (e) {
    if (slug === community.value) {
      render();
      status.textContent = e.message;
    }
  }
}
async function load() {
  controller?.abort();
  controller = new AbortController();
  const slug = community.value;
  plans = [];
  heyhoodSkeletons(grid);
  status.textContent = "Loading hood plans…";
  history.replaceState(null, "", `?community=${slug}`);
  try {
    const r = await fetch(`/api/${slug}/plans?visitorId=${visitor}`, {
      signal: controller.signal,
    });
    if (!r.ok) throw Error();
    const data = await r.json();
    if (slug !== community.value) return;
    plans = data;
    render();
  } catch (e) {
    if(e.name!=="AbortError") { grid.replaceChildren();status.textContent="Couldn’t load plans. ";const retry=el("button","save-action","Retry ↻");retry.onclick=load;status.append(retry); }
  }
}
document.querySelectorAll("[data-filter]").forEach(
  (b) =>
    (b.onclick = () => {
      filter = b.dataset.filter;
      document
        .querySelectorAll("[data-filter]")
        .forEach((x) => x.setAttribute("aria-pressed", String(x === b)));
      render();
    }),
);
community.onchange = load;
load();
