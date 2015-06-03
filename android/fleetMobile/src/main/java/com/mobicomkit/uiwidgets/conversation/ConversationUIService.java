package com.mobicomkit.uiwidgets.conversation;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.widget.TextView;

import com.azuga.framework.ui.UIService;
import com.mobicomkit.api.account.user.MobiComUserPreference;
import com.mobicomkit.api.conversation.Message;
import com.mobicomkit.broadcast.BroadcastService;
import com.mobicomkit.uiwidgets.R;
import com.mobicomkit.uiwidgets.conversation.fragment.ConversationFragment;
import com.mobicomkit.uiwidgets.conversation.fragment.MobiComQuickConversationFragment;

import net.mobitexter.mobiframework.commons.core.utils.ContactNumberUtils;
import net.mobitexter.mobiframework.people.contact.Contact;
import net.mobitexter.mobiframework.people.group.Group;


public class ConversationUIService {

    private static final String TAG = "ConversationUIService";

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

    public void onQuickConversationFragmentItemClick(View view, Contact contact) {
        TextView textView = (TextView) view.findViewById(R.id.unreadSmsCount);
        textView.setVisibility(View.GONE);
        openConversationFragment(contact);
    }

    public void openConversationFragment(Contact contact) {
        ConversationFragment conversationFragment = new ConversationFragment();
        UIService.getInstance().addFragment(conversationFragment);
        conversationFragment.loadConversation(contact);
    }

    public void openConversationFragment(Group group) {
        ConversationFragment conversationFragment = new ConversationFragment();
        UIService.getInstance().addFragment(conversationFragment);
        conversationFragment.loadConversation(group);
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
        ConversationFragment conversationFragment = new ConversationFragment();
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

}