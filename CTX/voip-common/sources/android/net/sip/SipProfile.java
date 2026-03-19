package android.net.sip;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;

public class SipProfile implements Parcelable, Serializable, Cloneable {
    public static final Parcelable.Creator<SipProfile> CREATOR = new Parcelable.Creator<SipProfile>() {
        @Override
        public SipProfile createFromParcel(Parcel parcel) {
            return new SipProfile(parcel);
        }

        @Override
        public SipProfile[] newArray(int i) {
            return new SipProfile[i];
        }
    };
    private static final int DEFAULT_PORT = 5060;
    private static final String TCP = "TCP";
    private static final String UDP = "UDP";
    private static final long serialVersionUID = 1;
    private Address mAddress;
    private String mAuthUserName;
    private boolean mAutoRegistration;
    private transient int mCallingUid;
    private String mDomain;
    private String mPassword;
    private int mPort;
    private String mProfileName;
    private String mProtocol;
    private String mProxyAddress;
    private boolean mSendKeepAlive;

    public static class Builder {
        private AddressFactory mAddressFactory;
        private String mDisplayName;
        private SipProfile mProfile;
        private String mProxyAddress;
        private SipURI mUri;

        public Builder(SipProfile sipProfile) {
            this.mProfile = new SipProfile();
            try {
                this.mAddressFactory = SipFactory.getInstance().createAddressFactory();
                if (sipProfile != null) {
                    try {
                        this.mProfile = (SipProfile) sipProfile.clone();
                        this.mProfile.mAddress = null;
                        this.mUri = sipProfile.getUri();
                        this.mUri.setUserPassword(sipProfile.getPassword());
                        this.mDisplayName = sipProfile.getDisplayName();
                        this.mProxyAddress = sipProfile.getProxyAddress();
                        this.mProfile.mPort = sipProfile.getPort();
                        return;
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException("should not occur", e);
                    }
                }
                throw new NullPointerException();
            } catch (PeerUnavailableException e2) {
                throw new RuntimeException((Throwable) e2);
            }
        }

        public Builder(String str) throws ParseException {
            this.mProfile = new SipProfile();
            try {
                this.mAddressFactory = SipFactory.getInstance().createAddressFactory();
                if (str == null) {
                    throw new NullPointerException("uriString cannot be null");
                }
                SipURI sipURICreateURI = this.mAddressFactory.createURI(fix(str));
                if (sipURICreateURI instanceof SipURI) {
                    this.mUri = sipURICreateURI;
                    this.mProfile.mDomain = this.mUri.getHost();
                } else {
                    throw new ParseException(str + " is not a SIP URI", 0);
                }
            } catch (PeerUnavailableException e) {
                throw new RuntimeException((Throwable) e);
            }
        }

        public Builder(String str, String str2) throws ParseException {
            this.mProfile = new SipProfile();
            try {
                this.mAddressFactory = SipFactory.getInstance().createAddressFactory();
                if (str == null || str2 == null) {
                    throw new NullPointerException("username and serverDomain cannot be null");
                }
                this.mUri = this.mAddressFactory.createSipURI(str, str2);
                this.mProfile.mDomain = str2;
            } catch (PeerUnavailableException e) {
                throw new RuntimeException((Throwable) e);
            }
        }

        private String fix(String str) {
            if (str.trim().toLowerCase().startsWith("sip:")) {
                return str;
            }
            return "sip:" + str;
        }

        public Builder setAuthUserName(String str) {
            this.mProfile.mAuthUserName = str;
            return this;
        }

        public Builder setProfileName(String str) {
            this.mProfile.mProfileName = str;
            return this;
        }

        public Builder setPassword(String str) {
            this.mUri.setUserPassword(str);
            return this;
        }

        public Builder setPort(int i) throws IllegalArgumentException {
            if (i <= 65535 && i >= 1000) {
                this.mProfile.mPort = i;
                return this;
            }
            throw new IllegalArgumentException("incorrect port arugment: " + i);
        }

        public Builder setProtocol(String str) throws IllegalArgumentException {
            if (str == null) {
                throw new NullPointerException("protocol cannot be null");
            }
            String upperCase = str.toUpperCase();
            if (upperCase.equals(SipProfile.UDP) || upperCase.equals(SipProfile.TCP)) {
                this.mProfile.mProtocol = upperCase;
                return this;
            }
            throw new IllegalArgumentException("unsupported protocol: " + upperCase);
        }

        public Builder setOutboundProxy(String str) {
            this.mProxyAddress = str;
            return this;
        }

        public Builder setDisplayName(String str) {
            this.mDisplayName = str;
            return this;
        }

        public Builder setSendKeepAlive(boolean z) {
            this.mProfile.mSendKeepAlive = z;
            return this;
        }

        public Builder setAutoRegistration(boolean z) {
            this.mProfile.mAutoRegistration = z;
            return this;
        }

        public SipProfile build() {
            this.mProfile.mPassword = this.mUri.getUserPassword();
            this.mUri.setUserPassword((String) null);
            try {
                if (TextUtils.isEmpty(this.mProxyAddress)) {
                    if (!this.mProfile.mProtocol.equals(SipProfile.UDP)) {
                        this.mUri.setTransportParam(this.mProfile.mProtocol);
                    }
                    if (this.mProfile.mPort != SipProfile.DEFAULT_PORT) {
                        this.mUri.setPort(this.mProfile.mPort);
                    }
                } else {
                    SipURI sipURICreateURI = this.mAddressFactory.createURI(fix(this.mProxyAddress));
                    this.mProfile.mProxyAddress = sipURICreateURI.getHost();
                }
                this.mProfile.mAddress = this.mAddressFactory.createAddress(this.mDisplayName, this.mUri);
                return this.mProfile;
            } catch (InvalidArgumentException e) {
                throw new RuntimeException((Throwable) e);
            } catch (ParseException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    private SipProfile() {
        this.mProtocol = UDP;
        this.mPort = DEFAULT_PORT;
        this.mSendKeepAlive = false;
        this.mAutoRegistration = true;
        this.mCallingUid = 0;
    }

    private SipProfile(Parcel parcel) {
        boolean z;
        this.mProtocol = UDP;
        this.mPort = DEFAULT_PORT;
        this.mSendKeepAlive = false;
        this.mAutoRegistration = true;
        this.mCallingUid = 0;
        this.mAddress = parcel.readSerializable();
        this.mProxyAddress = parcel.readString();
        this.mPassword = parcel.readString();
        this.mDomain = parcel.readString();
        this.mProtocol = parcel.readString();
        this.mProfileName = parcel.readString();
        if (parcel.readInt() == 0) {
            z = false;
        } else {
            z = true;
        }
        this.mSendKeepAlive = z;
        this.mAutoRegistration = parcel.readInt() != 0;
        this.mCallingUid = parcel.readInt();
        this.mPort = parcel.readInt();
        this.mAuthUserName = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(this.mAddress);
        parcel.writeString(this.mProxyAddress);
        parcel.writeString(this.mPassword);
        parcel.writeString(this.mDomain);
        parcel.writeString(this.mProtocol);
        parcel.writeString(this.mProfileName);
        parcel.writeInt(this.mSendKeepAlive ? 1 : 0);
        parcel.writeInt(this.mAutoRegistration ? 1 : 0);
        parcel.writeInt(this.mCallingUid);
        parcel.writeInt(this.mPort);
        parcel.writeString(this.mAuthUserName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public SipURI getUri() {
        return this.mAddress.getURI();
    }

    public String getUriString() {
        if (!TextUtils.isEmpty(this.mProxyAddress)) {
            return "sip:" + getUserName() + "@" + this.mDomain;
        }
        return getUri().toString();
    }

    public Address getSipAddress() {
        return this.mAddress;
    }

    public String getDisplayName() {
        return this.mAddress.getDisplayName();
    }

    public String getUserName() {
        return getUri().getUser();
    }

    public String getAuthUserName() {
        return this.mAuthUserName;
    }

    public String getPassword() {
        return this.mPassword;
    }

    public String getSipDomain() {
        return this.mDomain;
    }

    public int getPort() {
        return this.mPort;
    }

    public String getProtocol() {
        return this.mProtocol;
    }

    public String getProxyAddress() {
        return this.mProxyAddress;
    }

    public String getProfileName() {
        return this.mProfileName;
    }

    public boolean getSendKeepAlive() {
        return this.mSendKeepAlive;
    }

    public boolean getAutoRegistration() {
        return this.mAutoRegistration;
    }

    public void setCallingUid(int i) {
        this.mCallingUid = i;
    }

    public int getCallingUid() {
        return this.mCallingUid;
    }

    private Object readResolve() throws ObjectStreamException {
        if (this.mPort == 0) {
            this.mPort = DEFAULT_PORT;
        }
        return this;
    }
}
