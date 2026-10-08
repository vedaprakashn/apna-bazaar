(() => {
  const slug = () => document.querySelector("#community")?.value || "tridasa";
  const node = (tag, text, cls = "") => {
    const n = document.createElement(tag);
    n.className = cls;
    if (text != null) n.textContent = text;
    return n;
  };
  window.heyhoodModuleApi = async (path, options = {}) => {
    const r = await fetch(`/api/${slug()}/${path}`, {
      ...options,
      headers: heyhoodResidentHeaders(),
    });
    const text = await r.text();
    let data;
    try {
      data = text ? JSON.parse(text) : null;
    } catch {}
    if (!r.ok)
      throw Error(
        data?.error ||
          data?.message ||
          (r.status === 401
            ? "Open My profile to sign in again."
            : r.status === 403
              ? "Verify your flat in My profile first."
              : r.status === 429
                ? "Give it a minute, then try again."
                : "Couldn’t complete that action. Please retry."),
      );
    return data;
  };
  window.heyhoodRequireMember = () => {
    const p = heyhoodResident();
    if (p?.verification === "verified") return true;
    location.href = heyhoodActivityUrl({
      view: "profile",
      return: location.pathname + location.search,
    });
    return false;
  };
  function dialog(title) {
    const d = node("dialog", null, "hood-action-sheet"),
      f = node("form"),
      head = node("div", null, "hood-sheet-head"),
      close = node("button", "✕");
    close.type = "button";
    close.setAttribute("aria-label", "Close");
    close.onclick = () => d.close();
    head.append(node("h2", title), close);
    f.append(head);
    d.append(f);
    document.body.append(d);
    d.addEventListener("close", () => d.remove());
    d.showModal();
    return { d, f };
  }
  function field(f, label, type = "text", required = true, max) {
    const l = node("label", label),
      input = node(type === "textarea" ? "textarea" : "input");
    if (type !== "textarea") input.type = type;
    input.required = required;
    if (max) input.maxLength = max;
    l.append(input);
    f.append(l);
    return input;
  }
  function submit(f, label, action) {
    const feedback = node("p", "", "hood-action-status"),
      b = node("button", label, "hood-primary");
    feedback.setAttribute("role", "status");
    f.append(feedback, b);
    f.onsubmit = async (e) => {
      e.preventDefault();
      if (b.disabled) return;
      b.disabled = true;
      const d = f.closest("dialog");
      d.querySelectorAll(".hood-sheet-head button").forEach(
        (x) => (x.disabled = true),
      );
      const block = (e) => e.preventDefault();
      d.addEventListener("cancel", block);
      try {
        await action();
      } catch (err) {
        feedback.textContent = err.message;
      } finally {
        b.disabled = false;
        d.querySelectorAll(".hood-sheet-head button").forEach(
          (x) => (x.disabled = false),
        );
        d.removeEventListener("cancel", block);
      }
    };
  }
  window.heyhoodRequestRide = (r, refresh) => {
    if (!heyhoodRequireMember()) return;
    const { d, f } = dialog(
      r.kind === "offer" ? "Go together?" : "Help with this ride?",
    );
    f.append(
      node(
        "p",
        `${r.destination} · ${new Date(r.departure_at).toLocaleString("en-IN", { timeZone: "Asia/Kolkata", dateStyle: "medium", timeStyle: "short" })} IST`,
      ),
    );
    const seats = field(
      f,
      r.kind === "offer" ? "Seats you need" : "Seats you can provide",
      "number",
    );
    seats.min = 1;
    seats.max = r.seats;
    seats.value = r.kind === "request" ? r.seats : 1;
    seats.readOnly = r.kind === "request";
    f.append(
      node(
        "p",
        "This request is for the date above only. The organiser shares the details, then you confirm. No cab or driver is booked.",
        "muted",
      ),
    );
    submit(f, "Send ride request ↗", async () => {
      await heyhoodModuleApi(`rides/${r.id}/agreements`, {
        method: "POST",
        body: JSON.stringify({
          departureAt: r.departure_at,
          seats: Number(seats.value),
        }),
      });
      d.close();
      heyhoodNotice("Sent. Track the arrangement in My stuff.");
      refresh?.();
    });
  };
  window.heyhoodRideArrangements = async (root, refresh) => {
    const rows = await heyhoodModuleApi("rides/agreements/mine");
    root.replaceChildren();
    if (!rows.length) {
      root.append(
        node(
          "p",
          "No ride arrangements yet. Find a ride in your hood.",
          "empty-card",
        ),
      );
      return;
    }
    for (const a of rows) {
      const c = node("article", null, "activity-card");
      c.append(
        node("span", a.state, "status-pill"),
        node("h3", a.destination),
        node(
          "p",
          `${a.neighbour}${a.flat_number ? " · Flat " + a.flat_number : ""}`,
        ),
        node(
          "p",
          `${new Date(a.occurrence_at).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })} IST · ${a.seats} seat${a.seats === 1 ? "" : "s"}`,
        ),
      );
      if (a.terms) c.append(node("p", a.terms, "agreement-terms"));
      if (a.exchange_terms) c.append(node("p", a.exchange_terms, "muted"));
      if (a.state === "proposed")
        c.append(
          node(
            "p",
            `Seats held until ${new Date(a.hold_until).toLocaleTimeString("en-IN", { timeZone: "Asia/Kolkata", hour: "numeric", minute: "2-digit" })} IST.`,
            "muted",
          ),
        );
      if (a.state === "confirmed")
        c.append(
          node(
            "p",
            "Both neighbours agreed. Confirm pickup and any school handover directly; HeyHood does not book transport.",
            "muted",
          ),
        );
      const actions = node("div", null, "hood-inline-actions");
      function button(label, action, terms) {
        const b = node("button", label);
        b.type = "button";
        b.onclick = async () => {
          b.disabled = true;
          try {
            await heyhoodModuleApi(`rides/agreements/${a.id}/action`, {
              method: "POST",
              body: JSON.stringify({ action, revision: a.revision, terms }),
            });
            await refresh();
          } catch (e) {
            heyhoodNotice(e.message);
          } finally {
            b.disabled = false;
          }
        };
        actions.append(b);
        return b;
      }
      if (a.organiser && a.state === "requested") {
        const proposal = button("Share arrangement ↗", "propose");
        proposal.onclick = () => {
          const { d, f } = dialog("Agree the ride details");
          f.append(
            node(
              "p",
              "Include pickup, fare sharing, driver and handover details. Seats are held for up to two hours while your neighbour confirms.",
              "muted",
            ),
          );
          const terms = field(f, "Your arrangement", "textarea", true, 500);
          terms.value = `Meet at ${a.pickup}. ${a.exchange_terms || "Agree timing and fare sharing before travelling."}`;
          submit(f, "Send proposal & hold seats", async () => {
            await heyhoodModuleApi(`rides/agreements/${a.id}/action`, {
              method: "POST",
              body: JSON.stringify({
                action: "propose",
                revision: a.revision,
                terms: terms.value,
              }),
            });
            d.close();
            await refresh();
          });
        };
        button("Not this time", "decline");
      }
      if (!a.organiser && a.state === "proposed")
        button("Agree & confirm ✓", "confirm");
      if (["requested", "proposed", "confirmed"].includes(a.state)) {
        const b = button("Cancel arrangement", "cancel");
        const run = b.onclick;
        b.onclick = () => {
          if (
            confirm(
              "Cancel this arrangement? The other neighbour will see the update and the seats will be released.",
            )
          )
            run();
        };
      }
      c.append(actions);
      root.append(c);
    }
  };
  window.heyhoodCreatePlan = (refresh) => {
    if (!heyhoodRequireMember()) return;
    const { d, f } = dialog("Make a hood plan");
    const title = field(f, "Name your plan", "text", true, 180),
      description = field(f, "What’s happening?", "textarea", true, 1000),
      category = field(f, "Category", "text", true, 60),
      place = field(f, "Meet at", "text", true, 160),
      time = field(f, "When (IST)", "datetime-local"),
      minimum = field(f, "Minimum sign-ups", "number"),
      capacity = field(f, "Maximum places (optional)", "number", false);
    minimum.min = 2;
    minimum.max = 1000;
    minimum.value = 20;
    capacity.min = 2;
    capacity.max = 1000;
    const consent = node("label", "", "check"),
      check = node("input");
    check.type = "checkbox";
    check.required = true;
    consent.append(
      check,
      document.createTextNode(
        "Publish this plan to my community. I’ll confirm the organiser, location and final arrangements once the target is reached.",
      ),
    );
    f.append(consent);
    submit(f, "Put the plan in my hood ↗", async () => {
      await heyhoodModuleApi("plans", {
        method: "POST",
        body: JSON.stringify({
          title: title.value,
          description: description.value,
          category: category.value,
          location: place.value,
          startsAt: new Date(time.value + "+05:30").toISOString(),
          minimum: Number(minimum.value),
          capacity: capacity.value ? Number(capacity.value) : null,
          publish: check.checked,
        }),
      });
      d.close();
      await refresh();
    });
  };
  window.heyhoodManagePlan = (p, action, refresh) => {
    const { d, f } = dialog(
      action === "confirm" ? "Make it official" : "Cancel this plan",
    );
    f.append(node("p", p.title));
    const note = field(
      f,
      action === "confirm"
        ? "Confirmed organiser, time, location and any costs"
        : "Let your neighbours know why",
      "textarea",
      true,
      500,
    );
    submit(
      f,
      action === "confirm" ? "Confirm plan ✓" : "Cancel plan",
      async () => {
        await heyhoodModuleApi(`plans/${p.id}/manage`, {
          method: "POST",
          body: JSON.stringify({ action, note: note.value }),
        });
        d.close();
        await refresh();
      },
    );
  };
})();
