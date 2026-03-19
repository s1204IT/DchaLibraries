package android.support.v4.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.CancellationSignal;
import android.support.v4.content.res.FontResourcesParserCompat;
import android.support.v4.provider.FontsContractCompat;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

class TypefaceCompatBaseImpl {

    private interface StyleExtractor<T> {
        int getWeight(T t);

        boolean isItalic(T t);
    }

    TypefaceCompatBaseImpl() {
    }

    private static <T> T findBestFont(T[] fonts, int style, StyleExtractor<T> extractor) {
        int targetWeight = (style & 1) == 0 ? 400 : 700;
        boolean isTargetItalic = (style & 2) != 0;
        int bestScore = Integer.MAX_VALUE;
        T best = null;
        for (T font : fonts) {
            int score = (Math.abs(extractor.getWeight(font) - targetWeight) * 2) + (extractor.isItalic(font) == isTargetItalic ? 0 : 1);
            if (best == null || bestScore > score) {
                best = font;
                bestScore = score;
            }
        }
        return best;
    }

    protected FontsContractCompat.FontInfo findBestInfo(FontsContractCompat.FontInfo[] fonts, int style) {
        return (FontsContractCompat.FontInfo) findBestFont(fonts, style, new StyleExtractor<FontsContractCompat.FontInfo>() {
            @Override
            public int getWeight(FontsContractCompat.FontInfo info) {
                return info.getWeight();
            }

            @Override
            public boolean isItalic(FontsContractCompat.FontInfo info) {
                return info.isItalic();
            }
        });
    }

    protected Typeface createFromInputStream(Context context, InputStream is) {
        File tmpFile = TypefaceCompatUtil.getTempFile(context);
        if (tmpFile == null) {
            return null;
        }
        try {
            if (TypefaceCompatUtil.copyToFile(tmpFile, is)) {
                Typeface typefaceCreateFromFile = Typeface.createFromFile(tmpFile.getPath());
                tmpFile.delete();
                return typefaceCreateFromFile;
            }
        } catch (RuntimeException e) {
        } catch (Throwable th) {
            tmpFile.delete();
            throw th;
        }
        tmpFile.delete();
        return null;
    }

    public Typeface createFromFontInfo(Context context, CancellationSignal cancellationSignal, FontsContractCompat.FontInfo[] fonts, int style) {
        if (fonts.length < 1) {
            return null;
        }
        FontsContractCompat.FontInfo font = findBestInfo(fonts, style);
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(font.getUri());
            return createFromInputStream(context, is);
        } catch (IOException e) {
            return null;
        } finally {
            TypefaceCompatUtil.closeQuietly(is);
        }
    }

    private FontResourcesParserCompat.FontFileResourceEntry findBestEntry(FontResourcesParserCompat.FontFamilyFilesResourceEntry entry, int style) {
        return (FontResourcesParserCompat.FontFileResourceEntry) findBestFont(entry.getEntries(), style, new StyleExtractor<FontResourcesParserCompat.FontFileResourceEntry>() {
            @Override
            public int getWeight(FontResourcesParserCompat.FontFileResourceEntry entry2) {
                return entry2.getWeight();
            }

            @Override
            public boolean isItalic(FontResourcesParserCompat.FontFileResourceEntry entry2) {
                return entry2.isItalic();
            }
        });
    }

    public Typeface createFromFontFamilyFilesResourceEntry(Context context, FontResourcesParserCompat.FontFamilyFilesResourceEntry entry, Resources resources, int style) {
        FontResourcesParserCompat.FontFileResourceEntry best = findBestEntry(entry, style);
        if (best == null) {
            return null;
        }
        return TypefaceCompat.createFromResourcesFontFile(context, resources, best.getResourceId(), best.getFileName(), style);
    }

    public Typeface createFromResourcesFontFile(Context context, Resources resources, int id, String path, int style) {
        File tmpFile = TypefaceCompatUtil.getTempFile(context);
        if (tmpFile == null) {
            return null;
        }
        try {
            if (TypefaceCompatUtil.copyToFile(tmpFile, resources, id)) {
                Typeface typefaceCreateFromFile = Typeface.createFromFile(tmpFile.getPath());
                tmpFile.delete();
                return typefaceCreateFromFile;
            }
        } catch (RuntimeException e) {
        } catch (Throwable th) {
            tmpFile.delete();
            throw th;
        }
        tmpFile.delete();
        return null;
    }
}
