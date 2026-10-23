package com.heyhood.provider;

import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

final class ProviderLink {
  private static Map<String, String> parameters(String raw) throws Exception {
    Map<String, String> values = new HashMap<>();
    if (raw == null) return values;
    for (String pair : raw.split("&")) {
      String[] parts = pair.split("=", 2);
      String name = URLDecoder.decode(parts[0], "UTF-8");
      if (values.containsKey(name)) throw new IllegalArgumentException("Duplicate parameter");
      values.put(name, parts.length > 1 ? URLDecoder.decode(parts[1], "UTF-8") : "");
    }
    return values;
  }
  static String normalize(String value) {
    try {
      if (value == null || value.length() > 2048) return null;
      URI u = new URI(value.trim());
      if (!"https".equals(u.getScheme()) || !"heyhood-production-b1b8.up.railway.app".equals(u.getHost()) ||
          (u.getPort() != -1 && u.getPort() != 443) || u.getUserInfo() != null ||
          !("/seller/console.html".equals(u.getPath()) || "/seller/index.html".equals(u.getPath()))) return null;
      Map<String, String> query = parameters(u.getRawQuery()), fragment = parameters(u.getRawFragment());
      String community = query.get("community"), id = query.get("id"), key = fragment.get("key");
      if (community == null || !community.matches("[a-z0-9-]{1,100}") || id == null ||
          !id.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}") ||
          key == null || !key.matches("[A-Za-z0-9_-]{32,120}")) return null;
      return "https://heyhood-production-b1b8.up.railway.app/seller/console.html?community=" + community + "&id=" + id + "#key=" + key;
    } catch (Exception ignored) { return null; }
  }
}
