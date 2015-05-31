package com.mobicomkit.uiwidgets.conversation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.mobicomkit.api.MobiComKitConstants;
import com.mobicomkit.api.account.user.MobiComUserPreference;
import com.mobicomkit.api.conversation.Message;
import com.mobicomkit.broadcast.BroadcastService;
import com.mobicomkit.uiwidgets.R;
import com.mobicomkit.uiwidgets.conversation.activity.MobiComActivity;
import com.mobicomkit.uiwidgets.conversation.fragment.MobiComConversationFragment;
import com.mobicomkit.uiwidgets.conversation.fragment.MobiComQuickConversationFragment;
import com.mobicomkit.uiwidgets.instruction.InstructionUtil;

import net.mobitexter.mobiframework.commons.core.utils.ContactNumberUtils;
import net.mobitexter.mobiframework.json.GsonUtils;
import net.mobitexter.mobiframework.people.contact.Contact;
import net.mobitexter.mobiframework.people.contact.ContactUtils;

/**
 * Created by devashish on 4/2/15.
 */
public class MobiComKitBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "MTBroadcastReceiver";

    private MobiComActivity activity;

    public MobiComKitBroadcastReceiver(MobiComActivity activity) {
        this.activity = activity;
    }

    public MobiComKitBroadcastReceiver(MobiComQuickConversationFragment quickConversationFragment, MobiComConversationFragment conversationFragment) {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Message message = null;
        String messageJson = intent.getStringExtra(MobiComKitConstants.MESSAGE_JSON_INTENT);
        if (!TextUtils.isEmpty(messageJson)) {
            message = (Message) GsonUtils.getObjectFromJson(messageJson, Message.class);
        }
        Log.i(TAG, "Received broadcast, action: " + action + ", sms: " + message);

        MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);
        String formattedContactNumber = ContactNumberUtils.getPhoneNumber(message.getTo(), userPreferences.getCountryCode());

        if (message != null && !message.isSentToMany()) {
            /*Todo: update the quick conversation fragment on resume, commented because now it is not a sliding pane activity and
            quickconversationfragment is not activity.*/
            activity.addMessage(message);
        } else if (message != null && message.isSentToMany() && BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString().equals(intent.getAction())) {
            for (String toField : message.getTo().split(",")) {
                Message singleMessage = new Message(message);
                singleMessage.setBroadcastGroupId(null);
                singleMessage.setKeyString(message.getKeyString());
                singleMessage.setTo(toField);
                singleMessage.processContactIds(context);
                activity.addMessage(message);
            }
        }

        String keyString = intent.getStringExtra("keyString");

        if (BroadcastService.INTENT_ACTIONS.INSTRUCTION.toString().equals(action)) {
            InstructionUtil.showInstruction(context, intent.getIntExtra("resId", -1), intent.getBooleanExtra("actionable", false), R.color.instruction_color);
        } else if (BroadcastService.INTENT_ACTIONS.FIRST_TIME_SYNC_COMPLETE.toString().equals(action)) {
            activity.downloadConversations(true);
        } else if (BroadcastService.INTENT_ACTIONS.LOAD_MORE.toString().equals(action)) {
            activity.setLoadMore(intent.getBooleanExtra("loadMore", true));
        } else if (BroadcastService.INTENT_ACTIONS.MESSAGE_SYNC_ACK_FROM_SERVER.toString().equals(action)) {
            activity.updateMessageKeyString(message);
        } else if (BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString().equals(intent.getAction())) {
            activity.syncMessages(message, keyString);
        } else if (BroadcastService.INTENT_ACTIONS.DELETE_MESSAGE.toString().equals(intent.getAction())) {
            formattedContactNumber = intent.getStringExtra("contactNumbers");
           activity.deleteMessage(message, keyString, formattedContactNumber);
        } else if (BroadcastService.INTENT_ACTIONS.MESSAGE_DELIVERY.toString().equals(action)) {
            activity.updateDeliveryStatus(message, formattedContactNumber);
        } else if (BroadcastService.INTENT_ACTIONS.DELETE_CONVERSATION.toString().equals(action)) {
            String contactNumber = intent.getStringExtra("contactNumber");
            Contact contact = ContactUtils.getContact(context, contactNumber);
            activity.deleteConversation(contact);
        } else if (BroadcastService.INTENT_ACTIONS.UPLOAD_ATTACHMENT_FAILED.toString().equals(action) && message != null) {
            activity.updateUploadFailedStatus(message);
        } else if (BroadcastService.INTENT_ACTIONS.MESSAGE_ATTACHMENT_DOWNLOAD_DONE.toString().equals(action) && message != null) {
            activity.updateDownloadStatus(message);
        }
    }
}
