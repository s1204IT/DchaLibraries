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

    private Path(Path parent, String segment) {
        this.mParent = parent;
        this.mSegment = segment;
    }

    public Path getChild(String segment) {
        synchronized (Path.class) {
            if (this.mChildren == null) {
                this.mChildren = new IdentityCache<>();
            } else {
                Path p = this.mChildren.get(segment);
                if (p != null) {
                    return p;
                }
            }
            Path p2 = new Path(this, segment);
            this.mChildren.put(segment, p2);
            return p2;
        }
    }

    public Path getParent() {
        Path path;
        synchronized (Path.class) {
            path = this.mParent;
        }
        return path;
    }

    public Path getChild(int segment) {
        return getChild(String.valueOf(segment));
    }

    public Path getChild(long segment) {
        return getChild(String.valueOf(segment));
    }

    public void setObject(MediaObject object) {
        synchronized (Path.class) {
            Utils.assertTrue(this.mObject == null || this.mObject.get() == null);
            this.mObject = new WeakReference<>(object);
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
            String[] segments = split();
            for (String str : segments) {
                sb.append("/");
                sb.append(str);
            }
            string = sb.toString();
        }
        return string;
    }

    public boolean equalsIgnoreCase(String p) {
        String path = toString();
        return path.equalsIgnoreCase(p);
    }

    public static Path fromString(String s) {
        Path current;
        synchronized (Path.class) {
            String[] segments = split(s);
            current = sRoot;
            for (String str : segments) {
                current = current.getChild(str);
            }
        }
        return current;
    }

    public String[] split() {
        String[] segments;
        synchronized (Path.class) {
            int n = 0;
            for (Path p = this; p != sRoot; p = p.mParent) {
                n++;
            }
            segments = new String[n];
            int i = n - 1;
            Path p2 = this;
            int i2 = i;
            while (p2 != sRoot) {
                segments[i2] = p2.mSegment;
                p2 = p2.mParent;
                i2--;
            }
        }
        return segments;
    }

    public static String[] split(String s) {
        int n = s.length();
        if (n == 0) {
            return new String[0];
        }
        if (s.charAt(0) != '/') {
            throw new RuntimeException("malformed path:" + s);
        }
        ArrayList<String> segments = new ArrayList<>();
        int i = 1;
        while (i < n) {
            int brace = 0;
            int j = i;
            while (j < n) {
                char c = s.charAt(j);
                if (c != '{') {
                    if (c != '}') {
                        if (brace == 0 && c == '/') {
                            break;
                        }
                    } else {
                        brace--;
                    }
                } else {
                    brace++;
                }
                j++;
            }
            if (brace != 0) {
                throw new RuntimeException("unbalanced brace in path:" + s);
            }
            segments.add(s.substring(i, j));
            i = j + 1;
        }
        String[] result = new String[segments.size()];
        segments.toArray(result);
        return result;
    }

    public static String[] splitSequence(String s) {
        int j;
        int n = s.length();
        if (s.charAt(0) != '{' || s.charAt(n - 1) != '}') {
            throw new RuntimeException("bad sequence: " + s);
        }
        ArrayList<String> segments = new ArrayList<>();
        for (int i = 1; i < n - 1; i = j + 1) {
            int brace = 0;
            j = i;
            while (j < n - 1) {
                char c = s.charAt(j);
                if (c == '{') {
                    brace++;
                } else if (c != '}') {
                    if (brace == 0 && c == ',') {
                        break;
                    }
                } else {
                    brace--;
                }
                j++;
            }
            if (brace != 0) {
                throw new RuntimeException("unbalanced brace in path:" + s);
            }
            segments.add(s.substring(i, j));
        }
        String[] result = new String[segments.size()];
        segments.toArray(result);
        return result;
    }

    public String getPrefix() {
        return this == sRoot ? "" : getPrefixPath().mSegment;
    }

    public Path getPrefixPath() {
        Path current;
        synchronized (Path.class) {
            current = this;
            if (current == sRoot) {
                throw new IllegalStateException();
            }
            while (current.mParent != sRoot) {
                current = current.mParent;
            }
        }
        return current;
    }

    public String getSuffix() {
        return this.mSegment;
    }
}
