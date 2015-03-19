package com.mobicomkit.client.ui.c2dm;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.mobicomkit.client.ui.GeneralConstants;
import com.mobicomkit.client.ui.message.MessageService;
import com.mobicomkit.client.ui.message.conversation.ConversationService;
import com.mobicomkit.broadcast.BroadcastService;
import com.mobicomkit.communication.message.MessageDeleteContent;
import com.mobicomkit.communication.message.database.MessageDatabaseService;
import com.mobicomkit.user.MobiComUserPreference;
import com.mobicomkit.user.UserClientService;
import net.mobitexter.mobiframework.call.CallService;


public class C2DMReceiver extends C2DMBaseReceiver {
    private static final String TAG = "C2DMReceiver";
    private static String SENDER_ID = null;

    public C2DMReceiver() {
        // send the email address you set up earlier
        super(SENDER_ID);
    }

    @Override
    public void onRegistered(Context context, String registrationId)
            throws java.io.IOException {
        Log.i(TAG, "Registration ID arrived: Fantastic!!!: " + registrationId);
        //DeviceRegistrar.sendPushNotificationIdToServer(context);
    }

    @Override
    public void onUnregistered(Context context) {
        Log.i(TAG, "successfully unregistered with C2DM server");
    }

    @Override
    public void onError(Context context, String errorId) {
        // notify the user
        Log.e(TAG, "error with C2DM receiver: " + errorId);

        if ("ACCOUNT_MISSING".equals(errorId)) {
            // no Google account on the phone; ask the user to open the account
            // manager and add a google account and then try again
            // TODO

        } else if ("AUTHENTICATION_FAILED".equals(errorId)) {
            // bad password (ask the user to enter password and try. Q: what
            // password - their google password or the sender_id password? ...)
            // i _think_ this goes hand in hand with google account; have them
            // re-try their google account on the phone to ensure it's working
            // and then try again
            // TODO

        } else if ("TOO_MANY_REGISTRATIONS".equals(errorId)) {
            // user has too many apps registered; ask user to uninstall other
            // apps and try again
            // TODO

        } else if ("INVALID_SENDER".equals(errorId)) {
            // this shouldn't happen in a properly configured system
            // TODO: send a message to app publisher?, inform user that service
            // is down

        } else if ("PHONE_REGISTRATION_ERROR".equals(errorId)) {
            // the phone doesn't support C2DM; inform the user
            // TODO

        } // else: SERVICE_NOT_AVAILABLE is handled by the super class and does
        // exponential backoff retries
    }

    public static void sendConversationDeleteBroadcast(Context context, String action, String contactNumber) {
        Log.i(TAG, "Sending conversation delete broadcast for " + action);
        Intent intentDelete = new Intent();
        intentDelete.setAction(action);
        intentDelete.putExtra("contactNumber", contactNumber);
        intentDelete.addCategory(Intent.CATEGORY_DEFAULT);
        context.sendBroadcast(intentDelete);
    }

    @Override
    public void onMessage(Context context, Intent intent) {
        Log.i(TAG, "Received a new message");
        try {
            processMessage(context, intent);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "Exception while processing C2DM message");
        }
    }

    public static void processMessage(Context context, Intent intent) {
        MobiComUserPreference usrpref = MobiComUserPreference.getInstance(context);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            // ToDo: do something for invalidkey ;
            // && extras.get("InvalidKey") != null

            /*
                * AppUtil.myLogger(TAG, "Payload is : "+extras.get("payload")); //parse the
                * message and do something with it. //For example, if the server
                * sent the payload as "data.message=xxx", here you would have an
                * extra called "message" String message =
                * extras.getString("message"); AppUtil.myLogger(TAG, "received message: " +
                * message);
                * // Now do something smart based on the information
                * //MainActivity.setMessage(message);
                */
            String message = intent.getStringExtra("collapse_key");
            String payloadForMtextReceived = intent.getStringExtra(GeneralConstants.MTEXT_RECEIVED);
            String deleteConversationForContact = intent.getStringExtra("DELETE_SMS_CONTACT");
            String deleteSms = intent.getStringExtra(GeneralConstants.DELETE_SMS);
            String multipleMessageDelete = intent.getStringExtra(GeneralConstants.DELETE_MULTIPLE_MESSAGE);
            String cancelCall = intent.getStringExtra(GeneralConstants.CANCEL_CALL);
            String displayCallSetting = intent.getStringExtra(GeneralConstants.DISPLAY_CALLS);

            String keyDial = intent.getStringExtra(GeneralConstants.PUSH_NOTIFICATION_KEY_DIAL);

            if (intent.getStringExtra(GeneralConstants.EMAIL_VERIFIED) != null && !usrpref.isEmailVerified()) {
                boolean emailVerified = Boolean.valueOf(intent.getStringExtra(GeneralConstants.EMAIL_VERIFIED));
                Log.i(TAG, "Received GCM message emailVerified: " + emailVerified);
                usrpref.setEmailVerified(emailVerified);
                if (!emailVerified) {
                    usrpref.setEmailIdValue(null);
                    //Todo: clear data and logout user
                }
            }

            String mtexterUser = intent.getStringExtra(GeneralConstants.MTEXTER_USER);

            if (!TextUtils.isEmpty(deleteSms)) {
                String contactNumbers = deleteSms.split(",").length > 1 ? deleteSms.split(",")[1] : null;
                processDeleteSingleMessageRequest(context, deleteSms.split(",")[0], contactNumbers);
            }

            if(!TextUtils.isEmpty(cancelCall)){
                CallService.cancelCall(context);
            }

            if (!TextUtils.isEmpty(multipleMessageDelete)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                MessageDeleteContent messageDeleteContent = gson.fromJson(multipleMessageDelete, MessageDeleteContent.class);

                for (String deletedSmsKeyString: messageDeleteContent.getDeleteKeyStrings()) {
                    processDeleteSingleMessageRequest(context, deletedSmsKeyString, messageDeleteContent.getContactNumber());
                }
            }

            String payloadForDelivered = intent.getStringExtra("DELIVERED");

            MessageService messageService = new MessageService(context);
            if (!TextUtils.isEmpty(payloadForDelivered)) {
                messageService.updateDeliveryStatus(payloadForDelivered);
            }
            if (!TextUtils.isEmpty(payloadForMtextReceived)) {
                messageService.putMtextToDatabase(payloadForMtextReceived);
            }
            if (!TextUtils.isEmpty(deleteConversationForContact)) {
                ConversationService conversationService = new ConversationService(context);
                conversationService.deleteConversationFromDevice(deleteConversationForContact);
                sendConversationDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_CONVERSATION.toString(), deleteConversationForContact);
            }

            if (!TextUtils.isEmpty(keyDial)) {
                Log.i(TAG, "Received push notification for dial number: " + keyDial);
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                callIntent.setData(Uri.parse("tel:" + keyDial));
                context.startActivity(callIntent);
            }

            if (GeneralConstants.SYNC.equalsIgnoreCase(message)) {
                messageService.syncSms();
            } else if (GeneralConstants.UPDATE_AVAILABLE.equalsIgnoreCase(message)) {
                new UserClientService(context).updateCodeVersion(GeneralConstants.APP_CODE_VERSION, usrpref.getDeviceKeyString());
            } else {
                try {
                    if (GeneralConstants.STOP_SERVICE_FLAG.equalsIgnoreCase(message) || GeneralConstants.START_SERVICE_FLAG.equalsIgnoreCase(message)) {
                        boolean stopState = GeneralConstants.STOP_SERVICE_FLAG.equalsIgnoreCase(message);
                        usrpref.setStopServiceFlag(stopState);
                    }
                    if (GeneralConstants.NEW_PATCH_AVAILABLE.equalsIgnoreCase(message)) {
                        usrpref.setPatchAvailable(true);
                    }
                } finally {

                }
            }
        }
    }

    private static void processDeleteSingleMessageRequest(Context context, String deletedSmsKeyString, String contactNumber) {
        ConversationService conversationService = new ConversationService(context);
        contactNumber = conversationService.deleteMessageFromDevice(new MessageDatabaseService(context).getSms(deletedSmsKeyString), contactNumber);
        BroadcastService.sendMessageDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_MESSAGE.toString(), deletedSmsKeyString, contactNumber);
    }

}