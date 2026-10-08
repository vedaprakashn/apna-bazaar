const community = document.querySelector("#community");
const search = document.querySelector("#search");
const category = document.querySelector("#category");
const directory = document.querySelector("#directory");
const status = document.querySelector("#status");
const tabs = [...document.querySelectorAll("[role=tab]")];
let contacts = [],
  section = "urgent",
  controller;
const params = new URLSearchParams(location.search);
const selected =
  params.get("community") || sessionStorage.getItem("heyhood-community");
if (["tridasa", "sayuk"].includes(selected)) community.value = selected;
function el(tag, cls, text) {
  const n = document.createElement(tag);
  n.className = cls || "";
  if (text != null) n.textContent = text;
  return n;
}
function render() {
  directory.replaceChildren();
  const q = search.value.normalize("NFKC").toLowerCase().trim();
  const results = contacts.filter(
    (c) =>
      c.section === section &&
      (!category.value || c.category === category.value) &&
      (!q ||
        [
          c.name,
          c.category,
          c.service_area,
          c.location,
          c.notes,
          c.specialty,
          c.flat_number,
        ]
          .join(" ")
          .normalize("NFKC")
          .toLowerCase()
          .includes(q)),
  );
  status.textContent = results.length
    ? `${results.length} ${results.length === 1 ? "contact" : "contacts"} · ${community.selectedOptions[0].textContent}`
    : "No contacts match. Try another service or clear your search.";
  for (const c of results) {
    const demo = c.is_demo === true;
    const card = el(
      "article",
      `contact-card ${c.section === "urgent" ? "response-card" : "resident-card"}`,
    );
    const top = el("div", "contact-top");
    const symbols = {
      Doctors: "🩺",
      Nurses: "✚",
      Lawyers: "⚖",
      Physiotherapists: "↗",
      "First aid": "✚",
      Police: "🛡",
      "Fire services": "🚒",
      "Snake rescue": "🐍",
      Ambulance: "🚑",
    };
    const icon = el("span", "contact-avatar", symbols[c.category] || "✦");
    icon.setAttribute("aria-hidden", "true");
    const identity = el("div", "contact-identity");
    identity.append(
      el("span", "contact-category", c.category),
      el("h2", "", c.name.replace(/ · Demo$/, "")),
    );
    top.append(icon, identity);
    card.append(top);
    if (c.specialty)
      card.append(el("strong", "contact-specialty", c.specialty));
    const badges = el("div", "contact-badges");
    if (c.flat_number)
      badges.append(el("span", "flat-badge", `Flat ${c.flat_number}`));
    badges.append(
      el(
        "span",
        "scope-badge",
        c.scope === "community" ? "Your hood" : "Nearby response",
      ),
    );
    card.append(badges);
    const info = el("div", "contact-info");
    info.append(el("p", "availability", `◷ ${c.availability}`));
    card.append(info);
    const footer = el("div", "contact-bottom");
    const verified = !demo && c.verified_at && c.consent_to_listing;
    footer.append(
      el(
        "span",
        verified ? "verification verified" : "verification demo",
        demo ? "Demo" : verified ? "Verified listing" : "Unverified listing",
      ),
    );
    if (verified && /^\+?[0-9 ()-]{7,25}$/.test(c.phone || "")) {
      const call = el("a", "contact-call", `Call ↗`);
      call.href = `tel:${c.phone.replace(/[^+0-9]/g, "")}`;
      call.setAttribute("aria-label", `Call ${c.name} at ${c.phone}`);
      footer.append(call);
    } else footer.append(el("span", "contact-unavailable", "Not live"));
    card.append(footer);
    if (
      c.section === "professional" &&
      ((demo && c.whatsapp_number === "919740893534") || verified)
    ) {
      const contact = heyhoodWhatsAppContact(
        c.whatsapp_number,
        null,
        heyhoodEnquiry(
          c.name,
          community.selectedOptions[0].textContent,
          c.flat_number,
          "",
        ),
      );
      if (contact) {
        const wa = el("a", "professional-whatsapp", "WhatsApp ↗");
        wa.href = contact.url;
        card.append(wa);
        if (demo)
          card.append(
            el("small", "pilot-contact-note", "Test WhatsApp number"),
          );
      }
    }
    const details = el("details", "contact-details");
    details.append(
      el("summary", "", "Good to know"),
      el("p", "contact-notes", c.notes),
      el("p", "contact-area", c.service_area),
    );
    if (c.location) details.append(el("p", "", c.location));
    if (verified)
      details.append(
        el(
          "small",
          "",
          `Verified ${new Date(c.verified_at).toLocaleDateString("en-IN")}`,
        ),
      );
    card.append(details);
    directory.append(card);
  }
}
function categories() {
  category.replaceChildren(new Option("All services", ""));
  for (const c of [
    ...new Set(
      contacts.filter((c) => c.section === section).map((c) => c.category),
    ),
  ].sort())
    category.append(new Option(c, c));
}
function selectSection(index, focus = false) {
  section = index === 0 ? "urgent" : "professional";
  document.body.dataset.section = section;
  tabs.forEach((t, i) => {
    t.setAttribute("aria-selected", String(i === index));
    t.tabIndex = i === index ? 0 : -1;
  });
  directory.setAttribute("aria-labelledby", tabs[index].id);
  document.querySelector("#section-note").textContent =
    index === 0
      ? "Response contacts. Demo numbers aren’t live."
      : "Care, recovery and advice—from people in your hood.";
  categories();
  render();
  if (focus) tabs[index].focus();
}
tabs.forEach((t, i) => {
  t.onclick = () => selectSection(i);
  t.onkeydown = (e) => {
    if (["ArrowRight", "ArrowLeft", "Home", "End"].includes(e.key)) {
      e.preventDefault();
      selectSection(e.key === "Home" ? 0 : e.key === "End" ? 1 : 1 - i, true);
    }
  };
});
search.oninput = render;
category.onchange = render;
async function load() {
  controller?.abort();
  controller = new AbortController();
  contacts = [];
  directory.replaceChildren();
  status.textContent = "Loading contacts…";
  const slug = community.value;
  history.replaceState(null, "", `?community=${encodeURIComponent(slug)}`);
  try {
    const r = await fetch(`/api/${encodeURIComponent(slug)}/help-contacts`, {
      signal: controller.signal,
    });
    if (!r.ok) throw Error();
    const data = await r.json();
    if (slug !== community.value) return;
    contacts = data.contacts || [];
    categories();
    render();
  } catch (e) {
    if (e.name === "AbortError") return;
    status.textContent =
      "The local directory couldn’t load. You can still call 112 above in a real emergency.";
  }
}
community.onchange = load;
load();
