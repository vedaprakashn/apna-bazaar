(() => {
  const params = new URLSearchParams(location.search),
    slug = params.get("community") || "tridasa",
    focus = params.get("order"),
    list = document.getElementById("consumer-order-list"),
    status = document.getElementById("orders-status");
  let busy = false;
  const codes = new Map();
  const labels = {
    requested: "Waiting for the provider",
    accepted: "Accepted",
    preparing: "In the making",
    ready: "Ready for handover",
    completed: "Handed over",
    rejected: "Declined",
    cancelled: "Cancelled",
    expired: "Request expired",
  };
  function n(t, text) {
    const e = document.createElement(t);
    if (text != null) e.textContent = text;
    return e;
  }
  async function api(path, method = "GET") {
    const res = await fetch(`/api/${encodeURIComponent(slug)}/orders${path}`, {
      method,
      headers: heyhoodResidentHeaders(),
    });
    const data = await res.json();
    if (!res.ok)
      throw Error(
        data.error ||
          "Sign in and verify your resident profile to view your orders.",
      );
    return data;
  }
  function button(text, fn) {
    const b = n("button", text);
    b.onclick = async () => {
      b.disabled = true;
      try {
        await fn();
      } catch (e) {
        status.textContent = e.message;
      } finally {
        b.disabled = false;
      }
    };
    return b;
  }
  async function load() {
    if (busy || document.hidden) return;
    busy = true;
    try {
      const orders = await api("/mine");
      list.replaceChildren();
      status.textContent = "";
      if (!orders.length)
        list.append(
          n("p", "No orders yet. Open a shop to find your next good thing."),
        );
      for (const o of orders) {
        const card = n("article");
        card.className = "consumer-order";
        card.id = "order-" + o.id;
        card.append(
          n("h2", `${o.reference} · ${labels[o.status] || o.status}`),
          n(
            "p",
            `${o.fulfilment} · ${new Date(o.fulfilment_at).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })} IST · ${o.fulfilment === "Pickup" ? "Pickup at " + o.pickup_flat : "Your flat " + o.customer_flat}`,
          ),
        );
        for (const [name, qty, price] of o.items)
          card.append(n("p", `${qty}× ${name} · ₹${price} each`));
        card.append(
          n(
            "strong",
            `Order total ₹${Number(o.total).toLocaleString("en-IN")}`,
          ),
        );
        if (o.reason) card.append(n("p", o.reason));
        const shop = n("a", "View shop ↗");
        shop.href = `../provider/index.html?community=${encodeURIComponent(slug)}&id=${o.provider_id}`;
        card.append(shop);
        if (["accepted", "preparing", "ready"].includes(o.status)) {
          const box = n("div");
          box.className = "consumer-code-box";
          box.textContent =
            "Keep your handover code private. Share it only after you receive the order.";
          if (codes.has(o.id))
            box.replaceChildren(
              n("span", "Share only after receiving your order"),
              n("strong", codes.get(o.id)),
            );
          card.append(box);
          card.append(
            button("Reveal handover code", async () => {
              const data = await api("/" + o.id + "/code");
              codes.set(o.id, data.code);
              box.replaceChildren(
                n("span", "Share only after receiving your order"),
                n("strong", data.code),
              );
            }),
            button("Renew code", async () => {
              const data = await api("/" + o.id + "/code", "POST");
              codes.set(o.id, data.code);
              box.replaceChildren(
                n("span", "New code · the previous code no longer works"),
                n("strong", data.code),
              );
            }),
          );
        }
        if (o.status === "requested")
          card.append(
            button("Cancel request", async () => {
              if (confirm("Cancel this order request?")) {
                await api("/" + o.id + "/cancel", "POST");
                busy = false;
                await load();
              }
            }),
          );
        list.append(card);
      }
      if (focus && !load.focused) {
        document
          .getElementById("order-" + focus)
          ?.scrollIntoView({ block: "center" });
        load.focused = true;
      }
    } catch (e) {
      status.textContent = e.message;
      const a = n("a", "Sign in / verify profile ↗");
      a.href = `../activity/index.html?community=${encodeURIComponent(slug)}&view=profile`;
      list.replaceChildren(a);
    } finally {
      busy = false;
    }
  }
  load();
  setInterval(load, 15000);
  addEventListener("focus", load);
})();
