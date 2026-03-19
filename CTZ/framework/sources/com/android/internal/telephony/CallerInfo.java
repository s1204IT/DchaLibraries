package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Country;
import android.location.CountryDetector;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import com.android.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.android.internal.R;
import java.lang.reflect.Method;
import java.util.Locale;

public class CallerInfo {
    static final String EXTENSION_CLASS_NAME = "com.mediatek.internal.telephony.MtkCallerInfo";
    public static final long USER_TYPE_CURRENT = 0;
    public static final long USER_TYPE_WORK = 1;
    public Drawable cachedPhoto;
    public Bitmap cachedPhotoIcon;
    public String cnapName;
    public Uri contactDisplayPhotoUri;
    public boolean contactExists;
    public long contactIdOrZero;
    public Uri contactRefUri;
    public Uri contactRingtoneUri;
    public String geoDescription;
    public boolean isCachedPhotoCurrent;
    public String lookupKey;
    public String name;
    public int namePresentation;
    public boolean needUpdate;
    public String normalizedNumber;
    public String numberLabel;
    public int numberPresentation;
    public int numberType;
    public String phoneLabel;
    public String phoneNumber;
    public int photoResource;
    public boolean shouldSendToVoicemail;
    protected static final String TAG = "CallerInfo";
    protected static final boolean VDBG = Rlog.isLoggable(TAG, 2);
    private boolean mIsEmergency = false;
    private boolean mIsVoiceMail = false;
    public long userType = 0;

    public static CallerInfo getCallerInfo(Context context, Uri uri, Cursor cursor) {
        int columnIndex;
        try {
            Class<?> cls = Class.forName(EXTENSION_CLASS_NAME);
            Method declaredMethod = cls.getDeclaredMethod("getCallerInfo", Context.class, Uri.class, Cursor.class);
            Object[] objArr = {context, uri, cursor};
            Rlog.d(TAG, "invoke redirect to " + cls.getName() + "." + declaredMethod.getName());
            return (CallerInfo) declaredMethod.invoke(null, objArr);
        } catch (Exception e) {
            e.printStackTrace();
            Rlog.d(TAG, "getCallerInfo invoke redirect fails. Use AOSP instead.");
            CallerInfo callerInfo = new CallerInfo();
            callerInfo.photoResource = 0;
            callerInfo.phoneLabel = null;
            callerInfo.numberType = 0;
            callerInfo.numberLabel = null;
            callerInfo.cachedPhoto = null;
            callerInfo.isCachedPhotoCurrent = false;
            callerInfo.contactExists = false;
            callerInfo.userType = 0L;
            if (VDBG) {
                Rlog.v(TAG, "getCallerInfo() based on cursor...");
            }
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex2 = cursor.getColumnIndex("display_name");
                    if (columnIndex2 != -1) {
                        callerInfo.name = cursor.getString(columnIndex2);
                    }
                    int columnIndex3 = cursor.getColumnIndex("number");
                    if (columnIndex3 != -1) {
                        callerInfo.phoneNumber = cursor.getString(columnIndex3);
                    }
                    int columnIndex4 = cursor.getColumnIndex("normalized_number");
                    if (columnIndex4 != -1) {
                        callerInfo.normalizedNumber = cursor.getString(columnIndex4);
                    }
                    int columnIndex5 = cursor.getColumnIndex("label");
                    if (columnIndex5 != -1 && (columnIndex = cursor.getColumnIndex("type")) != -1) {
                        callerInfo.numberType = cursor.getInt(columnIndex);
                        callerInfo.numberLabel = cursor.getString(columnIndex5);
                        callerInfo.phoneLabel = ContactsContract.CommonDataKinds.Phone.getDisplayLabel(context, callerInfo.numberType, callerInfo.numberLabel).toString();
                    }
                    int columnIndexForPersonId = getColumnIndexForPersonId(uri, cursor);
                    if (columnIndexForPersonId != -1) {
                        long j = cursor.getLong(columnIndexForPersonId);
                        if (j != 0 && !ContactsContract.Contacts.isEnterpriseContactId(j)) {
                            callerInfo.contactIdOrZero = j;
                            if (VDBG) {
                                Rlog.v(TAG, "==> got info.contactIdOrZero: " + callerInfo.contactIdOrZero);
                            }
                        }
                        if (ContactsContract.Contacts.isEnterpriseContactId(j)) {
                            callerInfo.userType = 1L;
                        }
                    } else {
                        Rlog.w(TAG, "Couldn't find contact_id column for " + uri);
                    }
                    int columnIndex6 = cursor.getColumnIndex(ContactsContract.ContactsColumns.LOOKUP_KEY);
                    if (columnIndex6 != -1) {
                        callerInfo.lookupKey = cursor.getString(columnIndex6);
                    }
                    int columnIndex7 = cursor.getColumnIndex("photo_uri");
                    if (columnIndex7 != -1 && cursor.getString(columnIndex7) != null) {
                        callerInfo.contactDisplayPhotoUri = Uri.parse(cursor.getString(columnIndex7));
                    } else {
                        callerInfo.contactDisplayPhotoUri = null;
                    }
                    int columnIndex8 = cursor.getColumnIndex("custom_ringtone");
                    if (columnIndex8 != -1 && cursor.getString(columnIndex8) != null) {
                        if (TextUtils.isEmpty(cursor.getString(columnIndex8))) {
                            callerInfo.contactRingtoneUri = Uri.EMPTY;
                        } else {
                            callerInfo.contactRingtoneUri = Uri.parse(cursor.getString(columnIndex8));
                        }
                    } else {
                        callerInfo.contactRingtoneUri = null;
                    }
                    int columnIndex9 = cursor.getColumnIndex("send_to_voicemail");
                    callerInfo.shouldSendToVoicemail = columnIndex9 != -1 && cursor.getInt(columnIndex9) == 1;
                    callerInfo.contactExists = true;
                }
                cursor.close();
            }
            callerInfo.needUpdate = false;
            callerInfo.name = normalize(callerInfo.name);
            callerInfo.contactRefUri = uri;
            return callerInfo;
        }
    }

    public static CallerInfo getCallerInfo(Context context, Uri uri) {
        ContentResolver currentProfileContentResolver = CallerInfoAsyncQuery.getCurrentProfileContentResolver(context);
        if (currentProfileContentResolver != null) {
            try {
                return getCallerInfo(context, uri, currentProfileContentResolver.query(uri, null, null, null, null));
            } catch (RuntimeException e) {
                Rlog.e(TAG, "Error getting caller info.", e);
            }
        }
        return null;
    }

    public static CallerInfo getCallerInfo(Context context, String str) {
        if (VDBG) {
            Rlog.v(TAG, "getCallerInfo() based on number...");
        }
        return getCallerInfo(context, str, SubscriptionManager.getDefaultSubscriptionId());
    }

    public static CallerInfo getCallerInfo(Context context, String str, int i) {
        try {
            Class<?> cls = Class.forName(EXTENSION_CLASS_NAME);
            Method declaredMethod = cls.getDeclaredMethod("getCallerInfo", Context.class, String.class, Integer.TYPE);
            Object[] objArr = {context, str, Integer.valueOf(i)};
            Rlog.d(TAG, "invoke redirect to " + cls.getName() + "." + declaredMethod.getName() + "(subId=" + i + ")");
            return (CallerInfo) declaredMethod.invoke(null, objArr);
        } catch (Exception e) {
            e.printStackTrace();
            Rlog.d(TAG, "getCallerInfo invoke redirect fails. Use AOSP instead.");
            if (TextUtils.isEmpty(str)) {
                return null;
            }
            if (PhoneNumberUtils.isLocalEmergencyNumber(context, str)) {
                return new CallerInfo().markAsEmergency(context);
            }
            if (PhoneNumberUtils.isVoiceMailNumber(i, str)) {
                return new CallerInfo().markAsVoiceMail();
            }
            CallerInfo callerInfoDoSecondaryLookupIfNecessary = doSecondaryLookupIfNecessary(context, str, getCallerInfo(context, Uri.withAppendedPath(ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, Uri.encode(str))));
            if (TextUtils.isEmpty(callerInfoDoSecondaryLookupIfNecessary.phoneNumber)) {
                callerInfoDoSecondaryLookupIfNecessary.phoneNumber = str;
            }
            return callerInfoDoSecondaryLookupIfNecessary;
        }
    }

    public static CallerInfo doSecondaryLookupIfNecessary(Context context, String str, CallerInfo callerInfo) {
        if (!callerInfo.contactExists && PhoneNumberUtils.isUriNumber(str)) {
            String usernameFromUriNumber = PhoneNumberUtils.getUsernameFromUriNumber(str);
            if (PhoneNumberUtils.isGlobalPhoneNumber(usernameFromUriNumber)) {
                return getCallerInfo(context, Uri.withAppendedPath(ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, Uri.encode(usernameFromUriNumber)));
            }
            return callerInfo;
        }
        return callerInfo;
    }

    public boolean isEmergencyNumber() {
        return this.mIsEmergency;
    }

    public boolean isVoiceMailNumber() {
        return this.mIsVoiceMail;
    }

    public CallerInfo markAsEmergency(Context context) {
        this.phoneNumber = context.getString(R.string.emergency_call_dialog_number_for_display);
        this.photoResource = R.drawable.picture_emergency;
        this.mIsEmergency = true;
        return this;
    }

    CallerInfo markAsVoiceMail() {
        return markAsVoiceMail(SubscriptionManager.getDefaultSubscriptionId());
    }

    public CallerInfo markAsVoiceMail(int i) {
        this.mIsVoiceMail = true;
        try {
            this.phoneNumber = TelephonyManager.getDefault().getVoiceMailAlphaTag(i);
        } catch (SecurityException e) {
            Rlog.e(TAG, "Cannot access VoiceMail.", e);
        }
        return this;
    }

    protected static String normalize(String str) {
        if (str == null || str.length() > 0) {
            return str;
        }
        return null;
    }

    protected static int getColumnIndexForPersonId(Uri uri, Cursor cursor) {
        if (VDBG) {
            Rlog.v(TAG, "- getColumnIndexForPersonId: contactRef URI = '" + uri + "'...");
        }
        String string = uri.toString();
        String str = null;
        if (string.startsWith("content://com.android.contacts/data/phones")) {
            if (VDBG) {
                Rlog.v(TAG, "'data/phones' URI; using RawContacts.CONTACT_ID");
            }
            str = "contact_id";
        } else if (string.startsWith("content://com.android.contacts/data")) {
            if (VDBG) {
                Rlog.v(TAG, "'data' URI; using Data.CONTACT_ID");
            }
            str = "contact_id";
        } else if (string.startsWith("content://com.android.contacts/phone_lookup")) {
            if (VDBG) {
                Rlog.v(TAG, "'phone_lookup' URI; using PhoneLookup._ID");
            }
            str = "_id";
        } else {
            Rlog.w(TAG, "Unexpected prefix for contactRef '" + string + "'");
        }
        int columnIndex = str != null ? cursor.getColumnIndex(str) : -1;
        if (VDBG) {
            Rlog.v(TAG, "==> Using column '" + str + "' (columnIndex = " + columnIndex + ") for person_id lookup...");
        }
        return columnIndex;
    }

    public void updateGeoDescription(Context context, String str) {
        if (!TextUtils.isEmpty(this.phoneNumber)) {
            str = this.phoneNumber;
        }
        this.geoDescription = getGeoDescription(context, str);
    }

    public static String getGeoDescription(Context context, String str) {
        Phonenumber.PhoneNumber phoneNumber;
        if (VDBG) {
            Rlog.v(TAG, "getGeoDescription('" + str + "')...");
        }
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        PhoneNumberOfflineGeocoder phoneNumberOfflineGeocoder = PhoneNumberOfflineGeocoder.getInstance();
        Locale locale = context.getResources().getConfiguration().locale;
        String currentCountryIso = getCurrentCountryIso(context, locale);
        try {
            if (VDBG) {
                Rlog.v(TAG, "parsing '" + str + "' for countryIso '" + currentCountryIso + "'...");
            }
            phoneNumber = phoneNumberUtil.parse(str, currentCountryIso);
        } catch (NumberParseException e) {
            phoneNumber = null;
        }
        try {
            if (VDBG) {
                Rlog.v(TAG, "- parsed number: " + phoneNumber);
            }
        } catch (NumberParseException e2) {
            Rlog.w(TAG, "getGeoDescription: NumberParseException for incoming number '" + Rlog.pii(TAG, str) + "'");
        }
        if (phoneNumber == null) {
            return null;
        }
        String descriptionForNumber = phoneNumberOfflineGeocoder.getDescriptionForNumber(phoneNumber, locale);
        if (VDBG) {
            Rlog.v(TAG, "- got description: '" + descriptionForNumber + "'");
        }
        return descriptionForNumber;
    }

    private static String getCurrentCountryIso(Context context, Locale locale) {
        String countryIso;
        CountryDetector countryDetector = (CountryDetector) context.getSystemService(Context.COUNTRY_DETECTOR);
        if (countryDetector != null) {
            Country countryDetectCountry = countryDetector.detectCountry();
            if (countryDetectCountry != null) {
                countryIso = countryDetectCountry.getCountryIso();
            } else {
                Rlog.e(TAG, "CountryDetector.detectCountry() returned null.");
                countryIso = null;
            }
        } else {
            countryIso = null;
        }
        if (countryIso == null) {
            String country = locale.getCountry();
            Rlog.w(TAG, "No CountryDetector; falling back to countryIso based on locale: " + country);
            return country;
        }
        return countryIso;
    }

    public static String getCurrentCountryIso(Context context) {
        return getCurrentCountryIso(context, Locale.getDefault());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(super.toString() + " { ");
        StringBuilder sb2 = new StringBuilder();
        sb2.append("name ");
        sb2.append(this.name == null ? "null" : "non-null");
        sb.append(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append(", phoneNumber ");
        sb3.append(this.phoneNumber == null ? "null" : "non-null");
        sb.append(sb3.toString());
        sb.append(" }");
        return sb.toString();
    }
}
