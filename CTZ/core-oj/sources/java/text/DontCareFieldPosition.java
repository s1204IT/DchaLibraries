package java.text;

import java.text.Format;

class DontCareFieldPosition extends FieldPosition {
    static final FieldPosition INSTANCE = new DontCareFieldPosition();
    private final Format.FieldDelegate noDelegate;

    private DontCareFieldPosition() {
        super(0);
        this.noDelegate = new Format.FieldDelegate() {
            @Override
            public void formatted(Format.Field field, Object obj, int i, int i2, StringBuffer stringBuffer) {
            }

            @Override
            public void formatted(int i, Format.Field field, Object obj, int i2, int i3, StringBuffer stringBuffer) {
            }
        };
    }

    @Override
    Format.FieldDelegate getFieldDelegate() {
        return this.noDelegate;
    }
}
