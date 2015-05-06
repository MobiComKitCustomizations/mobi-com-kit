package com.mobicomkit.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;


import com.mobicomkit.MobiComKitConstants;
import com.mobicomkit.client.ui.MessageIntentService;
import com.mobicomkit.notification.WearableNotificationWithVoice;
import com.mobicomkit.communication.message.Message;
import com.mobicomkit.user.MobiComUserPreference;

import net.mobitexter.mobiframework.json.GsonUtils;

/**
 * Created by adarsh on 4/12/14.
 * <p/>
 * This class should handle all notification types coming from server or some other client.
 * Depending upon actionType it should either do some bg service,task or open some view like activity.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    public static String LAUNCH_MOBITEXTER = "net.mobitexter.LAUNCH_APP";

    @Override
    public void onReceive(Context context, Intent intent) {

        String actionName = intent.getAction();

        String messageJson = intent.getStringExtra(MobiComKitConstants.MESSAGE_JSON_INTENT);
        Intent newIntent;
        if (actionName.equals(LAUNCH_MOBITEXTER)) {
            String messageText = getMessageText(intent) == null ? null : getMessageText(intent).toString();
            if (!TextUtils.isEmpty(messageText)) {
                Message message = (Message) GsonUtils.getObjectFromJson(messageJson, Message.class);
                Message replyMessage = new Message();
                replyMessage.setTo(message.getTo());
                replyMessage.setStoreOnDevice(Boolean.TRUE);
                replyMessage.setSendToDevice(Boolean.FALSE);
                replyMessage.setType(Message.MessageType.MT_OUTBOX.getValue());
                replyMessage.setMessage(messageText);
                replyMessage.setDeviceKeyString(MobiComUserPreference.getInstance(context).getDeviceKeyString());
                replyMessage.setSource(Message.Source.MT_MOBILE_APP.getValue());

                newIntent = new Intent(context, MessageIntentService.class);
                newIntent.putExtra(MobiComKitConstants.MESSAGE_JSON_INTENT, GsonUtils.getJsonFromObject(replyMessage, Message.class));
                context.startService(newIntent);
                return;
            }
            //TODO: get activity name in intent...
            newIntent = new Intent(context,getActivityToOpen("com.mobicomkit.client.ui.activity.SlidingPaneActivity") );
            newIntent.putExtra(MobiComKitConstants.MESSAGE_JSON_INTENT, messageJson);
            newIntent.putExtra("sms_body", "text");
            newIntent.setType("vnd.android-dir/mms-sms");
            newIntent.setAction(NotificationBroadcastReceiver.LAUNCH_MOBITEXTER);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newIntent);
        }
    }

    private CharSequence getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(WearableNotificationWithVoice.EXTRA_VOICE_REPLY);
        }
        return null;
    }

    public Class getActivityToOpen(String StringClassname ) {
        if (StringClassname != null) {
            try {
                Class c = Class.forName(StringClassname);
                return c;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();

            }
        }
        return null;
    }


}
