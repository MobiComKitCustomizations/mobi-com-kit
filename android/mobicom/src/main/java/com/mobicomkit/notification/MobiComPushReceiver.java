package com.mobicomkit.notification;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mobicomkit.broadcast.BroadcastService;
import com.mobicomkit.client.ui.MessageIntentService;
import com.mobicomkit.communication.message.MessageDeleteContent;
import com.mobicomkit.communication.message.MobiComMessageService;
import com.mobicomkit.communication.message.conversation.MobiComConversationService;
import com.mobicomkit.communication.message.database.MessageDatabaseService;
import com.mobicomkit.contact.ContactService;
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

    String playload = intent.getStringExtra("collapse_key");
    if (playload.contains(MTCOM_PREFIX) || notificationKeyList.contains(playload)){
        return true;
    }else{
        for(String key :notificationKeyList ){
            playload = intent.getStringExtra(key);
            if (playload!=null){
                return true;
            }
        }
        return false;
    }
    }

    public static void  processPushNoticiation( Context context, Intent intent ) {
        //TODO: process notification logic from C2DMReciver.....
        Bundle extras = intent.getExtras();


    }

    public static void processMessage(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            // ToDo: do something for invalidkey ;
            // && extras.get("InvalidKey") != null
              String message = intent.getStringExtra("collapse_key");
              String deleteConversationForContact = intent.getStringExtra("DELETE_SMS_CONTACT");
              String deleteSms = intent.getStringExtra("DELETE_SMS");
              String multipleMessageDelete = intent.getStringExtra("DELETE_MULTIPLE_MESSAGE");
              String mtexterUser = intent.getStringExtra("MTEXTER_USER");
              String payloadForDelivered = intent.getStringExtra("DELIVERED");

              MobiComMessageService messageService = new MobiComMessageService(context, MessageIntentService.class);

              if (!TextUtils.isEmpty(payloadForDelivered)) {
                messageService.updateDeliveryStatus(payloadForDelivered);
              }
              if (!TextUtils.isEmpty(deleteConversationForContact)) {
                MobiComConversationService conversationService = new MobiComConversationService(context);
                conversationService.deleteConversationFromDevice(deleteConversationForContact);
                BroadcastService.sendConversationDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_CONVERSATION.toString(), deleteConversationForContact);
            }

            if (!TextUtils.isEmpty(mtexterUser)) {
                Log.i(TAG, "Received GCM message MTEXTER_USER: " + mtexterUser);
                if (mtexterUser.contains("{")) {
                    Gson gson = new Gson();
                    ContactContent contactContent = gson.fromJson(mtexterUser, ContactContent.class);
                    ContactService.addUsersToContact(context, contactContent.getContactNumber(), contactContent.getAppVersion(), true);
                } else {
                    String[] details = mtexterUser.split(",");
                    ContactService.addUsersToContact(context, details[0], Short.parseShort(details[1]), true);
                }
            }
            if (!TextUtils.isEmpty(multipleMessageDelete)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                MessageDeleteContent messageDeleteContent = gson.fromJson(multipleMessageDelete, MessageDeleteContent.class);

                for (String deletedSmsKeyString: messageDeleteContent.getDeleteKeyStrings()) {
                    processDeleteSingleMessageRequest(context, deletedSmsKeyString, messageDeleteContent.getContactNumber());
                }
            }

            if (!TextUtils.isEmpty(deleteSms)) {
                String contactNumbers = deleteSms.split(",").length > 1 ? deleteSms.split(",")[1] : null;
                processDeleteSingleMessageRequest(context, deleteSms.split(",")[0], contactNumbers);
            }

            if ("MARK_ALL_SMS_AS_READ".equalsIgnoreCase(message)) {

            } else if ("SYNC".equalsIgnoreCase(message)) {
                messageService.syncMessages();
            } else if ("SYNC_PENDING".equalsIgnoreCase(message)) {
              //  MessageStatUtil.sendMessageStatsToServer(context);
            }
        }
    }


    private static void processDeleteSingleMessageRequest(Context context, String deletedSmsKeyString, String contactNumber) {
        MobiComConversationService conversationService = new MobiComConversationService(context);
        contactNumber = conversationService.deleteMessageFromDevice(new MessageDatabaseService(context).getSms(deletedSmsKeyString), contactNumber);
        BroadcastService.sendMessageDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_MESSAGE.toString(), deletedSmsKeyString, contactNumber);
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
