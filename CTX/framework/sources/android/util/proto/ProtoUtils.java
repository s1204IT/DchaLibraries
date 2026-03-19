package android.util.proto;

public class ProtoUtils {
    public static void toAggStatsProto(ProtoOutputStream protoOutputStream, long j, long j2, long j3, long j4) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1112396529665L, j2);
        protoOutputStream.write(1112396529666L, j3);
        protoOutputStream.write(1112396529667L, j4);
        protoOutputStream.end(jStart);
    }

    public static void toDuration(ProtoOutputStream protoOutputStream, long j, long j2, long j3) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1112396529665L, j2);
        protoOutputStream.write(1112396529666L, j3);
        protoOutputStream.end(jStart);
    }

    public static void writeBitWiseFlagsToProtoEnum(ProtoOutputStream protoOutputStream, long j, int i, int[] iArr, int[] iArr2) {
        if (iArr2.length != iArr.length) {
            throw new IllegalArgumentException("The length of origEnums must match protoEnums");
        }
        int length = iArr.length;
        for (int i2 = 0; i2 < length; i2++) {
            if (iArr[i2] == 0 && i == 0) {
                protoOutputStream.write(j, iArr2[i2]);
                return;
            } else {
                if ((iArr[i2] & i) != 0) {
                    protoOutputStream.write(j, iArr2[i2]);
                }
            }
        }
    }
}
