package org.gsma.joyn;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.util.Log;
import com.android.ims.ImsConfig;
import org.gsma.joyn.chat.ChatLog;

public class JoynServiceConfiguration {
    public static final String TRUE = Boolean.toString(true);
    public static final String FALSE = Boolean.toString(false);

    public static boolean isServiceActivated() {
        return false;
    }

    public static boolean isServiceActivated(Context ctx) {
        boolean result = false;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='ServiceActivated'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                String value = c.getString(2);
                result = Boolean.parseBoolean(value);
            }
            c.close();
        }
        return result;
    }

    public static String getUserDisplayName() {
        return null;
    }

    public static String getAliasName(Context ctx, String Contact) {
        Log.d("getAliasName ", Contact);
        Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.chat/message");
        ContentResolver cr = ctx.getContentResolver();
        String aliasName = "";
        Cursor cursor = cr.query(CONTENT_URI, new String[]{ChatLog.Message.DISPLAY_NAME}, "(sender='" + Contact + "' AND " + ChatLog.Message.DISPLAY_NAME + " <> '' )", null, "timestamp DESC");
        if (cursor.moveToFirst()) {
            String status = cursor.getString(0);
            aliasName = status;
        }
        cursor.close();
        return aliasName;
    }

    public boolean getProfileAuth(Context ctx) {
        boolean result = false;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='profileAuth'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                String value = c.getString(2);
                result = Boolean.parseBoolean(value);
            }
            c.close();
        }
        return result;
    }

    public boolean getNABAuth(Context ctx) {
        boolean result = false;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='nabAuth'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                String value = c.getString(2);
                result = Boolean.parseBoolean(value);
            }
            c.close();
        }
        return result;
    }

    public boolean getPublicAccountAUTH(Context ctx) {
        boolean result = false;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='publicAccountAuth'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                String value = c.getString(2);
                result = Boolean.parseBoolean(value);
            }
            c.close();
        }
        return result;
    }

    public boolean getSSOAuth(Context ctx) {
        boolean result = false;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='ssoAuth'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                String value = c.getString(2);
                result = Boolean.parseBoolean(value);
            }
            c.close();
        }
        return result;
    }

    public String getProfileAddress(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='profileAddress'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getProfileAddressPort(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='profileAddressPort'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getProfileAddressType(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='ProfileAddressType'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getNABAddress(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='nabAddress'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getNABAddressPort(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='nabAddressPort'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getNABAddressType(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='nabAddressType'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getPublicAccountAddress(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='publicAccountAddress'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getPublicAccountAddressPort(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='publicAccountAddressPort'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getPublicAccountAddressType(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='publicAccountAddressType'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getSSOAddress(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='SSOAddress'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getSSOAddressPort(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='SSOAddressPort'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getSSOAddressType(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='SSOAddressType'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getQRAddress(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='QRAddress'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getQRAddressType(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='QRAddressType'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getMessageStoreAddress(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='msgStoreAddress'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getMessageStoreUser(Context ctx) {
        String result = null;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='msgStoreUser'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getPublicUri(Context ctx) {
        String result = "";
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='publicUri'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public String getSecondaryUserIdentity(Context ctx) {
        String result = "";
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='publicUserIdentityPC'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        return result;
    }

    public boolean getConfigurationState(Context ctx) {
        boolean result = false;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='configurationState'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                String value = c.getString(2);
                result = Boolean.parseBoolean(value);
            }
            c.close();
        }
        return result;
    }

    public static boolean getServiceState(Context ctx) {
        boolean result = false;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='ServiceActivated'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                String value = c.getString(2);
                result = Boolean.parseBoolean(value);
            }
            c.close();
        }
        return result;
    }

    public void setFileRootDirectory(String path, Context ctx) {
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        if (ctx == null) {
            return;
        }
        ContentResolver cr = ctx.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(ImsConfig.EXTRA_NEW_VALUE, path);
        cr.update(databaseUri, values, "key='DirectoryPathFiles'", null);
    }

    public void setPhotoRootDirectory(String path, Context ctx) {
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        if (ctx == null) {
            return;
        }
        ContentResolver cr = ctx.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(ImsConfig.EXTRA_NEW_VALUE, path);
        cr.update(databaseUri, values, "key='DirectoryPathPhotos'", null);
    }

    public void setVideoRootDirectory(String path, Context ctx) {
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        if (ctx == null) {
            return;
        }
        ContentResolver cr = ctx.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(ImsConfig.EXTRA_NEW_VALUE, path);
        cr.update(databaseUri, values, "key='DirectoryPathVideos'", null);
    }

    public static void setServicePermissionState(boolean state, Context ctx) {
        String stringState;
        if (state) {
            stringState = TRUE;
        } else {
            stringState = FALSE;
        }
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        if (ctx == null) {
            return;
        }
        ContentResolver cr = ctx.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(ImsConfig.EXTRA_NEW_VALUE, stringState);
        cr.update(databaseUri, values, "key='servicePermitted'", null);
    }

    public static boolean isServicePermission(Context ctx) {
        boolean result = false;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='servicePermitted'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                String value = c.getString(2);
                result = Boolean.parseBoolean(value);
            }
            c.close();
        }
        return result;
    }

    public static boolean isClosedGroupSupported(Context ctx) {
        String optr = SystemProperties.get("persist.operator.optr");
        if (!optr.equalsIgnoreCase("op08")) {
            return false;
        }
        return true;
    }

    public static boolean getIR94VideoCapabilityAuth(Context ctx) {
        boolean result = false;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='ir94VideoSupported'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                String value = c.getString(2);
                result = Boolean.parseBoolean(value);
            }
            c.close();
        }
        return result;
    }

    public static int getEnableRcsSwitch(Context ctx) {
        int result = 1;
        Uri databaseUri = Uri.parse("content://com.orangelabs.rcs.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='enableRcsSwitch'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                String value = c.getString(2);
                try {
                    result = Integer.parseInt(value);
                } catch (Exception e) {
                }
            }
            c.close();
        }
        return result;
    }

    public static boolean isPresenceDiscoverySupported(Context ctx) {
        boolean result = false;
        String xdmServer = null;
        Uri databaseUri = Uri.parse("content://com.mediatek.presence.settings/settings");
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(databaseUri, null, "key='XdmServerAddr'", null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                xdmServer = c.getString(2);
            }
            c.close();
        }
        Log.v("JoynServiceConfiguration", "xdmServer: " + xdmServer);
        if (xdmServer == null || xdmServer.length() == 0) {
            return false;
        }
        Cursor c2 = cr.query(databaseUri, null, "key='CapabilityPresenceDiscovery'", null, null);
        if (c2 != null) {
            if (c2.getCount() > 0 && c2.moveToFirst()) {
                result = Boolean.parseBoolean(c2.getString(2));
            }
            c2.close();
        }
        Log.v("JoynServiceConfiguration", "isPresenceDiscoverySupported: " + result);
        return result;
    }
}
