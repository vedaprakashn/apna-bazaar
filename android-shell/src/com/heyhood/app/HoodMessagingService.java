package com.heyhood.app;

import android.app.*;
import android.content.Intent;
import android.graphics.Color;
import com.google.firebase.messaging.*;
import java.util.Map;

public class HoodMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token){PushStore.initialize(this);PushStore.token(this,token);}
    @Override public void onMessageReceived(RemoteMessage message){
        PushStore.initialize(this);if(!PushStore.enabled(this))return;
        Map<String,String> data=message.getData();String delivery=data.get("deliveryId");
        if(delivery==null)return;
        String title=message.getNotification()!=null?message.getNotification().getTitle():"A little good from your hood";
        String body=message.getNotification()!=null?message.getNotification().getBody():"See what’s happening nearby.";
        Intent intent=new Intent(this,MainActivity.class).putExtra("path",PushStore.safePath(data.get("path"))).putExtra("deliveryId",delivery)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap=PendingIntent.getActivity(this,delivery.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(this,PushStore.CHANNEL).setSmallIcon(com.heyhood.app.R.drawable.notification_icon)
                .setColor(Color.rgb(110,48,255)).setContentTitle(title).setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(tap).build();
        getSystemService(NotificationManager.class).notify(delivery.hashCode(),notification);
        PushStore.event(this,delivery,"received");
    }
}
