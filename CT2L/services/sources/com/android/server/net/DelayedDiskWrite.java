package com.android.server.net;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class DelayedDiskWrite {
    private Handler mDiskWriteHandler;
    private HandlerThread mDiskWriteHandlerThread;
    private int mWriteSequence = 0;
    private final String TAG = "DelayedDiskWrite";

    public interface Writer {
        void onWriteCalled(DataOutputStream dataOutputStream) throws IOException;
    }

    public void write(final String filePath, final Writer w) {
        if (TextUtils.isEmpty(filePath)) {
            throw new IllegalArgumentException("empty file path");
        }
        synchronized (this) {
            int i = this.mWriteSequence + 1;
            this.mWriteSequence = i;
            if (i == 1) {
                this.mDiskWriteHandlerThread = new HandlerThread("DelayedDiskWriteThread");
                this.mDiskWriteHandlerThread.start();
                this.mDiskWriteHandler = new Handler(this.mDiskWriteHandlerThread.getLooper());
            }
        }
        this.mDiskWriteHandler.post(new Runnable() {
            @Override
            public void run() throws Throwable {
                DelayedDiskWrite.this.doWrite(filePath, w);
            }
        });
    }

    private void doWrite(String filePath, Writer w) throws Throwable {
        DataOutputStream out;
        DataOutputStream out2 = null;
        try {
            try {
                out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
            } catch (IOException e) {
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            w.onWriteCalled(out);
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e2) {
                }
            }
            synchronized (this) {
                int i = this.mWriteSequence - 1;
                this.mWriteSequence = i;
                if (i == 0) {
                    this.mDiskWriteHandler.getLooper().quit();
                    this.mDiskWriteHandler = null;
                    this.mDiskWriteHandlerThread = null;
                }
            }
        } catch (IOException e3) {
            out2 = out;
            loge("Error writing data file " + filePath);
            if (out2 != null) {
                try {
                    out2.close();
                } catch (Exception e4) {
                }
            }
            synchronized (this) {
                int i2 = this.mWriteSequence - 1;
                this.mWriteSequence = i2;
                if (i2 == 0) {
                    this.mDiskWriteHandler.getLooper().quit();
                    this.mDiskWriteHandler = null;
                    this.mDiskWriteHandlerThread = null;
                }
            }
        } catch (Throwable th2) {
            th = th2;
            out2 = out;
            if (out2 != null) {
                try {
                    out2.close();
                } catch (Exception e5) {
                }
            }
            synchronized (this) {
                int i3 = this.mWriteSequence - 1;
                this.mWriteSequence = i3;
                if (i3 == 0) {
                    this.mDiskWriteHandler.getLooper().quit();
                    this.mDiskWriteHandler = null;
                    this.mDiskWriteHandlerThread = null;
                }
            }
            throw th;
        }
    }

    private void loge(String s) {
        Log.e("DelayedDiskWrite", s);
    }
}
