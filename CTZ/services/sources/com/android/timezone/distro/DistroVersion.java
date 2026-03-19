package com.android.timezone.distro;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DistroVersion {
    public static final int CURRENT_FORMAT_MAJOR_VERSION = 2;
    public static final int CURRENT_FORMAT_MINOR_VERSION = 1;
    private static final int REVISION_LENGTH = 3;
    private static final int RULES_VERSION_LENGTH = 5;
    public final int formatMajorVersion;
    public final int formatMinorVersion;
    public final int revision;
    public final String rulesVersion;
    private static final String FULL_CURRENT_FORMAT_VERSION_STRING = toFormatVersionString(2, 1);
    private static final int FORMAT_VERSION_STRING_LENGTH = FULL_CURRENT_FORMAT_VERSION_STRING.length();
    private static final Pattern FORMAT_VERSION_PATTERN = Pattern.compile("(\\d{3})\\.(\\d{3})");
    private static final Pattern RULES_VERSION_PATTERN = Pattern.compile("(\\d{4}\\w)");
    private static final Pattern REVISION_PATTERN = Pattern.compile("(\\d{3})");
    public static final int DISTRO_VERSION_FILE_LENGTH = (((FORMAT_VERSION_STRING_LENGTH + 1) + 5) + 1) + 3;
    private static final Pattern DISTRO_VERSION_PATTERN = Pattern.compile(FORMAT_VERSION_PATTERN.pattern() + "\\|" + RULES_VERSION_PATTERN.pattern() + "\\|" + REVISION_PATTERN.pattern() + ".*");

    public DistroVersion(int i, int i2, String str, int i3) throws DistroException {
        this.formatMajorVersion = validate3DigitVersion(i);
        this.formatMinorVersion = validate3DigitVersion(i2);
        if (!RULES_VERSION_PATTERN.matcher(str).matches()) {
            throw new DistroException("Invalid rulesVersion: " + str);
        }
        this.rulesVersion = str;
        this.revision = validate3DigitVersion(i3);
    }

    public static DistroVersion fromBytes(byte[] bArr) throws DistroException {
        String str = new String(bArr, StandardCharsets.US_ASCII);
        try {
            Matcher matcher = DISTRO_VERSION_PATTERN.matcher(str);
            if (!matcher.matches()) {
                throw new DistroException("Invalid distro version string: \"" + str + "\"");
            }
            return new DistroVersion(from3DigitVersionString(matcher.group(1)), from3DigitVersionString(matcher.group(2)), matcher.group(3), from3DigitVersionString(matcher.group(4)));
        } catch (IndexOutOfBoundsException e) {
            throw new DistroException("Distro version string too short: \"" + str + "\"");
        }
    }

    public byte[] toBytes() {
        return toBytes(this.formatMajorVersion, this.formatMinorVersion, this.rulesVersion, this.revision);
    }

    public static byte[] toBytes(int i, int i2, String str, int i3) {
        return (toFormatVersionString(i, i2) + "|" + str + "|" + to3DigitVersionString(i3)).getBytes(StandardCharsets.US_ASCII);
    }

    public static boolean isCompatibleWithThisDevice(DistroVersion distroVersion) {
        return 2 == distroVersion.formatMajorVersion && 1 <= distroVersion.formatMinorVersion;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DistroVersion distroVersion = (DistroVersion) obj;
        if (this.formatMajorVersion != distroVersion.formatMajorVersion || this.formatMinorVersion != distroVersion.formatMinorVersion || this.revision != distroVersion.revision) {
            return false;
        }
        return this.rulesVersion.equals(distroVersion.rulesVersion);
    }

    public int hashCode() {
        return (31 * ((((this.formatMajorVersion * 31) + this.formatMinorVersion) * 31) + this.rulesVersion.hashCode())) + this.revision;
    }

    public String toString() {
        return "DistroVersion{formatMajorVersion=" + this.formatMajorVersion + ", formatMinorVersion=" + this.formatMinorVersion + ", rulesVersion='" + this.rulesVersion + "', revision=" + this.revision + '}';
    }

    private static String to3DigitVersionString(int i) {
        try {
            return String.format(Locale.ROOT, "%03d", Integer.valueOf(validate3DigitVersion(i)));
        } catch (DistroException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static int from3DigitVersionString(String str) throws DistroException {
        if (str.length() != 3) {
            throw new DistroException("versionString must be a zero padded, 3 digit, positive decimal integer");
        }
        try {
            return validate3DigitVersion(Integer.parseInt(str));
        } catch (NumberFormatException e) {
            throw new DistroException("versionString must be a zero padded, 3 digit, positive decimal integer", e);
        }
    }

    private static int validate3DigitVersion(int i) throws DistroException {
        if (i < 0 || i > 999) {
            throw new DistroException("Expected 0 <= value <= 999, was " + i);
        }
        return i;
    }

    private static String toFormatVersionString(int i, int i2) {
        return to3DigitVersionString(i) + "." + to3DigitVersionString(i2);
    }
}
