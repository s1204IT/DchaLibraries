package libcore.util;

import android.icu.util.TimeZone;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import libcore.util.CountryTimeZones;
import libcore.util.TimeZoneFinder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public final class TimeZoneFinder {
    private static final String COUNTRY_CODE_ATTRIBUTE = "code";
    private static final String COUNTRY_ELEMENT = "country";
    private static final String COUNTRY_ZONES_ELEMENT = "countryzones";
    private static final String DEFAULT_TIME_ZONE_ID_ATTRIBUTE = "default";
    private static final String EVER_USES_UTC_ATTRIBUTE = "everutc";
    private static final String FALSE_ATTRIBUTE_VALUE = "n";
    private static final String IANA_VERSION_ATTRIBUTE = "ianaversion";
    private static final String TIMEZONES_ELEMENT = "timezones";
    private static final String TRUE_ATTRIBUTE_VALUE = "y";
    private static final String TZLOOKUP_FILE_NAME = "tzlookup.xml";
    private static final String ZONE_ID_ELEMENT = "id";
    private static final String ZONE_NOT_USED_AFTER_ATTRIBUTE = "notafter";
    private static final String ZONE_SHOW_IN_PICKER_ATTRIBUTE = "picker";
    private static TimeZoneFinder instance;
    private CountryTimeZones lastCountryTimeZones;
    private final ReaderSupplier xmlSource;

    private TimeZoneFinder(ReaderSupplier readerSupplier) {
        this.xmlSource = readerSupplier;
    }

    public static TimeZoneFinder getInstance() {
        synchronized (TimeZoneFinder.class) {
            if (instance == null) {
                String[] timeZoneFilePaths = TimeZoneDataFiles.getTimeZoneFilePaths(TZLOOKUP_FILE_NAME);
                instance = createInstanceWithFallback(timeZoneFilePaths[0], timeZoneFilePaths[1]);
            }
        }
        return instance;
    }

    public static TimeZoneFinder createInstanceWithFallback(String... strArr) {
        int length = strArr.length;
        IOException iOException = null;
        int i = 0;
        while (i < length) {
            try {
                return createInstance(strArr[i]);
            } catch (IOException e) {
                if (iOException != null) {
                    e.addSuppressed(iOException);
                }
                i++;
                iOException = e;
            }
        }
        System.logE("No valid file found in set: " + Arrays.toString(strArr) + " Printing exceptions and falling back to empty map.", iOException);
        return createInstanceForTests("<timezones><countryzones /></timezones>");
    }

    public static TimeZoneFinder createInstance(String str) throws IOException {
        return new TimeZoneFinder(ReaderSupplier.forFile(str, StandardCharsets.UTF_8));
    }

    public static TimeZoneFinder createInstanceForTests(String str) {
        return new TimeZoneFinder(ReaderSupplier.forString(str));
    }

    public void validate() throws IOException {
        try {
            processXml(new TimeZonesValidator());
        } catch (XmlPullParserException e) {
            throw new IOException("Parsing error", e);
        }
    }

    public String getIanaVersion() {
        IanaVersionExtractor ianaVersionExtractor = new IanaVersionExtractor();
        try {
            processXml(ianaVersionExtractor);
            return ianaVersionExtractor.getIanaVersion();
        } catch (IOException | XmlPullParserException e) {
            return null;
        }
    }

    public CountryZonesFinder getCountryZonesFinder() {
        CountryZonesLookupExtractor countryZonesLookupExtractor = new CountryZonesLookupExtractor();
        try {
            processXml(countryZonesLookupExtractor);
            return countryZonesLookupExtractor.getCountryZonesLookup();
        } catch (IOException | XmlPullParserException e) {
            System.logW("Error reading country zones ", e);
            return null;
        }
    }

    public TimeZone lookupTimeZoneByCountryAndOffset(String str, int i, boolean z, long j, TimeZone timeZone) {
        CountryTimeZones.OffsetResult offsetResultLookupByOffsetWithBias;
        CountryTimeZones countryTimeZonesLookupCountryTimeZones = lookupCountryTimeZones(str);
        if (countryTimeZonesLookupCountryTimeZones == null || (offsetResultLookupByOffsetWithBias = countryTimeZonesLookupCountryTimeZones.lookupByOffsetWithBias(i, z, j, timeZone)) == null) {
            return null;
        }
        return offsetResultLookupByOffsetWithBias.mTimeZone;
    }

    public String lookupDefaultTimeZoneIdByCountry(String str) {
        CountryTimeZones countryTimeZonesLookupCountryTimeZones = lookupCountryTimeZones(str);
        if (countryTimeZonesLookupCountryTimeZones == null) {
            return null;
        }
        return countryTimeZonesLookupCountryTimeZones.getDefaultTimeZoneId();
    }

    public List<TimeZone> lookupTimeZonesByCountry(String str) {
        CountryTimeZones countryTimeZonesLookupCountryTimeZones = lookupCountryTimeZones(str);
        if (countryTimeZonesLookupCountryTimeZones == null) {
            return null;
        }
        return countryTimeZonesLookupCountryTimeZones.getIcuTimeZones();
    }

    public List<String> lookupTimeZoneIdsByCountry(String str) {
        CountryTimeZones countryTimeZonesLookupCountryTimeZones = lookupCountryTimeZones(str);
        if (countryTimeZonesLookupCountryTimeZones == null) {
            return null;
        }
        return extractTimeZoneIds(countryTimeZonesLookupCountryTimeZones.getTimeZoneMappings());
    }

    public CountryTimeZones lookupCountryTimeZones(String str) {
        synchronized (this) {
            if (this.lastCountryTimeZones != null && this.lastCountryTimeZones.isForCountryCode(str)) {
                return this.lastCountryTimeZones;
            }
            SelectiveCountryTimeZonesExtractor selectiveCountryTimeZonesExtractor = new SelectiveCountryTimeZonesExtractor(str);
            try {
                processXml(selectiveCountryTimeZonesExtractor);
                CountryTimeZones validatedCountryTimeZones = selectiveCountryTimeZonesExtractor.getValidatedCountryTimeZones();
                if (validatedCountryTimeZones == null) {
                    return null;
                }
                synchronized (this) {
                    this.lastCountryTimeZones = validatedCountryTimeZones;
                }
                return validatedCountryTimeZones;
            } catch (IOException | XmlPullParserException e) {
                System.logW("Error reading country zones ", e);
                return null;
            }
        }
    }

    private void processXml(TimeZonesProcessor timeZonesProcessor) throws XmlPullParserException, IOException {
        Reader reader = this.xmlSource.get();
        Throwable th = null;
        try {
            XmlPullParserFactory xmlPullParserFactoryNewInstance = XmlPullParserFactory.newInstance();
            xmlPullParserFactoryNewInstance.setNamespaceAware(false);
            XmlPullParser xmlPullParserNewPullParser = xmlPullParserFactoryNewInstance.newPullParser();
            xmlPullParserNewPullParser.setInput(reader);
            findRequiredStartTag(xmlPullParserNewPullParser, TIMEZONES_ELEMENT);
            if (!timeZonesProcessor.processHeader(xmlPullParserNewPullParser.getAttributeValue(null, IANA_VERSION_ATTRIBUTE))) {
                if (reader != null) {
                    reader.close();
                    return;
                }
                return;
            }
            findRequiredStartTag(xmlPullParserNewPullParser, COUNTRY_ZONES_ELEMENT);
            if (!processCountryZones(xmlPullParserNewPullParser, timeZonesProcessor)) {
                if (reader != null) {
                    reader.close();
                    return;
                }
                return;
            }
            checkOnEndTag(xmlPullParserNewPullParser, COUNTRY_ZONES_ELEMENT);
            xmlPullParserNewPullParser.next();
            consumeUntilEndTag(xmlPullParserNewPullParser, TIMEZONES_ELEMENT);
            checkOnEndTag(xmlPullParserNewPullParser, TIMEZONES_ELEMENT);
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th2) {
            if (reader != null) {
                if (0 != 0) {
                    try {
                        reader.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    reader.close();
                }
            }
            throw th2;
        }
    }

    private static boolean processCountryZones(XmlPullParser xmlPullParser, TimeZonesProcessor timeZonesProcessor) throws XmlPullParserException, IOException {
        while (findOptionalStartTag(xmlPullParser, COUNTRY_ELEMENT)) {
            if (timeZonesProcessor == null) {
                consumeUntilEndTag(xmlPullParser, COUNTRY_ELEMENT);
            } else {
                String attributeValue = xmlPullParser.getAttributeValue(null, COUNTRY_CODE_ATTRIBUTE);
                if (attributeValue == null || attributeValue.isEmpty()) {
                    throw new XmlPullParserException("Unable to find country code: " + xmlPullParser.getPositionDescription());
                }
                String attributeValue2 = xmlPullParser.getAttributeValue(null, DEFAULT_TIME_ZONE_ID_ATTRIBUTE);
                if (attributeValue2 == null || attributeValue2.isEmpty()) {
                    throw new XmlPullParserException("Unable to find default time zone ID: " + xmlPullParser.getPositionDescription());
                }
                Boolean booleanAttribute = parseBooleanAttribute(xmlPullParser, EVER_USES_UTC_ATTRIBUTE, null);
                if (booleanAttribute == null) {
                    throw new XmlPullParserException("Unable to find UTC hint attribute (everutc): " + xmlPullParser.getPositionDescription());
                }
                if (!timeZonesProcessor.processCountryZones(attributeValue, attributeValue2, booleanAttribute.booleanValue(), parseTimeZoneMappings(xmlPullParser), xmlPullParser.getPositionDescription())) {
                    return false;
                }
            }
            checkOnEndTag(xmlPullParser, COUNTRY_ELEMENT);
        }
        return true;
    }

    private static List<CountryTimeZones.TimeZoneMapping> parseTimeZoneMappings(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        ArrayList arrayList = new ArrayList();
        while (findOptionalStartTag(xmlPullParser, ZONE_ID_ELEMENT)) {
            boolean zBooleanValue = parseBooleanAttribute(xmlPullParser, ZONE_SHOW_IN_PICKER_ATTRIBUTE, true).booleanValue();
            Long longAttribute = parseLongAttribute(xmlPullParser, ZONE_NOT_USED_AFTER_ATTRIBUTE, null);
            String strConsumeText = consumeText(xmlPullParser);
            checkOnEndTag(xmlPullParser, ZONE_ID_ELEMENT);
            if (strConsumeText == null || strConsumeText.length() == 0) {
                throw new XmlPullParserException("Missing text for id): " + xmlPullParser.getPositionDescription());
            }
            arrayList.add(new CountryTimeZones.TimeZoneMapping(strConsumeText, zBooleanValue, longAttribute));
        }
        return Collections.unmodifiableList(arrayList);
    }

    private static Long parseLongAttribute(XmlPullParser xmlPullParser, String str, Long l) throws XmlPullParserException {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue == null) {
            return l;
        }
        try {
            return Long.valueOf(Long.parseLong(attributeValue));
        } catch (NumberFormatException e) {
            throw new XmlPullParserException("Attribute \"" + str + "\" is not a long value: " + xmlPullParser.getPositionDescription());
        }
    }

    private static Boolean parseBooleanAttribute(XmlPullParser xmlPullParser, String str, Boolean bool) throws XmlPullParserException {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue == null) {
            return bool;
        }
        boolean zEquals = "y".equals(attributeValue);
        if (!zEquals && !FALSE_ATTRIBUTE_VALUE.equals(attributeValue)) {
            throw new XmlPullParserException("Attribute \"" + str + "\" is not \"y\" or \"n\": " + xmlPullParser.getPositionDescription());
        }
        return Boolean.valueOf(zEquals);
    }

    private static void findRequiredStartTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        findStartTag(xmlPullParser, str, true);
    }

    private static boolean findOptionalStartTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        return findStartTag(xmlPullParser, str, false);
    }

    private static boolean findStartTag(XmlPullParser xmlPullParser, String str, boolean z) throws XmlPullParserException, IOException {
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                switch (next) {
                    case 2:
                        String name = xmlPullParser.getName();
                        if (str.equals(name)) {
                            return true;
                        }
                        xmlPullParser.next();
                        consumeUntilEndTag(xmlPullParser, name);
                        break;
                    case 3:
                        if (z) {
                            throw new XmlPullParserException("No child element found with name " + str);
                        }
                        return false;
                }
            } else {
                throw new XmlPullParserException("Unexpected end of document while looking for " + str);
            }
        }
    }

    private static void consumeUntilEndTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        if (xmlPullParser.getEventType() == 3 && str.equals(xmlPullParser.getName())) {
            return;
        }
        int depth = xmlPullParser.getDepth();
        if (xmlPullParser.getEventType() == 2) {
            depth--;
        }
        while (xmlPullParser.getEventType() != 1) {
            int next = xmlPullParser.next();
            int depth2 = xmlPullParser.getDepth();
            if (depth2 < depth) {
                throw new XmlPullParserException("Unexpected depth while looking for end tag: " + xmlPullParser.getPositionDescription());
            }
            if (depth2 == depth && next == 3) {
                if (str.equals(xmlPullParser.getName())) {
                    return;
                }
                throw new XmlPullParserException("Unexpected eng tag: " + xmlPullParser.getPositionDescription());
            }
        }
        throw new XmlPullParserException("Unexpected end of document");
    }

    private static String consumeText(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int next = xmlPullParser.next();
        if (next == 4) {
            String text = xmlPullParser.getText();
            int next2 = xmlPullParser.next();
            if (next2 != 3) {
                throw new XmlPullParserException("Unexpected nested tag or end of document when expecting text: type=" + next2 + " at " + xmlPullParser.getPositionDescription());
            }
            return text;
        }
        throw new XmlPullParserException("Text not found. Found type=" + next + " at " + xmlPullParser.getPositionDescription());
    }

    private static void checkOnEndTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException {
        if (xmlPullParser.getEventType() != 3 || !xmlPullParser.getName().equals(str)) {
            throw new XmlPullParserException("Unexpected tag encountered: " + xmlPullParser.getPositionDescription());
        }
    }

    private interface TimeZonesProcessor {
        public static final boolean CONTINUE = true;
        public static final boolean HALT = false;

        default boolean processHeader(String str) throws XmlPullParserException {
            return true;
        }

        default boolean processCountryZones(String str, String str2, boolean z, List<CountryTimeZones.TimeZoneMapping> list, String str3) throws XmlPullParserException {
            return true;
        }
    }

    private static class TimeZonesValidator implements TimeZonesProcessor {
        private final Set<String> knownCountryCodes;

        private TimeZonesValidator() {
            this.knownCountryCodes = new HashSet();
        }

        @Override
        public boolean processCountryZones(String str, String str2, boolean z, List<CountryTimeZones.TimeZoneMapping> list, String str3) throws XmlPullParserException {
            if (!TimeZoneFinder.normalizeCountryIso(str).equals(str)) {
                throw new XmlPullParserException("Country code: " + str + " is not normalized at " + str3);
            }
            if (this.knownCountryCodes.contains(str)) {
                throw new XmlPullParserException("Second entry for country code: " + str + " at " + str3);
            }
            if (list.isEmpty()) {
                throw new XmlPullParserException("No time zone IDs for country code: " + str + " at " + str3);
            }
            if (!CountryTimeZones.TimeZoneMapping.containsTimeZoneId(list, str2)) {
                throw new XmlPullParserException("defaultTimeZoneId for country code: " + str + " is not one of the zones " + list + " at " + str3);
            }
            this.knownCountryCodes.add(str);
            return true;
        }
    }

    private static class IanaVersionExtractor implements TimeZonesProcessor {
        private String ianaVersion;

        private IanaVersionExtractor() {
        }

        @Override
        public boolean processHeader(String str) throws XmlPullParserException {
            this.ianaVersion = str;
            return false;
        }

        public String getIanaVersion() {
            return this.ianaVersion;
        }
    }

    private static class CountryZonesLookupExtractor implements TimeZonesProcessor {
        private List<CountryTimeZones> countryTimeZonesList;

        private CountryZonesLookupExtractor() {
            this.countryTimeZonesList = new ArrayList(250);
        }

        @Override
        public boolean processCountryZones(String str, String str2, boolean z, List<CountryTimeZones.TimeZoneMapping> list, String str3) throws XmlPullParserException {
            this.countryTimeZonesList.add(CountryTimeZones.createValidated(str, str2, z, list, str3));
            return true;
        }

        CountryZonesFinder getCountryZonesLookup() {
            return new CountryZonesFinder(this.countryTimeZonesList);
        }
    }

    private static class SelectiveCountryTimeZonesExtractor implements TimeZonesProcessor {
        private final String countryCodeToMatch;
        private CountryTimeZones validatedCountryTimeZones;

        private SelectiveCountryTimeZonesExtractor(String str) {
            this.countryCodeToMatch = TimeZoneFinder.normalizeCountryIso(str);
        }

        @Override
        public boolean processCountryZones(String str, String str2, boolean z, List<CountryTimeZones.TimeZoneMapping> list, String str3) {
            String strNormalizeCountryIso = TimeZoneFinder.normalizeCountryIso(str);
            if (!this.countryCodeToMatch.equals(strNormalizeCountryIso)) {
                return true;
            }
            this.validatedCountryTimeZones = CountryTimeZones.createValidated(strNormalizeCountryIso, str2, z, list, str3);
            return false;
        }

        CountryTimeZones getValidatedCountryTimeZones() {
            return this.validatedCountryTimeZones;
        }
    }

    private interface ReaderSupplier {
        Reader get() throws IOException;

        static ReaderSupplier forFile(String str, final Charset charset) throws IOException {
            final Path path = Paths.get(str, new String[0]);
            if (!Files.exists(path, new LinkOption[0])) {
                throw new FileNotFoundException(str + " does not exist");
            }
            if (!Files.isRegularFile(path, new LinkOption[0]) && Files.isReadable(path)) {
                throw new IOException(str + " must be a regular readable file.");
            }
            return new ReaderSupplier() {
                @Override
                public final Reader get() {
                    return Files.newBufferedReader(path, charset);
                }
            };
        }

        static ReaderSupplier forString(final String str) {
            return new ReaderSupplier() {
                @Override
                public final Reader get() {
                    return TimeZoneFinder.ReaderSupplier.lambda$forString$1(str);
                }
            };
        }

        static Reader lambda$forString$1(String str) throws IOException {
            return new StringReader(str);
        }
    }

    private static List<String> extractTimeZoneIds(List<CountryTimeZones.TimeZoneMapping> list) {
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<CountryTimeZones.TimeZoneMapping> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().timeZoneId);
        }
        return Collections.unmodifiableList(arrayList);
    }

    static String normalizeCountryIso(String str) {
        return str.toLowerCase(Locale.US);
    }
}
