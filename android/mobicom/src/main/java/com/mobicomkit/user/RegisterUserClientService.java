package com.mobicomkit.user;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import com.mobicomkit.MobiComKitClientService;
import com.mobicomkit.MobiComKitServer;
import com.mobicomkit.register.RegistrationResponse;
import com.mobicomkit.HttpRequestUtils;

import java.util.TimeZone;

/**
 * Created by devashish on 2/2/15.
 */
public class RegisterUserClientService extends MobiComKitClientService {

    private static final String TAG = "RegisterUserClient";

    public RegisterUserClientService(Context context) {
        this.context = context;
    }

    public RegistrationResponse createAccount(User user) throws Exception {
        Gson gson = new Gson();
        user.setAppVersionCode(MobiComKitServer.MOBICOMKIT_VERSION_CODE);
        user.setApplicationId(MobiComKitServer.APPLICATION_KEY_HEADER_VALUE);
        String response = HttpRequestUtils.postJsonToServer(MobiComKitServer.CREATE_ACCOUNT_URL, gson.toJson(user));

        Log.i(TAG, "Registration response is: " + response);
        if (response.startsWith("<html>")) {
            return null;
        }
        RegistrationResponse registrationResponse = gson.fromJson(response, RegistrationResponse.class);

        MobiComUserPreference mobiComUserPreference = MobiComUserPreference.getInstance(context);
        //mobiComUserPreference.setCountryCode(user.getCountryCode());
        mobiComUserPreference.setUserId(user.getUserId());
        mobiComUserPreference.setContactNumber(user.getContactNumber());
        mobiComUserPreference.setEmailVerified(user.isEmailVerified());
        mobiComUserPreference.setDeviceKeyString(registrationResponse.getDeviceKeyString());
        mobiComUserPreference.setEmailIdValue(user.getEmailId());
        mobiComUserPreference.setSuUserKeyString(registrationResponse.getSuUserKeyString());
        mobiComUserPreference.setLastSyncTime(String.valueOf(registrationResponse.getLastSyncTime()));
        return registrationResponse;
    }

    public RegistrationResponse createAccount(String email, String userId, String phoneNumber, String pushNotificationId) throws Exception {
        User user = new User();
        user.setEmailId(email);
        user.setUserId(userId);
        user.setDeviceType(Short.valueOf("1"));
        user.setPrefContactAPI(Short.valueOf("2"));
        user.setTimezone(TimeZone.getDefault().getID());
        user.setRegistrationId(pushNotificationId);
        MobiComUserPreference mobiComUserPreference = MobiComUserPreference.getInstance(context);

        user.setCountryCode(mobiComUserPreference.getCountryCode());
        //ContactNumberUtils.getPhoneNumber(phoneNumber, countryCode);
        user.setContactNumber(phoneNumber);

        return createAccount(user);
        /*
        user.setRegistrationId(registrationId);
        String countryCode = usrpref.getCountryCode();
        if (countryCode != null && !countryCode.equals("")) {
            user.setCountryCode(countryCode);
            user.setPrefContactAPI(Short.valueOf("1"));
        } else {
            user.setPrefContactAPI(Short.valueOf("0"));
        }
        if (!TextUtils.isEmpty(password)) {
            user.setPassword(password);
        }
        user.setEmailVerified(usrpref.isEmailVerified());
        user.setTimezone(TimeZone.getDefault().getID());
        user.setRoleName(userType);*/
    }
}
