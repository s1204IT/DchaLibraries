package android.support.v4.graphics;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Typeface;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.provider.FontsContractCompat;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RequiresApi(21)
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
class TypefaceCompatApi21Impl extends TypefaceCompatBaseImpl {
    private static final String TAG = "TypefaceCompatApi21Impl";

    TypefaceCompatApi21Impl() {
    }

    private File getFile(ParcelFileDescriptor fd) {
        try {
            String path = Os.readlink("/proc/self/fd/" + fd.getFd());
            if (!OsConstants.S_ISREG(Os.stat(path).st_mode)) {
                return null;
            }
            return new File(path);
        } catch (ErrnoException e) {
            return null;
        }
    }

    @Override
    public Typeface createFromFontInfo(Context context, CancellationSignal cancellationSignal, @NonNull FontsContractCompat.FontInfo[] fonts, int style) throws Throwable {
        Throwable th;
        Throwable th2;
        if (fonts.length < 1) {
            return null;
        }
        FontsContractCompat.FontInfo bestFont = findBestInfo(fonts, style);
        ContentResolver resolver = context.getContentResolver();
        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(bestFont.getUri(), "r", cancellationSignal);
            try {
                try {
                    File file = getFile(pfd);
                    if (file != null && file.canRead()) {
                        Typeface typefaceCreateFromFile = Typeface.createFromFile(file);
                        if (pfd != null) {
                            pfd.close();
                        }
                        return typefaceCreateFromFile;
                    }
                    FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
                    try {
                        Typeface typefaceCreateFromInputStream = super.createFromInputStream(context, fis);
                        fis.close();
                        if (pfd != null) {
                            pfd.close();
                        }
                        return typefaceCreateFromInputStream;
                    } catch (Throwable th3) {
                        th = th3;
                        th2 = null;
                        if (th2 != null) {
                        }
                    }
                } catch (Throwable th4) {
                    try {
                        throw th4;
                    } catch (Throwable th5) {
                        th = th4;
                        th = th5;
                        if (pfd != null) {
                            throw th;
                        }
                        if (th == null) {
                            pfd.close();
                            throw th;
                        }
                        try {
                            pfd.close();
                            throw th;
                        } catch (Throwable th6) {
                            th.addSuppressed(th6);
                            throw th;
                        }
                    }
                }
            } catch (Throwable th7) {
                th = th7;
                th = null;
                if (pfd != null) {
                }
            }
        } catch (IOException e) {
            return null;
        }
    }
}
