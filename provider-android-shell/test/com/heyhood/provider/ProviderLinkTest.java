package com.heyhood.provider;

public class ProviderLinkTest {
  public static void main(String[] args) {
    String url = "https://heyhood-production-b1b8.up.railway.app/seller/console.html?community=tridasa&id=11111111-2222-3333-4444-555555555555#key=fixture_not_a_real_private_key_1234567890";
    if (!url.equals(ProviderLink.normalize(url))) throw new AssertionError("Valid link");
    if (!url.equals(ProviderLink.normalize(url.replace("console.html", "index.html")))) throw new AssertionError("Legacy link");
    if (!url.equals(ProviderLink.normalize(url.replace(".app/", ".app:443/")))) throw new AssertionError("HTTPS port");
    String[] invalid = {null, "", url.replace("https:", "http:"), url.replace(".app/", ".app.attacker.invalid/"),
      url.replace(".app/", ".app:8080/"), url.replace("https://", "https://somebody@"),
      url.replace("/seller/console.html", "/provider/index.html"), url.split("#")[0],
      url.replace("#key=", "&key="), url.replace("community=tridasa", "community=tridasa&community=sayuk"),
      url.replace("community=tridasa", "community=%3Cscript%3E"), url.replace("11111111-2222-3333-4444-555555555555", "anything"),
      url.replace("#key=", "#key=bad&key="), "javascript:alert(1)"};
    for (String candidate : invalid) if (ProviderLink.normalize(candidate) != null) throw new AssertionError("Unsafe link accepted");
    System.out.println("Provider link checks passed: valid/legacy links and 14 invalid/private-boundary cases");
  }
}
