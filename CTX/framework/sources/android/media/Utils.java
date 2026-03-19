package android.media;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

class Utils {
    private static final String TAG = "Utils";

    Utils() {
    }

    public static <T extends Comparable<? super T>> void sortDistinctRanges(Range<T>[] rangeArr) {
        Arrays.sort(rangeArr, new Comparator<Range<T>>() {
            @Override
            public int compare(Range<T> range, Range<T> range2) {
                if (range.getUpper().compareTo(range2.getLower()) < 0) {
                    return -1;
                }
                if (range.getLower().compareTo(range2.getUpper()) > 0) {
                    return 1;
                }
                throw new IllegalArgumentException("sample rate ranges must be distinct (" + range + " and " + range2 + ")");
            }
        });
    }

    public static <T extends Comparable<? super T>> Range<T>[] intersectSortedDistinctRanges(Range<T>[] rangeArr, Range<T>[] rangeArr2) {
        Vector vector = new Vector();
        int i = 0;
        for (Range<T> range : rangeArr2) {
            while (i < rangeArr.length && rangeArr[i].getUpper().compareTo(range.getLower()) < 0) {
                i++;
            }
            while (i < rangeArr.length && rangeArr[i].getUpper().compareTo(range.getUpper()) < 0) {
                vector.add(range.intersect(rangeArr[i]));
                i++;
            }
            if (i == rangeArr.length) {
                break;
            }
            if (rangeArr[i].getLower().compareTo(range.getUpper()) <= 0) {
                vector.add(range.intersect(rangeArr[i]));
            }
        }
        return (Range[]) vector.toArray(new Range[vector.size()]);
    }

    public static <T extends Comparable<? super T>> int binarySearchDistinctRanges(Range<T>[] rangeArr, T t) {
        return Arrays.binarySearch(rangeArr, Range.create(t, t), new Comparator<Range<T>>() {
            @Override
            public int compare(Range<T> range, Range<T> range2) {
                if (range.getUpper().compareTo(range2.getLower()) < 0) {
                    return -1;
                }
                if (range.getLower().compareTo(range2.getUpper()) > 0) {
                    return 1;
                }
                return 0;
            }
        });
    }

    static int gcd(int i, int i2) {
        if (i == 0 && i2 == 0) {
            return 1;
        }
        if (i2 < 0) {
            i2 = -i2;
        }
        if (i < 0) {
            i = -i;
        }
        while (true) {
            int i3 = i2;
            i2 = i;
            if (i2 == 0) {
                return i3;
            }
            i = i3 % i2;
        }
    }

    static Range<Integer> factorRange(Range<Integer> range, int i) {
        if (i == 1) {
            return range;
        }
        return Range.create(Integer.valueOf(divUp(((Integer) range.getLower()).intValue(), i)), Integer.valueOf(((Integer) range.getUpper()).intValue() / i));
    }

    static Range<Long> factorRange(Range<Long> range, long j) {
        if (j == 1) {
            return range;
        }
        return Range.create(Long.valueOf(divUp(((Long) range.getLower()).longValue(), j)), Long.valueOf(((Long) range.getUpper()).longValue() / j));
    }

    private static Rational scaleRatio(Rational rational, int i, int i2) {
        int iGcd = gcd(i, i2);
        return new Rational((int) (((double) rational.getNumerator()) * ((double) (i / iGcd))), (int) (((double) rational.getDenominator()) * ((double) (i2 / iGcd))));
    }

    static Range<Rational> scaleRange(Range<Rational> range, int i, int i2) {
        if (i == i2) {
            return range;
        }
        return Range.create(scaleRatio((Rational) range.getLower(), i, i2), scaleRatio((Rational) range.getUpper(), i, i2));
    }

    static Range<Integer> alignRange(Range<Integer> range, int i) {
        return range.intersect(Integer.valueOf(divUp(((Integer) range.getLower()).intValue(), i) * i), Integer.valueOf((((Integer) range.getUpper()).intValue() / i) * i));
    }

    static int divUp(int i, int i2) {
        return ((i + i2) - 1) / i2;
    }

    static long divUp(long j, long j2) {
        return ((j + j2) - 1) / j2;
    }

    private static long lcm(int i, int i2) {
        if (i == 0 || i2 == 0) {
            throw new IllegalArgumentException("lce is not defined for zero arguments");
        }
        return (((long) i) * ((long) i2)) / ((long) gcd(i, i2));
    }

    static Range<Integer> intRangeFor(double d) {
        return Range.create(Integer.valueOf((int) d), Integer.valueOf((int) Math.ceil(d)));
    }

    static Range<Long> longRangeFor(double d) {
        return Range.create(Long.valueOf((long) d), Long.valueOf((long) Math.ceil(d)));
    }

    static Size parseSize(Object obj, Size size) {
        try {
            return Size.parseSize((String) obj);
        } catch (ClassCastException e) {
            Log.w(TAG, "could not parse size '" + obj + "'");
            return size;
        } catch (NullPointerException e2) {
            return size;
        } catch (NumberFormatException e3) {
            Log.w(TAG, "could not parse size '" + obj + "'");
            return size;
        }
    }

    static int parseIntSafely(Object obj, int i) {
        if (obj == null) {
            return i;
        }
        try {
            return Integer.parseInt((String) obj);
        } catch (ClassCastException e) {
            Log.w(TAG, "could not parse integer '" + obj + "'");
            return i;
        } catch (NullPointerException e2) {
            return i;
        } catch (NumberFormatException e3) {
            Log.w(TAG, "could not parse integer '" + obj + "'");
            return i;
        }
    }

    static Range<Integer> parseIntRange(Object obj, Range<Integer> range) {
        try {
            String str = (String) obj;
            int iIndexOf = str.indexOf(45);
            if (iIndexOf >= 0) {
                return Range.create(Integer.valueOf(Integer.parseInt(str.substring(0, iIndexOf), 10)), Integer.valueOf(Integer.parseInt(str.substring(iIndexOf + 1), 10)));
            }
            int i = Integer.parseInt(str);
            return Range.create(Integer.valueOf(i), Integer.valueOf(i));
        } catch (ClassCastException e) {
            Log.w(TAG, "could not parse integer range '" + obj + "'");
            return range;
        } catch (NullPointerException e2) {
            return range;
        } catch (NumberFormatException e3) {
            Log.w(TAG, "could not parse integer range '" + obj + "'");
            return range;
        } catch (IllegalArgumentException e4) {
            Log.w(TAG, "could not parse integer range '" + obj + "'");
            return range;
        }
    }

    static Range<Long> parseLongRange(Object obj, Range<Long> range) {
        try {
            String str = (String) obj;
            int iIndexOf = str.indexOf(45);
            if (iIndexOf >= 0) {
                return Range.create(Long.valueOf(Long.parseLong(str.substring(0, iIndexOf), 10)), Long.valueOf(Long.parseLong(str.substring(iIndexOf + 1), 10)));
            }
            long j = Long.parseLong(str);
            return Range.create(Long.valueOf(j), Long.valueOf(j));
        } catch (ClassCastException e) {
            Log.w(TAG, "could not parse long range '" + obj + "'");
            return range;
        } catch (NullPointerException e2) {
            return range;
        } catch (NumberFormatException e3) {
            Log.w(TAG, "could not parse long range '" + obj + "'");
            return range;
        } catch (IllegalArgumentException e4) {
            Log.w(TAG, "could not parse long range '" + obj + "'");
            return range;
        }
    }

    static Range<Rational> parseRationalRange(Object obj, Range<Rational> range) {
        try {
            String str = (String) obj;
            int iIndexOf = str.indexOf(45);
            if (iIndexOf >= 0) {
                return Range.create(Rational.parseRational(str.substring(0, iIndexOf)), Rational.parseRational(str.substring(iIndexOf + 1)));
            }
            Rational rational = Rational.parseRational(str);
            return Range.create(rational, rational);
        } catch (ClassCastException e) {
            Log.w(TAG, "could not parse rational range '" + obj + "'");
            return range;
        } catch (NumberFormatException e2) {
            Log.w(TAG, "could not parse rational range '" + obj + "'");
            return range;
        } catch (IllegalArgumentException e3) {
            Log.w(TAG, "could not parse rational range '" + obj + "'");
            return range;
        } catch (NullPointerException e4) {
            return range;
        }
    }

    static Pair<Size, Size> parseSizeRange(Object obj) {
        try {
            String str = (String) obj;
            int iIndexOf = str.indexOf(45);
            if (iIndexOf >= 0) {
                return Pair.create(Size.parseSize(str.substring(0, iIndexOf)), Size.parseSize(str.substring(iIndexOf + 1)));
            }
            Size size = Size.parseSize(str);
            return Pair.create(size, size);
        } catch (ClassCastException e) {
            Log.w(TAG, "could not parse size range '" + obj + "'");
            return null;
        } catch (IllegalArgumentException e2) {
            Log.w(TAG, "could not parse size range '" + obj + "'");
            return null;
        } catch (NullPointerException e3) {
            return null;
        } catch (NumberFormatException e4) {
            Log.w(TAG, "could not parse size range '" + obj + "'");
            return null;
        }
    }

    public static File getUniqueExternalFile(Context context, String str, String str2, String str3) {
        File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(str);
        externalStoragePublicDirectory.mkdirs();
        try {
            return FileUtils.buildUniqueFile(externalStoragePublicDirectory, str3, str2);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to get a unique file name: " + e);
            return null;
        }
    }

    static String getFileDisplayNameFromUri(Context context, Uri uri) {
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            return uri.getLastPathSegment();
        }
        if ("content".equals(scheme)) {
            Cursor cursorQuery = context.getContentResolver().query(uri, new String[]{"_display_name"}, null, null, null);
            Throwable th = null;
            try {
                if (cursorQuery != null) {
                    if (cursorQuery.getCount() != 0) {
                        cursorQuery.moveToFirst();
                        String string = cursorQuery.getString(cursorQuery.getColumnIndex("_display_name"));
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return string;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (Throwable th2) {
                if (cursorQuery != null) {
                    if (0 != 0) {
                        try {
                            cursorQuery.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        cursorQuery.close();
                    }
                }
                throw th2;
            }
        }
        return uri.toString();
    }
}
