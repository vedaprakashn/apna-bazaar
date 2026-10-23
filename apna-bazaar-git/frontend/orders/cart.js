(() => {
  const params = new URLSearchParams(location.search),
    slug = params.get("community") || "tridasa",
    provider = params.get("id"),
    key = `heyhood-cart:${slug}:${provider}`;
  let shop,
    items = [],
    busy = false,
    requestKey = crypto.randomUUID();
  function n(t, text) {
    const e = document.createElement(t);
    if (text != null) e.textContent = text;
    return e;
  }
  function save() {
    localStorage.setItem(key, JSON.stringify({ items, requestKey }));
  }
  function amount() {
    return (
      items.reduce((v, i) => v + Math.round(i.price * 100) * i.quantity, 0) /
      100
    );
  }
  const dialog = n("dialog");
  dialog.className = "cart-dialog";
  dialog.setAttribute("aria-label", "Your order cart");
  dialog.innerHTML =
    '<div class="cart-head"><h2>A little goodness, lined up.</h2><button type="button" aria-label="Close cart">×</button></div><p class="cart-note">One shop per order. A request becomes confirmed only when the provider accepts. Pay the provider directly.</p><div id="cart-lines"></div><div class="cart-summary"><span>Order total</span><span id="cart-total"></span></div><form id="cart-form"><label>Fulfilment<select id="cart-fulfil"><option value="Pickup">Pickup</option></select></label><label>Date · IST<input id="cart-date" type="date" required></label><label>Preferred time · IST<input id="cart-time" type="time" required></label><label>Note for the provider<textarea id="cart-note" maxlength="500" rows="2"></textarea></label><label class="cart-check"><input id="cart-confirm" type="checkbox" required>I’ve reviewed the items, total and timing, and agree to share my profile name and flat with this provider to fulfil this order.</label><button class="cart-launch" type="submit">Place order request ↗</button></form><p id="cart-status" class="cart-status" role="status"></p><a id="cart-signin" hidden>Sign in / verify your profile ↗</a>';
  document.body.append(dialog);
  dialog.querySelector(".cart-head button").onclick = () => dialog.close();
  const $ = (id) => dialog.querySelector("#" + id);
  function status(s) {
    $("cart-status").textContent = s;
  }
  function render() {
    const list = $("cart-lines");
    list.replaceChildren();
    for (const item of items) {
      const row = n("div");
      row.className = "cart-line";
      const info = n("div", `${item.name} · ₹${item.price} each`),
        qty = n("input");
      qty.type = "number";
      qty.min = 1;
      qty.max = 100;
      qty.value = item.quantity;
      qty.setAttribute("aria-label", `Quantity for ${item.name}`);
      qty.onchange = () => {
        item.quantity = Math.max(1, Math.min(100, Number(qty.value) || 1));
        requestKey = crypto.randomUUID();
        save();
        render();
      };
      const remove = n("button", "×");
      remove.type = "button";
      remove.setAttribute("aria-label", `Remove ${item.name}`);
      remove.onclick = () => {
        items = items.filter((i) => i.id !== item.id);
        requestKey = crypto.randomUUID();
        save();
        render();
      };
      row.append(info, qty, remove);
      list.append(row);
    }
    if (!items.length)
      list.append(n("p", "Choose something from this shop to get started."));
    $("cart-total").textContent = `₹${amount().toLocaleString("en-IN")}`;
    dialog.querySelector("[type=submit]").disabled = !items.length || busy;
    const contact = document.getElementById("contact");
    if (contact)
      contact.textContent = items.length
        ? `View cart · ${items.reduce((s, i) => s + i.quantity, 0)} items`
        : "View cart ↗";
  }
  window.heyhoodCartOpen = () => {
    render();
    dialog.showModal();
  };
  window.heyhoodCartAdd = (item) => {
    if (item.price == null || !Number.isFinite(Number(item.price))) {
      status("Ask the provider to add a price before ordering this item.");
      dialog.showModal();
      return;
    }
    const existing = items.find((i) => i.id === item.id);
    if (existing) existing.quantity = Math.min(100, existing.quantity + 1);
    else {
      if (items.length >= 20) {
        status("Up to 20 different items per order.");
        dialog.showModal();
        return;
      }
      items.push({
        id: item.id,
        name: item.name,
        price: Number(item.price),
        quantity: 1,
      });
    }
    requestKey = crypto.randomUUID();
    save();
    render();
    dialog.showModal();
  };
  window.heyhoodInitCart = (data) => {
    shop = data;
    try {
      const old = JSON.parse(localStorage.getItem(key));
      if (old) {
        items = old.items || [];
        requestKey = old.requestKey || requestKey;
      }
    } catch {}
    let changed = false;
    items = items.filter((i) => {
      const current = data.offerings.find((o) => o.id === i.id);
      if (!current || current.price == null) return false;
      if (i.price !== Number(current.price)) {
        changed = true;
        i.price = Number(current.price);
      }
      i.name = current.name;
      return true;
    });
    if (changed) {
      requestKey = crypto.randomUUID();
      status(
        "Prices were updated. Review this cart before placing your request.",
      );
    }
    save();
    if (data.provider.delivery_enabled) {
      const option = n("option", "Delivery");
      option.value = "Delivery";
      $("cart-fulfil").append(option);
    }
    const date = new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Kolkata",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).format(new Date());
    $("cart-date").value = date;
    $("cart-date").min = date;
    $("cart-signin").href =
      `../activity/index.html?community=${encodeURIComponent(slug)}&view=profile`;
    render();
  };
  $("cart-form").onsubmit = async (e) => {
    e.preventDefault();
    if (busy || !items.length) return;
    if (!window.heyhoodResident?.()?.token) {
      status(
        "Sign in and verify your resident profile to submit. Your cart is saved on this device.",
      );
      $("cart-signin").hidden = false;
      return;
    }
    busy = true;
    render();
    try {
      const res = await fetch(`/api/${encodeURIComponent(slug)}/orders`, {
        method: "POST",
        headers: heyhoodResidentHeaders(),
        body: JSON.stringify({
          key: requestKey,
          providerId: provider,
          fulfilment: $("cart-fulfil").value,
          date: $("cart-date").value,
          time: $("cart-time").value,
          note: $("cart-note").value,
          confirmed: $("cart-confirm").checked,
          lines: items.map((i) => ({
            offeringId: i.id,
            quantity: i.quantity,
            price: i.price,
          })),
        }),
      });
      const data = await res.json();
      if (!res.ok)
        throw Error(
          data.error ||
            "Could not place this request. Check your profile and order details.",
        );
      items = [];
      save();
      location.href = `../orders/index.html?community=${encodeURIComponent(slug)}&order=${data.id}`;
    } catch (e) {
      status(e.message);
      $("cart-signin").hidden = false;
    } finally {
      busy = false;
      render();
    }
  };
})();
