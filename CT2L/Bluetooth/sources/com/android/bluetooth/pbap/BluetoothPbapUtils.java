package com.android.bluetooth.pbap;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.opp.BluetoothShare;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class BluetoothPbapUtils {
    private static final String TAG = "FilterUtils";
    private static final boolean V = false;
    public static int FILTER_PHOTO = 3;
    public static int FILTER_TEL = 7;
    public static int FILTER_NICKNAME = 23;

    public static boolean hasFilter(byte[] filter) {
        return filter != null && filter.length > 0;
    }

    public static boolean isNameAndNumberOnly(byte[] filter) {
        if (!hasFilter(filter)) {
            Log.v(TAG, "No filter set. isNameAndNumberOnly=false");
            return false;
        }
        for (int i = 0; i <= 4; i++) {
            if (filter[i] != 0) {
                return false;
            }
        }
        return (filter[5] & 127) <= 0 && filter[6] == 0 && (filter[7] & 120) <= 0;
    }

    public static boolean isFilterBitSet(byte[] filter, int filterBit) {
        if (hasFilter(filter)) {
            int byteNumber = 7 - (filterBit / 8);
            int bitNumber = filterBit % 8;
            if (byteNumber < filter.length) {
                return (filter[byteNumber] & (1 << bitNumber)) > 0;
            }
        }
        return false;
    }

    public static VCardComposer createFilteredVCardComposer(Context ctx, int vcardType, byte[] filter) {
        int vType = vcardType;
        boolean includePhoto = BluetoothPbapConfig.includePhotosInVcard() && (!hasFilter(filter) || isFilterBitSet(filter, FILTER_PHOTO));
        if (!includePhoto) {
            vType |= VCardConfig.FLAG_REFRAIN_IMAGE_EXPORT;
        }
        return new VCardComposer(ctx, vType, true);
    }

    public static boolean isProfileSet(Context context) {
        Cursor c = context.getContentResolver().query(ContactsContract.Profile.CONTENT_VCARD_URI, new String[]{"_id"}, null, null, null);
        boolean isSet = c != null && c.getCount() > 0;
        if (c != null) {
            c.close();
        }
        return isSet;
    }

    public static String getProfileName(Context context) {
        Cursor c = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, new String[]{"display_name"}, null, null, null);
        String ownerName = null;
        if (c != null && c.moveToFirst()) {
            ownerName = c.getString(0);
        }
        if (c != null) {
            c.close();
        }
        return ownerName;
    }

    public static final String createProfileVCard(Context ctx, int vcardType, byte[] filter) {
        VCardComposer composer = null;
        String vcard = null;
        try {
            composer = createFilteredVCardComposer(ctx, vcardType, filter);
            if (composer.init(ContactsContract.Profile.CONTENT_URI, null, null, null, null, Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.RawContactsEntity.CONTENT_URI.getLastPathSegment()))) {
                vcard = composer.createOneEntry();
            } else {
                Log.e(TAG, "Unable to create profile vcard. Error initializing composer: " + composer.getErrorReason());
            }
        } catch (Throwable t) {
            Log.e(TAG, "Unable to create profile vcard.", t);
        }
        if (composer != null) {
            try {
                composer.terminate();
            } catch (Throwable th) {
            }
        }
        return vcard;
    }

    public static boolean createProfileVCardFile(File file, Context context) {
        AssetFileDescriptor fd;
        FileInputStream is = null;
        FileOutputStream os = null;
        boolean success = true;
        try {
            fd = context.getContentResolver().openAssetFileDescriptor(ContactsContract.Profile.CONTENT_VCARD_URI, "r");
        } catch (Throwable th) {
            t = th;
        }
        if (fd == null) {
            return false;
        }
        is = fd.createInputStream();
        FileOutputStream os2 = new FileOutputStream(file);
        try {
            Utils.copyStream(is, os2, BluetoothShare.STATUS_SUCCESS);
            os = os2;
        } catch (Throwable th2) {
            t = th2;
            os = os2;
            Log.e(TAG, "Unable to create default contact vcard file", t);
            success = false;
        }
        Utils.safeCloseStream(is);
        Utils.safeCloseStream(os);
        return success;
        Log.e(TAG, "Unable to create default contact vcard file", t);
        success = false;
        Utils.safeCloseStream(is);
        Utils.safeCloseStream(os);
        return success;
    }
}
