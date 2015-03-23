package com.mobicomkit.client.ui.message;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.c2dm.C2DMessaging;
import com.mobicomkit.MobiComKitConstants;
import com.mobicomkit.broadcast.BroadcastService;
import com.mobicomkit.client.ui.GeneralConstants;
import com.mobicomkit.client.ui.NotificationService;
import com.mobicomkit.client.ui.R;
import com.mobicomkit.client.ui.message.conversation.ConversationService;
import com.mobicomkit.client.ui.message.timer.MessageSenderTimerTask;
import com.mobicomkit.communication.message.Message;
import com.mobicomkit.communication.message.MessageClientService;
import com.mobicomkit.communication.message.database.MessageDatabaseService;
import com.mobicomkit.communication.message.schedule.ScheduledMessageUtil;
import com.mobicomkit.communication.message.selfdestruct.DisappearingMessageTask;
import com.mobicomkit.sync.SyncMessageFeed;
import com.mobicomkit.user.MobiComUserPreference;

import net.mobitexter.mobiframework.commons.core.utils.ContactNumberUtils;
import net.mobitexter.mobiframework.commons.core.utils.Support;
import net.mobitexter.mobiframework.json.GsonUtils;
import net.mobitexter.mobiframework.people.contact.Contact;
import net.mobitexter.mobiframework.people.contact.ContactUtils;
import net.mobitexter.mobiframework.personalization.PersonalizedMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

/**
 * Created by devashish on 28/11/14.
 */
public class MessageService {

    private static final String TAG = "MessageService";

    public static final long DELAY = 60000L;
    public static Map<String, Uri> map = new HashMap<String, Uri>();
    public static Map<String, Message> mtMessages = new LinkedHashMap<String, Message>();
    private Context context;
    private MessageDatabaseService messageDatabaseService;
    private MessageClientService messageClientService;

    public MessageService(Context context) {
        this.context = context;
        this.messageDatabaseService = new MessageDatabaseService(context);
        this.messageClientService = new MessageClientService(context);
    }

    public synchronized void syncSms() {
        Log.i(TAG, "Starting syncSms");

        final MobiComUserPreference userpref = MobiComUserPreference.getInstance(context);
        SyncMessageFeed syncMessageFeed = messageClientService.getMessageFeed(userpref.getDeviceKeyString(), userpref.getLastSyncTime());
        Log.i(TAG, "Got sync response " + syncMessageFeed);
        // if regIdInvalid in syncrequest, tht means device reg with c2dm is no
        // more valid, do it again and make the sync request again
        if (syncMessageFeed != null && syncMessageFeed.isRegIdInvalid()
                && GeneralConstants.ANDROID_VERSION >= 8) {
            Log.i(TAG,
                    "Going to call C2DM device registration with "
                            + GeneralConstants.SENDER_ID);
            C2DMessaging.register(context);
        }
        if (syncMessageFeed != null && syncMessageFeed.getMessages() != null) {
            List<Message> messageList = syncMessageFeed.getMessages();
            Log.i(TAG, "got messages : " + messageList.size());

            for (final Message message : messageList) {
                Log.i(TAG, "calling  syncSms : " + message.getTo() + " " + message.getMessage());

                if (message.getScheduledAt() != null) {
                    new ScheduledMessageUtil(context, null).createScheduleMessage(message, context);
                    MobiComUserPreference.getInstance(context).setLastInboxSyncTime(message.getCreatedAtTime());
                } else {
                    String[] toList = message.getTo().trim().replace("undefined,", "").split(",");
                    final int groupSmsDelayInSec = MobiComUserPreference.getInstance(context).getGroupSmsDelayInSec();
                    boolean isDelayRequire = groupSmsDelayInSec > 0 && message.isSentViaCarrier() && message.isSentToMany();

                    for (String tofield : toList) {
                        if (isDelayRequire && !message.getTo().startsWith(tofield)) {
                            new Timer().schedule(new MessageSenderTimerTask(context, message, tofield), groupSmsDelayInSec * 1000);
                        } else {
                            processSms(message, tofield);
                        }
                        MobiComUserPreference.getInstance(context).setLastInboxSyncTime(message.getCreatedAtTime());
                    }
                }

                MessageClientService.recentProcessedMessage.add(message);
                BroadcastService.sendMessageUpdateBroadcast(context, BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString(), message);

                messageDatabaseService.createMessage(message);
            }

            //OsuConstants.LAST_SYNC_TIME = messageFeed.getCurrentSyncTime().toString();
            userpref.setLastSyncTime(String.valueOf(syncMessageFeed.getLastSyncTime()));
            /*if (!calledFromC2DM) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        txtLastSyncTime.setText("Last Sync Time: " +
                                new Date(Long.parseLong(UserPreferences.OsuDBHelper(context).getLastSyncTime())).toLocaleString());

                    }
                });
            }*/
        }
    }

    public void addMTMessage(Message mTextSmsReceived) {
        MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);
        if (userPreferences.getCountryCode() != null) {
            mTextSmsReceived.setContactIds(ContactNumberUtils.getPhoneNumber(mTextSmsReceived.getTo(), userPreferences.getCountryCode()));
        } else {
            mTextSmsReceived.setContactIds(ContactUtils.getContactId(mTextSmsReceived.getTo(), context.getContentResolver()));
        }

        mTextSmsReceived.setTo(mTextSmsReceived.getTo());
        Contact receiverContact = ContactUtils.getContact(context, mTextSmsReceived.getTo());

        if (mTextSmsReceived.getMessage() != null && PersonalizedMessage.isPersonalized(mTextSmsReceived.getMessage())) {
            mTextSmsReceived.setMessage(PersonalizedMessage.prepareMessageFromTemplate(mTextSmsReceived.getMessage(), receiverContact));
        }

        messageDatabaseService.createMessage(mTextSmsReceived);

        Contact contact = ContactUtils.getContact(context, mTextSmsReceived.getTo());
        BroadcastService.sendMessageUpdateBroadcast(context, BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString(), mTextSmsReceived);
        //Todo: use email if contact number is empty
        Log.i(TAG, "Updating delivery status: " + mTextSmsReceived.getPairedMessageKeyString() + ", " + userPreferences.getContactNumber());
        NotificationService.notifyUser(context, contact, mTextSmsReceived);
        messageClientService.updateDeliveryStatus(mTextSmsReceived.getPairedMessageKeyString(), userPreferences.getContactNumber());
    }

    public void putMtextToDatabase(String payloadForMtextReceived) {
        JSONObject json = null;
        try {
            json = new JSONObject(payloadForMtextReceived);

            String smsKeyString = json.getString("keyString");
            String receiverNumber = json.getString("contactNumber");
            String body = json.getString("message");
            Integer timeToLive = json.isNull("timeToLive") ? null : Integer.parseInt(json.getString("timeToLive"));
            Message mTextMessageReceived = new Message();
            mTextMessageReceived.setTo(json.getString("senderContactNumber"));
            mTextMessageReceived.setCreatedAtTime(System.currentTimeMillis());
            mTextMessageReceived.setMessage(body);
            mTextMessageReceived.setSendToDevice(Boolean.FALSE);
            mTextMessageReceived.setSent(Boolean.TRUE);
            mTextMessageReceived.setDeviceKeyString(MobiComUserPreference.getInstance(context).getDeviceKeyString());
            mTextMessageReceived.setType(Message.MessageType.MT_INBOX.getValue());
            mTextMessageReceived.setSource(Message.Source.MT_MOBILE_APP.getValue());
            mTextMessageReceived.setTimeToLive(timeToLive);

            if (json.has("fileMetaKeyStrings")) {
                JSONArray fileMetaKeyStringsJSONArray = json.getJSONArray("fileMetaKeyStrings");
                List<String> fileMetaKeyStrings = new ArrayList<String>();
                for (int i = 0; i < fileMetaKeyStringsJSONArray.length(); i++) {
                    JSONObject fileMeta = fileMetaKeyStringsJSONArray.getJSONObject(i);
                    fileMetaKeyStrings.add(fileMeta.toString());
                }
                mTextMessageReceived.setFileMetaKeyStrings(fileMetaKeyStrings);
            }

            MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);
            if (userPreferences.getCountryCode() != null) {
                mTextMessageReceived.setContactIds(ContactNumberUtils.getPhoneNumber(mTextMessageReceived.getTo(), userPreferences.getCountryCode()));
            } else {
                mTextMessageReceived.setContactIds(ContactUtils.getContactId(mTextMessageReceived.getTo(), context.getContentResolver()));
            }

            mTextMessageReceived.setTo(mTextMessageReceived.getTo());
            Contact receiverContact = ContactUtils.getContact(context, receiverNumber);

            if (mTextMessageReceived.getMessage() != null && PersonalizedMessage.isPersonalized(mTextMessageReceived.getMessage())) {
                mTextMessageReceived.setMessage(PersonalizedMessage.prepareMessageFromTemplate(mTextMessageReceived.getMessage(), receiverContact));
            }

            Contact contact = ContactUtils.getContact(context, mTextMessageReceived.getTo());
            NotificationService.notifyUser(context, contact, mTextMessageReceived);

            try {
                messageClientService.sendMessageToServer(mTextMessageReceived, null);
            } catch (Exception ex) {
                Log.i(TAG, "Received Sms error " + ex.getMessage());
            }
            messageClientService.updateDeliveryStatus(smsKeyString, receiverNumber);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }

    public void addCallMessage(Message.MessageType messageType, String phoneNumber, Long createdAtTime) {
        Message message = new Message();
        MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);
        message.setContactIds(ContactNumberUtils.getPhoneNumber(phoneNumber, userPreferences.getCountryCode()));
        message.setTo(phoneNumber);
        message.setMessage(context.getString(Message.MessageType.CALL_OUTGOING.equals(messageType) ? R.string.outgoing_call : R.string.incoming_call));
        message.setCreatedAtTime(createdAtTime);
        message.setStoreOnDevice(Boolean.TRUE);
        message.setSendToDevice(Boolean.FALSE);
        message.setType(messageType.getValue());
        message.setDeviceKeyString(userPreferences.getDeviceKeyString());
        message.setSource(Message.Source.DEVICE_NATIVE_APP.getValue());
        Intent intent = new Intent(context, MessageIntentService.class);
        intent.putExtra(MobiComKitConstants.MESSAGE_JSON_INTENT, GsonUtils.getJsonFromObject(message, Message.class));
        context.startService(intent);
    }

    public void addWelcomeMessage() {
        Message message = new Message();
        MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);
        message.setContactIds(Support.getSupportNumber());
        message.setTo(Support.getSupportNumber());
        message.setMessage(context.getString(R.string.welcome_message));
        message.setStoreOnDevice(Boolean.TRUE);
        message.setSendToDevice(Boolean.FALSE);
        message.setType(Message.MessageType.MT_INBOX.getValue());
        message.setDeviceKeyString(userPreferences.getDeviceKeyString());
        message.setSource(Message.Source.MT_MOBILE_APP.getValue());
        //ContactService.createOrUpdateMTContacts(context, Support.getSupportNumber(), OsuConstants.APP_CODE_VERSION);
        Intent intent = new Intent(context, MessageIntentService.class);
        intent.putExtra(MobiComKitConstants.MESSAGE_JSON_INTENT, GsonUtils.getJsonFromObject(message, Message.class));
        context.startService(intent);
    }

    public synchronized void syncMessagesWithServer() {
        Toast.makeText(context, R.string.sync_messages_from_server, Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                syncSms();
            }
        }).start();
    }

   /* public synchronized static void asyncLoadMessagesFromServer(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new ConversationUtil(context).getQuickMessages(false);
            }
        }).start();
    } */


    public void processSms(final Message messageToProcess, String tofield) {
        Message message = new Message(messageToProcess);
        message.setMessageId(messageToProcess.getMessageId());
        message.setKeyString(messageToProcess.getKeyString());
        message.setPairedMessageKeyString(messageToProcess.getPairedMessageKeyString());

        if (message.getMessage() != null && PersonalizedMessage.isPersonalized(message.getMessage())) {
            Contact contact = ContactUtils.getContact(context, tofield);
            if (contact != null) {
                message.setMessage(PersonalizedMessage.prepareMessageFromTemplate(message.getMessage(), contact));
            }
        }

        if (message.getType().equals(Message.MessageType.OUTBOX.getValue())) {
            //Note: native sms not supported
        } else if (message.getType().equals(Message.MessageType.MT_INBOX.getValue())) {
            addMTMessage(message);
            //TODO: in case of isStoreOn device is false ..have to handle fall back
        } else if (message.getType().equals(Message.MessageType.MT_OUTBOX.getValue())) {
            Uri uri = null;
            Log.i(TAG, "Sending mt message");
            String mapKey = message.getKeyString() + "," + message.getContactIds();
            map.put(mapKey, uri);
            mtMessages.put(mapKey, message);
        }
        Log.i(TAG, "Sending message: " + message);
    }

    public void updateDeliveryStatus(String key) {
        //Todo: Check if this is possible? In case the delivery report reaches before the sms is reached, then wait for the sms.
        Log.i(TAG, "Got the delivery report for key: " + key);
        Uri uri = map.get(key);

        String keyParts[] = key.split((","));
        Message message = messageDatabaseService.getSms(keyParts[0]);
        if (message != null) {
            message.setDelivered(Boolean.TRUE);
            //Todo: Server need to send the contactNumber of the receiver in case of group messaging and update
            //delivery report only for that number
            messageDatabaseService.updateMessageDeliveryReport(keyParts[0], null);
            BroadcastService.sendMessageUpdateBroadcast(context, BroadcastService.INTENT_ACTIONS.MESSAGE_DELIVERY.toString(), message);
            if (message.getTimeToLive() != null && message.getTimeToLive() != 0) {
                Timer timer = new Timer();
                timer.schedule(new DisappearingMessageTask(context, new ConversationService(context), message), message.getTimeToLive() * 60 * 1000);
            }
        } else {
            Log.i(TAG, "Sms is not present in table, keyString: " + keyParts[0]);
        }
        map.remove(key);
        mtMessages.remove(key);
    }

}
