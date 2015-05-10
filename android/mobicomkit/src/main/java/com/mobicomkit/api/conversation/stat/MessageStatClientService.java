package com.mobicomkit.api.conversation.stat;

import android.content.Context;

import com.mobicomkit.api.MobiComKitClientService;
import com.mobicomkit.api.MobiComKitServer;
import com.mobicomkit.api.HttpRequestUtils;
import net.mobitexter.mobiframework.json.GsonUtils;

/**
 * Created by devashish on 26/12/14.
 */
public class MessageStatClientService extends MobiComKitClientService {

    private static final String TAG = "MessageStatClientService";

    public MessageStatClientService(Context context) {
        super(context);
    }

    public String sendMessageStat(MessageStat messageStat) {
        return HttpRequestUtils.postData(credentials, MobiComKitServer.MESSAGE_STAT_URL, "application/json", null, GsonUtils.getJsonFromObject(messageStat, MessageStat.class));
    }

}
