package mediatek.telephony;

import android.app.ActivityThread;
import android.os.Parcel;
import android.telephony.Rlog;
import android.telephony.SignalStrength;
import com.mediatek.internal.telephony.MtkOpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.MtkOpTelephonyCustomizationUtils;

public class MtkSignalStrength extends SignalStrength {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "MtkSignalStrength";
    private static ISignalStrengthExt mSignalStrengthExt = null;
    protected int mPhoneId;
    private MtkOpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory;

    public MtkSignalStrength(int i) {
        this.mTelephonyCustomizationFactory = null;
        this.mPhoneId = i;
    }

    public MtkSignalStrength(int i, SignalStrength signalStrength) {
        super(signalStrength);
        this.mTelephonyCustomizationFactory = null;
        this.mPhoneId = i;
    }

    public MtkSignalStrength(Parcel parcel) {
        super(parcel);
        this.mTelephonyCustomizationFactory = null;
        this.mPhoneId = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.mPhoneId);
    }

    private ISignalStrengthExt getOpInstance() {
        if (mSignalStrengthExt == null) {
            try {
                this.mTelephonyCustomizationFactory = MtkOpTelephonyCustomizationUtils.getOpFactory(ActivityThread.currentApplication().getApplicationContext());
                mSignalStrengthExt = this.mTelephonyCustomizationFactory.makeSignalStrengthExt();
            } catch (Exception e) {
                log("mSignalStrengthExt init fail");
                e.printStackTrace();
            }
        }
        return mSignalStrengthExt;
    }

    private static void log(String str) {
        Rlog.w(LOG_TAG, str);
    }

    @Override
    public int getLevel() {
        return super.getLevel();
    }

    public int getLteLevel() {
        ISignalStrengthExt opInstance = getOpInstance();
        if (opInstance != null) {
            return opInstance.mapLteSignalLevel(this.mLteRsrp, this.mLteRssnr, this.mLteSignalStrength);
        }
        log("[getLteLevel] null op customization instance");
        return super.getLteLevel();
    }
}
