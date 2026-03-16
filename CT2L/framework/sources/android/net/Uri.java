package android.net;

import android.content.ContentResolver;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StrictMode;
import android.telecom.PhoneAccount;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import libcore.net.UriCodec;

public abstract class Uri implements Parcelable, Comparable<Uri> {
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final int NOT_CALCULATED = -2;
    private static final int NOT_FOUND = -1;
    private static final String NOT_HIERARCHICAL = "This isn't a hierarchical URI.";
    private static final int NULL_TYPE_ID = 0;
    private static final String LOG = Uri.class.getSimpleName();
    private static final String NOT_CACHED = new String("NOT CACHED");
    public static final Uri EMPTY = new HierarchicalUri(null, Part.NULL, PathPart.EMPTY, Part.NULL, Part.NULL);
    public static final Parcelable.Creator<Uri> CREATOR = new Parcelable.Creator<Uri>() {
        @Override
        public Uri createFromParcel(Parcel in) {
            int type = in.readInt();
            switch (type) {
                case 0:
                    return null;
                case 1:
                    return StringUri.readFrom(in);
                case 2:
                    return OpaqueUri.readFrom(in);
                case 3:
                    return HierarchicalUri.readFrom(in);
                default:
                    throw new IllegalArgumentException("Unknown URI type: " + type);
            }
        }

        @Override
        public Uri[] newArray(int size) {
            return new Uri[size];
        }
    };
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    public abstract Builder buildUpon();

    public abstract String getAuthority();

    public abstract String getEncodedAuthority();

    public abstract String getEncodedFragment();

    public abstract String getEncodedPath();

    public abstract String getEncodedQuery();

    public abstract String getEncodedSchemeSpecificPart();

    public abstract String getEncodedUserInfo();

    public abstract String getFragment();

    public abstract String getHost();

    public abstract String getLastPathSegment();

    public abstract String getPath();

    public abstract List<String> getPathSegments();

    public abstract int getPort();

    public abstract String getQuery();

    public abstract String getScheme();

    public abstract String getSchemeSpecificPart();

    public abstract String getUserInfo();

    public abstract boolean isHierarchical();

    public abstract boolean isRelative();

    public abstract String toString();

    private Uri() {
    }

    public boolean isOpaque() {
        return !isHierarchical();
    }

    public boolean isAbsolute() {
        return !isRelative();
    }

    public boolean equals(Object o) {
        if (!(o instanceof Uri)) {
            return false;
        }
        Uri other = (Uri) o;
        return toString().equals(other.toString());
    }

    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public int compareTo(Uri other) {
        return toString().compareTo(other.toString());
    }

    public String toSafeString() {
        String scheme = getScheme();
        String ssp = getSchemeSpecificPart();
        if (scheme != null && (scheme.equalsIgnoreCase(PhoneAccount.SCHEME_TEL) || scheme.equalsIgnoreCase("sip") || scheme.equalsIgnoreCase("sms") || scheme.equalsIgnoreCase("smsto") || scheme.equalsIgnoreCase("mailto"))) {
            StringBuilder builder = new StringBuilder(64);
            builder.append(scheme);
            builder.append(':');
            if (ssp != null) {
                for (int i = 0; i < ssp.length(); i++) {
                    char c = ssp.charAt(i);
                    if (c == '-' || c == '@' || c == '.') {
                        builder.append(c);
                    } else {
                        builder.append('x');
                    }
                }
            }
            return builder.toString();
        }
        StringBuilder builder2 = new StringBuilder(64);
        if (scheme != null) {
            builder2.append(scheme);
            builder2.append(':');
        }
        if (ssp != null) {
            builder2.append(ssp);
        }
        return builder2.toString();
    }

    public static Uri parse(String uriString) {
        return new StringUri(uriString);
    }

    public static Uri fromFile(File file) {
        if (file == null) {
            throw new NullPointerException(ContentResolver.SCHEME_FILE);
        }
        PathPart path = PathPart.fromDecoded(file.getAbsolutePath());
        return new HierarchicalUri(ContentResolver.SCHEME_FILE, Part.EMPTY, path, Part.NULL, Part.NULL);
    }

    private static class StringUri extends AbstractHierarchicalUri {
        static final int TYPE_ID = 1;
        private Part authority;
        private volatile int cachedFsi;
        private volatile int cachedSsi;
        private Part fragment;
        private PathPart path;
        private Part query;
        private volatile String scheme;
        private Part ssp;
        private final String uriString;

        private StringUri(String uriString) {
            super();
            this.cachedSsi = -2;
            this.cachedFsi = -2;
            this.scheme = Uri.NOT_CACHED;
            if (uriString == null) {
                throw new NullPointerException("uriString");
            }
            this.uriString = uriString;
        }

        static Uri readFrom(Parcel parcel) {
            return new StringUri(parcel.readString());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(1);
            parcel.writeString(this.uriString);
        }

        private int findSchemeSeparator() {
            if (this.cachedSsi != -2) {
                return this.cachedSsi;
            }
            int iIndexOf = this.uriString.indexOf(58);
            this.cachedSsi = iIndexOf;
            return iIndexOf;
        }

        private int findFragmentSeparator() {
            if (this.cachedFsi != -2) {
                return this.cachedFsi;
            }
            int iIndexOf = this.uriString.indexOf(35, findSchemeSeparator());
            this.cachedFsi = iIndexOf;
            return iIndexOf;
        }

        @Override
        public boolean isHierarchical() {
            int ssi = findSchemeSeparator();
            if (ssi == -1) {
                return true;
            }
            return this.uriString.length() != ssi + 1 && this.uriString.charAt(ssi + 1) == '/';
        }

        @Override
        public boolean isRelative() {
            return findSchemeSeparator() == -1;
        }

        @Override
        public String getScheme() {
            boolean cached = this.scheme != Uri.NOT_CACHED;
            if (cached) {
                return this.scheme;
            }
            String scheme = parseScheme();
            this.scheme = scheme;
            return scheme;
        }

        private String parseScheme() {
            int ssi = findSchemeSeparator();
            if (ssi == -1) {
                return null;
            }
            return this.uriString.substring(0, ssi);
        }

        private Part getSsp() {
            if (this.ssp != null) {
                return this.ssp;
            }
            Part partFromEncoded = Part.fromEncoded(parseSsp());
            this.ssp = partFromEncoded;
            return partFromEncoded;
        }

        @Override
        public String getEncodedSchemeSpecificPart() {
            return getSsp().getEncoded();
        }

        @Override
        public String getSchemeSpecificPart() {
            return getSsp().getDecoded();
        }

        private String parseSsp() {
            int ssi = findSchemeSeparator();
            int fsi = findFragmentSeparator();
            return fsi == -1 ? this.uriString.substring(ssi + 1) : this.uriString.substring(ssi + 1, fsi);
        }

        private Part getAuthorityPart() {
            if (this.authority != null) {
                return this.authority;
            }
            String encodedAuthority = parseAuthority(this.uriString, findSchemeSeparator());
            Part partFromEncoded = Part.fromEncoded(encodedAuthority);
            this.authority = partFromEncoded;
            return partFromEncoded;
        }

        @Override
        public String getEncodedAuthority() {
            return getAuthorityPart().getEncoded();
        }

        @Override
        public String getAuthority() {
            return getAuthorityPart().getDecoded();
        }

        private PathPart getPathPart() {
            if (this.path != null) {
                return this.path;
            }
            PathPart pathPartFromEncoded = PathPart.fromEncoded(parsePath());
            this.path = pathPartFromEncoded;
            return pathPartFromEncoded;
        }

        @Override
        public String getPath() {
            return getPathPart().getDecoded();
        }

        @Override
        public String getEncodedPath() {
            return getPathPart().getEncoded();
        }

        @Override
        public List<String> getPathSegments() {
            return getPathPart().getPathSegments();
        }

        private String parsePath() {
            String uriString = this.uriString;
            int ssi = findSchemeSeparator();
            if (ssi > -1) {
                boolean schemeOnly = ssi + 1 == uriString.length();
                if (schemeOnly || uriString.charAt(ssi + 1) != '/') {
                    return null;
                }
            }
            return parsePath(uriString, ssi);
        }

        private Part getQueryPart() {
            if (this.query != null) {
                return this.query;
            }
            Part partFromEncoded = Part.fromEncoded(parseQuery());
            this.query = partFromEncoded;
            return partFromEncoded;
        }

        @Override
        public String getEncodedQuery() {
            return getQueryPart().getEncoded();
        }

        private String parseQuery() {
            int qsi = this.uriString.indexOf(63, findSchemeSeparator());
            if (qsi == -1) {
                return null;
            }
            int fsi = findFragmentSeparator();
            if (fsi == -1) {
                return this.uriString.substring(qsi + 1);
            }
            if (fsi >= qsi) {
                return this.uriString.substring(qsi + 1, fsi);
            }
            return null;
        }

        @Override
        public String getQuery() {
            return getQueryPart().getDecoded();
        }

        private Part getFragmentPart() {
            if (this.fragment != null) {
                return this.fragment;
            }
            Part partFromEncoded = Part.fromEncoded(parseFragment());
            this.fragment = partFromEncoded;
            return partFromEncoded;
        }

        @Override
        public String getEncodedFragment() {
            return getFragmentPart().getEncoded();
        }

        private String parseFragment() {
            int fsi = findFragmentSeparator();
            if (fsi == -1) {
                return null;
            }
            return this.uriString.substring(fsi + 1);
        }

        @Override
        public String getFragment() {
            return getFragmentPart().getDecoded();
        }

        @Override
        public String toString() {
            return this.uriString;
        }

        static String parseAuthority(String uriString, int ssi) {
            int length = uriString.length();
            if (length <= ssi + 2 || uriString.charAt(ssi + 1) != '/' || uriString.charAt(ssi + 2) != '/') {
                return null;
            }
            for (int end = ssi + 3; end < length; end++) {
                switch (uriString.charAt(end)) {
                    case '#':
                    case '/':
                    case '?':
                        break;
                    default:
                        break;
                }
                return uriString.substring(ssi + 3, end);
            }
            return uriString.substring(ssi + 3, end);
        }

        static String parsePath(String uriString, int ssi) {
            int pathStart;
            int length = uriString.length();
            if (length > ssi + 2 && uriString.charAt(ssi + 1) == '/' && uriString.charAt(ssi + 2) == '/') {
                pathStart = ssi + 3;
                while (pathStart < length) {
                    switch (uriString.charAt(pathStart)) {
                        case '#':
                        case '?':
                            return ProxyInfo.LOCAL_EXCL_LIST;
                        case '/':
                            break;
                        default:
                            pathStart++;
                            break;
                    }
                }
            } else {
                pathStart = ssi + 1;
            }
            for (int pathEnd = pathStart; pathEnd < length; pathEnd++) {
                switch (uriString.charAt(pathEnd)) {
                    case '#':
                    case '?':
                        break;
                    default:
                        break;
                }
                return uriString.substring(pathStart, pathEnd);
            }
            return uriString.substring(pathStart, pathEnd);
        }

        @Override
        public Builder buildUpon() {
            return isHierarchical() ? new Builder().scheme(getScheme()).authority(getAuthorityPart()).path(getPathPart()).query(getQueryPart()).fragment(getFragmentPart()) : new Builder().scheme(getScheme()).opaquePart(getSsp()).fragment(getFragmentPart());
        }
    }

    public static Uri fromParts(String scheme, String ssp, String fragment) {
        if (scheme == null) {
            throw new NullPointerException("scheme");
        }
        if (ssp == null) {
            throw new NullPointerException("ssp");
        }
        return new OpaqueUri(scheme, Part.fromDecoded(ssp), Part.fromDecoded(fragment));
    }

    private static class OpaqueUri extends Uri {
        static final int TYPE_ID = 2;
        private volatile String cachedString;
        private final Part fragment;
        private final String scheme;
        private final Part ssp;

        @Override
        public int compareTo(Uri uri) {
            return super.compareTo(uri);
        }

        private OpaqueUri(String scheme, Part ssp, Part fragment) {
            super();
            this.cachedString = Uri.NOT_CACHED;
            this.scheme = scheme;
            this.ssp = ssp;
            this.fragment = fragment == null ? Part.NULL : fragment;
        }

        static Uri readFrom(Parcel parcel) {
            return new OpaqueUri(parcel.readString(), Part.readFrom(parcel), Part.readFrom(parcel));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(2);
            parcel.writeString(this.scheme);
            this.ssp.writeTo(parcel);
            this.fragment.writeTo(parcel);
        }

        @Override
        public boolean isHierarchical() {
            return false;
        }

        @Override
        public boolean isRelative() {
            return this.scheme == null;
        }

        @Override
        public String getScheme() {
            return this.scheme;
        }

        @Override
        public String getEncodedSchemeSpecificPart() {
            return this.ssp.getEncoded();
        }

        @Override
        public String getSchemeSpecificPart() {
            return this.ssp.getDecoded();
        }

        @Override
        public String getAuthority() {
            return null;
        }

        @Override
        public String getEncodedAuthority() {
            return null;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public String getEncodedPath() {
            return null;
        }

        @Override
        public String getQuery() {
            return null;
        }

        @Override
        public String getEncodedQuery() {
            return null;
        }

        @Override
        public String getFragment() {
            return this.fragment.getDecoded();
        }

        @Override
        public String getEncodedFragment() {
            return this.fragment.getEncoded();
        }

        @Override
        public List<String> getPathSegments() {
            return Collections.emptyList();
        }

        @Override
        public String getLastPathSegment() {
            return null;
        }

        @Override
        public String getUserInfo() {
            return null;
        }

        @Override
        public String getEncodedUserInfo() {
            return null;
        }

        @Override
        public String getHost() {
            return null;
        }

        @Override
        public int getPort() {
            return -1;
        }

        @Override
        public String toString() {
            boolean cached = this.cachedString != Uri.NOT_CACHED;
            if (cached) {
                return this.cachedString;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(this.scheme).append(':');
            sb.append(getEncodedSchemeSpecificPart());
            if (!this.fragment.isEmpty()) {
                sb.append('#').append(this.fragment.getEncoded());
            }
            String string = sb.toString();
            this.cachedString = string;
            return string;
        }

        @Override
        public Builder buildUpon() {
            return new Builder().scheme(this.scheme).opaquePart(this.ssp).fragment(this.fragment);
        }
    }

    static class PathSegments extends AbstractList<String> implements RandomAccess {
        static final PathSegments EMPTY = new PathSegments(null, 0);
        final String[] segments;
        final int size;

        PathSegments(String[] segments, int size) {
            this.segments = segments;
            this.size = size;
        }

        @Override
        public String get(int index) {
            if (index >= this.size) {
                throw new IndexOutOfBoundsException();
            }
            return this.segments[index];
        }

        @Override
        public int size() {
            return this.size;
        }
    }

    static class PathSegmentsBuilder {
        String[] segments;
        int size = 0;

        PathSegmentsBuilder() {
        }

        void add(String segment) {
            if (this.segments == null) {
                this.segments = new String[4];
            } else if (this.size + 1 == this.segments.length) {
                String[] expanded = new String[this.segments.length * 2];
                System.arraycopy(this.segments, 0, expanded, 0, this.segments.length);
                this.segments = expanded;
            }
            String[] strArr = this.segments;
            int i = this.size;
            this.size = i + 1;
            strArr[i] = segment;
        }

        PathSegments build() {
            if (this.segments == null) {
                return PathSegments.EMPTY;
            }
            try {
                return new PathSegments(this.segments, this.size);
            } finally {
                this.segments = null;
            }
        }
    }

    private static abstract class AbstractHierarchicalUri extends Uri {
        private volatile String host;
        private volatile int port;
        private Part userInfo;

        private AbstractHierarchicalUri() {
            super();
            this.host = Uri.NOT_CACHED;
            this.port = -2;
        }

        @Override
        public int compareTo(Uri uri) {
            return super.compareTo(uri);
        }

        @Override
        public String getLastPathSegment() {
            List<String> segments = getPathSegments();
            int size = segments.size();
            if (size == 0) {
                return null;
            }
            return segments.get(size - 1);
        }

        private Part getUserInfoPart() {
            if (this.userInfo != null) {
                return this.userInfo;
            }
            Part partFromEncoded = Part.fromEncoded(parseUserInfo());
            this.userInfo = partFromEncoded;
            return partFromEncoded;
        }

        @Override
        public final String getEncodedUserInfo() {
            return getUserInfoPart().getEncoded();
        }

        private String parseUserInfo() {
            int end;
            String authority = getEncodedAuthority();
            if (authority == null || (end = authority.lastIndexOf(64)) == -1) {
                return null;
            }
            return authority.substring(0, end);
        }

        @Override
        public String getUserInfo() {
            return getUserInfoPart().getDecoded();
        }

        @Override
        public String getHost() {
            boolean cached = this.host != Uri.NOT_CACHED;
            if (cached) {
                return this.host;
            }
            String host = parseHost();
            this.host = host;
            return host;
        }

        private String parseHost() {
            String authority = getEncodedAuthority();
            if (authority == null) {
                return null;
            }
            int userInfoSeparator = authority.lastIndexOf(64);
            int portSeparator = authority.indexOf(58, userInfoSeparator);
            String encodedHost = portSeparator == -1 ? authority.substring(userInfoSeparator + 1) : authority.substring(userInfoSeparator + 1, portSeparator);
            return decode(encodedHost);
        }

        @Override
        public int getPort() {
            if (this.port != -2) {
                return this.port;
            }
            int port = parsePort();
            this.port = port;
            return port;
        }

        private int parsePort() {
            String authority = getEncodedAuthority();
            if (authority == null) {
                return -1;
            }
            int userInfoSeparator = authority.lastIndexOf(64);
            int portSeparator = authority.indexOf(58, userInfoSeparator);
            if (portSeparator == -1) {
                return -1;
            }
            String portString = decode(authority.substring(portSeparator + 1));
            try {
                return Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                Log.w(Uri.LOG, "Error parsing port string.", e);
                return -1;
            }
        }
    }

    private static class HierarchicalUri extends AbstractHierarchicalUri {
        static final int TYPE_ID = 3;
        private final Part authority;
        private final Part fragment;
        private final PathPart path;
        private final Part query;
        private final String scheme;
        private Part ssp;
        private volatile String uriString;

        private HierarchicalUri(String scheme, Part authority, PathPart path, Part query, Part fragment) {
            super();
            this.uriString = Uri.NOT_CACHED;
            this.scheme = scheme;
            this.authority = Part.nonNull(authority);
            this.path = path == null ? PathPart.NULL : path;
            this.query = Part.nonNull(query);
            this.fragment = Part.nonNull(fragment);
        }

        static Uri readFrom(Parcel parcel) {
            return new HierarchicalUri(parcel.readString(), Part.readFrom(parcel), PathPart.readFrom(parcel), Part.readFrom(parcel), Part.readFrom(parcel));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(3);
            parcel.writeString(this.scheme);
            this.authority.writeTo(parcel);
            this.path.writeTo(parcel);
            this.query.writeTo(parcel);
            this.fragment.writeTo(parcel);
        }

        @Override
        public boolean isHierarchical() {
            return true;
        }

        @Override
        public boolean isRelative() {
            return this.scheme == null;
        }

        @Override
        public String getScheme() {
            return this.scheme;
        }

        private Part getSsp() {
            if (this.ssp != null) {
                return this.ssp;
            }
            Part partFromEncoded = Part.fromEncoded(makeSchemeSpecificPart());
            this.ssp = partFromEncoded;
            return partFromEncoded;
        }

        @Override
        public String getEncodedSchemeSpecificPart() {
            return getSsp().getEncoded();
        }

        @Override
        public String getSchemeSpecificPart() {
            return getSsp().getDecoded();
        }

        private String makeSchemeSpecificPart() {
            StringBuilder builder = new StringBuilder();
            appendSspTo(builder);
            return builder.toString();
        }

        private void appendSspTo(StringBuilder builder) {
            String encodedAuthority = this.authority.getEncoded();
            if (encodedAuthority != null) {
                builder.append("//").append(encodedAuthority);
            }
            String encodedPath = this.path.getEncoded();
            if (encodedPath != null) {
                builder.append(encodedPath);
            }
            if (!this.query.isEmpty()) {
                builder.append('?').append(this.query.getEncoded());
            }
        }

        @Override
        public String getAuthority() {
            return this.authority.getDecoded();
        }

        @Override
        public String getEncodedAuthority() {
            return this.authority.getEncoded();
        }

        @Override
        public String getEncodedPath() {
            return this.path.getEncoded();
        }

        @Override
        public String getPath() {
            return this.path.getDecoded();
        }

        @Override
        public String getQuery() {
            return this.query.getDecoded();
        }

        @Override
        public String getEncodedQuery() {
            return this.query.getEncoded();
        }

        @Override
        public String getFragment() {
            return this.fragment.getDecoded();
        }

        @Override
        public String getEncodedFragment() {
            return this.fragment.getEncoded();
        }

        @Override
        public List<String> getPathSegments() {
            return this.path.getPathSegments();
        }

        @Override
        public String toString() {
            boolean cached = this.uriString != Uri.NOT_CACHED;
            if (cached) {
                return this.uriString;
            }
            String strMakeUriString = makeUriString();
            this.uriString = strMakeUriString;
            return strMakeUriString;
        }

        private String makeUriString() {
            StringBuilder builder = new StringBuilder();
            if (this.scheme != null) {
                builder.append(this.scheme).append(':');
            }
            appendSspTo(builder);
            if (!this.fragment.isEmpty()) {
                builder.append('#').append(this.fragment.getEncoded());
            }
            return builder.toString();
        }

        @Override
        public Builder buildUpon() {
            return new Builder().scheme(this.scheme).authority(this.authority).path(this.path).query(this.query).fragment(this.fragment);
        }
    }

    public static final class Builder {
        private Part authority;
        private Part fragment;
        private Part opaquePart;
        private PathPart path;
        private Part query;
        private String scheme;

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        Builder opaquePart(Part opaquePart) {
            this.opaquePart = opaquePart;
            return this;
        }

        public Builder opaquePart(String opaquePart) {
            return opaquePart(Part.fromDecoded(opaquePart));
        }

        public Builder encodedOpaquePart(String opaquePart) {
            return opaquePart(Part.fromEncoded(opaquePart));
        }

        Builder authority(Part authority) {
            this.opaquePart = null;
            this.authority = authority;
            return this;
        }

        public Builder authority(String authority) {
            return authority(Part.fromDecoded(authority));
        }

        public Builder encodedAuthority(String authority) {
            return authority(Part.fromEncoded(authority));
        }

        Builder path(PathPart path) {
            this.opaquePart = null;
            this.path = path;
            return this;
        }

        public Builder path(String path) {
            return path(PathPart.fromDecoded(path));
        }

        public Builder encodedPath(String path) {
            return path(PathPart.fromEncoded(path));
        }

        public Builder appendPath(String newSegment) {
            return path(PathPart.appendDecodedSegment(this.path, newSegment));
        }

        public Builder appendEncodedPath(String newSegment) {
            return path(PathPart.appendEncodedSegment(this.path, newSegment));
        }

        Builder query(Part query) {
            this.opaquePart = null;
            this.query = query;
            return this;
        }

        public Builder query(String query) {
            return query(Part.fromDecoded(query));
        }

        public Builder encodedQuery(String query) {
            return query(Part.fromEncoded(query));
        }

        Builder fragment(Part fragment) {
            this.fragment = fragment;
            return this;
        }

        public Builder fragment(String fragment) {
            return fragment(Part.fromDecoded(fragment));
        }

        public Builder encodedFragment(String fragment) {
            return fragment(Part.fromEncoded(fragment));
        }

        public Builder appendQueryParameter(String key, String value) {
            String oldQuery;
            this.opaquePart = null;
            String encodedParameter = Uri.encode(key, null) + "=" + Uri.encode(value, null);
            if (this.query == null || (oldQuery = this.query.getEncoded()) == null || oldQuery.length() == 0) {
                this.query = Part.fromEncoded(encodedParameter);
            } else {
                this.query = Part.fromEncoded(oldQuery + "&" + encodedParameter);
            }
            return this;
        }

        public Builder clearQuery() {
            return query((Part) null);
        }

        public Uri build() {
            if (this.opaquePart != null) {
                if (this.scheme == null) {
                    throw new UnsupportedOperationException("An opaque URI must have a scheme.");
                }
                return new OpaqueUri(this.scheme, this.opaquePart, this.fragment);
            }
            PathPart path = this.path;
            if (path == null || path == PathPart.NULL) {
                path = PathPart.EMPTY;
            } else if (hasSchemeOrAuthority()) {
                path = PathPart.makeAbsolute(path);
            }
            return new HierarchicalUri(this.scheme, this.authority, path, this.query, this.fragment);
        }

        private boolean hasSchemeOrAuthority() {
            return (this.scheme == null && (this.authority == null || this.authority == Part.NULL)) ? false : true;
        }

        public String toString() {
            return build().toString();
        }
    }

    public Set<String> getQueryParameterNames() {
        if (isOpaque()) {
            throw new UnsupportedOperationException(NOT_HIERARCHICAL);
        }
        String query = getEncodedQuery();
        if (query == null) {
            return Collections.emptySet();
        }
        Set<String> names = new LinkedHashSet<>();
        int start = 0;
        do {
            int next = query.indexOf(38, start);
            int end = next == -1 ? query.length() : next;
            int separator = query.indexOf(61, start);
            if (separator > end || separator == -1) {
                separator = end;
            }
            String name = query.substring(start, separator);
            names.add(decode(name));
            start = end + 1;
        } while (start < query.length());
        return Collections.unmodifiableSet(names);
    }

    public List<String> getQueryParameters(String key) {
        if (isOpaque()) {
            throw new UnsupportedOperationException(NOT_HIERARCHICAL);
        }
        if (key == null) {
            throw new NullPointerException("key");
        }
        String query = getEncodedQuery();
        if (query == null) {
            return Collections.emptyList();
        }
        try {
            String encodedKey = URLEncoder.encode(key, DEFAULT_ENCODING);
            ArrayList<String> values = new ArrayList<>();
            int start = 0;
            while (true) {
                int nextAmpersand = query.indexOf(38, start);
                int end = nextAmpersand != -1 ? nextAmpersand : query.length();
                int separator = query.indexOf(61, start);
                if (separator > end || separator == -1) {
                    separator = end;
                }
                if (separator - start == encodedKey.length() && query.regionMatches(start, encodedKey, 0, encodedKey.length())) {
                    if (separator == end) {
                        values.add(ProxyInfo.LOCAL_EXCL_LIST);
                    } else {
                        values.add(decode(query.substring(separator + 1, end)));
                    }
                }
                if (nextAmpersand != -1) {
                    start = nextAmpersand + 1;
                } else {
                    return Collections.unmodifiableList(values);
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public String getQueryParameter(String key) {
        if (isOpaque()) {
            throw new UnsupportedOperationException(NOT_HIERARCHICAL);
        }
        if (key == null) {
            throw new NullPointerException("key");
        }
        String query = getEncodedQuery();
        if (query == null) {
            return null;
        }
        String encodedKey = encode(key, null);
        int length = query.length();
        int start = 0;
        while (true) {
            int nextAmpersand = query.indexOf(38, start);
            int end = nextAmpersand != -1 ? nextAmpersand : length;
            int separator = query.indexOf(61, start);
            if (separator > end || separator == -1) {
                separator = end;
            }
            if (separator - start == encodedKey.length() && query.regionMatches(start, encodedKey, 0, encodedKey.length())) {
                if (separator == end) {
                    return ProxyInfo.LOCAL_EXCL_LIST;
                }
                String encodedValue = query.substring(separator + 1, end);
                return UriCodec.decode(encodedValue, true, StandardCharsets.UTF_8, false);
            }
            if (nextAmpersand == -1) {
                return null;
            }
            start = nextAmpersand + 1;
        }
    }

    public boolean getBooleanQueryParameter(String key, boolean defaultValue) {
        String flag = getQueryParameter(key);
        if (flag != null) {
            String flag2 = flag.toLowerCase(Locale.ROOT);
            boolean defaultValue2 = ("false".equals(flag2) || WifiEnterpriseConfig.ENGINE_DISABLE.equals(flag2)) ? false : true;
            return defaultValue2;
        }
        return defaultValue;
    }

    public Uri normalizeScheme() {
        String scheme = getScheme();
        if (scheme != null) {
            String lowerScheme = scheme.toLowerCase(Locale.ROOT);
            return !scheme.equals(lowerScheme) ? buildUpon().scheme(lowerScheme).build() : this;
        }
        return this;
    }

    public static void writeToParcel(Parcel out, Uri uri) {
        if (uri == null) {
            out.writeInt(0);
        } else {
            uri.writeToParcel(out, 0);
        }
    }

    public static String encode(String s) {
        return encode(s, null);
    }

    public static String encode(String s, String allow) {
        if (s == null) {
            return null;
        }
        StringBuilder encoded = null;
        int oldLength = s.length();
        int current = 0;
        while (current < oldLength) {
            int nextToEncode = current;
            while (nextToEncode < oldLength && isAllowed(s.charAt(nextToEncode), allow)) {
                nextToEncode++;
            }
            if (nextToEncode == oldLength) {
                if (current != 0) {
                    encoded.append((CharSequence) s, current, oldLength);
                    return encoded.toString();
                }
                return s;
            }
            if (encoded == null) {
                encoded = new StringBuilder();
            }
            if (nextToEncode > current) {
                encoded.append((CharSequence) s, current, nextToEncode);
            }
            int current2 = nextToEncode;
            int nextAllowed = current2 + 1;
            while (nextAllowed < oldLength && !isAllowed(s.charAt(nextAllowed), allow)) {
                nextAllowed++;
            }
            String toEncode = s.substring(current2, nextAllowed);
            try {
                byte[] bytes = toEncode.getBytes(DEFAULT_ENCODING);
                int bytesLength = bytes.length;
                for (int i = 0; i < bytesLength; i++) {
                    encoded.append('%');
                    encoded.append(HEX_DIGITS[(bytes[i] & 240) >> 4]);
                    encoded.append(HEX_DIGITS[bytes[i] & 15]);
                }
                current = nextAllowed;
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
        }
        return encoded != null ? encoded.toString() : s;
    }

    private static boolean isAllowed(char c, String allow) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || !((c < '0' || c > '9') && "_-!.~'()*".indexOf(c) == -1 && (allow == null || allow.indexOf(c) == -1));
    }

    public static String decode(String s) {
        if (s == null) {
            return null;
        }
        return UriCodec.decode(s, false, StandardCharsets.UTF_8, false);
    }

    static abstract class AbstractPart {
        volatile String decoded;
        volatile String encoded;

        abstract String getEncoded();

        static class Representation {
            static final int BOTH = 0;
            static final int DECODED = 2;
            static final int ENCODED = 1;

            Representation() {
            }
        }

        AbstractPart(String encoded, String decoded) {
            this.encoded = encoded;
            this.decoded = decoded;
        }

        final String getDecoded() {
            boolean hasDecoded = this.decoded != Uri.NOT_CACHED;
            if (hasDecoded) {
                return this.decoded;
            }
            String strDecode = Uri.decode(this.encoded);
            this.decoded = strDecode;
            return strDecode;
        }

        final void writeTo(Parcel parcel) {
            boolean hasEncoded = this.encoded != Uri.NOT_CACHED;
            boolean hasDecoded = this.decoded != Uri.NOT_CACHED;
            if (hasEncoded && hasDecoded) {
                parcel.writeInt(0);
                parcel.writeString(this.encoded);
                parcel.writeString(this.decoded);
            } else if (hasEncoded) {
                parcel.writeInt(1);
                parcel.writeString(this.encoded);
            } else {
                if (hasDecoded) {
                    parcel.writeInt(2);
                    parcel.writeString(this.decoded);
                    return;
                }
                throw new IllegalArgumentException("Neither encoded nor decoded");
            }
        }
    }

    static class Part extends AbstractPart {
        static final Part NULL = new EmptyPart(null);
        static final Part EMPTY = new EmptyPart(ProxyInfo.LOCAL_EXCL_LIST);

        private Part(String encoded, String decoded) {
            super(encoded, decoded);
        }

        boolean isEmpty() {
            return false;
        }

        @Override
        String getEncoded() {
            boolean hasEncoded = this.encoded != Uri.NOT_CACHED;
            if (hasEncoded) {
                return this.encoded;
            }
            String strEncode = Uri.encode(this.decoded);
            this.encoded = strEncode;
            return strEncode;
        }

        static Part readFrom(Parcel parcel) {
            int representation = parcel.readInt();
            switch (representation) {
                case 0:
                    return from(parcel.readString(), parcel.readString());
                case 1:
                    return fromEncoded(parcel.readString());
                case 2:
                    return fromDecoded(parcel.readString());
                default:
                    throw new IllegalArgumentException("Unknown representation: " + representation);
            }
        }

        static Part nonNull(Part part) {
            return part == null ? NULL : part;
        }

        static Part fromEncoded(String encoded) {
            return from(encoded, Uri.NOT_CACHED);
        }

        static Part fromDecoded(String decoded) {
            return from(Uri.NOT_CACHED, decoded);
        }

        static Part from(String encoded, String decoded) {
            if (encoded == null) {
                return NULL;
            }
            if (encoded.length() == 0) {
                return EMPTY;
            }
            if (decoded == null) {
                return NULL;
            }
            if (decoded.length() == 0) {
                return EMPTY;
            }
            return new Part(encoded, decoded);
        }

        private static class EmptyPart extends Part {
            public EmptyPart(String value) {
                super(value, value);
            }

            @Override
            boolean isEmpty() {
                return true;
            }
        }
    }

    static class PathPart extends AbstractPart {
        private PathSegments pathSegments;
        static final PathPart NULL = new PathPart(null, null);
        static final PathPart EMPTY = new PathPart(ProxyInfo.LOCAL_EXCL_LIST, ProxyInfo.LOCAL_EXCL_LIST);

        private PathPart(String encoded, String decoded) {
            super(encoded, decoded);
        }

        @Override
        String getEncoded() {
            boolean hasEncoded = this.encoded != Uri.NOT_CACHED;
            if (hasEncoded) {
                return this.encoded;
            }
            String strEncode = Uri.encode(this.decoded, "/");
            this.encoded = strEncode;
            return strEncode;
        }

        PathSegments getPathSegments() {
            if (this.pathSegments != null) {
                return this.pathSegments;
            }
            String path = getEncoded();
            if (path == null) {
                PathSegments pathSegments = PathSegments.EMPTY;
                this.pathSegments = pathSegments;
                return pathSegments;
            }
            PathSegmentsBuilder segmentBuilder = new PathSegmentsBuilder();
            int previous = 0;
            while (true) {
                int current = path.indexOf(47, previous);
                if (current <= -1) {
                    break;
                }
                if (previous < current) {
                    String decodedSegment = Uri.decode(path.substring(previous, current));
                    segmentBuilder.add(decodedSegment);
                }
                previous = current + 1;
            }
            if (previous < path.length()) {
                segmentBuilder.add(Uri.decode(path.substring(previous)));
            }
            PathSegments pathSegmentsBuild = segmentBuilder.build();
            this.pathSegments = pathSegmentsBuild;
            return pathSegmentsBuild;
        }

        static PathPart appendEncodedSegment(PathPart oldPart, String newSegment) {
            String newPath;
            if (oldPart == null) {
                return fromEncoded("/" + newSegment);
            }
            String oldPath = oldPart.getEncoded();
            if (oldPath == null) {
                oldPath = ProxyInfo.LOCAL_EXCL_LIST;
            }
            int oldPathLength = oldPath.length();
            if (oldPathLength == 0) {
                newPath = "/" + newSegment;
            } else if (oldPath.charAt(oldPathLength - 1) == '/') {
                newPath = oldPath + newSegment;
            } else {
                newPath = oldPath + "/" + newSegment;
            }
            return fromEncoded(newPath);
        }

        static PathPart appendDecodedSegment(PathPart oldPart, String decoded) {
            String encoded = Uri.encode(decoded);
            return appendEncodedSegment(oldPart, encoded);
        }

        static PathPart readFrom(Parcel parcel) {
            int representation = parcel.readInt();
            switch (representation) {
                case 0:
                    return from(parcel.readString(), parcel.readString());
                case 1:
                    return fromEncoded(parcel.readString());
                case 2:
                    return fromDecoded(parcel.readString());
                default:
                    throw new IllegalArgumentException("Bad representation: " + representation);
            }
        }

        static PathPart fromEncoded(String encoded) {
            return from(encoded, Uri.NOT_CACHED);
        }

        static PathPart fromDecoded(String decoded) {
            return from(Uri.NOT_CACHED, decoded);
        }

        static PathPart from(String encoded, String decoded) {
            if (encoded == null) {
                return NULL;
            }
            if (encoded.length() == 0) {
                return EMPTY;
            }
            return new PathPart(encoded, decoded);
        }

        static PathPart makeAbsolute(PathPart oldPart) {
            boolean encodedCached = oldPart.encoded != Uri.NOT_CACHED;
            String oldPath = encodedCached ? oldPart.encoded : oldPart.decoded;
            if (oldPath != null && oldPath.length() != 0 && !oldPath.startsWith("/")) {
                String newEncoded = encodedCached ? "/" + oldPart.encoded : Uri.NOT_CACHED;
                boolean decodedCached = oldPart.decoded != Uri.NOT_CACHED;
                String newDecoded = decodedCached ? "/" + oldPart.decoded : Uri.NOT_CACHED;
                return new PathPart(newEncoded, newDecoded);
            }
            return oldPart;
        }
    }

    public static Uri withAppendedPath(Uri baseUri, String pathSegment) {
        Builder builder = baseUri.buildUpon();
        return builder.appendEncodedPath(pathSegment).build();
    }

    public Uri getCanonicalUri() {
        if (ContentResolver.SCHEME_FILE.equals(getScheme())) {
            try {
                String canonicalPath = new File(getPath()).getCanonicalPath();
                if (Environment.isExternalStorageEmulated()) {
                    String legacyPath = Environment.getLegacyExternalStorageDirectory().toString();
                    if (canonicalPath.startsWith(legacyPath)) {
                        return fromFile(new File(Environment.getExternalStorageDirectory().toString(), canonicalPath.substring(legacyPath.length() + 1)));
                    }
                }
                return fromFile(new File(canonicalPath));
            } catch (IOException e) {
                return this;
            }
        }
        return this;
    }

    public void checkFileUriExposed(String location) {
        if (ContentResolver.SCHEME_FILE.equals(getScheme())) {
            StrictMode.onFileUriExposed(location);
        }
    }

    public boolean isPathPrefixMatch(Uri prefix) {
        if (!Objects.equals(getScheme(), prefix.getScheme()) || !Objects.equals(getAuthority(), prefix.getAuthority())) {
            return false;
        }
        List<String> seg = getPathSegments();
        List<String> prefixSeg = prefix.getPathSegments();
        int prefixSize = prefixSeg.size();
        if (seg.size() < prefixSize) {
            return false;
        }
        for (int i = 0; i < prefixSize; i++) {
            if (!Objects.equals(seg.get(i), prefixSeg.get(i))) {
                return false;
            }
        }
        return true;
    }
}
