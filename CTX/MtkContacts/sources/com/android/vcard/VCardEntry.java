package com.android.vcard;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.contacts.compat.CompatUtils;
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
    private List<Pair<String, String>> mUnknownXData;
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
        sImMap.put("X-QQ", 4);
        sImMap.put("X-CUSTOM-IM", -1);
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
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/name");
            if (!TextUtils.isEmpty(this.mGiven)) {
                builderNewInsert.withValue("data2", this.mGiven);
            }
            if (!TextUtils.isEmpty(this.mFamily)) {
                builderNewInsert.withValue("data3", this.mFamily);
            }
            if (!TextUtils.isEmpty(this.mMiddle)) {
                builderNewInsert.withValue("data5", this.mMiddle);
            }
            if (!TextUtils.isEmpty(this.mPrefix)) {
                builderNewInsert.withValue("data4", this.mPrefix);
            }
            if (!TextUtils.isEmpty(this.mSuffix)) {
                builderNewInsert.withValue("data6", this.mSuffix);
            }
            boolean z = false;
            if (!TextUtils.isEmpty(this.mPhoneticGiven)) {
                builderNewInsert.withValue("data7", this.mPhoneticGiven);
                z = true;
            }
            if (!TextUtils.isEmpty(this.mPhoneticFamily)) {
                builderNewInsert.withValue("data9", this.mPhoneticFamily);
                z = true;
            }
            if (!TextUtils.isEmpty(this.mPhoneticMiddle)) {
                builderNewInsert.withValue("data8", this.mPhoneticMiddle);
                z = true;
            }
            if (!z) {
                builderNewInsert.withValue("data7", this.mSortString);
            }
            builderNewInsert.withValue("data1", this.displayName);
            list.add(builderNewInsert.build());
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
            String[] strArr = {this.mFamily, this.mMiddle, this.mGiven, this.mPrefix, this.mSuffix, this.mFormatted, this.mPhoneticFamily, this.mPhoneticMiddle, this.mPhoneticGiven, this.mSortString};
            int length = strArr.length;
            int iHashCode = 0;
            for (int i = 0; i < length; i++) {
                String str = strArr[i];
                iHashCode = (iHashCode * 31) + (str != null ? str.hashCode() : 0);
            }
            return iHashCode;
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

        public PhoneData(String str, int i, String str2, boolean z) {
            this.mNumber = str;
            this.mType = i;
            this.mLabel = str2;
            this.mIsPrimary = z;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
            builderNewInsert.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builderNewInsert.withValue("data3", this.mLabel);
            }
            builderNewInsert.withValue("data1", this.mNumber);
            if (this.mIsPrimary) {
                builderNewInsert.withValue("is_primary", 1);
            }
            list.add(builderNewInsert.build());
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
            return (((((this.mType * 31) + (this.mNumber != null ? this.mNumber.hashCode() : 0)) * 31) + (this.mLabel != null ? this.mLabel.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
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

        public EmailData(String str, int i, String str2, boolean z) {
            this.mType = i;
            this.mAddress = str;
            this.mLabel = str2;
            this.mIsPrimary = z;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/email_v2");
            builderNewInsert.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builderNewInsert.withValue("data3", this.mLabel);
            }
            builderNewInsert.withValue("data1", this.mAddress);
            if (this.mIsPrimary) {
                builderNewInsert.withValue("is_primary", 1);
            }
            list.add(builderNewInsert.build());
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
            return (((((this.mType * 31) + (this.mAddress != null ? this.mAddress.hashCode() : 0)) * 31) + (this.mLabel != null ? this.mLabel.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
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

        public PostalData(String str, String str2, String str3, String str4, String str5, String str6, String str7, int i, String str8, boolean z, int i2) {
            this.mType = i;
            this.mPobox = str;
            this.mExtendedAddress = str2;
            this.mStreet = str3;
            this.mLocalty = str4;
            this.mRegion = str5;
            this.mPostalCode = str6;
            this.mCountry = str7;
            this.mLabel = str8;
            this.mIsPrimary = z;
            this.mVCardType = i2;
        }

        public static PostalData constructPostalData(List<String> list, int i, String str, boolean z, int i2) {
            String[] strArr = new String[7];
            int size = list.size();
            if (size > 7) {
                size = 7;
            }
            Iterator<String> it = list.iterator();
            int i3 = 0;
            while (it.hasNext()) {
                strArr[i3] = it.next();
                i3++;
                if (i3 >= size) {
                    break;
                }
            }
            while (i3 < 7) {
                strArr[i3] = null;
                i3++;
            }
            return new PostalData(strArr[0], strArr[1], strArr[2], strArr[3], strArr[4], strArr[5], strArr[6], i, str, z, i2);
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/postal-address_v2");
            builderNewInsert.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builderNewInsert.withValue("data3", this.mLabel);
            }
            builderNewInsert.withValue("data5", this.mPobox);
            builderNewInsert.withValue("data6", this.mExtendedAddress);
            builderNewInsert.withValue("data4", this.mStreet);
            builderNewInsert.withValue("data7", this.mLocalty);
            builderNewInsert.withValue("data8", this.mRegion);
            builderNewInsert.withValue("data9", this.mPostalCode);
            builderNewInsert.withValue("data10", this.mCountry);
            if (this.mIsPrimary) {
                builderNewInsert.withValue("is_primary", 1);
            }
            list.add(builderNewInsert.build());
        }

        public String getFormattedAddress(int i) {
            StringBuilder sb = new StringBuilder();
            boolean z = true;
            String[] strArr = {this.mPobox, this.mExtendedAddress, this.mStreet, this.mLocalty, this.mRegion, this.mPostalCode, this.mCountry};
            if (VCardConfig.isJapaneseDevice(i)) {
                for (int i2 = 6; i2 >= 0; i2--) {
                    String str = strArr[i2];
                    if (!TextUtils.isEmpty(str)) {
                        if (!z) {
                            sb.append(' ');
                        } else {
                            z = false;
                        }
                        sb.append(str);
                    }
                }
            } else {
                for (int i3 = 0; i3 < 7; i3++) {
                    String str2 = strArr[i3];
                    if (!TextUtils.isEmpty(str2)) {
                        if (!z) {
                            sb.append(' ');
                        } else {
                            z = false;
                        }
                        sb.append(str2);
                    }
                }
            }
            return sb.toString().trim();
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
            int iHashCode = (((this.mType * 31) + (this.mLabel != null ? this.mLabel.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
            String[] strArr = {this.mPobox, this.mExtendedAddress, this.mStreet, this.mLocalty, this.mRegion, this.mPostalCode, this.mCountry};
            int length = strArr.length;
            int iHashCode2 = iHashCode;
            for (int i = 0; i < length; i++) {
                String str = strArr[i];
                iHashCode2 = (iHashCode2 * 31) + (str != null ? str.hashCode() : 0);
            }
            return iHashCode2;
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

        public OrganizationData(String str, String str2, String str3, String str4, int i, boolean z) {
            this.mType = i;
            this.mOrganizationName = str;
            this.mDepartmentName = str2;
            this.mTitle = str3;
            this.mPhoneticName = str4;
            this.mIsPrimary = z;
        }

        public String getFormattedString() {
            StringBuilder sb = new StringBuilder();
            if (!TextUtils.isEmpty(this.mOrganizationName)) {
                sb.append(this.mOrganizationName);
            }
            if (!TextUtils.isEmpty(this.mDepartmentName)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(this.mDepartmentName);
            }
            if (!TextUtils.isEmpty(this.mTitle)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(this.mTitle);
            }
            return sb.toString();
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/organization");
            builderNewInsert.withValue("data2", Integer.valueOf(this.mType));
            if (this.mOrganizationName != null) {
                builderNewInsert.withValue("data1", this.mOrganizationName);
            }
            if (this.mDepartmentName != null) {
                builderNewInsert.withValue("data5", this.mDepartmentName);
            }
            if (this.mTitle != null) {
                builderNewInsert.withValue("data4", this.mTitle);
            }
            if (this.mPhoneticName != null) {
                builderNewInsert.withValue("data8", this.mPhoneticName);
            }
            if (this.mIsPrimary) {
                builderNewInsert.withValue("is_primary", 1);
            }
            list.add(builderNewInsert.build());
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
            OrganizationData organizationData = (OrganizationData) obj;
            return this.mType == organizationData.mType && TextUtils.equals(this.mOrganizationName, organizationData.mOrganizationName) && TextUtils.equals(this.mDepartmentName, organizationData.mDepartmentName) && TextUtils.equals(this.mTitle, organizationData.mTitle) && this.mIsPrimary == organizationData.mIsPrimary;
        }

        public int hashCode() {
            return (((((((this.mType * 31) + (this.mOrganizationName != null ? this.mOrganizationName.hashCode() : 0)) * 31) + (this.mDepartmentName != null ? this.mDepartmentName.hashCode() : 0)) * 31) + (this.mTitle != null ? this.mTitle.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
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

        public ImData(int i, String str, String str2, int i2, boolean z) {
            this.mProtocol = i;
            this.mCustomProtocol = str;
            this.mType = i2;
            this.mAddress = str2;
            this.mIsPrimary = z;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/im");
            builderNewInsert.withValue("data2", Integer.valueOf(this.mType));
            builderNewInsert.withValue("data5", Integer.valueOf(this.mProtocol));
            builderNewInsert.withValue("data1", this.mAddress);
            if (this.mProtocol == -1) {
                builderNewInsert.withValue("data6", this.mCustomProtocol);
            }
            if (this.mIsPrimary) {
                builderNewInsert.withValue("is_primary", 1);
            }
            list.add(builderNewInsert.build());
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
            return (((((((this.mType * 31) + this.mProtocol) * 31) + (this.mCustomProtocol != null ? this.mCustomProtocol.hashCode() : 0)) * 31) + (this.mAddress != null ? this.mAddress.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
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

        public PhotoData(String str, byte[] bArr, boolean z) {
            this.mFormat = str;
            this.mBytes = bArr;
            this.mIsPrimary = z;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/photo");
            builderNewInsert.withValue("data15", this.mBytes);
            if (this.mIsPrimary) {
                builderNewInsert.withValue("is_primary", 1);
            }
            list.add(builderNewInsert.build());
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
            int iHashCode = (this.mFormat != null ? this.mFormat.hashCode() : 0) * 31;
            if (this.mBytes != null) {
                for (byte b : this.mBytes) {
                    iHashCode += b;
                }
            }
            int i = (iHashCode * 31) + (this.mIsPrimary ? 1231 : 1237);
            this.mHashCode = Integer.valueOf(i);
            return i;
        }

        public String toString() {
            return String.format("format: %s: size: %d, isPrimary: %s", this.mFormat, Integer.valueOf(this.mBytes.length), Boolean.valueOf(this.mIsPrimary));
        }

        @Override
        public final EntryLabel getEntryLabel() {
            return EntryLabel.PHOTO;
        }

        public byte[] getBytes() {
            return this.mBytes;
        }
    }

    public static class NicknameData implements EntryElement {
        private final String mNickname;

        public NicknameData(String str) {
            this.mNickname = str;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/nickname");
            builderNewInsert.withValue("data2", 1);
            builderNewInsert.withValue("data1", this.mNickname);
            list.add(builderNewInsert.build());
        }

        @Override
        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mNickname);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof NicknameData)) {
                return false;
            }
            return TextUtils.equals(this.mNickname, ((NicknameData) obj).mNickname);
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

        public NoteData(String str) {
            this.mNote = str;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/note");
            builderNewInsert.withValue("data1", this.mNote);
            list.add(builderNewInsert.build());
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
            return TextUtils.equals(this.mNote, ((NoteData) obj).mNote);
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

        public WebsiteData(String str) {
            this.mWebsite = str;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/website");
            builderNewInsert.withValue("data1", this.mWebsite);
            builderNewInsert.withValue("data2", 1);
            list.add(builderNewInsert.build());
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
            return TextUtils.equals(this.mWebsite, ((WebsiteData) obj).mWebsite);
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

        public BirthdayData(String str) {
            this.mBirthday = str;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/contact_event");
            builderNewInsert.withValue("data1", this.mBirthday);
            builderNewInsert.withValue("data2", 3);
            list.add(builderNewInsert.build());
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
            return TextUtils.equals(this.mBirthday, ((BirthdayData) obj).mBirthday);
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

        public AnniversaryData(String str) {
            this.mAnniversary = str;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/contact_event");
            builderNewInsert.withValue("data1", this.mAnniversary);
            builderNewInsert.withValue("data2", 1);
            list.add(builderNewInsert.build());
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
            return TextUtils.equals(this.mAnniversary, ((AnniversaryData) obj).mAnniversary);
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

        public SipData(String str, int i, String str2, boolean z) {
            if (str.startsWith("sip:")) {
                this.mAddress = str.substring(4);
            } else {
                this.mAddress = str;
            }
            this.mType = i;
            this.mLabel = str2;
            this.mIsPrimary = z;
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/sip_address");
            builderNewInsert.withValue("data1", this.mAddress);
            builderNewInsert.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builderNewInsert.withValue("data3", this.mLabel);
            }
            if (this.mIsPrimary) {
                builderNewInsert.withValue("is_primary", Boolean.valueOf(this.mIsPrimary));
            }
            list.add(builderNewInsert.build());
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
            return (((((this.mType * 31) + (this.mLabel != null ? this.mLabel.hashCode() : 0)) * 31) + (this.mAddress != null ? this.mAddress.hashCode() : 0)) * 31) + (this.mIsPrimary ? 1231 : 1237);
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

        public AndroidCustomData(String str, List<String> list) {
            this.mMimeType = str;
            this.mDataList = list;
        }

        public static AndroidCustomData constructAndroidCustomData(List<String> list) {
            List<String> listSubList;
            String str = null;
            if (list != null) {
                if (list.size() < 2) {
                    str = list.get(0);
                    listSubList = null;
                } else {
                    int size = list.size() < 16 ? list.size() : 16;
                    str = list.get(0);
                    listSubList = list.subList(1, size);
                }
            } else {
                listSubList = null;
            }
            return new AndroidCustomData(str, listSubList);
        }

        @Override
        public void constructInsertOperation(List<ContentProviderOperation> list, int i) {
            ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builderNewInsert.withValueBackReference("raw_contact_id", i);
            builderNewInsert.withValue("mimetype", this.mMimeType);
            for (int i2 = 0; i2 < this.mDataList.size(); i2++) {
                String str = this.mDataList.get(i2);
                if (!TextUtils.isEmpty(str)) {
                    builderNewInsert.withValue("data" + (i2 + 1), str);
                }
            }
            list.add(builderNewInsert.build());
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
            AndroidCustomData androidCustomData = (AndroidCustomData) obj;
            if (!TextUtils.equals(this.mMimeType, androidCustomData.mMimeType)) {
                return false;
            }
            if (this.mDataList == null) {
                return androidCustomData.mDataList == null;
            }
            int size = this.mDataList.size();
            if (size != androidCustomData.mDataList.size()) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                if (!TextUtils.equals(this.mDataList.get(i), androidCustomData.mDataList.get(i))) {
                    return false;
                }
            }
            return true;
        }

        public int hashCode() {
            int iHashCode = this.mMimeType != null ? this.mMimeType.hashCode() : 0;
            if (this.mDataList != null) {
                Iterator<String> it = this.mDataList.iterator();
                while (it.hasNext()) {
                    String next = it.next();
                    iHashCode = (iHashCode * 31) + (next != null ? next.hashCode() : 0);
                }
            }
            return iHashCode;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("android-custom: " + this.mMimeType + ", data: ");
            sb.append(this.mDataList == null ? "null" : Arrays.toString(this.mDataList.toArray()));
            return sb.toString();
        }

        @Override
        public EntryLabel getEntryLabel() {
            return EntryLabel.ANDROID_CUSTOM;
        }
    }

    public final void iterateAllData(EntryElementIterator entryElementIterator) {
        entryElementIterator.onIterationStarted();
        entryElementIterator.onElementGroupStarted(this.mNameData.getEntryLabel());
        entryElementIterator.onElement(this.mNameData);
        entryElementIterator.onElementGroupEnded();
        iterateOneList(this.mPhoneList, entryElementIterator);
        iterateOneList(this.mEmailList, entryElementIterator);
        iterateOneList(this.mPostalList, entryElementIterator);
        iterateOneList(this.mOrganizationList, entryElementIterator);
        iterateOneList(this.mImList, entryElementIterator);
        iterateOneList(this.mPhotoList, entryElementIterator);
        iterateOneList(this.mWebsiteList, entryElementIterator);
        iterateOneList(this.mSipList, entryElementIterator);
        iterateOneList(this.mNicknameList, entryElementIterator);
        iterateOneList(this.mNoteList, entryElementIterator);
        iterateOneList(this.mAndroidCustomDataList, entryElementIterator);
        if (this.mBirthday != null) {
            entryElementIterator.onElementGroupStarted(this.mBirthday.getEntryLabel());
            entryElementIterator.onElement(this.mBirthday);
            entryElementIterator.onElementGroupEnded();
        }
        if (this.mAnniversary != null) {
            entryElementIterator.onElementGroupStarted(this.mAnniversary.getEntryLabel());
            entryElementIterator.onElement(this.mAnniversary);
            entryElementIterator.onElementGroupEnded();
        }
        entryElementIterator.onIterationEnded();
    }

    private void iterateOneList(List<? extends EntryElement> list, EntryElementIterator entryElementIterator) {
        if (list != null && list.size() > 0) {
            entryElementIterator.onElementGroupStarted(list.get(0).getEntryLabel());
            Iterator<? extends EntryElement> it = list.iterator();
            while (it.hasNext()) {
                entryElementIterator.onElement(it.next());
            }
            entryElementIterator.onElementGroupEnded();
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
        public void onElementGroupStarted(EntryLabel entryLabel) {
        }

        @Override
        public void onElementGroupEnded() {
        }

        @Override
        public boolean onElement(EntryElement entryElement) {
            if (!entryElement.isEmpty()) {
                this.mEmpty = false;
                return false;
            }
            return true;
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
        public void onElementGroupStarted(EntryLabel entryLabel) {
            this.mBuilder.append(entryLabel.toString() + ": ");
            this.mFirstElement = true;
        }

        @Override
        public boolean onElement(EntryElement entryElement) {
            if (!this.mFirstElement) {
                this.mBuilder.append(", ");
                this.mFirstElement = false;
            }
            StringBuilder sb = this.mBuilder;
            sb.append("[");
            sb.append(entryElement.toString());
            sb.append("]");
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

        public InsertOperationConstrutor(List<ContentProviderOperation> list, int i) {
            this.mOperationList = list;
            this.mBackReferenceIndex = i;
        }

        @Override
        public void onIterationStarted() {
        }

        @Override
        public void onIterationEnded() {
        }

        @Override
        public void onElementGroupStarted(EntryLabel entryLabel) {
        }

        @Override
        public void onElementGroupEnded() {
        }

        @Override
        public boolean onElement(EntryElement entryElement) {
            if (!entryElement.isEmpty()) {
                entryElement.constructInsertOperation(this.mOperationList, this.mBackReferenceIndex);
                return true;
            }
            return true;
        }
    }

    public String toString() {
        ToStringIterator toStringIterator = new ToStringIterator();
        iterateAllData(toStringIterator);
        return toStringIterator.toString();
    }

    public VCardEntry() {
        this(-1073741824);
    }

    public VCardEntry(int i) {
        this(i, null);
    }

    public VCardEntry(int i, Account account) {
        this.mNameData = new NameData();
        this.mVCardType = i;
        this.mAccount = account;
    }

    private void addPhone(int i, String str, String str2, boolean z) {
        if (this.mPhoneList == null) {
            this.mPhoneList = new ArrayList();
        }
        StringBuilder sb = new StringBuilder();
        String strTrim = str.trim();
        if (i != 6 && !VCardConfig.refrainPhoneNumberFormatting(this.mVCardType)) {
            int length = strTrim.length();
            boolean z2 = false;
            for (int i2 = 0; i2 < length; i2++) {
                char cCharAt = strTrim.charAt(i2);
                if (cCharAt == 'p' || cCharAt == 'P' || cCharAt == 'w' || cCharAt == 'W') {
                    sb.append(cCharAt);
                    z2 = true;
                } else {
                    if (PhoneNumberUtils.is12Key(cCharAt) || ((i2 == 0 && cCharAt == '+') || cCharAt == ' ' || cCharAt == ';' || cCharAt == ',' || cCharAt == '-' || cCharAt == '/' || cCharAt == '*' || cCharAt == '#' || cCharAt == '.')) {
                        sb.append(cCharAt);
                    }
                }
            }
            if (!z2) {
                VCardUtils.getPhoneNumberFormat(this.mVCardType);
                strTrim = VCardUtils.PhoneNumberUtilsPort.formatNumber(sb.toString(), 0);
            } else {
                strTrim = sb.toString();
            }
        }
        this.mPhoneList.add(new PhoneData(strTrim, i, str2, z));
    }

    private void addSip(String str, int i, String str2, boolean z) {
        if (this.mSipList == null) {
            this.mSipList = new ArrayList();
        }
        this.mSipList.add(new SipData(str, i, str2, z));
    }

    private void addNickName(String str) {
        if (this.mNicknameList == null) {
            this.mNicknameList = new ArrayList();
        }
        this.mNicknameList.add(new NicknameData(str));
    }

    private void addEmail(int i, String str, String str2, boolean z) {
        if (this.mEmailList == null) {
            this.mEmailList = new ArrayList();
        }
        this.mEmailList.add(new EmailData(str, i, str2, z));
    }

    private void addPostal(int i, List<String> list, String str, boolean z) {
        if (this.mPostalList == null) {
            this.mPostalList = new ArrayList(0);
        }
        this.mPostalList.add(PostalData.constructPostalData(list, i, str, z, this.mVCardType));
    }

    private void addNewOrganization(String str, String str2, String str3, String str4, int i, boolean z) {
        if (this.mOrganizationList == null) {
            this.mOrganizationList = new ArrayList();
        }
        this.mOrganizationList.add(new OrganizationData(str, str2, str3, str4, i, z));
    }

    private String buildSinglePhoneticNameFromSortAsParam(Map<String, Collection<String>> map) {
        Collection<String> collection = map.get("SORT-AS");
        if (collection != null && collection.size() != 0) {
            if (collection.size() > 1) {
                Log.w("MTK_vCard", "Incorrect multiple SORT_AS parameters detected: " + Arrays.toString(collection.toArray()));
            }
            List<String> listConstructListFromValue = VCardUtils.constructListFromValue(collection.iterator().next(), this.mVCardType);
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = listConstructListFromValue.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
            }
            return sb.toString();
        }
        return null;
    }

    private void handleOrgValue(int i, List<String> list, Map<String, Collection<String>> map, boolean z) {
        String str;
        String str2;
        String string;
        String strBuildSinglePhoneticNameFromSortAsParam = buildSinglePhoneticNameFromSortAsParam(map);
        if (list == null) {
            list = sEmptyList;
        }
        int size = list.size();
        switch (size) {
            case 0:
                str = "";
                str2 = str;
                string = null;
                break;
            case 1:
                str = list.get(0);
                str2 = str;
                string = null;
                break;
            default:
                String str3 = list.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i2 = 1; i2 < size; i2++) {
                    if (i2 > 1) {
                        sb.append(' ');
                    }
                    sb.append(list.get(i2));
                }
                string = sb.toString();
                str2 = str3;
                break;
        }
        if (this.mOrganizationList == null) {
            addNewOrganization(str2, string, null, strBuildSinglePhoneticNameFromSortAsParam, i, z);
            return;
        }
        for (OrganizationData organizationData : this.mOrganizationList) {
            if (organizationData.mOrganizationName == null && organizationData.mDepartmentName == null) {
                organizationData.mOrganizationName = str2;
                organizationData.mDepartmentName = string;
                organizationData.mIsPrimary = z;
                return;
            }
        }
        addNewOrganization(str2, string, null, strBuildSinglePhoneticNameFromSortAsParam, i, z);
    }

    private void handleTitleValue(String str) {
        if (this.mOrganizationList == null) {
            addNewOrganization(null, null, str, null, 1, false);
            return;
        }
        for (OrganizationData organizationData : this.mOrganizationList) {
            if (organizationData.mTitle == null) {
                organizationData.mTitle = str;
                return;
            }
        }
        addNewOrganization(null, null, str, null, 1, false);
    }

    private void addIm(int i, String str, String str2, int i2, boolean z) {
        if (this.mImList == null) {
            this.mImList = new ArrayList();
        }
        this.mImList.add(new ImData(i, str, str2, i2, z));
    }

    private void addNote(String str) {
        if (this.mNoteList == null) {
            this.mNoteList = new ArrayList(1);
        }
        this.mNoteList.add(new NoteData(str));
    }

    private void addPhotoBytes(String str, byte[] bArr, boolean z) {
        if (this.mPhotoList == null) {
            this.mPhotoList = new ArrayList(1);
        }
        this.mPhotoList.add(new PhotoData(str, bArr, z));
    }

    private void tryHandleSortAsName(Map<String, Collection<String>> map) {
        Collection<String> collection;
        if ((!VCardConfig.isVersion30(this.mVCardType) || (TextUtils.isEmpty(this.mNameData.mPhoneticFamily) && TextUtils.isEmpty(this.mNameData.mPhoneticMiddle) && TextUtils.isEmpty(this.mNameData.mPhoneticGiven))) && (collection = map.get("SORT-AS")) != null && collection.size() != 0) {
            if (collection.size() > 1) {
                Log.w("MTK_vCard", "Incorrect multiple SORT_AS parameters detected: " + Arrays.toString(collection.toArray()));
            }
            List<String> listConstructListFromValue = VCardUtils.constructListFromValue(collection.iterator().next(), this.mVCardType);
            int size = listConstructListFromValue.size();
            if (size > 3) {
                size = 3;
            }
            switch (size) {
                case 3:
                    this.mNameData.mPhoneticMiddle = listConstructListFromValue.get(2);
                case 2:
                    this.mNameData.mPhoneticGiven = listConstructListFromValue.get(1);
                    break;
            }
            this.mNameData.mPhoneticFamily = listConstructListFromValue.get(0);
        }
    }

    private void handleNProperty(List<String> list, Map<String, Collection<String>> map) {
        int size;
        tryHandleSortAsName(map);
        if (list == null || (size = list.size()) < 1) {
            return;
        }
        if (size > 5) {
            size = 5;
        }
        switch (size) {
            case 5:
                this.mNameData.mSuffix = list.get(4);
            case CompatUtils.TYPE_ASSERT:
                this.mNameData.mPrefix = list.get(3);
            case 3:
                this.mNameData.mMiddle = list.get(2);
            case 2:
                this.mNameData.mGiven = list.get(1);
                break;
        }
        this.mNameData.mFamily = list.get(0);
    }

    private void handlePhoneticNameFromSound(List<String> list) {
        int size;
        boolean z;
        if (!TextUtils.isEmpty(this.mNameData.mPhoneticFamily) || !TextUtils.isEmpty(this.mNameData.mPhoneticMiddle) || !TextUtils.isEmpty(this.mNameData.mPhoneticGiven) || list == null || (size = list.size()) < 1) {
            return;
        }
        if (size > 3) {
            size = 3;
        }
        if (list.get(0).length() > 0) {
            int i = 1;
            while (true) {
                if (i < size) {
                    if (list.get(i).length() <= 0) {
                        i++;
                    } else {
                        z = false;
                        break;
                    }
                } else {
                    z = true;
                    break;
                }
            }
            if (z) {
                String[] strArrSplit = list.get(0).split(" ");
                int length = strArrSplit.length;
                if (length == 3) {
                    this.mNameData.mPhoneticFamily = strArrSplit[0];
                    this.mNameData.mPhoneticMiddle = strArrSplit[1];
                    this.mNameData.mPhoneticGiven = strArrSplit[2];
                    return;
                } else if (length == 2) {
                    this.mNameData.mPhoneticFamily = strArrSplit[0];
                    this.mNameData.mPhoneticGiven = strArrSplit[1];
                    return;
                } else {
                    this.mNameData.mPhoneticGiven = list.get(0);
                    return;
                }
            }
        }
        switch (size) {
            case 3:
                this.mNameData.mPhoneticMiddle = list.get(2);
            case 2:
                this.mNameData.mPhoneticGiven = list.get(1);
                break;
        }
        this.mNameData.mPhoneticFamily = list.get(0);
    }

    public void addProperty(VCardProperty vCardProperty) {
        int i;
        String strSubstring;
        boolean z;
        int iIntValue;
        boolean z2;
        boolean z3;
        String strSubstring2;
        boolean z4;
        String name = vCardProperty.getName();
        Map<String, Collection<String>> parameterMap = vCardProperty.getParameterMap();
        List<String> valueList = vCardProperty.getValueList();
        byte[] byteValue = vCardProperty.getByteValue();
        if ((valueList == null || valueList.size() == 0) && byteValue == null) {
            return;
        }
        String string = null;
        String strTrim = valueList != null ? listToString(valueList).trim() : null;
        if (name.equals("VERSION")) {
            return;
        }
        if (name.equals("FN")) {
            this.mNameData.mFormatted = strTrim;
            return;
        }
        if (name.equals("NAME")) {
            if (TextUtils.isEmpty(this.mNameData.mFormatted)) {
                this.mNameData.mFormatted = strTrim;
                return;
            }
            return;
        }
        if (name.equals("N")) {
            handleNProperty(valueList, parameterMap);
            return;
        }
        if (name.equals("SORT-STRING")) {
            this.mNameData.mSortString = strTrim;
            return;
        }
        if (name.equals("NICKNAME") || name.equals("X-NICKNAME")) {
            addNickName(strTrim);
            return;
        }
        if (name.equals("SOUND")) {
            Collection<String> collection = parameterMap.get("TYPE");
            if (collection == null || !collection.contains("X-IRMC-N")) {
                return;
            }
            handlePhoneticNameFromSound(VCardUtils.constructListFromValue(strTrim, this.mVCardType));
            return;
        }
        int i2 = -1;
        boolean z5 = false;
        if (name.equals("ADR")) {
            Iterator<String> it = valueList.iterator();
            while (true) {
                if (it.hasNext()) {
                    if (!TextUtils.isEmpty(it.next())) {
                        z3 = false;
                        break;
                    }
                } else {
                    z3 = true;
                    break;
                }
            }
            if (z3) {
                return;
            }
            Collection<String> collection2 = parameterMap.get("TYPE");
            if (collection2 != null) {
                strSubstring2 = null;
                z4 = false;
                for (String str : collection2) {
                    String upperCase = str.toUpperCase();
                    if (upperCase.equals("PREF")) {
                        z4 = true;
                    } else if (upperCase.equals("HOME")) {
                        strSubstring2 = null;
                        i2 = 1;
                    } else if (upperCase.equals("WORK") || upperCase.equalsIgnoreCase("COMPANY")) {
                        strSubstring2 = null;
                        i2 = 2;
                    } else if (!upperCase.equals("PARCEL") && !upperCase.equals("DOM") && !upperCase.equals("INTL")) {
                        if (upperCase.equals("OTHER")) {
                            strSubstring2 = null;
                            i2 = 3;
                        } else if (i2 < 0) {
                            strSubstring2 = upperCase.startsWith("X-") ? str.substring(2) : str;
                            i2 = 0;
                        }
                    }
                }
            } else {
                strSubstring2 = null;
                z4 = false;
            }
            if (i2 < 0) {
                i2 = 1;
            }
            addPostal(i2, valueList, strSubstring2, z4);
            return;
        }
        if (name.equals("EMAIL")) {
            Collection<String> collection3 = parameterMap.get("TYPE");
            if (collection3 != null) {
                z2 = false;
                for (String strSubstring3 : collection3) {
                    String upperCase2 = strSubstring3.toUpperCase();
                    if (upperCase2.equals("PREF")) {
                        z2 = true;
                    } else if (upperCase2.equals("HOME")) {
                        i2 = 1;
                    } else if (upperCase2.equals("WORK")) {
                        i2 = 2;
                    } else if (upperCase2.equals("CELL")) {
                        i2 = 4;
                    } else if (i2 < 0) {
                        if (upperCase2.startsWith("X-")) {
                            strSubstring3 = strSubstring3.substring(2);
                        }
                        string = strSubstring3;
                        i2 = 0;
                    }
                }
            } else {
                z2 = false;
            }
            addEmail(i2 >= 0 ? i2 : 3, strTrim, string, z2);
            return;
        }
        if (name.equals("ORG")) {
            Collection<String> collection4 = parameterMap.get("TYPE");
            if (collection4 != null) {
                Iterator<String> it2 = collection4.iterator();
                while (it2.hasNext()) {
                    if (it2.next().equals("PREF")) {
                        z5 = true;
                    }
                }
            }
            handleOrgValue(1, valueList, parameterMap, z5);
            return;
        }
        if (name.equals("TITLE")) {
            handleTitleValue(strTrim);
            return;
        }
        if (name.equals("ROLE")) {
            return;
        }
        if (name.equals("PHOTO") || name.equals("LOGO")) {
            Collection<String> collection5 = parameterMap.get("VALUE");
            if (collection5 == null || !collection5.contains("URL")) {
                Collection<String> collection6 = parameterMap.get("TYPE");
                if (collection6 != null) {
                    for (String str2 : collection6) {
                        if ("PREF".equals(str2)) {
                            z5 = true;
                        } else if (string == null) {
                            string = str2;
                        }
                    }
                }
                addPhotoBytes(string, byteValue, z5);
                return;
            }
            return;
        }
        if (name.equals("TEL")) {
            if (!VCardConfig.isVersion40(this.mVCardType)) {
                strSubstring = strTrim;
                z = false;
            } else if (strTrim.startsWith("sip:")) {
                strSubstring = null;
                z = true;
            } else {
                if (strTrim.startsWith("tel:")) {
                    strSubstring = strTrim.substring(4);
                }
                z = false;
            }
            if (z) {
                handleSipCase(strTrim, parameterMap.get("TYPE"));
                return;
            }
            if (strTrim.length() == 0) {
                return;
            }
            Collection<String> collection7 = parameterMap.get("TYPE");
            Object phoneTypeFromStrings = VCardUtils.getPhoneTypeFromStrings(collection7, strSubstring);
            if (phoneTypeFromStrings instanceof Integer) {
                iIntValue = ((Integer) phoneTypeFromStrings).intValue();
            } else {
                string = phoneTypeFromStrings.toString();
                iIntValue = 0;
            }
            if (collection7 != null && collection7.contains("PREF")) {
                z5 = true;
            }
            addPhone(iIntValue, strSubstring, string, z5);
            return;
        }
        if (name.equals("X-SKYPE-PSTNNUMBER")) {
            Collection<String> collection8 = parameterMap.get("TYPE");
            if (collection8 != null && collection8.contains("PREF")) {
                z5 = true;
            }
            addPhone(7, strTrim, null, z5);
            return;
        }
        if (sImMap.containsKey(name)) {
            int iIntValue2 = sImMap.get(name).intValue();
            Collection<String> collection9 = parameterMap.get("TYPE");
            if (collection9 != null) {
                i = -1;
                for (String str3 : collection9) {
                    if (str3.equals("PREF")) {
                        z5 = true;
                    } else if (i < 0) {
                        if (str3.equalsIgnoreCase("HOME")) {
                            i = 1;
                        } else if (str3.equalsIgnoreCase("WORK")) {
                            i = 2;
                        }
                    }
                }
            } else {
                i = -1;
            }
            boolean z6 = z5;
            int i3 = i < 0 ? 1 : i;
            if (collection9 != null && iIntValue2 == -1 && collection9.size() > 0) {
                StringBuilder sb = new StringBuilder();
                Iterator<String> it3 = collection9.iterator();
                while (it3.hasNext()) {
                    sb.append(it3.next());
                }
                string = sb.toString();
            }
            addIm(iIntValue2, string, strTrim, i3, z6);
            return;
        }
        if (name.equals("NOTE")) {
            addNote(strTrim);
            return;
        }
        if (name.equals("URL")) {
            if (this.mWebsiteList == null) {
                this.mWebsiteList = new ArrayList(1);
            }
            this.mWebsiteList.add(new WebsiteData(strTrim));
            return;
        }
        if (name.equals("BDAY")) {
            this.mBirthday = new BirthdayData(strTrim);
            return;
        }
        if (name.equals("ANNIVERSARY")) {
            this.mAnniversary = new AnniversaryData(strTrim);
            return;
        }
        if (name.equals("X-PHONETIC-FIRST-NAME")) {
            this.mNameData.mPhoneticGiven = strTrim;
            return;
        }
        if (name.equals("X-PHONETIC-MIDDLE-NAME")) {
            this.mNameData.mPhoneticMiddle = strTrim;
            return;
        }
        if (name.equals("X-PHONETIC-LAST-NAME")) {
            this.mNameData.mPhoneticFamily = strTrim;
            return;
        }
        if (name.equals("IMPP")) {
            if (strTrim.startsWith("sip:")) {
                handleSipCase(strTrim, parameterMap.get("TYPE"));
            }
        } else if (name.equals("X-SIP")) {
            if (TextUtils.isEmpty(strTrim)) {
                return;
            }
            handleSipCase(strTrim, parameterMap.get("TYPE"));
        } else if (name.equals("X-ANDROID-CUSTOM")) {
            handleAndroidCustomProperty(VCardUtils.constructListFromValue(strTrim, this.mVCardType));
        } else if (name.toUpperCase().startsWith("X-")) {
            if (this.mUnknownXData == null) {
                this.mUnknownXData = new ArrayList();
            }
            this.mUnknownXData.add(new Pair<>(name, strTrim));
        }
    }

    private void handleSipCase(String str, Collection<String> collection) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        if (str.startsWith("sip:")) {
            str = str.substring(4);
            if (str.length() == 0) {
                return;
            }
        }
        int i = -1;
        String strSubstring = null;
        boolean z = false;
        if (collection != null) {
            boolean z2 = false;
            for (String str2 : collection) {
                String upperCase = str2.toUpperCase();
                if (upperCase.equals("PREF")) {
                    z2 = true;
                } else if (upperCase.equals("HOME")) {
                    i = 1;
                } else if (upperCase.equals("WORK")) {
                    i = 2;
                } else if (i < 0) {
                    strSubstring = upperCase.startsWith("X-") ? str2.substring(2) : str2;
                    i = 0;
                }
            }
            z = z2;
        }
        if (i < 0) {
            i = 3;
        }
        addSip(str, i, strSubstring, z);
    }

    public void addChild(VCardEntry vCardEntry) {
        if (this.mChildren == null) {
            this.mChildren = new ArrayList();
        }
        this.mChildren.add(vCardEntry);
    }

    private void handleAndroidCustomProperty(List<String> list) {
        if (this.mAndroidCustomDataList == null) {
            this.mAndroidCustomDataList = new ArrayList();
        }
        this.mAndroidCustomDataList.add(AndroidCustomData.constructAndroidCustomData(list));
    }

    private String constructDisplayName() {
        String formattedString;
        if (!TextUtils.isEmpty(this.mNameData.mFormatted)) {
            formattedString = this.mNameData.mFormatted;
        } else if (!this.mNameData.emptyStructuredName()) {
            formattedString = VCardUtils.constructNameFromElements(this.mVCardType, this.mNameData.mFamily, this.mNameData.mMiddle, this.mNameData.mGiven, this.mNameData.mPrefix, this.mNameData.mSuffix);
        } else if (!this.mNameData.emptyPhoneticStructuredName()) {
            formattedString = VCardUtils.constructNameFromElements(this.mVCardType, this.mNameData.mPhoneticFamily, this.mNameData.mPhoneticMiddle, this.mNameData.mPhoneticGiven);
        } else if (this.mEmailList != null && this.mEmailList.size() > 0) {
            formattedString = this.mEmailList.get(0).mAddress;
        } else if (this.mPhoneList != null && this.mPhoneList.size() > 0) {
            formattedString = this.mPhoneList.get(0).mNumber;
        } else if (this.mPostalList != null && this.mPostalList.size() > 0) {
            formattedString = this.mPostalList.get(0).getFormattedAddress(this.mVCardType);
        } else if (this.mOrganizationList != null && this.mOrganizationList.size() > 0) {
            formattedString = this.mOrganizationList.get(0).getFormattedString();
        } else {
            formattedString = null;
        }
        if (formattedString == null) {
            return "";
        }
        return formattedString;
    }

    public void consolidateFields() {
        this.mNameData.displayName = constructDisplayName();
    }

    public boolean isIgnorable() {
        IsIgnorableIterator isIgnorableIterator = new IsIgnorableIterator();
        iterateAllData(isIgnorableIterator);
        return isIgnorableIterator.getResult();
    }

    public ArrayList<ContentProviderOperation> constructInsertOperations(ContentResolver contentResolver, ArrayList<ContentProviderOperation> arrayList) {
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        if (isIgnorable()) {
            return arrayList;
        }
        int size = arrayList.size();
        ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI);
        if (this.mAccount != null) {
            builderNewInsert.withValue("account_name", this.mAccount.name);
            builderNewInsert.withValue("account_type", this.mAccount.type);
        } else {
            builderNewInsert.withValue("account_name", null);
            builderNewInsert.withValue("account_type", null);
        }
        builderNewInsert.withValue("aggregation_mode", 3);
        arrayList.add(builderNewInsert.build());
        arrayList.size();
        iterateAllData(new InsertOperationConstrutor(arrayList, size));
        arrayList.size();
        return arrayList;
    }

    private String listToString(List<String> list) {
        int size = list.size();
        if (size > 1) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (size - 1 > 0) {
                    sb.append(";");
                }
            }
            return sb.toString();
        }
        if (size == 1) {
            return list.get(0);
        }
        return "";
    }

    public final List<PhotoData> getPhotoList() {
        return this.mPhotoList;
    }

    public String getDisplayName() {
        if (this.mNameData.displayName == null) {
            this.mNameData.displayName = constructDisplayName();
        }
        return this.mNameData.displayName;
    }
}
