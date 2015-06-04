package com.mobicomkit.uiwidgets.conversation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.azuga.framework.ui.UIService;
import com.mobicomkit.api.MobiComKitConstants;
import com.mobicomkit.api.account.user.MobiComUserPreference;
import com.mobicomkit.api.conversation.Message;
import com.mobicomkit.broadcast.BroadcastService;
import com.mobicomkit.uiwidgets.R;
import com.mobicomkit.uiwidgets.conversation.fragment.ConversationFragment;
import com.mobicomkit.uiwidgets.conversation.fragment.MobiComQuickConversationFragment;
import com.mobicomkit.uiwidgets.conversation.fragment.MultimediaOptionFragment;

import net.mobitexter.mobiframework.commons.core.utils.ContactNumberUtils;
import net.mobitexter.mobiframework.commons.core.utils.Support;
import net.mobitexter.mobiframework.commons.image.ImageUtils;
import net.mobitexter.mobiframework.file.FilePathFinder;
import net.mobitexter.mobiframework.json.GsonUtils;
import net.mobitexter.mobiframework.people.activity.MobiComKitPeopleActivity;
import net.mobitexter.mobiframework.people.contact.Contact;
import net.mobitexter.mobiframework.people.contact.ContactUtils;
import net.mobitexter.mobiframework.people.group.Group;
import net.mobitexter.mobiframework.people.group.GroupUtils;


public class ConversationUIService {

    private static final String TAG = "ConversationUIService";

    public static final int REQUEST_CODE_FULL_SCREEN_ACTION = 301;
    public static final int REQUEST_CODE_CONTACT_GROUP_SELECTION = 101;
    public static final int INSTRUCTION_DELAY = 5000;

    private Context context;

    public ConversationUIService(Context context) {
        this.context = context;
    }

    public MobiComQuickConversationFragment getQuickConversationFragment() {
        return (MobiComQuickConversationFragment) UIService.getInstance().getActiveFragment();
    }

    public ConversationFragment getConversationFragment() {
        return (ConversationFragment) UIService.getInstance().getActiveFragment();
    }

    public static void onQuickConversationFragmentItemClick(View view, Contact contact) {
        TextView textView = (TextView) view.findViewById(R.id.unreadSmsCount);
        textView.setVisibility(View.GONE);
        openConversationFragment(contact);
    }

    public static void openConversationFragment(Contact contact) {
        ConversationFragment conversationFragment = new ConversationFragment();
        UIService.getInstance().addFragment(conversationFragment);
        conversationFragment.loadConversation(contact);
    }

    public static void openConversationFragment(Group group) {
        ConversationFragment conversationFragment = new ConversationFragment();
        UIService.getInstance().addFragment(conversationFragment);
        conversationFragment.loadConversation(group);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //if (BroadcastService.isIndividual() && (requestCode == MultimediaOptionFragment.REQUEST_CODE_ATTACH_PHOTO ||
        if ((requestCode == MultimediaOptionFragment.REQUEST_CODE_ATTACH_PHOTO ||
                requestCode == MultimediaOptionFragment.REQUEST_CODE_TAKE_PHOTO)
                && resultCode == Activity.RESULT_OK) {
            Uri selectedFileUri = (intent == null ? null : intent.getData());
            if (selectedFileUri == null) {
                selectedFileUri = getConversationFragment().getMultimediaOptionFragment().getCapturedImageUri();
                ImageUtils.addImageToGallery(FilePathFinder.getPath(context, selectedFileUri), context);
            }

            if (selectedFileUri == null) {
                Bitmap photo = (Bitmap) intent.getExtras().get("data");
                selectedFileUri = ImageUtils.getImageUri(context, photo);
            }
            getConversationFragment().loadFile(selectedFileUri);

            Log.i(TAG, "File uri: " + selectedFileUri);
        }

        if (requestCode == REQUEST_CODE_CONTACT_GROUP_SELECTION && resultCode == Activity.RESULT_OK) {
            checkForStartNewConversation(intent);
        }
    }

    public void updateLatestMessage(Message message, String formattedContactNumber) {
        if (!BroadcastService.isQuick()) {
            return;
        }
        getQuickConversationFragment().updateLatestMessage(message, formattedContactNumber);
    }

    public void removeConversation(Message message, String formattedContactNumber) {
        if (!BroadcastService.isQuick()) {
            return;
        }
        getQuickConversationFragment().removeConversation(message, formattedContactNumber);
    }

    public void addMessage(Message message) {
        if (!BroadcastService.isQuick()) {
            return;
        }

        MobiComQuickConversationFragment fragment = (MobiComQuickConversationFragment) UIService.getInstance().getActiveFragment();
        fragment.addMessage(message);
    }

    public void updateLastMessage(String keyString, String formattedContactNumber) {
        if (!BroadcastService.isQuick()) {
            return;
        }
        getQuickConversationFragment().updateLastMessage(keyString, formattedContactNumber);
    }

    public boolean isBroadcastedToGroup(Long broadcastGroupId) {
        if (!BroadcastService.isIndividual()) {
            return false;
        }
        return getConversationFragment().isBroadcastedToGroup(broadcastGroupId);
    }

    public void syncMessages(Message message, String keyString) {
        String formattedContactNumber = ContactNumberUtils.getPhoneNumber(message.getTo(), MobiComUserPreference.getInstance(context).getCountryCode());

        if (BroadcastService.isIndividual()) {
            ConversationFragment conversationFragment = getConversationFragment();
            if (formattedContactNumber.equals(conversationFragment.getFormattedContactNumber()) ||
                    conversationFragment.isBroadcastedToGroup(message.getBroadcastGroupId())) {
                conversationFragment.addMessage(message);
            }
        }

        if (message.getBroadcastGroupId() == null) {
            updateLastMessage(keyString, formattedContactNumber);
        }
    }

    public void downloadConversations(boolean showInstruction) {
        if (!BroadcastService.isQuick()) {
            return;
        }
        getQuickConversationFragment().downloadConversations(showInstruction);
    }

    public void setLoadMore(boolean loadMore) {
        if (!BroadcastService.isQuick()) {
            return;
        }
        getQuickConversationFragment().setLoadMore(loadMore);
    }

    public void updateMessageKeyString(Message message) {
        if (!BroadcastService.isIndividual()) {
            return;
        }
        String formattedContactNumber = ContactNumberUtils.getPhoneNumber(message.getTo(), MobiComUserPreference.getInstance(context).getCountryCode());
        ConversationFragment conversationFragment = getConversationFragment();
        if (formattedContactNumber.equals(conversationFragment.getFormattedContactNumber()) ||
                conversationFragment.isBroadcastedToGroup(message.getBroadcastGroupId())) {
            conversationFragment.updateMessageKeyString(message);
        }
    }

    public void deleteMessage(Message message, String keyString, String formattedContactNumber) {
        //Todo: replace currentOpenedContactNumber with MobiComKitBroadcastReceiver.currentUserId
        if (PhoneNumberUtils.compare(formattedContactNumber, BroadcastService.currentUserId)) {
            getConversationFragment().deleteMessageFromDeviceList(keyString);
        } else {
            updateLastMessage(keyString, formattedContactNumber);
        }
    }

    public void updateDeliveryStatus(Message message, String formattedContactNumber) {
        if (!BroadcastService.isIndividual()) {
            return;
        }
        ConversationFragment conversationFragment = getConversationFragment();
        if (formattedContactNumber.equals(conversationFragment.getContactIds())) {
            conversationFragment.updateDeliveryStatus(message);
        }
    }

    public void deleteConversation(Contact contact) {
        if (BroadcastService.isIndividual()) {
            getConversationFragment().clearList();
        }
        if (BroadcastService.isQuick()) {
            getQuickConversationFragment().removeConversation(contact);
        }
    }

    public void updateUploadFailedStatus(Message message) {
        if (!BroadcastService.isIndividual()) {
            return;
        }
        getConversationFragment().updateUploadFailedStatus(message);
    }

    public void updateDownloadStatus(Message message) {
        if (!BroadcastService.isIndividual()) {
            return;
        }
        getConversationFragment().updateDownloadStatus(message);
    }

    public void startContactActivityForResult(Intent intent, Message message, String messageContent) {
        if (message != null) {
            intent.putExtra(MobiComKitPeopleActivity.FORWARD_MESSAGE, GsonUtils.getJsonFromObject(message, message.getClass()));
        }
        if (messageContent != null) {
            intent.putExtra(MobiComKitPeopleActivity.SHARED_TEXT, messageContent);
        }

        ((Activity) context).startActivityForResult(intent, REQUEST_CODE_CONTACT_GROUP_SELECTION);
    }

    public void startContactActivityForResult() {
        startContactActivityForResult(null, null);
    }

    public void startContactActivityForResult(Message message, String messageContent) {
        //Todo: Change this to driver list fragment or activity
        Intent intent = new Intent(context, MobiComKitPeopleActivity.class);
        startContactActivityForResult(intent, message, messageContent);
    }

    public void checkForStartNewConversation(Intent intent) {
        Contact contact = null;
        Group group = null;

        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            if ("text/plain".equals(intent.getType())) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    startContactActivityForResult(null, sharedText);
                }
            } else if (intent.getType().startsWith("image/")) {
                //Todo: use this for image forwarding
            }
        }

        final Uri uri = intent.getData();
        if (uri != null) {
            Long contactId = intent.getLongExtra("contactId", 0);
            if (contactId == 0) {
                //Todo: show warning that the user doesn't have any number stored.
                return;
            }
            contact = ContactUtils.getContact(context, contactId);
        }

        Long groupId = intent.getLongExtra("groupId", -1);
        String groupName = intent.getStringExtra("groupName");
        if (groupId != -1) {
            group = GroupUtils.fetchGroup(context, groupId, groupName);
        }

        String contactNumber = intent.getStringExtra("contactNumber");
        boolean firstTimeMTexterFriend = intent.getBooleanExtra("firstTimeMTexterFriend", false);
        if (!TextUtils.isEmpty(contactNumber)) {
            contact = ContactUtils.getContact(context, contactNumber);
            if (BroadcastService.isIndividual()) {
                getConversationFragment().setFirstTimeMTexterFriend(firstTimeMTexterFriend);
            }
        }

        String userId = intent.getStringExtra("userId");
        if (!TextUtils.isEmpty(userId)) {
            contact = new Contact();
            contact.setUserId(userId);
            //Todo: Load contact details from server.
        }

        String messageJson = intent.getStringExtra(MobiComKitConstants.MESSAGE_JSON_INTENT);
        if (!TextUtils.isEmpty(messageJson)) {
            Message message = (Message) GsonUtils.getObjectFromJson(messageJson, Message.class);
            contact = ContactUtils.getContact(context, message.getTo());
        }

        boolean support = intent.getBooleanExtra(Support.SUPPORT_INTENT_KEY, false);
        if (support) {
            contact = Support.getSupportContact();
        }

        if (contact != null) {
            ConversationUIService.openConversationFragment(contact);
        }

        if (group != null) {
            ConversationUIService.openConversationFragment(group);
        }

        String forwardMessage = intent.getStringExtra(MobiComKitPeopleActivity.FORWARD_MESSAGE);
        if (!TextUtils.isEmpty(forwardMessage)) {
            Message messageToForward = (Message) GsonUtils.getObjectFromJson(forwardMessage, Message.class);
            getConversationFragment().forwardMessage(messageToForward);
        }

        String sharedText = intent.getStringExtra(MobiComKitPeopleActivity.SHARED_TEXT);
        if (!TextUtils.isEmpty(sharedText)) {
            getConversationFragment().sendMessage(sharedText);
        }
    }

    public void processLocation() {
    }
}