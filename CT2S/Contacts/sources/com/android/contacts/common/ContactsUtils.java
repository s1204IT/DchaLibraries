package com.android.contacts.common;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Pair;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.dataitem.ImDataItem;
import java.util.List;

public class ContactsUtils {
    private static int sThumbnailSize = -1;

    public static String lookupProviderNameFromId(int protocol) {
        switch (protocol) {
            case 0:
                return "AIM";
            case 1:
                return "MSN";
            case 2:
                return "Yahoo";
            case 3:
                return "SKYPE";
            case 4:
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

    public static boolean isGraphic(CharSequence str) {
        return !TextUtils.isEmpty(str) && TextUtils.isGraphic(str);
    }

    public static boolean areObjectsEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static boolean areContactWritableAccountsAvailable(Context context) {
        List<AccountWithDataSet> accounts = AccountTypeManager.getInstance(context).getAccounts(true);
        return !accounts.isEmpty();
    }

    public static int getThumbnailSize(Context context) {
        Cursor c;
        if (sThumbnailSize == -1 && (c = context.getContentResolver().query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI, new String[]{"thumbnail_max_dim"}, null, null, null)) != null) {
            try {
                if (c.moveToFirst()) {
                    sThumbnailSize = c.getInt(0);
                }
            } finally {
                c.close();
            }
        }
        if (sThumbnailSize != -1) {
            return sThumbnailSize;
        }
        return 96;
    }

    private static Intent getCustomImIntent(ImDataItem im, int protocol) {
        String host = im.getCustomProtocol();
        String data = im.getData();
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        if (protocol != -1) {
            host = lookupProviderNameFromId(protocol);
        }
        if (TextUtils.isEmpty(host)) {
            return null;
        }
        String authority = host.toLowerCase();
        Uri imUri = new Uri.Builder().scheme("imto").authority(authority).appendPath(data).build();
        return new Intent("android.intent.action.SENDTO", imUri);
    }

    public static Pair<Intent, Intent> buildImIntent(Context context, ImDataItem im) {
        Intent intent;
        Intent secondaryIntent = null;
        boolean isEmail = im.isCreatedFromEmail();
        if (!isEmail && !im.isProtocolValid()) {
            return new Pair<>(null, null);
        }
        String data = im.getData();
        if (TextUtils.isEmpty(data)) {
            return new Pair<>(null, null);
        }
        int protocol = isEmail ? 5 : im.getProtocol().intValue();
        if (protocol == 5) {
            int chatCapability = im.getChatCapability();
            if ((chatCapability & 4) != 0 || (chatCapability & 1) != 0) {
                intent = new Intent("android.intent.action.SENDTO", Uri.parse("xmpp:" + data + "?message"));
                secondaryIntent = new Intent("android.intent.action.SENDTO", Uri.parse("xmpp:" + data + "?call"));
            } else {
                intent = new Intent("android.intent.action.SENDTO", Uri.parse("xmpp:" + data + "?message"));
            }
        } else {
            intent = getCustomImIntent(im, protocol);
        }
        return new Pair<>(intent, secondaryIntent);
    }
}
