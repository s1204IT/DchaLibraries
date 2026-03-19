package android.os;

import android.os.Parcelable;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;

public final class Message implements Parcelable {
    static final int FLAGS_TO_CLEAR_ON_COPY_FROM = 1;
    static final int FLAG_ASYNCHRONOUS = 2;
    static final int FLAG_IN_USE = 1;
    private static final int MAX_POOL_SIZE = 50;
    private static Message sPool;
    public int arg1;
    public int arg2;
    Runnable callback;
    Bundle data;
    int flags;
    Message next;
    public Object obj;
    public Messenger replyTo;
    public int sendingUid = -1;
    Handler target;
    public int what;
    long when;
    public static final Object sPoolSync = new Object();
    private static int sPoolSize = 0;
    private static boolean gCheckRecycle = true;
    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel parcel) {
            Message messageObtain = Message.obtain();
            messageObtain.readFromParcel(parcel);
            return messageObtain;
        }

        @Override
        public Message[] newArray(int i) {
            return new Message[i];
        }
    };

    public static Message obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                Message message = sPool;
                sPool = message.next;
                message.next = null;
                message.flags = 0;
                sPoolSize--;
                return message;
            }
            return new Message();
        }
    }

    public static Message obtain(Message message) {
        Message messageObtain = obtain();
        messageObtain.what = message.what;
        messageObtain.arg1 = message.arg1;
        messageObtain.arg2 = message.arg2;
        messageObtain.obj = message.obj;
        messageObtain.replyTo = message.replyTo;
        messageObtain.sendingUid = message.sendingUid;
        if (message.data != null) {
            messageObtain.data = new Bundle(message.data);
        }
        messageObtain.target = message.target;
        messageObtain.callback = message.callback;
        return messageObtain;
    }

    public static Message obtain(Handler handler) {
        Message messageObtain = obtain();
        messageObtain.target = handler;
        return messageObtain;
    }

    public static Message obtain(Handler handler, Runnable runnable) {
        Message messageObtain = obtain();
        messageObtain.target = handler;
        messageObtain.callback = runnable;
        return messageObtain;
    }

    public static Message obtain(Handler handler, int i) {
        Message messageObtain = obtain();
        messageObtain.target = handler;
        messageObtain.what = i;
        return messageObtain;
    }

    public static Message obtain(Handler handler, int i, Object obj) {
        Message messageObtain = obtain();
        messageObtain.target = handler;
        messageObtain.what = i;
        messageObtain.obj = obj;
        return messageObtain;
    }

    public static Message obtain(Handler handler, int i, int i2, int i3) {
        Message messageObtain = obtain();
        messageObtain.target = handler;
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        messageObtain.arg2 = i3;
        return messageObtain;
    }

    public static Message obtain(Handler handler, int i, int i2, int i3, Object obj) {
        Message messageObtain = obtain();
        messageObtain.target = handler;
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        messageObtain.arg2 = i3;
        messageObtain.obj = obj;
        return messageObtain;
    }

    public static void updateCheckRecycle(int i) {
        if (i < 21) {
            gCheckRecycle = false;
        }
    }

    public void recycle() {
        if (isInUse()) {
            if (gCheckRecycle) {
                throw new IllegalStateException("This message cannot be recycled because it is still in use.");
            }
        } else {
            recycleUnchecked();
        }
    }

    void recycleUnchecked() {
        this.flags = 1;
        this.what = 0;
        this.arg1 = 0;
        this.arg2 = 0;
        this.obj = null;
        this.replyTo = null;
        this.sendingUid = -1;
        this.when = 0L;
        this.target = null;
        this.callback = null;
        this.data = null;
        synchronized (sPoolSync) {
            if (sPoolSize < 50) {
                this.next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }

    public void copyFrom(Message message) {
        this.flags = message.flags & (-2);
        this.what = message.what;
        this.arg1 = message.arg1;
        this.arg2 = message.arg2;
        this.obj = message.obj;
        this.replyTo = message.replyTo;
        this.sendingUid = message.sendingUid;
        if (message.data != null) {
            this.data = (Bundle) message.data.clone();
        } else {
            this.data = null;
        }
    }

    public long getWhen() {
        return this.when;
    }

    public void setTarget(Handler handler) {
        this.target = handler;
    }

    public Handler getTarget() {
        return this.target;
    }

    public Runnable getCallback() {
        return this.callback;
    }

    public Message setCallback(Runnable runnable) {
        this.callback = runnable;
        return this;
    }

    public Bundle getData() {
        if (this.data == null) {
            this.data = new Bundle();
        }
        return this.data;
    }

    public Bundle peekData() {
        return this.data;
    }

    public void setData(Bundle bundle) {
        this.data = bundle;
    }

    public Message setWhat(int i) {
        this.what = i;
        return this;
    }

    public void sendToTarget() {
        this.target.sendMessage(this);
    }

    public boolean isAsynchronous() {
        return (this.flags & 2) != 0;
    }

    public void setAsynchronous(boolean z) {
        if (z) {
            this.flags |= 2;
        } else {
            this.flags &= -3;
        }
    }

    boolean isInUse() {
        return (this.flags & 1) == 1;
    }

    void markInUse() {
        this.flags |= 1;
    }

    public String toString() {
        return toString(SystemClock.uptimeMillis());
    }

    String toString(long j) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ when=");
        TimeUtils.formatDuration(this.when - j, sb);
        if (this.target != null) {
            if (this.callback != null) {
                sb.append(" callback=");
                sb.append(this.callback.getClass().getName());
            } else {
                sb.append(" what=");
                sb.append(this.what);
            }
            if (this.arg1 != 0) {
                sb.append(" arg1=");
                sb.append(this.arg1);
            }
            if (this.arg2 != 0) {
                sb.append(" arg2=");
                sb.append(this.arg2);
            }
            if (this.obj != null) {
                sb.append(" obj=");
                sb.append(this.obj);
            }
            sb.append(" target=");
            sb.append(this.target.getClass().getName());
        } else {
            sb.append(" barrier=");
            sb.append(this.arg1);
        }
        sb.append(" }");
        return sb.toString();
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1112396529665L, this.when);
        if (this.target != null) {
            if (this.callback != null) {
                protoOutputStream.write(1138166333442L, this.callback.getClass().getName());
            } else {
                protoOutputStream.write(1120986464259L, this.what);
            }
            if (this.arg1 != 0) {
                protoOutputStream.write(1120986464260L, this.arg1);
            }
            if (this.arg2 != 0) {
                protoOutputStream.write(1120986464261L, this.arg2);
            }
            if (this.obj != null) {
                protoOutputStream.write(1138166333446L, this.obj.toString());
            }
            protoOutputStream.write(1138166333447L, this.target.getClass().getName());
        } else {
            protoOutputStream.write(1120986464264L, this.arg1);
        }
        protoOutputStream.end(jStart);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.callback != null) {
            throw new RuntimeException("Can't marshal callbacks across processes.");
        }
        parcel.writeInt(this.what);
        parcel.writeInt(this.arg1);
        parcel.writeInt(this.arg2);
        if (this.obj != null) {
            try {
                Parcelable parcelable = (Parcelable) this.obj;
                parcel.writeInt(1);
                parcel.writeParcelable(parcelable, i);
            } catch (ClassCastException e) {
                throw new RuntimeException("Can't marshal non-Parcelable objects across processes.");
            }
        } else {
            parcel.writeInt(0);
        }
        parcel.writeLong(this.when);
        parcel.writeBundle(this.data);
        Messenger.writeMessengerOrNullToParcel(this.replyTo, parcel);
        parcel.writeInt(this.sendingUid);
    }

    private void readFromParcel(Parcel parcel) {
        this.what = parcel.readInt();
        this.arg1 = parcel.readInt();
        this.arg2 = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.obj = parcel.readParcelable(getClass().getClassLoader());
        }
        this.when = parcel.readLong();
        this.data = parcel.readBundle();
        this.replyTo = Messenger.readMessengerOrNullFromParcel(parcel);
        this.sendingUid = parcel.readInt();
    }
}
