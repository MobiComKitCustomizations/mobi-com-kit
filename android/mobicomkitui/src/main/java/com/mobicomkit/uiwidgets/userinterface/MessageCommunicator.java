package com.mobicomkit.uiwidgets.userinterface;

import com.mobicomkit.communication.message.Message;

/**
 * Created by devashish on 10/2/15.
 */
public interface MessageCommunicator {

    void updateLatestMessage(Message message, String formattedContactNumber);

    void removeConversation(Message message, String formattedContactNumber);
}