package net.mobitexter.mobiframework.commons.core.utils;

import android.telephony.PhoneNumberUtils;

import net.mobitexter.mobiframework.people.contact.Contact;

/**
 * Created by devashish on 2/12/14.
 */
public class Support {

    public static final String MOBITEXTER_SUPPORT_PHONE_NUMBER = "+918042028425";
    public static final String MOBITEXTER_SUPPORT_INTENT_KEY = "MOBITEXTER_SUPPORT_INTENT_KEY";

    public static String getSupportNumber() {
        return MOBITEXTER_SUPPORT_PHONE_NUMBER;
    }

    public static boolean isSupportNumber(String contactNumber) {
        return PhoneNumberUtils.compare(getSupportNumber(), contactNumber);
    }

    public static Contact getSupportContact() {
        Contact contact = new Contact();
        contact.setFirstName("MobiTexter");
        contact.setLastName("Support");
        contact.setFullName("MobiTexter Support");
        contact.setContactNumber(MOBITEXTER_SUPPORT_PHONE_NUMBER);
        contact.setFormattedContactNumber(MOBITEXTER_SUPPORT_PHONE_NUMBER);
        return contact;
    }
}
