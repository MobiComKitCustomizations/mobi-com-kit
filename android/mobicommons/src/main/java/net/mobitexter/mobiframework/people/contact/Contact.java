package net.mobitexter.mobiframework.people.contact;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.mobitexter.mobiframework.commons.core.utils.ContactNumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author devashish
 */
public class Contact {

    @Expose
    private String firstName = "";
    @Expose
    private String middleName = "";
    @Expose
    private String lastName = "";
    @Expose
    @SerializedName("emailId")
    private List<String> emailIds;
    @Expose
    @SerializedName("contactNumber")
    private List<String> contactNumbers = new ArrayList<String>();
    private Map<String, String> phoneNumbers;
    private String contactNumber;
    private String formattedContactNumber;
    @Expose
    private long contactId;
    private String fullName;

    private String userId;

    public Contact() {

    }

    public Contact(long contactId) {
        this.contactId = contactId;
    }

    public Contact(String firstName, String lastName) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Contact(String fullName, List<String> emailIds, List<String> contactNumbers, long contactId) {
        this(contactId);
        processFullName(fullName);
        this.emailIds = emailIds;
        this.contactNumbers = contactNumbers;
    }

    public Contact(String userId) {
        this.userId = userId;
    }

    public Contact(Context context, String userId) {
        this.userId = userId;
        this.setContactNumber(userId);
        this.processContactNumbers(context);
    }

    public void processContactNumbers(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String countryCode = telephonyManager.getSimCountryIso().toUpperCase();
        phoneNumbers = ContactUtils.getPhoneNumbers(context, getContactId());
        if (TextUtils.isEmpty(getFormattedContactNumber()) && !TextUtils.isEmpty(getContactNumber())) {
            setFormattedContactNumber(ContactNumberUtils.getPhoneNumber(getContactNumber(), countryCode));
        }

        if (!TextUtils.isEmpty(getContactNumber()) || phoneNumbers.isEmpty()) {
            return;
        }

        String mobileNumber = null;
        String mainNumber = null;
        for (String phoneNumber : phoneNumbers.keySet()) {
            setContactNumber(phoneNumber);
            //if (phoneNumbers.get(phoneNumber).equals(ContactsContract.CommonDataKinds.Phone.TYPE_MAIN)) {
            if (phoneNumbers.get(phoneNumber).equals("Main")) {
                mainNumber = phoneNumber;
                break;
            }
            if (phoneNumbers.get(phoneNumber).equals("Mobile")) {
                //if (phoneNumbers.get(phoneNumber).equals(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)) {
                mobileNumber = phoneNumber;
            }
        }

        if (!TextUtils.isEmpty(mobileNumber)) {
            setContactNumber(mobileNumber);
        }

        if (!TextUtils.isEmpty(mainNumber)) {
            setContactNumber(mainNumber);
        }

        //Note: contact.getContactNumber() is not a formattedNumber with country code so it might not match with
        //phoneNumbers key
        if (phoneNumbers.get(getContactNumber()) == null) {
            for (String phoneNumber : phoneNumbers.keySet()) {
                if (PhoneNumberUtils.compare(getContactNumber(), phoneNumber)) {
                    setContactNumber(phoneNumber);
                    break;
                }
            }
        }

        setFormattedContactNumber(ContactNumberUtils.getPhoneNumber(getContactNumber(), countryCode));
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getFormattedContactNumber() {
        return formattedContactNumber;
    }

    public void setFormattedContactNumber(String formattedContactNumber) {
        this.formattedContactNumber = formattedContactNumber;
    }

    public List<String> getContactNumbers() {
        return contactNumbers;
    }

    public void setContactNumbers(List<String> contactNumbers) {
        this.contactNumbers = contactNumbers;
    }

    public List<String> getEmailIds() {
        return emailIds;
    }

    public void setEmailIds(List<String> emailIds) {
        this.emailIds = emailIds;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public long getContactId() {
        return contactId;
    }

    public String getFullName() {
        return fullName == null ? "" : fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Map<String, String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(Map<String, String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public boolean hasMultiplePhoneNumbers() {
        return getPhoneNumbers() != null && !getPhoneNumbers().isEmpty() && getPhoneNumbers().size() > 1;
    }

    public void processFullName(String fullName) {
        this.fullName = fullName;
        if (fullName != null) {
            fullName = fullName.trim();
            String[] name = fullName.split(" ");
            firstName = name[0];
            if (firstName.length() <= 3 && name.length > 1) {
                firstName = name[1];
                lastName = name[name.length - 1];
                if (name.length > 2) {
                    middleName = fullName.substring(name[0].length() + firstName.length() + 1, fullName.length() - (lastName.length() + 1));
                }
            } else {
                if (name.length > 1) {
                    lastName = name[name.length - 1];
                    if (name.length > 2) {
                        middleName = fullName.substring(firstName.length() + 1, fullName.length() - (lastName.length() + 1));
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Contact{" +
                "firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", emailIds=" + emailIds +
                ", contactNumber=" + contactNumber +
                ", formattedContactNumber=" + formattedContactNumber +
                ", contactNumbers=" + contactNumbers +
                ", phoneNumbers=" + phoneNumbers +
                ", contactId=" + contactId +
                '}';
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getContactIds() {
        return TextUtils.isEmpty(getUserId()) ? getFormattedContactNumber() : getUserId();
    }
}
