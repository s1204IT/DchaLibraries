package android.location;

import android.util.SparseArray;
import java.util.Iterator;
import java.util.NoSuchElementException;

@Deprecated
public final class GpsStatus {
    private static final int BEIDOU_SVID_OFFSET = 200;
    private static final int GLONASS_SVID_OFFSET = 64;
    public static final int GPS_EVENT_FIRST_FIX = 3;
    public static final int GPS_EVENT_SATELLITE_STATUS = 4;
    public static final int GPS_EVENT_STARTED = 1;
    public static final int GPS_EVENT_STOPPED = 2;
    private static final int NUM_SATELLITES = 255;
    private static final int SBAS_SVID_OFFSET = -87;
    private int mTimeToFirstFix;
    private final SparseArray<GpsSatellite> mSatellites = new SparseArray<>();
    private Iterable<GpsSatellite> mSatelliteList = new Iterable<GpsSatellite>() {
        @Override
        public Iterator<GpsSatellite> iterator() {
            return GpsStatus.this.new SatelliteIterator();
        }
    };

    @Deprecated
    public interface Listener {
        void onGpsStatusChanged(int i);
    }

    @Deprecated
    public interface NmeaListener {
        void onNmeaReceived(long j, String str);
    }

    private final class SatelliteIterator implements Iterator<GpsSatellite> {
        private int mIndex = 0;
        private final int mSatellitesCount;

        SatelliteIterator() {
            this.mSatellitesCount = GpsStatus.this.mSatellites.size();
        }

        @Override
        public boolean hasNext() {
            while (this.mIndex < this.mSatellitesCount) {
                if (((GpsSatellite) GpsStatus.this.mSatellites.valueAt(this.mIndex)).mValid) {
                    return true;
                }
                this.mIndex++;
            }
            return false;
        }

        @Override
        public GpsSatellite next() {
            while (this.mIndex < this.mSatellitesCount) {
                GpsSatellite gpsSatellite = (GpsSatellite) GpsStatus.this.mSatellites.valueAt(this.mIndex);
                this.mIndex++;
                if (gpsSatellite.mValid) {
                    return gpsSatellite;
                }
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    GpsStatus() {
    }

    private void setStatus(int i, int[] iArr, float[] fArr, float[] fArr2, float[] fArr3) {
        clearSatellites();
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = (iArr[i2] >> 4) & 15;
            int i4 = iArr[i2] >> 8;
            if (i3 == 3) {
                i4 += 64;
            } else if (i3 == 5) {
                i4 += 200;
            } else if (i3 == 2) {
                i4 += SBAS_SVID_OFFSET;
            } else {
                if (i3 == 1 || i3 == 4) {
                }
            }
            if (i4 > 0 && i4 <= 255) {
                GpsSatellite gpsSatellite = this.mSatellites.get(i4);
                if (gpsSatellite == null) {
                    gpsSatellite = new GpsSatellite(i4);
                    this.mSatellites.put(i4, gpsSatellite);
                }
                gpsSatellite.mValid = true;
                gpsSatellite.mSnr = fArr[i2];
                gpsSatellite.mElevation = fArr2[i2];
                gpsSatellite.mAzimuth = fArr3[i2];
                gpsSatellite.mHasEphemeris = (iArr[i2] & 1) != 0;
                gpsSatellite.mHasAlmanac = (iArr[i2] & 2) != 0;
                gpsSatellite.mUsedInFix = (4 & iArr[i2]) != 0;
            }
        }
    }

    void setStatus(GnssStatus gnssStatus, int i) {
        this.mTimeToFirstFix = i;
        setStatus(gnssStatus.mSvCount, gnssStatus.mSvidWithFlags, gnssStatus.mCn0DbHz, gnssStatus.mElevations, gnssStatus.mAzimuths);
    }

    void setTimeToFirstFix(int i) {
        this.mTimeToFirstFix = i;
    }

    public int getTimeToFirstFix() {
        return this.mTimeToFirstFix;
    }

    public Iterable<GpsSatellite> getSatellites() {
        return this.mSatelliteList;
    }

    public int getMaxSatellites() {
        return 255;
    }

    private void clearSatellites() {
        int size = this.mSatellites.size();
        for (int i = 0; i < size; i++) {
            this.mSatellites.valueAt(i).mValid = false;
        }
    }
}
