package android.os;

import android.provider.ContactsContract;
import com.android.internal.util.FastPrintWriter;
import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public abstract class ShellCommand {
    static final boolean DEBUG = false;
    static final String TAG = "ShellCommand";
    private int mArgPos;
    private String[] mArgs;
    private String mCmd;
    private String mCurArgData;
    private FileDescriptor mErr;
    private FastPrintWriter mErrPrintWriter;
    private FileOutputStream mFileErr;
    private FileInputStream mFileIn;
    private FileOutputStream mFileOut;
    private FileDescriptor mIn;
    private InputStream mInputStream;
    private FileDescriptor mOut;
    private FastPrintWriter mOutPrintWriter;
    private ResultReceiver mResultReceiver;
    private Binder mTarget;

    public abstract int onCommand(String str);

    public abstract void onHelp();

    public void init(Binder target, FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, int firstArgPos) {
        this.mTarget = target;
        this.mIn = in;
        this.mOut = out;
        this.mErr = err;
        this.mArgs = args;
        this.mResultReceiver = null;
        this.mCmd = null;
        this.mArgPos = firstArgPos;
        this.mCurArgData = null;
        this.mFileIn = null;
        this.mFileOut = null;
        this.mFileErr = null;
        this.mOutPrintWriter = null;
        this.mErrPrintWriter = null;
        this.mInputStream = null;
    }

    public int exec(Binder target, FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) {
        String cmd;
        int start;
        if (args == null || args.length <= 0) {
            cmd = null;
            start = 0;
        } else {
            cmd = args[0];
            start = 1;
        }
        init(target, in, out, err, args, start);
        this.mCmd = cmd;
        this.mResultReceiver = resultReceiver;
        int res = -1;
        try {
            try {
                res = onCommand(this.mCmd);
                if (this.mOutPrintWriter != null) {
                    this.mOutPrintWriter.flush();
                }
                if (this.mErrPrintWriter != null) {
                    this.mErrPrintWriter.flush();
                }
                this.mResultReceiver.send(res, null);
            } catch (SecurityException e) {
                PrintWriter eout = getErrPrintWriter();
                eout.println("Security exception: " + e.getMessage());
                eout.println();
                e.printStackTrace(eout);
                if (this.mOutPrintWriter != null) {
                    this.mOutPrintWriter.flush();
                }
                if (this.mErrPrintWriter != null) {
                    this.mErrPrintWriter.flush();
                }
                this.mResultReceiver.send(-1, null);
            } catch (Throwable e2) {
                PrintWriter eout2 = getErrPrintWriter();
                eout2.println();
                eout2.println("Exception occurred while dumping:");
                e2.printStackTrace(eout2);
                if (this.mOutPrintWriter != null) {
                    this.mOutPrintWriter.flush();
                }
                if (this.mErrPrintWriter != null) {
                    this.mErrPrintWriter.flush();
                }
                this.mResultReceiver.send(-1, null);
            }
            return res;
        } catch (Throwable th) {
            if (this.mOutPrintWriter != null) {
                this.mOutPrintWriter.flush();
            }
            if (this.mErrPrintWriter != null) {
                this.mErrPrintWriter.flush();
            }
            this.mResultReceiver.send(res, null);
            throw th;
        }
    }

    public OutputStream getRawOutputStream() {
        if (this.mFileOut == null) {
            this.mFileOut = new FileOutputStream(this.mOut);
        }
        return this.mFileOut;
    }

    public PrintWriter getOutPrintWriter() {
        if (this.mOutPrintWriter == null) {
            this.mOutPrintWriter = new FastPrintWriter(getRawOutputStream());
        }
        return this.mOutPrintWriter;
    }

    public OutputStream getRawErrorStream() {
        if (this.mFileErr == null) {
            this.mFileErr = new FileOutputStream(this.mErr);
        }
        return this.mFileErr;
    }

    public PrintWriter getErrPrintWriter() {
        if (this.mErr == null) {
            return getOutPrintWriter();
        }
        if (this.mErrPrintWriter == null) {
            this.mErrPrintWriter = new FastPrintWriter(getRawErrorStream());
        }
        return this.mErrPrintWriter;
    }

    public InputStream getRawInputStream() {
        if (this.mFileIn == null) {
            this.mFileIn = new FileInputStream(this.mIn);
        }
        return this.mFileIn;
    }

    public InputStream getBufferedInputStream() {
        if (this.mInputStream == null) {
            this.mInputStream = new BufferedInputStream(getRawInputStream());
        }
        return this.mInputStream;
    }

    public String getNextOption() {
        if (this.mCurArgData != null) {
            String prev = this.mArgs[this.mArgPos - 1];
            throw new IllegalArgumentException("No argument expected after \"" + prev + "\"");
        }
        if (this.mArgPos >= this.mArgs.length) {
            return null;
        }
        String arg = this.mArgs[this.mArgPos];
        if (!arg.startsWith(ContactsContract.Aas.ENCODE_SYMBOL)) {
            return null;
        }
        this.mArgPos++;
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

    public String getNextArg() {
        if (this.mCurArgData != null) {
            String arg = this.mCurArgData;
            this.mCurArgData = null;
            return arg;
        }
        if (this.mArgPos >= this.mArgs.length) {
            return null;
        }
        String[] strArr = this.mArgs;
        int i = this.mArgPos;
        this.mArgPos = i + 1;
        return strArr[i];
    }

    public String peekNextArg() {
        if (this.mCurArgData != null) {
            return this.mCurArgData;
        }
        if (this.mArgPos < this.mArgs.length) {
            return this.mArgs[this.mArgPos];
        }
        return null;
    }

    public String getNextArgRequired() {
        String arg = getNextArg();
        if (arg == null) {
            String prev = this.mArgs[this.mArgPos - 1];
            throw new IllegalArgumentException("Argument expected after \"" + prev + "\"");
        }
        return arg;
    }

    public int handleDefaultCommands(String cmd) {
        if ("dump".equals(cmd)) {
            String[] newArgs = new String[this.mArgs.length - 1];
            System.arraycopy(this.mArgs, 1, newArgs, 0, this.mArgs.length - 1);
            this.mTarget.doDump(this.mOut, getOutPrintWriter(), newArgs);
            return 0;
        }
        if (cmd == null || "help".equals(cmd) || "-h".equals(cmd)) {
            onHelp();
            return -1;
        }
        getOutPrintWriter().println("Unknown command: " + cmd);
        return -1;
    }
}
