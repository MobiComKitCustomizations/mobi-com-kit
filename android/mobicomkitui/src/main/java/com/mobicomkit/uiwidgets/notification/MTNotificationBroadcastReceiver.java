package com.mobicomkit.uiwidgets.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.mobicomkit.api.MobiComKitConstants;
import com.mobicomkit.api.conversation.Message;
import com.mobicomkit.api.notification.NotificationService;
import com.mobicomkit.uiwidgets.R;

import net.mobitexter.mobiframework.json.GsonUtils;
import net.mobitexter.mobiframework.people.contact.Contact;
import net.mobitexter.mobiframework.people.contact.ContactUtils;

/**
 * Created by adarsh on 3/5/15.
 */
public class MTNotificationBroadcastReceiver extends BroadcastReceiver {


    private static final String TAG = "MTBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Message message = null;
        String messageJson = intent.getStringExtra(MobiComKitConstants.MESSAGE_JSON_INTENT);
        Log.i(TAG, "Received broadcast, action: " + action + ", message: " + message);
        if (!TextUtils.isEmpty(messageJson)) {
            message = (Message) GsonUtils.getObjectFromJson(messageJson, Message.class);
        }
        NotificationService notificationService =
                new NotificationService(R.drawable.ic_launcher, context, R.string.wearable_action_label, R.string.wearable_action_title, R.drawable.ic_action_send);

        Log.i(TAG, "Received broadcast, action: " + action + ", sms: " + message);
        Contact contact = ContactUtils.getContact(context, message.getContactIds());

        notificationService.notifyUser(contact, message);

    }
}
