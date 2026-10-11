(() => {
  const params = new URLSearchParams(location.search),
    community = params.get("community") || "tridasa",
    id = params.get("id");
  const key = `heyhood-seller-${community}-${id}`;
  let token = new URLSearchParams(location.hash.slice(1)).get("key");
  if (token) {
    sessionStorage.setItem(key, token);
    history.replaceState(null, "", location.pathname + location.search);
  }
  token ||= sessionStorage.getItem(key);
  let state,
    dirty = false,
    busy = false;
  const $ = (id) => document.getElementById(id);
  const today = () =>
    new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Kolkata",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).format(new Date());
  const publicUrl = new URL(
    `../provider/index.html?community=${encodeURIComponent(community)}&id=${encodeURIComponent(id)}`,
    location.href,
  ).href;
  function node(tag, text, cls) {
    const n = document.createElement(tag);
    if (text) n.textContent = text;
    if (cls) n.className = cls;
    return n;
  }
  function status(message, error = false) {
    $("status").textContent = message;
    $("status").className = error ? "error" : "";
  }
  async function api(path, method = "GET", body) {
    const res = await fetch(
      `/api/${encodeURIComponent(community)}/provider-workspace/${encodeURIComponent(id)}${path}`,
      {
        method,
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: body === undefined ? undefined : JSON.stringify(body),
      },
    );
    const data = await res.json().catch(() => ({}));
    if (!res.ok)
      throw Error(
        data.detail ||
          data.message ||
          (res.status === 409
            ? "A newer draft exists. Reload before saving."
            : res.status === 401
              ? "Your edit link expired or was replaced. Ask your operator for a new one."
              : "Could not update your shop. Check the fields and try again."),
      );
    return data;
  }
  function changed() {
    dirty = true;
    $("saved").textContent = "Unsaved changes · not live yet";
  }
  function control(parent, label, type, value, update, options = {}) {
    const l = node("label", label);
    let input;
    if (type === "select") {
      input = node("select");
      for (const [v, t] of options.choices) {
        const o = node("option", t);
        o.value = v;
        input.append(o);
      }
    } else {
      input = node(type === "textarea" ? "textarea" : "input");
      if (type !== "textarea") input.type = type;
    }
    input.setAttribute("aria-label", label);
    Object.assign(input, options);
    input.value = value ?? "";
    input.addEventListener("input", () => {
      update(input.value);
      changed();
    });
    l.append(input);
    parent.append(l);
    return input;
  }
  function render() {
    $("items").replaceChildren();
    $("count").textContent = state.draft.items.length;
    $("empty").hidden = state.draft.items.length > 0;
    state.draft.items.forEach((item, index) => {
      item.days ||= ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];
      item.dates ||= {};
      const card = node("article", null, "item-card"),
        top = node("div", null, "item-title"),
        title = node("h3", item.name || `New item ${index + 1}`),
        remove = node("button", "Remove");
      remove.type = "button";
      remove.onclick = () => {
        if (
          !confirm(
            `Remove ${item.name || "this item"} from the draft? It leaves the public shop only after publishing.`,
          )
        )
          return;
        state.draft.items.splice(index, 1);
        changed();
        render();
      };
      top.append(title, remove);
      card.append(top);
      const grid = node("div", null, "item-grid");
      control(
        grid,
        "Item name",
        "text",
        item.name,
        (v) => {
          item.name = v;
          title.textContent = v || "New item";
        },
        { maxLength: 200, required: true },
      );
      control(
        grid,
        "Category",
        "select",
        item.categoryId,
        (v) => (item.categoryId = v),
        {
          choices: state.categories.map((c) => [
            c.id,
            `${c.emoji || ""} ${c.name}`,
          ]),
        },
      );
      control(
        grid,
        "Price · ₹",
        "number",
        item.price,
        (v) => (item.price = v === "" ? null : Number(v)),
        { min: "0", max: "999999.99", step: "0.01", required: true },
      );
      control(
        grid,
        "Price is per…",
        "text",
        item.unit,
        (v) => (item.unit = v),
        {
          maxLength: 50,
          placeholder: "plate, kg, class or session",
          required: true,
        },
      );
      control(
        grid,
        "What’s on offer?",
        "textarea",
        item.description,
        (v) => (item.description = v),
        { maxLength: 2000, rows: 3 },
      );
      control(
        grid,
        "Offering type",
        "select",
        item.type,
        (v) => (item.type = v),
        {
          choices: [
            ["food_item", "Food"],
            ["grocery_item", "Grocery"],
            ["service_package", "Service"],
            ["class", "Class"],
            ["other", "Other"],
          ],
        },
      );
      control(
        grid,
        "Serving from · IST",
        "time",
        item.starts?.slice(0, 5),
        (v) => (item.starts = v),
        { required: true },
      );
      control(
        grid,
        "Serving until · IST",
        "time",
        item.ends?.slice(0, 5),
        (v) => (item.ends = v),
        { required: true },
      );
      card.append(grid);
      const enabled = node("label", null, "check"),
        checkbox = node("input");
      checkbox.type = "checkbox";
      checkbox.checked = item.enabled;
      checkbox.onchange = () => {
        item.enabled = checkbox.checked;
        changed();
      };
      enabled.append(checkbox, node("span", "Usually available"));
      card.append(enabled);
      card.append(node("p", "Serving days"));
      const days = node("div", null, "day-buttons");
      for (const day of ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]) {
        const b = node("button", day);
        b.type = "button";
        b.setAttribute("aria-pressed", item.days.includes(day));
        b.onclick = () => {
          item.days = item.days.includes(day)
            ? item.days.filter((d) => d !== day)
            : [...item.days, day];
          b.setAttribute("aria-pressed", item.days.includes(day));
          changed();
        };
        days.append(b);
      }
      card.append(days);
      const dates = node("div", null, "date-editor");
      dates.append(
        node("strong", "One-day switches"),
        node("p", "Override a date. Other days keep your usual setting."),
      );
      const row = node("div", null, "date-controls"),
        date = node("input");
      date.type = "date";
      date.value = today();
      date.setAttribute("aria-label", "Availability date");
      const on = node("select");
      on.setAttribute("aria-label", "Availability for this date");
      for (const [value, label] of [
        ["true", "On for this day"],
        ["false", "Off for this day"],
      ]) {
        const option = node("option", label);
        option.value = value;
        on.append(option);
      }
      const apply = node("button", "Set day");
      apply.type = "button";
      apply.onclick = () => {
        if (!date.value) return;
        item.dates[date.value] = on.value === "true";
        changed();
        render();
      };
      row.append(date, on, apply);
      dates.append(row);
      const chips = node("div", null, "date-chips");
      for (const [date, on] of Object.entries(item.dates).sort()) {
        const b = node("button", `${date} · ${on ? "On" : "Off"} ×`);
        b.type = "button";
        b.setAttribute("aria-label", `Remove override for ${date}`);
        b.onclick = () => {
          delete item.dates[date];
          changed();
          render();
        };
        chips.append(b);
      }
      dates.append(chips);
      card.append(dates);
      $("items").append(card);
    });
    if (busy)
      for (const c of document.querySelectorAll(
        "#workspace input,#workspace select,#workspace textarea,#workspace button",
      ))
        c.disabled = true;
  }
  function adopt(data) {
    state = data;
    dirty = false;
    render();
    $("shop").textContent = state.provider.shop;
    $("owner").textContent =
      `${state.provider.name} · Flat ${state.provider.flat || "—"}`;
    $("saved").textContent = state.publishedAt
      ? `Last published ${new Date(state.publishedAt).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })} IST${state.revision !== state.publishedRevision ? " · draft changes pending" : ""}`
      : "Draft only · publish to get discovered";
  }
  async function save() {
    state = await api("/draft", "PUT", {
      revision: state.revision,
      draft: state.draft,
    });
    adopt(state);
  }
  async function action(publish) {
    if (busy) return;
    const invalid = document.querySelector("#workspace :invalid");
    if (invalid) {
      invalid.reportValidity();
      return;
    }
    status(
      publish ? "Getting your menu ready to publish…" : "Saving your draft…",
    );
    busy = true;
    for (const c of document.querySelectorAll(
      "#workspace input,#workspace select,#workspace textarea,#workspace button",
    ))
      c.disabled = true;
    try {
      if (dirty || !state.revision) await save();
      if (publish) {
        if (
          !confirm(
            "Publish this menu to your public shop and HeyHood discovery?",
          )
        ) {
          status("Draft saved. Publish when you’re ready.");
          return;
        }
        adopt(await api("/publish", "POST", { revision: state.revision }));
        status("You’re live ✨ Your shop and Aapta have the latest offerings.");
      } else status("Draft saved. Your public shop hasn’t changed yet.");
    } catch (e) {
      status(e.message, true);
    } finally {
      busy = false;
      for (const c of document.querySelectorAll(
        "#workspace input,#workspace select,#workspace textarea,#workspace button",
      ))
        c.disabled = false;
    }
  }
  $("save").onclick = () => action(false);
  $("publish").onclick = () => action(true);
  $("add").onclick = () => {
    if (state.draft.items.length >= 100)
      return status("Up to 100 items per shop.", true);
    state.draft.items.push({
      id: crypto.randomUUID(),
      categoryId: state.categories[0].id,
      name: "",
      description: "",
      type: "food_item",
      price: null,
      unit: "plate",
      enabled: true,
      starts: "09:00",
      ends: "18:00",
      days: ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"],
      dates: {},
    });
    changed();
    render();
    $("items").lastElementChild.scrollIntoView({ behavior: "smooth" });
  };
  $("preview").href = $("public-url").href = publicUrl;
  $("public-url").textContent = publicUrl;
  $("share").onclick = async () => {
    try {
      if (navigator.share)
        await navigator.share({
          title: `${state.provider.shop} · HeyHood`,
          text: "See what’s on offer in your hood",
          url: publicUrl,
        });
      else {
        await navigator.clipboard.writeText(publicUrl);
        status("Public shop link copied. Drop it in your group ✨");
      }
    } catch (e) {
      if (e.name !== "AbortError")
        status("Copy the public link shown above to share your shop.");
    }
  };
  addEventListener("beforeunload", (e) => {
    if (dirty) {
      e.preventDefault();
      e.returnValue = "";
    }
  });
  if (!id || !token)
    return status(
      "Open the private edit link your HeyHood operator sent you. Public shop links can’t edit a shop.",
      true,
    );
  api("")
    .then((data) => {
      adopt(data);
      $("workspace").hidden = false;
      status("Your workspace is ready. Edits stay private until you publish.");
    })
    .catch((e) => status(e.message, true));
})();
