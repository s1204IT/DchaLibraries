package com.android.server.pm;

import android.content.pm.PackageParser;
import android.os.FileUtils;
import android.util.AtomicFile;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import libcore.io.IoUtils;

class PackageUsage extends AbstractStatsBase<Map<String, PackageParser.Package>> {
    private static final String USAGE_FILE_MAGIC = "PACKAGE_USAGE__VERSION_";
    private static final String USAGE_FILE_MAGIC_VERSION_1 = "PACKAGE_USAGE__VERSION_1";
    private boolean mIsHistoricalPackageUsageAvailable;

    PackageUsage() {
        super("package-usage.list", "PackageUsage_DiskWriter", true);
        this.mIsHistoricalPackageUsageAvailable = true;
    }

    boolean isHistoricalPackageUsageAvailable() {
        return this.mIsHistoricalPackageUsageAvailable;
    }

    @Override
    protected void writeInternal(Map<String, PackageParser.Package> map) {
        FileOutputStream fileOutputStreamStartWrite;
        AtomicFile file = getFile();
        try {
            fileOutputStreamStartWrite = file.startWrite();
        } catch (IOException e) {
            e = e;
            fileOutputStreamStartWrite = null;
        }
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStreamStartWrite);
            FileUtils.setPermissions(file.getBaseFile().getPath(), 416, 1000, 1032);
            StringBuilder sb = new StringBuilder();
            sb.append(USAGE_FILE_MAGIC_VERSION_1);
            sb.append('\n');
            bufferedOutputStream.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
            for (PackageParser.Package r5 : map.values()) {
                if (r5.getLatestPackageUseTimeInMills() != 0) {
                    sb.setLength(0);
                    sb.append(r5.packageName);
                    for (long j : r5.mLastPackageUsageTimeInMills) {
                        sb.append(' ');
                        sb.append(j);
                    }
                    sb.append('\n');
                    bufferedOutputStream.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
                }
            }
            bufferedOutputStream.flush();
            file.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e2) {
            e = e2;
            if (fileOutputStreamStartWrite != null) {
                file.failWrite(fileOutputStreamStartWrite);
            }
            Log.e("PackageManager", "Failed to write package usage times", e);
        }
    }

    @Override
    protected void readInternal(Map<String, PackageParser.Package> map) throws Throwable {
        BufferedInputStream bufferedInputStream;
        BufferedInputStream bufferedInputStream2 = null;
        try {
            try {
                bufferedInputStream = new BufferedInputStream(getFile().openRead());
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            e = e2;
        }
        try {
            StringBuffer stringBuffer = new StringBuffer();
            String line = readLine(bufferedInputStream, stringBuffer);
            if (line != null) {
                if (USAGE_FILE_MAGIC_VERSION_1.equals(line)) {
                    readVersion1LP(map, bufferedInputStream, stringBuffer);
                } else {
                    readVersion0LP(map, bufferedInputStream, stringBuffer, line);
                }
            }
            IoUtils.closeQuietly(bufferedInputStream);
        } catch (FileNotFoundException e3) {
            bufferedInputStream2 = bufferedInputStream;
            this.mIsHistoricalPackageUsageAvailable = false;
            IoUtils.closeQuietly(bufferedInputStream2);
        } catch (IOException e4) {
            e = e4;
            bufferedInputStream2 = bufferedInputStream;
            Log.w("PackageManager", "Failed to read package usage times", e);
            IoUtils.closeQuietly(bufferedInputStream2);
        } catch (Throwable th2) {
            th = th2;
            bufferedInputStream2 = bufferedInputStream;
            IoUtils.closeQuietly(bufferedInputStream2);
            throw th;
        }
    }

    private void readVersion0LP(Map<String, PackageParser.Package> map, InputStream inputStream, StringBuffer stringBuffer, String str) throws IOException {
        while (str != null) {
            String[] strArrSplit = str.split(" ");
            if (strArrSplit.length != 2) {
                throw new IOException("Failed to parse " + str + " as package-timestamp pair.");
            }
            PackageParser.Package r1 = map.get(strArrSplit[0]);
            if (r1 != null) {
                long asLong = parseAsLong(strArrSplit[1]);
                for (int i = 0; i < 8; i++) {
                    r1.mLastPackageUsageTimeInMills[i] = asLong;
                }
            }
            str = readLine(inputStream, stringBuffer);
        }
    }

    private void readVersion1LP(Map<String, PackageParser.Package> map, InputStream inputStream, StringBuffer stringBuffer) throws IOException {
        while (true) {
            String line = readLine(inputStream, stringBuffer);
            if (line != null) {
                String[] strArrSplit = line.split(" ");
                if (strArrSplit.length != 9) {
                    throw new IOException("Failed to parse " + line + " as a timestamp array.");
                }
                int i = 0;
                PackageParser.Package r2 = map.get(strArrSplit[0]);
                if (r2 != null) {
                    while (i < 8) {
                        int i2 = i + 1;
                        r2.mLastPackageUsageTimeInMills[i] = parseAsLong(strArrSplit[i2]);
                        i = i2;
                    }
                }
            } else {
                return;
            }
        }
    }

    private long parseAsLong(String str) throws IOException {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new IOException("Failed to parse " + str + " as a long.", e);
        }
    }

    private String readLine(InputStream inputStream, StringBuffer stringBuffer) throws IOException {
        return readToken(inputStream, stringBuffer, '\n');
    }

    private String readToken(InputStream inputStream, StringBuffer stringBuffer, char c) throws IOException {
        stringBuffer.setLength(0);
        while (true) {
            int i = inputStream.read();
            if (i == -1) {
                if (stringBuffer.length() == 0) {
                    return null;
                }
                throw new IOException("Unexpected EOF");
            }
            if (i == c) {
                return stringBuffer.toString();
            }
            stringBuffer.append((char) i);
        }
    }
}
