package com.android.commands.settings;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SettingsCmd {

    private static final int[] f0xc47d3e85 = null;
    static String[] mArgs;
    int mNextArg;
    int mUser = -1;
    CommandVerb mVerb = CommandVerb.UNSPECIFIED;
    String mTable = null;
    String mKey = null;
    String mValue = null;

    private static int[] m0x2d213d29() {
        if (f0xc47d3e85 != null) {
            return f0xc47d3e85;
        }
        int[] iArr = new int[CommandVerb.valuesCustom().length];
        try {
            iArr[CommandVerb.DELETE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[CommandVerb.GET.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[CommandVerb.LIST.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[CommandVerb.PUT.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[CommandVerb.UNSPECIFIED.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        f0xc47d3e85 = iArr;
        return iArr;
    }

    enum CommandVerb {
        UNSPECIFIED,
        GET,
        PUT,
        DELETE,
        LIST;

        public static CommandVerb[] valuesCustom() {
            return values();
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            printUsage();
            return;
        }
        mArgs = args;
        try {
            new SettingsCmd().run();
        } catch (Exception e) {
            System.err.println("Unable to run settings command");
        }
    }

    public void run() {
        boolean valid = false;
        while (true) {
            try {
                String arg = nextArg();
                if (arg == null) {
                    break;
                }
                if ("--user".equals(arg)) {
                    if (this.mUser != -1) {
                        break;
                    }
                    String arg2 = nextArg();
                    if ("current".equals(arg2) || "cur".equals(arg2)) {
                        this.mUser = -2;
                    } else {
                        this.mUser = Integer.parseInt(arg2);
                    }
                } else if (this.mVerb == CommandVerb.UNSPECIFIED) {
                    if ("get".equalsIgnoreCase(arg)) {
                        this.mVerb = CommandVerb.GET;
                    } else if ("put".equalsIgnoreCase(arg)) {
                        this.mVerb = CommandVerb.PUT;
                    } else if ("delete".equalsIgnoreCase(arg)) {
                        this.mVerb = CommandVerb.DELETE;
                    } else if ("list".equalsIgnoreCase(arg)) {
                        this.mVerb = CommandVerb.LIST;
                    } else {
                        System.err.println("Invalid command: " + arg);
                        break;
                    }
                } else if (this.mTable == null) {
                    if (!"system".equalsIgnoreCase(arg) && !"secure".equalsIgnoreCase(arg) && !"global".equalsIgnoreCase(arg)) {
                        System.err.println("Invalid namespace '" + arg + "'");
                        break;
                    }
                    this.mTable = arg.toLowerCase();
                    if (this.mVerb == CommandVerb.LIST) {
                        valid = true;
                        break;
                    }
                } else {
                    if (this.mVerb == CommandVerb.GET || this.mVerb == CommandVerb.DELETE) {
                        break;
                    }
                    if (this.mKey == null) {
                        this.mKey = arg;
                    } else {
                        this.mValue = arg;
                        if (this.mNextArg >= mArgs.length) {
                            valid = true;
                        } else {
                            System.err.println("Too many arguments");
                        }
                    }
                }
            } catch (Exception e) {
                valid = false;
            }
        }
        if (valid) {
            try {
                IActivityManager activityManager = ActivityManagerNative.getDefault();
                if (this.mUser == -2) {
                    this.mUser = activityManager.getCurrentUser().id;
                }
                if (this.mUser < 0) {
                    this.mUser = 0;
                }
                IBinder token = new Binder();
                try {
                    IActivityManager.ContentProviderHolder holder = activityManager.getContentProviderExternal("settings", 0, token);
                    if (holder == null) {
                        throw new IllegalStateException("Could not find settings provider");
                    }
                    IContentProvider provider = holder.provider;
                    switch (m0x2d213d29()[this.mVerb.ordinal()]) {
                        case 1:
                            System.out.println("Deleted " + deleteForUser(provider, this.mUser, this.mTable, this.mKey) + " rows");
                            break;
                        case 2:
                            System.out.println(getForUser(provider, this.mUser, this.mTable, this.mKey));
                            break;
                        case 3:
                            for (String line : listForUser(provider, this.mUser, this.mTable)) {
                                System.out.println(line);
                            }
                            break;
                        case 4:
                            putForUser(provider, this.mUser, this.mTable, this.mKey, this.mValue);
                            break;
                        default:
                            System.err.println("Unspecified command");
                            break;
                    }
                    if (provider == null) {
                        return;
                    }
                    activityManager.removeContentProviderExternal("settings", token);
                    return;
                } catch (Throwable th) {
                    if (0 != 0) {
                        activityManager.removeContentProviderExternal("settings", token);
                    }
                    throw th;
                }
            } catch (Exception e2) {
                System.err.println("Error while accessing settings provider");
                e2.printStackTrace();
                return;
            }
        }
        printUsage();
    }

    private List<String> listForUser(IContentProvider provider, int userHandle, String table) {
        Uri uri;
        if ("system".equals(table)) {
            uri = Settings.System.CONTENT_URI;
        } else if ("secure".equals(table)) {
            uri = Settings.Secure.CONTENT_URI;
        } else {
            uri = "global".equals(table) ? Settings.Global.CONTENT_URI : null;
        }
        ArrayList<String> lines = new ArrayList<>();
        if (uri == null) {
            return lines;
        }
        try {
            Cursor cursor = provider.query(resolveCallingPackage(), uri, (String[]) null, (String) null, (String[]) null, (String) null, (ICancellationSignal) null);
            while (cursor != null) {
                try {
                    if (!cursor.moveToNext()) {
                        break;
                    }
                    lines.add(cursor.getString(1) + "=" + cursor.getString(2));
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            Collections.sort(lines);
        } catch (RemoteException e) {
            System.err.println("List failed in " + table + " for user " + userHandle);
        }
        return lines;
    }

    private String nextArg() {
        if (this.mNextArg >= mArgs.length) {
            return null;
        }
        String arg = mArgs[this.mNextArg];
        this.mNextArg++;
        return arg;
    }

    String getForUser(IContentProvider provider, int userHandle, String table, String key) {
        String callGetCommand;
        if ("system".equals(table)) {
            callGetCommand = "GET_system";
        } else if ("secure".equals(table)) {
            callGetCommand = "GET_secure";
        } else {
            if (!"global".equals(table)) {
                System.err.println("Invalid table; no put performed");
                throw new IllegalArgumentException("Invalid table " + table);
            }
            callGetCommand = "GET_global";
        }
        try {
            Bundle arg = new Bundle();
            arg.putInt("_user", userHandle);
            Bundle b = provider.call(resolveCallingPackage(), callGetCommand, key, arg);
            if (b == null) {
                return null;
            }
            String result = b.getPairValue();
            return result;
        } catch (RemoteException e) {
            System.err.println("Can't read key " + key + " in " + table + " for user " + userHandle);
            return null;
        }
    }

    void putForUser(IContentProvider provider, int userHandle, String table, String key, String value) {
        String callPutCommand;
        if ("system".equals(table)) {
            callPutCommand = "PUT_system";
        } else if ("secure".equals(table)) {
            callPutCommand = "PUT_secure";
        } else {
            if (!"global".equals(table)) {
                System.err.println("Invalid table; no put performed");
                return;
            }
            callPutCommand = "PUT_global";
        }
        try {
            Bundle arg = new Bundle();
            arg.putString("value", value);
            arg.putInt("_user", userHandle);
            provider.call(resolveCallingPackage(), callPutCommand, key, arg);
        } catch (RemoteException e) {
            System.err.println("Can't set key " + key + " in " + table + " for user " + userHandle);
        }
    }

    int deleteForUser(IContentProvider provider, int userHandle, String table, String key) {
        Uri targetUri;
        if ("system".equals(table)) {
            targetUri = Settings.System.getUriFor(key);
        } else if ("secure".equals(table)) {
            targetUri = Settings.Secure.getUriFor(key);
        } else {
            if (!"global".equals(table)) {
                System.err.println("Invalid table; no delete performed");
                throw new IllegalArgumentException("Invalid table " + table);
            }
            targetUri = Settings.Global.getUriFor(key);
        }
        try {
            int num = provider.delete(resolveCallingPackage(), targetUri, (String) null, (String[]) null);
            return num;
        } catch (RemoteException e) {
            System.err.println("Can't clear key " + key + " in " + table + " for user " + userHandle);
            return 0;
        }
    }

    private static void printUsage() {
        System.err.println("usage:  settings [--user <USER_ID> | current] get namespace key");
        System.err.println("        settings [--user <USER_ID> | current] put namespace key value");
        System.err.println("        settings [--user <USER_ID> | current] delete namespace key");
        System.err.println("        settings [--user <USER_ID> | current] list namespace");
        System.err.println("\n'namespace' is one of {system, secure, global}, case-insensitive");
        System.err.println("If '--user <USER_ID> | current' is not given, the operations are performed on the system user.");
    }

    public static String resolveCallingPackage() {
        switch (Process.myUid()) {
            case 0:
                return "root";
            case 2000:
                return "com.android.shell";
            default:
                return null;
        }
    }
}
