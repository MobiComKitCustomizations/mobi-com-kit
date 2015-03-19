package com.mobicomkit.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.mobicomkit.R;
import net.mobitexter.mobiframework.commons.core.utils.ContactNumberUtils;

import java.util.Date;

public class MobiComUserPreference {

    private Context context;
    private String countryCode;
    public static MobiComUserPreference userpref;
    public SharedPreferences sharedPreferences;

    private MobiComUserPreference(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        initialize(context);
    }

    public static MobiComUserPreference getInstance(Context context) {
        if (userpref == null) {
            userpref = new MobiComUserPreference(context);
        }
        return userpref;
    }

    /*
    public void setDeviceRegistrationId(String deviceRegistrationId) {
        sharedPreferences.edit().putString(OsuConstants.DEVICE_REGISTRATION_ID, deviceRegistrationId).commit();
    }

    public String getDeviceRegistrationId() {
        return sharedPreferences.getString(OsuConstants.DEVICE_REGISTRATION_ID, null);
    }*/

    public boolean isRegistered() {
        return !TextUtils.isEmpty(getDeviceKeyString());
    }

    public void setDeviceRegistrationId(String deviceRegistrationId) {
        sharedPreferences.edit().putString(context.getString(R.string.device_registration_id), deviceRegistrationId).commit();
    }

    public String getDeviceRegistrationId() {
        return sharedPreferences.getString(context.getString(R.string.device_registration_id), null);
    }

    public void setDeviceKeyString(String deviceKeyString) {
        sharedPreferences.edit().putString(context.getString(R.string.device_key_string), deviceKeyString).commit();
    }

    public String getDeviceKeyString() {
        return sharedPreferences.getString(context.getString(R.string.device_key_string), null);
    }

    public long getLastOutboxSyncTime() {
        return sharedPreferences.getLong(context.getString(R.string.last_outbox_sync_time), 0L);
    }

    public void setLastOutboxSyncTime(long lastOutboxSyncTime) {
        sharedPreferences.edit().putLong(context.getString(R.string.last_outbox_sync_time), lastOutboxSyncTime).commit();
    }

    public void setLastSyncTime(String lastSyncTime) {
        sharedPreferences.edit().putString(context.getString(R.string.last_sms_sync_time), lastSyncTime).commit();
    }

    public boolean isReportEnable() {
        return sharedPreferences.getBoolean(context.getString(R.string.delivery_report_pref_key), false);
    }

    public void setReportEnable(boolean  reportEnable) {
        sharedPreferences.edit().putBoolean(context.getString(R.string.delivery_report_pref_key),reportEnable).commit();
    }

    public String getLastSyncTime() {
        return sharedPreferences.getString(context.getString(R.string.last_sms_sync_time), "0");
    }

    public long getLastInboxSyncTime() {
        return sharedPreferences.getLong(context.getString(R.string.last_inbox_sync_time), 0L);
    }

    public void setLastInboxSyncTime(long lastInboxSyncTime) {
        sharedPreferences.edit().putLong(context.getString(R.string.last_inbox_sync_time), lastInboxSyncTime).commit();
    }

    public void setLastMessageStatSyncTime(long lastMessageStatSyncTime) {
        sharedPreferences.edit().putLong(context.getString(R.string.last_message_stat_sync_time), lastMessageStatSyncTime).commit();
    }

    public Long getLastMessageStatSyncTime() {
        return sharedPreferences.getLong(context.getString(R.string.last_message_stat_sync_time), 0);
    }

    public boolean isSentSmsSyncFlag() {
        return sharedPreferences.getBoolean(context.getString(R.string.sent_sms_sync_pref_key), true);
    }

    public void setSentSmsSyncFlag(boolean sentSmsSyncFlag) {
        sharedPreferences.edit().putBoolean(context.getString(R.string.sent_sms_sync_pref_key), sentSmsSyncFlag).commit();
    }

    public void setEmailIdValue(String emailIdValue) {
        sharedPreferences.edit().putString(context.getString(R.string.email), emailIdValue).commit();
    }

    public String getEmailIdValue() {
        return sharedPreferences.getString(context.getString(R.string.email), null);
    }

    public void setEmailVerified(boolean emailVerified) {
        sharedPreferences.edit().putBoolean(context.getString(R.string.email_verified), emailVerified).commit();
    }

    public boolean isEmailVerified() {
        return sharedPreferences.getBoolean(context.getString(R.string.email_verified), true);
    }

    public void setSuUserKeyString(String suUserKeyString) {
        sharedPreferences.edit().putString(context.getString(R.string.user_key_string), suUserKeyString).commit();
    }

    public String getSuUserKeyString() {
        return sharedPreferences.getString(context.getString(R.string.user_key_string), null);
    }

    public void setStopServiceFlag(Boolean stopServiceFlag) {
        sharedPreferences.edit().putBoolean(context.getString(R.string.stop_service), stopServiceFlag).commit();
    }

    public boolean isStopServiceFlag() {
        return sharedPreferences.getBoolean(context.getString(R.string.stop_service), false);
    }

    public void setPatchAvailable(Boolean patchAvailable) {
        sharedPreferences.edit().putBoolean(context.getString(R.string.patch_available), patchAvailable).commit();
    }

    public boolean isPatchAvailable() {
        return sharedPreferences.getBoolean(context.getString(R.string.patch_available), false);
    }

    public boolean isWebHookEnable(){
        return sharedPreferences.getBoolean(context.getString(R.string.webhook_enable_key), false);
    }

    public void setWebHookEnable(boolean enable){
        sharedPreferences.edit().putBoolean(context.getString(R.string.webhook_enable_key), enable).commit();
    }

    public int getGroupSmsDelayInSec(){
        return sharedPreferences.getInt(context.getString(R.string.group_sms_freq_key), 0);
    }

    public void setDelayGroupSmsDelayTime(int delay) {
        sharedPreferences.edit().
                putInt(context.getString(R.string.group_sms_freq_key),delay).commit();
    }


//    public boolean getNewPatchAvailable() {
//        return newPatchAvailable;
//    }
//
//    public boolean getUpdateRegFlag() {
//        return updateRegFlag;
//    }

    public void setUpdateRegFlag(boolean updateRegFlag) {
        sharedPreferences.edit().putBoolean(context.getString(R.string.update_push_registration), updateRegFlag).commit();
    }

    public boolean isUpdateRegFlag() {
        return sharedPreferences.getBoolean(context.getString(R.string.update_push_registration), false);
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public boolean isVerifyContactNumber() {
        return sharedPreferences.getBoolean(context.getString(R.string.verify_contact_number), false);
    }

    public void setVerifyContactNumber(boolean verifyContactNumber) {
        sharedPreferences.edit().putBoolean(context.getString(R.string.verify_contact_number), verifyContactNumber).commit();
    }

    public boolean getReceivedSmsSyncFlag() {
        return sharedPreferences.getBoolean(context.getString(R.string.received_sms_sync_pref_key), true);
    }

    public void setReceivedSmsSyncFlag(boolean receivedSmsSyncFlag) {
        sharedPreferences.edit().putBoolean(context.getString(R.string.received_sms_sync_pref_key), receivedSmsSyncFlag).commit();
    }

    public String getContactNumber() {
        return sharedPreferences.getString(context.getString(R.string.phone_number_key), null);
    }

    public void setContactNumber(String contactNumber) {
        contactNumber = ContactNumberUtils.getPhoneNumber(contactNumber, getCountryCode());
        sharedPreferences.edit().putString(context.getString(R.string.phone_number_key), contactNumber).commit();
    }

    public boolean isDisplayCallRecordEnable() {
        return sharedPreferences.getBoolean(context.getString(R.string.call_history_display_within_messages_pref_key), false);
    }

    public void setDisplayCallRecordEnable(boolean enable) {
        sharedPreferences.edit().putBoolean(context.getString(R.string.call_history_display_within_messages_pref_key), enable).commit();
    }

    //Local initialization of few fields
    public void initialize(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String countryCode = telephonyManager.getSimCountryIso().toUpperCase();
        String contactNumber = telephonyManager.getLine1Number();
        setCountryCode(countryCode);
        if (!TextUtils.isEmpty(contactNumber)) {
            setContactNumber(contactNumber);
        }
        if (getLastMessageStatSyncTime() == null || getLastMessageStatSyncTime() == 0) {
            setLastMessageStatSyncTime(new Date().getTime());
        }
    }

    public void setMobiTexterContactSyncCompleted(boolean status) {
        sharedPreferences.edit().
        putBoolean(context.getString(R.string.mobitexter_contact_sync_key), status).commit();
    }

    public boolean isMobiTexterContactSyncCompleted() {
        return sharedPreferences.getBoolean(context.getString(R.string.mobitexter_contact_sync_key), false);
    }

    @Override
    public String toString() {
        return "";
    }

}
