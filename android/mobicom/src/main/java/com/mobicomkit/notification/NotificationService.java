package com.mobicomkit.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneNumberUtils;

import com.mobicomkit.MobiComKitClientService;
import com.mobicomkit.MobiComKitConstants;

import com.mobicomkit.broadcast.NotificationBroadcastReceiver;
import com.mobicomkit.communication.message.Message;
import com.mobicomkit.userinterface.BaseMobiComActivity;


import net.mobitexter.mobiframework.json.GsonUtils;
import net.mobitexter.mobiframework.people.contact.Contact;

import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Created with IntelliJ IDEA.
 * User: devashish
 * Date: 17/3/13
 * Time: 7:36 PM
 */
public class NotificationService {

    private static final int NOTIFICATION_ID = 1000;
    private Context context;
    private int iconResourceId;
    private int wearable_action_title;
    private int wearable_action_label;
    private int wearable_send_icon;


    public NotificationService( int iconResourceID, Context context,int wearable_action_label,int wearable_action_title,int wearable_send_icon){
        this.context =  context;
        this.iconResourceId = iconResourceID;
        this.wearable_action_label= wearable_action_label;
        this.wearable_action_title= wearable_action_title;
        this.wearable_send_icon = wearable_send_icon;


    }

    public void notifyUser(Contact contact, Message sms) {
       if (BaseMobiComActivity.mobiTexterBroadcastReceiverActivated &&
                (BaseMobiComActivity.currentOpenedContactNumber == null ||
                        PhoneNumberUtils.compare(sms.getContactIds(), BaseMobiComActivity.currentOpenedContactNumber))) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(MobiComKitConstants.MESSAGE_JSON_INTENT, GsonUtils.getJsonFromObject(sms, Message.class));
        intent.setAction(NotificationBroadcastReceiver.LAUNCH_MOBITEXTER);
        intent.setClass(context,NotificationBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) (System.currentTimeMillis() & 0xfffffff), intent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(iconResourceId)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), iconResourceId))
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setWhen(System.currentTimeMillis())
                        .setContentTitle(contact.getFullName() != null ? contact.getFullName() : contact.getContactNumber())
                        .setContentText(sms.getMessage())
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);
        if (sms.hasAttachment()) {
            try {
                InputStream in;
                HttpURLConnection httpConn = new MobiComKitClientService(context).openHttpConnection(sms.getFileMetas().get(0).getThumbnailUrl());
                int response = httpConn.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {
                    in = httpConn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(in);
                    mBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        WearableNotificationWithVoice notificationWithVoice =
                new WearableNotificationWithVoice(mBuilder,wearable_action_title,
                        wearable_action_label,wearable_send_icon,sms.getContactIds().hashCode());
        notificationWithVoice.setCurrentContext(context);
        notificationWithVoice.setPendingIntent(pendingIntent);

        try {
            notificationWithVoice.sendNotification();
        }catch (RuntimeException e){
            e.printStackTrace();
        }
    }

    public static void notifyUserForMT(Context context, String contactNumber) {
       /* NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        intent.putExtra("contactNumber", contactNumber);
        intent.setAction(String.valueOf(R.string.launch_mobitexter_app));

        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff), intent, 0);

        Contact contact = ContactUtils.getContact(context, contactNumber);
        if (contact.getContactId() == 0) {
            return;
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .setWhen(System.currentTimeMillis())
                        .setContentTitle(contact.getFullName() != null ? contact.getFullName() : contact.getContactNumber())
                        .setContentText((contact.getFullName() != null ? contact.getFullName() : contact.getContactNumber()) + " joined MobiTexter")
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        mBuilder.setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);*/
    }

}
