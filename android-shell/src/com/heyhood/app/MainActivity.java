package com.heyhood.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.widget.*;
import android.graphics.Color;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import android.provider.Settings;
import org.json.*;

public class MainActivity extends Activity {
    private static final String HOME = "https://heyhood-production-b1b8.up.railway.app/chatbot/index.html";
    private WebView web;
    private TextView error;
    private static final int NOTIFICATIONS = 42;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        PushStore.initialize(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        error = new TextView(this);
        error.setPadding(24, 24, 24, 24);
        error.setText("Can’t reach your hood right now. Tap here to retry.");
        error.setVisibility(android.view.View.GONE);
        error.setOnClickListener(v -> { error.setVisibility(android.view.View.GONE); web.loadUrl(HOME); });
        root.addView(error);
        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("heyhood".equals(uri.getScheme())) {
                    if(request.isForMainFrame() && trusted(view.getUrl()) && "notifications".equals(uri.getHost())) notificationAction(uri.getPath());
                    return true;
                }
                if ("https".equals(uri.getScheme()) && "heyhood-production-b1b8.up.railway.app".equals(uri.getHost())) return false;
                if (!request.isForMainFrame()) return true;
                if ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()) || "whatsapp".equals(uri.getScheme()) || "tel".equals(uri.getScheme()) || "mailto".equals(uri.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                    catch (android.content.ActivityNotFoundException e) { Toast.makeText(MainActivity.this, "No app available to open this link", Toast.LENGTH_SHORT).show(); }
                }
                return true;
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError failure) {
                if (request.isForMainFrame()) error.setVisibility(android.view.View.VISIBLE);
            }
            @Override public void onPageFinished(WebView view, String url) {
                if(!trusted(url))return;setTitle("HeyHood");publishPushState();
                view.evaluateJavascript("JSON.stringify({community:new URLSearchParams(location.search).get('community')||sessionStorage.getItem('heyhood-community')||document.querySelector('#community')?.value||'tridasa'})",value->{
                    try {String decoded=(String)new JSONTokener(value).nextValue();PushStore.community(MainActivity.this,new JSONObject(decoded).getString("community"));}
                    catch(Exception ignored){}
                });
            }
        });
        if (state == null || web.restoreState(state) == null) web.loadUrl(HOME);
        openNotification(getIntent());
    }
    private boolean trusted(String url){if(url==null)return false;Uri u=Uri.parse(url);return "https".equals(u.getScheme()) && "heyhood-production-b1b8.up.railway.app".equals(u.getHost());}
    private void publishPushState(){runOnUiThread(()->{if(web!=null && trusted(web.getUrl()))web.evaluateJavascript("window.heyhoodPushState="+PushStore.state(this)+";window.dispatchEvent(new Event('heyhood-push-state'));",null);});}
    private void notificationAction(String action){
        if("/disable".equals(action)){PushStore.choose(this,false,this::publishPushState);publishPushState();return;}
        if("/settings".equals(action)){startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));return;}
        if(!"/enable".equals(action))return;
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATIONS);return;}
        if(!PushStore.allowed(this)){Toast.makeText(this,"Enable HeyHood notifications in phone settings, then turn them on here.",Toast.LENGTH_LONG).show();notificationAction("/settings");return;}
        PushStore.choose(this,true,this::publishPushState);publishPushState();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==NOTIFICATIONS){PushStore.choose(this,results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED,this::publishPushState);publishPushState();}}
    private void openNotification(Intent intent){
        String delivery=intent.getStringExtra("deliveryId");if(delivery==null)return;
        PushStore.event(this,delivery,"opened");web.loadUrl(PushStore.BASE+PushStore.safePath(intent.getStringExtra("path")));intent.removeExtra("deliveryId");
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);openNotification(intent);}
    @Override protected void onResume(){super.onResume();PushStore.sync(this,this::publishPushState);}
    @Override protected void onSaveInstanceState(Bundle state) { web.saveState(state); super.onSaveInstanceState(state); }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
