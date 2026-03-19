package com.android.documentsui.base;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;
import com.android.documentsui.IconUtils;
import com.android.documentsui.R;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Objects;

public class RootInfo implements Parcelable, Durable, Comparable<RootInfo> {
    public static final Parcelable.Creator<RootInfo> CREATOR = new Parcelable.Creator<RootInfo>() {
        @Override
        public RootInfo createFromParcel(Parcel parcel) {
            RootInfo rootInfo = new RootInfo();
            DurableUtils.readFromParcel(parcel, rootInfo);
            return rootInfo;
        }

        @Override
        public RootInfo[] newArray(int i) {
            return new RootInfo[i];
        }
    };
    public String authority;
    public long availableBytes;
    public int derivedIcon;
    public String[] derivedMimeTypes;
    public int derivedType;
    public String documentId;
    public transient boolean ejecting;
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
        this.ejecting = false;
        this.derivedMimeTypes = null;
        this.derivedIcon = 0;
        this.derivedType = 0;
    }

    @Override
    public void read(DataInputStream dataInputStream) throws IOException {
        int i = dataInputStream.readInt();
        if (i == 2) {
            this.authority = DurableUtils.readNullableString(dataInputStream);
            this.rootId = DurableUtils.readNullableString(dataInputStream);
            this.flags = dataInputStream.readInt();
            this.icon = dataInputStream.readInt();
            this.title = DurableUtils.readNullableString(dataInputStream);
            this.summary = DurableUtils.readNullableString(dataInputStream);
            this.documentId = DurableUtils.readNullableString(dataInputStream);
            this.availableBytes = dataInputStream.readLong();
            this.mimeTypes = DurableUtils.readNullableString(dataInputStream);
            deriveFields();
            return;
        }
        throw new ProtocolException("Unknown version " + i);
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(2);
        DurableUtils.writeNullableString(dataOutputStream, this.authority);
        DurableUtils.writeNullableString(dataOutputStream, this.rootId);
        dataOutputStream.writeInt(this.flags);
        dataOutputStream.writeInt(this.icon);
        DurableUtils.writeNullableString(dataOutputStream, this.title);
        DurableUtils.writeNullableString(dataOutputStream, this.summary);
        DurableUtils.writeNullableString(dataOutputStream, this.documentId);
        dataOutputStream.writeLong(this.availableBytes);
        DurableUtils.writeNullableString(dataOutputStream, this.mimeTypes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        DurableUtils.writeToParcel(parcel, this);
    }

    public static RootInfo fromRootsCursor(String str, Cursor cursor) {
        RootInfo rootInfo = new RootInfo();
        rootInfo.authority = str;
        rootInfo.rootId = DocumentInfo.getCursorString(cursor, "root_id");
        rootInfo.flags = DocumentInfo.getCursorInt(cursor, "flags");
        rootInfo.icon = DocumentInfo.getCursorInt(cursor, "icon");
        rootInfo.title = DocumentInfo.getCursorString(cursor, "title");
        rootInfo.summary = DocumentInfo.getCursorString(cursor, "summary");
        rootInfo.documentId = DocumentInfo.getCursorString(cursor, "document_id");
        rootInfo.availableBytes = DocumentInfo.getCursorLong(cursor, "available_bytes");
        rootInfo.mimeTypes = DocumentInfo.getCursorString(cursor, "mime_types");
        rootInfo.deriveFields();
        return rootInfo;
    }

    private void deriveFields() {
        this.derivedMimeTypes = this.mimeTypes != null ? this.mimeTypes.split("\n") : null;
        if (isHome()) {
            this.derivedType = 6;
            this.derivedIcon = R.drawable.ic_root_documents;
        } else if (isMtp()) {
            this.derivedType = 7;
            this.derivedIcon = R.drawable.ic_usb_storage;
        } else if (isUsb()) {
            this.derivedType = 9;
            this.derivedIcon = R.drawable.ic_usb_storage;
        } else if (isSd()) {
            this.derivedType = 8;
            this.derivedIcon = R.drawable.ic_sd_storage;
        } else if (isExternalStorage()) {
            this.derivedType = 6;
            this.derivedIcon = R.drawable.ic_root_smartphone;
        } else if (isDownloads()) {
            this.derivedType = 5;
            this.derivedIcon = R.drawable.ic_root_download;
        } else if (isImages()) {
            this.derivedType = 1;
            this.derivedIcon = R.drawable.image_root_icon;
        } else if (isVideos()) {
            this.derivedType = 2;
            this.derivedIcon = R.drawable.video_root_icon;
        } else if (isAudio()) {
            this.derivedType = 3;
            this.derivedIcon = R.drawable.audio_root_icon;
        } else if (isRecents()) {
            this.derivedType = 4;
        } else {
            this.derivedType = 10;
        }
        if (SharedMinimal.VERBOSE) {
            Log.v("RootInfo", "Derived fields: " + this);
        }
    }

    public Uri getUri() {
        return DocumentsContract.buildRootUri(this.authority, this.rootId);
    }

    public boolean isRecents() {
        return this.authority == null && this.rootId == null;
    }

    public boolean isHome() {
        return isExternalStorage() && "home".equals(this.rootId);
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

    public boolean isMtp() {
        return "com.android.mtp.documents".equals(this.authority);
    }

    public boolean isLibrary() {
        return this.derivedType == 1 || this.derivedType == 2 || this.derivedType == 3 || this.derivedType == 4;
    }

    public boolean hasSettings() {
        return (this.flags & 262144) != 0;
    }

    public boolean supportsChildren() {
        return (this.flags & 16) != 0;
    }

    public boolean supportsCreate() {
        return (this.flags & 1) != 0;
    }

    public boolean supportsRecents() {
        return (this.flags & 4) != 0;
    }

    public boolean supportsEject() {
        return (this.flags & 32) != 0;
    }

    public boolean isAdvanced() {
        return (this.flags & 131072) != 0;
    }

    public boolean isLocalOnly() {
        return (this.flags & 2) != 0;
    }

    public boolean isEmpty() {
        return (this.flags & 65536) != 0;
    }

    public boolean isSd() {
        return (this.flags & 524288) != 0;
    }

    public boolean isUsb() {
        return (this.flags & 1048576) != 0;
    }

    public Drawable loadDrawerIcon(Context context) {
        if (this.derivedIcon != 0) {
            return IconUtils.applyTintColor(context, this.derivedIcon, R.color.item_root_icon);
        }
        return IconUtils.loadPackageIcon(context, this.authority, this.icon);
    }

    public Drawable loadEjectIcon(Context context) {
        return IconUtils.applyTintColor(context, R.drawable.ic_eject, R.color.item_eject_icon);
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RootInfo)) {
            return false;
        }
        RootInfo rootInfo = (RootInfo) obj;
        if (!Objects.equals(this.authority, rootInfo.authority) || !Objects.equals(this.rootId, rootInfo.rootId)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return Objects.hash(this.authority, this.rootId);
    }

    @Override
    public int compareTo(RootInfo rootInfo) {
        int i = this.derivedType - rootInfo.derivedType;
        if (i != 0) {
            return i;
        }
        int iCompareToIgnoreCaseNullable = Shared.compareToIgnoreCaseNullable(this.title, rootInfo.title);
        if (iCompareToIgnoreCaseNullable != 0) {
            return iCompareToIgnoreCaseNullable;
        }
        return Shared.compareToIgnoreCaseNullable(this.summary, rootInfo.summary);
    }

    public String toString() {
        return "Root{authority=" + this.authority + ", rootId=" + this.rootId + ", title=" + this.title + ", isUsb=" + isUsb() + ", isSd=" + isSd() + ", isMtp=" + isMtp() + "} @ " + getUri();
    }

    public String toDebugString() {
        if (TextUtils.isEmpty(this.summary)) {
            return "\"" + this.title + "\" @ " + getUri();
        }
        return "\"" + this.title + " (" + this.summary + ")\" @ " + getUri();
    }
}
