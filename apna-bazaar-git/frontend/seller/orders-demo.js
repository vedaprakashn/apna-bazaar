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
  const today = () =>
    new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Kolkata",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).format(new Date());
  const dateAt = (offset = 0) => {
    const d = new Date(today() + "T12:00:00Z");
    d.setUTCDate(d.getUTCDate() + offset);
    return d.toISOString().slice(0, 10);
  };
  const dateLabel = (value) =>
    value === today()
      ? "Today"
      : value === dateAt(-1)
        ? "Yesterday"
        : new Intl.DateTimeFormat("en-IN", {
            day: "numeric",
            month: "short",
            year: "numeric",
            timeZone: "Asia/Kolkata",
          }).format(new Date(value + "T12:00:00Z"));
  const seed = () =>
    [
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
    ]
      .map((o) => ({ ...o, date: today() }))
      .concat([
        {
          id: "HH-1038",
          name: "Nisha Kapoor",
          flat: "T2-506",
          initials: "NK",
          status: "completed",
          ago: "Yesterday",
          date: dateAt(-1),
          fulfil: "Pickup",
          time: "Yesterday · 5:00–5:30 pm",
          items: [["Mirchi bajji", 2, 50]],
          note: "",
        },
        {
          id: "HH-1037",
          name: "Vikram Bose",
          flat: "T1-220",
          initials: "VB",
          status: "rejected",
          ago: "Yesterday",
          date: dateAt(-1),
          fulfil: "Pickup",
          time: "Yesterday · 6:00–6:30 pm",
          items: [["Aloo samosa", 2, 40]],
          note: "",
          reason: "Shop was taking a break",
        },
        {
          id: "HH-1032",
          name: "Asha Nair",
          flat: "T3-305",
          initials: "AN",
          status: "completed",
          ago: dateLabel(dateAt(-3)),
          date: dateAt(-3),
          fulfil: "Delivery",
          time: dateLabel(dateAt(-3)) + " · 6:00–6:30 pm",
          items: [["Punugulu", 3, 50]],
          note: "",
        },
        {
          id: "HH-1019",
          name: "Rohan Sen",
          flat: "T1-701",
          initials: "RS",
          status: "completed",
          ago: dateLabel(dateAt(-10)),
          date: dateAt(-10),
          fulfil: "Pickup",
          time: dateLabel(dateAt(-10)) + " · 5:30–6:00 pm",
          items: [["Onion pakora", 2, 60]],
          note: "",
        },
      ])
      .map((o) =>
        o.id === "HH-1042"
          ? {
              ...o,
              date: dateAt(-1),
              ago: "Yesterday · still open",
              time: "Yesterday · 5:30–6:00 pm",
            }
          : o,
      );
  let appView = "orders",
    alerts = [],
    menuDirty = false,
    demoSerial = 1050;
  const defaultMenu = () => [
    {
      id: 1,
      name: "Mirchi bajji",
      category: "Snacks",
      price: 50,
      unit: "plate of 4",
      on: true,
    },
    {
      id: 2,
      name: "Onion pakora",
      category: "Snacks",
      price: 60,
      unit: "plate",
      on: true,
    },
    {
      id: 3,
      name: "Aloo samosa",
      category: "Snacks",
      price: 40,
      unit: "pair",
      on: true,
    },
    {
      id: 4,
      name: "Punugulu",
      category: "Snacks",
      price: 50,
      unit: "plate",
      on: true,
    },
  ];
  const defaultProfile = () => ({
    shop: "Crispy Corner",
    name: "Sravani Reddy",
    flat: "T1-416",
    bio: "A little crunch, a little comfort. Evening snacks made in your hood.",
    starts: "16:00",
    ends: "20:30",
    delivery: true,
  });
  let menu = defaultMenu(),
    publishedMenu = defaultMenu(),
    profile = defaultProfile();
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
  const sources = {
    heyhood: "HeyHood",
    whatsapp: "WhatsApp",
    phone: "Phone call",
    paper: "Paper record",
    excel: "Excel record",
    walkin: "In person",
  };
  const sourceOf = (o) => o.source || "heyhood";
  function sourceBadge(o) {
    return n(
      "span",
      `${sources[sourceOf(o)]} · ${o.recordedBy === "provider" ? "By you" : "Auto"}`,
      "source-badge",
    );
  }
  function consumerEvent(o, title, body) {
    if (o.consumerLinked === false) return;
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
    if (
      status === "completed" &&
      o.completionMethod !==
        (o.consumerLinked === false ? "provider_recorded" : "otp")
    )
      return;
    o.status = status;
    tab = bucket(o);
    appView = tab === "history" ? "history" : "orders";
    if (appView === "history") {
      $("history-range").value = "all";
      $("history-status").value = "all";
    }
    alerts.filter((a) => a.id === o.id).forEach((a) => (a.read = true));
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
      toast(
        o.consumerLinked === false
          ? "Ready to go. Coordinate with this customer outside HeyHood. · demo"
          : "Ready to go. Consumer update added. · demo",
      );
    }
    if (status === "completed") {
      if (o.consumerLinked === false) {
        providerAlert(
          o,
          "Handover recorded by you",
          `${o.name}'s order is complete. Provider-recorded, not OTP-verified.`,
          false,
        );
        toast("Handover recorded. Clearly marked as not OTP-verified. · demo");
      } else {
        consumerEvent(
          o,
          "Handed over. Happy snacking 💛",
          "Your handover code was verified. The order is complete. Payment is arranged directly with your provider.",
        );
        providerAlert(
          o,
          "Handover verified",
          `${o.name}'s order is complete.`,
          false,
        );
        toast("Code checked. Order complete. Happy hood 💛");
      }
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
    if (o.consumerLinked === false) return;
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
      o.completionMethod = "otp";
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
    heading.append(person, sourceBadge(o));
    container.append(heading);
    const body = n("div", null, "detail-body"),
      fulfil = n("div", null, "fulfilment-grid");
    for (const [label, value] of [
      [
        "FULFILMENT",
        o.fulfil === "Pickup"
          ? `↗ Pickup at ${o.pickupFlat || "T1-416"}`
          : `⌂ Delivery to ${o.flat}`,
      ],
      ["PREFERRED WINDOW", o.time],
    ]) {
      const cell = n("div", null, "info-cell");
      cell.append(n("small", label), n("strong", value));
      fulfil.append(cell);
    }
    body.append(fulfil);
    if (o.consumerLinked === false)
      body.append(
        n(
          "div",
          "Recorded by you from " +
            sources[sourceOf(o)] +
            ". Customer not linked to HeyHood: no in-app alert or handover code has been sent.",
          "manual-consumer-note",
        ),
      );
    body.append(n("p", "THE GOOD STUFF", "section-label"));
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
          (o.recordedBy === "provider"
            ? ["Recorded", "Confirmed", "Making", "Ready", "Done"]
            : ["Requested", "Accepted", "Making", "Ready", "Done"])[i],
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
          o.consumerLinked === false
            ? "Confirmed with the customer outside HeyHood. You’re tracking it here."
            : "Consumer updated. Their handover code stays private until delivery.",
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
          o.consumerLinked === false
            ? "Customer is not linked to HeyHood. Record handover only once they have received the order."
            : "Consumer notified. Verify their code only after handover.",
          "action-hint",
        ),
      );
      row.append(
        o.consumerLinked === false
          ? button("Record handover ↗", "primary", () =>
              recordManualHandover(o),
            )
          : button("Verify handover code ↗", "primary", () => verify(o)),
      );
    }
    if (o.status === "completed")
      actions.append(
        n(
          "p",
          o.completionMethod === "provider_recorded"
            ? "✓ Handover recorded by you · not OTP-verified."
            : "✓ Handover verified. This order’s all wrapped up.",
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
    renderConsole();
    const counts = { incoming: 0, active: 0, history: 0 };
    orders.forEach((o) => counts[bucket(o)]++);
    document.querySelectorAll("[data-tab]").forEach((b) => {
      b.setAttribute("aria-selected", b.dataset.tab === tab);
      b.querySelector("span").textContent = counts[b.dataset.tab];
    });
    const search = $("search").value.toLowerCase().trim(),
      list = orders.filter(
        (o) =>
          (appView === "history"
            ? bucket(o) === "history" && historyMatches(o)
            : bucket(o) === tab && bucket(o) !== "history") &&
          `${o.name} ${o.flat} ${o.id}`.toLowerCase().includes(search),
      );
    list.sort(
      (a, b) => b.date.localeCompare(a.date) || b.id.localeCompare(a.id),
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
      name.append(
        n("strong", o.name),
        n(
          "small",
          `Flat ${o.flat} · ${appView === "history" ? dateLabel(o.date) : o.ago}`,
        ),
      );
      person.append(n("span", o.initials, "person-avatar"), name);
      const bottom = n("div", null, "tile-bottom");
      bottom.append(
        n("span", `${o.fulfil} · ${o.time.replace("Today · ", "")}`),
        n("strong", money(total(o))),
      );
      tile.append(
        top,
        person,
        sourceBadge(o),
        n(
          "p",
          o.items.map((i) => `${i[1]}× ${i[0]}`).join(" · "),
          "tile-summary",
        ),
        bottom,
      );
      tile.onclick = () => {
        selected = o.id;
        alerts.filter((a) => a.id === o.id).forEach((a) => (a.read = true));
        render();
        if (matchMedia("(max-width:1100px)").matches)
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
    const relevant = orders.filter(
      (o) =>
        o.id === selected &&
        o.consumerLinked !== false &&
        ["accepted", "preparing", "ready"].includes(o.status),
    );
    const updates = events.filter((e) => e.id === selected);
    if (!updates.length)
      $("consumer-feed").append(
        n(
          "p",
          orders.find((o) => o.id === selected)?.consumerLinked === false
            ? "This customer is not linked to HeyHood. No consumer notification or OTP was sent. Coordinate outside the app; their order stays in your provider console."
            : "This previews only the selected order’s consumer. Accept it to create their update and handover code.",
        ),
      );
    for (const event of updates.slice(0, 4)) {
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
    appView = "orders";
    menu = defaultMenu();
    publishedMenu = defaultMenu();
    profile = defaultProfile();
    menuDirty = false;
    demoSerial = 1050;
    seedAlerts();
    fillProfile();
    renderMenu();
    $("profile-state").textContent = "";
    $("history-range").value = "today";
    $("history-status").value = "all";
    $("history-date").value = today();
    tab = "incoming";
    selected = "HH-1048";
    events = [];
    attempts = {};
    $("search").value = "";
    render();
    toast("Fresh start. All demo orders reset.");
  };
  function historyMatches(o) {
    const range = $("history-range").value,
      outcome = $("history-status").value;
    if (outcome !== "all" && o.status !== outcome) return false;
    return (
      range === "all" ||
      (range === "today" && o.date === today()) ||
      (range === "yesterday" && o.date === dateAt(-1)) ||
      (range === "week" && o.date >= dateAt(-6) && o.date <= today()) ||
      (range === "custom" && o.date === $("history-date").value)
    );
  }
  function providerAlert(o, title, body, rerender = true) {
    alerts.unshift({
      key: `${o.id}-${title}`,
      id: o.id,
      title,
      body,
      read: false,
    });
    if (rerender) render();
  }
  function seedAlerts() {
    alerts = orders
      .filter((o) => o.status === "requested")
      .map((o) => ({
        key: o.id + "-new",
        id: o.id,
        title: "A new craving just landed",
        body: `${o.name} · ${o.id} · ${money(total(o))}`,
        read: false,
      }));
  }
  function setView(view) {
    appView = view;
    if (view === "orders" && tab === "history") tab = "incoming";
    $("search").value = "";
    render();
  }
  function renderConsole() {
    const orderView = appView === "orders" || appView === "history";
    $("orders-panel").hidden = !orderView;
    for (const v of ["menu", "notifications", "profile"])
      $(v + "-panel").hidden = appView !== v;
    document.querySelectorAll("[data-console]").forEach((b) => {
      if (b.dataset.console === appView) b.setAttribute("aria-current", "page");
      else b.removeAttribute("aria-current");
    });
    $("page-kicker").textContent =
      appView === "history"
        ? "THE ORDERS THAT MADE YOUR DAYS"
        : "TODAY + ALL OPEN ORDERS";
    $("page-title").textContent =
      appView === "history" ? "A little look back." : "Your hood is hungry.";
    $("page-description").textContent =
      appView === "history"
        ? "Completed and declined orders, by fulfilment date."
        : "New requests and unfinished orders—even if they started yesterday.";
    document.querySelector(".order-tabs").hidden = appView === "history";
    $("history-tools").hidden = appView !== "history";
    $("custom-date-label").hidden = $("history-range").value !== "custom";
    $("nav-orders-count").textContent =
      orders.filter((o) => bucket(o) === "incoming").length || "";
    const unread = alerts.filter((a) => !a.read).length;
    $("nav-alert-count").textContent = unread || "";
    $("consumer-preview").disabled = !orderView;
    renderNotifications();
  }
  function openAlert(alert) {
    alert.read = true;
    const o = orders.find((o) => o.id === alert.id);
    if (!o) return;
    appView = bucket(o) === "history" ? "history" : "orders";
    tab = bucket(o);
    selected = o.id;
    $("search").value = "";
    if (appView === "history") {
      $("history-range").value = "all";
      $("history-status").value = "all";
    }
    render();
    if (matchMedia("(max-width:1100px)").matches)
      $("mobile-detail").showModal();
  }
  function renderNotifications() {
    const list = $("console-notifications");
    list.replaceChildren();
    if (!alerts.length)
      list.append(n("p", "All quiet in the hood for now.", "empty-state"));
    for (const alert of alerts) {
      const b = button("", `console-alert ${alert.read ? "" : "unread"}`, () =>
        openAlert(alert),
      );
      b.append(n("span", alert.read ? "✓" : "●", "alert-dot"));
      const copy = n("div");
      copy.append(n("strong", alert.title), n("span", alert.body));
      b.append(copy, n("small", alert.read ? "Read" : "New"));
      list.append(b);
    }
  }
  function renderMenu() {
    const list = $("console-menu");
    list.replaceChildren();
    $("menu-summary").textContent =
      `${menu.filter((i) => i.on).length} of ${menu.length} items on today`;
    $("menu-state").textContent = menuDirty
      ? "Draft changes · publish the demo menu when ready"
      : "Sample menu · no unpublished changes";
    for (const item of menu) {
      const card = n("article", null, "console-menu-card");
      const head = n("div", null, "menu-card-heading");
      head.append(n("span", "🥨", "menu-item-icon"), n("h2", item.name));
      card.append(head);
      const category = n("span", item.category, "status-badge");
      card.append(category);
      const nameLabel = n("label", "Item name"),
        name = n("input");
      name.value = item.name;
      name.maxLength = 100;
      name.setAttribute("aria-label", `Item name for ${item.id}`);
      name.oninput = () => {
        item.name = name.value;
        menuChanged();
      };
      nameLabel.append(name);
      const priceLabel = n("label", "Price · ₹"),
        price = n("input");
      price.type = "number";
      price.min = 0;
      price.max = 999999;
      price.step = "0.01";
      price.value = item.price;
      price.setAttribute("aria-label", `Price for ${item.name}`);
      price.oninput = () => {
        item.price = price.value === "" ? null : Number(price.value);
        menuChanged();
      };
      priceLabel.append(price);
      const unitLabel = n("label", "Price per"),
        unit = n("input");
      unit.value = item.unit;
      unit.maxLength = 50;
      unit.setAttribute("aria-label", `Unit for ${item.id}`);
      unit.oninput = () => {
        item.unit = unit.value;
        menuChanged();
      };
      unitLabel.append(unit);
      const toggle = button(
        item.on ? "On today ✓" : "Off today",
        "menu-toggle",
        () => {
          item.on = !item.on;
          menuChanged();
          renderMenu();
        },
      );
      toggle.setAttribute("aria-pressed", item.on);
      toggle.setAttribute("aria-label", `Availability for ${item.name}`);
      const grid = n("div", null, "menu-edit-grid");
      grid.append(nameLabel, priceLabel, unitLabel);
      card.append(
        grid,
        n("p", `Serving ${profile.starts}–${profile.ends} IST · ${item.unit}`),
        toggle,
      );
      list.append(card);
    }
  }
  function menuChanged() {
    menuDirty = true;
    $("menu-state").textContent =
      "Draft changes · publish the demo menu when ready";
  }
  function fillProfile() {
    for (const [id, value] of Object.entries({
      shop: profile.shop,
      name: profile.name,
      flat: profile.flat,
      bio: profile.bio,
      start: profile.starts,
      end: profile.ends,
    }))
      $("profile-" + id).value = value;
    $("profile-delivery").checked = profile.delivery;
    document.querySelector(".shop-switch strong").textContent = profile.shop;
    document.querySelector(".shop-switch small").textContent =
      `MyHome Tridasa · ${profile.flat}`;
  }
  document
    .querySelectorAll("[data-console]")
    .forEach((b) => (b.onclick = () => setView(b.dataset.console)));
  for (const id of ["history-range", "history-date", "history-status"])
    $(id).onchange = render;
  $("read-alerts").onclick = () => {
    alerts.forEach((a) => (a.read = true));
    render();
    toast("Inbox caught up ✓ · demo");
  };
  $("simulate-order").onclick = () => {
    const available = publishedMenu.find((i) => i.on);
    if (!available) {
      toast(
        "Your published demo menu has no items on today. Switch one on and publish first.",
      );
      return;
    }
    const serial = demoSerial++,
      id = "HH-" + serial;
    const o = {
      id,
      name: "Riya Sharma",
      flat: "T2-512",
      initials: "RS",
      status: "requested",
      ago: "Just now",
      date: today(),
      fulfil: "Pickup",
      time: "Today · 6:00–6:30 pm",
      items: [[available.name, 2, available.price]],
      note: "Simulated incoming order for this UX preview.",
    };
    orders.unshift(o);
    codes[id] = "629714";
    providerAlert(
      o,
      "A new craving just landed",
      `Riya Sharma · ${id} · ${money(total(o))}`,
    );
    toast(`New order ${id}. Your provider inbox has the details. · demo`);
  };
  $("add-demo-item").onclick = () => {
    menu.push({
      id: Date.now(),
      name: "New snack",
      category: "Snacks",
      price: 50,
      unit: "plate",
      on: true,
    });
    menuChanged();
    renderMenu();
  };
  $("publish-demo-menu").onclick = () => {
    if (
      menu.some(
        (i) =>
          !i.name.trim() ||
          !i.unit.trim() ||
          i.price === null ||
          !Number.isFinite(i.price) ||
          i.price < 0 ||
          i.price > 999999,
      )
    ) {
      toast("Check item names, price units and prices first.");
      return;
    }
    publishedMenu = menu.map((i) => ({ ...i }));
    menuDirty = false;
    renderMenu();
    $("menu-state").textContent =
      "Demo menu published in this preview only. Real shop unchanged.";
    toast("Demo menu updated ✨ Existing order snapshots stay the same.");
  };
  $("demo-profile").onsubmit = (e) => {
    e.preventDefault();
    if ($("profile-end").value <= $("profile-start").value) {
      $("profile-state").textContent =
        "Choose an end time after the start time.";
      return;
    }
    const draftProfile = {
      shop: $("profile-shop").value.trim(),
      name: $("profile-name").value.trim(),
      flat: $("profile-flat").value.trim(),
      bio: $("profile-bio").value.trim(),
      starts: $("profile-start").value,
      ends: $("profile-end").value,
      delivery: $("profile-delivery").checked,
    };
    if (!draftProfile.shop || !draftProfile.name || !draftProfile.flat) {
      $("profile-state").textContent =
        "Add your shop name, name and pickup flat.";
      return;
    }
    profile = draftProfile;
    fillProfile();
    renderMenu();
    $("profile-state").textContent =
      "Demo profile saved. Your real shop profile hasn’t changed.";
    toast("A fresh hello for your shop ✨ · demo");
  };
  function recordManualHandover(o) {
    if (o.consumerLinked !== false || o.status !== "ready") return;
    const box = $("action-content");
    box.replaceChildren(
      n("p", "A CLEAR RECORD, NO FAKE OTP", "eyebrow"),
      n("h2", "Order handed over?"),
      n(
        "p",
        "This customer isn’t linked to HeyHood. Record only the actual handover; this will be marked as provider-recorded, not OTP-verified.",
      ),
    );
    const form = n("form"),
      label = n("label", null, "manual-check"),
      check = n("input");
    check.type = "checkbox";
    check.required = true;
    check.id = "manual-handover-confirmed";
    label.append(
      check,
      n("span", "I confirm the customer has received this order."),
    );
    const submit = button("Confirm handover", "primary");
    submit.type = "submit";
    form.append(label, submit);
    form.onsubmit = (e) => {
      e.preventDefault();
      if (!check.checked) return;
      o.completionMethod = "provider_recorded";
      move(o, "completed");
      $("action-dialog").close();
      $("mobile-detail").close();
    };
    box.append(form);
    $("action-dialog").showModal();
  }
  let manualLines = [];
  function manualTotal() {
    const amount =
      manualLines.reduce(
        (sum, i) =>
          sum + Math.round((Number(i.price) || 0) * 100) * (Number(i.qty) || 0),
        0,
      ) / 100;
    $("manual-total").textContent = money(amount);
    return amount;
  }
  function manualLine(item) {
    return {
      key: crypto.randomUUID(),
      menuId: item ? String(item.id) : "custom",
      name: item?.name || "",
      price: item?.price ?? 0,
      qty: 1,
    };
  }
  function renderManualLines() {
    const list = $("manual-lines");
    list.replaceChildren();
    manualLines.forEach((line, index) => {
      const card = n("div", null, "manual-line"),
        selectLabel = n("label", "Item"),
        select = n("select");
      select.setAttribute("aria-label", `Item ${index + 1}`);
      for (const item of publishedMenu.filter((i) => i.on)) {
        const option = n("option", item.name);
        option.value = String(item.id);
        select.append(option);
      }
      const custom = n("option", "Custom item / agreed special");
      custom.value = "custom";
      select.append(custom);
      select.value = line.menuId;
      select.onchange = () => {
        const item = publishedMenu.find((i) => String(i.id) === select.value);
        line.menuId = select.value;
        line.name = item?.name || "";
        line.price = item?.price ?? 0;
        renderManualLines();
      };
      selectLabel.append(select);
      const nameLabel = n("label", "Agreed item name"),
        name = n("input");
      name.value = line.name;
      name.maxLength = 100;
      name.required = true;
      name.setAttribute("aria-label", `Item name ${index + 1}`);
      name.oninput = () => (line.name = name.value);
      nameLabel.append(name);
      const qtyLabel = n("label", "Quantity"),
        qty = n("input");
      qty.type = "number";
      qty.min = 1;
      qty.max = 100;
      qty.step = 1;
      qty.required = true;
      qty.value = line.qty;
      qty.setAttribute("aria-label", `Quantity ${index + 1}`);
      qty.oninput = () => {
        line.qty = Number(qty.value);
        manualTotal();
      };
      qtyLabel.append(qty);
      const priceLabel = n("label", "Agreed unit price · ₹"),
        price = n("input");
      price.type = "number";
      price.min = 0;
      price.max = 999999;
      price.step = "0.01";
      price.required = true;
      price.value = line.price;
      price.setAttribute("aria-label", `Unit price ${index + 1}`);
      price.oninput = () => {
        line.price = price.value === "" ? null : Number(price.value);
        manualTotal();
      };
      priceLabel.append(price);
      card.append(selectLabel, nameLabel, qtyLabel, priceLabel);
      const remove = button("Remove item", "", () => {
        manualLines.splice(index, 1);
        renderManualLines();
      });
      remove.disabled = manualLines.length === 1;
      card.append(remove);
      list.append(card);
    });
    manualTotal();
  }
  $("add-manual-order").onclick = () => {
    $("manual-order-form").reset();
    $("manual-error").textContent = "";
    $("manual-date").value = today();
    $("manual-date").min = dateAt(-90);
    $("manual-date").max = dateAt(90);
    $("manual-time").value = "18:00";
    $("manual-delivery-option").disabled = !profile.delivery;
    manualLines = [manualLine(publishedMenu.find((i) => i.on))];
    renderManualLines();
    $("manual-dialog").showModal();
  };
  $("manual-add-line").onclick = () => {
    if (manualLines.length >= 20) {
      $("manual-error").textContent = "Use up to 20 item lines in this demo.";
      return;
    }
    manualLines.push(manualLine(publishedMenu.find((i) => i.on)));
    renderManualLines();
  };
  $("manual-order-form").onsubmit = (e) => {
    e.preventDefault();
    const name = $("manual-name").value.trim(),
      flat = $("manual-flat").value.trim(),
      date = $("manual-date").value,
      time = $("manual-time").value,
      source = $("manual-source").value;
    if (
      !name ||
      !flat ||
      !date ||
      !time ||
      !manualLines.length ||
      manualLines.some(
        (i) =>
          !i.name.trim() ||
          !Number.isInteger(i.qty) ||
          i.qty < 1 ||
          i.qty > 100 ||
          i.price === null ||
          !Number.isFinite(i.price) ||
          i.price < 0 ||
          i.price > 999999,
      )
    ) {
      $("manual-error").textContent =
        "Check customer details, item names, quantities and agreed prices.";
      return;
    }
    if (
      date < dateAt(-90) ||
      date > dateAt(90) ||
      !sources[source] ||
      source === "heyhood" ||
      !$("manual-confirmed").checked ||
      ($("manual-fulfil").value === "Delivery" && !profile.delivery)
    ) {
      $("manual-error").textContent =
        "Check the fulfilment date, delivery option and confirmation.";
      return;
    }
    const o = {
      id: "HH-" + demoSerial++,
      name,
      flat,
      initials: name
        .split(/\s+/)
        .slice(0, 2)
        .map((w) => w[0])
        .join("")
        .toUpperCase(),
      status: "accepted",
      ago: "Recorded just now",
      date,
      fulfil: $("manual-fulfil").value,
      time: `${dateLabel(date)} · ${time} IST`,
      items: manualLines.map((i) => [i.name.trim(), i.qty, i.price]),
      note: $("manual-note").value.trim(),
      source,
      recordedBy: "provider",
      consumerLinked: false,
      pickupFlat: profile.flat,
    };
    orders.unshift(o);
    appView = "orders";
    tab = "active";
    selected = o.id;
    $("search").value = "";
    $("manual-dialog").close();
    render();
    toast(
      `Order ${o.id} recorded from ${sources[source]}. No consumer alert or OTP sent. · demo`,
    );
    if (matchMedia("(max-width:1100px)").matches)
      $("mobile-detail").showModal();
  };
  $("history-date").value = today();
  seedAlerts();
  fillProfile();
  renderMenu();
  render();
})();
