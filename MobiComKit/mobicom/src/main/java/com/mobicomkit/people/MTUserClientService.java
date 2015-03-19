package com.mobicomkit.people;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;

import com.mobicomkit.MobiComKitClientService;
import com.mobicomkit.MobiComKitServer;
import com.mobicomkit.HttpRequestUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


/**
 * Created by devashish on 23/12/14.
 */
public class MTUserClientService extends MobiComKitClientService {

    public MTUserClientService(Context context) {
        super(context);
    }

    public ContactContent getContactContent( String contactNumber) {
        String response = null;
        try {
            response = HttpRequestUtils.getResponse(credentials, MobiComKitServer.CHECK_FOR_MT_USER + "?requestSource=1&contactNumber=" + URLEncoder.encode(contactNumber, "UTF-8"), "text/plain", "application/json");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(response)) {
            Gson gson = new Gson();
            return gson.fromJson(response, ContactContent.class);
        }
        return null;
    }
}
