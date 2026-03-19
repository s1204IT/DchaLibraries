package java.lang;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

final class UNIXProcess extends Process {
    private static final Executor processReaperExecutor = (Executor) AccessController.doPrivileged(new PrivilegedAction<Executor>() {
        @Override
        public Executor run() {
            return Executors.newCachedThreadPool(new ProcessReaperThreadFactory());
        }
    });
    private int exitcode;
    private boolean hasExited;
    private final int pid;
    private InputStream stderr;
    private OutputStream stdin;
    private InputStream stdout;

    private static native void destroyProcess(int i);

    private native int forkAndExec(byte[] bArr, byte[] bArr2, int i, byte[] bArr3, int i2, byte[] bArr4, int[] iArr, boolean z) throws IOException;

    private static native void initIDs();

    private native int waitForProcessExit(int i);

    private static class ProcessReaperThreadFactory implements ThreadFactory {
        private static final ThreadGroup group = getRootThreadGroup();

        private ProcessReaperThreadFactory() {
        }

        private static ThreadGroup getRootThreadGroup() {
            return (ThreadGroup) AccessController.doPrivileged(new PrivilegedAction<ThreadGroup>() {
                @Override
                public ThreadGroup run() {
                    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
                    while (threadGroup.getParent() != null) {
                        threadGroup = threadGroup.getParent();
                    }
                    return threadGroup;
                }
            });
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(group, runnable, "process reaper", 32768L);
            thread.setDaemon(true);
            thread.setPriority(10);
            return thread;
        }
    }

    static {
        initIDs();
    }

    UNIXProcess(byte[] bArr, byte[] bArr2, int i, byte[] bArr3, int i2, byte[] bArr4, final int[] iArr, boolean z) throws IOException {
        this.pid = forkAndExec(bArr, bArr2, i, bArr3, i2, bArr4, iArr, z);
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws IOException {
                    UNIXProcess.this.initStreams(iArr);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw ((IOException) e.getException());
        }
    }

    static FileDescriptor newFileDescriptor(int i) {
        FileDescriptor fileDescriptor = new FileDescriptor();
        fileDescriptor.setInt$(i);
        return fileDescriptor;
    }

    void initStreams(int[] iArr) throws IOException {
        OutputStream processPipeOutputStream;
        InputStream processPipeInputStream;
        InputStream processPipeInputStream2;
        if (iArr[0] == -1) {
            processPipeOutputStream = ProcessBuilder.NullOutputStream.INSTANCE;
        } else {
            processPipeOutputStream = new ProcessPipeOutputStream(iArr[0]);
        }
        this.stdin = processPipeOutputStream;
        if (iArr[1] == -1) {
            processPipeInputStream = ProcessBuilder.NullInputStream.INSTANCE;
        } else {
            processPipeInputStream = new ProcessPipeInputStream(iArr[1]);
        }
        this.stdout = processPipeInputStream;
        if (iArr[2] == -1) {
            processPipeInputStream2 = ProcessBuilder.NullInputStream.INSTANCE;
        } else {
            processPipeInputStream2 = new ProcessPipeInputStream(iArr[2]);
        }
        this.stderr = processPipeInputStream2;
        processReaperExecutor.execute(new Runnable() {
            @Override
            public void run() {
                UNIXProcess.this.processExited(UNIXProcess.this.waitForProcessExit(UNIXProcess.this.pid));
            }
        });
    }

    void processExited(int i) {
        synchronized (this) {
            this.exitcode = i;
            this.hasExited = true;
            notifyAll();
        }
        if (this.stdout instanceof ProcessPipeInputStream) {
            ((ProcessPipeInputStream) this.stdout).processExited();
        }
        if (this.stderr instanceof ProcessPipeInputStream) {
            ((ProcessPipeInputStream) this.stderr).processExited();
        }
        if (this.stdin instanceof ProcessPipeOutputStream) {
            ((ProcessPipeOutputStream) this.stdin).processExited();
        }
    }

    @Override
    public OutputStream getOutputStream() {
        return this.stdin;
    }

    @Override
    public InputStream getInputStream() {
        return this.stdout;
    }

    @Override
    public InputStream getErrorStream() {
        return this.stderr;
    }

    @Override
    public synchronized int waitFor() throws InterruptedException {
        while (!this.hasExited) {
            wait();
        }
        return this.exitcode;
    }

    @Override
    public synchronized int exitValue() {
        if (!this.hasExited) {
            throw new IllegalThreadStateException("process hasn't exited");
        }
        return this.exitcode;
    }

    @Override
    public void destroy() {
        synchronized (this) {
            if (!this.hasExited) {
                destroyProcess(this.pid);
            }
        }
        try {
            this.stdin.close();
        } catch (IOException e) {
        }
        try {
            this.stdout.close();
        } catch (IOException e2) {
        }
        try {
            this.stderr.close();
        } catch (IOException e3) {
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Process[pid=");
        sb.append(this.pid);
        if (this.hasExited) {
            sb.append(" ,hasExited=true, exitcode=");
            sb.append(this.exitcode);
            sb.append("]");
        } else {
            sb.append(", hasExited=false]");
        }
        return sb.toString();
    }

    static class ProcessPipeInputStream extends BufferedInputStream {
        ProcessPipeInputStream(int i) {
            super(new FileInputStream(UNIXProcess.newFileDescriptor(i), true));
        }

        private static byte[] drainInputStream(InputStream inputStream) throws IOException {
            byte[] bArrCopyOf = null;
            if (inputStream == null) {
                return null;
            }
            int i = 0;
            while (true) {
                int iAvailable = inputStream.available();
                if (iAvailable <= 0) {
                    break;
                }
                bArrCopyOf = bArrCopyOf == null ? new byte[iAvailable] : Arrays.copyOf(bArrCopyOf, i + iAvailable);
                i += inputStream.read(bArrCopyOf, i, iAvailable);
            }
            return (bArrCopyOf == null || i == bArrCopyOf.length) ? bArrCopyOf : Arrays.copyOf(bArrCopyOf, i);
        }

        synchronized void processExited() {
            InputStream byteArrayInputStream;
            try {
                InputStream inputStream = this.in;
                if (inputStream != null) {
                    byte[] bArrDrainInputStream = drainInputStream(inputStream);
                    inputStream.close();
                    if (bArrDrainInputStream == null) {
                        byteArrayInputStream = ProcessBuilder.NullInputStream.INSTANCE;
                    } else {
                        byteArrayInputStream = new ByteArrayInputStream(bArrDrainInputStream);
                    }
                    this.in = byteArrayInputStream;
                    if (this.buf == null) {
                        this.in = null;
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    static class ProcessPipeOutputStream extends BufferedOutputStream {
        ProcessPipeOutputStream(int i) {
            super(new FileOutputStream(UNIXProcess.newFileDescriptor(i), true));
        }

        synchronized void processExited() {
            OutputStream outputStream = this.out;
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
                this.out = ProcessBuilder.NullOutputStream.INSTANCE;
            }
        }
    }
}
