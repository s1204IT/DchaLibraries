package com.android.commands.requestsync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;
import java.io.PrintStream;
import java.net.URISyntaxException;

public class RequestSync {
    private String[] mArgs;
    private String mCurArgData;
    private int mNextArg;
    private String mAccountName = null;
    private String mAccountType = null;
    private String mAuthority = null;
    private Bundle mExtras = new Bundle();

    public static void main(String[] args) {
        try {
            new RequestSync().run(args);
        } catch (IllegalArgumentException e) {
            showUsage();
            System.err.println("Error: " + e);
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private void run(String[] args) throws Exception {
        this.mArgs = args;
        this.mNextArg = 0;
        boolean ok = parseArgs();
        if (ok) {
            Account account = (this.mAccountName == null || this.mAccountType == null) ? null : new Account(this.mAccountName, this.mAccountType);
            System.out.printf("Requesting sync for: \n", new Object[0]);
            if (account != null) {
                System.out.printf("  Account: %s (%s)\n", account.name, account.type);
            } else {
                System.out.printf("  Account: all\n", new Object[0]);
            }
            PrintStream printStream = System.out;
            Object[] objArr = new Object[1];
            objArr[0] = this.mAuthority != null ? this.mAuthority : "All";
            printStream.printf("  Authority: %s\n", objArr);
            if (this.mExtras.size() > 0) {
                System.out.printf("  Extras:\n", new Object[0]);
                for (String key : this.mExtras.keySet()) {
                    System.out.printf("    %s: %s\n", key, this.mExtras.get(key));
                }
            }
            ContentResolver.requestSync(account, this.mAuthority, this.mExtras);
        }
    }

    private boolean parseArgs() throws URISyntaxException {
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("-h") || opt.equals("--help")) {
                    break;
                }
                if (opt.equals("-n") || opt.equals("--account-name")) {
                    this.mAccountName = nextArgRequired();
                } else if (opt.equals("-t") || opt.equals("--account-type")) {
                    this.mAccountType = nextArgRequired();
                } else if (opt.equals("-a") || opt.equals("--authority")) {
                    this.mAuthority = nextArgRequired();
                } else if (opt.equals("--is") || opt.equals("--ignore-settings")) {
                    this.mExtras.putBoolean("ignore_settings", true);
                } else if (opt.equals("--ib") || opt.equals("--ignore-backoff")) {
                    this.mExtras.putBoolean("ignore_backoff", true);
                } else if (opt.equals("--dd") || opt.equals("--discard-deletions")) {
                    this.mExtras.putBoolean("discard_deletions", true);
                } else if (opt.equals("--nr") || opt.equals("--no-retry")) {
                    this.mExtras.putBoolean("do_not_retry", true);
                } else if (opt.equals("--ex") || opt.equals("--expedited")) {
                    this.mExtras.putBoolean("expedited", true);
                } else if (opt.equals("-i") || opt.equals("--initialize")) {
                    this.mExtras.putBoolean("initialize", true);
                } else if (opt.equals("-m") || opt.equals("--manual")) {
                    this.mExtras.putBoolean("force", true);
                } else if (opt.equals("--od") || opt.equals("--override-deletions")) {
                    this.mExtras.putBoolean("deletions_override", true);
                } else if (opt.equals("-u") || opt.equals("--upload-only")) {
                    this.mExtras.putBoolean("upload", true);
                } else if (opt.equals("-e") || opt.equals("--es") || opt.equals("--extra-string")) {
                    String key = nextArgRequired();
                    String value = nextArgRequired();
                    this.mExtras.putString(key, value);
                } else if (opt.equals("--esn") || opt.equals("--extra-string-null")) {
                    String key2 = nextArgRequired();
                    this.mExtras.putString(key2, null);
                } else if (opt.equals("--ei") || opt.equals("--extra-int")) {
                    String key3 = nextArgRequired();
                    String value2 = nextArgRequired();
                    this.mExtras.putInt(key3, Integer.valueOf(value2).intValue());
                } else if (opt.equals("--el") || opt.equals("--extra-long")) {
                    String key4 = nextArgRequired();
                    String value3 = nextArgRequired();
                    this.mExtras.putLong(key4, Long.valueOf(value3).longValue());
                } else if (opt.equals("--ef") || opt.equals("--extra-float")) {
                    String key5 = nextArgRequired();
                    String value4 = nextArgRequired();
                    this.mExtras.putFloat(key5, Long.valueOf(value4).longValue());
                } else if (opt.equals("--ed") || opt.equals("--extra-double")) {
                    String key6 = nextArgRequired();
                    String value5 = nextArgRequired();
                    this.mExtras.putFloat(key6, Long.valueOf(value5).longValue());
                } else if (opt.equals("--ez") || opt.equals("--extra-bool")) {
                    String key7 = nextArgRequired();
                    String value6 = nextArgRequired();
                    this.mExtras.putBoolean(key7, Boolean.valueOf(value6).booleanValue());
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    showUsage();
                    return false;
                }
            } else {
                if (this.mNextArg >= this.mArgs.length) {
                    return true;
                }
                showUsage();
                return false;
            }
        }
    }

    private String nextOption() {
        if (this.mCurArgData != null) {
            String prev = this.mArgs[this.mNextArg - 1];
            throw new IllegalArgumentException("No argument expected after \"" + prev + "\"");
        }
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String arg = this.mArgs[this.mNextArg];
        if (!arg.startsWith("-")) {
            return null;
        }
        this.mNextArg++;
        if (arg.equals("--")) {
            return null;
        }
        if (arg.length() > 1 && arg.charAt(1) != '-') {
            if (arg.length() > 2) {
                this.mCurArgData = arg.substring(2);
                return arg.substring(0, 2);
            }
            this.mCurArgData = null;
            return arg;
        }
        this.mCurArgData = null;
        return arg;
    }

    private String nextArg() {
        if (this.mCurArgData != null) {
            String arg = this.mCurArgData;
            this.mCurArgData = null;
            return arg;
        }
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String[] strArr = this.mArgs;
        int i = this.mNextArg;
        this.mNextArg = i + 1;
        String arg2 = strArr[i];
        return arg2;
    }

    private String nextArgRequired() {
        String arg = nextArg();
        if (arg == null) {
            String prev = this.mArgs[this.mNextArg - 1];
            throw new IllegalArgumentException("Argument expected after \"" + prev + "\"");
        }
        return arg;
    }

    private static void showUsage() {
        System.err.println("usage: requestsync [options]\nWith no options, a sync will be requested for all account and all sync\nauthorities with no extras. Options can be:\n    -h|--help: Display this message\n    -n|--account-name <ACCOUNT-NAME>\n    -t|--account-type <ACCOUNT-TYPE>\n    -a|--authority <AUTHORITY>\n  Add ContentResolver extras:\n    --is|--ignore-settings: Add SYNC_EXTRAS_IGNORE_SETTINGS\n    --ib|--ignore-backoff: Add SYNC_EXTRAS_IGNORE_BACKOFF\n    --dd|--discard-deletions: Add SYNC_EXTRAS_DISCARD_LOCAL_DELETIONS\n    --nr|--no-retry: Add SYNC_EXTRAS_DO_NOT_RETRY\n    --ex|--expedited: Add SYNC_EXTRAS_EXPEDITED\n    --i|--initialize: Add SYNC_EXTRAS_INITIALIZE\n    --m|--manual: Add SYNC_EXTRAS_MANUAL\n    --od|--override-deletions: Add SYNC_EXTRAS_OVERRIDE_TOO_MANY_DELETIONS\n    --u|--upload-only: Add SYNC_EXTRAS_UPLOAD\n  Add custom extras:\n    -e|--es|--extra-string <KEY> <VALUE>\n    --esn|--extra-string-null <KEY>\n    --ei|--extra-int <KEY> <VALUE>\n    --el|--extra-long <KEY> <VALUE>\n    --ef|--extra-float <KEY> <VALUE>\n    --ed|--extra-double <KEY> <VALUE>\n    --ez|--extra-bool <KEY> <VALUE>\n");
    }
}
