package java.lang;

import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.MutableInt;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import libcore.io.IoUtils;
import libcore.io.Libcore;

final class ProcessManager {
    private static final ProcessManager instance = new ProcessManager();
    private final Map<Integer, ProcessReference> processReferences = new HashMap();
    private final ProcessReferenceQueue referenceQueue = new ProcessReferenceQueue();

    private static native int exec(String[] strArr, String[] strArr2, String str, FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, boolean z) throws IOException;

    private ProcessManager() {
        Thread reaperThread = new Thread(ProcessManager.class.getName()) {
            @Override
            public void run() {
                ProcessManager.this.watchChildren();
            }
        };
        reaperThread.setDaemon(true);
        reaperThread.start();
    }

    private void cleanUp() {
        while (true) {
            ?? Poll2 = this.referenceQueue.poll2();
            if (Poll2 != 0) {
                synchronized (this.processReferences) {
                    this.processReferences.remove(Integer.valueOf(Poll2.processId));
                }
            } else {
                return;
            }
        }
    }

    private void watchChildren() {
        int pid;
        int exitValue;
        MutableInt status = new MutableInt(-1);
        while (true) {
            try {
                pid = Libcore.os.waitpid(0, status, 0);
            } catch (ErrnoException errnoException) {
                if (errnoException.errno == OsConstants.ECHILD) {
                    waitForMoreChildren();
                } else {
                    throw new AssertionError(errnoException);
                }
            }
            if (OsConstants.WIFEXITED(status.value)) {
                exitValue = OsConstants.WEXITSTATUS(status.value);
            } else if (OsConstants.WIFSIGNALED(status.value)) {
                exitValue = OsConstants.WTERMSIG(status.value);
            } else if (OsConstants.WIFSTOPPED(status.value)) {
                exitValue = OsConstants.WSTOPSIG(status.value);
            } else {
                throw new AssertionError("unexpected status from waitpid: " + status.value);
            }
            onExit(pid, exitValue);
        }
    }

    private void onExit(int pid, int exitValue) {
        ProcessReference processReference;
        ProcessImpl process;
        synchronized (this.processReferences) {
            cleanUp();
            processReference = this.processReferences.remove(Integer.valueOf(pid));
        }
        if (processReference != null && (process = processReference.get()) != null) {
            process.setExitValue(exitValue);
        }
    }

    private void waitForMoreChildren() {
        synchronized (this.processReferences) {
            if (this.processReferences.isEmpty()) {
                try {
                    this.processReferences.wait();
                } catch (InterruptedException e) {
                    throw new AssertionError("unexpected interrupt");
                }
            }
        }
    }

    public Process exec(String[] taintedCommand, String[] taintedEnvironment, File workingDirectory, boolean redirectErrorStream) throws IOException {
        ProcessImpl process;
        if (taintedCommand == null) {
            throw new NullPointerException("taintedCommand == null");
        }
        if (taintedCommand.length == 0) {
            throw new IndexOutOfBoundsException("taintedCommand.length == 0");
        }
        String[] command = (String[]) taintedCommand.clone();
        String[] environment = taintedEnvironment != null ? (String[]) taintedEnvironment.clone() : null;
        for (int i = 0; i < command.length; i++) {
            if (command[i] == null) {
                throw new NullPointerException("taintedCommand[" + i + "] == null");
            }
        }
        if (environment != null) {
            for (int i2 = 0; i2 < environment.length; i2++) {
                if (environment[i2] == null) {
                    throw new NullPointerException("taintedEnvironment[" + i2 + "] == null");
                }
            }
        }
        FileDescriptor in = new FileDescriptor();
        FileDescriptor out = new FileDescriptor();
        FileDescriptor err = new FileDescriptor();
        String workingPath = workingDirectory == null ? null : workingDirectory.getPath();
        synchronized (this.processReferences) {
            try {
                int pid = exec(command, environment, workingPath, in, out, err, redirectErrorStream);
                process = new ProcessImpl(pid, in, out, err);
                ProcessReference processReference = new ProcessReference(process, this.referenceQueue);
                this.processReferences.put(Integer.valueOf(pid), processReference);
                this.processReferences.notifyAll();
            } catch (IOException e) {
                IOException wrapper = new IOException("Error running exec(). Command: " + Arrays.toString(command) + " Working Directory: " + workingDirectory + " Environment: " + Arrays.toString(environment));
                wrapper.initCause(e);
                throw wrapper;
            }
        }
        return process;
    }

    static class ProcessImpl extends Process {
        private final InputStream errorStream;
        private Integer exitValue = null;
        private final Object exitValueMutex = new Object();
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final int pid;

        ProcessImpl(int pid, FileDescriptor in, FileDescriptor out, FileDescriptor err) {
            this.pid = pid;
            this.errorStream = new ProcessInputStream(err);
            this.inputStream = new ProcessInputStream(in);
            this.outputStream = new ProcessOutputStream(out);
        }

        @Override
        public void destroy() {
            synchronized (this.exitValueMutex) {
                if (this.exitValue == null) {
                    try {
                        Libcore.os.kill(this.pid, OsConstants.SIGKILL);
                    } catch (ErrnoException e) {
                        System.logI("Failed to destroy process " + this.pid, e);
                    }
                }
            }
            IoUtils.closeQuietly(this.inputStream);
            IoUtils.closeQuietly(this.errorStream);
            IoUtils.closeQuietly(this.outputStream);
        }

        @Override
        public int exitValue() {
            int iIntValue;
            synchronized (this.exitValueMutex) {
                if (this.exitValue == null) {
                    throw new IllegalThreadStateException("Process has not yet terminated: " + this.pid);
                }
                iIntValue = this.exitValue.intValue();
            }
            return iIntValue;
        }

        @Override
        public InputStream getErrorStream() {
            return this.errorStream;
        }

        @Override
        public InputStream getInputStream() {
            return this.inputStream;
        }

        @Override
        public OutputStream getOutputStream() {
            return this.outputStream;
        }

        @Override
        public int waitFor() throws InterruptedException {
            int iIntValue;
            synchronized (this.exitValueMutex) {
                while (this.exitValue == null) {
                    this.exitValueMutex.wait();
                }
                iIntValue = this.exitValue.intValue();
            }
            return iIntValue;
        }

        void setExitValue(int exitValue) {
            synchronized (this.exitValueMutex) {
                this.exitValue = Integer.valueOf(exitValue);
                this.exitValueMutex.notifyAll();
            }
        }

        public String toString() {
            return "Process[pid=" + this.pid + "]";
        }
    }

    static class ProcessReference extends WeakReference<ProcessImpl> {
        final int processId;

        public ProcessReference(ProcessImpl referent, ProcessReferenceQueue referenceQueue) {
            super(referent, referenceQueue);
            this.processId = referent.pid;
        }
    }

    static class ProcessReferenceQueue extends ReferenceQueue<ProcessImpl> {
        ProcessReferenceQueue() {
        }

        @Override
        public Reference<? extends ProcessImpl> poll2() {
            Reference reference = super.poll2();
            return (ProcessReference) reference;
        }
    }

    public static ProcessManager getInstance() {
        return instance;
    }

    private static class ProcessInputStream extends FileInputStream {
        private FileDescriptor fd;

        private ProcessInputStream(FileDescriptor fd) {
            super(fd);
            this.fd = fd;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
                synchronized (this) {
                    try {
                        IoUtils.close(this.fd);
                    } catch (Throwable th) {
                        this.fd = null;
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                synchronized (this) {
                    try {
                        IoUtils.close(this.fd);
                        this.fd = null;
                        throw th2;
                    } finally {
                        this.fd = null;
                    }
                }
            }
        }
    }

    private static class ProcessOutputStream extends FileOutputStream {
        private FileDescriptor fd;

        private ProcessOutputStream(FileDescriptor fd) {
            super(fd);
            this.fd = fd;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
                synchronized (this) {
                    try {
                        IoUtils.close(this.fd);
                    } catch (Throwable th) {
                        this.fd = null;
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                synchronized (this) {
                    try {
                        IoUtils.close(this.fd);
                        this.fd = null;
                        throw th2;
                    } finally {
                        this.fd = null;
                    }
                }
            }
        }
    }
}
