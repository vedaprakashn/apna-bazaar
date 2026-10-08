package com.heyhood.app;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

final class PushStore {
    static final String BASE="https://heyhood-production-b1b8.up.railway.app";
    static final String CHANNEL="hood_updates";
    private static final ExecutorService NETWORK=Executors.newSingleThreadExecutor();
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("hood_push",Context.MODE_PRIVATE);}
    static synchronized void initialize(Context c){
        SharedPreferences p=prefs(c);
        if(!p.contains("installation")) {
            byte[] b=new byte[32];new SecureRandom().nextBytes(b);StringBuilder secret=new StringBuilder();
            for(byte x:b)secret.append(String.format(Locale.ROOT,"%02x",x & 255));
            p.edit().putString("installation",UUID.randomUUID().toString()).putString("secret",secret.toString()).putString("community","tridasa").commit();
        }
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Your hood updates",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Opt-in shop promotions and community updates from HeyHood");
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    static boolean allowed(Context c){
        NotificationManager n=c.getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        NotificationChannel channel=n.getNotificationChannel(CHANNEL);
        return n.areNotificationsEnabled() && (channel==null || channel.getImportance()!=NotificationManager.IMPORTANCE_NONE);
    }
    static boolean enabled(Context c){return prefs(c).getBoolean("enabled",false) && allowed(c);}
    static void choose(Context c,boolean on,Runnable done){
        initialize(c);boolean value=on && allowed(c);
        prefs(c).edit().putBoolean("enabled",value).putBoolean("synced",false).apply();
        FirebaseMessaging.getInstance().setAutoInitEnabled(value);
        if(!value){FirebaseMessaging.getInstance().deleteToken();c.getSystemService(NotificationManager.class).cancelAll();}
        sync(c,done);
    }
    static void community(Context c,String slug){
        if(slug!=null && slug.matches("[a-z][a-z0-9-]{0,39}") && !slug.equals(prefs(c).getString("community","tridasa"))) {
            prefs(c).edit().putString("community",slug).putBoolean("synced",false).apply();sync(c,null);
        }
    }
    static void token(Context c,String token){prefs(c).edit().putString("token",token).putBoolean("synced",false).apply();sync(c,null);}
    static void sync(Context c,Runnable done){
        Context app=c.getApplicationContext();initialize(app);
        if(prefs(app).getBoolean("enabled",false) && !allowed(app)) {
            prefs(app).edit().putBoolean("enabled",false).putBoolean("synced",false).apply();
            FirebaseMessaging.getInstance().setAutoInitEnabled(false);FirebaseMessaging.getInstance().deleteToken();
        }
        if(enabled(app)) {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task->{
                if(task.isSuccessful()) {
                    prefs(app).edit().putString("token",task.getResult()).apply();NETWORK.execute(()->register(app,done));
                } else {prefs(app).edit().putBoolean("synced",false).apply();if(done!=null)done.run();}
            });
        } else NETWORK.execute(()->register(app,done));
    }
    private static void register(Context c,Runnable done){
        SharedPreferences p=prefs(c);String id=p.getString("installation","");boolean success=false;
        try {
            if(enabled(c)) {
                String token=p.getString("token","");if(!token.isEmpty()) {
                    JSONObject body=new JSONObject().put("token",token).put("consent",true);
                    success=request(c,"PUT","/api/"+p.getString("community","tridasa")+"/push/installations/"+id,body);
                }
            } else success=request(c,"DELETE","/api/push/installations/"+id,null);
            p.edit().putBoolean("synced",success).apply();flushEvents(c);
        } catch(Exception ignored){p.edit().putBoolean("synced",false).apply();}
        if(done!=null)done.run();
    }
    private static boolean request(Context c,String method,String path,JSONObject body){
        HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)new URL(BASE+path).openConnection();connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);connection.setReadTimeout(15000);connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization","Bearer "+prefs(c).getString("secret",""));
            if(body!=null){connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");try(OutputStream out=connection.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
            int status=connection.getResponseCode();return status>=200 && status<300;
        } catch(Exception ignored){return false;}finally{if(connection!=null)connection.disconnect();}
    }
    static void event(Context c,String delivery,String type){
        try{UUID.fromString(delivery);}catch(Exception invalid){return;}
        Context app=c.getApplicationContext();initialize(app);
        NETWORK.execute(()->{
            try {
                JSONArray queue=new JSONArray(prefs(app).getString("events","[]"));
                if(queue.length()<20)queue.put(new JSONObject().put("deliveryId",delivery).put("type",type));
                prefs(app).edit().putString("events",queue.toString()).commit();flushEvents(app);
            }catch(Exception ignored){}
        });
    }
    private static void flushEvents(Context c){
        try {
            JSONArray queue=new JSONArray(prefs(c).getString("events","[]")),remaining=new JSONArray();
            for(int i=0;i<queue.length();i++) {
                JSONObject event=queue.getJSONObject(i);
                if(!request(c,"POST","/api/push/installations/"+prefs(c).getString("installation","")+"/events",event))remaining.put(event);
            }
            prefs(c).edit().putString("events",remaining.toString()).commit();
        }catch(Exception ignored){}
    }
    static String state(Context c){
        try{return new JSONObject().put("supported",true).put("enabled",enabled(c)).put("synced",prefs(c).getBoolean("synced",false)).put("permission",allowed(c)).toString();}
        catch(Exception ignored){return "{}";}
    }
    static String safePath(String path){
        if(path!=null && path.matches("^/(provider|chatbot|activity|rides|plans|help)/index\\.html(?:\\?[^#\\\\]*)?$") && !path.contains("\n") && !path.contains("\r"))return path;
        return "/chatbot/index.html";
    }
}
