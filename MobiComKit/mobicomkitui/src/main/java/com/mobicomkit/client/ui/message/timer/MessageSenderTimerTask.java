package com.mobicomkit.client.ui.message.timer;

import android.content.Context;
import android.util.Log;

import com.mobicomkit.client.ui.message.MessageService;
import com.mobicomkit.communication.message.Message;

import java.util.TimerTask;

/**
 * Created by devashish on 24/1/15.
 */
public class MessageSenderTimerTask extends TimerTask {
    private static final String TAG = "MessageSenderTimerTask";

    private Context context;
    private Message message;
    private String to;

    public MessageSenderTimerTask(Context context, Message message, String to) {
        this.context = context;
        this.message = message;
        this.to = to;
    }

    @Override
    public void run() {
        Log.i(TAG, "Sending message to: " + to + " from MessageSenderTimerTask");
        new MessageService(context).processSms(message, to);
    }
}