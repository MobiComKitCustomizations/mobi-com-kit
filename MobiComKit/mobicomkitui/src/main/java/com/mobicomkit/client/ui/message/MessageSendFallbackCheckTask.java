package com.mobicomkit.client.ui.message;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.mobicomkit.MobiComKitConstants;
import com.mobicomkit.client.ui.message.conversation.ConversationService;
import com.mobicomkit.broadcast.BroadcastService;
import com.mobicomkit.communication.message.Message;
import com.mobicomkit.communication.message.MessageClientService;
import com.mobicomkit.communication.message.database.MessageDatabaseService;
import net.mobitexter.mobiframework.json.GsonUtils;

import java.util.Date;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: adarsh
 * Date: 29/7/13
 * Time: 11:34 PM
  */
public class MessageSendFallbackCheckTask extends TimerTask {
    private static final String TAG = "MessageSendFallbackCheckTask";

    private Context context;
    private Uri smsUri;
    private Message message;
    private String mapKey;
    private String address;

    public MessageSendFallbackCheckTask(Context context, Uri smsUri, Message sms, String mapKey, String address) {
        this.context = context;
        this.smsUri = smsUri;
        this.message = sms;
        this.mapKey = mapKey;
        this.address = address;
    }

    @Override
    public void run() {
        if (MessageService.mtMessages.containsKey(mapKey) || MessageService.map.containsKey(mapKey)) {
            Log.i(TAG, "Sending message by fallback to carrier: " + message);
            String data = new MessageClientService(context).deleteMessage(message);
            Log.i(TAG, "Delete message response: " + data);
            ConversationService conversationService = new ConversationService(context);
            String contactNumbers = conversationService.deleteMessageFromDevice(new MessageDatabaseService(context).getSms(message.getKeyString()), null);
            BroadcastService.sendMessageDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_MESSAGE.toString(), message.getKeyString(), contactNumbers);

            MessageClientService.recentProcessedMessage.remove(message);

            Message fallbackSms = new Message(message);
            fallbackSms.setCreatedAtTime(new Date().getTime());
            fallbackSms.setType(Message.MessageType.OUTBOX.getValue());
            fallbackSms.setSendToDevice(Boolean.FALSE);
            Intent intent = new Intent(context, MessageIntentService.class);
            intent.putExtra(MobiComKitConstants.MESSAGE_JSON_INTENT, GsonUtils.getJsonFromObject(fallbackSms, Message.class));
            context.startService(intent);
        } else {
            Log.i(TAG, "Sms is already delivered.");
        }
        MessageService.map.remove(mapKey);
        MessageService.mtMessages.remove(mapKey);
    }
}
