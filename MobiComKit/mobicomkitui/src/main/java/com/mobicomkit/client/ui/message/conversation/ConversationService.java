package com.mobicomkit.client.ui.message.conversation;

import android.content.Context;

import com.mobicomkit.communication.message.Message;
import com.mobicomkit.communication.message.conversation.MobiComConversationService;

import java.util.ArrayList;
import java.util.List;

public class ConversationService extends MobiComConversationService {

    private static final String TAG = "ConversationService";

    public ConversationService(Context context) {
        super(context, ConversationLoadingIntentService.class);
    }

    public String deleteMessageFromDevice(Message sms, String contactNumber) {
        if (sms == null) {
            return null;
        }
        String contactNumbers = super.deleteMessageFromDevice(sms, contactNumber);
        return contactNumbers;
    }

    public List<Message> syncNativeSms(Long startTime, Long endTime, List<Message> messageList, String contactNumber) {
        //Note: Native SMS not supported
        return new ArrayList<Message>();
    }

    public void deleteConversationFromDevice(String contactNumber) {
        super.deleteConversationFromDevice(contactNumber);
    }

}