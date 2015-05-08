package com.mobicomkit.timer;

import android.util.Log;

import com.mobicomkit.communication.message.Message;
import com.mobicomkit.communication.message.MobiComMessageService;

import java.util.TimerTask;

/**
 * Created by devashish on 24/1/15.
 */
public class MessageSenderTimerTask extends TimerTask {

    private static final String TAG = "MessageSenderTimerTask";

    private MobiComMessageService mobiComMessageService;
    private Message message;
    private String to;

    public MessageSenderTimerTask(MobiComMessageService mobiComMessageService, Message message, String to) {
        this.mobiComMessageService = mobiComMessageService;
        this.message = message;
        this.to = to;
    }

    @Override
    public void run() {
        Log.i(TAG, "Sending message to: " + to + " from MessageSenderTimerTask");
        mobiComMessageService.processMessage(message, to);
    }
}