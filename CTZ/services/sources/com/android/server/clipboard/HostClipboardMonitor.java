package com.android.server.clipboard;

import android.util.Slog;
import java.io.IOException;
import java.io.RandomAccessFile;

class HostClipboardMonitor implements Runnable {
    private static final String PIPE_DEVICE = "/dev/qemu_pipe";
    private static final String PIPE_NAME = "pipe:clipboard";
    private HostClipboardCallback mHostClipboardCallback;
    private RandomAccessFile mPipe = null;

    public interface HostClipboardCallback {
        void onHostClipboardUpdated(String str);
    }

    private void openPipe() {
        try {
            byte[] bArr = new byte[PIPE_NAME.length() + 1];
            bArr[PIPE_NAME.length()] = 0;
            System.arraycopy(PIPE_NAME.getBytes(), 0, bArr, 0, PIPE_NAME.length());
            this.mPipe = new RandomAccessFile(PIPE_DEVICE, "rw");
            this.mPipe.write(bArr);
        } catch (IOException e) {
            try {
                if (this.mPipe != null) {
                    this.mPipe.close();
                }
            } catch (IOException e2) {
            }
            this.mPipe = null;
        }
    }

    public HostClipboardMonitor(HostClipboardCallback hostClipboardCallback) {
        this.mHostClipboardCallback = hostClipboardCallback;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            while (this.mPipe == null) {
                try {
                    openPipe();
                    Thread.sleep(100L);
                } catch (IOException e) {
                    try {
                        this.mPipe.close();
                    } catch (IOException e2) {
                    }
                    this.mPipe = null;
                } catch (InterruptedException e3) {
                }
            }
            byte[] bArr = new byte[Integer.reverseBytes(this.mPipe.readInt())];
            this.mPipe.readFully(bArr);
            this.mHostClipboardCallback.onHostClipboardUpdated(new String(bArr));
        }
    }

    public void setHostClipboard(String str) {
        try {
            if (this.mPipe != null) {
                this.mPipe.writeInt(Integer.reverseBytes(str.getBytes().length));
                this.mPipe.write(str.getBytes());
            }
        } catch (IOException e) {
            Slog.e("HostClipboardMonitor", "Failed to set host clipboard " + e.getMessage());
        }
    }
}
