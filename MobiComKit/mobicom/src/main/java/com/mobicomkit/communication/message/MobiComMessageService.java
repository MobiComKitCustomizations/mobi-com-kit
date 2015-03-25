package com.mobicomkit.communication.message;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.mobicomkit.GeneralConstants;
import com.mobicomkit.MobiComKitConstants;
import com.mobicomkit.R;
import com.mobicomkit.broadcast.BroadcastService;
import com.mobicomkit.client.ui.NotificationService;
import com.mobicomkit.communication.message.conversation.MobiComConversationService;
import com.mobicomkit.communication.message.database.MessageDatabaseService;
import com.mobicomkit.communication.message.schedule.ScheduledMessageUtil;
import com.mobicomkit.communication.message.selfdestruct.DisappearingMessageTask;
import com.mobicomkit.sync.SyncMessageFeed;
import com.mobicomkit.timer.MessageSenderTimerTask;
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
 * Created by devashish on 24/3/15.
 */
public class MobiComMessageService {

    private static final String TAG = "MobiComMessageService";

    public static final long DELAY = 60000L;
    public static Map<String, Uri> map = new HashMap<String, Uri>();
    protected Context context;
    protected MessageDatabaseService messageDatabaseService;
    protected MessageClientService messageClientService;
    protected Class messageIntentServiceClass;
    public static Map<String, Message> mtMessages = new LinkedHashMap<String, Message>();

    public MobiComMessageService(Context context, Class messageIntentServiceClass) {
        this.context = context;
        this.messageDatabaseService = new MessageDatabaseService(context);
        this.messageClientService = new MessageClientService(context);
        this.messageIntentServiceClass = messageIntentServiceClass;
    }

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

    public Contact addMTMessage(Message message) {
        MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);
        if (userPreferences.getCountryCode() != null) {
            message.setContactIds(ContactNumberUtils.getPhoneNumber(message.getTo(), userPreferences.getCountryCode()));
        } else {
            message.setContactIds(ContactUtils.getContactId(message.getTo(), context.getContentResolver()));
        }

        message.setTo(message.getTo());
        Contact receiverContact = ContactUtils.getContact(context, message.getTo());

        if (message.getMessage() != null && PersonalizedMessage.isPersonalized(message.getMessage())) {
            message.setMessage(PersonalizedMessage.prepareMessageFromTemplate(message.getMessage(), receiverContact));
        }

        messageDatabaseService.createMessage(message);

        Contact contact = ContactUtils.getContact(context, message.getTo());
        BroadcastService.sendMessageUpdateBroadcast(context, BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString(), message);
        //Todo: use email if contact number is empty
        Log.i(TAG, "Updating delivery status: " + message.getPairedMessageKeyString() + ", " + userPreferences.getContactNumber());
        messageClientService.updateDeliveryStatus(message.getPairedMessageKeyString(), userPreferences.getContactNumber());
        return contact;
    }

    public synchronized void syncMessages() {
        Log.i(TAG, "Starting syncMessages");

        final MobiComUserPreference userpref = MobiComUserPreference.getInstance(context);
        SyncMessageFeed syncMessageFeed = messageClientService.getMessageFeed(userpref.getDeviceKeyString(), userpref.getLastSyncTime());
        Log.i(TAG, "Got sync response " + syncMessageFeed);
        // if regIdInvalid in syncrequest, tht means device reg with c2dm is no
        // more valid, do it again and make the sync request again
        if (syncMessageFeed != null && syncMessageFeed.isRegIdInvalid()
                && GeneralConstants.ANDROID_VERSION >= 8) {
            Log.i(TAG, "Going to call GCM device registration");
            //Todo: Replace it with mobicomkit gcm registration
            // C2DMessaging.register(context);
        }
        if (syncMessageFeed != null && syncMessageFeed.getMessages() != null) {
            List<Message> messageList = syncMessageFeed.getMessages();
            Log.i(TAG, "got messages : " + messageList.size());

            for (final Message message : messageList) {
                Log.i(TAG, "calling  syncMessages : " + message.getTo() + " " + message.getMessage());
                    String[] toList = message.getTo().trim().replace("undefined,", "").split(",");
                    final int groupSmsDelayInSec = MobiComUserPreference.getInstance(context).getGroupSmsDelayInSec();
                    boolean isDelayRequire = groupSmsDelayInSec > 0 && message.isSentViaCarrier() && message.isSentToMany();

                    for (String tofield : toList) {

                            processSms(message, tofield);

                        MobiComUserPreference.getInstance(context).setLastInboxSyncTime(message.getCreatedAtTime());
                    }


                MessageClientService.recentProcessedMessage.add(message);
                BroadcastService.sendMessageUpdateBroadcast(context, BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString(), message);

                messageDatabaseService.createMessage(message);
            }

            userpref.setLastSyncTime(String.valueOf(syncMessageFeed.getLastSyncTime()));
        }
    }


    public synchronized void syncMessagesWithServer() {
        Toast.makeText(context, R.string.sync_messages_from_server, Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                syncMessages();
            }
        }).start();
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
        Intent intent = new Intent(context, messageIntentServiceClass);
        intent.putExtra(MobiComKitConstants.MESSAGE_JSON_INTENT, GsonUtils.getJsonFromObject(message, Message.class));
        context.startService(intent);
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
                timer.schedule(new DisappearingMessageTask(context, new MobiComConversationService(context), message), message.getTimeToLive() * 60 * 1000);
            }
        } else {
            Log.i(TAG, "Sms is not present in table, keyString: " + keyParts[0]);
        }
        map.remove(key);
        mtMessages.remove(key);
    }

    public void createEmptyMessages(List<Contact> contactList) {
        for (Contact contact: contactList) {
            createEmptyMessage(contact);
        }

        BroadcastService.sendLoadMoreBroadcast(context, true);
    }

    public void createEmptyMessage(Contact contact) {
        Message sms = new Message();
        MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);
        sms.setContactIds(contact.getFormattedContactNumber());
        sms.setTo(contact.getContactNumber());
        sms.setCreatedAtTime(0L);
        sms.setStoreOnDevice(Boolean.TRUE);
        sms.setSendToDevice(Boolean.FALSE);
        sms.setType(Message.MessageType.MT_OUTBOX.getValue());
        sms.setDeviceKeyString(userPreferences.getDeviceKeyString());
        sms.setSource(Message.Source.MT_MOBILE_APP.getValue());
        messageDatabaseService.createMessage(sms);
    }
}
