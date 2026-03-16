package com.android.providers.downloads;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.os.SystemClock;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helpers {
    public static Random sRandom = new Random(SystemClock.uptimeMillis());
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile("attachment;\\s*filename\\s*=\\s*\"([^\"]*)\"");
    private static final Object sUniqueLock = new Object();

    private static String parseContentDisposition(String contentDisposition) {
        try {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(1);
            }
        } catch (IllegalStateException e) {
        }
        return null;
    }

    static String generateSaveFile(Context context, String url, String hint, String contentDisposition, String contentLocation, String mimeType, int destination) throws IOException {
        File parent;
        File[] parentTest;
        String name;
        String prefix;
        String suffix;
        String absolutePath;
        if (destination == 4) {
            File file = new File(Uri.parse(hint).getPath());
            parent = file.getParentFile().getAbsoluteFile();
            parentTest = new File[]{parent};
            name = file.getName();
        } else {
            parent = getRunningDestinationDirectory(context, destination);
            parentTest = new File[]{parent, getSuccessDestinationDirectory(context, destination)};
            name = chooseFilename(url, hint, contentDisposition, contentLocation);
        }
        File[] arr$ = parentTest;
        for (File test : arr$) {
            if (!test.isDirectory() && !test.mkdirs()) {
                throw new IOException("Failed to create parent for " + test);
            }
        }
        if (DownloadDrmHelper.isDrmConvertNeeded(mimeType)) {
            name = DownloadDrmHelper.modifyDrmFwLockFileExtension(name);
        }
        int dotIndex = name.lastIndexOf(46);
        boolean missingExtension = dotIndex < 0;
        if (destination == 4) {
            if (missingExtension) {
                prefix = name;
                suffix = "";
            } else {
                prefix = name.substring(0, dotIndex);
                suffix = name.substring(dotIndex);
            }
        } else if (missingExtension) {
            prefix = name;
            suffix = chooseExtensionFromMimeType(mimeType, true);
        } else {
            prefix = name.substring(0, dotIndex);
            suffix = chooseExtensionFromFilename(mimeType, destination, name, dotIndex);
        }
        synchronized (sUniqueLock) {
            String name2 = generateAvailableFilenameLocked(parentTest, prefix, suffix);
            File file2 = new File(parent, name2);
            file2.createNewFile();
            absolutePath = file2.getAbsolutePath();
        }
        return absolutePath;
    }

    private static String chooseFilename(String url, String hint, String contentDisposition, String contentLocation) {
        String decodedUrl;
        int index;
        String decodedContentLocation;
        String filename = null;
        if (0 == 0 && hint != null && !hint.endsWith("/")) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "getting filename from hint");
            }
            int index2 = hint.lastIndexOf(47) + 1;
            filename = index2 > 0 ? hint.substring(index2) : hint;
        }
        if (filename == null && contentDisposition != null && (filename = parseContentDisposition(contentDisposition)) != null) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "getting filename from content-disposition");
            }
            int index3 = filename.lastIndexOf(47) + 1;
            if (index3 > 0) {
                filename = filename.substring(index3);
            }
        }
        if (filename == null && contentLocation != null && (decodedContentLocation = Uri.decode(contentLocation)) != null && !decodedContentLocation.endsWith("/") && decodedContentLocation.indexOf(63) < 0) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "getting filename from content-location");
            }
            int index4 = decodedContentLocation.lastIndexOf(47) + 1;
            if (index4 > 0) {
                filename = decodedContentLocation.substring(index4);
            } else {
                filename = decodedContentLocation;
            }
        }
        if (filename == null && (decodedUrl = Uri.decode(url)) != null && !decodedUrl.endsWith("/") && decodedUrl.indexOf(63) < 0 && (index = decodedUrl.lastIndexOf(47) + 1) > 0) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "getting filename from uri");
            }
            filename = decodedUrl.substring(index);
        }
        if (filename == null) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "using default filename");
            }
            filename = "downloadfile";
        }
        return FileUtils.buildValidFatFilename(filename);
    }

    private static String chooseExtensionFromMimeType(String mimeType, boolean useDefaults) {
        String extension = null;
        if (mimeType != null) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension != null) {
                if (Constants.LOGVV) {
                    Log.v("DownloadManager", "adding extension from type");
                }
                extension = "." + extension;
            } else if (Constants.LOGVV) {
                Log.v("DownloadManager", "couldn't find extension for " + mimeType);
            }
        }
        if (extension == null) {
            if (mimeType != null && mimeType.toLowerCase().startsWith("text/")) {
                if (mimeType.equalsIgnoreCase("text/html")) {
                    if (Constants.LOGVV) {
                        Log.v("DownloadManager", "adding default html extension");
                    }
                    return ".html";
                }
                if (useDefaults) {
                    if (Constants.LOGVV) {
                        Log.v("DownloadManager", "adding default text extension");
                    }
                    return ".txt";
                }
                return extension;
            }
            if (useDefaults) {
                if (Constants.LOGVV) {
                    Log.v("DownloadManager", "adding default binary extension");
                }
                return ".bin";
            }
            return extension;
        }
        return extension;
    }

    private static String chooseExtensionFromFilename(String mimeType, int destination, String filename, int lastDotIndex) {
        String typeFromExt;
        String extension = null;
        if (mimeType != null && ((typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(filename.substring(lastDotIndex + 1))) == null || !typeFromExt.equalsIgnoreCase(mimeType))) {
            extension = chooseExtensionFromMimeType(mimeType, false);
            if (extension != null) {
                if (Constants.LOGVV) {
                    Log.v("DownloadManager", "substituting extension from type");
                }
            } else if (Constants.LOGVV) {
                Log.v("DownloadManager", "couldn't find extension for " + mimeType);
            }
        }
        if (extension == null) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "keeping extension");
            }
            return filename.substring(lastDotIndex);
        }
        return extension;
    }

    private static boolean isFilenameAvailableLocked(File[] parents, String name) {
        if ("recovery".equalsIgnoreCase(name)) {
            return false;
        }
        for (File parent : parents) {
            if (new File(parent, name).exists()) {
                return false;
            }
        }
        return true;
    }

    private static String generateAvailableFilenameLocked(File[] parents, String prefix, String suffix) throws IOException {
        String name = prefix + suffix;
        if (isFilenameAvailableLocked(parents, name)) {
            return name;
        }
        int sequence = 1;
        for (int magnitude = 1; magnitude < 1000000000; magnitude *= 10) {
            for (int iteration = 0; iteration < 9; iteration++) {
                String name2 = prefix + "-" + sequence + suffix;
                if (isFilenameAvailableLocked(parents, name2)) {
                    return name2;
                }
                sequence += sRandom.nextInt(magnitude) + 1;
            }
        }
        throw new IOException("Failed to generate an available filename");
    }

    static boolean isFilenameValid(Context context, File file) {
        try {
            File[] whitelist = {context.getFilesDir().getCanonicalFile(), context.getCacheDir().getCanonicalFile(), Environment.getDownloadCacheDirectory().getCanonicalFile(), Environment.getExternalStorageDirectory().getCanonicalFile()};
            for (File testDir : whitelist) {
                if (FileUtils.contains(testDir, file)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            Log.w("DownloadManager", "Failed to resolve canonical path: " + e);
            return false;
        }
    }

    public static File getRunningDestinationDirectory(Context context, int destination) throws IOException {
        return getDestinationDirectory(context, destination, true);
    }

    public static File getSuccessDestinationDirectory(Context context, int destination) throws IOException {
        return getDestinationDirectory(context, destination, false);
    }

    private static File getDestinationDirectory(Context context, int destination, boolean running) throws IOException {
        switch (destination) {
            case 0:
                File target = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
                if (!target.isDirectory() && target.mkdirs()) {
                    throw new IOException("unable to create external downloads directory");
                }
                return target;
            case 1:
            case 2:
            case 3:
                if (running) {
                    return context.getFilesDir();
                }
                return context.getCacheDir();
            case 4:
            default:
                throw new IllegalStateException("unexpected destination: " + destination);
            case 5:
                if (running) {
                    return new File(Environment.getDownloadCacheDirectory(), "partial_downloads");
                }
                return Environment.getDownloadCacheDirectory();
        }
    }

    public static void validateSelection(String selection, Set<String> allowedColumns) {
        if (selection != null) {
            try {
                if (!selection.isEmpty()) {
                    Lexer lexer = new Lexer(selection, allowedColumns);
                    parseExpression(lexer);
                    if (lexer.currentToken() != 9) {
                        throw new IllegalArgumentException("syntax error");
                    }
                }
            } catch (RuntimeException ex) {
                if (Constants.LOGV) {
                    Log.d("DownloadManager", "invalid selection [" + selection + "] triggered " + ex);
                }
                throw ex;
            }
        }
    }

    private static void parseExpression(Lexer lexer) {
        while (true) {
            if (lexer.currentToken() == 1) {
                lexer.advance();
                parseExpression(lexer);
                if (lexer.currentToken() != 2) {
                    throw new IllegalArgumentException("syntax error, unmatched parenthese");
                }
                lexer.advance();
            } else {
                parseStatement(lexer);
            }
            if (lexer.currentToken() == 3) {
                lexer.advance();
            } else {
                return;
            }
        }
    }

    private static void parseStatement(Lexer lexer) {
        if (lexer.currentToken() != 4) {
            throw new IllegalArgumentException("syntax error, expected column name");
        }
        lexer.advance();
        if (lexer.currentToken() == 5) {
            lexer.advance();
            if (lexer.currentToken() != 6) {
                throw new IllegalArgumentException("syntax error, expected quoted string");
            }
            lexer.advance();
            return;
        }
        if (lexer.currentToken() == 7) {
            lexer.advance();
            if (lexer.currentToken() != 8) {
                throw new IllegalArgumentException("syntax error, expected NULL");
            }
            lexer.advance();
            return;
        }
        throw new IllegalArgumentException("syntax error after column name");
    }

    private static class Lexer {
        private final Set<String> mAllowedColumns;
        private final char[] mChars;
        private final String mSelection;
        private int mOffset = 0;
        private int mCurrentToken = 0;

        public Lexer(String selection, Set<String> allowedColumns) {
            this.mSelection = selection;
            this.mAllowedColumns = allowedColumns;
            this.mChars = new char[this.mSelection.length()];
            this.mSelection.getChars(0, this.mChars.length, this.mChars, 0);
            advance();
        }

        public int currentToken() {
            return this.mCurrentToken;
        }

        public void advance() {
            char[] chars = this.mChars;
            while (this.mOffset < chars.length && chars[this.mOffset] == ' ') {
                this.mOffset++;
            }
            if (this.mOffset == chars.length) {
                this.mCurrentToken = 9;
                return;
            }
            if (chars[this.mOffset] == '(') {
                this.mOffset++;
                this.mCurrentToken = 1;
                return;
            }
            if (chars[this.mOffset] == ')') {
                this.mOffset++;
                this.mCurrentToken = 2;
                return;
            }
            if (chars[this.mOffset] == '?') {
                this.mOffset++;
                this.mCurrentToken = 6;
                return;
            }
            if (chars[this.mOffset] == '=') {
                this.mOffset++;
                this.mCurrentToken = 5;
                if (this.mOffset < chars.length && chars[this.mOffset] == '=') {
                    this.mOffset++;
                    return;
                }
                return;
            }
            if (chars[this.mOffset] == '>') {
                this.mOffset++;
                this.mCurrentToken = 5;
                if (this.mOffset < chars.length && chars[this.mOffset] == '=') {
                    this.mOffset++;
                    return;
                }
                return;
            }
            if (chars[this.mOffset] == '<') {
                this.mOffset++;
                this.mCurrentToken = 5;
                if (this.mOffset < chars.length) {
                    if (chars[this.mOffset] == '=' || chars[this.mOffset] == '>') {
                        this.mOffset++;
                        return;
                    }
                    return;
                }
                return;
            }
            if (chars[this.mOffset] == '!') {
                this.mOffset++;
                this.mCurrentToken = 5;
                if (this.mOffset < chars.length && chars[this.mOffset] == '=') {
                    this.mOffset++;
                    return;
                }
                throw new IllegalArgumentException("Unexpected character after !");
            }
            if (isIdentifierStart(chars[this.mOffset])) {
                int startOffset = this.mOffset;
                this.mOffset++;
                while (this.mOffset < chars.length && isIdentifierChar(chars[this.mOffset])) {
                    this.mOffset++;
                }
                String word = this.mSelection.substring(startOffset, this.mOffset);
                if (this.mOffset - startOffset <= 4) {
                    if (word.equals("IS")) {
                        this.mCurrentToken = 7;
                        return;
                    } else if (word.equals("OR") || word.equals("AND")) {
                        this.mCurrentToken = 3;
                        return;
                    } else if (word.equals("NULL")) {
                        this.mCurrentToken = 8;
                        return;
                    }
                }
                if (this.mAllowedColumns.contains(word)) {
                    this.mCurrentToken = 4;
                    return;
                }
                throw new IllegalArgumentException("unrecognized column or keyword");
            }
            if (chars[this.mOffset] == '\'') {
                this.mOffset++;
                while (this.mOffset < chars.length) {
                    if (chars[this.mOffset] == '\'') {
                        if (this.mOffset + 1 >= chars.length || chars[this.mOffset + 1] != '\'') {
                            break;
                        } else {
                            this.mOffset++;
                        }
                    }
                    this.mOffset++;
                }
                if (this.mOffset == chars.length) {
                    throw new IllegalArgumentException("unterminated string");
                }
                this.mOffset++;
                this.mCurrentToken = 6;
                return;
            }
            throw new IllegalArgumentException("illegal character: " + chars[this.mOffset]);
        }

        private static final boolean isIdentifierStart(char c) {
            return c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
        }

        private static final boolean isIdentifierChar(char c) {
            return c == '_' || (c >= 'A' && c <= 'Z') || ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'));
        }
    }
}
