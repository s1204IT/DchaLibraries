package com.android.managedprovisioning.common;

import android.accounts.Account;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.PersistableBundle;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.function.Function;

public class StoreUtils {

    public interface TextFileReader {
        String read(File file) throws IOException;
    }

    public static Account persistableBundleToAccount(PersistableBundle persistableBundle) {
        return new Account(persistableBundle.getString("account-name"), persistableBundle.getString("account-type"));
    }

    public static PersistableBundle accountToPersistableBundle(Account account) {
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putString("account-name", account.name);
        persistableBundle.putString("account-type", account.type);
        return persistableBundle;
    }

    public static String componentNameToString(ComponentName componentName) {
        if (componentName == null) {
            return null;
        }
        return componentName.getPackageName() + "/" + componentName.getClassName();
    }

    public static ComponentName stringToComponentName(String str) {
        int i;
        int iIndexOf = str.indexOf(47);
        if (iIndexOf < 0 || (i = iIndexOf + 1) >= str.length()) {
            return null;
        }
        return new ComponentName(str.substring(0, iIndexOf), str.substring(i));
    }

    public static Locale stringToLocale(String str) throws IllformedLocaleException {
        if (str != null) {
            return new Locale.Builder().setLanguageTag(str.replace("_", "-")).build();
        }
        return null;
    }

    public static String localeToString(Locale locale) {
        if (locale != null) {
            return locale.toLanguageTag();
        }
        return null;
    }

    public static byte[] stringToByteArray(String str) throws NumberFormatException {
        try {
            return Base64.decode(str, 8);
        } catch (IllegalArgumentException e) {
            throw new NumberFormatException("Incorrect format. Should be Url-safe Base64 encoded.");
        }
    }

    public static String byteArrayToString(byte[] bArr) {
        return Base64.encodeToString(bArr, 11);
    }

    public static void putIntegerIfNotNull(PersistableBundle persistableBundle, String str, Integer num) {
        if (num != null) {
            persistableBundle.putInt(str, num.intValue());
        }
    }

    public static void putPersistableBundlableIfNotNull(PersistableBundle persistableBundle, String str, PersistableBundlable persistableBundlable) {
        if (persistableBundlable != null) {
            persistableBundle.putPersistableBundle(str, persistableBundlable.toPersistableBundle());
        }
    }

    public static <E> E getObjectAttrFromPersistableBundle(PersistableBundle persistableBundle, String str, Function<PersistableBundle, E> function) {
        PersistableBundle persistableBundle2 = persistableBundle.getPersistableBundle(str);
        if (persistableBundle2 == null) {
            return null;
        }
        return function.apply(persistableBundle2);
    }

    public static <E> E getStringAttrFromPersistableBundle(PersistableBundle persistableBundle, String str, Function<String, E> function) {
        String string = persistableBundle.getString(str);
        if (string == null) {
            return null;
        }
        return function.apply(string);
    }

    public static Integer getIntegerAttrFromPersistableBundle(PersistableBundle persistableBundle, String str) {
        if (persistableBundle.containsKey(str)) {
            return Integer.valueOf(persistableBundle.getInt(str));
        }
        return null;
    }

    public static boolean copyUriIntoFile(ContentResolver contentResolver, Uri uri, File file) {
        Throwable th;
        Throwable th2;
        try {
            InputStream inputStreamOpenInputStream = contentResolver.openInputStream(uri);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try {
                    copyStream(inputStreamOpenInputStream, fileOutputStream);
                    $closeResource(null, fileOutputStream);
                    ProvisionLogger.logi("Successfully copy from uri " + uri + " to " + file);
                    return true;
                } catch (Throwable th3) {
                    try {
                        throw th3;
                    } catch (Throwable th4) {
                        th = th3;
                        th2 = th4;
                        $closeResource(th, fileOutputStream);
                        throw th2;
                    }
                }
            } finally {
                if (inputStreamOpenInputStream != null) {
                    $closeResource(null, inputStreamOpenInputStream);
                }
            }
        } catch (IOException | SecurityException e) {
            ProvisionLogger.logi("Could not write file from " + uri + " to " + file, e);
            file.delete();
            return false;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public static String readString(File file) throws Exception {
        Throwable th;
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                copyStream(fileInputStream, byteArrayOutputStream);
                String string = byteArrayOutputStream.toString();
                $closeResource(null, byteArrayOutputStream);
                return string;
            } catch (Throwable th2) {
                th = th2;
                th = null;
                $closeResource(th, byteArrayOutputStream);
                throw th;
            }
        } finally {
            $closeResource(null, fileInputStream);
        }
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[1024];
        while (true) {
            int i = inputStream.read(bArr);
            if (i != -1) {
                outputStream.write(bArr, 0, i);
            } else {
                return;
            }
        }
    }
}
