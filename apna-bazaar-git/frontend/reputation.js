(() => {
  const slug = () =>
    new URLSearchParams(location.search).get("community") ||
    sessionStorage.getItem("heyhood-community") ||
    document.querySelector("#community")?.value ||
    "tridasa";
  const key = () => `heyhood-contacts:${slug()}`;
  const read = () => {
    try {
      const value = JSON.parse(localStorage.getItem(key()));
      return Array.isArray(value)
        ? value.filter((x) => x && typeof x.id === "string")
        : [];
    } catch {
      return [];
    }
  };
  window.heyhoodPendingContacts = read;
  const write = (items) => {
    try {
      localStorage.setItem(key(), JSON.stringify(items.slice(0, 30)));
    } catch {
      /* A contact must still open if device storage is unavailable. */
    }
  };
  const node = (tag, text) => {
    const n = document.createElement(tag);
    if (text != null) n.textContent = text;
    return n;
  };
  const link = (id) =>
    `../provider/index.html?community=${encodeURIComponent(slug())}&id=${encodeURIComponent(id)}#reviews`;
  async function api(id, path = "", options = {}) {
    const response = await fetch(
      `/api/${encodeURIComponent(slug())}/providers/${encodeURIComponent(id)}/${path || "reputation"}`,
      { ...options, headers: heyhoodResidentHeaders() },
    );
    if (!response.ok)
      throw Error(
        response.status === 401
          ? "Set up your resident profile in My stuff to continue."
          : response.status === 429
            ? "A little breather—try again in a minute."
            : response.status === 422
              ? "Keep the review respectful. Please change that text."
              : response.status === 503
                ? "Review checks are unavailable. Your draft is safe—please retry."
                : "Could not save that. Check the details and retry.",
      );
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  }
  window.heyhoodRecordContact = (id, title) => {
    const items = read().filter((x) => x.id !== id);
    items.unshift({ id, title, at: Date.now(), outcome: null, synced: false });
    write(items);
    if (heyhoodResident())
      api(id, "contact", { method: "POST", keepalive: true })
        .then(() => {
          const saved = read();
          const item = saved.find((x) => x.id === id);
          if (item) {
            item.synced = true;
            write(saved);
          }
        })
        .catch(() => {});
  };
  async function sync(item) {
    if (!item.synced) {
      await api(item.id, "contact", { method: "POST" });
      item.synced = true;
      const items = read();
      const saved = items.find((x) => x.id === item.id);
      if (saved) {
        saved.synced = true;
        write(items);
      }
    }
  }
  window.heyhoodFeedbackCard = (item) => {
    const card = node("aside");
    card.className = "reputation-card";
    card.dataset.contactId = item.id;
    card.append(
      node("strong", `Did you connect with ${item.title}?`),
      node(
        "p",
        "Opening WhatsApp isn’t a confirmed conversation. Let us know how it went.",
      ),
    );
    const actions = node("div");
    actions.className = "reputation-actions";
    for (const [outcome, label] of [
      ["connected", "Yes, connected"],
      ["waiting", "Still waiting"],
      ["not_connected", "Couldn’t connect"],
    ]) {
      const b = node("button", label);
      b.type = "button";
      b.setAttribute("aria-pressed", String(item.outcome === outcome));
      b.onclick = async () => {
        if (!heyhoodResident()) {
          location.href = heyhoodActivityUrl({
            view: "profile",
            return: location.pathname + location.search,
          });
          return;
        }
        actions.querySelectorAll("button").forEach((b) => (b.disabled = true));
        try {
          await sync(item);
          await api(item.id, "feedback", {
            method: "PUT",
            body: JSON.stringify({ outcome }),
          });
          const items = read();
          const saved = items.find((x) => x.id === item.id);
          if (saved) {
            saved.outcome = outcome;
            write(items);
          }
          item.outcome = outcome;
          card.replaceWith(heyhoodFeedbackCard(item));
          heyhoodNotice("Thanks! Your connection feedback is private.");
          if (document.querySelector("#reviews"))
            await heyhoodLoadReputation(item.id);
        } catch (e) {
          heyhoodNotice(e.message);
        } finally {
          actions
            .querySelectorAll("button")
            .forEach((b) => (b.disabled = false));
        }
      };
      actions.append(b);
    }
    card.append(actions);
    if (item.outcome === "connected") {
      const a = node("a", "Rate your experience ↗");
      a.href = link(item.id);
      card.append(a);
    }
    const later = node("button", "Later");
    later.type = "button";
    later.className = "save-action";
    later.onclick = () => {
      const items = read();
      const saved = items.find((x) => x.id === item.id);
      if (saved) {
        saved.dismissed = Date.now();
        write(items);
      }
      card.remove();
    };
    card.append(later);
    return card;
  };
  function returning() {
    const stream = document.querySelector("#stream");
    if (!stream) return;
    const item = read().find(
      (x) =>
        !x.outcome &&
        Date.now() - x.at < 30 * 86400000 &&
        (!x.dismissed || Date.now() - x.dismissed > 86400000),
    );
    if (item && !stream.querySelector(`[data-contact-id="${item.id}"]`))
      stream.append(heyhoodFeedbackCard(item));
  }
  window.addEventListener("pageshow", returning);
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") returning();
  });
  window.heyhoodReputationBadge = (p) => {
    const n = node("div");
    n.className = "reputation-summary";
    const count = Number(p.review_count ?? p.reviewCount ?? 0);
    if (count)
      n.append(
        node(
          "span",
          `★ ${Number(p.rating).toFixed(1)} · ${count} ${count === 1 ? "review" : "reviews"}`,
        ),
      );
    if (p.verified && p.verification_checked_at) {
      const badge = node("span", "Provider checked ✓");
      badge.title =
        "Operator checked identity, flat association and contact details. Not a qualification or background check.";
      n.append(badge);
    }
    return n;
  };
  window.heyhoodLoadReputation = async (id) => {
    const root = document.querySelector("#reviews");
    if (!root) return;
    root.replaceChildren(
      node("h2", "From your hood"),
      node("p", "Loading resident reviews…"),
    );
    try {
      const data = await api(id);
      root.replaceChildren(
        node("h2", "From your hood"),
        heyhoodReputationBadge({
          ...data.provider,
          verified: data.provider.is_verified,
        }),
      );
      if (!data.provider.review_count)
        root.append(
          node(
            "p",
            "No reviews yet. Had an actual interaction? Be the first to share.",
          ),
        );
      root.append(
        node(
          "p",
          "Resident-reported interactions, not verified purchases. Provider checks cover identity, flat association and contact details only.",
        ),
      );
      for (const review of data.reviews) {
        const c = node("article");
        c.className = "reputation-card";
        c.append(
          node("strong", `${review.name} · ${"★".repeat(review.stars)}`),
          node("p", review.body || "Rating only"),
          node(
            "small",
            new Date(review.updated_at).toLocaleDateString("en-IN", {
              timeZone: "Asia/Kolkata",
            }),
          ),
        );
        root.append(c);
      }
      if (data.provider.review_count > 20)
        root.append(node("p", "Showing the 20 most recent reviews."));
      let pending = read().find((x) => x.id === id);
      if (pending) root.append(heyhoodFeedbackCard(pending));
      if (!heyhoodResident()) {
        const a = node(
          "a",
          "Set up your resident profile to leave a review ↗",
        );
        a.href = heyhoodActivityUrl({
          view: "profile",
          return: location.pathname + location.search + "#reviews",
        });
        root.append(a);
        return;
      }
      const mine = await api(id, "reputation/mine");
      if (!pending && mine.feedback.length)
        root.append(
          heyhoodFeedbackCard({
            id,
            title: document.querySelector("#shop").textContent,
            outcome: mine.feedback[0].outcome,
            synced: true,
          }),
        );
      const form = node("form");
      form.className = "reputation-card";
      form.append(
        node(
          "h3",
          mine.reviews.length ? "Your review" : "Share your experience",
        ),
      );
      const starsLabel = node("label", "Your rating "),
        stars = node("select");
      stars.name = "stars";
      stars.required = true;
      stars.append(new Option("Choose a rating", ""));
      for (let i = 1; i <= 5; i++)
        stars.append(
          new Option(`${i} ${i === 1 ? "star" : "stars"}`, String(i)),
        );
      stars.value = mine.reviews[0]?.stars || "";
      starsLabel.append(stars);
      const bodyLabel = node("label", "A few words (optional) "),
        body = node("textarea");
      body.maxLength = 1000;
      body.rows = 3;
      body.value = mine.reviews[0]?.body || "";
      bodyLabel.append(body);
      const consent = node("label"),
        check = node("input");
      check.type = "checkbox";
      check.required = true;
      consent.append(
        check,
        document.createTextNode(
          " I actually interacted with this provider. This is my own experience.",
        ),
      );
      const save = node(
        "button",
        mine.reviews.length ? "Update review" : "Post review",
      );
      save.type = "submit";
      const status = node("p");
      status.setAttribute("role", "status");
      form.append(starsLabel, bodyLabel, consent, save, status);
      if (mine.reviews[0]?.hidden)
        form.append(
          node(
            "p",
            "Your review is hidden by moderation. Editing won’t automatically republish it.",
          ),
        );
      if (!mine.feedback.some((f) => f.outcome === "connected")) {
        save.disabled = true;
        form.append(
          node(
            "p",
            "Use WhatsApp and confirm “Yes, connected” above after your interaction, then return here to review.",
          ),
        );
      }
      form.onsubmit = async (e) => {
        e.preventDefault();
        save.disabled = true;
        try {
          await api(id, "review", {
            method: "PUT",
            body: JSON.stringify({
              stars: Number(stars.value),
              body: body.value,
              interacted: check.checked,
            }),
          });
          heyhoodNotice("Your review is saved.");
          await heyhoodLoadReputation(id);
        } catch (e) {
          status.textContent = e.message;
          save.disabled = false;
        }
      };
      if (mine.reviews.length) {
        const remove = node("button", "Remove my review");
        remove.type = "button";
        remove.onclick = async () => {
          if (!confirm("Remove your review?")) return;
          remove.disabled = true;
          try {
            await api(id, "review", { method: "DELETE" });
            await heyhoodLoadReputation(id);
          } catch (e) {
            status.textContent = e.message;
            remove.disabled = false;
          }
        };
        form.append(remove);
      }
      root.append(form);
    } catch (e) {
      root.replaceChildren(node("h2", "From your hood"), node("p", e.message));
      const retry = node("button", "Try again");
      retry.onclick = () => heyhoodLoadReputation(id);
      root.append(retry);
    }
  };
})();
