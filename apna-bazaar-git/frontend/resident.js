(() => {
  const community = () =>
    new URLSearchParams(location.search).get("community") ||
    sessionStorage.getItem("heyhood-community") ||
    document.querySelector("#community")?.value ||
    "tridasa";
  const key = () => `heyhood-resident:${community()}`;
  window.heyhoodResident = () => {
    try {
      return JSON.parse(localStorage.getItem(key()));
    } catch {
      return null;
    }
  };
  window.heyhoodStoreResident = (p) =>
    localStorage.setItem(key(), JSON.stringify(p));
  window.heyhoodResidentHeaders = () => ({
    "Content-Type": "application/json",
    ...(heyhoodResident()?.token
      ? { Authorization: `Bearer ${heyhoodResident().token}` }
      : {}),
  });
  window.heyhoodActivityUrl = (query = {}) =>
    `../activity/index.html?${new URLSearchParams({ community: community(), ...query })}`;
  window.heyhoodNotice = (text) => {
    let n = document.querySelector("#resident-notice");
    if (!n) {
      n = document.createElement("div");
      n.id = "resident-notice";
      n.className = "resident-notice";
      n.setAttribute("role", "status");
      document.body.append(n);
    }
    n.textContent = text;
    n.hidden = false;
    clearTimeout(n.timer);
    n.timer = setTimeout(() => (n.hidden = true), 5000);
  };
  window.heyhoodResidentFetch = async (path, options = {}) => {
    const r = await fetch(`/api/${community()}/resident/${path}`, {
      ...options,
      headers: heyhoodResidentHeaders(),
    });
    if (!r.ok) {
      if (r.status === 401)
        throw Error("Create or restore your resident profile in My activity.");
      if (r.status === 429)
        throw Error("A little breather—try again in a minute.");
      if (r.status === 422)
        throw Error("Keep it respectful, neighbour. Please change that text.");
      throw Error(
        r.status === 503
          ? "Temporarily unavailable. Your draft is safe—please retry."
          : "Could not complete that action. Check the details and retry.",
      );
    }
    const text = await r.text();
    return text ? JSON.parse(text) : null;
  };
  window.heyhoodSkeletons = (container) => {
    container.replaceChildren();
    for (let i = 0; i < 3; i++) {
      const n = document.createElement("div");
      n.className = "loading-skeleton";
      n.setAttribute("aria-hidden", "true");
      container.append(n);
    }
  };
  let cached;
  async function saved() {
    if (!heyhoodResident()) return [];
    cached ||= heyhoodResidentFetch("activity")
      .then((r) => r.saved)
      .catch(() => []);
    return cached;
  }
  window.heyhoodSaveButton = (kind, id, follow = false) => {
    const b = document.createElement("button");
    b.type = "button";
    b.className = "save-action";
    let selected = false;
    function label() {
      b.textContent = selected
        ? follow
          ? "Following ✓"
          : "Saved ✓"
        : follow
          ? "Follow updates"
          : "Save ♡";
      b.setAttribute("aria-pressed", String(selected));
    }
    label();
    function refresh() {
      return saved().then((items) => {
        const item = items.find((s) => s.kind === kind && s.entity_id === id);
        selected = Boolean(item && (!follow || item.following));
        label();
      });
    }
    b.disabled = Boolean(heyhoodResident());
    refresh().finally(() => (b.disabled = false));
    window.addEventListener("heyhood-saves-changed", () => {
      if (b.isConnected) refresh();
    });
    b.onclick = async () => {
      if (!heyhoodResident()) {
        location.href = heyhoodActivityUrl({
          view: "profile",
          return: location.pathname + location.search,
        });
        return;
      }
      b.disabled = true;
      try {
        if (selected && !follow)
          await heyhoodResidentFetch(`saved/${kind}/${id}`, {
            method: "DELETE",
          });
        else
          await heyhoodResidentFetch("saved", {
            method: "POST",
            body: JSON.stringify({
              kind,
              entityId: id,
              following: follow ? !selected : false,
            }),
          });
        selected = !selected;
        cached = null;
        label();
        window.dispatchEvent(new Event("heyhood-saves-changed"));
        heyhoodNotice(
          selected
            ? follow
              ? "Following. Updates will appear in My activity."
              : "Saved to My activity."
            : "Updated your saved items.",
        );
      } catch (e) {
        heyhoodNotice(e.message);
      } finally {
        b.disabled = false;
      }
    };
    return b;
  };
  const offline = document.createElement("div");
  offline.className = "offline-banner";
  offline.setAttribute("role", "status");
  offline.textContent =
    "You’re offline. Your drafts are safe—reconnect to search or post.";
  function connectivity() {
    offline.hidden = navigator.onLine;
  }
  document.addEventListener("DOMContentLoaded", () => {
    document.body.append(offline);
    connectivity();
  });
  window.addEventListener("online", connectivity);
  window.addEventListener("offline", connectivity);
})();
