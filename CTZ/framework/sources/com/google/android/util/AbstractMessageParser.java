package com.google.android.util;

import android.app.backup.FullBackup;
import android.media.TtmlUtils;
import android.provider.Telephony;
import android.view.ThreadedRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractMessageParser {
    public static final String musicNote = "♫ ";
    private HashMap<Character, Format> formatStart;
    private int nextChar;
    private int nextClass;
    private boolean parseAcronyms;
    private boolean parseFormatting;
    private boolean parseMeText;
    private boolean parseMusic;
    private boolean parseSmilies;
    private boolean parseUrls;
    private ArrayList<Part> parts;
    private String text;
    private ArrayList<Token> tokens;

    public interface Resources {
        TrieNode getAcronyms();

        TrieNode getDomainSuffixes();

        Set<String> getSchemes();

        TrieNode getSmileys();
    }

    protected abstract Resources getResources();

    public AbstractMessageParser(String str) {
        this(str, true, true, true, true, true, true);
    }

    public AbstractMessageParser(String str, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6) {
        this.text = str;
        this.nextChar = 0;
        this.nextClass = 10;
        this.parts = new ArrayList<>();
        this.tokens = new ArrayList<>();
        this.formatStart = new HashMap<>();
        this.parseSmilies = z;
        this.parseAcronyms = z2;
        this.parseFormatting = z3;
        this.parseUrls = z4;
        this.parseMusic = z5;
        this.parseMeText = z6;
    }

    public final String getRawText() {
        return this.text;
    }

    public final int getPartCount() {
        return this.parts.size();
    }

    public final Part getPart(int i) {
        return this.parts.get(i);
    }

    public final List<Part> getParts() {
        return this.parts;
    }

    public void parse() {
        String strSubstring;
        if (parseMusicTrack()) {
            buildParts(null);
            return;
        }
        int i = 0;
        if (this.parseMeText && this.text.startsWith("/me") && this.text.length() > 3 && Character.isWhitespace(this.text.charAt(3))) {
            strSubstring = this.text.substring(0, 4);
            this.text = this.text.substring(4);
        } else {
            strSubstring = null;
        }
        loop0: while (true) {
            boolean z = false;
            while (this.nextChar < this.text.length()) {
                if (!isWordBreak(this.nextChar) && (!z || !isSmileyBreak(this.nextChar))) {
                    break loop0;
                }
                if (parseSmiley()) {
                    z = true;
                } else if (!parseAcronym() && !parseURL() && !parseFormatting()) {
                    parseText();
                }
            }
            for (int i2 = 0; i2 < this.tokens.size(); i2++) {
                if (this.tokens.get(i2).isMedia()) {
                    if (i2 > 0) {
                        int i3 = i2 - 1;
                        if (this.tokens.get(i3) instanceof Html) {
                            ((Html) this.tokens.get(i3)).trimLeadingWhitespace();
                        }
                    }
                    int i4 = i2 + 1;
                    if (i4 < this.tokens.size() && (this.tokens.get(i4) instanceof Html)) {
                        ((Html) this.tokens.get(i4)).trimTrailingWhitespace();
                    }
                }
            }
            while (i < this.tokens.size()) {
                if (this.tokens.get(i).isHtml() && this.tokens.get(i).toHtml(true).length() == 0) {
                    this.tokens.remove(i);
                    i--;
                }
                i++;
            }
            buildParts(strSubstring);
            return;
        }
        throw new AssertionError("last chunk did not end at word break");
    }

    public static Token tokenForUrl(String str, String str2) {
        if (str == null) {
            return null;
        }
        Video videoMatchURL = Video.matchURL(str, str2);
        if (videoMatchURL != null) {
            return videoMatchURL;
        }
        YouTubeVideo youTubeVideoMatchURL = YouTubeVideo.matchURL(str, str2);
        if (youTubeVideoMatchURL != null) {
            return youTubeVideoMatchURL;
        }
        Photo photoMatchURL = Photo.matchURL(str, str2);
        if (photoMatchURL != null) {
            return photoMatchURL;
        }
        FlickrPhoto flickrPhotoMatchURL = FlickrPhoto.matchURL(str, str2);
        if (flickrPhotoMatchURL != null) {
            return flickrPhotoMatchURL;
        }
        return new Link(str, str2);
    }

    private void buildParts(String str) {
        for (int i = 0; i < this.tokens.size(); i++) {
            Token token = this.tokens.get(i);
            if (token.isMedia() || this.parts.size() == 0 || lastPart().isMedia()) {
                this.parts.add(new Part());
            }
            lastPart().add(token);
        }
        if (this.parts.size() > 0) {
            this.parts.get(0).setMeText(str);
        }
    }

    private Part lastPart() {
        return this.parts.get(this.parts.size() - 1);
    }

    private boolean parseMusicTrack() {
        if (this.parseMusic && this.text.startsWith(musicNote)) {
            addToken(new MusicTrack(this.text.substring(musicNote.length())));
            this.nextChar = this.text.length();
            return true;
        }
        return false;
    }

    private void parseText() {
        StringBuilder sb = new StringBuilder();
        int i = this.nextChar;
        do {
            String str = this.text;
            int i2 = this.nextChar;
            this.nextChar = i2 + 1;
            char cCharAt = str.charAt(i2);
            if (cCharAt == '\n') {
                sb.append("<br>");
            } else if (cCharAt == '\"') {
                sb.append("&quot;");
            } else if (cCharAt == '<') {
                sb.append("&lt;");
            } else if (cCharAt == '>') {
                sb.append("&gt;");
            } else {
                switch (cCharAt) {
                    case '&':
                        sb.append("&amp;");
                        break;
                    case '\'':
                        sb.append("&apos;");
                        break;
                    default:
                        sb.append(cCharAt);
                        break;
                }
            }
        } while (!isWordBreak(this.nextChar));
        addToken(new Html(this.text.substring(i, this.nextChar), sb.toString()));
    }

    private boolean parseSmiley() {
        TrieNode trieNodeLongestMatch;
        if (!this.parseSmilies || (trieNodeLongestMatch = longestMatch(getResources().getSmileys(), this, this.nextChar, true)) == null) {
            return false;
        }
        int charClass = getCharClass(this.nextChar - 1);
        int charClass2 = getCharClass(this.nextChar + trieNodeLongestMatch.getText().length());
        if ((charClass == 2 || charClass == 3) && (charClass2 == 2 || charClass2 == 3)) {
            return false;
        }
        addToken(new Smiley(trieNodeLongestMatch.getText()));
        this.nextChar += trieNodeLongestMatch.getText().length();
        return true;
    }

    private boolean parseAcronym() {
        TrieNode trieNodeLongestMatch;
        if (!this.parseAcronyms || (trieNodeLongestMatch = longestMatch(getResources().getAcronyms(), this, this.nextChar)) == null) {
            return false;
        }
        addToken(new Acronym(trieNodeLongestMatch.getText(), trieNodeLongestMatch.getValue()));
        this.nextChar += trieNodeLongestMatch.getText().length();
        return true;
    }

    private boolean isDomainChar(char c) {
        return c == '-' || Character.isLetter(c) || Character.isDigit(c);
    }

    private boolean isValidDomain(String str) {
        if (matches(getResources().getDomainSuffixes(), reverse(str))) {
            return true;
        }
        return false;
    }

    private boolean parseURL() {
        char cCharAt;
        boolean z = false;
        if (!this.parseUrls || !isURLBreak(this.nextChar)) {
            return false;
        }
        int i = this.nextChar;
        int i2 = i;
        while (i2 < this.text.length() && isDomainChar(this.text.charAt(i2))) {
            i2++;
        }
        String str = "";
        if (i2 == this.text.length()) {
            return false;
        }
        if (this.text.charAt(i2) == ':') {
            if (!getResources().getSchemes().contains(this.text.substring(this.nextChar, i2))) {
                return false;
            }
        } else {
            if (this.text.charAt(i2) != '.') {
                return false;
            }
            while (i2 < this.text.length() && ((cCharAt = this.text.charAt(i2)) == '.' || isDomainChar(cCharAt))) {
                i2++;
            }
            if (!isValidDomain(this.text.substring(this.nextChar, i2))) {
                return false;
            }
            int i3 = i2 + 1;
            if (i3 < this.text.length() && this.text.charAt(i2) == ':' && Character.isDigit(this.text.charAt(i3))) {
                while (i3 < this.text.length() && Character.isDigit(this.text.charAt(i3))) {
                    i3++;
                }
                i2 = i3;
            }
            if (i2 != this.text.length()) {
                char cCharAt2 = this.text.charAt(i2);
                if (cCharAt2 == '?') {
                    int i4 = i2 + 1;
                    if (i4 != this.text.length()) {
                        char cCharAt3 = this.text.charAt(i4);
                        if (Character.isWhitespace(cCharAt3) || isPunctuation(cCharAt3)) {
                            z = true;
                        }
                    } else {
                        z = true;
                    }
                    str = "http://";
                } else {
                    if (!isPunctuation(cCharAt2) && !Character.isWhitespace(cCharAt2)) {
                        if (cCharAt2 != '/' && cCharAt2 != '#') {
                            return false;
                        }
                    }
                    str = "http://";
                }
            }
        }
        if (!z) {
            while (i2 < this.text.length() && !Character.isWhitespace(this.text.charAt(i2))) {
                i2++;
            }
        }
        String strSubstring = this.text.substring(i, i2);
        addURLToken(str + strSubstring, strSubstring);
        this.nextChar = i2;
        return true;
    }

    private void addURLToken(String str, String str2) {
        addToken(tokenForUrl(str, str2));
    }

    private boolean parseFormatting() {
        if (!this.parseFormatting) {
            return false;
        }
        int i = this.nextChar;
        while (i < this.text.length() && isFormatChar(this.text.charAt(i))) {
            i++;
        }
        if (i == this.nextChar || !isWordBreak(i)) {
            return false;
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (int i2 = this.nextChar; i2 < i; i2++) {
            char cCharAt = this.text.charAt(i2);
            Character chValueOf = Character.valueOf(cCharAt);
            if (linkedHashMap.containsKey(chValueOf)) {
                addToken(new Format(cCharAt, false));
            } else {
                Format format = this.formatStart.get(chValueOf);
                if (format != null) {
                    format.setMatched(true);
                    this.formatStart.remove(chValueOf);
                    linkedHashMap.put(chValueOf, Boolean.TRUE);
                } else {
                    Format format2 = new Format(cCharAt, true);
                    this.formatStart.put(chValueOf, format2);
                    addToken(format2);
                    linkedHashMap.put(chValueOf, Boolean.FALSE);
                }
            }
        }
        for (Character ch : linkedHashMap.keySet()) {
            if (linkedHashMap.get(ch) == Boolean.TRUE) {
                Format format3 = new Format(ch.charValue(), false);
                format3.setMatched(true);
                addToken(format3);
            }
        }
        this.nextChar = i;
        return true;
    }

    private boolean isWordBreak(int i) {
        return getCharClass(i + (-1)) != getCharClass(i);
    }

    private boolean isSmileyBreak(int i) {
        if (i > 0 && i < this.text.length() && isSmileyBreak(this.text.charAt(i - 1), this.text.charAt(i))) {
            return true;
        }
        return false;
    }

    private boolean isURLBreak(int i) {
        switch (getCharClass(i - 1)) {
            case 2:
            case 3:
            case 4:
                return false;
            default:
                return true;
        }
    }

    private int getCharClass(int i) {
        if (i < 0 || this.text.length() <= i) {
            return 0;
        }
        char cCharAt = this.text.charAt(i);
        if (Character.isWhitespace(cCharAt)) {
            return 1;
        }
        if (Character.isLetter(cCharAt)) {
            return 2;
        }
        if (Character.isDigit(cCharAt)) {
            return 3;
        }
        if (isPunctuation(cCharAt)) {
            int i2 = this.nextClass + 1;
            this.nextClass = i2;
            return i2;
        }
        return 4;
    }

    private static boolean isSmileyBreak(char c, char c2) {
        if (c != '$' && c != '&' && c != '-' && c != '/' && c != '@') {
            switch (c) {
                case '*':
                case '+':
                    break;
                default:
                    switch (c) {
                        case '<':
                        case '=':
                        case '>':
                            break;
                        default:
                            switch (c) {
                                case '[':
                                case '\\':
                                case ']':
                                case '^':
                                    break;
                                default:
                                    switch (c) {
                                        case '|':
                                        case '}':
                                        case '~':
                                            break;
                                        default:
                                            return false;
                                    }
                                    break;
                            }
                            break;
                    }
                    break;
            }
        }
        switch (c2) {
            case '#':
            case '$':
            case '%':
            case '*':
            case '/':
            case '<':
            case '=':
            case '>':
            case '@':
            case '[':
            case '\\':
            case '^':
            case '~':
                return true;
            default:
                return false;
        }
    }

    private static boolean isPunctuation(char c) {
        switch (c) {
            case '!':
            case '\"':
            case '(':
            case ')':
            case ',':
            case '.':
            case ':':
            case ';':
            case '?':
                return true;
            default:
                return false;
        }
    }

    private static boolean isFormatChar(char c) {
        if (c != '*') {
            switch (c) {
                case '^':
                case '_':
                    return true;
                default:
                    return false;
            }
        }
        return true;
    }

    public static abstract class Token {
        protected String text;
        protected Type type;

        public abstract boolean isHtml();

        public enum Type {
            HTML("html"),
            FORMAT("format"),
            LINK("l"),
            SMILEY("e"),
            ACRONYM(FullBackup.APK_TREE_TOKEN),
            MUSIC("m"),
            GOOGLE_VIDEO(Telephony.BaseMmsColumns.MMS_VERSION),
            YOUTUBE_VIDEO("yt"),
            PHOTO(TtmlUtils.TAG_P),
            FLICKR(FullBackup.FILES_TREE_TOKEN);

            private String stringRep;

            Type(String str) {
                this.stringRep = str;
            }

            @Override
            public String toString() {
                return this.stringRep;
            }
        }

        protected Token(Type type, String str) {
            this.type = type;
            this.text = str;
        }

        public Type getType() {
            return this.type;
        }

        public List<String> getInfo() {
            ArrayList arrayList = new ArrayList();
            arrayList.add(getType().toString());
            return arrayList;
        }

        public String getRawText() {
            return this.text;
        }

        public boolean isMedia() {
            return false;
        }

        public boolean isArray() {
            return !isHtml();
        }

        public String toHtml(boolean z) {
            throw new AssertionError("not html");
        }

        public boolean controlCaps() {
            return false;
        }

        public boolean setCaps() {
            return false;
        }
    }

    public static class Html extends Token {
        private String html;

        public Html(String str, String str2) {
            super(Token.Type.HTML, str);
            this.html = str2;
        }

        @Override
        public boolean isHtml() {
            return true;
        }

        @Override
        public String toHtml(boolean z) {
            return z ? this.html.toUpperCase() : this.html;
        }

        @Override
        public List<String> getInfo() {
            throw new UnsupportedOperationException();
        }

        public void trimLeadingWhitespace() {
            this.text = trimLeadingWhitespace(this.text);
            this.html = trimLeadingWhitespace(this.html);
        }

        public void trimTrailingWhitespace() {
            this.text = trimTrailingWhitespace(this.text);
            this.html = trimTrailingWhitespace(this.html);
        }

        private static String trimLeadingWhitespace(String str) {
            int i = 0;
            while (i < str.length() && Character.isWhitespace(str.charAt(i))) {
                i++;
            }
            return str.substring(i);
        }

        public static String trimTrailingWhitespace(String str) {
            int length = str.length();
            while (length > 0 && Character.isWhitespace(str.charAt(length - 1))) {
                length--;
            }
            return str.substring(0, length);
        }
    }

    public static class MusicTrack extends Token {
        private String track;

        public MusicTrack(String str) {
            super(Token.Type.MUSIC, str);
            this.track = str;
        }

        public String getTrack() {
            return this.track;
        }

        @Override
        public boolean isHtml() {
            return false;
        }

        @Override
        public List<String> getInfo() {
            List<String> info = super.getInfo();
            info.add(getTrack());
            return info;
        }
    }

    public static class Link extends Token {
        private String url;

        public Link(String str, String str2) {
            super(Token.Type.LINK, str2);
            this.url = str;
        }

        public String getURL() {
            return this.url;
        }

        @Override
        public boolean isHtml() {
            return false;
        }

        @Override
        public List<String> getInfo() {
            List<String> info = super.getInfo();
            info.add(getURL());
            info.add(getRawText());
            return info;
        }
    }

    public static class Video extends Token {
        private static final Pattern URL_PATTERN = Pattern.compile("(?i)http://video\\.google\\.[a-z0-9]+(?:\\.[a-z0-9]+)?/videoplay\\?.*?\\bdocid=(-?\\d+).*");
        private String docid;

        public Video(String str, String str2) {
            super(Token.Type.GOOGLE_VIDEO, str2);
            this.docid = str;
        }

        public String getDocID() {
            return this.docid;
        }

        @Override
        public boolean isHtml() {
            return false;
        }

        @Override
        public boolean isMedia() {
            return true;
        }

        public static Video matchURL(String str, String str2) {
            Matcher matcher = URL_PATTERN.matcher(str);
            if (matcher.matches()) {
                return new Video(matcher.group(1), str2);
            }
            return null;
        }

        @Override
        public List<String> getInfo() {
            List<String> info = super.getInfo();
            info.add(getRssUrl(this.docid));
            info.add(getURL(this.docid));
            return info;
        }

        public static String getRssUrl(String str) {
            return "http://video.google.com/videofeed?type=docid&output=rss&sourceid=gtalk&docid=" + str;
        }

        public static String getURL(String str) {
            return getURL(str, null);
        }

        public static String getURL(String str, String str2) {
            if (str2 == null) {
                str2 = "";
            } else if (str2.length() > 0) {
                str2 = str2 + "&";
            }
            return "http://video.google.com/videoplay?" + str2 + "docid=" + str;
        }
    }

    public static class YouTubeVideo extends Token {
        private static final Pattern URL_PATTERN = Pattern.compile("(?i)http://(?:[a-z0-9]+\\.)?youtube\\.[a-z0-9]+(?:\\.[a-z0-9]+)?/watch\\?.*\\bv=([-_a-zA-Z0-9=]+).*");
        private String docid;

        public YouTubeVideo(String str, String str2) {
            super(Token.Type.YOUTUBE_VIDEO, str2);
            this.docid = str;
        }

        public String getDocID() {
            return this.docid;
        }

        @Override
        public boolean isHtml() {
            return false;
        }

        @Override
        public boolean isMedia() {
            return true;
        }

        public static YouTubeVideo matchURL(String str, String str2) {
            Matcher matcher = URL_PATTERN.matcher(str);
            if (matcher.matches()) {
                return new YouTubeVideo(matcher.group(1), str2);
            }
            return null;
        }

        @Override
        public List<String> getInfo() {
            List<String> info = super.getInfo();
            info.add(getRssUrl(this.docid));
            info.add(getURL(this.docid));
            return info;
        }

        public static String getRssUrl(String str) {
            return "http://youtube.com/watch?v=" + str;
        }

        public static String getURL(String str) {
            return getURL(str, null);
        }

        public static String getURL(String str, String str2) {
            if (str2 == null) {
                str2 = "";
            } else if (str2.length() > 0) {
                str2 = str2 + "&";
            }
            return "http://youtube.com/watch?" + str2 + "v=" + str;
        }

        public static String getPrefixedURL(boolean z, String str, String str2, String str3) {
            String str4 = "";
            if (z) {
                str4 = "http://";
            }
            if (str == null) {
                str = "";
            }
            if (str3 == null) {
                str3 = "";
            } else if (str3.length() > 0) {
                str3 = str3 + "&";
            }
            return str4 + str + "youtube.com/watch?" + str3 + "v=" + str2;
        }
    }

    public static class Photo extends Token {
        private static final Pattern URL_PATTERN = Pattern.compile("http://picasaweb.google.com/([^/?#&]+)/+((?!searchbrowse)[^/?#&]+)(?:/|/photo)?(?:\\?[^#]*)?(?:#(.*))?");
        private String album;
        private String photo;
        private String user;

        public Photo(String str, String str2, String str3, String str4) {
            super(Token.Type.PHOTO, str4);
            this.user = str;
            this.album = str2;
            this.photo = str3;
        }

        public String getUser() {
            return this.user;
        }

        public String getAlbum() {
            return this.album;
        }

        public String getPhoto() {
            return this.photo;
        }

        @Override
        public boolean isHtml() {
            return false;
        }

        @Override
        public boolean isMedia() {
            return true;
        }

        public static Photo matchURL(String str, String str2) {
            Matcher matcher = URL_PATTERN.matcher(str);
            if (matcher.matches()) {
                return new Photo(matcher.group(1), matcher.group(2), matcher.group(3), str2);
            }
            return null;
        }

        @Override
        public List<String> getInfo() {
            List<String> info = super.getInfo();
            info.add(getRssUrl(getUser()));
            info.add(getAlbumURL(getUser(), getAlbum()));
            if (getPhoto() != null) {
                info.add(getPhotoURL(getUser(), getAlbum(), getPhoto()));
            } else {
                info.add((String) null);
            }
            return info;
        }

        public static String getRssUrl(String str) {
            return "http://picasaweb.google.com/data/feed/api/user/" + str + "?category=album&alt=rss";
        }

        public static String getAlbumURL(String str, String str2) {
            return "http://picasaweb.google.com/" + str + "/" + str2;
        }

        public static String getPhotoURL(String str, String str2, String str3) {
            return "http://picasaweb.google.com/" + str + "/" + str2 + "/photo#" + str3;
        }
    }

    public static class FlickrPhoto extends Token {
        private static final String SETS = "sets";
        private static final String TAGS = "tags";
        private String grouping;
        private String groupingId;
        private String photo;
        private String user;
        private static final Pattern URL_PATTERN = Pattern.compile("http://(?:www.)?flickr.com/photos/([^/?#&]+)/?([^/?#&]+)?/?.*");
        private static final Pattern GROUPING_PATTERN = Pattern.compile("http://(?:www.)?flickr.com/photos/([^/?#&]+)/(tags|sets)/([^/?#&]+)/?");

        public FlickrPhoto(String str, String str2, String str3, String str4, String str5) {
            super(Token.Type.FLICKR, str5);
            if (!"tags".equals(str)) {
                this.user = str;
                this.photo = ThreadedRenderer.OVERDRAW_PROPERTY_SHOW.equals(str2) ? null : str2;
                this.grouping = str3;
                this.groupingId = str4;
                return;
            }
            this.user = null;
            this.photo = null;
            this.grouping = "tags";
            this.groupingId = str2;
        }

        public String getUser() {
            return this.user;
        }

        public String getPhoto() {
            return this.photo;
        }

        public String getGrouping() {
            return this.grouping;
        }

        public String getGroupingId() {
            return this.groupingId;
        }

        @Override
        public boolean isHtml() {
            return false;
        }

        @Override
        public boolean isMedia() {
            return true;
        }

        public static FlickrPhoto matchURL(String str, String str2) {
            Matcher matcher = GROUPING_PATTERN.matcher(str);
            if (matcher.matches()) {
                return new FlickrPhoto(matcher.group(1), null, matcher.group(2), matcher.group(3), str2);
            }
            Matcher matcher2 = URL_PATTERN.matcher(str);
            if (matcher2.matches()) {
                return new FlickrPhoto(matcher2.group(1), matcher2.group(2), null, null, str2);
            }
            return null;
        }

        @Override
        public List<String> getInfo() {
            List<String> info = super.getInfo();
            info.add(getUrl());
            info.add(getUser() != null ? getUser() : "");
            info.add(getPhoto() != null ? getPhoto() : "");
            info.add(getGrouping() != null ? getGrouping() : "");
            info.add(getGroupingId() != null ? getGroupingId() : "");
            return info;
        }

        public String getUrl() {
            if (SETS.equals(this.grouping)) {
                return getUserSetsURL(this.user, this.groupingId);
            }
            if ("tags".equals(this.grouping)) {
                if (this.user != null) {
                    return getUserTagsURL(this.user, this.groupingId);
                }
                return getTagsURL(this.groupingId);
            }
            if (this.photo != null) {
                return getPhotoURL(this.user, this.photo);
            }
            return getUserURL(this.user);
        }

        public static String getRssUrl(String str) {
            return null;
        }

        public static String getTagsURL(String str) {
            return "http://flickr.com/photos/tags/" + str;
        }

        public static String getUserURL(String str) {
            return "http://flickr.com/photos/" + str;
        }

        public static String getPhotoURL(String str, String str2) {
            return "http://flickr.com/photos/" + str + "/" + str2;
        }

        public static String getUserTagsURL(String str, String str2) {
            return "http://flickr.com/photos/" + str + "/tags/" + str2;
        }

        public static String getUserSetsURL(String str, String str2) {
            return "http://flickr.com/photos/" + str + "/sets/" + str2;
        }
    }

    public static class Smiley extends Token {
        public Smiley(String str) {
            super(Token.Type.SMILEY, str);
        }

        @Override
        public boolean isHtml() {
            return false;
        }

        @Override
        public List<String> getInfo() {
            List<String> info = super.getInfo();
            info.add(getRawText());
            return info;
        }
    }

    public static class Acronym extends Token {
        private String value;

        public Acronym(String str, String str2) {
            super(Token.Type.ACRONYM, str);
            this.value = str2;
        }

        public String getValue() {
            return this.value;
        }

        @Override
        public boolean isHtml() {
            return false;
        }

        @Override
        public List<String> getInfo() {
            List<String> info = super.getInfo();
            info.add(getRawText());
            info.add(getValue());
            return info;
        }
    }

    public static class Format extends Token {
        private char ch;
        private boolean matched;
        private boolean start;

        public Format(char c, boolean z) {
            super(Token.Type.FORMAT, String.valueOf(c));
            this.ch = c;
            this.start = z;
        }

        public void setMatched(boolean z) {
            this.matched = z;
        }

        @Override
        public boolean isHtml() {
            return true;
        }

        @Override
        public String toHtml(boolean z) {
            return this.matched ? this.start ? getFormatStart(this.ch) : getFormatEnd(this.ch) : this.ch == '\"' ? "&quot;" : String.valueOf(this.ch);
        }

        @Override
        public List<String> getInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean controlCaps() {
            return this.ch == '^';
        }

        @Override
        public boolean setCaps() {
            return this.start;
        }

        private String getFormatStart(char c) {
            if (c == '\"') {
                return "<font color=\"#999999\">“";
            }
            if (c == '*') {
                return "<b>";
            }
            switch (c) {
                case '^':
                    return "<b><font color=\"#005FFF\">";
                case '_':
                    return "<i>";
                default:
                    throw new AssertionError("unknown format '" + c + "'");
            }
        }

        private String getFormatEnd(char c) {
            if (c == '\"') {
                return "”</font>";
            }
            if (c == '*') {
                return "</b>";
            }
            switch (c) {
                case '^':
                    return "</font></b>";
                case '_':
                    return "</i>";
                default:
                    throw new AssertionError("unknown format '" + c + "'");
            }
        }
    }

    private void addToken(Token token) {
        this.tokens.add(token);
    }

    public String toHtml() {
        StringBuilder sb = new StringBuilder();
        for (Part part : this.parts) {
            boolean caps = false;
            sb.append("<p>");
            for (Token token : part.getTokens()) {
                if (token.isHtml()) {
                    sb.append(token.toHtml(caps));
                } else {
                    switch (token.getType()) {
                        case LINK:
                            sb.append("<a href=\"");
                            sb.append(((Link) token).getURL());
                            sb.append("\">");
                            sb.append(token.getRawText());
                            sb.append("</a>");
                            break;
                        case SMILEY:
                            sb.append(token.getRawText());
                            break;
                        case ACRONYM:
                            sb.append(token.getRawText());
                            break;
                        case MUSIC:
                            sb.append(((MusicTrack) token).getTrack());
                            break;
                        case GOOGLE_VIDEO:
                            sb.append("<a href=\"");
                            sb.append(Video.getURL(((Video) token).getDocID()));
                            sb.append("\">");
                            sb.append(token.getRawText());
                            sb.append("</a>");
                            break;
                        case YOUTUBE_VIDEO:
                            sb.append("<a href=\"");
                            sb.append(YouTubeVideo.getURL(((YouTubeVideo) token).getDocID()));
                            sb.append("\">");
                            sb.append(token.getRawText());
                            sb.append("</a>");
                            break;
                        case PHOTO:
                            sb.append("<a href=\"");
                            Photo photo = (Photo) token;
                            sb.append(Photo.getAlbumURL(photo.getUser(), photo.getAlbum()));
                            sb.append("\">");
                            sb.append(token.getRawText());
                            sb.append("</a>");
                            break;
                        case FLICKR:
                            sb.append("<a href=\"");
                            sb.append(((FlickrPhoto) token).getUrl());
                            sb.append("\">");
                            sb.append(token.getRawText());
                            sb.append("</a>");
                            break;
                        default:
                            throw new AssertionError("unknown token type: " + token.getType());
                    }
                }
                if (token.controlCaps()) {
                    caps = token.setCaps();
                }
            }
            sb.append("</p>\n");
        }
        return sb.toString();
    }

    protected static String reverse(String str) {
        StringBuilder sb = new StringBuilder();
        for (int length = str.length() - 1; length >= 0; length--) {
            sb.append(str.charAt(length));
        }
        return sb.toString();
    }

    public static class TrieNode {
        private final HashMap<Character, TrieNode> children;
        private String text;
        private String value;

        public TrieNode() {
            this("");
        }

        public TrieNode(String str) {
            this.children = new HashMap<>();
            this.text = str;
        }

        public final boolean exists() {
            return this.value != null;
        }

        public final String getText() {
            return this.text;
        }

        public final String getValue() {
            return this.value;
        }

        public void setValue(String str) {
            this.value = str;
        }

        public TrieNode getChild(char c) {
            return this.children.get(Character.valueOf(c));
        }

        public TrieNode getOrCreateChild(char c) {
            Character chValueOf = Character.valueOf(c);
            TrieNode trieNode = this.children.get(chValueOf);
            if (trieNode == null) {
                TrieNode trieNode2 = new TrieNode(this.text + String.valueOf(c));
                this.children.put(chValueOf, trieNode2);
                return trieNode2;
            }
            return trieNode;
        }

        public static void addToTrie(TrieNode trieNode, String str, String str2) {
            for (int i = 0; i < str.length(); i++) {
                trieNode = trieNode.getOrCreateChild(str.charAt(i));
            }
            trieNode.setValue(str2);
        }
    }

    private static boolean matches(TrieNode trieNode, String str) {
        TrieNode child = trieNode;
        int i = 0;
        while (i < str.length()) {
            int i2 = i + 1;
            child = child.getChild(str.charAt(i));
            if (child == null) {
                break;
            }
            if (!child.exists()) {
                i = i2;
            } else {
                return true;
            }
        }
        return false;
    }

    private static TrieNode longestMatch(TrieNode trieNode, AbstractMessageParser abstractMessageParser, int i) {
        return longestMatch(trieNode, abstractMessageParser, i, false);
    }

    private static TrieNode longestMatch(TrieNode trieNode, AbstractMessageParser abstractMessageParser, int i, boolean z) {
        TrieNode trieNode2 = null;
        while (i < abstractMessageParser.getRawText().length()) {
            int i2 = i + 1;
            trieNode = trieNode.getChild(abstractMessageParser.getRawText().charAt(i));
            if (trieNode == null) {
                break;
            }
            if (trieNode.exists() && (abstractMessageParser.isWordBreak(i2) || (z && abstractMessageParser.isSmileyBreak(i2)))) {
                trieNode2 = trieNode;
            }
            i = i2;
        }
        return trieNode2;
    }

    public static class Part {
        private String meText;
        private ArrayList<Token> tokens = new ArrayList<>();

        public String getType(boolean z) {
            StringBuilder sb = new StringBuilder();
            sb.append(z ? "s" : FullBackup.ROOT_TREE_TOKEN);
            sb.append(getPartType());
            return sb.toString();
        }

        private String getPartType() {
            if (isMedia()) {
                return "d";
            }
            if (this.meText != null) {
                return "m";
            }
            return "";
        }

        public boolean isMedia() {
            return this.tokens.size() == 1 && this.tokens.get(0).isMedia();
        }

        public Token getMediaToken() {
            if (isMedia()) {
                return this.tokens.get(0);
            }
            return null;
        }

        public void add(Token token) {
            if (isMedia()) {
                throw new AssertionError("media ");
            }
            this.tokens.add(token);
        }

        public void setMeText(String str) {
            this.meText = str;
        }

        public String getRawText() {
            StringBuilder sb = new StringBuilder();
            if (this.meText != null) {
                sb.append(this.meText);
            }
            for (int i = 0; i < this.tokens.size(); i++) {
                sb.append(this.tokens.get(i).getRawText());
            }
            return sb.toString();
        }

        public ArrayList<Token> getTokens() {
            return this.tokens;
        }
    }
}
