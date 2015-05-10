package com.mobicomkit.api.conversation.selfdestruct;

import android.content.Context;
import android.util.Log;

import com.mobicomkit.api.conversation.Message;
import com.mobicomkit.api.conversation.MobiComConversationService;
import com.mobicomkit.broadcast.BroadcastService;

import java.util.TimerTask;

public class DisappearingMessageTask extends TimerTask {
    private static final String TAG = "DisappearingMessageTask";

    private Context context;
    private MobiComConversationService conversationService;
    private Message message;

    public DisappearingMessageTask(Context context, MobiComConversationService conversationService, Message sms) {
        this.context = context;
        this.conversationService = conversationService;
        this.message = sms;
    }

    @Override
    public void run() {
        String smsKeyString = message.getKeyString();
        Log.i(TAG, "Self deleting message for keyString: " + smsKeyString);
        conversationService.deleteMessage(message);
        BroadcastService.sendMessageDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_MESSAGE.toString(), smsKeyString, message.getContactIds());
    }
}
