package com.mobicomkit.client.ui.message;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.mobicomkit.MobiComKitConstants;
import com.mobicomkit.client.ui.message.timer.MessageSenderTimerTask;
import com.mobicomkit.communication.message.Message;
import com.mobicomkit.communication.message.MessageClientService;
import com.mobicomkit.user.MobiComUserPreference;

import net.mobitexter.mobiframework.json.GsonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

/**
 * Created by devashish on 15/12/13.
 */
public class MessageIntentService extends IntentService {

    private static final String TAG = "MessageIntentService";
    public static final String UPLOAD_CANCEL = "cancel_upload";

    private Map<String, Thread> runningTaskMap = new HashMap<String, Thread>();

    public MessageIntentService() {
        super("SmsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getStringExtra(UPLOAD_CANCEL) != null) {
            //TODO: not completed yet ....
            Thread thread = runningTaskMap.get(intent.getStringExtra(UPLOAD_CANCEL));
            if (thread != null) {
                thread.interrupt();
            } else {
                Log.w(TAG, "Thread not found..." + runningTaskMap);
            }
            return;
            //System.out.println("@@@ interupting threads....");
        }
        final Message message = (Message) GsonUtils.getObjectFromJson(intent.getStringExtra(MobiComKitConstants.MESSAGE_JSON_INTENT), Message.class);
        Thread thread = new Thread(new MessegeSender(message));
        thread.start();

        if (message.hasAttachment()) {
            runningTaskMap.put(getMapKey(message), thread);
        }
    }

    private class MessegeSender implements Runnable {
        private Message message;

        public MessegeSender(Message sms) {
            this.message = sms;
        }

        @Override
        public void run() {
            try {
                new MessageClientService(MessageIntentService.this).sendMessageToServer(message, null);
                if (message.hasAttachment() && !message.isAttachmentUploadInProgress()) {
                    runningTaskMap.remove(getMapKey(message));
                }
                int groupSmsDelayInSec = MobiComUserPreference.getInstance(MessageIntentService.this).getGroupSmsDelayInSec();
                boolean isDelayRequire = (groupSmsDelayInSec > 0 && message.isSentViaCarrier() && message.isSentToMany());
                if (message.getScheduledAt() == null) {
                    String[] toList = message.getTo().trim().replace("undefined,", "").split(",");

                    for (String tofield : toList) {
                        if (isDelayRequire && !message.getTo().startsWith(tofield)) {
                            new Timer().schedule(new MessageSenderTimerTask(MessageIntentService.this, message, tofield), groupSmsDelayInSec * 1000);
                        } else {
                            new MessageService(MessageIntentService.this).processSms(message, tofield);
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getMapKey(Message sms) {
        return sms.getFilePaths().get(0) + sms.getContactIds();
    }
}
