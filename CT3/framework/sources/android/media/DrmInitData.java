package android.media;

import java.util.Arrays;
import java.util.UUID;

public abstract class DrmInitData {
    public abstract SchemeInitData get(UUID uuid);

    DrmInitData() {
    }

    public static final class SchemeInitData {
        public final byte[] data;
        public final String mimeType;

        public SchemeInitData(String mimeType, byte[] data) {
            this.mimeType = mimeType;
            this.data = data;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SchemeInitData)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (this.mimeType.equals(obj.mimeType)) {
                return Arrays.equals(this.data, obj.data);
            }
            return false;
        }

        public int hashCode() {
            return this.mimeType.hashCode() + (Arrays.hashCode(this.data) * 31);
        }
    }
}
