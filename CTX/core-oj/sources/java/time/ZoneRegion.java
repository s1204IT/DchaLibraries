package java.time;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesException;
import java.time.zone.ZoneRulesProvider;
import java.util.Objects;

final class ZoneRegion extends ZoneId implements Serializable {
    private static final long serialVersionUID = 8386373296231747096L;
    private final String id;
    private final transient ZoneRules rules;

    static ZoneRegion ofId(String str, boolean z) {
        ZoneRules rules;
        Objects.requireNonNull(str, "zoneId");
        checkName(str);
        try {
            rules = ZoneRulesProvider.getRules(str, true);
        } catch (ZoneRulesException e) {
            if (z) {
                throw e;
            }
            rules = null;
        }
        return new ZoneRegion(str, rules);
    }

    private static void checkName(String str) {
        int length = str.length();
        if (length < 2) {
            throw new DateTimeException("Invalid ID for region-based ZoneId, invalid format: " + str);
        }
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if ((cCharAt < 'a' || cCharAt > 'z') && ((cCharAt < 'A' || cCharAt > 'Z') && ((cCharAt != '/' || i == 0) && ((cCharAt < '0' || cCharAt > '9' || i == 0) && ((cCharAt != '~' || i == 0) && ((cCharAt != '.' || i == 0) && ((cCharAt != '_' || i == 0) && ((cCharAt != '+' || i == 0) && (cCharAt != '-' || i == 0))))))))) {
                throw new DateTimeException("Invalid ID for region-based ZoneId, invalid format: " + str);
            }
        }
    }

    ZoneRegion(String str, ZoneRules zoneRules) {
        this.id = str;
        this.rules = zoneRules;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public ZoneRules getRules() {
        return this.rules != null ? this.rules : ZoneRulesProvider.getRules(this.id, false);
    }

    private Object writeReplace() {
        return new Ser((byte) 7, this);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    @Override
    void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(7);
        writeExternal(dataOutput);
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.id);
    }

    static ZoneId readExternal(DataInput dataInput) throws IOException {
        return ZoneId.of(dataInput.readUTF(), false);
    }
}
