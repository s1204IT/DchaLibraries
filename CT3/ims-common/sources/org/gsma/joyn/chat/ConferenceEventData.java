package org.gsma.joyn.chat;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
import org.gsma.joyn.Logger;

public class ConferenceEventData implements Parcelable {
    public static final Parcelable.Creator<ConferenceEventData> CREATOR = new Parcelable.Creator<ConferenceEventData>() {
        @Override
        public ConferenceEventData createFromParcel(Parcel source) {
            return new ConferenceEventData(source);
        }

        @Override
        public ConferenceEventData[] newArray(int size) {
            return new ConferenceEventData[size];
        }
    };
    public static final String TAG = "TAPI-ConferenceEventData";
    private String chairman;
    private String state;
    private String subject;
    private List<ConferenceUser> users;

    public static final class State {
        public static final String FULL = "full";
        public static final String PARTIAL = "partial";
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getChairman() {
        return this.chairman;
    }

    public void setChairman(String chairman) {
        this.chairman = chairman;
    }

    public List<ConferenceUser> getUsers() {
        return this.users;
    }

    public void setUsers(List<ConferenceUser> users) {
        this.users = users;
    }

    public ConferenceEventData(String state, String subject, String chairman, List<ConferenceUser> users) {
        this.state = "";
        this.subject = "";
        this.chairman = "";
        this.state = state;
        this.subject = subject;
        this.chairman = chairman;
        this.users = users;
    }

    public ConferenceEventData(Parcel source) {
        this.state = "";
        this.subject = "";
        this.chairman = "";
        this.state = source.readString();
        this.subject = source.readString();
        this.chairman = source.readString();
        if (source.dataAvail() <= 0) {
            return;
        }
        int size = source.readInt();
        this.users = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            ConferenceUser user = (ConferenceUser) source.readParcelable(null);
            this.users.add(user);
        }
    }

    public ConferenceEventData() {
        this.state = "";
        this.subject = "";
        this.chairman = "";
    }

    @Override
    public void writeToParcel(Parcel dest, int arg1) {
        dest.writeString(this.state);
        dest.writeString(this.subject);
        dest.writeString(this.chairman);
        if (this.users == null) {
            return;
        }
        dest.writeInt(this.users.size());
        for (ConferenceUser user : this.users) {
            dest.writeParcelable(user, 0);
        }
    }

    public static class ConferenceUser implements Parcelable {
        public static final Parcelable.Creator<ConferenceUser> CREATOR = new Parcelable.Creator<ConferenceUser>() {
            @Override
            public ConferenceUser createFromParcel(Parcel source) {
                return new ConferenceUser(source);
            }

            @Override
            public ConferenceUser[] newArray(int size) {
                return new ConferenceUser[size];
            }
        };
        private String displayName;
        private String entity;
        private String etype;
        private String method;
        private String role;
        private String state;
        private String status;

        public static class Method {
            public static final String BOOTED = "booted";
            public static final String DEPARTED = "departed";
        }

        public static class Role {
            public static final String CHAIRMAN = "chairman";
            public static final String PARTICIPANT = "participant";
        }

        public static class State {
            public static final String DELETED = "deleted";
            public static final String FULL = "full";
            public static final String PARTIAL = "partial";
        }

        public static class Status {
            public static final String CONNECTED = "connected";
            public static final String DISCONNECTED = "disconnected";
            public static final String PENDING = "pending";
        }

        public ConferenceUser(String entity, String state, String status, String method, String role, String etype, String displayName) {
            Logger.i(ConferenceEventData.TAG, "ConferenceUser entryentity=" + entity + " state=" + state + " status=" + status + " method=" + method + " role=" + role + " etype=" + etype + " displayName=" + displayName);
            this.entity = entity;
            this.state = state;
            this.status = status;
            this.method = method;
            this.role = role;
            this.etype = etype;
            this.displayName = displayName;
        }

        public ConferenceUser(Parcel source) {
            this.entity = source.readString();
            this.state = source.readString();
            this.status = source.readString();
            this.method = source.readString();
            this.role = source.readString();
            this.etype = source.readString();
            this.displayName = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.entity);
            dest.writeString(this.state);
            dest.writeString(this.status);
            dest.writeString(this.method);
            dest.writeString(this.role);
            dest.writeString(this.etype);
            dest.writeString(this.displayName);
        }

        public String getRole() {
            return this.role;
        }

        public String getState() {
            return this.state;
        }

        public String getStatus() {
            return this.status;
        }

        public String getDisconnectMethod() {
            return this.method;
        }

        public String getEtype() {
            return this.etype;
        }

        public String getEntity() {
            return this.entity;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
