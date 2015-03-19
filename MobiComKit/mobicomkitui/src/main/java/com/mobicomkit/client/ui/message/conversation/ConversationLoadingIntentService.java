package com.mobicomkit.client.ui.message.conversation;

import android.app.IntentService;
import android.content.Intent;

import com.mobicomkit.broadcast.BroadcastService;
import com.mobicomkit.communication.message.Message;

import java.util.List;

/**
 * Created by devashish on 15/12/13.
 */
public class ConversationLoadingIntentService extends IntentService {

    public ConversationLoadingIntentService() {
        super("ConversationLoadingIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            ConversationService conversationService = new ConversationService(this);
            Long createdAt = intent.getLongExtra("createdAt", 0L);
            List<Message> smsList = conversationService.getMessageList(null, createdAt, null, null);

            boolean notify = intent.getBooleanExtra("notify", false);
            if (notify) {
                BroadcastService.sendFirstTimeSyncCompletedBroadcast(this);
            } else {
                BroadcastService.sendLoadMoreBroadcast(this, !smsList.isEmpty());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
