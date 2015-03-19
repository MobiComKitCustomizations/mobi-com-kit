package com.mobicomkit.communication.message.stat;

import android.content.Context;

import com.mobicomkit.MobiComKitClientService;
import com.mobicomkit.MobiComKitServer;
import com.mobicomkit.HttpRequestUtils;
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
