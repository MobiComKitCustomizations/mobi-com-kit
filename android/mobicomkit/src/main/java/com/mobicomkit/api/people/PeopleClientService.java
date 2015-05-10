package com.mobicomkit.api.people;

import android.content.Context;

import com.mobicomkit.api.HttpRequestUtils;
import com.mobicomkit.api.MobiComKitClientService;
import com.mobicomkit.api.MobiComKitServer;

import net.mobitexter.mobiframework.json.GsonUtils;

/**
 * Created by devashish on 27/12/14.
 */
public class PeopleClientService extends MobiComKitClientService {

    public PeopleClientService(Context context) {
        super(context);
    }

    public String getGoogleContacts(int page) {
        return HttpRequestUtils.getResponse(credentials, MobiComKitServer.GOOGLE_CONTACT_URL + "?page=" + page, "application/json", "application/json");
    }

    public String getContactsInCurrentPlatform() {
        return HttpRequestUtils.getResponse(credentials, MobiComKitServer.PLATFORM_CONTACT_URL + "?mtexter=true", "application/json", "application/json");
    }

    public void addContacts(String url, ContactList contactList, boolean completed) throws Exception {
        String requestString = GsonUtils
                .getJsonWithExposeFromObject(contactList, ContactList.class);
        if (completed) {
            url = url + "?completed=true";
        }
        HttpRequestUtils.postData(credentials, url, "application/json", null, requestString);
    }
}
