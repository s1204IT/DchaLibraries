package com.android.internal.os;

import android.net.Credentials;
import android.net.LocalSocket;
import android.os.FactoryTest;
import android.os.Process;
import android.os.SystemProperties;
import android.os.Trace;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;
import android.util.TimeUtils;
import dalvik.system.VMRuntime;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import libcore.io.IoUtils;

class ZygoteConnection {
    private static final String TAG = "Zygote";
    private static final int[][] intArray2d = (int[][]) Array.newInstance((Class<?>) int.class, 0, 0);
    private final String abiList;
    private boolean isEof;
    private final LocalSocket mSocket;
    private final DataOutputStream mSocketOutStream;
    private final BufferedReader mSocketReader;
    private final Credentials peer;

    ZygoteConnection(LocalSocket localSocket, String str) throws IOException {
        this.mSocket = localSocket;
        this.abiList = str;
        this.mSocketOutStream = new DataOutputStream(localSocket.getOutputStream());
        this.mSocketReader = new BufferedReader(new InputStreamReader(localSocket.getInputStream()), 256);
        this.mSocket.setSoTimeout(1000);
        try {
            this.peer = this.mSocket.getPeerCredentials();
            this.isEof = false;
        } catch (IOException e) {
            Log.e(TAG, "Cannot read peer credentials", e);
            throw e;
        }
    }

    FileDescriptor getFileDesciptor() {
        return this.mSocket.getFileDescriptor();
    }

    Runnable processOneCommand(ZygoteServer zygoteServer) throws Throwable {
        FileDescriptor fileDescriptor;
        FileDescriptor fileDescriptor2;
        int[] iArr;
        FileDescriptor fileDescriptor3;
        try {
            String[] argumentList = readArgumentList();
            FileDescriptor[] ancillaryFileDescriptors = this.mSocket.getAncillaryFileDescriptors();
            if (argumentList == null) {
                this.isEof = true;
                return null;
            }
            Arguments arguments = new Arguments(argumentList);
            if (arguments.abiListQuery) {
                handleAbiListQuery();
                return null;
            }
            if (arguments.preloadDefault) {
                handlePreload();
                return null;
            }
            if (arguments.preloadPackage != null) {
                handlePreloadPackage(arguments.preloadPackage, arguments.preloadPackageLibs, arguments.preloadPackageLibFileName, arguments.preloadPackageCacheKey);
                return null;
            }
            if (arguments.apiBlacklistExemptions != null) {
                handleApiBlacklistExemptions(arguments.apiBlacklistExemptions);
                return null;
            }
            if (arguments.hiddenApiAccessLogSampleRate != -1) {
                handleHiddenApiAccessLogSampleRate(arguments.hiddenApiAccessLogSampleRate);
                return null;
            }
            if (arguments.permittedCapabilities != 0 || arguments.effectiveCapabilities != 0) {
                throw new ZygoteSecurityException("Client may not specify capabilities: permitted=0x" + Long.toHexString(arguments.permittedCapabilities) + ", effective=0x" + Long.toHexString(arguments.effectiveCapabilities));
            }
            applyUidSecurityPolicy(arguments, this.peer);
            applyInvokeWithSecurityPolicy(arguments, this.peer);
            applyDebuggerSystemProperty(arguments);
            applyInvokeWithSystemProperty(arguments);
            int[][] iArr2 = arguments.rlimits != null ? (int[][]) arguments.rlimits.toArray(intArray2d) : null;
            if (arguments.invokeWith != null) {
                try {
                    FileDescriptor[] fileDescriptorArrPipe2 = Os.pipe2(OsConstants.O_CLOEXEC);
                    FileDescriptor fileDescriptor4 = fileDescriptorArrPipe2[1];
                    FileDescriptor fileDescriptor5 = fileDescriptorArrPipe2[0];
                    Os.fcntlInt(fileDescriptor4, OsConstants.F_SETFD, 0);
                    int[] iArr3 = {fileDescriptor4.getInt$(), fileDescriptor5.getInt$()};
                    fileDescriptor = fileDescriptor5;
                    fileDescriptor2 = fileDescriptor4;
                    iArr = iArr3;
                } catch (ErrnoException e) {
                    throw new IllegalStateException("Unable to set up pipe for invoke-with", e);
                }
            } else {
                fileDescriptor2 = null;
                fileDescriptor = null;
                iArr = null;
            }
            int[] iArr4 = {-1, -1};
            FileDescriptor fileDescriptor6 = this.mSocket.getFileDescriptor();
            if (fileDescriptor6 != null) {
                iArr4[0] = fileDescriptor6.getInt$();
            }
            FileDescriptor serverSocketFileDescriptor = zygoteServer.getServerSocketFileDescriptor();
            if (serverSocketFileDescriptor != null) {
                iArr4[1] = serverSocketFileDescriptor.getInt$();
            }
            FileDescriptor fileDescriptor7 = fileDescriptor2;
            FileDescriptor fileDescriptor8 = fileDescriptor;
            int iForkAndSpecialize = Zygote.forkAndSpecialize(arguments.uid, arguments.gid, arguments.gids, arguments.runtimeFlags, iArr2, arguments.mountExternal, arguments.seInfo, arguments.niceName, iArr4, iArr, arguments.startChildZygote, arguments.instructionSet, arguments.appDataDir);
            if (iForkAndSpecialize == 0) {
                try {
                    zygoteServer.setForkChild();
                    zygoteServer.closeServerSocket();
                    IoUtils.closeQuietly(fileDescriptor8);
                    try {
                        fileDescriptor3 = fileDescriptor7;
                        try {
                            Runnable runnableHandleChildProc = handleChildProc(arguments, ancillaryFileDescriptors, fileDescriptor3, arguments.startChildZygote);
                            IoUtils.closeQuietly(fileDescriptor3);
                            IoUtils.closeQuietly((FileDescriptor) null);
                            return runnableHandleChildProc;
                        } catch (Throwable th) {
                            th = th;
                            fileDescriptor8 = null;
                            IoUtils.closeQuietly(fileDescriptor3);
                            IoUtils.closeQuietly(fileDescriptor8);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        fileDescriptor3 = fileDescriptor7;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fileDescriptor3 = fileDescriptor7;
                }
            } else {
                fileDescriptor3 = fileDescriptor7;
                try {
                    IoUtils.closeQuietly(fileDescriptor3);
                    try {
                        handleParentProc(iForkAndSpecialize, ancillaryFileDescriptors, fileDescriptor8);
                        IoUtils.closeQuietly((FileDescriptor) null);
                        IoUtils.closeQuietly(fileDescriptor8);
                        return null;
                    } catch (Throwable th4) {
                        th = th4;
                        fileDescriptor3 = null;
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            }
            IoUtils.closeQuietly(fileDescriptor3);
            IoUtils.closeQuietly(fileDescriptor8);
            throw th;
        } catch (IOException e2) {
            throw new IllegalStateException("IOException on command socket", e2);
        }
    }

    private void handleAbiListQuery() {
        try {
            byte[] bytes = this.abiList.getBytes(StandardCharsets.US_ASCII);
            this.mSocketOutStream.writeInt(bytes.length);
            this.mSocketOutStream.write(bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to command socket", e);
        }
    }

    private void handlePreload() {
        try {
            if (isPreloadComplete()) {
                this.mSocketOutStream.writeInt(1);
            } else {
                preload();
                this.mSocketOutStream.writeInt(0);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to command socket", e);
        }
    }

    private void handleApiBlacklistExemptions(String[] strArr) {
        try {
            ZygoteInit.setApiBlacklistExemptions(strArr);
            this.mSocketOutStream.writeInt(0);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to command socket", e);
        }
    }

    private void handleHiddenApiAccessLogSampleRate(int i) {
        try {
            ZygoteInit.setHiddenApiAccessLogSampleRate(i);
            this.mSocketOutStream.writeInt(0);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to command socket", e);
        }
    }

    protected void preload() {
        ZygoteInit.lazyPreload();
    }

    protected boolean isPreloadComplete() {
        return ZygoteInit.isPreloadComplete();
    }

    protected DataOutputStream getSocketOutputStream() {
        return this.mSocketOutStream;
    }

    protected void handlePreloadPackage(String str, String str2, String str3, String str4) {
        throw new RuntimeException("Zyogte does not support package preloading");
    }

    void closeSocket() {
        try {
            this.mSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception while closing command socket in parent", e);
        }
    }

    boolean isClosedByPeer() {
        return this.isEof;
    }

    static class Arguments {
        boolean abiListQuery;
        String[] apiBlacklistExemptions;
        String appDataDir;
        boolean capabilitiesSpecified;
        long effectiveCapabilities;
        boolean gidSpecified;
        int[] gids;
        String instructionSet;
        String invokeWith;
        String niceName;
        long permittedCapabilities;
        boolean preloadDefault;
        String preloadPackage;
        String preloadPackageCacheKey;
        String preloadPackageLibFileName;
        String preloadPackageLibs;
        String[] remainingArgs;
        ArrayList<int[]> rlimits;
        int runtimeFlags;
        String seInfo;
        boolean seInfoSpecified;
        boolean startChildZygote;
        int targetSdkVersion;
        boolean targetSdkVersionSpecified;
        boolean uidSpecified;
        int uid = 0;
        int gid = 0;
        int mountExternal = 0;
        int hiddenApiAccessLogSampleRate = -1;

        Arguments(String[] strArr) throws IllegalArgumentException {
            parseArgs(strArr);
        }

        private void parseArgs(String[] strArr) throws IllegalArgumentException {
            boolean z = false;
            int length = 0;
            boolean z2 = false;
            boolean z3 = true;
            while (true) {
                if (length >= strArr.length) {
                    break;
                }
                String str = strArr[length];
                if (str.equals("--")) {
                    length++;
                    break;
                }
                if (str.startsWith("--setuid=")) {
                    if (this.uidSpecified) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    this.uidSpecified = true;
                    this.uid = Integer.parseInt(str.substring(str.indexOf(61) + 1));
                } else if (str.startsWith("--setgid=")) {
                    if (this.gidSpecified) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    this.gidSpecified = true;
                    this.gid = Integer.parseInt(str.substring(str.indexOf(61) + 1));
                } else if (str.startsWith("--target-sdk-version=")) {
                    if (this.targetSdkVersionSpecified) {
                        throw new IllegalArgumentException("Duplicate target-sdk-version specified");
                    }
                    this.targetSdkVersionSpecified = true;
                    this.targetSdkVersion = Integer.parseInt(str.substring(str.indexOf(61) + 1));
                } else if (str.equals("--runtime-args")) {
                    z2 = true;
                } else if (str.startsWith("--runtime-flags=")) {
                    this.runtimeFlags = Integer.parseInt(str.substring(str.indexOf(61) + 1));
                } else if (str.startsWith("--seinfo=")) {
                    if (this.seInfoSpecified) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    this.seInfoSpecified = true;
                    this.seInfo = str.substring(str.indexOf(61) + 1);
                } else if (str.startsWith("--capabilities=")) {
                    if (this.capabilitiesSpecified) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    this.capabilitiesSpecified = true;
                    String[] strArrSplit = str.substring(str.indexOf(61) + 1).split(",", 2);
                    if (strArrSplit.length == 1) {
                        this.effectiveCapabilities = Long.decode(strArrSplit[0]).longValue();
                        this.permittedCapabilities = this.effectiveCapabilities;
                    } else {
                        this.permittedCapabilities = Long.decode(strArrSplit[0]).longValue();
                        this.effectiveCapabilities = Long.decode(strArrSplit[1]).longValue();
                    }
                } else if (str.startsWith("--rlimit=")) {
                    String[] strArrSplit2 = str.substring(str.indexOf(61) + 1).split(",");
                    if (strArrSplit2.length != 3) {
                        throw new IllegalArgumentException("--rlimit= should have 3 comma-delimited ints");
                    }
                    int[] iArr = new int[strArrSplit2.length];
                    for (int i = 0; i < strArrSplit2.length; i++) {
                        iArr[i] = Integer.parseInt(strArrSplit2[i]);
                    }
                    if (this.rlimits == null) {
                        this.rlimits = new ArrayList<>();
                    }
                    this.rlimits.add(iArr);
                } else if (str.startsWith("--setgroups=")) {
                    if (this.gids != null) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    String[] strArrSplit3 = str.substring(str.indexOf(61) + 1).split(",");
                    this.gids = new int[strArrSplit3.length];
                    for (int length2 = strArrSplit3.length - 1; length2 >= 0; length2--) {
                        this.gids[length2] = Integer.parseInt(strArrSplit3[length2]);
                    }
                } else if (str.equals("--invoke-with")) {
                    if (this.invokeWith != null) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    length++;
                    try {
                        this.invokeWith = strArr[length];
                    } catch (IndexOutOfBoundsException e) {
                        throw new IllegalArgumentException("--invoke-with requires argument");
                    }
                } else if (str.startsWith("--nice-name=")) {
                    if (this.niceName != null) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    }
                    this.niceName = str.substring(str.indexOf(61) + 1);
                } else if (str.equals("--mount-external-default")) {
                    this.mountExternal = 1;
                } else if (str.equals("--mount-external-read")) {
                    this.mountExternal = 2;
                } else if (str.equals("--mount-external-write")) {
                    this.mountExternal = 3;
                } else if (str.equals("--query-abi-list")) {
                    this.abiListQuery = true;
                } else if (str.startsWith("--instruction-set=")) {
                    this.instructionSet = str.substring(str.indexOf(61) + 1);
                } else if (str.startsWith("--app-data-dir=")) {
                    this.appDataDir = str.substring(str.indexOf(61) + 1);
                } else if (str.equals("--preload-package")) {
                    int i2 = length + 1;
                    this.preloadPackage = strArr[i2];
                    int i3 = i2 + 1;
                    this.preloadPackageLibs = strArr[i3];
                    int i4 = i3 + 1;
                    this.preloadPackageLibFileName = strArr[i4];
                    length = i4 + 1;
                    this.preloadPackageCacheKey = strArr[length];
                } else {
                    if (str.equals("--preload-default")) {
                        this.preloadDefault = true;
                    } else if (str.equals("--start-child-zygote")) {
                        this.startChildZygote = true;
                    } else if (!str.equals("--set-api-blacklist-exemptions")) {
                        if (!str.startsWith("--hidden-api-log-sampling-rate=")) {
                            break;
                        }
                        String strSubstring = str.substring(str.indexOf(61) + 1);
                        try {
                            this.hiddenApiAccessLogSampleRate = Integer.parseInt(strSubstring);
                        } catch (NumberFormatException e2) {
                            throw new IllegalArgumentException("Invalid log sampling rate: " + strSubstring, e2);
                        }
                    } else {
                        this.apiBlacklistExemptions = (String[]) Arrays.copyOfRange(strArr, length + 1, strArr.length);
                        length = strArr.length;
                    }
                    z3 = false;
                }
                length++;
            }
            if (this.abiListQuery) {
                if (strArr.length - length > 0) {
                    throw new IllegalArgumentException("Unexpected arguments after --query-abi-list.");
                }
            } else if (this.preloadPackage != null) {
                if (strArr.length - length > 0) {
                    throw new IllegalArgumentException("Unexpected arguments after --preload-package.");
                }
            } else if (z3) {
                if (!z2) {
                    throw new IllegalArgumentException("Unexpected argument : " + strArr[length]);
                }
                this.remainingArgs = new String[strArr.length - length];
                System.arraycopy(strArr, length, this.remainingArgs, 0, this.remainingArgs.length);
            }
            if (this.startChildZygote) {
                String[] strArr2 = this.remainingArgs;
                int length3 = strArr2.length;
                int i5 = 0;
                while (true) {
                    if (i5 >= length3) {
                        break;
                    }
                    if (strArr2[i5].startsWith(Zygote.CHILD_ZYGOTE_SOCKET_NAME_ARG)) {
                        z = true;
                        break;
                    }
                    i5++;
                }
                if (!z) {
                    throw new IllegalArgumentException("--start-child-zygote specified without --zygote-socket=");
                }
            }
        }
    }

    private String[] readArgumentList() throws IOException {
        try {
            String line = this.mSocketReader.readLine();
            if (line == null) {
                return null;
            }
            int i = Integer.parseInt(line);
            if (i > 1024) {
                throw new IOException("max arg count exceeded");
            }
            String[] strArr = new String[i];
            for (int i2 = 0; i2 < i; i2++) {
                strArr[i2] = this.mSocketReader.readLine();
                if (strArr[i2] == null) {
                    throw new IOException("truncated request");
                }
            }
            return strArr;
        } catch (NumberFormatException e) {
            Log.e(TAG, "invalid Zygote wire format: non-int at argc");
            throw new IOException("invalid wire format");
        }
    }

    private static void applyUidSecurityPolicy(Arguments arguments, Credentials credentials) throws ZygoteSecurityException {
        boolean z;
        if (credentials.getUid() == 1000) {
            if (FactoryTest.getMode() != 0) {
                z = false;
            } else {
                z = true;
            }
            if (z && arguments.uidSpecified && arguments.uid < 1000) {
                throw new ZygoteSecurityException("System UID may not launch process with UID < 1000");
            }
        }
        if (!arguments.uidSpecified) {
            arguments.uid = credentials.getUid();
            arguments.uidSpecified = true;
        }
        if (!arguments.gidSpecified) {
            arguments.gid = credentials.getGid();
            arguments.gidSpecified = true;
        }
    }

    public static void applyDebuggerSystemProperty(Arguments arguments) {
        if (RoSystemProperties.DEBUGGABLE) {
            arguments.runtimeFlags |= 1;
        }
    }

    private static void applyInvokeWithSecurityPolicy(Arguments arguments, Credentials credentials) throws ZygoteSecurityException {
        int uid = credentials.getUid();
        if (arguments.invokeWith != null && uid != 0 && (arguments.runtimeFlags & 1) == 0) {
            throw new ZygoteSecurityException("Peer is permitted to specify anexplicit invoke-with wrapper command only for debuggableapplications.");
        }
    }

    public static void applyInvokeWithSystemProperty(Arguments arguments) {
        if (arguments.invokeWith == null && arguments.niceName != null) {
            arguments.invokeWith = SystemProperties.get("wrap." + arguments.niceName);
            if (arguments.invokeWith != null && arguments.invokeWith.length() == 0) {
                arguments.invokeWith = null;
            }
        }
    }

    private Runnable handleChildProc(Arguments arguments, FileDescriptor[] fileDescriptorArr, FileDescriptor fileDescriptor, boolean z) {
        closeSocket();
        if (fileDescriptorArr != null) {
            try {
                Os.dup2(fileDescriptorArr[0], OsConstants.STDIN_FILENO);
                Os.dup2(fileDescriptorArr[1], OsConstants.STDOUT_FILENO);
                Os.dup2(fileDescriptorArr[2], OsConstants.STDERR_FILENO);
                for (FileDescriptor fileDescriptor2 : fileDescriptorArr) {
                    IoUtils.closeQuietly(fileDescriptor2);
                }
            } catch (ErrnoException e) {
                Log.e(TAG, "Error reopening stdio", e);
            }
        }
        if (arguments.niceName != null) {
            Process.setArgV0(arguments.niceName);
        }
        Trace.traceEnd(64L);
        if (arguments.invokeWith == null) {
            return !z ? ZygoteInit.zygoteInit(arguments.targetSdkVersion, arguments.remainingArgs, null) : ZygoteInit.childZygoteInit(arguments.targetSdkVersion, arguments.remainingArgs, null);
        }
        WrapperInit.execApplication(arguments.invokeWith, arguments.niceName, arguments.targetSdkVersion, VMRuntime.getCurrentInstructionSet(), fileDescriptor, arguments.remainingArgs);
        throw new IllegalStateException("WrapperInit.execApplication unexpectedly returned");
    }

    private void handleParentProc(int i, FileDescriptor[] fileDescriptorArr, FileDescriptor fileDescriptor) throws IOException {
        int i2;
        int i3 = i;
        if (i3 > 0) {
            setChildPgid(i);
        }
        boolean z = false;
        if (fileDescriptorArr != null) {
            for (FileDescriptor fileDescriptor2 : fileDescriptorArr) {
                IoUtils.closeQuietly(fileDescriptor2);
            }
        }
        if (fileDescriptor != null && i3 > 0) {
            int i4 = -1;
            try {
                StructPollfd[] structPollfdArr = {new StructPollfd()};
                byte[] bArr = new byte[4];
                long jNanoTime = System.nanoTime();
                int i5 = 0;
                int i6 = 30000;
                while (i5 < bArr.length && i6 > 0) {
                    structPollfdArr[0].fd = fileDescriptor;
                    structPollfdArr[0].events = (short) OsConstants.POLLIN;
                    structPollfdArr[0].revents = (short) 0;
                    structPollfdArr[0].userData = null;
                    int iPoll = Os.poll(structPollfdArr, i6);
                    int iNanoTime = 30000 - ((int) ((System.nanoTime() - jNanoTime) / TimeUtils.NANOS_PER_MS));
                    if (iPoll > 0) {
                        if ((structPollfdArr[0].revents & OsConstants.POLLIN) == 0) {
                            break;
                        }
                        int i7 = Os.read(fileDescriptor, bArr, i5, 1);
                        if (i7 < 0) {
                            throw new RuntimeException("Some error");
                        }
                        i5 += i7;
                    } else if (iPoll == 0) {
                        Log.w(TAG, "Timed out waiting for child.");
                    }
                    i6 = iNanoTime;
                }
                if (i5 != bArr.length) {
                    i2 = -1;
                } else {
                    i2 = new DataInputStream(new ByteArrayInputStream(bArr)).readInt();
                }
                if (i2 == -1) {
                    try {
                        Log.w(TAG, "Error reading pid from wrapped process, child may have died");
                    } catch (Exception e) {
                        e = e;
                        i4 = i2;
                        Log.w(TAG, "Error reading pid from wrapped process, child may have died", e);
                        i2 = i4;
                    }
                }
            } catch (Exception e2) {
                e = e2;
            }
            if (i2 > 0) {
                int parentPid = i2;
                while (parentPid > 0 && parentPid != i3) {
                    parentPid = Process.getParentPid(parentPid);
                }
                if (parentPid > 0) {
                    Log.i(TAG, "Wrapped process has pid " + i2);
                    i3 = i2;
                    z = true;
                } else {
                    Log.w(TAG, "Wrapped process reported a pid that is not a child of the process that we forked: childPid=" + i3 + " innerPid=" + i2);
                }
            }
        }
        try {
            this.mSocketOutStream.writeInt(i3);
            this.mSocketOutStream.writeBoolean(z);
        } catch (IOException e3) {
            throw new IllegalStateException("Error writing to command socket", e3);
        }
    }

    private void setChildPgid(int i) {
        try {
            Os.setpgid(i, Os.getpgid(this.peer.getPid()));
        } catch (ErrnoException e) {
            Log.i(TAG, "Zygote: setpgid failed. This is normal if peer is not in our session");
        }
    }
}
