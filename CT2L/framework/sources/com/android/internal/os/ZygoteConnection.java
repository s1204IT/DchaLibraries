package com.android.internal.os;

import android.net.Credentials;
import android.net.LocalSocket;
import android.net.ProxyInfo;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Process;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.ZygoteInit;
import dalvik.system.PathClassLoader;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import libcore.io.IoUtils;

class ZygoteConnection {
    private static final int CONNECTION_TIMEOUT_MILLIS = 1000;
    private static final int MAX_ZYGOTE_ARGC = 1024;
    private static final String TAG = "Zygote";
    private static final int[][] intArray2d = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, 0, 0);
    private final String abiList;
    private final LocalSocket mSocket;
    private final DataOutputStream mSocketOutStream;
    private final BufferedReader mSocketReader;
    private final Credentials peer;
    private final String peerSecurityContext;

    ZygoteConnection(LocalSocket socket, String abiList) throws IOException {
        this.mSocket = socket;
        this.abiList = abiList;
        this.mSocketOutStream = new DataOutputStream(socket.getOutputStream());
        this.mSocketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()), 256);
        this.mSocket.setSoTimeout(1000);
        try {
            this.peer = this.mSocket.getPeerCredentials();
            this.peerSecurityContext = SELinux.getPeerContext(this.mSocket.getFileDescriptor());
        } catch (IOException ex) {
            Log.e(TAG, "Cannot read peer credentials", ex);
            throw ex;
        }
    }

    private void checkTime(long startTime, String where) {
        long now = SystemClock.elapsedRealtime();
        if (now - startTime > 1000) {
            Slog.w(TAG, "Slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    FileDescriptor getFileDescriptor() {
        return this.mSocket.getFileDescriptor();
    }

    boolean runOnce() throws ZygoteInit.MethodAndArgsCaller {
        boolean zHandleParentProc;
        Arguments parsedArgs;
        Arguments parsedArgs2 = null;
        long startTime = SystemClock.elapsedRealtime();
        try {
            String[] args = readArgumentList();
            FileDescriptor[] descriptors = this.mSocket.getAncillaryFileDescriptors();
            checkTime(startTime, "zygoteConnection.runOnce: readArgumentList");
            if (args == null) {
                closeSocket();
                return true;
            }
            PrintStream newStderr = null;
            if (descriptors != null && descriptors.length >= 3) {
                newStderr = new PrintStream(new FileOutputStream(descriptors[2]));
            }
            int pid = -1;
            FileDescriptor childPipeFd = null;
            FileDescriptor serverPipeFd = null;
            try {
                parsedArgs = new Arguments(args);
            } catch (ErrnoException e) {
                ex = e;
            } catch (ZygoteSecurityException e2) {
                ex = e2;
            } catch (IOException e3) {
                ex = e3;
            } catch (IllegalArgumentException e4) {
                ex = e4;
            }
            try {
            } catch (ErrnoException e5) {
                ex = e5;
                parsedArgs2 = parsedArgs;
                logAndPrintError(newStderr, "Exception creating pipe", ex);
            } catch (ZygoteSecurityException e6) {
                ex = e6;
                parsedArgs2 = parsedArgs;
                logAndPrintError(newStderr, "Zygote security policy prevents request: ", ex);
            } catch (IOException e7) {
                ex = e7;
                parsedArgs2 = parsedArgs;
                logAndPrintError(newStderr, "Exception creating pipe", ex);
            } catch (IllegalArgumentException e8) {
                ex = e8;
                parsedArgs2 = parsedArgs;
                logAndPrintError(newStderr, "Invalid zygote arguments", ex);
            }
            if (parsedArgs.abiListQuery) {
                return handleAbiListQuery();
            }
            if (parsedArgs.permittedCapabilities != 0 || parsedArgs.effectiveCapabilities != 0) {
                throw new ZygoteSecurityException("Client may not specify capabilities: permitted=0x" + Long.toHexString(parsedArgs.permittedCapabilities) + ", effective=0x" + Long.toHexString(parsedArgs.effectiveCapabilities));
            }
            applyUidSecurityPolicy(parsedArgs, this.peer, this.peerSecurityContext);
            applyRlimitSecurityPolicy(parsedArgs, this.peer, this.peerSecurityContext);
            applyInvokeWithSecurityPolicy(parsedArgs, this.peer, this.peerSecurityContext);
            applyseInfoSecurityPolicy(parsedArgs, this.peer, this.peerSecurityContext);
            checkTime(startTime, "zygoteConnection.runOnce: apply security policies");
            applyDebuggerSystemProperty(parsedArgs);
            applyInvokeWithSystemProperty(parsedArgs);
            checkTime(startTime, "zygoteConnection.runOnce: apply security policies");
            int[][] rlimits = (int[][]) null;
            if (parsedArgs.rlimits != null) {
                rlimits = (int[][]) parsedArgs.rlimits.toArray(intArray2d);
            }
            if (parsedArgs.runtimeInit && parsedArgs.invokeWith != null) {
                FileDescriptor[] pipeFds = Os.pipe();
                childPipeFd = pipeFds[1];
                serverPipeFd = pipeFds[0];
                ZygoteInit.setCloseOnExec(serverPipeFd, true);
            }
            int[] fdsToClose = {-1, -1};
            FileDescriptor fd = this.mSocket.getFileDescriptor();
            if (fd != null) {
                fdsToClose[0] = fd.getInt$();
            }
            FileDescriptor fd2 = ZygoteInit.getServerSocketFileDescriptor();
            if (fd2 != null) {
                fdsToClose[1] = fd2.getInt$();
            }
            checkTime(startTime, "zygoteConnection.runOnce: preForkAndSpecialize");
            pid = Zygote.forkAndSpecialize(parsedArgs.uid, parsedArgs.gid, parsedArgs.gids, parsedArgs.debugFlags, rlimits, parsedArgs.mountExternal, parsedArgs.seInfo, parsedArgs.niceName, fdsToClose, parsedArgs.instructionSet, parsedArgs.appDataDir);
            checkTime(startTime, "zygoteConnection.runOnce: postForkAndSpecialize");
            parsedArgs2 = parsedArgs;
            try {
                if (pid == 0) {
                    IoUtils.closeQuietly(serverPipeFd);
                    serverPipeFd = null;
                    handleChildProc(parsedArgs2, descriptors, childPipeFd, newStderr);
                    zHandleParentProc = true;
                } else {
                    IoUtils.closeQuietly(childPipeFd);
                    childPipeFd = null;
                    zHandleParentProc = handleParentProc(pid, descriptors, serverPipeFd, parsedArgs2);
                    IoUtils.closeQuietly((FileDescriptor) null);
                    IoUtils.closeQuietly(serverPipeFd);
                }
                return zHandleParentProc;
            } finally {
                IoUtils.closeQuietly(childPipeFd);
                IoUtils.closeQuietly(serverPipeFd);
            }
        } catch (IOException ex) {
            Log.w(TAG, "IOException on command socket " + ex.getMessage());
            closeSocket();
            return true;
        }
    }

    private boolean handleAbiListQuery() {
        try {
            byte[] abiListBytes = this.abiList.getBytes(StandardCharsets.US_ASCII);
            this.mSocketOutStream.writeInt(abiListBytes.length);
            this.mSocketOutStream.write(abiListBytes);
            return false;
        } catch (IOException ioe) {
            Log.e(TAG, "Error writing to command socket", ioe);
            return true;
        }
    }

    void closeSocket() {
        try {
            this.mSocket.close();
        } catch (IOException ex) {
            Log.e(TAG, "Exception while closing command socket in parent", ex);
        }
    }

    static class Arguments {
        boolean abiListQuery;
        String appDataDir;
        boolean capabilitiesSpecified;
        String classpath;
        int debugFlags;
        long effectiveCapabilities;
        boolean gidSpecified;
        int[] gids;
        String instructionSet;
        String invokeWith;
        String niceName;
        long permittedCapabilities;
        String[] remainingArgs;
        ArrayList<int[]> rlimits;
        boolean runtimeInit;
        String seInfo;
        boolean seInfoSpecified;
        int targetSdkVersion;
        boolean targetSdkVersionSpecified;
        boolean uidSpecified;
        int uid = 0;
        int gid = 0;
        int mountExternal = 0;

        Arguments(String[] args) throws IllegalArgumentException {
            parseArgs(args);
        }

        private void parseArgs(String[] args) throws IllegalArgumentException {
            int curArg = 0;
            while (true) {
                if (curArg >= args.length) {
                    break;
                }
                String arg = args[curArg];
                if (arg.equals("--")) {
                    curArg++;
                    break;
                }
                if (arg.startsWith("--setuid=")) {
                    if (this.uidSpecified) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    this.uidSpecified = true;
                    this.uid = Integer.parseInt(arg.substring(arg.indexOf(61) + 1));
                } else if (arg.startsWith("--setgid=")) {
                    if (this.gidSpecified) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    this.gidSpecified = true;
                    this.gid = Integer.parseInt(arg.substring(arg.indexOf(61) + 1));
                } else if (arg.startsWith("--target-sdk-version=")) {
                    if (this.targetSdkVersionSpecified) {
                        throw new IllegalArgumentException("Duplicate target-sdk-version specified");
                    }
                    this.targetSdkVersionSpecified = true;
                    this.targetSdkVersion = Integer.parseInt(arg.substring(arg.indexOf(61) + 1));
                } else if (arg.equals("--enable-debugger")) {
                    this.debugFlags |= 1;
                } else if (arg.equals("--enable-safemode")) {
                    this.debugFlags |= 8;
                } else if (arg.equals("--enable-checkjni")) {
                    this.debugFlags |= 2;
                } else if (arg.equals("--enable-jni-logging")) {
                    this.debugFlags |= 16;
                } else if (arg.equals("--enable-assert")) {
                    this.debugFlags |= 4;
                } else if (arg.equals("--runtime-init")) {
                    this.runtimeInit = true;
                } else if (arg.startsWith("--seinfo=")) {
                    if (this.seInfoSpecified) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    this.seInfoSpecified = true;
                    this.seInfo = arg.substring(arg.indexOf(61) + 1);
                } else if (arg.startsWith("--capabilities=")) {
                    if (this.capabilitiesSpecified) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    this.capabilitiesSpecified = true;
                    String capString = arg.substring(arg.indexOf(61) + 1);
                    String[] capStrings = capString.split(",", 2);
                    if (capStrings.length == 1) {
                        this.effectiveCapabilities = Long.decode(capStrings[0]).longValue();
                        this.permittedCapabilities = this.effectiveCapabilities;
                    } else {
                        this.permittedCapabilities = Long.decode(capStrings[0]).longValue();
                        this.effectiveCapabilities = Long.decode(capStrings[1]).longValue();
                    }
                } else if (arg.startsWith("--rlimit=")) {
                    String[] limitStrings = arg.substring(arg.indexOf(61) + 1).split(",");
                    if (limitStrings.length != 3) {
                        throw new IllegalArgumentException("--rlimit= should have 3 comma-delimited ints");
                    }
                    int[] rlimitTuple = new int[limitStrings.length];
                    for (int i = 0; i < limitStrings.length; i++) {
                        rlimitTuple[i] = Integer.parseInt(limitStrings[i]);
                    }
                    if (this.rlimits == null) {
                        this.rlimits = new ArrayList<>();
                    }
                    this.rlimits.add(rlimitTuple);
                } else if (arg.equals("-classpath")) {
                    if (this.classpath != null) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    curArg++;
                    try {
                        this.classpath = args[curArg];
                    } catch (IndexOutOfBoundsException e) {
                        throw new IllegalArgumentException("-classpath requires argument");
                    }
                } else if (arg.startsWith("--setgroups=")) {
                    if (this.gids != null) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    String[] params = arg.substring(arg.indexOf(61) + 1).split(",");
                    this.gids = new int[params.length];
                    for (int i2 = params.length - 1; i2 >= 0; i2--) {
                        this.gids[i2] = Integer.parseInt(params[i2]);
                    }
                } else if (arg.equals("--invoke-with")) {
                    if (this.invokeWith != null) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    curArg++;
                    try {
                        this.invokeWith = args[curArg];
                    } catch (IndexOutOfBoundsException e2) {
                        throw new IllegalArgumentException("--invoke-with requires argument");
                    }
                } else if (arg.startsWith("--nice-name=")) {
                    if (this.niceName != null) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    this.niceName = arg.substring(arg.indexOf(61) + 1);
                } else if (arg.equals("--mount-external-multiuser")) {
                    this.mountExternal = 2;
                } else if (arg.equals("--mount-external-multiuser-all")) {
                    this.mountExternal = 3;
                } else if (arg.equals("--query-abi-list")) {
                    this.abiListQuery = true;
                } else if (arg.startsWith("--instruction-set=")) {
                    this.instructionSet = arg.substring(arg.indexOf(61) + 1);
                } else if (!arg.startsWith("--app-data-dir=")) {
                    break;
                } else {
                    this.appDataDir = arg.substring(arg.indexOf(61) + 1);
                }
                curArg++;
            }
            if (this.runtimeInit && this.classpath != null) {
                throw new IllegalArgumentException("--runtime-init and -classpath are incompatible");
            }
            this.remainingArgs = new String[args.length - curArg];
            System.arraycopy(args, curArg, this.remainingArgs, 0, this.remainingArgs.length);
        }
    }

    private String[] readArgumentList() throws IOException {
        try {
            String s = this.mSocketReader.readLine();
            if (s == null) {
                return null;
            }
            int argc = Integer.parseInt(s);
            if (argc > 1024) {
                throw new IOException("max arg count exceeded");
            }
            String[] result = new String[argc];
            for (int i = 0; i < argc; i++) {
                result[i] = this.mSocketReader.readLine();
                if (result[i] == null) {
                    throw new IOException("truncated request");
                }
            }
            return result;
        } catch (NumberFormatException e) {
            Log.e(TAG, "invalid Zygote wire format: non-int at argc");
            throw new IOException("invalid wire format");
        }
    }

    private static void applyUidSecurityPolicy(Arguments args, Credentials peer, String peerSecurityContext) throws ZygoteSecurityException {
        int peerUid = peer.getUid();
        if (peerUid != 0) {
            if (peerUid == 1000) {
                String factoryTest = SystemProperties.get("ro.factorytest");
                boolean uidRestricted = (factoryTest.equals(WifiEnterpriseConfig.ENGINE_ENABLE) || factoryTest.equals("2")) ? false : true;
                if (uidRestricted && args.uidSpecified && args.uid < 1000) {
                    throw new ZygoteSecurityException("System UID may not launch process with UID < 1000");
                }
            } else if (args.uidSpecified || args.gidSpecified || args.gids != null) {
                throw new ZygoteSecurityException("App UIDs may not specify uid's or gid's");
            }
        }
        if (args.uidSpecified || args.gidSpecified || args.gids != null) {
            boolean allowed = SELinux.checkSELinuxAccess(peerSecurityContext, peerSecurityContext, Process.ZYGOTE_SOCKET, "specifyids");
            if (!allowed) {
                throw new ZygoteSecurityException("Peer may not specify uid's or gid's");
            }
        }
        if (!args.uidSpecified) {
            args.uid = peer.getUid();
            args.uidSpecified = true;
        }
        if (!args.gidSpecified) {
            args.gid = peer.getGid();
            args.gidSpecified = true;
        }
    }

    public static void applyDebuggerSystemProperty(Arguments args) {
        if (WifiEnterpriseConfig.ENGINE_ENABLE.equals(SystemProperties.get("ro.debuggable"))) {
            args.debugFlags |= 1;
        }
    }

    private static void applyRlimitSecurityPolicy(Arguments args, Credentials peer, String peerSecurityContext) throws ZygoteSecurityException {
        int peerUid = peer.getUid();
        if (peerUid != 0 && peerUid != 1000 && args.rlimits != null) {
            throw new ZygoteSecurityException("This UID may not specify rlimits.");
        }
        if (args.rlimits != null) {
            boolean allowed = SELinux.checkSELinuxAccess(peerSecurityContext, peerSecurityContext, Process.ZYGOTE_SOCKET, "specifyrlimits");
            if (!allowed) {
                throw new ZygoteSecurityException("Peer may not specify rlimits");
            }
        }
    }

    private static void applyInvokeWithSecurityPolicy(Arguments args, Credentials peer, String peerSecurityContext) throws ZygoteSecurityException {
        int peerUid = peer.getUid();
        if (args.invokeWith != null && peerUid != 0) {
            throw new ZygoteSecurityException("Peer is not permitted to specify an explicit invoke-with wrapper command");
        }
        if (args.invokeWith != null) {
            boolean allowed = SELinux.checkSELinuxAccess(peerSecurityContext, peerSecurityContext, Process.ZYGOTE_SOCKET, "specifyinvokewith");
            if (!allowed) {
                throw new ZygoteSecurityException("Peer is not permitted to specify an explicit invoke-with wrapper command");
            }
        }
    }

    private static void applyseInfoSecurityPolicy(Arguments args, Credentials peer, String peerSecurityContext) throws ZygoteSecurityException {
        int peerUid = peer.getUid();
        if (args.seInfo != null) {
            if (peerUid != 0 && peerUid != 1000) {
                throw new ZygoteSecurityException("This UID may not specify SELinux info.");
            }
            boolean allowed = SELinux.checkSELinuxAccess(peerSecurityContext, peerSecurityContext, Process.ZYGOTE_SOCKET, "specifyseinfo");
            if (!allowed) {
                throw new ZygoteSecurityException("Peer may not specify SELinux info");
            }
        }
    }

    public static void applyInvokeWithSystemProperty(Arguments args) {
        if (args.invokeWith == null && args.niceName != null && args.niceName != null) {
            String property = "wrap." + args.niceName;
            if (property.length() > 31) {
                if (property.charAt(30) != '.') {
                    property = property.substring(0, 31);
                } else {
                    property = property.substring(0, 30);
                }
            }
            args.invokeWith = SystemProperties.get(property);
            if (args.invokeWith != null && args.invokeWith.length() == 0) {
                args.invokeWith = null;
            }
        }
    }

    private void handleChildProc(Arguments parsedArgs, FileDescriptor[] descriptors, FileDescriptor pipeFd, PrintStream newStderr) throws ZygoteInit.MethodAndArgsCaller {
        ClassLoader cloader;
        closeSocket();
        ZygoteInit.closeServerSocket();
        if (descriptors != null) {
            try {
                ZygoteInit.reopenStdio(descriptors[0], descriptors[1], descriptors[2]);
                for (FileDescriptor fd : descriptors) {
                    IoUtils.closeQuietly(fd);
                }
                newStderr = System.err;
            } catch (IOException ex) {
                Log.e(TAG, "Error reopening stdio", ex);
            }
        }
        if (parsedArgs.niceName != null) {
            Process.setArgV0(parsedArgs.niceName);
        }
        if (parsedArgs.runtimeInit) {
            if (parsedArgs.invokeWith != null) {
                WrapperInit.execApplication(parsedArgs.invokeWith, parsedArgs.niceName, parsedArgs.targetSdkVersion, pipeFd, parsedArgs.remainingArgs);
                return;
            } else {
                RuntimeInit.zygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, null);
                return;
            }
        }
        try {
            String className = parsedArgs.remainingArgs[0];
            String[] mainArgs = new String[parsedArgs.remainingArgs.length - 1];
            System.arraycopy(parsedArgs.remainingArgs, 1, mainArgs, 0, mainArgs.length);
            if (parsedArgs.invokeWith != null) {
                WrapperInit.execStandalone(parsedArgs.invokeWith, parsedArgs.classpath, className, mainArgs);
                return;
            }
            if (parsedArgs.classpath != null) {
                cloader = new PathClassLoader(parsedArgs.classpath, ClassLoader.getSystemClassLoader());
            } else {
                cloader = ClassLoader.getSystemClassLoader();
            }
            try {
                ZygoteInit.invokeStaticMain(cloader, className, mainArgs);
            } catch (RuntimeException ex2) {
                logAndPrintError(newStderr, "Error starting.", ex2);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            logAndPrintError(newStderr, "Missing required class name argument", null);
        }
    }

    private boolean handleParentProc(int pid, FileDescriptor[] descriptors, FileDescriptor pipeFd, Arguments parsedArgs) {
        if (pid > 0) {
            setChildPgid(pid);
        }
        if (descriptors != null) {
            for (FileDescriptor fd : descriptors) {
                IoUtils.closeQuietly(fd);
            }
        }
        boolean usingWrapper = false;
        if (pipeFd != null && pid > 0) {
            DataInputStream is = new DataInputStream(new FileInputStream(pipeFd));
            int innerPid = -1;
            try {
                try {
                    innerPid = is.readInt();
                } catch (IOException ex) {
                    Log.w(TAG, "Error reading pid from wrapped process, child may have died", ex);
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
                if (innerPid > 0) {
                    int parentPid = innerPid;
                    while (parentPid > 0 && parentPid != pid) {
                        parentPid = Process.getParentPid(parentPid);
                    }
                    if (parentPid > 0) {
                        Log.i(TAG, "Wrapped process has pid " + innerPid);
                        pid = innerPid;
                        usingWrapper = true;
                    } else {
                        Log.w(TAG, "Wrapped process reported a pid that is not a child of the process that we forked: childPid=" + pid + " innerPid=" + innerPid);
                    }
                }
            } finally {
                try {
                    is.close();
                } catch (IOException e2) {
                }
            }
        }
        try {
            this.mSocketOutStream.writeInt(pid);
            this.mSocketOutStream.writeBoolean(usingWrapper);
            return false;
        } catch (IOException ex2) {
            Log.e(TAG, "Error writing to command socket", ex2);
            return true;
        }
    }

    private void setChildPgid(int pid) {
        try {
            ZygoteInit.setpgid(pid, ZygoteInit.getpgid(this.peer.getPid()));
        } catch (IOException e) {
            Log.i(TAG, "Zygote: setpgid failed. This is normal if peer is not in our session");
        }
    }

    private static void logAndPrintError(PrintStream printStream, String str, Throwable th) {
        Log.e(TAG, str, th);
        if (printStream != null) {
            StringBuilder sbAppend = new StringBuilder().append(str);
            Object obj = th;
            if (th == null) {
                obj = ProxyInfo.LOCAL_EXCL_LIST;
            }
            printStream.println(sbAppend.append(obj).toString());
        }
    }
}
