package com.mobicomkit.sync;

import com.mobicomkit.api.conversation.Message;

import java.util.List;

public class SmsSyncRequest {

    private List<Message> smsList;

    public List<Message> getSmsList() {
        return smsList;
    }

    public void setSmsList(List<Message> smsList) {
        this.smsList = smsList;
    }


}


