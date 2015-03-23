package net.mobitexter.mobiframework.commons.core.utils;

import android.telephony.PhoneNumberUtils;

import net.mobitexter.mobiframework.people.contact.Contact;

/**
 * Created by devashish on 2/12/14.
 */
public class Support {

    public static final String SUPPORT_PHONE_NUMBER = "+918042028425";
    public static final String SUPPORT_INTENT_KEY = "SUPPORT_INTENT_KEY";

    public static String getSupportNumber() {
        return SUPPORT_PHONE_NUMBER;
    }

    public static boolean isSupportNumber(String contactNumber) {
        return PhoneNumberUtils.compare(getSupportNumber(), contactNumber);
    }

    public static Contact getSupportContact() {
        Contact contact = new Contact();
        contact.setFirstName("Support");
        contact.setLastName("");
        contact.setFullName("Support");
        contact.setContactNumber(SUPPORT_PHONE_NUMBER);
        contact.setFormattedContactNumber(SUPPORT_PHONE_NUMBER);
        return contact;
    }
}
