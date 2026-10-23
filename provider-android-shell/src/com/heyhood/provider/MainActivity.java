package com.heyhood.provider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/** Private-link entry for the existing provider workspace, not a new authentication system. */
public class MainActivity extends Activity {
  static final String HOST = "heyhood-production-b1b8.up.railway.app";
  private WebView web;
  private String entry;
  private LinearLayout root;
  private TextView error;
  private ProgressBar progress;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    try { entry = PrivateLink.read(this); } catch (Exception ignored) { entry = null; }
    if (entry == null) setup(); else console(state);
  }

  private int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
  private LinearLayout layout() {
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setBackgroundColor(Color.rgb(252, 249, 255));
    box.setOnApplyWindowInsetsListener((v, insets) -> {
      v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
          insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
      return insets;
    });
    setContentView(box);
    return box;
  }
  private TextView text(String value, int size) {
    TextView t = new TextView(this); t.setText(value); t.setTextSize(size);
    t.setTextColor(Color.rgb(48, 33, 67)); t.setPadding(0, dp(8), 0, dp(8)); return t;
  }
  private Button button(String label) {
    Button b = new Button(this); b.setText(label); b.setAllCaps(false);
    b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(99, 54, 245)));
    return b;
  }
  private void setup() {
    if (web != null) { web.destroy(); web = null; }
    root = layout();
    android.widget.ScrollView scroll = new android.widget.ScrollView(this);
    LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(dp(24), dp(35), dp(24), dp(24));
    android.widget.ImageView logo = new android.widget.ImageView(this);
    logo.setImageResource(R.drawable.icon); logo.setContentDescription("HeyHood");
    form.addView(logo, new LinearLayout.LayoutParams(dp(76), dp(76)));
    form.addView(text("HeyHood Provider", 29));
    form.addView(text("Your shop. All together.", 21));
    form.addView(text("Orders, menu, updates and your shop profile—all in one space.", 16));
    form.addView(text("Paste your private provider link", 15));
    EditText input = new EditText(this);
    input.setHint("https://…/seller/console.html?…#key=…");
    input.setTextSize(14); input.setSingleLine(true);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
    input.setSaveEnabled(false);
    form.addView(input, new LinearLayout.LayoutParams(-1, dp(58)));
    TextView message = text("", 14); message.setTextColor(Color.rgb(152, 50, 85));
    Button open = button("Open my console ↗");
    open.setOnClickListener(v -> {
      String normalized = normalize(input.getText().toString());
      if (normalized == null) { message.setText("Use the full private provider link from HeyHood onboarding, including #key=. A public shop link won’t work."); return; }
      try { PrivateLink.save(this, normalized); entry = normalized; input.setText(""); console(null); }
      catch (Exception ignored) { message.setText("Couldn’t securely save this link. Please try again."); }
    });
    form.addView(open); form.addView(message);
    form.addView(text("Get this link from your HeyHood operator. Keep it private—it gives access to your shop. This app never asks for the operator key.", 13));
    scroll.addView(form); root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
  }

  static String normalize(String value) { return ProviderLink.normalize(value); }
  private static boolean trusted(Uri u) {
    return "https".equals(u.getScheme()) && HOST.equals(u.getHost()) &&
        (u.getPort() == -1 || u.getPort() == 443) && u.getUserInfo() == null;
  }
  private void console(Bundle state) {
    root = layout();
    LinearLayout tools = new LinearLayout(this); tools.setGravity(android.view.Gravity.CENTER_VERTICAL);
    TextView label = text("  HeyHood · Provider", 13);
    tools.addView(label, new LinearLayout.LayoutParams(0, dp(40), 1));
    Button menu = new Button(this); menu.setText("⋯"); menu.setContentDescription("Provider app options");
    menu.setTextSize(22); tools.addView(menu, new LinearLayout.LayoutParams(dp(56), dp(40)));
    menu.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Your provider app")
        .setItems(new String[]{"Reload console", "Switch shop / remove private link"}, (d, which) -> {
          if (which == 0) web.loadUrl(entry); else switchShop();
        }).show());
    root.addView(tools);
    progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setIndeterminate(true); root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));
    error = text("Can’t reach your shop. Tap to retry.", 14); error.setVisibility(View.GONE);
    error.setOnClickListener(v -> { error.setVisibility(View.GONE); web.loadUrl(entry); }); root.addView(error);
    web = new WebView(this);
    WebSettings settings = web.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
    web.setWebViewClient(new WebViewClient() {
      @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap favicon) { progress.setVisibility(View.VISIBLE); }
      @Override public void onPageFinished(WebView v, String url) { progress.setVisibility(View.GONE); }
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
        Uri u = request.getUrl(); if (trusted(u)) return false;
        if (request.isForMainFrame() && ("https".equals(u.getScheme()) || "tel".equals(u.getScheme()) || "mailto".equals(u.getScheme()))) {
          try { startActivity(new Intent(Intent.ACTION_VIEW, u)); }
          catch (android.content.ActivityNotFoundException ignored) { Toast.makeText(MainActivity.this, "No app available to open this link", Toast.LENGTH_SHORT).show(); }
        }
        return true;
      }
      @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
        if (r.isForMainFrame()) { error.setVisibility(View.VISIBLE); progress.setVisibility(View.GONE); }
      }
      @Override public void onReceivedHttpError(WebView v, WebResourceRequest r, WebResourceResponse e) {
        if (r.isForMainFrame() && e.getStatusCode() >= 400) { error.setVisibility(View.VISIBLE); progress.setVisibility(View.GONE); }
      }
    });
    root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
    if (state == null || web.restoreState(state) == null) web.loadUrl(entry);
  }
  private void switchShop() {
    new AlertDialog.Builder(this).setTitle("Remove this shop from this device?")
        .setMessage("You’ll need its private link to open it again. This doesn’t delete orders or revoke the link on other devices.")
        .setNegativeButton("Keep it", null).setPositiveButton("Remove shop", (d, which) -> {
          PrivateLink.clear(this); entry = null;
          web.clearHistory(); web.clearCache(true);
          android.webkit.WebStorage.getInstance().deleteAllData();
          CookieManager.getInstance().removeAllCookies(null); setup();
        }).show();
  }
  @Override protected void onSaveInstanceState(Bundle state) {
    // Reopen using the encrypted link rather than writing private URL/history into an OS bundle.
    super.onSaveInstanceState(state);
  }
  @Override public void onBackPressed() { if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
  @Override protected void onDestroy() { if (web != null) { web.destroy(); web = null; } super.onDestroy(); }
}
