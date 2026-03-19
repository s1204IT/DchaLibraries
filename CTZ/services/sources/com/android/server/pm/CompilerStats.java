package com.android.server.pm;

import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Log;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.IndentingPrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import libcore.io.IoUtils;

class CompilerStats extends AbstractStatsBase<Void> {
    private static final int COMPILER_STATS_VERSION = 1;
    private static final String COMPILER_STATS_VERSION_HEADER = "PACKAGE_MANAGER__COMPILER_STATS__";
    private final Map<String, PackageStats> packageStats;

    static class PackageStats {
        private final Map<String, Long> compileTimePerCodePath = new ArrayMap(2);
        private final String packageName;

        public PackageStats(String str) {
            this.packageName = str;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public long getCompileTime(String str) {
            String storedPathFromCodePath = getStoredPathFromCodePath(str);
            synchronized (this.compileTimePerCodePath) {
                Long l = this.compileTimePerCodePath.get(storedPathFromCodePath);
                if (l == null) {
                    return 0L;
                }
                return l.longValue();
            }
        }

        public void setCompileTime(String str, long j) {
            String storedPathFromCodePath = getStoredPathFromCodePath(str);
            synchronized (this.compileTimePerCodePath) {
                try {
                    if (j <= 0) {
                        this.compileTimePerCodePath.remove(storedPathFromCodePath);
                    } else {
                        this.compileTimePerCodePath.put(storedPathFromCodePath, Long.valueOf(j));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        private static String getStoredPathFromCodePath(String str) {
            return str.substring(str.lastIndexOf(File.separatorChar) + 1);
        }

        public void dump(IndentingPrintWriter indentingPrintWriter) {
            synchronized (this.compileTimePerCodePath) {
                if (this.compileTimePerCodePath.size() == 0) {
                    indentingPrintWriter.println("(No recorded stats)");
                } else {
                    for (Map.Entry<String, Long> entry : this.compileTimePerCodePath.entrySet()) {
                        indentingPrintWriter.println(" " + entry.getKey() + " - " + entry.getValue());
                    }
                }
            }
        }
    }

    public CompilerStats() {
        super("package-cstats.list", "CompilerStats_DiskWriter", false);
        this.packageStats = new HashMap();
    }

    public PackageStats getPackageStats(String str) {
        PackageStats packageStats;
        synchronized (this.packageStats) {
            packageStats = this.packageStats.get(str);
        }
        return packageStats;
    }

    public void setPackageStats(String str, PackageStats packageStats) {
        synchronized (this.packageStats) {
            this.packageStats.put(str, packageStats);
        }
    }

    public PackageStats createPackageStats(String str) {
        PackageStats packageStats;
        synchronized (this.packageStats) {
            packageStats = new PackageStats(str);
            this.packageStats.put(str, packageStats);
        }
        return packageStats;
    }

    public PackageStats getOrCreatePackageStats(String str) {
        synchronized (this.packageStats) {
            PackageStats packageStats = this.packageStats.get(str);
            if (packageStats != null) {
                return packageStats;
            }
            return createPackageStats(str);
        }
    }

    public void deletePackageStats(String str) {
        synchronized (this.packageStats) {
            this.packageStats.remove(str);
        }
    }

    public void write(Writer writer) {
        FastPrintWriter fastPrintWriter = new FastPrintWriter(writer);
        fastPrintWriter.print(COMPILER_STATS_VERSION_HEADER);
        fastPrintWriter.println(1);
        synchronized (this.packageStats) {
            for (PackageStats packageStats : this.packageStats.values()) {
                synchronized (packageStats.compileTimePerCodePath) {
                    if (!packageStats.compileTimePerCodePath.isEmpty()) {
                        fastPrintWriter.println(packageStats.getPackageName());
                        for (Map.Entry entry : packageStats.compileTimePerCodePath.entrySet()) {
                            fastPrintWriter.println("-" + ((String) entry.getKey()) + ":" + entry.getValue());
                        }
                    }
                }
            }
        }
        fastPrintWriter.flush();
    }

    public boolean read(Reader reader) {
        String line;
        synchronized (this.packageStats) {
            this.packageStats.clear();
            try {
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line2 = bufferedReader.readLine();
                if (line2 == null) {
                    throw new IllegalArgumentException("No version line found.");
                }
                if (!line2.startsWith(COMPILER_STATS_VERSION_HEADER)) {
                    throw new IllegalArgumentException("Invalid version line: " + line2);
                }
                int i = Integer.parseInt(line2.substring(COMPILER_STATS_VERSION_HEADER.length()));
                if (i != 1) {
                    throw new IllegalArgumentException("Unexpected version: " + i);
                }
                PackageStats packageStats = new PackageStats("fake package");
                while (true) {
                    line = bufferedReader.readLine();
                    if (line != null) {
                        if (line.startsWith("-")) {
                            int iIndexOf = line.indexOf(58);
                            if (iIndexOf == -1 || iIndexOf == 1) {
                                break;
                            }
                            packageStats.setCompileTime(line.substring(1, iIndexOf), Long.parseLong(line.substring(iIndexOf + 1)));
                        } else {
                            packageStats = getOrCreatePackageStats(line);
                        }
                    }
                }
                throw new IllegalArgumentException("Could not parse data " + line);
            } catch (Exception e) {
                Log.e("PackageManager", "Error parsing compiler stats", e);
                return false;
            }
        }
        return true;
    }

    void writeNow() {
        writeNow(null);
    }

    boolean maybeWriteAsync() {
        return maybeWriteAsync(null);
    }

    @Override
    protected void writeInternal(Void r3) {
        FileOutputStream fileOutputStreamStartWrite;
        AtomicFile file = getFile();
        try {
            fileOutputStreamStartWrite = file.startWrite();
        } catch (IOException e) {
            e = e;
            fileOutputStreamStartWrite = null;
        }
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStreamStartWrite);
            write(outputStreamWriter);
            outputStreamWriter.flush();
            file.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e2) {
            e = e2;
            if (fileOutputStreamStartWrite != null) {
                file.failWrite(fileOutputStreamStartWrite);
            }
            Log.e("PackageManager", "Failed to write compiler stats", e);
        }
    }

    void read() {
        read((Void) null);
    }

    @Override
    protected void readInternal(Void r4) throws Throwable {
        BufferedReader bufferedReader;
        BufferedReader bufferedReader2 = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(getFile().openRead()));
        } catch (FileNotFoundException e) {
        } catch (Throwable th) {
            th = th;
        }
        try {
            read((Reader) bufferedReader);
            IoUtils.closeQuietly(bufferedReader);
        } catch (FileNotFoundException e2) {
            bufferedReader2 = bufferedReader;
            IoUtils.closeQuietly(bufferedReader2);
        } catch (Throwable th2) {
            th = th2;
            bufferedReader2 = bufferedReader;
            IoUtils.closeQuietly(bufferedReader2);
            throw th;
        }
    }
}
