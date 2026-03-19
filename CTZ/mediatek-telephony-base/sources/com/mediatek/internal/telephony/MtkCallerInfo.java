package com.mediatek.internal.telephony;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CallerInfo;
import mediatek.telephony.MtkTelephony;

public class MtkCallerInfo extends CallerInfo {
    public static CallerInfo getCallerInfo(Context context, Uri uri, Cursor cursor) {
        return getCallerInfo(context, uri, cursor, SubscriptionManager.getDefaultSubscriptionId());
    }

    public static CallerInfo getCallerInfo(Context context, Uri uri, Cursor cursor, int i) {
        int columnIndex;
        MtkCallerInfo mtkCallerInfo = new MtkCallerInfo();
        mtkCallerInfo.photoResource = 0;
        mtkCallerInfo.phoneLabel = null;
        mtkCallerInfo.numberType = 0;
        mtkCallerInfo.numberLabel = null;
        mtkCallerInfo.cachedPhoto = null;
        mtkCallerInfo.isCachedPhotoCurrent = false;
        mtkCallerInfo.contactExists = false;
        mtkCallerInfo.userType = 0L;
        if (VDBG) {
            Rlog.v("CallerInfo", "getCallerInfo() based on cursor...");
        }
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex2 = cursor.getColumnIndex("display_name");
                if (columnIndex2 != -1) {
                    mtkCallerInfo.name = cursor.getString(columnIndex2);
                }
                int columnIndex3 = cursor.getColumnIndex(MtkTelephony.SmsCb.CbChannel.NUMBER);
                if (columnIndex3 != -1) {
                    mtkCallerInfo.phoneNumber = cursor.getString(columnIndex3);
                }
                int columnIndex4 = cursor.getColumnIndex("normalized_number");
                if (columnIndex4 != -1) {
                    mtkCallerInfo.normalizedNumber = cursor.getString(columnIndex4);
                }
                int columnIndex5 = cursor.getColumnIndex("label");
                if (columnIndex5 != -1 && (columnIndex = cursor.getColumnIndex("type")) != -1) {
                    mtkCallerInfo.numberType = cursor.getInt(columnIndex);
                    mtkCallerInfo.numberLabel = cursor.getString(columnIndex5);
                    try {
                        IMtkCallerInfoExt iMtkCallerInfoExtMakeMtkCallerInfoExt = MtkOpTelephonyCustomizationUtils.getOpFactory(context).makeMtkCallerInfoExt();
                        if (iMtkCallerInfoExtMakeMtkCallerInfoExt != null) {
                            mtkCallerInfo.phoneLabel = iMtkCallerInfoExtMakeMtkCallerInfoExt.getTypeLabel(context, mtkCallerInfo.numberType, mtkCallerInfo.numberLabel, cursor, i).toString();
                        } else {
                            Rlog.e("CallerInfo", "Fail to initialize ICallerInfoExt");
                        }
                    } catch (Exception e) {
                        Rlog.e("CallerInfo", "Fail to create plug-in");
                        e.printStackTrace();
                    }
                }
                int columnIndexForPersonId = getColumnIndexForPersonId(uri, cursor);
                if (columnIndexForPersonId != -1) {
                    long j = cursor.getLong(columnIndexForPersonId);
                    if (j != 0 && !ContactsContract.Contacts.isEnterpriseContactId(j)) {
                        mtkCallerInfo.contactIdOrZero = j;
                        if (VDBG) {
                            Rlog.v("CallerInfo", "==> got info.contactIdOrZero: " + mtkCallerInfo.contactIdOrZero);
                        }
                    }
                    if (ContactsContract.Contacts.isEnterpriseContactId(j)) {
                        mtkCallerInfo.userType = 1L;
                    }
                } else {
                    Rlog.w("CallerInfo", "Couldn't find contact_id column for " + uri);
                }
                int columnIndex6 = cursor.getColumnIndex("lookup");
                if (columnIndex6 != -1) {
                    mtkCallerInfo.lookupKey = cursor.getString(columnIndex6);
                }
                int columnIndex7 = cursor.getColumnIndex("photo_uri");
                if (columnIndex7 != -1 && cursor.getString(columnIndex7) != null) {
                    mtkCallerInfo.contactDisplayPhotoUri = Uri.parse(cursor.getString(columnIndex7));
                } else {
                    mtkCallerInfo.contactDisplayPhotoUri = null;
                }
                int columnIndex8 = cursor.getColumnIndex("custom_ringtone");
                if (columnIndex8 != -1 && cursor.getString(columnIndex8) != null) {
                    if (TextUtils.isEmpty(cursor.getString(columnIndex8))) {
                        mtkCallerInfo.contactRingtoneUri = Uri.EMPTY;
                    } else {
                        mtkCallerInfo.contactRingtoneUri = Uri.parse(cursor.getString(columnIndex8));
                    }
                } else {
                    mtkCallerInfo.contactRingtoneUri = null;
                }
                int columnIndex9 = cursor.getColumnIndex("send_to_voicemail");
                mtkCallerInfo.shouldSendToVoicemail = columnIndex9 != -1 && cursor.getInt(columnIndex9) == 1;
                mtkCallerInfo.contactExists = true;
            }
            while (!mtkCallerInfo.shouldSendToVoicemail && cursor.moveToNext()) {
                int columnIndex10 = cursor.getColumnIndex("send_to_voicemail");
                mtkCallerInfo.shouldSendToVoicemail = columnIndex10 != -1 && cursor.getInt(columnIndex10) == 1;
            }
            cursor.close();
        }
        mtkCallerInfo.needUpdate = false;
        mtkCallerInfo.name = normalize(mtkCallerInfo.name);
        mtkCallerInfo.contactRefUri = uri;
        return mtkCallerInfo;
    }

    public static CallerInfo getCallerInfo(Context context, String str, int i) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        Rlog.d("CallerInfo", "number xxxxxx subId: " + i);
        int currentPhoneType = TelephonyManager.getDefault().getCurrentPhoneType(i);
        if (MtkPhoneNumberUtils.isEmergencyNumberExt(str, currentPhoneType)) {
            CallerInfo callerInfoMarkAsEmergency = new MtkCallerInfo().markAsEmergency(context);
            if (currentPhoneType == 2) {
                callerInfoMarkAsEmergency.name = callerInfoMarkAsEmergency.phoneNumber;
                callerInfoMarkAsEmergency.phoneNumber = str;
            }
            return callerInfoMarkAsEmergency;
        }
        if (PhoneNumberUtils.isVoiceMailNumber(i, str)) {
            return new MtkCallerInfo().markAsVoiceMail(i);
        }
        CallerInfo callerInfoDoSecondaryLookupIfNecessary = doSecondaryLookupIfNecessary(context, str, getCallerInfo(context, Uri.withAppendedPath(ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, Uri.encode(str))));
        if (TextUtils.isEmpty(callerInfoDoSecondaryLookupIfNecessary.phoneNumber)) {
            callerInfoDoSecondaryLookupIfNecessary.phoneNumber = str;
        }
        return callerInfoDoSecondaryLookupIfNecessary;
    }
}
