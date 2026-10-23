(() => {
  const $ = (id) => document.getElementById(id),
    names = {
      requested: "New order",
      accepted: "Accepted",
      preparing: "In the making",
      ready: "Ready to go",
      completed: "Handed over",
      rejected: "Declined",
    };
  const codes = {
    "HH-1048": "482913",
    "HH-1047": "715204",
    "HH-1046": "936125",
    "HH-1045": "280461",
    "HH-1044": "563890",
    "HH-1043": "824617",
    "HH-1042": "347928",
  };
  const seed = () => [
    {
      id: "HH-1048",
      name: "Ananya Rao",
      flat: "T2-308",
      initials: "AR",
      status: "requested",
      ago: "2 min ago",
      fulfil: "Pickup",
      time: "Today · 5:30–6:00 pm",
      items: [
        ["Mirchi bajji", 2, 50],
        ["Onion pakora", 1, 60],
      ],
      note: "A little less spicy, please. Picking up after my evening walk.",
    },
    {
      id: "HH-1047",
      name: "Rahul Mehta",
      flat: "T1-602",
      initials: "RM",
      status: "requested",
      ago: "6 min ago",
      fulfil: "Delivery",
      time: "Today · 6:00–6:30 pm",
      items: [
        ["Aloo samosa", 3, 40],
        ["Punugulu", 2, 50],
      ],
      note: "Please ring the bell. The intercom is acting up.",
    },
    {
      id: "HH-1046",
      name: "Divya Nair",
      flat: "T3-214",
      initials: "DN",
      status: "requested",
      ago: "9 min ago",
      fulfil: "Pickup",
      time: "Today · 6:30–7:00 pm",
      items: [["Onion pakora", 2, 60]],
      note: "An extra little pot of chutney would be lovely.",
    },
    {
      id: "HH-1045",
      name: "Karthik Reddy",
      flat: "T1-405",
      initials: "KR",
      status: "preparing",
      ago: "18 min ago",
      fulfil: "Pickup",
      time: "Today · 5:15–5:45 pm",
      items: [
        ["Mirchi bajji", 3, 50],
        ["Punugulu", 1, 50],
      ],
      note: "No special requests. Bring on the crunch!",
    },
    {
      id: "HH-1044",
      name: "Sneha Iyer",
      flat: "T2-110",
      initials: "SI",
      status: "ready",
      ago: "25 min ago",
      fulfil: "Pickup",
      time: "Today · 5:00–5:30 pm",
      items: [["Aloo samosa", 2, 40]],
      note: "I’ll pick it up from your flat.",
    },
    {
      id: "HH-1043",
      name: "Arjun Shah",
      flat: "T3-507",
      initials: "AS",
      status: "accepted",
      ago: "14 min ago",
      fulfil: "Delivery",
      time: "Today · 6:00–6:30 pm",
      items: [
        ["Mirchi bajji", 1, 50],
        ["Onion pakora", 1, 60],
      ],
      note: "",
    },
    {
      id: "HH-1042",
      name: "Meera Menon",
      flat: "T1-707",
      initials: "MM",
      status: "preparing",
      ago: "21 min ago",
      fulfil: "Pickup",
      time: "Today · 5:30–6:00 pm",
      items: [["Punugulu", 2, 50]],
      note: "",
    },
    {
      id: "HH-1041",
      name: "Priya Das",
      flat: "T2-410",
      initials: "PD",
      status: "completed",
      ago: "Earlier today",
      fulfil: "Pickup",
      time: "Today · 4:00–4:30 pm",
      items: [["Mirchi bajji", 2, 50]],
      note: "",
    },
    {
      id: "HH-1040",
      name: "Aditya Jain",
      flat: "T3-120",
      initials: "AJ",
      status: "rejected",
      ago: "Earlier today",
      fulfil: "Pickup",
      time: "Today · 4:00–4:30 pm",
      items: [["Aloo samosa", 1, 40]],
      note: "",
      reason: "Item sold out for this slot",
    },
  ];
  let orders = seed(),
    tab = "incoming",
    selected = "HH-1048",
    events = [],
    attempts = {};
  function n(tag, text, cls) {
    const el = document.createElement(tag);
    if (text !== undefined && text !== null) el.textContent = text;
    if (cls) el.className = cls;
    return el;
  }
  const total = (o) => o.items.reduce((s, i) => s + i[1] * i[2], 0),
    money = (v) => `₹${v.toLocaleString("en-IN")}`;
  const bucket = (o) =>
    o.status === "requested"
      ? "incoming"
      : ["completed", "rejected"].includes(o.status)
        ? "history"
        : "active";
  function badge(o) {
    return n("span", names[o.status], `status-badge ${o.status}`);
  }
  function toast(text) {
    $("order-toast").textContent = text;
    $("order-toast").hidden = false;
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => ($("order-toast").hidden = true), 4500);
  }
  function button(label, cls, fn) {
    const b = n("button", label, `order-btn ${cls || ""}`);
    b.type = "button";
    b.onclick = fn;
    return b;
  }
  function consumerEvent(o, title, body) {
    events.unshift({ id: o.id, name: o.name, title, body, status: o.status });
  }
  function move(o, status) {
    const allowed = {
      requested: ["accepted", "rejected"],
      accepted: ["preparing", "ready"],
      preparing: ["ready"],
      ready: ["completed"],
    };
    if (!allowed[o.status]?.includes(status)) return;
    o.status = status;
    tab = bucket(o);
    selected = o.id;
    if (status === "accepted") {
      consumerEvent(
        o,
        "You’re on the list ✨",
        "Crispy Corner accepted your order. Your handover code is ready—keep it to yourself until you receive your order.",
      );
      toast("Accepted! Your neighbour gets an in-app update. · demo");
    }
    if (status === "preparing") toast("A little kitchen magic is underway ✨");
    if (status === "ready") {
      consumerEvent(
        o,
        o.fulfil === "Pickup"
          ? "Your snacks are ready ✨"
          : "Ready for delivery ✨",
        o.fulfil === "Pickup"
          ? "Swing by T1-416 when you’re ready. Share your code only once you have your order."
          : "The shop has marked your order ready for delivery. This does not mean it has been delivered.",
      );
      toast("Ready to go. Consumer update added. · demo");
    }
    if (status === "completed") {
      consumerEvent(
        o,
        "Handed over. Happy snacking 💛",
        "Your handover code was verified. The order is complete. Payment is arranged directly with your provider.",
      );
      toast("Code checked. Order complete. Happy hood 💛");
    }
    render();
  }
  function reject(o) {
    const box = $("action-content");
    box.replaceChildren(
      n("p", "A LITTLE CHANGE OF PLANS", "eyebrow"),
      n("h2", "Can’t make this one?"),
      n(
        "p",
        "Let your neighbour know why. They’ll get an update so they aren’t left waiting.",
      ),
    );
    const label = n("label", "Reason for declining"),
      select = n("select");
    select.id = "reject-reason";
    label.htmlFor = select.id;
    for (const reason of [
      "Item sold out for this slot",
      "Can’t fulfil this time",
      "Shop is taking a break",
    ]) {
      const option = n("option", reason);
      select.append(option);
    }
    box.append(label, select);
    const row = n("div", null, "action-row");
    row.append(
      button("Keep order", "", () => $("action-dialog").close()),
      button("Decline order", "primary", () => {
        o.reason = select.value;
        move(o, "rejected");
        consumerEvent(
          o,
          "A little change of plans",
          `Crispy Corner couldn’t accept this order: ${o.reason.toLowerCase()}.`,
        );
        $("action-dialog").close();
        $("mobile-detail").close();
        toast("Consumer updated. Thanks for letting them know. · demo");
      }),
    );
    box.append(row);
    $("action-dialog").showModal();
  }
  function verify(o) {
    const box = $("action-content");
    box.replaceChildren(
      n("p", "THE FINAL LITTLE CHECK", "eyebrow"),
      n("h2", "Goodies handed over?"),
      n(
        "p",
        `Ask ${o.name.split(" ")[0]} for their six-digit handover code after they receive the order. It’s in their consumer order screen.`,
      ),
    );
    const form = n("form"),
      label = n("label", "Consumer’s handover code"),
      input = n("input");
    input.id = "handover-code";
    label.htmlFor = input.id;
    input.inputMode = "numeric";
    input.autocomplete = "off";
    input.pattern = "[0-9]{6}";
    input.maxLength = 6;
    input.required = true;
    input.className = "otp-field";
    input.placeholder = "••••••";
    input.setAttribute("aria-describedby", "otp-error");
    const error = n("p", "", "form-error");
    error.id = "otp-error";
    error.setAttribute("role", "alert");
    const submit = button("Verify & complete ↗", "primary");
    submit.type = "submit";
    form.append(label, input, error, submit);
    form.onsubmit = (e) => {
      e.preventDefault();
      if (o.status !== "ready") return;
      if ((attempts[o.id] || 0) >= 5) {
        error.textContent =
          "Demo locked after five attempts. Reset the demo to try again.";
        return;
      }
      if (input.value !== codes[o.id]) {
        attempts[o.id] = (attempts[o.id] || 0) + 1;
        error.textContent =
          attempts[o.id] >= 5
            ? "Too many tries. Reset this demo to start again."
            : "That code doesn’t match. Please check with your neighbour.";
        input.setAttribute("aria-invalid", "true");
        return;
      }
      move(o, "completed");
      $("action-dialog").close();
      $("mobile-detail").close();
    };
    box.append(
      form,
      n(
        "p",
        "Try both personas with “Consumer preview” at the top of this demo. Real provider APIs would never expose the code.",
      ),
    );
    $("action-dialog").showModal();
  }
  function detail(o) {
    if (!o)
      return n(
        "div",
        "Pick an order. Let’s make someone’s day.",
        "empty-state",
      );
    const container = n("div"),
      heading = n("div", null, "detail-heading"),
      meta = n("div", null, "tile-top");
    meta.append(n("span", o.id, "reference"), badge(o));
    heading.append(
      meta,
      n(
        "h2",
        o.status === "requested"
          ? "A craving just landed."
          : o.status === "ready"
            ? "Packed with a little love."
            : o.status === "completed"
              ? "One happy neighbour."
              : "A good thing in the making.",
      ),
    );
    const person = n("div", null, "detail-person"),
      identity = n("div");
    identity.append(
      n("strong", o.name),
      n("small", `Flat ${o.flat} · MyHome Tridasa`),
    );
    person.append(n("span", o.initials, "person-avatar"), identity);
    heading.append(person);
    container.append(heading);
    const body = n("div", null, "detail-body"),
      fulfil = n("div", null, "fulfilment-grid");
    for (const [label, value] of [
      [
        "FULFILMENT",
        o.fulfil === "Pickup"
          ? "↗ Pickup at T1-416"
          : `⌂ Delivery to ${o.flat}`,
      ],
      ["PREFERRED WINDOW", o.time],
    ]) {
      const cell = n("div", null, "info-cell");
      cell.append(n("small", label), n("strong", value));
      fulfil.append(cell);
    }
    body.append(fulfil, n("p", "THE GOOD STUFF", "section-label"));
    for (const [name, qty, price] of o.items) {
      const item = n("div", null, "line-item");
      item.append(
        n("span", `${qty}×`, "qty"),
        n("span", `${name} · ${money(price)} each`),
        n("strong", money(qty * price)),
      );
      body.append(item);
    }
    const sum = n("div", null, "order-total");
    sum.append(n("span", "Order total"), n("strong", money(total(o))));
    body.append(
      sum,
      n(
        "p",
        "No delivery fee in this sample. Payment arranged directly.",
        "payment-note",
      ),
    );
    if (o.note) body.append(n("div", `“${o.note}”`, "buyer-note"));
    if (o.reason) body.append(n("div", `Declined · ${o.reason}`, "buyer-note"));
    if (o.status !== "rejected") {
      const timeline = n("div", null, "timeline"),
        stages = ["requested", "accepted", "preparing", "ready", "completed"];
      for (const [i, s] of stages.entries()) {
        const step = n(
          "div",
          ["Requested", "Accepted", "Making", "Ready", "Done"][i],
          `timeline-step ${stages.indexOf(o.status) >= i ? "done" : ""}`,
        );
        timeline.append(step);
      }
      body.append(timeline);
    }
    container.append(body);
    const actions = n("div", null, "action-area"),
      row = n("div", null, "action-row");
    if (o.status === "requested") {
      actions.append(
        n("p", "Can you make this happen for your neighbour?", "action-hint"),
      );
      row.append(
        button("Can’t fulfil", "reject", () => reject(o)),
        button("Accept order ↗", "primary", () => move(o, "accepted")),
      );
    }
    if (o.status === "accepted") {
      actions.append(
        n(
          "p",
          "Consumer updated. Their handover code stays private until delivery.",
          "action-hint",
        ),
      );
      row.append(
        button("Start preparing ✨", "primary", () => move(o, "preparing")),
      );
    }
    if (o.status === "preparing") {
      actions.append(
        n(
          "p",
          o.fulfil === "Pickup"
            ? "Mark ready when it’s packed for pickup."
            : "Mark ready when it’s packed for delivery.",
          "action-hint",
        ),
      );
      row.append(button("Mark ready ↗", "primary", () => move(o, "ready")));
    }
    if (o.status === "ready") {
      actions.append(
        n(
          "p",
          "Consumer notified. Verify their code only after handover.",
          "action-hint",
        ),
      );
      row.append(button("Verify handover code ↗", "primary", () => verify(o)));
    }
    if (o.status === "completed")
      actions.append(
        n(
          "p",
          "✓ Handover verified. This order’s all wrapped up.",
          "action-hint",
        ),
      );
    if (o.status === "rejected")
      actions.append(
        n("p", "Consumer updated. This request is closed.", "action-hint"),
      );
    actions.append(row);
    container.append(actions);
    return container;
  }
  function render() {
    const counts = { incoming: 0, active: 0, history: 0 };
    orders.forEach((o) => counts[bucket(o)]++);
    $("incoming-count").textContent = counts.incoming;
    $("active-count").textContent = counts.active;
    $("completed-count").textContent = orders.filter(
      (o) => o.status === "completed",
    ).length;
    document.querySelectorAll("[data-tab]").forEach((b) => {
      b.setAttribute("aria-selected", b.dataset.tab === tab);
      b.querySelector("span").textContent = counts[b.dataset.tab];
    });
    const search = $("search").value.toLowerCase().trim(),
      list = orders.filter(
        (o) =>
          bucket(o) === tab &&
          `${o.name} ${o.flat} ${o.id}`.toLowerCase().includes(search),
      );
    if (!list.some((o) => o.id === selected)) selected = list[0]?.id;
    $("order-list").replaceChildren();
    if (!list.length)
      $("order-list").append(
        n(
          "div",
          search
            ? "No matches. Try a name, flat or order ID."
            : tab === "incoming"
              ? "All caught up. A little breather for you ✨"
              : "Nothing here just yet.",
          "empty-state",
        ),
      );
    for (const o of list) {
      const tile = n(
        "button",
        null,
        `order-tile ${o.id === selected ? "selected" : ""}`,
      );
      tile.setAttribute("aria-label", `${o.id} ${o.name}, ${names[o.status]}`);
      const top = n("div", null, "tile-top");
      top.append(n("span", o.id, "reference"), badge(o));
      const person = n("div", null, "tile-person"),
        name = n("div");
      name.append(n("strong", o.name), n("small", `Flat ${o.flat} · ${o.ago}`));
      person.append(n("span", o.initials, "person-avatar"), name);
      const bottom = n("div", null, "tile-bottom");
      bottom.append(
        n("span", `${o.fulfil} · ${o.time.replace("Today · ", "")}`),
        n("strong", money(total(o))),
      );
      tile.append(
        top,
        person,
        n(
          "p",
          o.items.map((i) => `${i[1]}× ${i[0]}`).join(" · "),
          "tile-summary",
        ),
        bottom,
      );
      tile.onclick = () => {
        selected = o.id;
        render();
        if (matchMedia("(max-width:760px)").matches)
          $("mobile-detail").showModal();
      };
      $("order-list").append(tile);
    }
    const order = orders.find((o) => o.id === selected);
    $("desktop-detail").replaceChildren(detail(order));
    $("mobile-body").replaceChildren(detail(order));
  }
  function consumerPreview() {
    $("consumer-feed").replaceChildren();
    const relevant = orders.filter((o) =>
      ["accepted", "preparing", "ready", "completed"].includes(o.status),
    );
    if (!events.length)
      $("consumer-feed").append(
        n(
          "p",
          "Accept an incoming order to see a fresh consumer update here. Ready orders below show a sample of the handover experience.",
        ),
      );
    for (const event of events.slice(0, 4)) {
      const card = n("div", null, "consumer-event");
      card.append(
        n("strong", `${event.id} · ${event.title}`),
        n("span", event.body),
      );
      $("consumer-feed").append(card);
    }
    for (const o of relevant.filter((o) => o.status !== "completed")) {
      const card = n("div", null, "consumer-event");
      card.append(
        n("strong", `${o.id} · ${o.name}’s order`),
        n("span", `${names[o.status]} · Crispy Corner`),
      );
      const code = n("div", null, "consumer-code");
      code.append(
        n("span", "Consumer-only handover code · share after receiving"),
        n("b", codes[o.id]),
      );
      card.append(code);
      $("consumer-feed").append(card);
    }
    $("consumer-dialog").showModal();
  }
  document.querySelectorAll("[data-tab]").forEach(
    (b) =>
      (b.onclick = () => {
        tab = b.dataset.tab;
        render();
      }),
  );
  $("search").oninput = render;
  document
    .querySelectorAll("[data-close]")
    .forEach((b) => (b.onclick = () => $(b.dataset.close).close()));
  $("consumer-preview").onclick = consumerPreview;
  $("reset").onclick = () => {
    document.querySelectorAll("dialog[open]").forEach((d) => d.close());
    orders = seed();
    tab = "incoming";
    selected = "HH-1048";
    events = [];
    attempts = {};
    $("search").value = "";
    render();
    toast("Fresh start. All demo orders reset.");
  };
  render();
})();
