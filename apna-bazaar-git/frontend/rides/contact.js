window.heyhoodRideEnquiry = function (r, community) {
  const when = new Intl.DateTimeFormat("en-IN", {
    timeZone: "Asia/Kolkata",
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(r.departure_at));
  const route =
    r.direction === "inbound"
      ? `${r.destination} → ${community}`
      : `${community} → ${r.destination}`;
  return `Hey! I found your ${r.kind === "offer" ? "ride offer" : "ride request"} on HeyHood: ${route}, ${when} IST, ${r.seats} ${r.kind === "offer" ? "seats offered" : "seats needed"}. ${r.kind === "offer" ? "I’m looking for a ride." : "I may be able to help."} Can we discuss pickup, timing and arrangements?`;
};
