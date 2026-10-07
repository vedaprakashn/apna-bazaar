// Shared resident navigation. Module switches retain community and chat history.
(() => {
  const current = location.pathname.split("/").filter(Boolean)[0];
  const nav = document.querySelector(".hood-modules");
  if (!nav) return;
  const items = [
    ["chatbot", "Chat", "💬", "Ask Aapta"],
    ["shops", "Shops", "🛍️", "Explore shops and services"],
    ["rides", "Rides", "🚘", "Hood Rides"],
    ["plans", "Plans", "🎉", "Hood Plans"],
    ["help", "Help", "🛟", "Help and contacts"],
  ];
  const selected =
    new URLSearchParams(location.search).get("explore") === "shops"
      ? "shops"
      : current;
  function route(module) {
    const community =
      document.querySelector("#community")?.value ||
      sessionStorage.getItem("heyhood-community") ||
      "tridasa";
    const query = new URLSearchParams({ community });
    if (module === "shops") query.set("explore", "shops");
    return `../${module === "shops" ? "chatbot" : module}/index.html?${query}`;
  }
  for (const [module, label, emoji, accessible] of items) {
    const link = document.createElement("a");
    link.className = "hood-module";
    link.dataset.module = module;
    link.href = route(module);
    link.setAttribute("aria-label", accessible);
    if (module === selected) link.setAttribute("aria-current", "page");
    const icon = document.createElement("span");
    icon.className = "module-icon";
    icon.textContent = emoji;
    icon.setAttribute("aria-hidden", "true");
    const text = document.createElement("span");
    text.textContent = label;
    link.append(icon, text);
    link.addEventListener("click", (e) => {
      link.href = route(module);
      if (module === "shops" && current === "chatbot") {
        e.preventDefault();
        document.querySelector(".mobile-explore").click();
      }
    });
    nav.append(link);
  }
  document.addEventListener("DOMContentLoaded", () => {
    if (current === "chatbot" && selected === "shops")
      document.querySelector(".mobile-explore").click();
  });
})();
