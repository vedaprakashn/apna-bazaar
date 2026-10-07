/* Group invitations and individual chats use different WhatsApp link formats. */
window.heyhoodWhatsAppContact = function (number, groupUrl, message = "") {
  if (groupUrl) {
    try {
      const url = new URL(groupUrl);
      if (
        url.protocol !== "https:" ||
        url.hostname !== "chat.whatsapp.com" ||
        url.username ||
        url.password ||
        (url.port && url.port !== "443") ||
        !/^\/[A-Za-z0-9_-]{10,64}\/?$/.test(url.pathname)
      )
        return null;
      return { type: "group", url: url.href, label: "WhatsApp group ↗" };
    } catch (_) {
      return null;
    }
  }
  if (!/^\d{10,15}$/.test(number || "")) return null;
  return {
    type: "individual",
    url: `https://wa.me/${number}?text=${encodeURIComponent(message)}`,
    label: "WhatsApp ↗",
  };
};

window.heyhoodEnquiry = function (shop, community, flat, intent) {
  const context = (intent || "").trim().replace(/\s+/g, " ").replace(/^I\s+(?:am looking for|was looking for|need|want|would like)\s+/i, "").replace(/[.!?]+$/, "");
  return `Hey! ${context ? `I was looking for ${context} on HeyHood and found` : "I found"} ${shop} in ${community}${flat ? `, flat ${flat}` : ""}. I’d like to know more about your offerings.`;
};
window.heyhoodCopyEnquiry = async function (message) {
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(message);
      return true;
    } catch (_) { /* WebViews may deny Clipboard API; try selection copy. */ }
  }
  const field = document.createElement("textarea");
  field.value = message;
  field.setAttribute("aria-label", "WhatsApp enquiry");
  field.style.position = "fixed";
  field.style.opacity = "0.01";
  document.body.append(field);
  field.focus({ preventScroll: true });
  field.select();
  field.setSelectionRange(0, message.length);
  let copied = false;
  try { copied = document.execCommand("copy"); } catch (_) {}
  field.remove();
  if (copied) return true;
  const dialog = document.createElement("dialog");
  dialog.className = "enquiry-dialog";
  const title = document.createElement("h3");
  title.textContent = "Your WhatsApp enquiry";
  const hint = document.createElement("p");
  hint.textContent = "Copy isn’t available here. Press and hold the text to copy it, then paste it into WhatsApp.";
  const text = document.createElement("textarea");
  text.readOnly = true;
  text.value = message;
  text.setAttribute("aria-label", "Enquiry to copy");
  const close = document.createElement("button");
  close.textContent = "Done";
  close.onclick = () => dialog.close();
  dialog.append(title, hint, text, close);
  dialog.addEventListener("close", () => dialog.remove(), { once: true });
  document.body.append(dialog);
  dialog.showModal();
  text.focus({ preventScroll: true });
  text.select();
  return false;
};
