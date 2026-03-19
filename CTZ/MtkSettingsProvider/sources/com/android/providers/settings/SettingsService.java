package com.android.providers.settings;

import android.app.ActivityManager;
import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.ICancellationSignal;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserManager;
import android.provider.Settings;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class SettingsService extends Binder {
    final SettingsProvider mProvider;

    public SettingsService(SettingsProvider settingsProvider) {
        this.mProvider = settingsProvider;
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new MyShellCommand(this.mProvider, false).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String str;
        if (this.mProvider.getContext().checkCallingPermission("android.permission.DUMP") != 0) {
            printWriter.println("Permission Denial: can't dump SettingsProvider from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            return;
        }
        int i = 0;
        boolean z = false;
        while (i < strArr.length && (str = strArr[i]) != null && str.length() > 0 && str.charAt(0) == '-') {
            i++;
            if ("-h".equals(str)) {
                MyShellCommand.dumpHelp(printWriter, true);
                return;
            } else if (!"--proto".equals(str)) {
                printWriter.println("Unknown argument: " + str + "; use -h for help");
            } else {
                z = true;
            }
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (z) {
                this.mProvider.dumpProto(fileDescriptor);
            } else {
                this.mProvider.dumpInternal(fileDescriptor, printWriter, strArr);
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    static final class MyShellCommand extends ShellCommand {
        final boolean mDumping;
        boolean mMakeDefault;
        final SettingsProvider mProvider;
        int mUser = -1;
        CommandVerb mVerb = CommandVerb.UNSPECIFIED;
        String mTable = null;
        String mKey = null;
        String mValue = null;
        String mPackageName = null;
        String mTag = null;
        int mResetMode = -1;

        enum CommandVerb {
            UNSPECIFIED,
            GET,
            PUT,
            DELETE,
            LIST,
            RESET
        }

        MyShellCommand(SettingsProvider settingsProvider, boolean z) {
            this.mProvider = settingsProvider;
            this.mDumping = z;
        }

        public int onCommand(String str) {
            boolean z;
            if (str == null) {
                return handleDefaultCommands(str);
            }
            PrintWriter errPrintWriter = getErrPrintWriter();
            String nextArg = str;
            boolean z2 = false;
            do {
                z = true;
                if ("--user".equals(nextArg)) {
                    if (this.mUser != -1) {
                        break;
                    }
                    String nextArgRequired = getNextArgRequired();
                    if ("current".equals(nextArgRequired) || "cur".equals(nextArgRequired)) {
                        this.mUser = -2;
                    } else {
                        this.mUser = Integer.parseInt(nextArgRequired);
                    }
                } else if (this.mVerb == CommandVerb.UNSPECIFIED) {
                    if ("get".equalsIgnoreCase(nextArg)) {
                        this.mVerb = CommandVerb.GET;
                    } else if ("put".equalsIgnoreCase(nextArg)) {
                        this.mVerb = CommandVerb.PUT;
                    } else if ("delete".equalsIgnoreCase(nextArg)) {
                        this.mVerb = CommandVerb.DELETE;
                    } else if ("list".equalsIgnoreCase(nextArg)) {
                        this.mVerb = CommandVerb.LIST;
                    } else if ("reset".equalsIgnoreCase(nextArg)) {
                        this.mVerb = CommandVerb.RESET;
                    } else {
                        errPrintWriter.println("Invalid command: " + nextArg);
                        return -1;
                    }
                } else if (this.mTable == null) {
                    if (!"system".equalsIgnoreCase(nextArg) && !"secure".equalsIgnoreCase(nextArg) && !"global".equalsIgnoreCase(nextArg)) {
                        errPrintWriter.println("Invalid namespace '" + nextArg + "'");
                        return -1;
                    }
                    this.mTable = nextArg.toLowerCase();
                    if (this.mVerb == CommandVerb.LIST) {
                        break;
                    }
                } else if (this.mVerb == CommandVerb.RESET) {
                    if ("untrusted_defaults".equalsIgnoreCase(nextArg)) {
                        this.mResetMode = 2;
                    } else if ("untrusted_clear".equalsIgnoreCase(nextArg)) {
                        this.mResetMode = 3;
                    } else if ("trusted_defaults".equalsIgnoreCase(nextArg)) {
                        this.mResetMode = 4;
                    } else {
                        this.mPackageName = nextArg;
                        this.mResetMode = 1;
                        if (peekNextArg() != null) {
                            this.mTag = getNextArg();
                            if (peekNextArg() != null) {
                                errPrintWriter.println("Too many arguments");
                                return -1;
                            }
                        }
                    }
                    if (peekNextArg() != null) {
                        errPrintWriter.println("Too many arguments");
                        return -1;
                    }
                    z2 = true;
                } else if (this.mVerb == CommandVerb.GET || this.mVerb == CommandVerb.DELETE) {
                    this.mKey = nextArg;
                    if (peekNextArg() != null) {
                        errPrintWriter.println("Too many arguments");
                        return -1;
                    }
                } else if (this.mKey == null) {
                    this.mKey = nextArg;
                } else if (this.mValue == null) {
                    this.mValue = nextArg;
                    z2 = true;
                } else if (this.mTag == null) {
                    this.mTag = nextArg;
                    if ("default".equalsIgnoreCase(this.mTag)) {
                        this.mTag = null;
                        this.mMakeDefault = true;
                        if (peekNextArg() != null) {
                            errPrintWriter.println("Too many arguments");
                            return -1;
                        }
                    } else if (peekNextArg() == null) {
                        break;
                    }
                } else {
                    if (!"default".equalsIgnoreCase(nextArg)) {
                        errPrintWriter.println("Argument expected to be 'default'");
                        return -1;
                    }
                    this.mMakeDefault = true;
                    if (peekNextArg() != null) {
                        errPrintWriter.println("Too many arguments");
                        return -1;
                    }
                }
                nextArg = getNextArg();
            } while (nextArg != null);
            z = z2;
            if (!z) {
                errPrintWriter.println("Bad arguments");
                return -1;
            }
            if (this.mUser == -2) {
                try {
                    this.mUser = ActivityManager.getService().getCurrentUser().id;
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed in IPC", e);
                }
            }
            if (this.mUser < 0) {
                this.mUser = 0;
            } else if (this.mVerb == CommandVerb.DELETE || this.mVerb == CommandVerb.LIST) {
                errPrintWriter.println("--user not supported for delete and list.");
                return -1;
            }
            if (UserManager.get(this.mProvider.getContext()).getUserInfo(this.mUser) == null) {
                errPrintWriter.println("Invalid user: " + this.mUser);
                return -1;
            }
            IContentProvider iContentProvider = this.mProvider.getIContentProvider();
            PrintWriter outPrintWriter = getOutPrintWriter();
            switch (this.mVerb) {
                case GET:
                    outPrintWriter.println(getForUser(iContentProvider, this.mUser, this.mTable, this.mKey));
                    return 0;
                case PUT:
                    putForUser(iContentProvider, this.mUser, this.mTable, this.mKey, this.mValue, this.mTag, this.mMakeDefault);
                    return 0;
                case DELETE:
                    outPrintWriter.println("Deleted " + deleteForUser(iContentProvider, this.mUser, this.mTable, this.mKey) + " rows");
                    return 0;
                case LIST:
                    Iterator<String> it = listForUser(iContentProvider, this.mUser, this.mTable).iterator();
                    while (it.hasNext()) {
                        outPrintWriter.println(it.next());
                    }
                    return 0;
                case RESET:
                    resetForUser(iContentProvider, this.mUser, this.mTable, this.mTag);
                    return 0;
                default:
                    errPrintWriter.println("Unspecified command");
                    return -1;
            }
        }

        private List<String> listForUser(IContentProvider iContentProvider, int i, String str) {
            Uri uri;
            if ("system".equals(str)) {
                uri = Settings.System.CONTENT_URI;
            } else if ("secure".equals(str)) {
                uri = Settings.Secure.CONTENT_URI;
            } else {
                uri = "global".equals(str) ? Settings.Global.CONTENT_URI : null;
            }
            Uri uri2 = uri;
            ArrayList arrayList = new ArrayList();
            if (uri2 == null) {
                return arrayList;
            }
            try {
                Cursor cursorQuery = iContentProvider.query(resolveCallingPackage(), uri2, (String[]) null, (Bundle) null, (ICancellationSignal) null);
                while (cursorQuery != null) {
                    try {
                        if (!cursorQuery.moveToNext()) {
                            break;
                        }
                        arrayList.add(cursorQuery.getString(1) + "=" + cursorQuery.getString(2));
                    } finally {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    }
                }
                Collections.sort(arrayList);
                return arrayList;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed in IPC", e);
            }
        }

        String getForUser(IContentProvider iContentProvider, int i, String str, String str2) {
            String str3;
            if ("system".equals(str)) {
                str3 = "GET_system";
            } else if ("secure".equals(str)) {
                str3 = "GET_secure";
            } else {
                if (!"global".equals(str)) {
                    getErrPrintWriter().println("Invalid table; no put performed");
                    throw new IllegalArgumentException("Invalid table " + str);
                }
                str3 = "GET_global";
            }
            try {
                Bundle bundle = new Bundle();
                bundle.putInt("_user", i);
                Bundle bundleCall = iContentProvider.call(resolveCallingPackage(), str3, str2, bundle);
                if (bundleCall == null) {
                    return null;
                }
                return bundleCall.getPairValue();
            } catch (RemoteException e) {
                throw new RuntimeException("Failed in IPC", e);
            }
        }

        void putForUser(IContentProvider iContentProvider, int i, String str, String str2, String str3, String str4, boolean z) {
            String str5;
            if ("system".equals(str)) {
                str5 = "PUT_system";
                if (z) {
                    getOutPrintWriter().print("Ignored makeDefault - doesn't apply to system settings");
                    z = false;
                }
            } else if ("secure".equals(str)) {
                str5 = "PUT_secure";
            } else {
                if (!"global".equals(str)) {
                    getErrPrintWriter().println("Invalid table; no put performed");
                    return;
                }
                str5 = "PUT_global";
            }
            try {
                Bundle bundle = new Bundle();
                bundle.putString("value", str3);
                bundle.putInt("_user", i);
                if (str4 != null) {
                    bundle.putString("_tag", str4);
                }
                if (z) {
                    bundle.putBoolean("_make_default", true);
                }
                iContentProvider.call(resolveCallingPackage(), str5, str2, bundle);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed in IPC", e);
            }
        }

        int deleteForUser(IContentProvider iContentProvider, int i, String str, String str2) {
            Uri uriFor;
            if ("system".equals(str)) {
                uriFor = Settings.System.getUriFor(str2);
            } else if ("secure".equals(str)) {
                uriFor = Settings.Secure.getUriFor(str2);
            } else {
                if (!"global".equals(str)) {
                    getErrPrintWriter().println("Invalid table; no delete performed");
                    throw new IllegalArgumentException("Invalid table " + str);
                }
                uriFor = Settings.Global.getUriFor(str2);
            }
            try {
                return iContentProvider.delete(resolveCallingPackage(), uriFor, (String) null, (String[]) null);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed in IPC", e);
            }
        }

        void resetForUser(IContentProvider iContentProvider, int i, String str, String str2) {
            String str3;
            if ("secure".equals(str)) {
                str3 = "RESET_secure";
            } else {
                if (!"global".equals(str)) {
                    getErrPrintWriter().println("Invalid table; no reset performed");
                    return;
                }
                str3 = "RESET_global";
            }
            try {
                Bundle bundle = new Bundle();
                bundle.putInt("_user", i);
                bundle.putInt("_reset_mode", this.mResetMode);
                if (str2 != null) {
                    bundle.putString("_tag", str2);
                }
                String strResolveCallingPackage = this.mPackageName != null ? this.mPackageName : resolveCallingPackage();
                bundle.putInt("_user", i);
                iContentProvider.call(strResolveCallingPackage, str3, (String) null, bundle);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed in IPC", e);
            }
        }

        public static String resolveCallingPackage() {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 0) {
                return "root";
            }
            if (callingUid == 2000) {
                return "com.android.shell";
            }
            return null;
        }

        public void onHelp() {
            dumpHelp(getOutPrintWriter(), this.mDumping);
        }

        static void dumpHelp(PrintWriter printWriter, boolean z) {
            if (z) {
                printWriter.println("Settings provider dump options:");
                printWriter.println("  [-h] [--proto]");
                printWriter.println("  -h: print this help.");
                printWriter.println("  --proto: dump as protobuf.");
                return;
            }
            printWriter.println("Settings provider (settings) commands:");
            printWriter.println("  help");
            printWriter.println("      Print this help text.");
            printWriter.println("  get [--user <USER_ID> | current] NAMESPACE KEY");
            printWriter.println("      Retrieve the current value of KEY.");
            printWriter.println("  put [--user <USER_ID> | current] NAMESPACE KEY VALUE [TAG] [default]");
            printWriter.println("      Change the contents of KEY to VALUE.");
            printWriter.println("      TAG to associate with the setting.");
            printWriter.println("      {default} to set as the default, case-insensitive only for global/secure namespace");
            printWriter.println("  delete NAMESPACE KEY");
            printWriter.println("      Delete the entry for KEY.");
            printWriter.println("  reset [--user <USER_ID> | current] NAMESPACE {PACKAGE_NAME | RESET_MODE}");
            printWriter.println("      Reset the global/secure table for a package with mode.");
            printWriter.println("      RESET_MODE is one of {untrusted_defaults, untrusted_clear, trusted_defaults}, case-insensitive");
            printWriter.println("  list NAMESPACE");
            printWriter.println("      Print all defined keys.");
            printWriter.println("      NAMESPACE is one of {system, secure, global}, case-insensitive");
        }
    }
}
