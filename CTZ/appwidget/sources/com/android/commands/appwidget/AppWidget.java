package com.android.commands.appwidget;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import com.android.internal.appwidget.IAppWidgetService;

public class AppWidget {
    private static final String USAGE = "usage: adb shell appwidget [subcommand] [options]\n\nusage: adb shell appwidget grantbind --package <PACKAGE>  [--user <USER_ID> | current]\n  <PACKAGE> an Android package name.\n  <USER_ID> The user id under which the package is installed.\n  Example:\n  # Grant the \"foo.bar.baz\" package to bind app widgets for the current user.\n  adb shell grantbind --package foo.bar.baz --user current\n\nusage: adb shell appwidget revokebind --package <PACKAGE> [--user <USER_ID> | current]\n  <PACKAGE> an Android package name.\n  <USER_ID> The user id under which the package is installed.\n  Example:\n  # Revoke the permisison to bind app widgets from the \"foo.bar.baz\" package.\n  adb shell revokebind --package foo.bar.baz --user current\n\n";

    private static class Parser {
        private static final String ARGUMENT_GRANT_BIND = "grantbind";
        private static final String ARGUMENT_PACKAGE = "--package";
        private static final String ARGUMENT_PREFIX = "--";
        private static final String ARGUMENT_REVOKE_BIND = "revokebind";
        private static final String ARGUMENT_USER = "--user";
        private static final String VALUE_USER_CURRENT = "current";
        private final Tokenizer mTokenizer;

        public Parser(String[] strArr) {
            this.mTokenizer = new Tokenizer(strArr);
        }

        public Runnable parseCommand() {
            try {
                String strNextArg = this.mTokenizer.nextArg();
                if (ARGUMENT_GRANT_BIND.equals(strNextArg)) {
                    return parseSetGrantBindAppWidgetPermissionCommand(true);
                }
                if (ARGUMENT_REVOKE_BIND.equals(strNextArg)) {
                    return parseSetGrantBindAppWidgetPermissionCommand(false);
                }
                throw new IllegalArgumentException("Unsupported operation: " + strNextArg);
            } catch (IllegalArgumentException e) {
                System.out.println(AppWidget.USAGE);
                System.out.println("[ERROR] " + e.getMessage());
                return null;
            }
        }

        private SetBindAppWidgetPermissionCommand parseSetGrantBindAppWidgetPermissionCommand(boolean z) {
            String strArgumentValueRequired = null;
            int i = 0;
            while (true) {
                String strNextArg = this.mTokenizer.nextArg();
                if (strNextArg != null) {
                    if (ARGUMENT_PACKAGE.equals(strNextArg)) {
                        strArgumentValueRequired = argumentValueRequired(strNextArg);
                    } else if (ARGUMENT_USER.equals(strNextArg)) {
                        String strArgumentValueRequired2 = argumentValueRequired(strNextArg);
                        if (VALUE_USER_CURRENT.equals(strArgumentValueRequired2)) {
                            i = -2;
                        } else {
                            i = Integer.parseInt(strArgumentValueRequired2);
                        }
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + strNextArg);
                    }
                } else {
                    if (strArgumentValueRequired == null) {
                        throw new IllegalArgumentException("Package name not specified. Did you specify --package argument?");
                    }
                    return new SetBindAppWidgetPermissionCommand(strArgumentValueRequired, z, i);
                }
            }
        }

        private String argumentValueRequired(String str) {
            String strNextArg = this.mTokenizer.nextArg();
            if (TextUtils.isEmpty(strNextArg) || strNextArg.startsWith(ARGUMENT_PREFIX)) {
                throw new IllegalArgumentException("No value for argument: " + str);
            }
            return strNextArg;
        }
    }

    private static class Tokenizer {
        private final String[] mArgs;
        private int mNextArg;

        public Tokenizer(String[] strArr) {
            this.mArgs = strArr;
        }

        private String nextArg() {
            if (this.mNextArg < this.mArgs.length) {
                String[] strArr = this.mArgs;
                int i = this.mNextArg;
                this.mNextArg = i + 1;
                return strArr[i];
            }
            return null;
        }
    }

    private static class SetBindAppWidgetPermissionCommand implements Runnable {
        final boolean mGranted;
        final String mPackageName;
        final int mUserId;

        public SetBindAppWidgetPermissionCommand(String str, boolean z, int i) {
            this.mPackageName = str;
            this.mGranted = z;
            this.mUserId = i;
        }

        @Override
        public void run() {
            try {
                IAppWidgetService.Stub.asInterface(ServiceManager.getService("appwidget")).setBindAppWidgetPermission(this.mPackageName, this.mUserId, this.mGranted);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] strArr) {
        Runnable command = new Parser(strArr).parseCommand();
        if (command != null) {
            command.run();
        }
    }
}
