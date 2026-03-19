package com.android.contacts;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Pair;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.compat.ContactsCompat;
import com.android.contacts.compat.DirectoryCompat;
import com.android.contacts.model.dataitem.ImDataItem;

public class ContactsUtils {
    private static final int DEFAULT_THUMBNAIL_SIZE = 96;
    public static final boolean FLAG_N_FEATURE;
    public static final String SCHEME_IMTO = "imto";
    public static final String SCHEME_MAILTO = "mailto";
    public static final String SCHEME_SMSTO = "smsto";
    private static final String TAG = "ContactsUtils";
    public static final long USER_TYPE_CURRENT = 0;
    public static final long USER_TYPE_WORK = 1;
    private static int sThumbnailSize = -1;

    static {
        FLAG_N_FEATURE = Build.VERSION.SDK_INT >= 24;
    }

    public static String lookupProviderNameFromId(int i) {
        switch (i) {
            case 0:
                return "AIM";
            case 1:
                return "MSN";
            case 2:
                return "Yahoo";
            case 3:
                return "SKYPE";
            case CompatUtils.TYPE_ASSERT:
                return "QQ";
            case 5:
                return "GTalk";
            case 6:
                return "ICQ";
            case 7:
                return "JABBER";
            default:
                return null;
        }
    }

    public static boolean isGraphic(CharSequence charSequence) {
        return !TextUtils.isEmpty(charSequence) && TextUtils.isGraphic(charSequence);
    }

    public static boolean areObjectsEqual(Object obj, Object obj2) {
        return obj == obj2 || (obj != null && obj.equals(obj2));
    }

    public static final boolean areIntentActionEqual(Intent intent, Intent intent2) {
        if (intent == intent2) {
            return true;
        }
        if (intent == null || intent2 == null) {
            return false;
        }
        return TextUtils.equals(intent.getAction(), intent2.getAction());
    }

    public static int getThumbnailSize(Context context) {
        Cursor cursorQuery;
        if (sThumbnailSize == -1 && (cursorQuery = context.getContentResolver().query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI, new String[]{"thumbnail_max_dim"}, null, null, null)) != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    sThumbnailSize = cursorQuery.getInt(0);
                }
            } finally {
                cursorQuery.close();
            }
        }
        return sThumbnailSize != -1 ? sThumbnailSize : DEFAULT_THUMBNAIL_SIZE;
    }

    private static Intent getCustomImIntent(ImDataItem imDataItem, int i) {
        String customProtocol = imDataItem.getCustomProtocol();
        String data = imDataItem.getData();
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        if (i != -1) {
            customProtocol = lookupProviderNameFromId(i);
        }
        if (TextUtils.isEmpty(customProtocol)) {
            return null;
        }
        return new Intent("android.intent.action.SENDTO", new Uri.Builder().scheme(SCHEME_IMTO).authority(customProtocol.toLowerCase()).appendPath(data).build());
    }

    public static Pair<Intent, Intent> buildImIntent(Context context, ImDataItem imDataItem) {
        int iIntValue;
        Intent customImIntent;
        Intent intent;
        boolean zIsCreatedFromEmail = imDataItem.isCreatedFromEmail();
        Intent intent2 = null;
        if (!zIsCreatedFromEmail && !imDataItem.isProtocolValid()) {
            return new Pair<>(null, null);
        }
        String data = imDataItem.getData();
        if (TextUtils.isEmpty(data)) {
            return new Pair<>(null, null);
        }
        if (!zIsCreatedFromEmail) {
            iIntValue = imDataItem.getProtocol().intValue();
        } else {
            iIntValue = 5;
        }
        if (iIntValue == 5) {
            int chatCapability = imDataItem.getChatCapability();
            if ((chatCapability & 4) != 0) {
                customImIntent = new Intent("android.intent.action.SENDTO", Uri.parse("xmpp:" + data + "?message"));
                intent = new Intent("android.intent.action.SENDTO", Uri.parse("xmpp:" + data + "?call"));
            } else if ((chatCapability & 1) != 0) {
                customImIntent = new Intent("android.intent.action.SENDTO", Uri.parse("xmpp:" + data + "?message"));
                intent = new Intent("android.intent.action.SENDTO", Uri.parse("xmpp:" + data + "?call"));
            } else {
                customImIntent = new Intent("android.intent.action.SENDTO", Uri.parse("xmpp:" + data + "?message"));
            }
            intent2 = intent;
        } else {
            customImIntent = getCustomImIntent(imDataItem, iIntValue);
        }
        return new Pair<>(customImIntent, intent2);
    }

    public static long determineUserType(Long l, Long l2) {
        return l != null ? DirectoryCompat.isEnterpriseDirectoryId(l.longValue()) ? 1L : 0L : (l2 == null || l2.longValue() == 0 || !ContactsCompat.isEnterpriseContactId(l2.longValue())) ? 0L : 1L;
    }
}
