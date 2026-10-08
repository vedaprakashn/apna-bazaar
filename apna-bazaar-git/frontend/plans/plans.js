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
  const shown = plans.filter(
    (p) => filter === "all" || p.my_vote === "in" || p.mine,
  );
  status.textContent = shown.length
    ? `${shown.length} plans · ${community.selectedOptions[0].textContent}`
    : filter === "mine"
      ? "Nothing here yet. Tap I’m in on a plan you like."
      : "No plans right now. Check back soon.";
  for (const p of shown) {
    const card = el("article", "plan-card");
    card.dataset.id = p.id;

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
    const closed = p.ended || p.status === "cancelled";
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
                ? "Goal reached · Organiser confirmation pending"
                : `${minimum - count} more neighbours to reach the goal`,
      ),
    );
    const actions = el("div", "plan-actions");
    for (const [choice, label] of [
      [
        "in",
        p.my_vote === "in"
          ? p.my_place === "waitlisted"
            ? "Waitlisted ✓"
            : "I’m in ✓"
          : p.status === "confirmed" &&
              p.capacity &&
              Number(p.attending) >= p.capacity
            ? "Join waitlist"
            : "I’m in",
      ],
      ["pass", p.my_vote === "pass" ? "Meh! ✓" : "Meh!"],
    ]) {
      const button = el(
        "button",
        choice === "in" ? "plan-in" : "plan-pass",
        label,
      );
      button.setAttribute("aria-pressed", String(p.my_vote === choice));
      button.disabled = closed && p.my_vote !== choice;
      button.onclick = () =>
        vote(p, p.my_vote === choice ? "clear" : choice, card);
      actions.append(button);
    }
    card.append(actions);
    if (p.confirmation_note)
      card.append(el("p", "agreement-terms", p.confirmation_note));
    if (p.status === "confirmed")
      card.append(
        el(
          "p",
          "plan-place",
          `${p.attending || 0}${p.capacity ? " / " + p.capacity : ""} attending · ${p.waitlisted || 0} waitlisted${p.my_vote === "in" ? (p.my_place === "attending" ? " · Your place is confirmed ✓" : " · You’re on the waitlist") : ""}`,
        ),
      );
    if (p.mine && !p.ended && p.status !== "cancelled") {
      const manage = el("details", "plan-organiser");
      manage.append(el("summary", "", "Your organiser controls"));
      const group = el("div", "hood-inline-actions");
      if (p.status === "gathering") {
        const confirm = el("button", "", "Confirm this plan ✓");
        confirm.onclick = () => heyhoodManagePlan(p, "confirm", load);
        group.append(confirm);
      }
      const cancel = el("button", "", "Cancel plan");
      cancel.onclick = () => heyhoodManagePlan(p, "cancel", load);
      group.append(cancel);
      manage.append(group);
      card.append(manage);
    }
    card.append(
      el(
        "small",
        "plan-disclaimer",
        p.is_demo
          ? "Demo plan · not a confirmed event"
          : p.status === "confirmed"
            ? "Organiser confirmed"
            : p.status === "cancelled"
              ? "Plan cancelled"
              : p.ended
                ? "Interest closed"
                : "Interest only · not a booking",
      ),
    );
    const footer = el("div", "plan-footer");
    footer.append(
      heyhoodSaveButton("plan", p.id),
      card.querySelector(".plan-disclaimer"),
    );
    if (p.my_vote === "in" && heyhoodResident() && !closed && !p.is_demo) {
      const remind = el(
        "button",
        "plan-reminder",
        p.reminder_enabled ? "Reminder on ✓" : "Remind me 🔔",
      );
      remind.setAttribute("aria-pressed", String(p.reminder_enabled));
      remind.onclick = async () => {
        remind.disabled = true;
        try {
          await heyhoodModuleApi(`plans/${p.id}/reminder`, {
            method: "POST",
            body: JSON.stringify({ enabled: !p.reminder_enabled }),
          });
          await load();
          heyhoodNotice(
            "Reminders show up in My stuff, within two hours of a confirmed plan.",
          );
        } catch (e) {
          heyhoodNotice(e.message);
          remind.disabled = false;
        }
      };
      footer.append(remind);
    }
    card.append(footer);
    grid.append(card);
  }
}
async function vote(p, choice, card) {
  if (!p.is_demo && choice !== "clear" && !heyhoodRequireMember()) return;
  if (!p.is_demo && choice === "clear" && !heyhoodResident()) {
    heyhoodRequireMember();
    return;
  }
  const slug = community.value;
  card.querySelectorAll("button").forEach((b) => (b.disabled = true));
  try {
    const r = await fetch(`/api/${slug}/plans/${p.id}/vote`, {
      method: "POST",
      headers: heyhoodResidentHeaders(),
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
        ? "Saved. Check your card for interest, attendance or waitlist status."
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
  const q = new URLSearchParams(location.search);
  q.set("community", slug);
  history.replaceState(null, "", `?${q}`);
  try {
    const r = await fetch(`/api/${slug}/plans?visitorId=${visitor}`, {
      signal: controller.signal,
      headers: heyhoodResidentHeaders(),
    });
    if (!r.ok) throw Error();
    const data = await r.json();
    if (slug !== community.value) return;
    plans = data;
    render();
    const target = new URLSearchParams(location.search).get("id");
    if (target)
      [...grid.querySelectorAll(".plan-card")]
        .find((c) => c.dataset.id === target)
        ?.scrollIntoView({ block: "start" });
  } catch (e) {
    if (e.name !== "AbortError") {
      grid.replaceChildren();
      status.textContent = "Couldn’t load plans. ";
      const retry = el("button", "save-action", "Retry ↻");
      retry.onclick = load;
      status.append(retry);
    }
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
document.querySelector("#new-plan").onclick = () => heyhoodCreatePlan(load);
community.onchange = load;
load();
