package com.android.vcard;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import com.android.vcard.VCardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VCardEntry {
    private static final List<String> sEmptyList;
    private static final Map<String, Integer> sImMap = new HashMap();
    private final Account mAccount;
    private List<AndroidCustomData> mAndroidCustomDataList;
    private AnniversaryData mAnniversary;
    private BirthdayData mBirthday;
    private List<VCardEntry> mChildren;
    private List<EmailData> mEmailList;
    private List<ImData> mImList;
    private final NameData mNameData;
    private List<NicknameData> mNicknameList;
    private List<NoteData> mNoteList;
    private List<OrganizationData> mOrganizationList;
    private List<PhoneData> mPhoneList;
    private List<PhotoData> mPhotoList;
    private List<PostalData> mPostalList;
    private List<SipData> mSipList;
    private final int mVCardType;
    private List<WebsiteData> mWebsiteList;

    public interface EntryElement {
        void constructInsertOperation(List<ContentProviderOperation> list, int i);

        EntryLabel getEntryLabel();

        boolean isEmpty();
    }

    public interface EntryElementIterator {
        boolean onElement(EntryElement entryElement);

        void onElementGroupEnded();

        void onElementGroupStarted(EntryLabel entryLabel);

        void onIterationEnded();

        void onIterationStarted();
    }

    public enum EntryLabel {
        NAME,
        PHONE,
        EMAIL,
        POSTAL_ADDRESS,
        ORGANIZATION,
        IM,
        PHOTO,
        WEBSITE,
        SIP,
        NICKNAME,
        NOTE,
        BIRTHDAY,
        ANNIVERSARY,
        ANDROID_CUSTOM
    }

    static {
        sImMap.put("X-AIM", 0);
        sImMap.put("X-MSN", 1);
        sImMap.put("X-YAHOO", 2);
        sImMap.put("X-ICQ", 6);
        sImMap.put("X-JABBER", 7);
        sImMap.put("X-SKYPE-USERNAME", 3);
        sImMap.put("X-GOOGLE-TALK", 5);
        sImMap.put("X-GOOGLE TALK", 5);
        sEmptyList = Collections.unmodifiableList(new ArrayList(0));
    }

    public static class NameData implements EntryElement {
        public String displayName;
        private String mFamily;
        private String mFormatted;
        private String mGiven;
        private String mMiddle;
        private String mPhoneticFamily;
        private String mPhoneticGiven;
        private String mPhoneticMiddle;
        private String mPrefix;
        private String mSortString;
        private String mSuffix;

        public boolean emptyStructuredName() {
            return TextUtils.isEmpty(this.mFamily) && TextUtils.isEmpty(this.mGiven) && TextUtils.isEmpty(this.mMiddle) && TextUtils.isEmpty(this.mPrefix) && TextUtils.isEmpty(this.mSuffix);
        }

        public boolean emptyPhoneticStructuredName() {
            return TextUtils.isEmpty(this.mPhoneticFamily) && TextUtils.isEmpty(this.mPhoneticGiven) && TextUtils.isEmpty(this.mPhoneticMiddle);
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/name");
            if (!TextUtils.isEmpty(this.mGiven)) {
                builder.withValue("data2", this.mGiven);
            }
            if (!TextUtils.isEmpty(this.mFamily)) {
                builder.withValue("data3", this.mFamily);
            }
            if (!TextUtils.isEmpty(this.mMiddle)) {
                builder.withValue("data5", this.mMiddle);
            }
            if (!TextUtils.isEmpty(this.mPrefix)) {
                builder.withValue("data4", this.mPrefix);
            }
            if (!TextUtils.isEmpty(this.mSuffix)) {
                builder.withValue("data6", this.mSuffix);
            }
            boolean phoneticNameSpecified = false;
            if (!TextUtils.isEmpty(this.mPhoneticGiven)) {
                builder.withValue("data7", this.mPhoneticGiven);
                phoneticNameSpecified = true;
            }
            if (!TextUtils.isEmpty(this.mPhoneticFamily)) {
                builder.withValue("data9", this.mPhoneticFamily);
                phoneticNameSpecified = true;
            }
            if (!TextUtils.isEmpty(this.mPhoneticMiddle)) {
                builder.withValue("data8", this.mPhoneticMiddle);
                phoneticNameSpecified = true;
            }
            if (!phoneticNameSpecified) {
                builder.withValue("data7", this.mSortString);
            }
            builder.withValue("data1", this.displayName);
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mFamily) && TextUtils.isEmpty(this.mMiddle) && TextUtils.isEmpty(this.mGiven) && TextUtils.isEmpty(this.mPrefix) && TextUtils.isEmpty(this.mSuffix) && TextUtils.isEmpty(this.mFormatted) && TextUtils.isEmpty(this.mPhoneticFamily) && TextUtils.isEmpty(this.mPhoneticMiddle) && TextUtils.isEmpty(this.mPhoneticGiven) && TextUtils.isEmpty(this.mSortString);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NameData)) {
                return false;
            }
            NameData nameData = (NameData) obj;
            return TextUtils.equals(this.mFamily, nameData.mFamily) && TextUtils.equals(this.mMiddle, nameData.mMiddle) && TextUtils.equals(this.mGiven, nameData.mGiven) && TextUtils.equals(this.mPrefix, nameData.mPrefix) && TextUtils.equals(this.mSuffix, nameData.mSuffix) && TextUtils.equals(this.mFormatted, nameData.mFormatted) && TextUtils.equals(this.mPhoneticFamily, nameData.mPhoneticFamily) && TextUtils.equals(this.mPhoneticMiddle, nameData.mPhoneticMiddle) && TextUtils.equals(this.mPhoneticGiven, nameData.mPhoneticGiven) && TextUtils.equals(this.mSortString, nameData.mSortString);
        }

        public int hashCode() {
            String[] hashTargets = {this.mFamily, this.mMiddle, this.mGiven, this.mPrefix, this.mSuffix, this.mFormatted, this.mPhoneticFamily, this.mPhoneticMiddle, this.mPhoneticGiven, this.mSortString};
            int hash = 0;
            int len$ = hashTargets.length;
            for (int i$ = 0; i$ < len$; i$++) {
                String hashTarget = hashTargets[i$];
                hash = (hash * 31) + (hashTarget != null ? hashTarget.hashCode() : 0);
            }
            return hash;
        }

        public String toString() {
            return String.format("family: %s, given: %s, middle: %s, prefix: %s, suffix: %s", this.mFamily, this.mGiven, this.mMiddle, this.mPrefix, this.mSuffix);
        }

        @Override
        public final EntryLabel getEntryLabel() {
            return EntryLabel.NAME;
        }
    }

    public static class PhoneData implements EntryElement {
        private boolean mIsPrimary;
        private final String mLabel;
        private final String mNumber;
        private final int mType;

        public PhoneData(String data, int type, String label, boolean isPrimary) {
            this.mNumber = data;
            this.mType = type;
            this.mLabel = label;
            this.mIsPrimary = isPrimary;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
            builder.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builder.withValue("data3", this.mLabel);
            }
            builder.withValue("data1", this.mNumber);
            if (this.mIsPrimary) {
                builder.withValue("is_primary", 1);
            }
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mNumber);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PhoneData)) {
                return false;
            }
            PhoneData phoneData = (PhoneData) obj;
            return this.mType == phoneData.mType && TextUtils.equals(this.mNumber, phoneData.mNumber) && TextUtils.equals(this.mLabel, phoneData.mLabel) && this.mIsPrimary == phoneData.mIsPrimary;
        }

        public int hashCode() {
            int hash = this.mType;
            return (((((hash * 31) + (this.mNumber != null ? this.mNumber.hashCode() : 0)) * 31) + (this.mLabel != null ? this.mLabel.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
        }

        public String toString() {
            return String.format("type: %d, data: %s, label: %s, isPrimary: %s", Integer.valueOf(this.mType), this.mNumber, this.mLabel, Boolean.valueOf(this.mIsPrimary));
        }

        @Override
        public final EntryLabel getEntryLabel() {
            return EntryLabel.PHONE;
        }
    }

    public static class EmailData implements EntryElement {
        private final String mAddress;
        private final boolean mIsPrimary;
        private final String mLabel;
        private final int mType;

        public EmailData(String data, int type, String label, boolean isPrimary) {
            this.mType = type;
            this.mAddress = data;
            this.mLabel = label;
            this.mIsPrimary = isPrimary;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/email_v2");
            builder.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builder.withValue("data3", this.mLabel);
            }
            builder.withValue("data1", this.mAddress);
            if (this.mIsPrimary) {
                builder.withValue("is_primary", 1);
            }
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mAddress);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof EmailData)) {
                return false;
            }
            EmailData emailData = (EmailData) obj;
            return this.mType == emailData.mType && TextUtils.equals(this.mAddress, emailData.mAddress) && TextUtils.equals(this.mLabel, emailData.mLabel) && this.mIsPrimary == emailData.mIsPrimary;
        }

        public int hashCode() {
            int hash = this.mType;
            return (((((hash * 31) + (this.mAddress != null ? this.mAddress.hashCode() : 0)) * 31) + (this.mLabel != null ? this.mLabel.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
        }

        public String toString() {
            return String.format("type: %d, data: %s, label: %s, isPrimary: %s", Integer.valueOf(this.mType), this.mAddress, this.mLabel, Boolean.valueOf(this.mIsPrimary));
        }

        @Override
        public final EntryLabel getEntryLabel() {
            return EntryLabel.EMAIL;
        }
    }

    public static class PostalData implements EntryElement {
        private final String mCountry;
        private final String mExtendedAddress;
        private boolean mIsPrimary;
        private final String mLabel;
        private final String mLocalty;
        private final String mPobox;
        private final String mPostalCode;
        private final String mRegion;
        private final String mStreet;
        private final int mType;
        private int mVCardType;

        public PostalData(String pobox, String extendedAddress, String street, String localty, String region, String postalCode, String country, int type, String label, boolean isPrimary, int vcardType) {
            this.mType = type;
            this.mPobox = pobox;
            this.mExtendedAddress = extendedAddress;
            this.mStreet = street;
            this.mLocalty = localty;
            this.mRegion = region;
            this.mPostalCode = postalCode;
            this.mCountry = country;
            this.mLabel = label;
            this.mIsPrimary = isPrimary;
            this.mVCardType = vcardType;
        }

        public static PostalData constructPostalData(List<String> propValueList, int type, String label, boolean isPrimary, int vcardType) {
            String[] dataArray = new String[7];
            int size = propValueList.size();
            if (size > 7) {
                size = 7;
            }
            int i = 0;
            for (String addressElement : propValueList) {
                dataArray[i] = addressElement;
                i++;
                if (i >= size) {
                    break;
                }
            }
            while (true) {
                int i2 = i;
                if (i2 < 7) {
                    i = i2 + 1;
                    dataArray[i2] = null;
                } else {
                    return new PostalData(dataArray[0], dataArray[1], dataArray[2], dataArray[3], dataArray[4], dataArray[5], dataArray[6], type, label, isPrimary, vcardType);
                }
            }
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            String streetString;
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/postal-address_v2");
            builder.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builder.withValue("data3", this.mLabel);
            }
            if (TextUtils.isEmpty(this.mStreet)) {
                if (TextUtils.isEmpty(this.mExtendedAddress)) {
                    streetString = null;
                } else {
                    streetString = this.mExtendedAddress;
                }
            } else if (TextUtils.isEmpty(this.mExtendedAddress)) {
                streetString = this.mStreet;
            } else {
                streetString = this.mStreet + " " + this.mExtendedAddress;
            }
            builder.withValue("data5", this.mPobox);
            builder.withValue("data4", streetString);
            builder.withValue("data7", this.mLocalty);
            builder.withValue("data8", this.mRegion);
            builder.withValue("data9", this.mPostalCode);
            builder.withValue("data10", this.mCountry);
            builder.withValue("data1", getFormattedAddress(this.mVCardType));
            if (this.mIsPrimary) {
                builder.withValue("is_primary", 1);
            }
            operationList.add(builder.build());
        }

        public String getFormattedAddress(int vcardType) {
            StringBuilder builder = new StringBuilder();
            boolean empty = true;
            String[] dataArray = {this.mPobox, this.mExtendedAddress, this.mStreet, this.mLocalty, this.mRegion, this.mPostalCode, this.mCountry};
            if (VCardConfig.isJapaneseDevice(vcardType)) {
                for (int i = 6; i >= 0; i--) {
                    String addressPart = dataArray[i];
                    if (!TextUtils.isEmpty(addressPart)) {
                        if (!empty) {
                            builder.append(' ');
                        } else {
                            empty = false;
                        }
                        builder.append(addressPart);
                    }
                }
            } else {
                for (int i2 = 0; i2 < 7; i2++) {
                    String addressPart2 = dataArray[i2];
                    if (!TextUtils.isEmpty(addressPart2)) {
                        if (!empty) {
                            builder.append(' ');
                        } else {
                            empty = false;
                        }
                        builder.append(addressPart2);
                    }
                }
            }
            return builder.toString().trim();
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mPobox) && TextUtils.isEmpty(this.mExtendedAddress) && TextUtils.isEmpty(this.mStreet) && TextUtils.isEmpty(this.mLocalty) && TextUtils.isEmpty(this.mRegion) && TextUtils.isEmpty(this.mPostalCode) && TextUtils.isEmpty(this.mCountry);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PostalData)) {
                return false;
            }
            PostalData postalData = (PostalData) obj;
            return this.mType == postalData.mType && (this.mType != 0 || TextUtils.equals(this.mLabel, postalData.mLabel)) && this.mIsPrimary == postalData.mIsPrimary && TextUtils.equals(this.mPobox, postalData.mPobox) && TextUtils.equals(this.mExtendedAddress, postalData.mExtendedAddress) && TextUtils.equals(this.mStreet, postalData.mStreet) && TextUtils.equals(this.mLocalty, postalData.mLocalty) && TextUtils.equals(this.mRegion, postalData.mRegion) && TextUtils.equals(this.mPostalCode, postalData.mPostalCode) && TextUtils.equals(this.mCountry, postalData.mCountry);
        }

        public int hashCode() {
            int hash = this.mType;
            int hash2 = (((hash * 31) + (this.mLabel != null ? this.mLabel.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
            String[] hashTargets = {this.mPobox, this.mExtendedAddress, this.mStreet, this.mLocalty, this.mRegion, this.mPostalCode, this.mCountry};
            int len$ = hashTargets.length;
            for (int i$ = 0; i$ < len$; i$++) {
                String hashTarget = hashTargets[i$];
                hash2 = (hash2 * 31) + (hashTarget != null ? hashTarget.hashCode() : 0);
            }
            return hash2;
        }

        public String toString() {
            return String.format("type: %d, label: %s, isPrimary: %s, pobox: %s, extendedAddress: %s, street: %s, localty: %s, region: %s, postalCode %s, country: %s", Integer.valueOf(this.mType), this.mLabel, Boolean.valueOf(this.mIsPrimary), this.mPobox, this.mExtendedAddress, this.mStreet, this.mLocalty, this.mRegion, this.mPostalCode, this.mCountry);
        }

        @Override
        public final EntryLabel getEntryLabel() {
            return EntryLabel.POSTAL_ADDRESS;
        }
    }

    public static class OrganizationData implements EntryElement {
        private String mDepartmentName;
        private boolean mIsPrimary;
        private String mOrganizationName;
        private final String mPhoneticName;
        private String mTitle;
        private final int mType;

        public OrganizationData(String organizationName, String departmentName, String titleName, String phoneticName, int type, boolean isPrimary) {
            this.mType = type;
            this.mOrganizationName = organizationName;
            this.mDepartmentName = departmentName;
            this.mTitle = titleName;
            this.mPhoneticName = phoneticName;
            this.mIsPrimary = isPrimary;
        }

        public String getFormattedString() {
            StringBuilder builder = new StringBuilder();
            if (!TextUtils.isEmpty(this.mOrganizationName)) {
                builder.append(this.mOrganizationName);
            }
            if (!TextUtils.isEmpty(this.mDepartmentName)) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(this.mDepartmentName);
            }
            if (!TextUtils.isEmpty(this.mTitle)) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(this.mTitle);
            }
            return builder.toString();
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/organization");
            builder.withValue("data2", Integer.valueOf(this.mType));
            if (this.mOrganizationName != null) {
                builder.withValue("data1", this.mOrganizationName);
            }
            if (this.mDepartmentName != null) {
                builder.withValue("data5", this.mDepartmentName);
            }
            if (this.mTitle != null) {
                builder.withValue("data4", this.mTitle);
            }
            if (this.mPhoneticName != null) {
                builder.withValue("data8", this.mPhoneticName);
            }
            if (this.mIsPrimary) {
                builder.withValue("is_primary", 1);
            }
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mOrganizationName) && TextUtils.isEmpty(this.mDepartmentName) && TextUtils.isEmpty(this.mTitle) && TextUtils.isEmpty(this.mPhoneticName);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof OrganizationData)) {
                return false;
            }
            OrganizationData organization = (OrganizationData) obj;
            return this.mType == organization.mType && TextUtils.equals(this.mOrganizationName, organization.mOrganizationName) && TextUtils.equals(this.mDepartmentName, organization.mDepartmentName) && TextUtils.equals(this.mTitle, organization.mTitle) && this.mIsPrimary == organization.mIsPrimary;
        }

        public int hashCode() {
            int hash = this.mType;
            return (((((((hash * 31) + (this.mOrganizationName != null ? this.mOrganizationName.hashCode() : 0)) * 31) + (this.mDepartmentName != null ? this.mDepartmentName.hashCode() : 0)) * 31) + (this.mTitle != null ? this.mTitle.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
        }

        public String toString() {
            return String.format("type: %d, organization: %s, department: %s, title: %s, isPrimary: %s", Integer.valueOf(this.mType), this.mOrganizationName, this.mDepartmentName, this.mTitle, Boolean.valueOf(this.mIsPrimary));
        }

        @Override
        public final EntryLabel getEntryLabel() {
            return EntryLabel.ORGANIZATION;
        }
    }

    public static class ImData implements EntryElement {
        private final String mAddress;
        private final String mCustomProtocol;
        private final boolean mIsPrimary;
        private final int mProtocol;
        private final int mType;

        public ImData(int protocol, String customProtocol, String address, int type, boolean isPrimary) {
            this.mProtocol = protocol;
            this.mCustomProtocol = customProtocol;
            this.mType = type;
            this.mAddress = address;
            this.mIsPrimary = isPrimary;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/im");
            builder.withValue("data2", Integer.valueOf(this.mType));
            builder.withValue("data5", Integer.valueOf(this.mProtocol));
            builder.withValue("data1", this.mAddress);
            if (this.mProtocol == -1) {
                builder.withValue("data6", this.mCustomProtocol);
            }
            if (this.mIsPrimary) {
                builder.withValue("is_primary", 1);
            }
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mAddress);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ImData)) {
                return false;
            }
            ImData imData = (ImData) obj;
            return this.mType == imData.mType && this.mProtocol == imData.mProtocol && TextUtils.equals(this.mCustomProtocol, imData.mCustomProtocol) && TextUtils.equals(this.mAddress, imData.mAddress) && this.mIsPrimary == imData.mIsPrimary;
        }

        public int hashCode() {
            int hash = this.mType;
            return (((((((hash * 31) + this.mProtocol) * 31) + (this.mCustomProtocol != null ? this.mCustomProtocol.hashCode() : 0)) * 31) + (this.mAddress != null ? this.mAddress.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
        }

        public String toString() {
            return String.format("type: %d, protocol: %d, custom_protcol: %s, data: %s, isPrimary: %s", Integer.valueOf(this.mType), Integer.valueOf(this.mProtocol), this.mCustomProtocol, this.mAddress, Boolean.valueOf(this.mIsPrimary));
        }

        @Override
        public final EntryLabel getEntryLabel() {
            return EntryLabel.IM;
        }
    }

    public static class PhotoData implements EntryElement {
        private final byte[] mBytes;
        private final String mFormat;
        private Integer mHashCode = null;
        private final boolean mIsPrimary;

        public PhotoData(String format, byte[] photoBytes, boolean isPrimary) {
            this.mFormat = format;
            this.mBytes = photoBytes;
            this.mIsPrimary = isPrimary;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/photo");
            builder.withValue("data15", this.mBytes);
            if (this.mIsPrimary) {
                builder.withValue("is_primary", 1);
            }
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return this.mBytes == null || this.mBytes.length == 0;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PhotoData)) {
                return false;
            }
            PhotoData photoData = (PhotoData) obj;
            return TextUtils.equals(this.mFormat, photoData.mFormat) && Arrays.equals(this.mBytes, photoData.mBytes) && this.mIsPrimary == photoData.mIsPrimary;
        }

        public int hashCode() {
            if (this.mHashCode != null) {
                return this.mHashCode.intValue();
            }
            int hash = (this.mFormat != null ? this.mFormat.hashCode() : 0) * 31;
            if (this.mBytes != null) {
                byte[] arr$ = this.mBytes;
                for (byte b : arr$) {
                    hash += b;
                }
            }
            int hash2 = (hash * 31) + (this.mIsPrimary ? 1231 : 1237);
            this.mHashCode = Integer.valueOf(hash2);
            return hash2;
        }

        public String toString() {
            return String.format("format: %s: size: %d, isPrimary: %s", this.mFormat, Integer.valueOf(this.mBytes.length), Boolean.valueOf(this.mIsPrimary));
        }

        @Override
        public final EntryLabel getEntryLabel() {
            return EntryLabel.PHOTO;
        }
    }

    public static class NicknameData implements EntryElement {
        private final String mNickname;

        public NicknameData(String nickname) {
            this.mNickname = nickname;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/nickname");
            builder.withValue("data2", 1);
            builder.withValue("data1", this.mNickname);
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mNickname);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof NicknameData)) {
                return false;
            }
            NicknameData nicknameData = (NicknameData) obj;
            return TextUtils.equals(this.mNickname, nicknameData.mNickname);
        }

        public int hashCode() {
            if (this.mNickname != null) {
                return this.mNickname.hashCode();
            }
            return 0;
        }

        public String toString() {
            return "nickname: " + this.mNickname;
        }

        @Override
        public EntryLabel getEntryLabel() {
            return EntryLabel.NICKNAME;
        }
    }

    public static class NoteData implements EntryElement {
        public final String mNote;

        public NoteData(String note) {
            this.mNote = note;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/note");
            builder.withValue("data1", this.mNote);
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mNote);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NoteData)) {
                return false;
            }
            NoteData noteData = (NoteData) obj;
            return TextUtils.equals(this.mNote, noteData.mNote);
        }

        public int hashCode() {
            if (this.mNote != null) {
                return this.mNote.hashCode();
            }
            return 0;
        }

        public String toString() {
            return "note: " + this.mNote;
        }

        @Override
        public EntryLabel getEntryLabel() {
            return EntryLabel.NOTE;
        }
    }

    public static class WebsiteData implements EntryElement {
        private final String mWebsite;

        public WebsiteData(String website) {
            this.mWebsite = website;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/website");
            builder.withValue("data1", this.mWebsite);
            builder.withValue("data2", 1);
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mWebsite);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof WebsiteData)) {
                return false;
            }
            WebsiteData websiteData = (WebsiteData) obj;
            return TextUtils.equals(this.mWebsite, websiteData.mWebsite);
        }

        public int hashCode() {
            if (this.mWebsite != null) {
                return this.mWebsite.hashCode();
            }
            return 0;
        }

        public String toString() {
            return "website: " + this.mWebsite;
        }

        @Override
        public EntryLabel getEntryLabel() {
            return EntryLabel.WEBSITE;
        }
    }

    public static class BirthdayData implements EntryElement {
        private final String mBirthday;

        public BirthdayData(String birthday) {
            this.mBirthday = birthday;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/contact_event");
            builder.withValue("data1", this.mBirthday);
            builder.withValue("data2", 3);
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mBirthday);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BirthdayData)) {
                return false;
            }
            BirthdayData birthdayData = (BirthdayData) obj;
            return TextUtils.equals(this.mBirthday, birthdayData.mBirthday);
        }

        public int hashCode() {
            if (this.mBirthday != null) {
                return this.mBirthday.hashCode();
            }
            return 0;
        }

        public String toString() {
            return "birthday: " + this.mBirthday;
        }

        @Override
        public EntryLabel getEntryLabel() {
            return EntryLabel.BIRTHDAY;
        }
    }

    public static class AnniversaryData implements EntryElement {
        private final String mAnniversary;

        public AnniversaryData(String anniversary) {
            this.mAnniversary = anniversary;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/contact_event");
            builder.withValue("data1", this.mAnniversary);
            builder.withValue("data2", 1);
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mAnniversary);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AnniversaryData)) {
                return false;
            }
            AnniversaryData anniversaryData = (AnniversaryData) obj;
            return TextUtils.equals(this.mAnniversary, anniversaryData.mAnniversary);
        }

        public int hashCode() {
            if (this.mAnniversary != null) {
                return this.mAnniversary.hashCode();
            }
            return 0;
        }

        public String toString() {
            return "anniversary: " + this.mAnniversary;
        }

        @Override
        public EntryLabel getEntryLabel() {
            return EntryLabel.ANNIVERSARY;
        }
    }

    public static class SipData implements EntryElement {
        private final String mAddress;
        private final boolean mIsPrimary;
        private final String mLabel;
        private final int mType;

        public SipData(String rawSip, int type, String label, boolean isPrimary) {
            if (rawSip.startsWith("sip:")) {
                this.mAddress = rawSip.substring(4);
            } else {
                this.mAddress = rawSip;
            }
            this.mType = type;
            this.mLabel = label;
            this.mIsPrimary = isPrimary;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/sip_address");
            builder.withValue("data1", this.mAddress);
            builder.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builder.withValue("data3", this.mLabel);
            }
            if (this.mIsPrimary) {
                builder.withValue("is_primary", Boolean.valueOf(this.mIsPrimary));
            }
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mAddress);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SipData)) {
                return false;
            }
            SipData sipData = (SipData) obj;
            return this.mType == sipData.mType && TextUtils.equals(this.mLabel, sipData.mLabel) && TextUtils.equals(this.mAddress, sipData.mAddress) && this.mIsPrimary == sipData.mIsPrimary;
        }

        public int hashCode() {
            int hash = this.mType;
            return (((((hash * 31) + (this.mLabel != null ? this.mLabel.hashCode() : 0)) * 31) + (this.mAddress != null ? this.mAddress.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
        }

        public String toString() {
            return "sip: " + this.mAddress;
        }

        @Override
        public EntryLabel getEntryLabel() {
            return EntryLabel.SIP;
        }
    }

    public static class AndroidCustomData implements EntryElement {
        private final List<String> mDataList;
        private final String mMimeType;

        public AndroidCustomData(String mimeType, List<String> dataList) {
            this.mMimeType = mimeType;
            this.mDataList = dataList;
        }

        public static AndroidCustomData constructAndroidCustomData(List<String> list) {
            String mimeType;
            List<String> dataList;
            if (list == null) {
                mimeType = null;
                dataList = null;
            } else if (list.size() < 2) {
                mimeType = list.get(0);
                dataList = null;
            } else {
                int max = list.size() < 16 ? list.size() : 16;
                mimeType = list.get(0);
                dataList = list.subList(1, max);
            }
            return new AndroidCustomData(mimeType, dataList);
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", this.mMimeType);
            for (int i = 0; i < this.mDataList.size(); i++) {
                String value = this.mDataList.get(i);
                if (!TextUtils.isEmpty(value)) {
                    builder.withValue("data" + (i + 1), value);
                }
            }
            operationList.add(builder.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mMimeType) || this.mDataList == null || this.mDataList.size() == 0;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AndroidCustomData)) {
                return false;
            }
            AndroidCustomData data = (AndroidCustomData) obj;
            if (!TextUtils.equals(this.mMimeType, data.mMimeType)) {
                return false;
            }
            if (this.mDataList == null) {
                return data.mDataList == null;
            }
            int size = this.mDataList.size();
            if (size != data.mDataList.size()) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                if (!TextUtils.equals(this.mDataList.get(i), data.mDataList.get(i))) {
                    return false;
                }
            }
            return true;
        }

        public int hashCode() {
            int hash = this.mMimeType != null ? this.mMimeType.hashCode() : 0;
            if (this.mDataList != null) {
                Iterator<String> it = this.mDataList.iterator();
                while (it.hasNext()) {
                    String data = it.next();
                    hash = (hash * 31) + (data != null ? data.hashCode() : 0);
                }
            }
            return hash;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("android-custom: " + this.mMimeType + ", data: ");
            builder.append(this.mDataList == null ? "null" : Arrays.toString(this.mDataList.toArray()));
            return builder.toString();
        }

        @Override
        public EntryLabel getEntryLabel() {
            return EntryLabel.ANDROID_CUSTOM;
        }
    }

    public final void iterateAllData(EntryElementIterator iterator) {
        iterator.onIterationStarted();
        iterator.onElementGroupStarted(this.mNameData.getEntryLabel());
        iterator.onElement(this.mNameData);
        iterator.onElementGroupEnded();
        iterateOneList(this.mPhoneList, iterator);
        iterateOneList(this.mEmailList, iterator);
        iterateOneList(this.mPostalList, iterator);
        iterateOneList(this.mOrganizationList, iterator);
        iterateOneList(this.mImList, iterator);
        iterateOneList(this.mPhotoList, iterator);
        iterateOneList(this.mWebsiteList, iterator);
        iterateOneList(this.mSipList, iterator);
        iterateOneList(this.mNicknameList, iterator);
        iterateOneList(this.mNoteList, iterator);
        iterateOneList(this.mAndroidCustomDataList, iterator);
        if (this.mBirthday != null) {
            iterator.onElementGroupStarted(this.mBirthday.getEntryLabel());
            iterator.onElement(this.mBirthday);
            iterator.onElementGroupEnded();
        }
        if (this.mAnniversary != null) {
            iterator.onElementGroupStarted(this.mAnniversary.getEntryLabel());
            iterator.onElement(this.mAnniversary);
            iterator.onElementGroupEnded();
        }
        iterator.onIterationEnded();
    }

    private void iterateOneList(List<? extends EntryElement> elemList, EntryElementIterator iterator) {
        if (elemList != null && elemList.size() > 0) {
            iterator.onElementGroupStarted(elemList.get(0).getEntryLabel());
            for (EntryElement elem : elemList) {
                iterator.onElement(elem);
            }
            iterator.onElementGroupEnded();
        }
    }

    private class IsIgnorableIterator implements EntryElementIterator {
        private boolean mEmpty;

        private IsIgnorableIterator() {
            this.mEmpty = true;
        }

        @Override
        public void onIterationStarted() {
        }

        @Override
        public void onIterationEnded() {
        }

        @Override
        public void onElementGroupStarted(EntryLabel label) {
        }

        @Override
        public void onElementGroupEnded() {
        }

        @Override
        public boolean onElement(EntryElement elem) {
            if (elem.isEmpty()) {
                return true;
            }
            this.mEmpty = false;
            return false;
        }

        public boolean getResult() {
            return this.mEmpty;
        }
    }

    private class ToStringIterator implements EntryElementIterator {
        private StringBuilder mBuilder;
        private boolean mFirstElement;

        private ToStringIterator() {
        }

        @Override
        public void onIterationStarted() {
            this.mBuilder = new StringBuilder();
            this.mBuilder.append("[[hash: " + VCardEntry.this.hashCode() + "\n");
        }

        @Override
        public void onElementGroupStarted(EntryLabel label) {
            this.mBuilder.append(label.toString() + ": ");
            this.mFirstElement = true;
        }

        @Override
        public boolean onElement(EntryElement elem) {
            if (!this.mFirstElement) {
                this.mBuilder.append(", ");
                this.mFirstElement = false;
            }
            this.mBuilder.append("[").append(elem.toString()).append("]");
            return true;
        }

        @Override
        public void onElementGroupEnded() {
            this.mBuilder.append("\n");
        }

        @Override
        public void onIterationEnded() {
            this.mBuilder.append("]]\n");
        }

        public String toString() {
            return this.mBuilder.toString();
        }
    }

    private class InsertOperationConstrutor implements EntryElementIterator {
        private final int mBackReferenceIndex;
        private final List<ContentProviderOperation> mOperationList;

        public InsertOperationConstrutor(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            this.mOperationList = operationList;
            this.mBackReferenceIndex = backReferenceIndex;
        }

        @Override
        public void onIterationStarted() {
        }

        @Override
        public void onIterationEnded() {
        }

        @Override
        public void onElementGroupStarted(EntryLabel label) {
        }

        @Override
        public void onElementGroupEnded() {
        }

        @Override
        public boolean onElement(EntryElement elem) {
            if (!elem.isEmpty()) {
                elem.constructInsertOperation(this.mOperationList, this.mBackReferenceIndex);
                return true;
            }
            return true;
        }
    }

    public String toString() {
        ToStringIterator iterator = new ToStringIterator();
        iterateAllData(iterator);
        return iterator.toString();
    }

    public VCardEntry() {
        this(-1073741824);
    }

    public VCardEntry(int vcardType) {
        this(vcardType, null);
    }

    public VCardEntry(int vcardType, Account account) {
        this.mNameData = new NameData();
        this.mVCardType = vcardType;
        this.mAccount = account;
    }

    private void addPhone(int type, String data, String label, boolean isPrimary) {
        String formattedNumber;
        if (this.mPhoneList == null) {
            this.mPhoneList = new ArrayList();
        }
        StringBuilder builder = new StringBuilder();
        String trimmed = data.trim();
        if (type == 6 || VCardConfig.refrainPhoneNumberFormatting(this.mVCardType)) {
            formattedNumber = trimmed;
        } else {
            boolean hasPauseOrWait = false;
            int length = trimmed.length();
            for (int i = 0; i < length; i++) {
                char ch = trimmed.charAt(i);
                if (ch == 'p' || ch == 'P') {
                    builder.append(',');
                    hasPauseOrWait = true;
                } else if (ch == 'w' || ch == 'W') {
                    builder.append(';');
                    hasPauseOrWait = true;
                } else if (('0' <= ch && ch <= '9') || (i == 0 && ch == '+')) {
                    builder.append(ch);
                }
            }
            if (!hasPauseOrWait) {
                int formattingType = VCardUtils.getPhoneNumberFormat(this.mVCardType);
                formattedNumber = VCardUtils.PhoneNumberUtilsPort.formatNumber(builder.toString(), formattingType);
            } else {
                formattedNumber = builder.toString();
            }
        }
        PhoneData phoneData = new PhoneData(formattedNumber, type, label, isPrimary);
        this.mPhoneList.add(phoneData);
    }

    private void addSip(String sipData, int type, String label, boolean isPrimary) {
        if (this.mSipList == null) {
            this.mSipList = new ArrayList();
        }
        this.mSipList.add(new SipData(sipData, type, label, isPrimary));
    }

    private void addNickName(String nickName) {
        if (this.mNicknameList == null) {
            this.mNicknameList = new ArrayList();
        }
        this.mNicknameList.add(new NicknameData(nickName));
    }

    private void addEmail(int type, String data, String label, boolean isPrimary) {
        if (this.mEmailList == null) {
            this.mEmailList = new ArrayList();
        }
        this.mEmailList.add(new EmailData(data, type, label, isPrimary));
    }

    private void addPostal(int type, List<String> propValueList, String label, boolean isPrimary) {
        if (this.mPostalList == null) {
            this.mPostalList = new ArrayList(0);
        }
        this.mPostalList.add(PostalData.constructPostalData(propValueList, type, label, isPrimary, this.mVCardType));
    }

    private void addNewOrganization(String organizationName, String departmentName, String titleName, String phoneticName, int type, boolean isPrimary) {
        if (this.mOrganizationList == null) {
            this.mOrganizationList = new ArrayList();
        }
        this.mOrganizationList.add(new OrganizationData(organizationName, departmentName, titleName, phoneticName, type, isPrimary));
    }

    private String buildSinglePhoneticNameFromSortAsParam(Map<String, Collection<String>> paramMap) {
        Collection<String> sortAsCollection = paramMap.get("SORT-AS");
        if (sortAsCollection == null || sortAsCollection.size() == 0) {
            return null;
        }
        if (sortAsCollection.size() > 1) {
            Log.w("vCard", "Incorrect multiple SORT_AS parameters detected: " + Arrays.toString(sortAsCollection.toArray()));
        }
        List<String> sortNames = VCardUtils.constructListFromValue(sortAsCollection.iterator().next(), this.mVCardType);
        StringBuilder builder = new StringBuilder();
        for (String elem : sortNames) {
            builder.append(elem);
        }
        return builder.toString();
    }

    private void handleOrgValue(int type, List<String> orgList, Map<String, Collection<String>> paramMap, boolean isPrimary) {
        String organizationName;
        String departmentName;
        String phoneticName = buildSinglePhoneticNameFromSortAsParam(paramMap);
        if (orgList == null) {
            orgList = sEmptyList;
        }
        int size = orgList.size();
        switch (size) {
            case 0:
                organizationName = "";
                departmentName = null;
                break;
            case 1:
                organizationName = orgList.get(0);
                departmentName = null;
                break;
            default:
                organizationName = orgList.get(0);
                StringBuilder builder = new StringBuilder();
                for (int i = 1; i < size; i++) {
                    if (i > 1) {
                        builder.append(' ');
                    }
                    builder.append(orgList.get(i));
                }
                departmentName = builder.toString();
                break;
        }
        if (this.mOrganizationList == null) {
            addNewOrganization(organizationName, departmentName, null, phoneticName, type, isPrimary);
            return;
        }
        for (OrganizationData organizationData : this.mOrganizationList) {
            if (organizationData.mOrganizationName == null && organizationData.mDepartmentName == null) {
                organizationData.mOrganizationName = organizationName;
                organizationData.mDepartmentName = departmentName;
                organizationData.mIsPrimary = isPrimary;
                return;
            }
        }
        addNewOrganization(organizationName, departmentName, null, phoneticName, type, isPrimary);
    }

    private void handleTitleValue(String title) {
        if (this.mOrganizationList == null) {
            addNewOrganization(null, null, title, null, 1, false);
            return;
        }
        for (OrganizationData organizationData : this.mOrganizationList) {
            if (organizationData.mTitle == null) {
                organizationData.mTitle = title;
                return;
            }
        }
        addNewOrganization(null, null, title, null, 1, false);
    }

    private void addIm(int protocol, String customProtocol, String propValue, int type, boolean isPrimary) {
        if (this.mImList == null) {
            this.mImList = new ArrayList();
        }
        this.mImList.add(new ImData(protocol, customProtocol, propValue, type, isPrimary));
    }

    private void addNote(String note) {
        if (this.mNoteList == null) {
            this.mNoteList = new ArrayList(1);
        }
        this.mNoteList.add(new NoteData(note));
    }

    private void addPhotoBytes(String formatName, byte[] photoBytes, boolean isPrimary) {
        if (this.mPhotoList == null) {
            this.mPhotoList = new ArrayList(1);
        }
        PhotoData photoData = new PhotoData(formatName, photoBytes, isPrimary);
        this.mPhotoList.add(photoData);
    }

    private void tryHandleSortAsName(Map<String, Collection<String>> paramMap) {
        Collection<String> sortAsCollection;
        if ((!VCardConfig.isVersion30(this.mVCardType) || (TextUtils.isEmpty(this.mNameData.mPhoneticFamily) && TextUtils.isEmpty(this.mNameData.mPhoneticMiddle) && TextUtils.isEmpty(this.mNameData.mPhoneticGiven))) && (sortAsCollection = paramMap.get("SORT-AS")) != null && sortAsCollection.size() != 0) {
            if (sortAsCollection.size() > 1) {
                Log.w("vCard", "Incorrect multiple SORT_AS parameters detected: " + Arrays.toString(sortAsCollection.toArray()));
            }
            List<String> sortNames = VCardUtils.constructListFromValue(sortAsCollection.iterator().next(), this.mVCardType);
            int size = sortNames.size();
            if (size > 3) {
                size = 3;
            }
            switch (size) {
                case 3:
                    this.mNameData.mPhoneticMiddle = sortNames.get(2);
                case 2:
                    this.mNameData.mPhoneticGiven = sortNames.get(1);
                    break;
            }
            this.mNameData.mPhoneticFamily = sortNames.get(0);
        }
    }

    private void handleNProperty(List<String> paramValues, Map<String, Collection<String>> paramMap) {
        int size;
        tryHandleSortAsName(paramMap);
        if (paramValues != null && (size = paramValues.size()) >= 1) {
            if (size > 5) {
                size = 5;
            }
            switch (size) {
                case 5:
                    this.mNameData.mSuffix = paramValues.get(4);
                case 4:
                    this.mNameData.mPrefix = paramValues.get(3);
                case 3:
                    this.mNameData.mMiddle = paramValues.get(2);
                case 2:
                    this.mNameData.mGiven = paramValues.get(1);
                    break;
            }
            this.mNameData.mFamily = paramValues.get(0);
        }
    }

    private void handlePhoneticNameFromSound(List<String> elems) {
        int size;
        if (TextUtils.isEmpty(this.mNameData.mPhoneticFamily) && TextUtils.isEmpty(this.mNameData.mPhoneticMiddle) && TextUtils.isEmpty(this.mNameData.mPhoneticGiven) && elems != null && (size = elems.size()) >= 1) {
            if (size > 3) {
                size = 3;
            }
            if (elems.get(0).length() > 0) {
                boolean onlyFirstElemIsNonEmpty = true;
                int i = 1;
                while (true) {
                    if (i >= size) {
                        break;
                    }
                    if (elems.get(i).length() <= 0) {
                        i++;
                    } else {
                        onlyFirstElemIsNonEmpty = false;
                        break;
                    }
                }
                if (onlyFirstElemIsNonEmpty) {
                    String[] namesArray = elems.get(0).split(" ");
                    int nameArrayLength = namesArray.length;
                    if (nameArrayLength == 3) {
                        this.mNameData.mPhoneticFamily = namesArray[0];
                        this.mNameData.mPhoneticMiddle = namesArray[1];
                        this.mNameData.mPhoneticGiven = namesArray[2];
                        return;
                    } else if (nameArrayLength == 2) {
                        this.mNameData.mPhoneticFamily = namesArray[0];
                        this.mNameData.mPhoneticGiven = namesArray[1];
                        return;
                    } else {
                        this.mNameData.mPhoneticGiven = elems.get(0);
                        return;
                    }
                }
            }
            switch (size) {
                case 3:
                    this.mNameData.mPhoneticMiddle = elems.get(2);
                case 2:
                    this.mNameData.mPhoneticGiven = elems.get(1);
                    break;
            }
            this.mNameData.mPhoneticFamily = elems.get(0);
        }
    }

    public void addProperty(VCardProperty property) {
        boolean isPrimary;
        int type;
        String label;
        boolean isPrimary2;
        String propertyName = property.getName();
        Map<String, Collection<String>> paramMap = property.getParameterMap();
        List<String> propertyValueList = property.getValueList();
        byte[] propertyBytes = property.getByteValue();
        if ((propertyValueList != null && propertyValueList.size() != 0) || propertyBytes != null) {
            String propValue = propertyValueList != null ? listToString(propertyValueList).trim() : null;
            if (!propertyName.equals("VERSION")) {
                if (propertyName.equals("FN")) {
                    this.mNameData.mFormatted = propValue;
                    return;
                }
                if (propertyName.equals("NAME")) {
                    if (TextUtils.isEmpty(this.mNameData.mFormatted)) {
                        this.mNameData.mFormatted = propValue;
                        return;
                    }
                    return;
                }
                if (propertyName.equals("N")) {
                    handleNProperty(propertyValueList, paramMap);
                    return;
                }
                if (propertyName.equals("SORT-STRING")) {
                    this.mNameData.mSortString = propValue;
                    return;
                }
                if (propertyName.equals("NICKNAME") || propertyName.equals("X-NICKNAME")) {
                    addNickName(propValue);
                    return;
                }
                if (propertyName.equals("SOUND")) {
                    Collection<String> typeCollection = paramMap.get("TYPE");
                    if (typeCollection != null && typeCollection.contains("X-IRMC-N")) {
                        List<String> phoneticNameList = VCardUtils.constructListFromValue(propValue, this.mVCardType);
                        handlePhoneticNameFromSound(phoneticNameList);
                        return;
                    }
                    return;
                }
                if (propertyName.equals("ADR")) {
                    boolean valuesAreAllEmpty = true;
                    Iterator<String> it = propertyValueList.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        String value = it.next();
                        if (!TextUtils.isEmpty(value)) {
                            valuesAreAllEmpty = false;
                            break;
                        }
                    }
                    if (!valuesAreAllEmpty) {
                        int type2 = -1;
                        String label2 = null;
                        boolean isPrimary3 = false;
                        Collection<String> typeCollection2 = paramMap.get("TYPE");
                        if (typeCollection2 != null) {
                            for (String typeStringOrg : typeCollection2) {
                                String typeStringUpperCase = typeStringOrg.toUpperCase();
                                if (typeStringUpperCase.equals("PREF")) {
                                    isPrimary3 = true;
                                } else if (typeStringUpperCase.equals("HOME")) {
                                    type2 = 1;
                                    label2 = null;
                                } else if (typeStringUpperCase.equals("WORK") || typeStringUpperCase.equalsIgnoreCase("COMPANY")) {
                                    type2 = 2;
                                    label2 = null;
                                } else if (!typeStringUpperCase.equals("PARCEL") && !typeStringUpperCase.equals("DOM") && !typeStringUpperCase.equals("INTL") && type2 < 0) {
                                    type2 = 0;
                                    if (typeStringUpperCase.startsWith("X-")) {
                                        label2 = typeStringOrg.substring(2);
                                    } else {
                                        label2 = typeStringOrg;
                                    }
                                }
                            }
                        }
                        if (type2 < 0) {
                            type2 = 1;
                        }
                        addPostal(type2, propertyValueList, label2, isPrimary3);
                        return;
                    }
                    return;
                }
                if (propertyName.equals("EMAIL")) {
                    int type3 = -1;
                    String label3 = null;
                    boolean isPrimary4 = false;
                    Collection<String> typeCollection3 = paramMap.get("TYPE");
                    if (typeCollection3 != null) {
                        for (String typeStringOrg2 : typeCollection3) {
                            String typeStringUpperCase2 = typeStringOrg2.toUpperCase();
                            if (typeStringUpperCase2.equals("PREF")) {
                                isPrimary4 = true;
                            } else if (typeStringUpperCase2.equals("HOME")) {
                                type3 = 1;
                            } else if (typeStringUpperCase2.equals("WORK")) {
                                type3 = 2;
                            } else if (typeStringUpperCase2.equals("CELL")) {
                                type3 = 4;
                            } else if (type3 < 0) {
                                if (typeStringUpperCase2.startsWith("X-")) {
                                    label3 = typeStringOrg2.substring(2);
                                } else {
                                    label3 = typeStringOrg2;
                                }
                                type3 = 0;
                            }
                        }
                    }
                    if (type3 < 0) {
                        type3 = 3;
                    }
                    addEmail(type3, propValue, label3, isPrimary4);
                    return;
                }
                if (propertyName.equals("ORG")) {
                    boolean isPrimary5 = false;
                    Collection<String> typeCollection4 = paramMap.get("TYPE");
                    if (typeCollection4 != null) {
                        Iterator<String> it2 = typeCollection4.iterator();
                        while (it2.hasNext()) {
                            if (it2.next().equals("PREF")) {
                                isPrimary5 = true;
                            }
                        }
                    }
                    handleOrgValue(1, propertyValueList, paramMap, isPrimary5);
                    return;
                }
                if (propertyName.equals("TITLE")) {
                    handleTitleValue(propValue);
                    return;
                }
                if (!propertyName.equals("ROLE")) {
                    if (propertyName.equals("PHOTO") || propertyName.equals("LOGO")) {
                        Collection<String> paramMapValue = paramMap.get("VALUE");
                        if (paramMapValue == null || !paramMapValue.contains("URL")) {
                            Collection<String> typeCollection5 = paramMap.get("TYPE");
                            String formatName = null;
                            boolean isPrimary6 = false;
                            if (typeCollection5 != null) {
                                for (String typeValue : typeCollection5) {
                                    if ("PREF".equals(typeValue)) {
                                        isPrimary6 = true;
                                    } else if (formatName == null) {
                                        formatName = typeValue;
                                    }
                                }
                            }
                            addPhotoBytes(formatName, propertyBytes, isPrimary6);
                            return;
                        }
                        return;
                    }
                    if (propertyName.equals("TEL")) {
                        String phoneNumber = null;
                        boolean isSip = false;
                        if (VCardConfig.isVersion40(this.mVCardType)) {
                            if (propValue.startsWith("sip:")) {
                                isSip = true;
                            } else if (propValue.startsWith("tel:")) {
                                phoneNumber = propValue.substring(4);
                            } else {
                                phoneNumber = propValue;
                            }
                        } else {
                            phoneNumber = propValue;
                        }
                        if (isSip) {
                            handleSipCase(propValue, paramMap.get("TYPE"));
                            return;
                        }
                        if (propValue.length() != 0) {
                            Collection<String> typeCollection6 = paramMap.get("TYPE");
                            Object typeObject = VCardUtils.getPhoneTypeFromStrings(typeCollection6, phoneNumber);
                            if (typeObject instanceof Integer) {
                                type = ((Integer) typeObject).intValue();
                                label = null;
                            } else {
                                type = 0;
                                label = typeObject.toString();
                            }
                            if (typeCollection6 != null && typeCollection6.contains("PREF")) {
                                isPrimary2 = true;
                            } else {
                                isPrimary2 = false;
                            }
                            addPhone(type, phoneNumber, label, isPrimary2);
                            return;
                        }
                        return;
                    }
                    if (propertyName.equals("X-SKYPE-PSTNNUMBER")) {
                        Collection<String> typeCollection7 = paramMap.get("TYPE");
                        if (typeCollection7 != null && typeCollection7.contains("PREF")) {
                            isPrimary = true;
                        } else {
                            isPrimary = false;
                        }
                        addPhone(7, propValue, null, isPrimary);
                        return;
                    }
                    if (sImMap.containsKey(propertyName)) {
                        int protocol = sImMap.get(propertyName).intValue();
                        boolean isPrimary7 = false;
                        int type4 = -1;
                        Collection<String> typeCollection8 = paramMap.get("TYPE");
                        if (typeCollection8 != null) {
                            for (String typeString : typeCollection8) {
                                if (typeString.equals("PREF")) {
                                    isPrimary7 = true;
                                } else if (type4 < 0) {
                                    if (typeString.equalsIgnoreCase("HOME")) {
                                        type4 = 1;
                                    } else if (typeString.equalsIgnoreCase("WORK")) {
                                        type4 = 2;
                                    }
                                }
                            }
                        }
                        if (type4 < 0) {
                            type4 = 1;
                        }
                        addIm(protocol, null, propValue, type4, isPrimary7);
                        return;
                    }
                    if (propertyName.equals("NOTE")) {
                        addNote(propValue);
                        return;
                    }
                    if (propertyName.equals("URL")) {
                        if (this.mWebsiteList == null) {
                            this.mWebsiteList = new ArrayList(1);
                        }
                        this.mWebsiteList.add(new WebsiteData(propValue));
                        return;
                    }
                    if (propertyName.equals("BDAY")) {
                        this.mBirthday = new BirthdayData(propValue);
                        return;
                    }
                    if (propertyName.equals("ANNIVERSARY")) {
                        this.mAnniversary = new AnniversaryData(propValue);
                        return;
                    }
                    if (propertyName.equals("X-PHONETIC-FIRST-NAME")) {
                        this.mNameData.mPhoneticGiven = propValue;
                        return;
                    }
                    if (propertyName.equals("X-PHONETIC-MIDDLE-NAME")) {
                        this.mNameData.mPhoneticMiddle = propValue;
                        return;
                    }
                    if (propertyName.equals("X-PHONETIC-LAST-NAME")) {
                        this.mNameData.mPhoneticFamily = propValue;
                        return;
                    }
                    if (propertyName.equals("IMPP")) {
                        if (propValue.startsWith("sip:")) {
                            handleSipCase(propValue, paramMap.get("TYPE"));
                        }
                    } else if (propertyName.equals("X-SIP")) {
                        if (!TextUtils.isEmpty(propValue)) {
                            handleSipCase(propValue, paramMap.get("TYPE"));
                        }
                    } else if (propertyName.equals("X-ANDROID-CUSTOM")) {
                        List<String> customPropertyList = VCardUtils.constructListFromValue(propValue, this.mVCardType);
                        handleAndroidCustomProperty(customPropertyList);
                    }
                }
            }
        }
    }

    private void handleSipCase(String propValue, Collection<String> typeCollection) {
        if (!TextUtils.isEmpty(propValue)) {
            if (propValue.startsWith("sip:")) {
                propValue = propValue.substring(4);
                if (propValue.length() == 0) {
                    return;
                }
            }
            int type = -1;
            String label = null;
            boolean isPrimary = false;
            if (typeCollection != null) {
                for (String typeStringOrg : typeCollection) {
                    String typeStringUpperCase = typeStringOrg.toUpperCase();
                    if (typeStringUpperCase.equals("PREF")) {
                        isPrimary = true;
                    } else if (typeStringUpperCase.equals("HOME")) {
                        type = 1;
                    } else if (typeStringUpperCase.equals("WORK")) {
                        type = 2;
                    } else if (type < 0) {
                        if (typeStringUpperCase.startsWith("X-")) {
                            label = typeStringOrg.substring(2);
                        } else {
                            label = typeStringOrg;
                        }
                        type = 0;
                    }
                }
            }
            if (type < 0) {
                type = 3;
            }
            addSip(propValue, type, label, isPrimary);
        }
    }

    public void addChild(VCardEntry child) {
        if (this.mChildren == null) {
            this.mChildren = new ArrayList();
        }
        this.mChildren.add(child);
    }

    private void handleAndroidCustomProperty(List<String> customPropertyList) {
        if (this.mAndroidCustomDataList == null) {
            this.mAndroidCustomDataList = new ArrayList();
        }
        this.mAndroidCustomDataList.add(AndroidCustomData.constructAndroidCustomData(customPropertyList));
    }

    private String constructDisplayName() {
        String displayName = null;
        if (!TextUtils.isEmpty(this.mNameData.mFormatted)) {
            displayName = this.mNameData.mFormatted;
        } else if (!this.mNameData.emptyStructuredName()) {
            displayName = VCardUtils.constructNameFromElements(this.mVCardType, this.mNameData.mFamily, this.mNameData.mMiddle, this.mNameData.mGiven, this.mNameData.mPrefix, this.mNameData.mSuffix);
        } else if (!this.mNameData.emptyPhoneticStructuredName()) {
            displayName = VCardUtils.constructNameFromElements(this.mVCardType, this.mNameData.mPhoneticFamily, this.mNameData.mPhoneticMiddle, this.mNameData.mPhoneticGiven);
        } else if (this.mEmailList != null && this.mEmailList.size() > 0) {
            displayName = this.mEmailList.get(0).mAddress;
        } else if (this.mPhoneList != null && this.mPhoneList.size() > 0) {
            displayName = this.mPhoneList.get(0).mNumber;
        } else if (this.mPostalList != null && this.mPostalList.size() > 0) {
            displayName = this.mPostalList.get(0).getFormattedAddress(this.mVCardType);
        } else if (this.mOrganizationList != null && this.mOrganizationList.size() > 0) {
            displayName = this.mOrganizationList.get(0).getFormattedString();
        }
        if (displayName == null) {
            return "";
        }
        return displayName;
    }

    public void consolidateFields() {
        this.mNameData.displayName = constructDisplayName();
    }

    public boolean isIgnorable() {
        IsIgnorableIterator iterator = new IsIgnorableIterator();
        iterateAllData(iterator);
        return iterator.getResult();
    }

    public ArrayList<ContentProviderOperation> constructInsertOperations(ContentResolver resolver, ArrayList<ContentProviderOperation> operationList) {
        if (operationList == null) {
            operationList = new ArrayList<>();
        }
        if (!isIgnorable()) {
            int backReferenceIndex = operationList.size();
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI);
            if (this.mAccount != null) {
                builder.withValue("account_name", this.mAccount.name);
                builder.withValue("account_type", this.mAccount.type);
            } else {
                builder.withValue("account_name", null);
                builder.withValue("account_type", null);
            }
            operationList.add(builder.build());
            operationList.size();
            iterateAllData(new InsertOperationConstrutor(operationList, backReferenceIndex));
            operationList.size();
        }
        return operationList;
    }

    private String listToString(List<String> list) {
        int size = list.size();
        if (size > 1) {
            StringBuilder builder = new StringBuilder();
            for (String type : list) {
                builder.append(type);
                if (0 < size - 1) {
                    builder.append(";");
                }
            }
            return builder.toString();
        }
        if (size == 1) {
            return list.get(0);
        }
        return "";
    }

    public String getDisplayName() {
        if (this.mNameData.displayName == null) {
            this.mNameData.displayName = constructDisplayName();
        }
        return this.mNameData.displayName;
    }
}
