package com.android.gallery3d.data;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.IdentityCache;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class Path {
    private static Path sRoot = new Path(null, "ROOT");
    private IdentityCache<String, Path> mChildren;
    private WeakReference<MediaObject> mObject;
    private final Path mParent;
    private final String mSegment;

    private Path(Path path, String str) {
        this.mParent = path;
        this.mSegment = str;
    }

    public Path getChild(String str) {
        synchronized (Path.class) {
            if (this.mChildren == null) {
                this.mChildren = new IdentityCache<>();
            } else {
                Path path = this.mChildren.get(str);
                if (path != null) {
                    return path;
                }
            }
            Path path2 = new Path(this, str);
            this.mChildren.put(str, path2);
            return path2;
        }
    }

    public Path getParent() {
        Path path;
        synchronized (Path.class) {
            path = this.mParent;
        }
        return path;
    }

    public Path getChild(int i) {
        return getChild(String.valueOf(i));
    }

    public Path getChild(long j) {
        return getChild(String.valueOf(j));
    }

    public void setObject(MediaObject mediaObject) {
        synchronized (Path.class) {
            Utils.assertTrue(this.mObject == null || this.mObject.get() == null);
            this.mObject = new WeakReference<>(mediaObject);
        }
    }

    MediaObject getObject() {
        MediaObject mediaObject;
        synchronized (Path.class) {
            mediaObject = this.mObject == null ? null : this.mObject.get();
        }
        return mediaObject;
    }

    public String toString() {
        String string;
        synchronized (Path.class) {
            StringBuilder sb = new StringBuilder();
            for (String str : split()) {
                sb.append("/");
                sb.append(str);
            }
            string = sb.toString();
        }
        return string;
    }

    public boolean equalsIgnoreCase(String str) {
        return toString().equalsIgnoreCase(str);
    }

    public static Path fromString(String str) {
        Path child;
        synchronized (Path.class) {
            String[] strArrSplit = split(str);
            child = sRoot;
            for (String str2 : strArrSplit) {
                child = child.getChild(str2);
            }
        }
        return child;
    }

    public String[] split() {
        String[] strArr;
        synchronized (Path.class) {
            int i = 0;
            for (Path path = this; path != sRoot; path = path.mParent) {
                i++;
            }
            strArr = new String[i];
            int i2 = i - 1;
            Path path2 = this;
            while (path2 != sRoot) {
                strArr[i2] = path2.mSegment;
                path2 = path2.mParent;
                i2--;
            }
        }
        return strArr;
    }

    public static String[] split(String str) {
        int length = str.length();
        if (length == 0) {
            return new String[0];
        }
        if (str.charAt(0) != '/') {
            throw new RuntimeException("malformed path:" + str);
        }
        ArrayList arrayList = new ArrayList();
        int i = 1;
        while (i < length) {
            int i2 = 0;
            int i3 = i;
            while (i3 < length) {
                char cCharAt = str.charAt(i3);
                if (cCharAt != '{') {
                    if (cCharAt != '}') {
                        if (i2 == 0 && cCharAt == '/') {
                            break;
                        }
                    } else {
                        i2--;
                    }
                } else {
                    i2++;
                }
                i3++;
            }
            if (i2 != 0) {
                throw new RuntimeException("unbalanced brace in path:" + str);
            }
            arrayList.add(str.substring(i, i3));
            i = i3 + 1;
        }
        String[] strArr = new String[arrayList.size()];
        arrayList.toArray(strArr);
        return strArr;
    }

    public static String[] splitSequence(String str) {
        int length = str.length();
        if (str.charAt(0) == '{') {
            int i = 1;
            int i2 = length - 1;
            if (str.charAt(i2) == '}') {
                ArrayList arrayList = new ArrayList();
                while (i < i2) {
                    int i3 = 0;
                    int i4 = i;
                    while (i4 < i2) {
                        char cCharAt = str.charAt(i4);
                        if (cCharAt != '{') {
                            if (cCharAt != '}') {
                                if (i3 == 0 && cCharAt == ',') {
                                    break;
                                }
                            } else {
                                i3--;
                            }
                        } else {
                            i3++;
                        }
                        i4++;
                    }
                    if (i3 != 0) {
                        throw new RuntimeException("unbalanced brace in path:" + str);
                    }
                    arrayList.add(str.substring(i, i4));
                    i = i4 + 1;
                }
                String[] strArr = new String[arrayList.size()];
                arrayList.toArray(strArr);
                return strArr;
            }
        }
        throw new RuntimeException("bad sequence: " + str);
    }

    public String getPrefix() {
        return this == sRoot ? "" : getPrefixPath().mSegment;
    }

    public Path getPrefixPath() {
        Path path;
        synchronized (Path.class) {
            if (this == sRoot) {
                throw new IllegalStateException();
            }
            path = this;
            while (path.mParent != sRoot) {
                path = path.mParent;
            }
        }
        return path;
    }

    public String getSuffix() {
        return this.mSegment;
    }

    public void clearObject() {
        if (this.mObject != null) {
            this.mObject.clear();
        }
        this.mObject = null;
    }
}
