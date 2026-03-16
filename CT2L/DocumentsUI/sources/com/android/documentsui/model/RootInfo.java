package com.android.documentsui.model;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.android.documentsui.IconUtils;
import com.android.documentsui.R;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Objects;

public class RootInfo implements Parcelable, Durable {
    public static final Parcelable.Creator<RootInfo> CREATOR = new Parcelable.Creator<RootInfo>() {
        @Override
        public RootInfo createFromParcel(Parcel in) {
            RootInfo root = new RootInfo();
            DurableUtils.readFromParcel(in, root);
            return root;
        }

        @Override
        public RootInfo[] newArray(int size) {
            return new RootInfo[size];
        }
    };
    public String authority;
    public long availableBytes;
    public int derivedIcon;
    public String[] derivedMimeTypes;
    public String derivedPackageName;
    public String documentId;
    public int flags;
    public int icon;
    public String mimeTypes;
    public String rootId;
    public String summary;
    public String title;

    public RootInfo() {
        reset();
    }

    @Override
    public void reset() {
        this.authority = null;
        this.rootId = null;
        this.flags = 0;
        this.icon = 0;
        this.title = null;
        this.summary = null;
        this.documentId = null;
        this.availableBytes = -1L;
        this.mimeTypes = null;
        this.derivedPackageName = null;
        this.derivedMimeTypes = null;
        this.derivedIcon = 0;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        int version = in.readInt();
        switch (version) {
            case 2:
                this.authority = DurableUtils.readNullableString(in);
                this.rootId = DurableUtils.readNullableString(in);
                this.flags = in.readInt();
                this.icon = in.readInt();
                this.title = DurableUtils.readNullableString(in);
                this.summary = DurableUtils.readNullableString(in);
                this.documentId = DurableUtils.readNullableString(in);
                this.availableBytes = in.readLong();
                this.mimeTypes = DurableUtils.readNullableString(in);
                deriveFields();
                return;
            default:
                throw new ProtocolException("Unknown version " + version);
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(2);
        DurableUtils.writeNullableString(out, this.authority);
        DurableUtils.writeNullableString(out, this.rootId);
        out.writeInt(this.flags);
        out.writeInt(this.icon);
        DurableUtils.writeNullableString(out, this.title);
        DurableUtils.writeNullableString(out, this.summary);
        DurableUtils.writeNullableString(out, this.documentId);
        out.writeLong(this.availableBytes);
        DurableUtils.writeNullableString(out, this.mimeTypes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        DurableUtils.writeToParcel(dest, this);
    }

    public static RootInfo fromRootsCursor(String authority, Cursor cursor) {
        RootInfo root = new RootInfo();
        root.authority = authority;
        root.rootId = DocumentInfo.getCursorString(cursor, "root_id");
        root.flags = DocumentInfo.getCursorInt(cursor, "flags");
        root.icon = DocumentInfo.getCursorInt(cursor, "icon");
        root.title = DocumentInfo.getCursorString(cursor, "title");
        root.summary = DocumentInfo.getCursorString(cursor, "summary");
        root.documentId = DocumentInfo.getCursorString(cursor, "document_id");
        root.availableBytes = DocumentInfo.getCursorLong(cursor, "available_bytes");
        root.mimeTypes = DocumentInfo.getCursorString(cursor, "mime_types");
        root.deriveFields();
        return root;
    }

    private void deriveFields() {
        this.derivedMimeTypes = this.mimeTypes != null ? this.mimeTypes.split("\n") : null;
        if (isExternalStorage()) {
            this.derivedIcon = R.drawable.ic_root_sdcard;
            return;
        }
        if (isDownloads()) {
            this.derivedIcon = R.drawable.ic_root_download;
            return;
        }
        if (isImages()) {
            this.derivedIcon = R.drawable.ic_doc_image;
        } else if (isVideos()) {
            this.derivedIcon = R.drawable.ic_doc_video;
        } else if (isAudio()) {
            this.derivedIcon = R.drawable.ic_doc_audio;
        }
    }

    public boolean isRecents() {
        return this.authority == null && this.rootId == null;
    }

    public boolean isExternalStorage() {
        return "com.android.externalstorage.documents".equals(this.authority);
    }

    public boolean isDownloads() {
        return "com.android.providers.downloads.documents".equals(this.authority);
    }

    public boolean isImages() {
        return "com.android.providers.media.documents".equals(this.authority) && "images_root".equals(this.rootId);
    }

    public boolean isVideos() {
        return "com.android.providers.media.documents".equals(this.authority) && "videos_root".equals(this.rootId);
    }

    public boolean isAudio() {
        return "com.android.providers.media.documents".equals(this.authority) && "audio_root".equals(this.rootId);
    }

    public String toString() {
        return "Root{authority=" + this.authority + ", rootId=" + this.rootId + ", title=" + this.title + "}";
    }

    public Drawable loadIcon(Context context) {
        return this.derivedIcon != 0 ? context.getDrawable(this.derivedIcon) : IconUtils.loadPackageIcon(context, this.authority, this.icon);
    }

    public Drawable loadDrawerIcon(Context context) {
        return this.derivedIcon != 0 ? IconUtils.applyTintColor(context, this.derivedIcon, R.color.item_root_icon) : IconUtils.loadPackageIcon(context, this.authority, this.icon);
    }

    public Drawable loadGridIcon(Context context) {
        return this.derivedIcon != 0 ? IconUtils.applyTintAttr(context, this.derivedIcon, android.R.attr.textColorPrimaryInverse) : IconUtils.loadPackageIcon(context, this.authority, this.icon);
    }

    public Drawable loadToolbarIcon(Context context) {
        return this.derivedIcon != 0 ? IconUtils.applyTintAttr(context, this.derivedIcon, android.R.attr.colorControlNormal) : IconUtils.loadPackageIcon(context, this.authority, this.icon);
    }

    public boolean equals(Object o) {
        if (!(o instanceof RootInfo)) {
            return false;
        }
        RootInfo root = (RootInfo) o;
        return Objects.equals(this.authority, root.authority) && Objects.equals(this.rootId, root.rootId);
    }

    public int hashCode() {
        return Objects.hash(this.authority, this.rootId);
    }

    public String getDirectoryString() {
        return !TextUtils.isEmpty(this.summary) ? this.summary : this.title;
    }
}
