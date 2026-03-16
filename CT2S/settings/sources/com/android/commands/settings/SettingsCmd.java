package com.android.commands.settings;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.IContentProvider;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;

public final class SettingsCmd {
    static final String TAG = "settings";
    static String[] mArgs;
    int mNextArg;
    int mUser = -1;
    CommandVerb mVerb = CommandVerb.UNSPECIFIED;
    String mTable = null;
    String mKey = null;
    String mValue = null;

    enum CommandVerb {
        UNSPECIFIED,
        GET,
        PUT,
        DELETE
    }

    public static void main(String[] args) {
        if (args == null || args.length < 3) {
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
                    } else {
                        this.mUser = Integer.parseInt(nextArg());
                    }
                } else if (this.mVerb == CommandVerb.UNSPECIFIED) {
                    if ("get".equalsIgnoreCase(arg)) {
                        this.mVerb = CommandVerb.GET;
                    } else if ("put".equalsIgnoreCase(arg)) {
                        this.mVerb = CommandVerb.PUT;
                    } else if ("delete".equalsIgnoreCase(arg)) {
                        this.mVerb = CommandVerb.DELETE;
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
            if (this.mUser < 0) {
                this.mUser = 0;
            }
            try {
                IActivityManager activityManager = ActivityManagerNative.getDefault();
                IContentProvider provider = null;
                IBinder token = new Binder();
                try {
                    IActivityManager.ContentProviderHolder holder = activityManager.getContentProviderExternal(TAG, 0, token);
                    if (holder == null) {
                        throw new IllegalStateException("Could not find settings provider");
                    }
                    provider = holder.provider;
                    switch (this.mVerb) {
                        case GET:
                            System.out.println(getForUser(provider, this.mUser, this.mTable, this.mKey));
                            break;
                        case PUT:
                            putForUser(provider, this.mUser, this.mTable, this.mKey, this.mValue);
                            break;
                        case DELETE:
                            System.out.println("Deleted " + deleteForUser(provider, this.mUser, this.mTable, this.mKey) + " rows");
                            break;
                        default:
                            System.err.println("Unspecified command");
                            break;
                    }
                    if (provider != null) {
                        return;
                    } else {
                        return;
                    }
                } finally {
                    if (provider != null) {
                        activityManager.removeContentProviderExternal(TAG, token);
                    }
                }
            } catch (Exception e2) {
                System.err.println("Error while accessing settings provider");
                e2.printStackTrace();
                return;
            }
        }
        printUsage();
    }

    private String nextArg() {
        if (this.mNextArg >= mArgs.length) {
            return null;
        }
        String str = mArgs[this.mNextArg];
        this.mNextArg++;
        return str;
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
            Bundle b = provider.call((String) null, callGetCommand, key, arg);
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
            provider.call((String) null, callPutCommand, key, arg);
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
            int num = provider.delete((String) null, targetUri, (String) null, (String[]) null);
            return num;
        } catch (RemoteException e) {
            System.err.println("Can't clear key " + key + " in " + table + " for user " + userHandle);
            return 0;
        }
    }

    private static void printUsage() {
        System.err.println("usage:  settings [--user NUM] get namespace key");
        System.err.println("        settings [--user NUM] put namespace key value");
        System.err.println("        settings [--user NUM] delete namespace key");
        System.err.println("\n'namespace' is one of {system, secure, global}, case-insensitive");
        System.err.println("If '--user NUM' is not given, the operations are performed on the owner user.");
    }
}
