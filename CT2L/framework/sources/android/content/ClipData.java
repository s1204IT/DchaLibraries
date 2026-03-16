package android.content;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.ProxyInfo;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ClipData implements Parcelable {
    final ClipDescription mClipDescription;
    final Bitmap mIcon;
    final ArrayList<Item> mItems;
    static final String[] MIMETYPES_TEXT_PLAIN = {"text/plain"};
    static final String[] MIMETYPES_TEXT_HTML = {ClipDescription.MIMETYPE_TEXT_HTML};
    static final String[] MIMETYPES_TEXT_URILIST = {ClipDescription.MIMETYPE_TEXT_URILIST};
    static final String[] MIMETYPES_TEXT_INTENT = {ClipDescription.MIMETYPE_TEXT_INTENT};
    public static final Parcelable.Creator<ClipData> CREATOR = new Parcelable.Creator<ClipData>() {
        @Override
        public ClipData createFromParcel(Parcel source) {
            return new ClipData(source);
        }

        @Override
        public ClipData[] newArray(int size) {
            return new ClipData[size];
        }
    };

    public static class Item {
        final String mHtmlText;
        final Intent mIntent;
        final CharSequence mText;
        Uri mUri;

        public Item(CharSequence text) {
            this.mText = text;
            this.mHtmlText = null;
            this.mIntent = null;
            this.mUri = null;
        }

        public Item(CharSequence text, String htmlText) {
            this.mText = text;
            this.mHtmlText = htmlText;
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

        public Item(CharSequence text, Intent intent, Uri uri) {
            this.mText = text;
            this.mHtmlText = null;
            this.mIntent = intent;
            this.mUri = uri;
        }

        public Item(CharSequence text, String htmlText, Intent intent, Uri uri) {
            if (htmlText != null && text == null) {
                throw new IllegalArgumentException("Plain text must be supplied if HTML text is supplied");
            }
            this.mText = text;
            this.mHtmlText = htmlText;
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
            CharSequence text = getText();
            if (text != null) {
                return text;
            }
            Uri uri = getUri();
            if (uri == null) {
                Intent intent = getIntent();
                return intent != null ? intent.toUri(1) : ProxyInfo.LOCAL_EXCL_LIST;
            }
            FileInputStream stream = null;
            try {
                try {
                    AssetFileDescriptor descr = context.getContentResolver().openTypedAssetFileDescriptor(uri, "text/*", null);
                    stream = descr.createInputStream();
                    InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
                    StringBuilder builder = new StringBuilder(128);
                    char[] buffer = new char[8192];
                    while (true) {
                        int len = reader.read(buffer);
                        if (len <= 0) {
                            break;
                        }
                        builder.append(buffer, 0, len);
                    }
                    CharSequence text2 = builder.toString();
                    if (stream == null) {
                        return text2;
                    }
                    try {
                        stream.close();
                        return text2;
                    } catch (IOException e) {
                        return text2;
                    }
                } catch (Throwable th) {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e2) {
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e3) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e4) {
                    }
                }
                return uri.toString();
            } catch (IOException e5) {
                Log.w("ClippedData", "Failure loading text", e5);
                CharSequence text3 = e5.toString();
                if (stream == null) {
                    return text3;
                }
                try {
                    stream.close();
                    return text3;
                } catch (IOException e6) {
                    return text3;
                }
            }
        }

        public CharSequence coerceToStyledText(Context context) {
            CharSequence text = getText();
            if (!(text instanceof Spanned)) {
                String htmlText = getHtmlText();
                if (htmlText != null) {
                    try {
                        CharSequence newText = Html.fromHtml(htmlText);
                        if (newText != null) {
                            return newText;
                        }
                    } catch (RuntimeException e) {
                    }
                }
                return text == null ? coerceToHtmlOrStyledText(context, true) : text;
            }
            return text;
        }

        public String coerceToHtmlText(Context context) {
            String htmlText = getHtmlText();
            if (htmlText == null) {
                CharSequence text = getText();
                if (text != null) {
                    if (text instanceof Spanned) {
                        return Html.toHtml((Spanned) text);
                    }
                    return Html.escapeHtml(text);
                }
                CharSequence text2 = coerceToHtmlOrStyledText(context, false);
                return text2 != null ? text2.toString() : null;
            }
            return htmlText;
        }

        private CharSequence coerceToHtmlOrStyledText(Context context, boolean styled) {
            if (this.mUri == null) {
                return this.mIntent != null ? styled ? uriToStyledText(this.mIntent.toUri(1)) : uriToHtml(this.mIntent.toUri(1)) : ProxyInfo.LOCAL_EXCL_LIST;
            }
            String[] types = context.getContentResolver().getStreamTypes(this.mUri, "text/*");
            boolean hasHtml = false;
            boolean hasText = false;
            if (types != null) {
                for (String type : types) {
                    if (ClipDescription.MIMETYPE_TEXT_HTML.equals(type)) {
                        hasHtml = true;
                    } else if (type.startsWith("text/")) {
                        hasText = true;
                    }
                }
            }
            if (hasHtml || hasText) {
                FileInputStream stream = null;
                try {
                    try {
                        AssetFileDescriptor descr = context.getContentResolver().openTypedAssetFileDescriptor(this.mUri, hasHtml ? ClipDescription.MIMETYPE_TEXT_HTML : "text/plain", null);
                        FileInputStream stream2 = descr.createInputStream();
                        InputStreamReader reader = new InputStreamReader(stream2, "UTF-8");
                        StringBuilder builder = new StringBuilder(128);
                        char[] buffer = new char[8192];
                        while (true) {
                            int len = reader.read(buffer);
                            if (len <= 0) {
                                break;
                            }
                            builder.append(buffer, 0, len);
                        }
                        String text = builder.toString();
                        if (!hasHtml) {
                            if (styled) {
                                if (stream2 == null) {
                                    return text;
                                }
                                try {
                                    stream2.close();
                                    return text;
                                } catch (IOException e) {
                                    return text;
                                }
                            }
                            String text2 = Html.escapeHtml(text);
                            if (stream2 == null) {
                                return text2;
                            }
                            try {
                                stream2.close();
                                return text2;
                            } catch (IOException e2) {
                                return text2;
                            }
                        }
                        if (!styled) {
                            String text3 = text.toString();
                            if (stream2 == null) {
                                return text3;
                            }
                            try {
                                stream2.close();
                                return text3;
                            } catch (IOException e3) {
                                return text3;
                            }
                        }
                        try {
                            CharSequence newText = Html.fromHtml(text);
                            if (newText == null) {
                                newText = text;
                            }
                            if (stream2 != null) {
                                try {
                                    stream2.close();
                                } catch (IOException e4) {
                                }
                            }
                            return newText;
                        } catch (RuntimeException e5) {
                            if (stream2 == null) {
                                return text;
                            }
                            try {
                                stream2.close();
                                return text;
                            } catch (IOException e6) {
                                return text;
                            }
                        }
                    } catch (Throwable th) {
                        if (0 != 0) {
                            try {
                                stream.close();
                            } catch (IOException e7) {
                            }
                        }
                        throw th;
                    }
                } catch (FileNotFoundException e8) {
                    if (0 != 0) {
                        try {
                            stream.close();
                        } catch (IOException e9) {
                        }
                    }
                } catch (IOException e10) {
                    Log.w("ClippedData", "Failure loading text", e10);
                    String strEscapeHtml = Html.escapeHtml(e10.toString());
                    if (0 == 0) {
                        return strEscapeHtml;
                    }
                    try {
                        stream.close();
                        return strEscapeHtml;
                    } catch (IOException e11) {
                        return strEscapeHtml;
                    }
                }
            }
            return styled ? uriToStyledText(this.mUri.toString()) : uriToHtml(this.mUri.toString());
        }

        private String uriToHtml(String uri) {
            StringBuilder builder = new StringBuilder(256);
            builder.append("<a href=\"");
            builder.append(Html.escapeHtml(uri));
            builder.append("\">");
            builder.append(Html.escapeHtml(uri));
            builder.append("</a>");
            return builder.toString();
        }

        private CharSequence uriToStyledText(String uri) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append((CharSequence) uri);
            builder.setSpan(new URLSpan(uri), 0, builder.length(), 33);
            return builder;
        }

        public String toString() {
            StringBuilder b = new StringBuilder(128);
            b.append("ClipData.Item { ");
            toShortString(b);
            b.append(" }");
            return b.toString();
        }

        public void toShortString(StringBuilder b) {
            if (this.mHtmlText != null) {
                b.append("H:");
                b.append(this.mHtmlText);
                return;
            }
            if (this.mText != null) {
                b.append("T:");
                b.append(this.mText);
            } else if (this.mUri != null) {
                b.append("U:");
                b.append(this.mUri);
            } else if (this.mIntent != null) {
                b.append("I:");
                this.mIntent.toShortString(b, true, true, true, true);
            } else {
                b.append(WifiEnterpriseConfig.EMPTY_VALUE);
            }
        }
    }

    public ClipData(CharSequence label, String[] mimeTypes, Item item) {
        this.mClipDescription = new ClipDescription(label, mimeTypes);
        if (item == null) {
            throw new NullPointerException("item is null");
        }
        this.mIcon = null;
        this.mItems = new ArrayList<>();
        this.mItems.add(item);
    }

    public ClipData(ClipDescription description, Item item) {
        this.mClipDescription = description;
        if (item == null) {
            throw new NullPointerException("item is null");
        }
        this.mIcon = null;
        this.mItems = new ArrayList<>();
        this.mItems.add(item);
    }

    public ClipData(ClipData other) {
        this.mClipDescription = other.mClipDescription;
        this.mIcon = other.mIcon;
        this.mItems = new ArrayList<>(other.mItems);
    }

    public static ClipData newPlainText(CharSequence label, CharSequence text) {
        Item item = new Item(text);
        return new ClipData(label, MIMETYPES_TEXT_PLAIN, item);
    }

    public static ClipData newHtmlText(CharSequence label, CharSequence text, String htmlText) {
        Item item = new Item(text, htmlText);
        return new ClipData(label, MIMETYPES_TEXT_HTML, item);
    }

    public static ClipData newIntent(CharSequence label, Intent intent) {
        Item item = new Item(intent);
        return new ClipData(label, MIMETYPES_TEXT_INTENT, item);
    }

    public static ClipData newUri(ContentResolver resolver, CharSequence label, Uri uri) {
        Item item = new Item(uri);
        String[] mimeTypes = null;
        if ("content".equals(uri.getScheme())) {
            String realType = resolver.getType(uri);
            mimeTypes = resolver.getStreamTypes(uri, "*/*");
            if (mimeTypes == null) {
                if (realType != null) {
                    mimeTypes = new String[]{realType, ClipDescription.MIMETYPE_TEXT_URILIST};
                }
            } else {
                String[] tmp = new String[(realType == null ? 1 : 2) + mimeTypes.length];
                int i = 0;
                if (realType != null) {
                    tmp[0] = realType;
                    i = 0 + 1;
                }
                System.arraycopy(mimeTypes, 0, tmp, i, mimeTypes.length);
                tmp[mimeTypes.length + i] = ClipDescription.MIMETYPE_TEXT_URILIST;
                mimeTypes = tmp;
            }
        }
        if (mimeTypes == null) {
            mimeTypes = MIMETYPES_TEXT_URILIST;
        }
        return new ClipData(label, mimeTypes, item);
    }

    public static ClipData newRawUri(CharSequence label, Uri uri) {
        Item item = new Item(uri);
        return new ClipData(label, MIMETYPES_TEXT_URILIST, item);
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

    public Bitmap getIcon() {
        return this.mIcon;
    }

    public int getItemCount() {
        return this.mItems.size();
    }

    public Item getItemAt(int index) {
        return this.mItems.get(index);
    }

    public void prepareToLeaveProcess() {
        int size = this.mItems.size();
        for (int i = 0; i < size; i++) {
            Item item = this.mItems.get(i);
            if (item.mIntent != null) {
                item.mIntent.prepareToLeaveProcess();
            }
            if (item.mUri != null && StrictMode.vmFileUriExposureEnabled()) {
                item.mUri.checkFileUriExposed("ClipData.Item.getUri()");
            }
        }
    }

    public void fixUris(int contentUserHint) {
        int size = this.mItems.size();
        for (int i = 0; i < size; i++) {
            Item item = this.mItems.get(i);
            if (item.mIntent != null) {
                item.mIntent.fixUris(contentUserHint);
            }
            if (item.mUri != null) {
                item.mUri = ContentProvider.maybeAddUserId(item.mUri, contentUserHint);
            }
        }
    }

    public void fixUrisLight(int contentUserHint) {
        Uri data;
        int size = this.mItems.size();
        for (int i = 0; i < size; i++) {
            Item item = this.mItems.get(i);
            if (item.mIntent != null && (data = item.mIntent.getData()) != null) {
                item.mIntent.setData(ContentProvider.maybeAddUserId(data, contentUserHint));
            }
            if (item.mUri != null) {
                item.mUri = ContentProvider.maybeAddUserId(item.mUri, contentUserHint);
            }
        }
    }

    public String toString() {
        StringBuilder b = new StringBuilder(128);
        b.append("ClipData { ");
        toShortString(b);
        b.append(" }");
        return b.toString();
    }

    public void toShortString(StringBuilder b) {
        boolean first = this.mClipDescription == null || !this.mClipDescription.toShortString(b);
        if (this.mIcon != null) {
            if (!first) {
                b.append(' ');
            }
            first = false;
            b.append("I:");
            b.append(this.mIcon.getWidth());
            b.append('x');
            b.append(this.mIcon.getHeight());
        }
        for (int i = 0; i < this.mItems.size(); i++) {
            if (!first) {
                b.append(' ');
            }
            first = false;
            b.append('{');
            this.mItems.get(i).toShortString(b);
            b.append('}');
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        this.mClipDescription.writeToParcel(dest, flags);
        if (this.mIcon != null) {
            dest.writeInt(1);
            this.mIcon.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        int N = this.mItems.size();
        dest.writeInt(N);
        for (int i = 0; i < N; i++) {
            Item item = this.mItems.get(i);
            TextUtils.writeToParcel(item.mText, dest, flags);
            dest.writeString(item.mHtmlText);
            if (item.mIntent != null) {
                dest.writeInt(1);
                item.mIntent.writeToParcel(dest, flags);
            } else {
                dest.writeInt(0);
            }
            if (item.mUri != null) {
                dest.writeInt(1);
                item.mUri.writeToParcel(dest, flags);
            } else {
                dest.writeInt(0);
            }
        }
    }

    ClipData(Parcel in) {
        this.mClipDescription = new ClipDescription(in);
        if (in.readInt() != 0) {
            this.mIcon = Bitmap.CREATOR.createFromParcel(in);
        } else {
            this.mIcon = null;
        }
        this.mItems = new ArrayList<>();
        int N = in.readInt();
        for (int i = 0; i < N; i++) {
            CharSequence text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            String htmlText = in.readString();
            Intent intent = in.readInt() != 0 ? Intent.CREATOR.createFromParcel(in) : null;
            Uri uri = in.readInt() != 0 ? Uri.CREATOR.createFromParcel(in) : null;
            this.mItems.add(new Item(text, htmlText, intent, uri));
        }
    }
}
