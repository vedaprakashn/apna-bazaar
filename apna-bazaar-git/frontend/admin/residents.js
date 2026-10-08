const status = document.querySelector("#status"),
  community = document.querySelector("#community");
let offerings = [];
function el(tag, text) {
  const n = document.createElement(tag);
  if (text) n.textContent = text;
  return n;
}
async function api(path, method = "GET", body) {
  const r = await fetch(`/api/${community.value}/admin/residents${path}`, {
    method,
    headers: {
      Authorization: "Bearer " + document.querySelector("#operator-key").value,
      "Content-Type": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });
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
    const [people, stock] = await Promise.all([api(""), api("/availability")]);
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
