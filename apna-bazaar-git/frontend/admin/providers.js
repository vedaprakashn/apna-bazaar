(() => {
  const $ = (id) => document.getElementById(id);
  let key = "",
    slug = "",
    busy = false;
  function status(msg) {
    $("status").textContent = msg;
  }
  async function api(path, method = "GET", body) {
    const r = await fetch(
      `/api/${encodeURIComponent(slug)}/admin/providers${path}`,
      {
        method,
        headers: {
          Authorization: `Bearer ${key}`,
          "Content-Type": "application/json",
        },
        body: body === undefined ? undefined : JSON.stringify(body),
      },
    );
    const d = await r.json().catch(() => ({}));
    if (!r.ok)
      throw Error(
        d.detail ||
          d.message ||
          (r.status === 503
            ? "Configure CAMPAIGN_ADMIN_TOKEN on the Railway app service first."
            : r.status === 401
              ? "Check your operator key."
              : "Could not complete that action."),
      );
    return d;
  }
  async function load() {
    const providers = await api("");
    $("provider").replaceChildren();
    for (const p of providers) {
      const o = document.createElement("option");
      o.value = p.id;
      o.textContent = `${p.shop} · ${p.name} · ${p.flat || "—"}`;
      $("provider").append(o);
    }
    $("tools").hidden = false;
  }
  function links(d) {
    const publicUrl = new URL(
      `../provider/index.html?community=${encodeURIComponent(slug)}&id=${d.providerId}`,
      location.href,
    ).href;
    const edit = new URL(
      `../seller/console.html?community=${encodeURIComponent(slug)}&id=${d.providerId}#key=${encodeURIComponent(d.token)}`,
      location.href,
    ).href;
    $("public-link").href = $("public-link").textContent = publicUrl;
    $("private-link").href = $("private-link").textContent = edit;
    $("links").hidden = false;
    $("links").scrollIntoView({ behavior: "smooth" });
  }
  async function run(action) {
    if (busy) return;
    busy = true;
    for (const b of document.querySelectorAll("form button")) b.disabled = true;
    try {
      await action();
    } catch (e) {
      status(e.message);
    } finally {
      busy = false;
      for (const b of document.querySelectorAll("form button"))
        b.disabled = false;
    }
  }
  $("operator").onsubmit = (e) => {
    e.preventDefault();
    key = $("key").value;
    slug = $("community").value;
    $("tools").hidden = $("links").hidden = true;
    run(async () => {
      await load();
      status("Choose an existing shop or create a new one.");
    });
  };
  $("existing").onsubmit = (e) => {
    e.preventDefault();
    if (!$("provider").value) return;
    if (
      !confirm(
        "Replace this provider’s private edit link? The old link will stop working.",
      )
    )
      return;
    run(async () => {
      links(await api(`/${$("provider").value}/edit-link`, "POST"));
      status("New private edit link issued. Send it only to the provider.");
    });
  };
  $("new").onsubmit = (e) => {
    e.preventDefault();
    if (!confirm("Create this provider in the selected community?")) return;
    run(async () => {
      links(await api("", "POST", Object.fromEntries(new FormData(e.target))));
      await load();
      status(
        "Provider created. They can build and publish their offerings using the private link.",
      );
      e.target.reset();
    });
  };
  $("copy").onclick = async () => {
    try {
      await navigator.clipboard.writeText($("private-link").href);
      status("Private edit link copied. Deliver it directly to the owner.");
    } catch {
      status("Copy the private link shown above.");
    }
  };
})();
