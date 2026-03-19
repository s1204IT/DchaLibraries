package android.content;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StrictMode;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import com.android.internal.transition.EpicenterTranslateClipReveal;
import com.android.internal.util.ArrayUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;

public class ClipData implements Parcelable {
    final ClipDescription mClipDescription;
    final Bitmap mIcon;
    final ArrayList<Item> mItems;
    static final String[] MIMETYPES_TEXT_PLAIN = {ClipDescription.MIMETYPE_TEXT_PLAIN};
    static final String[] MIMETYPES_TEXT_HTML = {ClipDescription.MIMETYPE_TEXT_HTML};
    static final String[] MIMETYPES_TEXT_URILIST = {ClipDescription.MIMETYPE_TEXT_URILIST};
    static final String[] MIMETYPES_TEXT_INTENT = {ClipDescription.MIMETYPE_TEXT_INTENT};
    public static final Parcelable.Creator<ClipData> CREATOR = new Parcelable.Creator<ClipData>() {
        @Override
        public ClipData createFromParcel(Parcel parcel) {
            return new ClipData(parcel);
        }

        @Override
        public ClipData[] newArray(int i) {
            return new ClipData[i];
        }
    };

    public static class Item {
        final String mHtmlText;
        final Intent mIntent;
        final CharSequence mText;
        Uri mUri;

        public Item(Item item) {
            this.mText = item.mText;
            this.mHtmlText = item.mHtmlText;
            this.mIntent = item.mIntent;
            this.mUri = item.mUri;
        }

        public Item(CharSequence charSequence) {
            this.mText = charSequence;
            this.mHtmlText = null;
            this.mIntent = null;
            this.mUri = null;
        }

        public Item(CharSequence charSequence, String str) {
            this.mText = charSequence;
            this.mHtmlText = str;
            this.mIntent = null;
            this.mUri = null;
        }

        public Item(Intent intent) {
            this.mText = null;
            this.mHtmlText = null;
            this.mIntent = intent;
            this.mUri = null;
        }

        public Item(Uri uri) {
            this.mText = null;
            this.mHtmlText = null;
            this.mIntent = null;
            this.mUri = uri;
        }

        public Item(CharSequence charSequence, Intent intent, Uri uri) {
            this.mText = charSequence;
            this.mHtmlText = null;
            this.mIntent = intent;
            this.mUri = uri;
        }

        public Item(CharSequence charSequence, String str, Intent intent, Uri uri) {
            if (str != null && charSequence == null) {
                throw new IllegalArgumentException("Plain text must be supplied if HTML text is supplied");
            }
            this.mText = charSequence;
            this.mHtmlText = str;
            this.mIntent = intent;
            this.mUri = uri;
        }

        public CharSequence getText() {
            return this.mText;
        }

        public String getHtmlText() {
            return this.mHtmlText;
        }

        public Intent getIntent() {
            return this.mIntent;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public CharSequence coerceToText(Context context) {
            AssetFileDescriptor assetFileDescriptorOpenTypedAssetFileDescriptor;
            FileInputStream fileInputStreamCreateInputStream;
            InputStreamReader inputStreamReader;
            Throwable th;
            IOException e;
            StringBuilder sb;
            char[] cArr;
            CharSequence text = getText();
            if (text != null) {
                return text;
            }
            Uri uri = getUri();
            if (uri == null) {
                Intent intent = getIntent();
                return intent != null ? intent.toUri(1) : "";
            }
            try {
                try {
                    assetFileDescriptorOpenTypedAssetFileDescriptor = context.getContentResolver().openTypedAssetFileDescriptor(uri, "text/*", null);
                } catch (Throwable th2) {
                    fileInputStreamCreateInputStream = null;
                    inputStreamReader = null;
                    th = th2;
                    assetFileDescriptorOpenTypedAssetFileDescriptor = null;
                }
            } catch (FileNotFoundException | RuntimeException e2) {
                assetFileDescriptorOpenTypedAssetFileDescriptor = null;
            } catch (SecurityException e3) {
                Log.w("ClipData", "Failure opening stream", e3);
                assetFileDescriptorOpenTypedAssetFileDescriptor = null;
            }
            if (assetFileDescriptorOpenTypedAssetFileDescriptor == null) {
                IoUtils.closeQuietly(assetFileDescriptorOpenTypedAssetFileDescriptor);
                IoUtils.closeQuietly((AutoCloseable) null);
                IoUtils.closeQuietly((AutoCloseable) null);
                String scheme = uri.getScheme();
                return ("content".equals(scheme) || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme) || ContentResolver.SCHEME_FILE.equals(scheme)) ? "" : uri.toString();
            }
            try {
                fileInputStreamCreateInputStream = assetFileDescriptorOpenTypedAssetFileDescriptor.createInputStream();
                try {
                    inputStreamReader = new InputStreamReader(fileInputStreamCreateInputStream, "UTF-8");
                    try {
                        try {
                            sb = new StringBuilder(128);
                            cArr = new char[8192];
                        } catch (IOException e4) {
                            e = e4;
                            Log.w("ClipData", "Failure loading text", e);
                            String string = e.toString();
                            IoUtils.closeQuietly(assetFileDescriptorOpenTypedAssetFileDescriptor);
                            IoUtils.closeQuietly(fileInputStreamCreateInputStream);
                            IoUtils.closeQuietly(inputStreamReader);
                            return string;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } catch (IOException e5) {
                    inputStreamReader = null;
                    e = e5;
                } catch (Throwable th4) {
                    inputStreamReader = null;
                    th = th4;
                }
            } catch (IOException e6) {
                inputStreamReader = null;
                e = e6;
                fileInputStreamCreateInputStream = null;
            } catch (Throwable th5) {
                inputStreamReader = null;
                th = th5;
                fileInputStreamCreateInputStream = null;
            }
            while (true) {
                int i = inputStreamReader.read(cArr);
                if (i <= 0) {
                    String string2 = sb.toString();
                    IoUtils.closeQuietly(assetFileDescriptorOpenTypedAssetFileDescriptor);
                    IoUtils.closeQuietly(fileInputStreamCreateInputStream);
                    IoUtils.closeQuietly(inputStreamReader);
                    return string2;
                }
                sb.append(cArr, 0, i);
                th = th3;
                IoUtils.closeQuietly(assetFileDescriptorOpenTypedAssetFileDescriptor);
                IoUtils.closeQuietly(fileInputStreamCreateInputStream);
                IoUtils.closeQuietly(inputStreamReader);
                throw th;
            }
        }

        public CharSequence coerceToStyledText(Context context) {
            CharSequence text = getText();
            if (text instanceof Spanned) {
                return text;
            }
            String htmlText = getHtmlText();
            if (htmlText != null) {
                try {
                    Spanned spannedFromHtml = Html.fromHtml(htmlText);
                    if (spannedFromHtml != null) {
                        return spannedFromHtml;
                    }
                } catch (RuntimeException e) {
                }
            }
            if (text != null) {
                return text;
            }
            return coerceToHtmlOrStyledText(context, true);
        }

        public String coerceToHtmlText(Context context) throws Throwable {
            String htmlText = getHtmlText();
            if (htmlText != null) {
                return htmlText;
            }
            CharSequence text = getText();
            if (text != null) {
                if (text instanceof Spanned) {
                    return Html.toHtml((Spanned) text);
                }
                return Html.escapeHtml(text);
            }
            CharSequence charSequenceCoerceToHtmlOrStyledText = coerceToHtmlOrStyledText(context, false);
            if (charSequenceCoerceToHtmlOrStyledText != null) {
                return charSequenceCoerceToHtmlOrStyledText.toString();
            }
            return null;
        }

        private CharSequence coerceToHtmlOrStyledText(Context context, boolean z) throws Throwable {
            String[] streamTypes;
            boolean z2;
            boolean z3;
            SecurityException e;
            FileInputStream fileInputStreamCreateInputStream;
            IOException e2;
            if (this.mUri == null) {
                return this.mIntent != null ? z ? uriToStyledText(this.mIntent.toUri(1)) : uriToHtml(this.mIntent.toUri(1)) : "";
            }
            FileInputStream fileInputStream = null;
            try {
                streamTypes = context.getContentResolver().getStreamTypes(this.mUri, "text/*");
            } catch (SecurityException e3) {
                streamTypes = null;
            }
            if (streamTypes != null) {
                z2 = false;
                z3 = false;
                for (String str : streamTypes) {
                    if (ClipDescription.MIMETYPE_TEXT_HTML.equals(str)) {
                        z2 = true;
                    } else if (str.startsWith("text/")) {
                        z3 = true;
                    }
                }
            } else {
                z2 = false;
                z3 = false;
            }
            if (z2 || z3) {
                try {
                    try {
                        try {
                            try {
                                fileInputStreamCreateInputStream = context.getContentResolver().openTypedAssetFileDescriptor(this.mUri, z2 ? ClipDescription.MIMETYPE_TEXT_HTML : ClipDescription.MIMETYPE_TEXT_PLAIN, null).createInputStream();
                            } catch (Throwable th) {
                                th = th;
                                context = 0;
                                if (context != 0) {
                                    try {
                                        context.close();
                                    } catch (IOException e4) {
                                    }
                                }
                                throw th;
                            }
                        } catch (IOException e5) {
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (context != 0) {
                        }
                        throw th;
                    }
                } catch (FileNotFoundException e6) {
                    fileInputStreamCreateInputStream = null;
                } catch (IOException e7) {
                    e2 = e7;
                } catch (SecurityException e8) {
                    e = e8;
                    fileInputStreamCreateInputStream = null;
                }
                try {
                    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStreamCreateInputStream, "UTF-8");
                    StringBuilder sb = new StringBuilder(128);
                    char[] cArr = new char[8192];
                    while (true) {
                        int i = inputStreamReader.read(cArr);
                        if (i <= 0) {
                            break;
                        }
                        sb.append(cArr, 0, i);
                    }
                    String string = sb.toString();
                    if (!z2) {
                        if (z) {
                            if (fileInputStreamCreateInputStream != null) {
                                try {
                                    fileInputStreamCreateInputStream.close();
                                } catch (IOException e9) {
                                }
                            }
                            return string;
                        }
                        String strEscapeHtml = Html.escapeHtml(string);
                        if (fileInputStreamCreateInputStream != null) {
                            try {
                                fileInputStreamCreateInputStream.close();
                            } catch (IOException e10) {
                            }
                        }
                        return strEscapeHtml;
                    }
                    if (!z) {
                        String string2 = string.toString();
                        if (fileInputStreamCreateInputStream != null) {
                            try {
                                fileInputStreamCreateInputStream.close();
                            } catch (IOException e11) {
                            }
                        }
                        return string2;
                    }
                    try {
                        Spanned spannedFromHtml = Html.fromHtml(string);
                        CharSequence charSequence = string;
                        if (spannedFromHtml != null) {
                            charSequence = spannedFromHtml;
                        }
                        if (fileInputStreamCreateInputStream != null) {
                            try {
                                fileInputStreamCreateInputStream.close();
                            } catch (IOException e12) {
                            }
                        }
                        return charSequence;
                    } catch (RuntimeException e13) {
                        if (fileInputStreamCreateInputStream != null) {
                            try {
                                fileInputStreamCreateInputStream.close();
                            } catch (IOException e14) {
                            }
                        }
                        return string;
                    }
                } catch (FileNotFoundException e15) {
                    if (fileInputStreamCreateInputStream != null) {
                        fileInputStreamCreateInputStream.close();
                    }
                    String scheme = this.mUri.getScheme();
                    return ("content".equals(scheme) || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme) || ContentResolver.SCHEME_FILE.equals(scheme)) ? "" : z ? uriToStyledText(this.mUri.toString()) : uriToHtml(this.mUri.toString());
                } catch (IOException e16) {
                    e2 = e16;
                    fileInputStream = fileInputStreamCreateInputStream;
                    Log.w("ClipData", "Failure loading text", e2);
                    String strEscapeHtml2 = Html.escapeHtml(e2.toString());
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e17) {
                        }
                    }
                    return strEscapeHtml2;
                } catch (SecurityException e18) {
                    e = e18;
                    Log.w("ClipData", "Failure opening stream", e);
                    if (fileInputStreamCreateInputStream != null) {
                        fileInputStreamCreateInputStream.close();
                    }
                    String scheme2 = this.mUri.getScheme();
                    if ("content".equals(scheme2)) {
                    }
                }
            }
            String scheme22 = this.mUri.getScheme();
            return ("content".equals(scheme22) || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme22) || ContentResolver.SCHEME_FILE.equals(scheme22)) ? "" : z ? uriToStyledText(this.mUri.toString()) : uriToHtml(this.mUri.toString());
        }

        private String uriToHtml(String str) {
            StringBuilder sb = new StringBuilder(256);
            sb.append("<a href=\"");
            sb.append(Html.escapeHtml(str));
            sb.append("\">");
            sb.append(Html.escapeHtml(str));
            sb.append("</a>");
            return sb.toString();
        }

        private CharSequence uriToStyledText(String str) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            spannableStringBuilder.append((CharSequence) str);
            spannableStringBuilder.setSpan(new URLSpan(str), 0, spannableStringBuilder.length(), 33);
            return spannableStringBuilder;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ClipData.Item { ");
            toShortString(sb);
            sb.append(" }");
            return sb.toString();
        }

        public void toShortString(StringBuilder sb) {
            if (this.mHtmlText != null) {
                sb.append("H:");
                sb.append(this.mHtmlText);
                return;
            }
            if (this.mText != null) {
                sb.append("T:");
                sb.append(this.mText);
            } else if (this.mUri != null) {
                sb.append("U:");
                sb.append(this.mUri);
            } else if (this.mIntent != null) {
                sb.append("I:");
                this.mIntent.toShortString(sb, true, true, true, true);
            } else {
                sb.append(WifiEnterpriseConfig.EMPTY_VALUE);
            }
        }

        public void toShortSummaryString(StringBuilder sb) {
            if (this.mHtmlText != null) {
                sb.append("HTML");
                return;
            }
            if (this.mText != null) {
                sb.append("TEXT");
                return;
            }
            if (this.mUri != null) {
                sb.append("U:");
                sb.append(this.mUri);
            } else if (this.mIntent != null) {
                sb.append("I:");
                this.mIntent.toShortString(sb, true, true, true, true);
            } else {
                sb.append(WifiEnterpriseConfig.EMPTY_VALUE);
            }
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            if (this.mHtmlText != null) {
                protoOutputStream.write(1138166333441L, this.mHtmlText);
            } else if (this.mText != null) {
                protoOutputStream.write(1138166333442L, this.mText.toString());
            } else if (this.mUri != null) {
                protoOutputStream.write(1138166333443L, this.mUri.toString());
            } else if (this.mIntent != null) {
                this.mIntent.writeToProto(protoOutputStream, 1146756268036L, true, true, true, true);
            } else {
                protoOutputStream.write(1133871366149L, true);
            }
            protoOutputStream.end(jStart);
        }
    }

    public ClipData(CharSequence charSequence, String[] strArr, Item item) {
        this.mClipDescription = new ClipDescription(charSequence, strArr);
        if (item == null) {
            throw new NullPointerException("item is null");
        }
        this.mIcon = null;
        this.mItems = new ArrayList<>();
        this.mItems.add(item);
    }

    public ClipData(ClipDescription clipDescription, Item item) {
        this.mClipDescription = clipDescription;
        if (item == null) {
            throw new NullPointerException("item is null");
        }
        this.mIcon = null;
        this.mItems = new ArrayList<>();
        this.mItems.add(item);
    }

    public ClipData(ClipDescription clipDescription, ArrayList<Item> arrayList) {
        this.mClipDescription = clipDescription;
        if (arrayList == null) {
            throw new NullPointerException("item is null");
        }
        this.mIcon = null;
        this.mItems = arrayList;
    }

    public ClipData(ClipData clipData) {
        this.mClipDescription = clipData.mClipDescription;
        this.mIcon = clipData.mIcon;
        this.mItems = new ArrayList<>(clipData.mItems);
    }

    public static ClipData newPlainText(CharSequence charSequence, CharSequence charSequence2) {
        return new ClipData(charSequence, MIMETYPES_TEXT_PLAIN, new Item(charSequence2));
    }

    public static ClipData newHtmlText(CharSequence charSequence, CharSequence charSequence2, String str) {
        return new ClipData(charSequence, MIMETYPES_TEXT_HTML, new Item(charSequence2, str));
    }

    public static ClipData newIntent(CharSequence charSequence, Intent intent) {
        return new ClipData(charSequence, MIMETYPES_TEXT_INTENT, new Item(intent));
    }

    public static ClipData newUri(ContentResolver contentResolver, CharSequence charSequence, Uri uri) {
        return new ClipData(charSequence, getMimeTypes(contentResolver, uri), new Item(uri));
    }

    private static String[] getMimeTypes(ContentResolver contentResolver, Uri uri) {
        String[] streamTypes;
        if ("content".equals(uri.getScheme())) {
            String type = contentResolver.getType(uri);
            streamTypes = contentResolver.getStreamTypes(uri, "*/*");
            if (type != null) {
                if (streamTypes == null) {
                    streamTypes = new String[]{type};
                } else if (!ArrayUtils.contains(streamTypes, type)) {
                    String[] strArr = new String[streamTypes.length + 1];
                    strArr[0] = type;
                    System.arraycopy(streamTypes, 0, strArr, 1, streamTypes.length);
                    streamTypes = strArr;
                }
            }
        } else {
            streamTypes = null;
        }
        if (streamTypes == null) {
            return MIMETYPES_TEXT_URILIST;
        }
        return streamTypes;
    }

    public static ClipData newRawUri(CharSequence charSequence, Uri uri) {
        return new ClipData(charSequence, MIMETYPES_TEXT_URILIST, new Item(uri));
    }

    public ClipDescription getDescription() {
        return this.mClipDescription;
    }

    public void addItem(Item item) {
        if (item == null) {
            throw new NullPointerException("item is null");
        }
        this.mItems.add(item);
    }

    @Deprecated
    public void addItem(Item item, ContentResolver contentResolver) {
        addItem(contentResolver, item);
    }

    public void addItem(ContentResolver contentResolver, Item item) {
        addItem(item);
        if (item.getHtmlText() != null) {
            this.mClipDescription.addMimeTypes(MIMETYPES_TEXT_HTML);
        } else if (item.getText() != null) {
            this.mClipDescription.addMimeTypes(MIMETYPES_TEXT_PLAIN);
        }
        if (item.getIntent() != null) {
            this.mClipDescription.addMimeTypes(MIMETYPES_TEXT_INTENT);
        }
        if (item.getUri() != null) {
            this.mClipDescription.addMimeTypes(getMimeTypes(contentResolver, item.getUri()));
        }
    }

    public Bitmap getIcon() {
        return this.mIcon;
    }

    public int getItemCount() {
        return this.mItems.size();
    }

    public Item getItemAt(int i) {
        return this.mItems.get(i);
    }

    public void setItemAt(int i, Item item) {
        this.mItems.set(i, item);
    }

    public void prepareToLeaveProcess(boolean z) {
        prepareToLeaveProcess(z, 1);
    }

    public void prepareToLeaveProcess(boolean z, int i) {
        int size = this.mItems.size();
        for (int i2 = 0; i2 < size; i2++) {
            Item item = this.mItems.get(i2);
            if (item.mIntent != null) {
                item.mIntent.prepareToLeaveProcess(z);
            }
            if (item.mUri != null && z) {
                if (StrictMode.vmFileUriExposureEnabled()) {
                    item.mUri.checkFileUriExposed("ClipData.Item.getUri()");
                }
                if (StrictMode.vmContentUriWithoutPermissionEnabled()) {
                    item.mUri.checkContentUriWithoutPermission("ClipData.Item.getUri()", i);
                }
            }
        }
    }

    public void prepareToEnterProcess() {
        int size = this.mItems.size();
        for (int i = 0; i < size; i++) {
            Item item = this.mItems.get(i);
            if (item.mIntent != null) {
                item.mIntent.prepareToEnterProcess();
            }
        }
    }

    public void fixUris(int i) {
        int size = this.mItems.size();
        for (int i2 = 0; i2 < size; i2++) {
            Item item = this.mItems.get(i2);
            if (item.mIntent != null) {
                item.mIntent.fixUris(i);
            }
            if (item.mUri != null) {
                item.mUri = ContentProvider.maybeAddUserId(item.mUri, i);
            }
        }
    }

    public void fixUrisLight(int i) {
        Uri data;
        int size = this.mItems.size();
        for (int i2 = 0; i2 < size; i2++) {
            Item item = this.mItems.get(i2);
            if (item.mIntent != null && (data = item.mIntent.getData()) != null) {
                item.mIntent.setData(ContentProvider.maybeAddUserId(data, i));
            }
            if (item.mUri != null) {
                item.mUri = ContentProvider.maybeAddUserId(item.mUri, i);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("ClipData { ");
        toShortString(sb);
        sb.append(" }");
        return sb.toString();
    }

    public void toShortString(StringBuilder sb) {
        boolean shortString = this.mClipDescription != null ? true ^ this.mClipDescription.toShortString(sb) : true;
        if (this.mIcon != null) {
            if (!shortString) {
                sb.append(' ');
            }
            sb.append("I:");
            sb.append(this.mIcon.getWidth());
            sb.append(EpicenterTranslateClipReveal.StateProperty.TARGET_X);
            sb.append(this.mIcon.getHeight());
            shortString = false;
        }
        int i = 0;
        while (i < this.mItems.size()) {
            if (!shortString) {
                sb.append(' ');
            }
            sb.append('{');
            this.mItems.get(i).toShortString(sb);
            sb.append('}');
            i++;
            shortString = false;
        }
    }

    public void toShortStringShortItems(StringBuilder sb, boolean z) {
        if (this.mItems.size() > 0) {
            if (!z) {
                sb.append(' ');
            }
            this.mItems.get(0).toShortString(sb);
            if (this.mItems.size() > 1) {
                sb.append(" ...");
            }
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        if (this.mClipDescription != null) {
            this.mClipDescription.writeToProto(protoOutputStream, 1146756268033L);
        }
        if (this.mIcon != null) {
            long jStart2 = protoOutputStream.start(1146756268034L);
            protoOutputStream.write(1120986464257L, this.mIcon.getWidth());
            protoOutputStream.write(1120986464258L, this.mIcon.getHeight());
            protoOutputStream.end(jStart2);
        }
        for (int i = 0; i < this.mItems.size(); i++) {
            this.mItems.get(i).writeToProto(protoOutputStream, 2246267895811L);
        }
        protoOutputStream.end(jStart);
    }

    public void collectUris(List<Uri> list) {
        for (int i = 0; i < this.mItems.size(); i++) {
            Item itemAt = getItemAt(i);
            if (itemAt.getUri() != null) {
                list.add(itemAt.getUri());
            }
            Intent intent = itemAt.getIntent();
            if (intent != null) {
                if (intent.getData() != null) {
                    list.add(intent.getData());
                }
                if (intent.getClipData() != null) {
                    intent.getClipData().collectUris(list);
                }
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.mClipDescription.writeToParcel(parcel, i);
        if (this.mIcon != null) {
            parcel.writeInt(1);
            this.mIcon.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        int size = this.mItems.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            Item item = this.mItems.get(i2);
            TextUtils.writeToParcel(item.mText, parcel, i);
            parcel.writeString(item.mHtmlText);
            if (item.mIntent != null) {
                parcel.writeInt(1);
                item.mIntent.writeToParcel(parcel, i);
            } else {
                parcel.writeInt(0);
            }
            if (item.mUri != null) {
                parcel.writeInt(1);
                item.mUri.writeToParcel(parcel, i);
            } else {
                parcel.writeInt(0);
            }
        }
    }

    ClipData(Parcel parcel) {
        this.mClipDescription = new ClipDescription(parcel);
        if (parcel.readInt() != 0) {
            this.mIcon = Bitmap.CREATOR.createFromParcel(parcel);
        } else {
            this.mIcon = null;
        }
        this.mItems = new ArrayList<>();
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            this.mItems.add(new Item(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel), parcel.readString(), parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0 ? Uri.CREATOR.createFromParcel(parcel) : null));
        }
    }
}
