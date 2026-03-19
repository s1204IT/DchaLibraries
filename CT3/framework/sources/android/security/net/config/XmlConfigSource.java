package android.security.net.config;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.security.net.config.NetworkSecurityConfig;
import android.util.ArraySet;
import android.util.Base64;
import android.util.Pair;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XmlConfigSource implements ConfigSource {
    private static final int CONFIG_BASE = 0;
    private static final int CONFIG_DEBUG = 2;
    private static final int CONFIG_DOMAIN = 1;
    private Context mContext;
    private final boolean mDebugBuild;
    private NetworkSecurityConfig mDefaultConfig;
    private Set<Pair<Domain, NetworkSecurityConfig>> mDomainMap;
    private boolean mInitialized;
    private final Object mLock;
    private final int mResourceId;
    private final int mTargetSdkVersion;

    public XmlConfigSource(Context context, int resourceId) {
        this(context, resourceId, false);
    }

    public XmlConfigSource(Context context, int resourceId, boolean debugBuild) {
        this(context, resourceId, debugBuild, 10000);
    }

    public XmlConfigSource(Context context, int resourceId, boolean debugBuild, int targetSdkVersion) {
        this.mLock = new Object();
        this.mResourceId = resourceId;
        this.mContext = context;
        this.mDebugBuild = debugBuild;
        this.mTargetSdkVersion = targetSdkVersion;
    }

    @Override
    public Set<Pair<Domain, NetworkSecurityConfig>> getPerDomainConfigs() {
        ensureInitialized();
        return this.mDomainMap;
    }

    @Override
    public NetworkSecurityConfig getDefaultConfig() {
        ensureInitialized();
        return this.mDefaultConfig;
    }

    private static final String getConfigString(int configType) {
        switch (configType) {
            case 0:
                return "base-config";
            case 1:
                return "domain-config";
            case 2:
                return "debug-overrides";
            default:
                throw new IllegalArgumentException("Unknown config type: " + configType);
        }
    }

    private void ensureInitialized() {
        Throwable th;
        Throwable th2 = null;
        synchronized (this.mLock) {
            if (this.mInitialized) {
                return;
            }
            XmlResourceParser parser = null;
            try {
                try {
                    parser = this.mContext.getResources().getXml(this.mResourceId);
                    parseNetworkSecurityConfig(parser);
                    this.mContext = null;
                    this.mInitialized = true;
                    if (parser != null) {
                        try {
                            parser.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 != null) {
                        throw th2;
                    }
                } catch (Resources.NotFoundException | ParserException | IOException | XmlPullParserException e) {
                    throw new RuntimeException("Failed to parse XML configuration from " + this.mContext.getResources().getResourceEntryName(this.mResourceId), e);
                }
            } catch (Throwable th4) {
                try {
                    throw th4;
                } catch (Throwable th5) {
                    th2 = th4;
                    th = th5;
                    if (parser != null) {
                        try {
                            parser.close();
                        } catch (Throwable th6) {
                            if (th2 == null) {
                                th2 = th6;
                            } else if (th2 != th6) {
                                th2.addSuppressed(th6);
                            }
                        }
                    }
                    if (th2 != null) {
                        throw th;
                    }
                    throw th2;
                }
            }
        }
    }

    private Pin parsePin(XmlResourceParser parser) throws XmlPullParserException, ParserException, IOException {
        String digestAlgorithm = parser.getAttributeValue(null, "digest");
        if (!Pin.isSupportedDigestAlgorithm(digestAlgorithm)) {
            throw new ParserException(parser, "Unsupported pin digest algorithm: " + digestAlgorithm);
        }
        if (parser.next() != 4) {
            throw new ParserException(parser, "Missing pin digest");
        }
        String digest = parser.getText().trim();
        try {
            byte[] decodedDigest = Base64.decode(digest, 0);
            int expectedLength = Pin.getDigestLength(digestAlgorithm);
            if (decodedDigest.length != expectedLength) {
                throw new ParserException(parser, "digest length " + decodedDigest.length + " does not match expected length for " + digestAlgorithm + " of " + expectedLength);
            }
            if (parser.next() != 3) {
                throw new ParserException(parser, "pin contains additional elements");
            }
            return new Pin(digestAlgorithm, decodedDigest);
        } catch (IllegalArgumentException e) {
            throw new ParserException(parser, "Invalid pin digest", e);
        }
    }

    private PinSet parsePinSet(XmlResourceParser parser) throws XmlPullParserException, ParserException, IOException {
        String expirationDate = parser.getAttributeValue(null, "expiration");
        long expirationTimestampMilis = Long.MAX_VALUE;
        if (expirationDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                Date date = sdf.parse(expirationDate);
                if (date == null) {
                    throw new ParserException(parser, "Invalid expiration date in pin-set");
                }
                expirationTimestampMilis = date.getTime();
            } catch (ParseException e) {
                throw new ParserException(parser, "Invalid expiration date in pin-set", e);
            }
        }
        int outerDepth = parser.getDepth();
        Set<Pin> pins = new ArraySet<>();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            String tagName = parser.getName();
            if (tagName.equals("pin")) {
                pins.add(parsePin(parser));
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
        return new PinSet(pins, expirationTimestampMilis);
    }

    private Domain parseDomain(XmlResourceParser parser, Set<String> seenDomains) throws XmlPullParserException, ParserException, IOException {
        boolean includeSubdomains = parser.getAttributeBooleanValue(null, "includeSubdomains", false);
        if (parser.next() != 4) {
            throw new ParserException(parser, "Domain name missing");
        }
        String domain = parser.getText().trim().toLowerCase(Locale.US);
        if (parser.next() != 3) {
            throw new ParserException(parser, "domain contains additional elements");
        }
        if (!seenDomains.add(domain)) {
            throw new ParserException(parser, domain + " has already been specified");
        }
        return new Domain(domain, includeSubdomains);
    }

    private CertificatesEntryRef parseCertificatesEntry(XmlResourceParser parser, boolean defaultOverridePins) throws XmlPullParserException, ParserException, IOException {
        CertificateSource source;
        boolean overridePins = parser.getAttributeBooleanValue(null, "overridePins", defaultOverridePins);
        int sourceId = parser.getAttributeResourceValue(null, "src", -1);
        String sourceString = parser.getAttributeValue(null, "src");
        if (sourceString == null) {
            throw new ParserException(parser, "certificates element missing src attribute");
        }
        if (sourceId != -1) {
            source = new ResourceCertificateSource(sourceId, this.mContext);
        } else if ("system".equals(sourceString)) {
            source = SystemCertificateSource.getInstance();
        } else if (Context.USER_SERVICE.equals(sourceString)) {
            source = UserCertificateSource.getInstance();
        } else {
            throw new ParserException(parser, "Unknown certificates src. Should be one of system|user|@resourceVal");
        }
        XmlUtils.skipCurrentTag(parser);
        return new CertificatesEntryRef(source, overridePins);
    }

    private Collection<CertificatesEntryRef> parseTrustAnchors(XmlResourceParser parser, boolean defaultOverridePins) throws XmlPullParserException, ParserException, IOException {
        int outerDepth = parser.getDepth();
        List<CertificatesEntryRef> anchors = new ArrayList<>();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            String tagName = parser.getName();
            if (tagName.equals("certificates")) {
                anchors.add(parseCertificatesEntry(parser, defaultOverridePins));
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
        return anchors;
    }

    private List<Pair<NetworkSecurityConfig.Builder, Set<Domain>>> parseConfigEntry(XmlResourceParser parser, Set<String> seenDomains, NetworkSecurityConfig.Builder parentBuilder, int configType) throws XmlPullParserException, ParserException, IOException {
        List<Pair<NetworkSecurityConfig.Builder, Set<Domain>>> builders = new ArrayList<>();
        NetworkSecurityConfig.Builder builder = new NetworkSecurityConfig.Builder();
        builder.setParent(parentBuilder);
        Set<Domain> domains = new ArraySet<>();
        boolean seenPinSet = false;
        boolean seenTrustAnchors = false;
        boolean defaultOverridePins = configType == 2;
        parser.getName();
        int outerDepth = parser.getDepth();
        builders.add(new Pair<>(builder, domains));
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String name = parser.getAttributeName(i);
            if ("hstsEnforced".equals(name)) {
                builder.setHstsEnforced(parser.getAttributeBooleanValue(i, false));
            } else if ("cleartextTrafficPermitted".equals(name)) {
                builder.setCleartextTrafficPermitted(parser.getAttributeBooleanValue(i, true));
            }
        }
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            String tagName = parser.getName();
            if ("domain".equals(tagName)) {
                if (configType != 1) {
                    throw new ParserException(parser, "domain element not allowed in " + getConfigString(configType));
                }
                Domain domain = parseDomain(parser, seenDomains);
                domains.add(domain);
            } else if ("trust-anchors".equals(tagName)) {
                if (seenTrustAnchors) {
                    throw new ParserException(parser, "Multiple trust-anchor elements not allowed");
                }
                builder.addCertificatesEntryRefs(parseTrustAnchors(parser, defaultOverridePins));
                seenTrustAnchors = true;
            } else if ("pin-set".equals(tagName)) {
                if (configType != 1) {
                    throw new ParserException(parser, "pin-set element not allowed in " + getConfigString(configType));
                }
                if (seenPinSet) {
                    throw new ParserException(parser, "Multiple pin-set elements not allowed");
                }
                builder.setPinSet(parsePinSet(parser));
                seenPinSet = true;
            } else if ("domain-config".equals(tagName)) {
                if (configType != 1) {
                    throw new ParserException(parser, "Nested domain-config not allowed in " + getConfigString(configType));
                }
                builders.addAll(parseConfigEntry(parser, seenDomains, builder, configType));
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
        if (configType == 1 && domains.isEmpty()) {
            throw new ParserException(parser, "No domain elements in domain-config");
        }
        return builders;
    }

    private void addDebugAnchorsIfNeeded(NetworkSecurityConfig.Builder debugConfigBuilder, NetworkSecurityConfig.Builder builder) {
        if (debugConfigBuilder == null || !debugConfigBuilder.hasCertificatesEntryRefs() || !builder.hasCertificatesEntryRefs()) {
            return;
        }
        builder.addCertificatesEntryRefs(debugConfigBuilder.getCertificatesEntryRefs());
    }

    private void parseNetworkSecurityConfig(XmlResourceParser parser) throws Throwable {
        Set<String> seenDomains = new ArraySet<>();
        List<Pair<NetworkSecurityConfig.Builder, Set<Domain>>> builders = new ArrayList<>();
        NetworkSecurityConfig.Builder baseConfigBuilder = null;
        NetworkSecurityConfig.Builder debugConfigBuilder = null;
        boolean seenDebugOverrides = false;
        boolean seenBaseConfig = false;
        XmlUtils.beginDocument(parser, "network-security-config");
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if ("base-config".equals(parser.getName())) {
                if (seenBaseConfig) {
                    throw new ParserException(parser, "Only one base-config allowed");
                }
                seenBaseConfig = true;
                baseConfigBuilder = (NetworkSecurityConfig.Builder) parseConfigEntry(parser, seenDomains, null, 0).get(0).first;
            } else if ("domain-config".equals(parser.getName())) {
                builders.addAll(parseConfigEntry(parser, seenDomains, baseConfigBuilder, 1));
            } else if ("debug-overrides".equals(parser.getName())) {
                if (seenDebugOverrides) {
                    throw new ParserException(parser, "Only one debug-overrides allowed");
                }
                if (this.mDebugBuild) {
                    debugConfigBuilder = (NetworkSecurityConfig.Builder) parseConfigEntry(parser, null, null, 2).get(0).first;
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
                seenDebugOverrides = true;
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
        if (this.mDebugBuild && debugConfigBuilder == null) {
            debugConfigBuilder = parseDebugOverridesResource();
        }
        NetworkSecurityConfig.Builder platformDefaultBuilder = NetworkSecurityConfig.getDefaultBuilder(this.mTargetSdkVersion);
        addDebugAnchorsIfNeeded(debugConfigBuilder, platformDefaultBuilder);
        if (baseConfigBuilder != null) {
            baseConfigBuilder.setParent(platformDefaultBuilder);
            addDebugAnchorsIfNeeded(debugConfigBuilder, baseConfigBuilder);
        } else {
            baseConfigBuilder = platformDefaultBuilder;
        }
        Set<Pair<Domain, NetworkSecurityConfig>> configs = new ArraySet<>();
        for (Pair<NetworkSecurityConfig.Builder, Set<Domain>> entry : builders) {
            NetworkSecurityConfig.Builder builder = (NetworkSecurityConfig.Builder) entry.first;
            Set<Domain> domains = (Set) entry.second;
            if (builder.getParent() == null) {
                builder.setParent(baseConfigBuilder);
            }
            addDebugAnchorsIfNeeded(debugConfigBuilder, builder);
            NetworkSecurityConfig config = builder.build();
            for (Domain domain : domains) {
                configs.add(new Pair<>(domain, config));
            }
        }
        this.mDefaultConfig = baseConfigBuilder.build();
        this.mDomainMap = configs;
    }

    private NetworkSecurityConfig.Builder parseDebugOverridesResource() throws Throwable {
        Throwable th = null;
        Resources resources = this.mContext.getResources();
        String packageName = resources.getResourcePackageName(this.mResourceId);
        String entryName = resources.getResourceEntryName(this.mResourceId);
        int resId = resources.getIdentifier(entryName + "_debug", "xml", packageName);
        if (resId == 0) {
            return null;
        }
        NetworkSecurityConfig.Builder debugConfigBuilder = null;
        XmlResourceParser parser = null;
        try {
            parser = resources.getXml(resId);
            XmlUtils.beginDocument(parser, "network-security-config");
            int outerDepth = parser.getDepth();
            boolean seenDebugOverrides = false;
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if ("debug-overrides".equals(parser.getName())) {
                    if (seenDebugOverrides) {
                        throw new ParserException(parser, "Only one debug-overrides allowed");
                    }
                    if (this.mDebugBuild) {
                        debugConfigBuilder = (NetworkSecurityConfig.Builder) parseConfigEntry(parser, null, null, 2).get(0).first;
                    } else {
                        XmlUtils.skipCurrentTag(parser);
                    }
                    seenDebugOverrides = true;
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
            if (parser != null) {
                try {
                    parser.close();
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            if (th != null) {
                throw th;
            }
            return debugConfigBuilder;
        } catch (Throwable th3) {
            th = th3;
            if (parser != null) {
            }
            if (th == null) {
            }
        }
    }

    public static class ParserException extends Exception {
        public ParserException(XmlPullParser parser, String message, Throwable cause) {
            super(message + " at: " + parser.getPositionDescription(), cause);
        }

        public ParserException(XmlPullParser parser, String message) {
            this(parser, message, null);
        }
    }
}
