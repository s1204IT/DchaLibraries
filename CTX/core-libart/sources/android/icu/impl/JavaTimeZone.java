package android.icu.impl;

import android.icu.util.TimeZone;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TreeSet;

public class JavaTimeZone extends TimeZone {
    private static final TreeSet<String> AVAILABLESET = new TreeSet<>();
    private static Method mObservesDaylightTime = null;
    private static final long serialVersionUID = 6977448185543929364L;
    private volatile transient boolean isFrozen;
    private transient Calendar javacal;
    private java.util.TimeZone javatz;

    static {
        for (String str : java.util.TimeZone.getAvailableIDs()) {
            AVAILABLESET.add(str);
        }
        try {
            mObservesDaylightTime = java.util.TimeZone.class.getMethod("observesDaylightTime", (Class[]) null);
        } catch (NoSuchMethodException e) {
        } catch (SecurityException e2) {
        }
    }

    public JavaTimeZone() {
        this(java.util.TimeZone.getDefault(), null);
    }

    public JavaTimeZone(java.util.TimeZone timeZone, String str) {
        this.isFrozen = false;
        str = str == null ? timeZone.getID() : str;
        this.javatz = timeZone;
        setID(str);
        this.javacal = new GregorianCalendar(this.javatz);
    }

    public static JavaTimeZone createTimeZone(String str) {
        java.util.TimeZone timeZone;
        if (AVAILABLESET.contains(str)) {
            timeZone = java.util.TimeZone.getTimeZone(str);
        } else {
            timeZone = null;
        }
        if (timeZone == null) {
            boolean[] zArr = new boolean[1];
            String canonicalID = TimeZone.getCanonicalID(str, zArr);
            if (zArr[0] && AVAILABLESET.contains(canonicalID)) {
                timeZone = java.util.TimeZone.getTimeZone(canonicalID);
            }
        }
        if (timeZone == null) {
            return null;
        }
        return new JavaTimeZone(timeZone, str);
    }

    @Override
    public int getOffset(int i, int i2, int i3, int i4, int i5, int i6) {
        return this.javatz.getOffset(i, i2, i3, i4, i5, i6);
    }

    @Override
    public void getOffset(long j, boolean z, int[] iArr) {
        int i;
        int i2;
        int i3;
        synchronized (this.javacal) {
            try {
                if (z) {
                    int[] iArr2 = new int[6];
                    Grego.timeToFields(j, iArr2);
                    int i4 = iArr2[5];
                    int i5 = i4 % 1000;
                    int i6 = i4 / 1000;
                    int i7 = i6 % 60;
                    int i8 = i6 / 60;
                    int i9 = i8 % 60;
                    int i10 = i8 / 60;
                    this.javacal.clear();
                    this.javacal.set(iArr2[0], iArr2[1], iArr2[2], i10, i9, i7);
                    this.javacal.set(14, i5);
                    int i11 = this.javacal.get(6);
                    int i12 = this.javacal.get(11);
                    int i13 = this.javacal.get(12);
                    int i14 = this.javacal.get(13);
                    int i15 = this.javacal.get(14);
                    if (iArr2[4] == i11 && i10 == i12) {
                        i = i9;
                        if (i == i13) {
                            i2 = i7;
                            if (i2 != i14 || i5 != i15) {
                            }
                        }
                        if (Math.abs(i11 - iArr2[4]) > 1) {
                            i3 = i11 - iArr2[4];
                        } else {
                            i3 = 1;
                        }
                        this.javacal.setTimeInMillis((this.javacal.getTimeInMillis() - ((long) ((((((((((((i3 * 24) + i12) - i10) * 60) + i13) - i) * 60) + i14) - i2) * 1000) + i15) - i5))) - 1);
                    } else {
                        i = i9;
                    }
                    i2 = i7;
                    if (Math.abs(i11 - iArr2[4]) > 1) {
                    }
                    this.javacal.setTimeInMillis((this.javacal.getTimeInMillis() - ((long) ((((((((((((i3 * 24) + i12) - i10) * 60) + i13) - i) * 60) + i14) - i2) * 1000) + i15) - i5))) - 1);
                } else {
                    this.javacal.setTimeInMillis(j);
                }
                iArr[0] = this.javacal.get(15);
                iArr[1] = this.javacal.get(16);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    @Override
    public int getRawOffset() {
        return this.javatz.getRawOffset();
    }

    @Override
    public boolean inDaylightTime(Date date) {
        return this.javatz.inDaylightTime(date);
    }

    @Override
    public void setRawOffset(int i) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen JavaTimeZone instance.");
        }
        this.javatz.setRawOffset(i);
    }

    @Override
    public boolean useDaylightTime() {
        return this.javatz.useDaylightTime();
    }

    @Override
    public boolean observesDaylightTime() {
        if (mObservesDaylightTime != null) {
            try {
                return ((Boolean) mObservesDaylightTime.invoke(this.javatz, (Object[]) null)).booleanValue();
            } catch (IllegalAccessException e) {
            } catch (IllegalArgumentException e2) {
            } catch (InvocationTargetException e3) {
            }
        }
        return super.observesDaylightTime();
    }

    @Override
    public int getDSTSavings() {
        return this.javatz.getDSTSavings();
    }

    public java.util.TimeZone unwrap() {
        return this.javatz;
    }

    @Override
    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    @Override
    public int hashCode() {
        return super.hashCode() + this.javatz.hashCode();
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.javacal = new GregorianCalendar(this.javatz);
    }

    @Override
    public boolean isFrozen() {
        return this.isFrozen;
    }

    @Override
    public TimeZone freeze() {
        this.isFrozen = true;
        return this;
    }

    @Override
    public TimeZone cloneAsThawed() {
        JavaTimeZone javaTimeZone = (JavaTimeZone) super.cloneAsThawed();
        javaTimeZone.javatz = (java.util.TimeZone) this.javatz.clone();
        javaTimeZone.javacal = new GregorianCalendar(this.javatz);
        javaTimeZone.isFrozen = false;
        return javaTimeZone;
    }
}
