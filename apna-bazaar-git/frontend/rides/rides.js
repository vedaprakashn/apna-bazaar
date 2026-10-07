const community = document.querySelector("#community"),
  grid = document.querySelector("#rides"),
  status = document.querySelector("#status"),
  destination = document.querySelector("#destination"),
  direction = document.querySelector("#direction"),
  around = document.querySelector("#around"),
  sheet = document.querySelector("#post-sheet"),
  form = document.querySelector("#post-form");
let visitor = localStorage.getItem("heyhood-ride-visitor");
if (
  !visitor ||
  !/^([0-9a-f]{8}-)([0-9a-f]{4}-){3}[0-9a-f]{12}$/i.test(visitor)
) {
  visitor = crypto.randomUUID();
  localStorage.setItem("heyhood-ride-visitor", visitor);
}
const params = new URLSearchParams(location.search),
  selected =
    params.get("community") || sessionStorage.getItem("heyhood-community");
if (["tridasa", "sayuk"].includes(selected)) community.value = selected;
if (params.get("destination")) destination.value = params.get("destination");
if (params.get("direction") === "inbound") direction.value = "inbound";
if (params.get("at") && !isNaN(new Date(params.get("at"))))
  around.value = istInput(new Date(params.get("at")));
let minimumSeats = Math.max(1, Math.min(6, Number(params.get("seats")) || 1));
const seatFilter = document.querySelector("#seat-filter");
seatFilter.hidden = minimumSeats === 1;
seatFilter.textContent = `${minimumSeats}+ seats ×`;
seatFilter.onclick = () => {
  minimumSeats = 1;
  seatFilter.hidden = true;
  load();
};
let kind = params.get("kind") === "request" ? "request" : "offer",
  rides = [],
  controller,
  timer,
  postKind = "request",
  postId,
  posting = false;
function el(tag, cls, text) {
  const n = document.createElement(tag);
  n.className = cls || "";
  if (text != null) n.textContent = text;
  return n;
}
function istInput(date) {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Kolkata",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    })
      .formatToParts(date)
      .map((p) => [p.type, p.value]),
  );
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`;
}
function when(value) {
  return value ? new Date(value + "+05:30").toISOString() : null;
}
function render() {
  grid.replaceChildren();
  status.textContent = rides.length
    ? `${rides.length} ${kind === "offer" ? "ride offers" : "ride requests"}`
    : "No match yet. Try another time, or post what you need.";
  for (const r of rides) {
    const card = el("article", "ride-card");
    const route =
      r.direction === "outbound"
        ? `${community.selectedOptions[0].textContent} → ${r.destination}`
        : `${r.destination} → ${community.selectedOptions[0].textContent}`;
    card.append(
      el(
        "span",
        "ride-kind",
        r.kind === "offer" ? "RIDE OFFER" : "NEEDS A RIDE",
      ),
      el("h2", "", route),
    );
    card.append(
      el(
        "p",
        "ride-when",
        new Intl.DateTimeFormat("en-IN", {
          timeZone: "Asia/Kolkata",
          dateStyle: "medium",
          timeStyle: "short",
        }).format(new Date(r.departure_at)) + " IST",
      ),
    );
    card.append(
      el(
        "p",
        "ride-person",
        `${r.name}${r.flat_number ? " · Flat " + r.flat_number : ""}`,
      ),
    );
    const badges = el("div", "ride-badges");
    badges.append(
      el(
        "span",
        "",
        `${r.seats} ${r.kind === "offer" ? "seats offered" : "seats needed"}`,
      ),
      el("span", "", `Meet: ${r.pickup}`),
    );
    card.append(badges);
    if (r.notes && !r.is_demo) card.append(el("p", "ride-notes", r.notes));
    const contact = heyhoodWhatsAppContact(
      r.whatsapp_number,
      null,
      heyhoodRideEnquiry(r, community.selectedOptions[0].textContent),
    );
    if (contact) {
      const link = el(
        "a",
        "ride-connect",
        r.kind === "offer" ? "Connect on WhatsApp ↗" : "I can help ↗",
      );
      link.href = contact.url;
      card.append(link);
    }
    if (r.mine) {
      const close = el("button", "ride-close", "Close my post");
      close.onclick = async () => {
        close.disabled = true;
        try {
          const response = await fetch(
            `/api/${community.value}/rides/${r.id}/close`,
            {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ visitorId: visitor }),
            },
          );
          if (!response.ok) throw Error();
          await load();
        } catch (_) {
          close.disabled = false;
          status.textContent = "Couldn’t close that post. Try again.";
        }
      };
      card.append(close);
    }
    card.append(
      el(
        "small",
        "ride-disclaimer",
        "Pilot · discuss first, ride not confirmed",
      ),
    );
    grid.append(card);
  }
}
async function load() {
  controller?.abort();
  controller = new AbortController();
  const slug = community.value;
  status.textContent = "Finding your way…";
  const q = new URLSearchParams({
    kind,
    direction: direction.value,
    destination: destination.value,
    visitorId: visitor,
    seats: String(minimumSeats),
  });
  if (around.value) q.set("at", when(around.value));
  document
    .querySelectorAll("[data-kind]")
    .forEach((b) =>
      b.setAttribute("aria-pressed", String(b.dataset.kind === kind)),
    );
  document.querySelector("#back").href =
    `../chatbot/index.html?community=${slug}`;
  try {
    const response = await fetch(`/api/${slug}/rides?${q}`, {
      signal: controller.signal,
    });
    if (!response.ok) throw Error();
    const data = await response.json();
    if (slug !== community.value) return;
    rides = data;
    render();
  } catch (e) {
    if (e.name !== "AbortError") {
      grid.replaceChildren();
      status.textContent = "Couldn’t load rides. Try refreshing.";
    }
  }
}
document.querySelectorAll("[data-kind]").forEach(
  (b) =>
    (b.onclick = () => {
      kind = b.dataset.kind;
      load();
    }),
);
destination.oninput = () => {
  clearTimeout(timer);
  timer = setTimeout(load, 300);
};
direction.onchange = load;
around.onchange = load;
community.onchange = () => {
  sheet.close();
  load();
};
function openPost(type) {
  postKind = type;
  postId = crypto.randomUUID();
  form.reset();
  document.querySelector("#post-title").textContent =
    type === "offer" ? "Got a spare seat?" : "Need a ride?";
  document.querySelector("#post-submit").textContent =
    type === "offer" ? "Post offer ↗" : "Post request ↗";
  document.querySelector("#post-destination").value = destination.value;
  document.querySelector("#post-direction").value = direction.value;
  document.querySelector("#post-time").min = istInput(
    new Date(Date.now() + 60000),
  );
  document.querySelector("#post-time").value =
    around.value || istInput(new Date(Date.now() + 3600000));
  document.querySelector("#post-status").textContent = "";
  sheet.showModal();
  document.querySelector("#post-destination").focus();
}
document
  .querySelectorAll("[data-new]")
  .forEach((b) => (b.onclick = () => openPost(b.dataset.new)));
document.querySelector("#close-sheet").onclick = () => {
  if (!posting) sheet.close();
};
sheet.addEventListener("cancel", (e) => {
  if (posting) e.preventDefault();
});
form.onsubmit = async (e) => {
  e.preventDefault();
  if (posting) return;
  posting = true;
  const submit = document.querySelector("#post-submit"),
    feedback = document.querySelector("#post-status");
  submit.disabled = true;
  community.disabled = true;
  document.querySelector("#close-sheet").disabled = true;
  const slug = community.value;
  const body = {
    id: postId,
    visitorId: visitor,
    kind: postKind,
    direction: document.querySelector("#post-direction").value,
    destination: document.querySelector("#post-destination").value,
    departureAt: when(document.querySelector("#post-time").value),
    seats: Number(document.querySelector("#post-seats").value),
    pickup: document.querySelector("#post-pickup").value,
    name: document.querySelector("#post-name").value.trim() || "Neighbour",
    flatNumber: document.querySelector("#post-flat").value,
    notes: document.querySelector("#post-notes").value,
    publish: document.querySelector("#publish").checked,
  };
  try {
    const response = await fetch(`/api/${slug}/rides`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    const result = await response.json();
    if (!response.ok)
      throw Error(result.error || "Couldn’t publish. Please retry.");
    sheet.close();
    kind = postKind;
    destination.value = body.destination;
    direction.value = body.direction;
    around.value = "";
    await load();
    status.textContent = "Posted in Hood Rides. No broadcast sent.";
  } catch (error) {
    feedback.textContent = error.message;
  } finally {
    posting = false;
    submit.disabled = false;
    community.disabled = false;
    document.querySelector("#close-sheet").disabled = false;
  }
};
load();
