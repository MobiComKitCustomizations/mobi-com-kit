package net.mobitexter.mobicom;

import android.content.Context;
import android.util.Log;

import com.mobicomkit.broadcast.BroadcastService;
import com.mobicomkit.communication.message.Message;
import com.mobicomkit.communication.message.MessageClientService;
import com.mobicomkit.communication.message.database.MessageDatabaseService;
import com.mobicomkit.sync.SyncMessageFeed;
import com.mobicomkit.user.MobiComUserPreference;

import java.util.List;

/**
 * Created by adarsh on 22/3/15.
 */
public class BaseMessageService {

    Context context;
    private MessageDatabaseService messageDatabaseService;
    private MessageClientService messageClientService;
    private static final String TAG = "MessageService";


    public BaseMessageService(Context context) {
        this.context = context;
        this.messageDatabaseService = new MessageDatabaseService(context);
        this.messageClientService = new MessageClientService(context);
    }

    public void updateDeliveryStatus(String payloadForDelivered) {

    }


    public void syncSms() {

        final MobiComUserPreference userpref = MobiComUserPreference.getInstance(context);
        SyncMessageFeed syncMessageFeed = messageClientService.getMessageFeed(userpref.getDeviceKeyString(), userpref.getLastSyncTime());
        Log.i(TAG, "Got sync response " + syncMessageFeed);
        List<Message> messageList = syncMessageFeed.getMessages();
        Log.i(TAG, "got messages : " + messageList.size());
        for ( Message message : messageList) {
            BroadcastService.sendMessageUpdateBroadcast(context, BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString(), message);
            messageDatabaseService.createMessage(message);
        }
    }

}
