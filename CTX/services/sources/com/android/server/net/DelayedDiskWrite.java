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

    public void write(String str, Writer writer) {
        write(str, writer, true);
    }

    public void write(final String str, final Writer writer, final boolean z) {
        if (TextUtils.isEmpty(str)) {
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
                DelayedDiskWrite.this.doWrite(str, writer, z);
            }
        });
    }

    private void doWrite(String str, Writer writer, boolean z) throws Throwable {
        try {
            if (z != 0) {
                try {
                    z = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(str)));
                } catch (IOException e) {
                    z = 0;
                    loge("Error writing data file " + str);
                    if (z != 0) {
                    }
                    synchronized (this) {
                    }
                    throw th;
                } catch (Throwable th) {
                    th = th;
                    z = 0;
                    if (z != 0) {
                        try {
                            z.close();
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
                        throw th;
                    }
                }
            } else {
                z = 0;
            }
            try {
                writer.onWriteCalled(z);
                if (z != 0) {
                    try {
                        z.close();
                    } catch (Exception e3) {
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
            } catch (IOException e4) {
                loge("Error writing data file " + str);
                if (z != 0) {
                    try {
                        z.close();
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
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void loge(String str) {
        Log.e("DelayedDiskWrite", str);
    }
}
