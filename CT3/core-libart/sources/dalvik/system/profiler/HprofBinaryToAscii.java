package dalvik.system.profiler;

import android.icu.text.PluralRules;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class HprofBinaryToAscii {
    public static void main(String[] args) {
        System.exit(convert(args) ? 0 : 1);
    }

    private static boolean convert(String[] args) throws Throwable {
        if (args.length != 1) {
            usage("binary hprof file argument expected");
            return false;
        }
        File file = new File(args[0]);
        if (!file.exists()) {
            usage("file " + file + " does not exist");
            return false;
        }
        if (startsWithMagic(file)) {
            try {
                HprofData hprofData = readHprof(file);
                return write(hprofData);
            } catch (IOException e) {
                System.out.println("Problem reading binary hprof data from " + file + PluralRules.KEYWORD_RULE_SEPARATOR + e.getMessage());
                return false;
            }
        }
        try {
            HprofData hprofData2 = readSnapshot(file);
            return write(hprofData2);
        } catch (IOException e2) {
            System.out.println("Problem reading snapshot containing binary hprof data from " + file + PluralRules.KEYWORD_RULE_SEPARATOR + e2.getMessage());
            return false;
        }
    }

    private static boolean startsWithMagic(File file) throws Throwable {
        DataInputStream inputStream = null;
        try {
            DataInputStream inputStream2 = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            try {
                boolean z = BinaryHprof.readMagic(inputStream2) != null;
                closeQuietly(inputStream2);
                return z;
            } catch (IOException e) {
                inputStream = inputStream2;
                closeQuietly(inputStream);
                return false;
            } catch (Throwable th) {
                th = th;
                inputStream = inputStream2;
                closeQuietly(inputStream);
                throw th;
            }
        } catch (IOException e2) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static HprofData readHprof(File file) throws Throwable {
        InputStream inputStream;
        InputStream inputStream2 = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
        } catch (Throwable th) {
            th = th;
        }
        try {
            HprofData hprofData = read(inputStream);
            closeQuietly(inputStream);
            return hprofData;
        } catch (Throwable th2) {
            th = th2;
            inputStream2 = inputStream;
            closeQuietly(inputStream2);
            throw th;
        }
    }

    private static HprofData readSnapshot(File file) throws Throwable {
        BufferedInputStream bufferedInputStream = null;
        try {
            BufferedInputStream bufferedInputStream2 = new BufferedInputStream(new FileInputStream(file));
            while (true) {
                try {
                    int ch = bufferedInputStream2.read();
                    if (ch != -1) {
                        if (ch == 10 && bufferedInputStream2.read() == 10) {
                            HprofData hprofData = read(bufferedInputStream2);
                            closeQuietly(bufferedInputStream2);
                            return hprofData;
                        }
                    } else {
                        throw new EOFException("Could not find expected header");
                    }
                } catch (Throwable th) {
                    th = th;
                    bufferedInputStream = bufferedInputStream2;
                    closeQuietly(bufferedInputStream);
                    throw th;
                }
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static HprofData read(InputStream inputStream) throws IOException {
        BinaryHprofReader reader = new BinaryHprofReader(inputStream);
        reader.setStrict(false);
        reader.read();
        return reader.getHprofData();
    }

    private static void closeQuietly(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException e) {
        }
    }

    private static boolean write(HprofData hprofData) {
        try {
            AsciiHprofWriter.write(hprofData, System.out);
            return true;
        } catch (IOException e) {
            System.out.println("Problem writing ASCII hprof data: " + e.getMessage());
            return false;
        }
    }

    private static void usage(String error) {
        System.out.print("ERROR: ");
        System.out.println(error);
        System.out.println();
        System.out.println("usage: HprofBinaryToAscii <binary-hprof-file>");
        System.out.println();
        System.out.println("Reads a binary hprof file and print it in ASCII format");
    }
}
