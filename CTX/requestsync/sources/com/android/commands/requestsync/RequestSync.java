package com.android.commands.requestsync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncRequest;
import android.os.Bundle;
import java.io.PrintStream;
import java.net.URISyntaxException;

public class RequestSync {
    private Account mAccount;
    private String mAccountName;
    private String mAccountType;
    private String[] mArgs;
    private String mAuthority;
    private String mCurArgData;
    private int mExemptionFlag = 0;
    private Bundle mExtras = new Bundle();
    private int mNextArg;
    private Operation mOperation;
    private int mPeriodicIntervalSeconds;

    enum Operation {
        REQUEST_SYNC {
            @Override
            void invoke(RequestSync requestSync) {
                int i = requestSync.mExemptionFlag;
                requestSync.mExtras.putInt("v_exemption", i);
                if (i == 0) {
                    System.out.println("Making a sync request as a background app.\nNote: request may be throttled by App Standby.\nTo override this behavior and run a sync immediately, pass a -f or -F option (use -h for help).\n");
                }
                ContentResolver.requestSync(new SyncRequest.Builder().setSyncAdapter(requestSync.mAccount, requestSync.mAuthority).setExtras(requestSync.mExtras).syncOnce().build());
            }
        },
        ADD_PERIODIC_SYNC {
            @Override
            void invoke(RequestSync requestSync) {
                ContentResolver.addPeriodicSync(requestSync.mAccount, requestSync.mAuthority, requestSync.mExtras, requestSync.mPeriodicIntervalSeconds);
            }
        },
        REMOVE_PERIODIC_SYNC {
            @Override
            void invoke(RequestSync requestSync) {
                ContentResolver.removePeriodicSync(requestSync.mAccount, requestSync.mAuthority, requestSync.mExtras);
            }
        };

        abstract void invoke(RequestSync requestSync);
    }

    public static void main(String[] strArr) {
        try {
            new RequestSync().run(strArr);
        } catch (IllegalArgumentException e) {
            showUsage();
            System.err.println("Error: " + e);
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private void run(String[] strArr) throws Exception {
        this.mArgs = strArr;
        this.mNextArg = 0;
        if (parseArgs()) {
            Account account = (this.mAccountName == null || this.mAccountType == null) ? null : new Account(this.mAccountName, this.mAccountType);
            System.out.printf("Requesting sync for: \n", new Object[0]);
            if (account == null) {
                System.out.printf("  Account: all\n", new Object[0]);
            } else {
                System.out.printf("  Account: %s (%s)\n", account.name, account.type);
            }
            PrintStream printStream = System.out;
            Object[] objArr = new Object[1];
            objArr[0] = this.mAuthority != null ? this.mAuthority : "All";
            printStream.printf("  Authority: %s\n", objArr);
            if (this.mExtras.size() > 0) {
                System.out.printf("  Extras:\n", new Object[0]);
                for (String str : this.mExtras.keySet()) {
                    System.out.printf("    %s: %s\n", str, this.mExtras.get(str));
                }
            }
            this.mAccount = account;
            this.mOperation.invoke(this);
        }
    }

    private boolean parseArgs() throws URISyntaxException {
        this.mOperation = Operation.REQUEST_SYNC;
        if (this.mArgs.length > 0) {
            String str = this.mArgs[0];
            byte b = -1;
            int iHashCode = str.hashCode();
            if (iHashCode != -1439021497) {
                if (iHashCode == 810481092 && str.equals("remove-periodic")) {
                    b = 1;
                }
            } else if (str.equals("add-periodic")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    this.mNextArg++;
                    this.mOperation = Operation.ADD_PERIODIC_SYNC;
                    this.mPeriodicIntervalSeconds = Integer.parseInt(nextArgRequired());
                    break;
                case 1:
                    this.mNextArg++;
                    this.mOperation = Operation.REMOVE_PERIODIC_SYNC;
                    break;
            }
        }
        while (true) {
            String strNextOption = nextOption();
            if (strNextOption != null) {
                if (!strNextOption.equals("-h") && !strNextOption.equals("--help")) {
                    if (strNextOption.equals("-n") || strNextOption.equals("--account-name")) {
                        this.mAccountName = nextArgRequired();
                    } else if (strNextOption.equals("-t") || strNextOption.equals("--account-type")) {
                        this.mAccountType = nextArgRequired();
                    } else if (strNextOption.equals("-a") || strNextOption.equals("--authority")) {
                        this.mAuthority = nextArgRequired();
                    } else if (strNextOption.equals("--is") || strNextOption.equals("--ignore-settings")) {
                        this.mExtras.putBoolean("ignore_settings", true);
                    } else if (strNextOption.equals("--ib") || strNextOption.equals("--ignore-backoff")) {
                        this.mExtras.putBoolean("ignore_backoff", true);
                    } else if (strNextOption.equals("--dd") || strNextOption.equals("--discard-deletions")) {
                        this.mExtras.putBoolean("discard_deletions", true);
                    } else if (strNextOption.equals("--nr") || strNextOption.equals("--no-retry")) {
                        this.mExtras.putBoolean("do_not_retry", true);
                    } else if (strNextOption.equals("--ex") || strNextOption.equals("--expedited")) {
                        this.mExtras.putBoolean("expedited", true);
                    } else if (strNextOption.equals("-i") || strNextOption.equals("--initialize")) {
                        this.mExtras.putBoolean("initialize", true);
                    } else if (strNextOption.equals("-m") || strNextOption.equals("--manual")) {
                        this.mExtras.putBoolean("force", true);
                    } else if (strNextOption.equals("--od") || strNextOption.equals("--override-deletions")) {
                        this.mExtras.putBoolean("deletions_override", true);
                    } else if (strNextOption.equals("-u") || strNextOption.equals("--upload-only")) {
                        this.mExtras.putBoolean("upload", true);
                    } else if (strNextOption.equals("--rc") || strNextOption.equals("--require-charging")) {
                        this.mExtras.putBoolean("require_charging", true);
                    } else if (strNextOption.equals("-e") || strNextOption.equals("--es") || strNextOption.equals("--extra-string")) {
                        this.mExtras.putString(nextArgRequired(), nextArgRequired());
                    } else if (strNextOption.equals("--esn") || strNextOption.equals("--extra-string-null")) {
                        this.mExtras.putString(nextArgRequired(), null);
                    } else if (strNextOption.equals("--ei") || strNextOption.equals("--extra-int")) {
                        this.mExtras.putInt(nextArgRequired(), Integer.valueOf(nextArgRequired()).intValue());
                    } else if (strNextOption.equals("--el") || strNextOption.equals("--extra-long")) {
                        this.mExtras.putLong(nextArgRequired(), Long.parseLong(nextArgRequired()));
                    } else if (strNextOption.equals("--ef") || strNextOption.equals("--extra-float")) {
                        this.mExtras.putFloat(nextArgRequired(), Long.parseLong(nextArgRequired()));
                    } else if (strNextOption.equals("--ed") || strNextOption.equals("--extra-double")) {
                        this.mExtras.putFloat(nextArgRequired(), Long.parseLong(nextArgRequired()));
                    } else if (strNextOption.equals("--ez") || strNextOption.equals("--extra-bool")) {
                        this.mExtras.putBoolean(nextArgRequired(), Boolean.valueOf(nextArgRequired()).booleanValue());
                    } else if (strNextOption.equals("-f") || strNextOption.equals("--foreground")) {
                        this.mExemptionFlag = 1;
                    } else if (strNextOption.equals("-F") || strNextOption.equals("--top")) {
                        this.mExemptionFlag = 2;
                    } else {
                        System.err.println("Error: Unknown option: " + strNextOption);
                        showUsage();
                        return false;
                    }
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
            throw new IllegalArgumentException("No argument expected after \"" + this.mArgs[this.mNextArg - 1] + "\"");
        }
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String str = this.mArgs[this.mNextArg];
        if (!str.startsWith("-")) {
            return null;
        }
        this.mNextArg++;
        if (str.equals("--")) {
            return null;
        }
        if (str.length() > 1 && str.charAt(1) != '-') {
            if (str.length() > 2) {
                this.mCurArgData = str.substring(2);
                return str.substring(0, 2);
            }
            this.mCurArgData = null;
            return str;
        }
        this.mCurArgData = null;
        return str;
    }

    private String nextArg() {
        if (this.mCurArgData != null) {
            String str = this.mCurArgData;
            this.mCurArgData = null;
            return str;
        }
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String[] strArr = this.mArgs;
        int i = this.mNextArg;
        this.mNextArg = i + 1;
        return strArr[i];
    }

    private String nextArgRequired() {
        String strNextArg = nextArg();
        if (strNextArg == null) {
            throw new IllegalArgumentException("Argument expected after \"" + this.mArgs[this.mNextArg - 1] + "\"");
        }
        return strNextArg;
    }

    private static void showUsage() {
        System.err.println("Usage:\n\n  requestsync [options]\n    With no options, a sync will be requested for all account and all sync\n    authorities with no extras.\n    Basic options:\n       -h|--help: Display this message\n       -n|--account-name <ACCOUNT-NAME>\n       -t|--account-type <ACCOUNT-TYPE>\n       -a|--authority <AUTHORITY>\n    App-standby related options\n\n       -f|--foreground (cause WORKING_SET, FREQUENT sync adapters to run immediately)\n       -F|--top (cause even RARE sync adapters to run immediately)\n    ContentResolver extra options:\n      --is|--ignore-settings: Add SYNC_EXTRAS_IGNORE_SETTINGS\n      --ib|--ignore-backoff: Add SYNC_EXTRAS_IGNORE_BACKOFF\n      --dd|--discard-deletions: Add SYNC_EXTRAS_DISCARD_LOCAL_DELETIONS\n      --nr|--no-retry: Add SYNC_EXTRAS_DO_NOT_RETRY\n      --ex|--expedited: Add SYNC_EXTRAS_EXPEDITED\n      -i|--initialize: Add SYNC_EXTRAS_INITIALIZE\n      --m|--manual: Add SYNC_EXTRAS_MANUAL\n      --od|--override-deletions: Add SYNC_EXTRAS_OVERRIDE_TOO_MANY_DELETIONS\n      -u|--upload-only: Add SYNC_EXTRAS_UPLOAD\n      --rc|--require-charging: Add SYNC_EXTRAS_REQUIRE_CHARGING\n    Custom extra options:\n      -e|--es|--extra-string <KEY> <VALUE>\n      --esn|--extra-string-null <KEY>\n      --ei|--extra-int <KEY> <VALUE>\n      --el|--extra-long <KEY> <VALUE>\n      --ef|--extra-float <KEY> <VALUE>\n      --ed|--extra-double <KEY> <VALUE>\n      --ez|--extra-bool <KEY> <VALUE>\n\n  requestsync add-periodic INTERVAL-SECOND [options]\n  requestsync remove-periodic [options]\n    Mandatory options:\n      -n|--account-name <ACCOUNT-NAME>\n      -t|--account-type <ACCOUNT-TYPE>\n      -a|--authority <AUTHORITY>\n    Also takes the above extra options.\n");
    }
}
