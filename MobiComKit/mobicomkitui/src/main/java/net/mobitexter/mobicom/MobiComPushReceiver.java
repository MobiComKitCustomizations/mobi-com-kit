package net.mobitexter.mobicom;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.mobicomkit.client.ui.message.MessageIntentService;
import com.mobicomkit.communication.message.MobiComMessageService;
import com.mobicomkit.people.ContactContent;


import java.util.ArrayList;
import java.util.List;


public class MobiComPushReceiver  {

    public static final List<String> notificationKeyList = new ArrayList<String>();
    static {

        notificationKeyList.add("SYNC");
        notificationKeyList.add("MARK_ALL_SMS_AS_READ");
        notificationKeyList.add("DELIVERED");
        notificationKeyList.add("SYNC_PENDING");
        notificationKeyList.add("DELETE_SMS");
        notificationKeyList.add("DELETE_MULTIPLE_MESSAGE");
        notificationKeyList.add("DELETE_SMS_CONTACT");
        notificationKeyList.add("MTEXTER_USER");
        notificationKeyList.add("MESSAGE");

    }

    public static final String MTCOM_PREFIX = "MTCOM";
    private static final String TAG = "MobiComPushReceiver";

    public static  boolean isMobiComPushNotification(Context context, Intent intent ){
        String message = intent.getStringExtra("collapse_key");
    if (message.contains(MTCOM_PREFIX) || notificationKeyList.contains(message)){
        return true;
    }else{
        return false;
    }
    }

    public static void  processPushNoticiation( Context context, Intent intent ) {
        //TODO: process notification logic from C2DMReciver.....
        Bundle extras = intent.getExtras();


    }

    public static void processMessage(Context context, Intent intent) {
//        UserPreferences usrpref = UserPreferences.getInstance(context);
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
              String deleteConversationForContact = intent.getStringExtra("DELETE_SMS_CONTACT");
              String deleteSms = intent.getStringExtra(notificationKeyList.get(2));
              String multipleMessageDelete = intent.getStringExtra("DELETE_MULTIPLE_MESSAGE");
              String mtexterUser = intent.getStringExtra("MTEXTER_USER");

//
//            /*
//                * AppUtil.myLogger(TAG, "Payload is : "+extras.get("payload")); //parse the
//                * message and do something with it. //For example, if the server
//                * sent the payload as "data.message=xxx", here you would have an
//                * extra called "message" String message =
//                * extras.getString("message"); AppUtil.myLogger(TAG, "received message: " +
//                * message);
//                * // Now do something smart based on the information
//                * //MainActivity.setMessage(message);
//                */
//            String message = intent.getStringExtra("collapse_key");
//            //Checking
//            String payloadForMtextReceived = intent.getStringExtra(notificationKeyList.get(0));
//            String deleteConversationForContact = intent.getStringExtra(notificationKeyList.get(1));
//            String deleteSms = intent.getStringExtra(notificationKeyList.get(2));
//            String multipleMessageDelete = intent.getStringExtra(OsuConstants.DELETE_MULTIPLE_MESSAGE);
//            String cancelCall = intent.getStringExtra(notificationKeyList.get(3));
//            String displayCallSetting = intent.getStringExtra(OsuConstants.DISPLAY_CALLS);
//
            String payloadForDelivered = intent.getStringExtra("DELIVERED");

            MobiComMessageService messageService = new MobiComMessageService(context, MessageIntentService.class);
            if (!TextUtils.isEmpty(payloadForDelivered)) {
                messageService.updateDeliveryStatus(payloadForDelivered);
            }

            if (!TextUtils.isEmpty(deleteConversationForContact)) {
//                ConversationService conversationService = new ConversationService(context);
//                conversationService.deleteConversationFromDevice(deleteConversationForContact);
//                sendConversationDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_CONVERSATION.toString(), deleteConversationForContact);
            }

            if (!TextUtils.isEmpty(mtexterUser)) {
                Log.i(TAG, "Received GCM message MTEXTER_USER: " + mtexterUser);
                if (mtexterUser.contains("{")) {
                    Gson gson = new Gson();
                    ContactContent contactContent = gson.fromJson(mtexterUser, ContactContent.class);
             //       ContactService.addMobiTexterUsers(context, contactContent.getContactNumber(), contactContent.getAppVersion(), true);
                } else {
                    String[] details = mtexterUser.split(",");
               //     ContactService.addMobiTexterUsers(context, details[0], Integer.parseInt(details[1]), true);
                }
            }
//

            if ("MARK_ALL_SMS_AS_READ".equalsIgnoreCase(message)) {
                //new NativeSmsService(context).markAllAsRead();
            } else if ("SYNC".equalsIgnoreCase(message)) {
                messageService.syncMessages();
            } else if ("SYNC_PENDING".equalsIgnoreCase(message)) {
//                new NativeSmsService(context).sync();
//                MessageStatUtil.sendMessageStatsToServer(context);
            }
        }
    }

    private static void processDeleteSingleMessageRequest(Context context, String deletedSmsKeyString, String contactNumber) {
//        ConversationService conversationService = new ConversationService(context);
//        contactNumber = conversationService.deleteMessageFromDevice(new MessageDatabaseService(context).getMessage(deletedSmsKeyString), contactNumber);
//        BroadcastService.sendMessageDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_SMS.toString(), deletedSmsKeyString, contactNumber);
    }

    public static void processMessageAsync(final Context context, final Intent intent) {
        new Thread( new Runnable() {
            @Override
            public void run() {
               processMessage(context,intent);
            }
        }).start();
    }
}
