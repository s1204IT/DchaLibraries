package com.android.documentsui.services;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Features;
import com.android.documentsui.clipping.UrisSupplier;
import com.android.documentsui.services.Job;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class FileOperation implements Parcelable {
    private DocumentStack mDestination;
    private final List<Handler.Callback> mMessageListeners;
    private Messenger mMessenger;
    private final int mOpType;
    private final UrisSupplier mSrcs;

    abstract Job createJob(Context context, Job.Listener listener, String str, Features features);

    FileOperation(int i, UrisSupplier urisSupplier, DocumentStack documentStack) {
        this.mMessageListeners = new ArrayList();
        this.mMessenger = new Messenger(new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public final boolean handleMessage(Message message) {
                return this.f$0.onMessage(message);
            }
        }));
        Preconditions.checkArgument(i != -1);
        Preconditions.checkArgument(urisSupplier.getItemCount() > 0);
        this.mOpType = i;
        this.mSrcs = urisSupplier;
        this.mDestination = documentStack;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getOpType() {
        return this.mOpType;
    }

    public UrisSupplier getSrc() {
        return this.mSrcs;
    }

    public DocumentStack getDestination() {
        return this.mDestination;
    }

    public Messenger getMessenger() {
        return this.mMessenger;
    }

    public void setDestination(DocumentStack documentStack) {
        this.mDestination = documentStack;
    }

    public void dispose() {
        this.mSrcs.dispose();
    }

    private void appendInfoTo(StringBuilder sb) {
        sb.append("opType=");
        sb.append(this.mOpType);
        sb.append(", srcs=");
        sb.append(this.mSrcs.toString());
        sb.append(", destination=");
        sb.append(this.mDestination.toString());
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mOpType);
        parcel.writeParcelable(this.mSrcs, i);
        parcel.writeParcelable(this.mDestination, i);
        parcel.writeParcelable(this.mMessenger, i);
    }

    private FileOperation(Parcel parcel) {
        this.mMessageListeners = new ArrayList();
        this.mMessenger = new Messenger(new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public final boolean handleMessage(Message message) {
                return this.f$0.onMessage(message);
            }
        }));
        this.mOpType = parcel.readInt();
        this.mSrcs = (UrisSupplier) parcel.readParcelable(FileOperation.class.getClassLoader());
        this.mDestination = (DocumentStack) parcel.readParcelable(FileOperation.class.getClassLoader());
        this.mMessenger = (Messenger) parcel.readParcelable(FileOperation.class.getClassLoader());
    }

    public static class CopyOperation extends FileOperation {
        public static final Parcelable.Creator<CopyOperation> CREATOR = new Parcelable.Creator<CopyOperation>() {
            @Override
            public CopyOperation createFromParcel(Parcel parcel) {
                return new CopyOperation(parcel);
            }

            @Override
            public CopyOperation[] newArray(int i) {
                return new CopyOperation[i];
            }
        };

        private CopyOperation(UrisSupplier urisSupplier, DocumentStack documentStack) {
            super(1, urisSupplier, documentStack);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CopyOperation{");
            appendInfoTo(sb);
            sb.append("}");
            return sb.toString();
        }

        @Override
        CopyJob createJob(Context context, Job.Listener listener, String str, Features features) {
            return new CopyJob(context, listener, str, getDestination(), getSrc(), getMessenger(), features);
        }

        private CopyOperation(Parcel parcel) {
            super(parcel);
        }
    }

    public static class CompressOperation extends FileOperation {
        public static final Parcelable.Creator<CompressOperation> CREATOR = new Parcelable.Creator<CompressOperation>() {
            @Override
            public CompressOperation createFromParcel(Parcel parcel) {
                return new CompressOperation(parcel);
            }

            @Override
            public CompressOperation[] newArray(int i) {
                return new CompressOperation[i];
            }
        };

        private CompressOperation(UrisSupplier urisSupplier, DocumentStack documentStack) {
            super(3, urisSupplier, documentStack);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CompressOperation{");
            appendInfoTo(sb);
            sb.append("}");
            return sb.toString();
        }

        @Override
        CopyJob createJob(Context context, Job.Listener listener, String str, Features features) {
            return new CompressJob(context, listener, str, getDestination(), getSrc(), getMessenger(), features);
        }

        private CompressOperation(Parcel parcel) {
            super(parcel);
        }
    }

    public static class ExtractOperation extends FileOperation {
        public static final Parcelable.Creator<ExtractOperation> CREATOR = new Parcelable.Creator<ExtractOperation>() {
            @Override
            public ExtractOperation createFromParcel(Parcel parcel) {
                return new ExtractOperation(parcel);
            }

            @Override
            public ExtractOperation[] newArray(int i) {
                return new ExtractOperation[i];
            }
        };

        private ExtractOperation(UrisSupplier urisSupplier, DocumentStack documentStack) {
            super(2, urisSupplier, documentStack);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ExtractOperation{");
            appendInfoTo(sb);
            sb.append("}");
            return sb.toString();
        }

        @Override
        CopyJob createJob(Context context, Job.Listener listener, String str, Features features) {
            return new CopyJob(context, listener, str, getDestination(), getSrc(), getMessenger(), features);
        }

        private ExtractOperation(Parcel parcel) {
            super(parcel);
        }
    }

    public static class MoveDeleteOperation extends FileOperation {
        public static final Parcelable.Creator<MoveDeleteOperation> CREATOR = new Parcelable.Creator<MoveDeleteOperation>() {
            @Override
            public MoveDeleteOperation createFromParcel(Parcel parcel) {
                return new MoveDeleteOperation(parcel);
            }

            @Override
            public MoveDeleteOperation[] newArray(int i) {
                return new MoveDeleteOperation[i];
            }
        };
        private final Uri mSrcParent;

        private MoveDeleteOperation(int i, UrisSupplier urisSupplier, DocumentStack documentStack, Uri uri) {
            super(i, urisSupplier, documentStack);
            this.mSrcParent = uri;
        }

        @Override
        Job createJob(Context context, Job.Listener listener, String str, Features features) {
            switch (getOpType()) {
                case 4:
                    return new MoveJob(context, listener, str, getDestination(), getSrc(), this.mSrcParent, getMessenger(), features);
                case 5:
                    return new DeleteJob(context, listener, str, getDestination(), getSrc(), this.mSrcParent, features);
                default:
                    throw new UnsupportedOperationException("Unsupported op type: " + getOpType());
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("MoveDeleteOperation{");
            appendInfoTo(sb);
            sb.append(", srcParent=");
            sb.append(this.mSrcParent.toString());
            sb.append("}");
            return sb.toString();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeParcelable(this.mSrcParent, i);
        }

        private MoveDeleteOperation(Parcel parcel) {
            super(parcel);
            this.mSrcParent = (Uri) parcel.readParcelable(null);
        }
    }

    public static class Builder {
        private DocumentStack mDestination;
        private int mOpType;
        private Uri mSrcParent;
        private UrisSupplier mSrcs;

        public Builder withOpType(int i) {
            this.mOpType = i;
            return this;
        }

        public Builder withSrcParent(Uri uri) {
            this.mSrcParent = uri;
            return this;
        }

        public Builder withSrcs(UrisSupplier urisSupplier) {
            this.mSrcs = urisSupplier;
            return this;
        }

        public Builder withDestination(DocumentStack documentStack) {
            this.mDestination = documentStack;
            return this;
        }

        public FileOperation build() {
            switch (this.mOpType) {
                case 1:
                    return new CopyOperation(this.mSrcs, this.mDestination);
                case 2:
                    return new ExtractOperation(this.mSrcs, this.mDestination);
                case 3:
                    return new CompressOperation(this.mSrcs, this.mDestination);
                case 4:
                case 5:
                    return new MoveDeleteOperation(this.mOpType, this.mSrcs, this.mDestination, this.mSrcParent);
                default:
                    throw new UnsupportedOperationException("Unsupported op type: " + this.mOpType);
            }
        }
    }

    boolean onMessage(Message message) {
        Iterator<Handler.Callback> it = this.mMessageListeners.iterator();
        while (it.hasNext()) {
            if (it.next().handleMessage(message)) {
                return true;
            }
        }
        return false;
    }

    public void addMessageListener(Handler.Callback callback) {
        this.mMessageListeners.add(callback);
    }

    public void removeMessageListener(Handler.Callback callback) {
        this.mMessageListeners.remove(callback);
    }
}
