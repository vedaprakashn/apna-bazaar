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
