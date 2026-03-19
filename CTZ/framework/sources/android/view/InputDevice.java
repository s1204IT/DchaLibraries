package android.view;

import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.NullVibrator;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Vibrator;
import com.android.internal.inputmethod.InputMethodUtils;
import java.util.ArrayList;
import java.util.List;

public final class InputDevice implements Parcelable {
    public static final Parcelable.Creator<InputDevice> CREATOR = new Parcelable.Creator<InputDevice>() {
        @Override
        public InputDevice createFromParcel(Parcel parcel) {
            return new InputDevice(parcel);
        }

        @Override
        public InputDevice[] newArray(int i) {
            return new InputDevice[i];
        }
    };
    public static final int KEYBOARD_TYPE_ALPHABETIC = 2;
    public static final int KEYBOARD_TYPE_NONE = 0;
    public static final int KEYBOARD_TYPE_NON_ALPHABETIC = 1;
    private static final int MAX_RANGES = 1000;

    @Deprecated
    public static final int MOTION_RANGE_ORIENTATION = 8;

    @Deprecated
    public static final int MOTION_RANGE_PRESSURE = 2;

    @Deprecated
    public static final int MOTION_RANGE_SIZE = 3;

    @Deprecated
    public static final int MOTION_RANGE_TOOL_MAJOR = 6;

    @Deprecated
    public static final int MOTION_RANGE_TOOL_MINOR = 7;

    @Deprecated
    public static final int MOTION_RANGE_TOUCH_MAJOR = 4;

    @Deprecated
    public static final int MOTION_RANGE_TOUCH_MINOR = 5;

    @Deprecated
    public static final int MOTION_RANGE_X = 0;

    @Deprecated
    public static final int MOTION_RANGE_Y = 1;
    public static final int SOURCE_ANY = -256;
    public static final int SOURCE_BLUETOOTH_STYLUS = 49154;
    public static final int SOURCE_CLASS_BUTTON = 1;
    public static final int SOURCE_CLASS_JOYSTICK = 16;
    public static final int SOURCE_CLASS_MASK = 255;
    public static final int SOURCE_CLASS_NONE = 0;
    public static final int SOURCE_CLASS_POINTER = 2;
    public static final int SOURCE_CLASS_POSITION = 8;
    public static final int SOURCE_CLASS_TRACKBALL = 4;
    public static final int SOURCE_DPAD = 513;
    public static final int SOURCE_GAMEPAD = 1025;
    public static final int SOURCE_HDMI = 33554433;
    public static final int SOURCE_JOYSTICK = 16777232;
    public static final int SOURCE_KEYBOARD = 257;
    public static final int SOURCE_MOUSE = 8194;
    public static final int SOURCE_MOUSE_RELATIVE = 131076;
    public static final int SOURCE_ROTARY_ENCODER = 4194304;
    public static final int SOURCE_STYLUS = 16386;
    public static final int SOURCE_TOUCHPAD = 1048584;
    public static final int SOURCE_TOUCHSCREEN = 4098;
    public static final int SOURCE_TOUCH_NAVIGATION = 2097152;
    public static final int SOURCE_TRACKBALL = 65540;
    public static final int SOURCE_UNKNOWN = 0;
    private final int mControllerNumber;
    private final String mDescriptor;
    private final int mGeneration;
    private final boolean mHasButtonUnderPad;
    private final boolean mHasMicrophone;
    private final boolean mHasVibrator;
    private final int mId;
    private final InputDeviceIdentifier mIdentifier;
    private final boolean mIsExternal;
    private final KeyCharacterMap mKeyCharacterMap;
    private final int mKeyboardType;
    private final ArrayList<MotionRange> mMotionRanges;
    private final String mName;
    private final int mProductId;
    private final int mSources;
    private final int mVendorId;
    private Vibrator mVibrator;

    private InputDevice(int i, int i2, int i3, String str, int i4, int i5, String str2, boolean z, int i6, int i7, KeyCharacterMap keyCharacterMap, boolean z2, boolean z3, boolean z4) {
        this.mMotionRanges = new ArrayList<>();
        this.mId = i;
        this.mGeneration = i2;
        this.mControllerNumber = i3;
        this.mName = str;
        this.mVendorId = i4;
        this.mProductId = i5;
        this.mDescriptor = str2;
        this.mIsExternal = z;
        this.mSources = i6;
        this.mKeyboardType = i7;
        this.mKeyCharacterMap = keyCharacterMap;
        this.mHasVibrator = z2;
        this.mHasMicrophone = z3;
        this.mHasButtonUnderPad = z4;
        this.mIdentifier = new InputDeviceIdentifier(str2, i4, i5);
    }

    private InputDevice(Parcel parcel) {
        this.mMotionRanges = new ArrayList<>();
        this.mId = parcel.readInt();
        this.mGeneration = parcel.readInt();
        this.mControllerNumber = parcel.readInt();
        this.mName = parcel.readString();
        this.mVendorId = parcel.readInt();
        this.mProductId = parcel.readInt();
        this.mDescriptor = parcel.readString();
        this.mIsExternal = parcel.readInt() != 0;
        this.mSources = parcel.readInt();
        this.mKeyboardType = parcel.readInt();
        this.mKeyCharacterMap = KeyCharacterMap.CREATOR.createFromParcel(parcel);
        this.mHasVibrator = parcel.readInt() != 0;
        this.mHasMicrophone = parcel.readInt() != 0;
        this.mHasButtonUnderPad = parcel.readInt() != 0;
        this.mIdentifier = new InputDeviceIdentifier(this.mDescriptor, this.mVendorId, this.mProductId);
        int i = parcel.readInt();
        i = i > 1000 ? 1000 : i;
        for (int i2 = 0; i2 < i; i2++) {
            addMotionRange(parcel.readInt(), parcel.readInt(), parcel.readFloat(), parcel.readFloat(), parcel.readFloat(), parcel.readFloat(), parcel.readFloat());
        }
    }

    public static InputDevice getDevice(int i) {
        return InputManager.getInstance().getInputDevice(i);
    }

    public static int[] getDeviceIds() {
        return InputManager.getInstance().getInputDeviceIds();
    }

    public int getId() {
        return this.mId;
    }

    public int getControllerNumber() {
        return this.mControllerNumber;
    }

    public InputDeviceIdentifier getIdentifier() {
        return this.mIdentifier;
    }

    public int getGeneration() {
        return this.mGeneration;
    }

    public int getVendorId() {
        return this.mVendorId;
    }

    public int getProductId() {
        return this.mProductId;
    }

    public String getDescriptor() {
        return this.mDescriptor;
    }

    public boolean isVirtual() {
        return this.mId < 0;
    }

    public boolean isExternal() {
        return this.mIsExternal;
    }

    public boolean isFullKeyboard() {
        return (this.mSources & 257) == 257 && this.mKeyboardType == 2;
    }

    public String getName() {
        return this.mName;
    }

    public int getSources() {
        return this.mSources;
    }

    public boolean supportsSource(int i) {
        return (this.mSources & i) == i;
    }

    public int getKeyboardType() {
        return this.mKeyboardType;
    }

    public KeyCharacterMap getKeyCharacterMap() {
        return this.mKeyCharacterMap;
    }

    public boolean[] hasKeys(int... iArr) {
        return InputManager.getInstance().deviceHasKeys(this.mId, iArr);
    }

    public MotionRange getMotionRange(int i) {
        int size = this.mMotionRanges.size();
        for (int i2 = 0; i2 < size; i2++) {
            MotionRange motionRange = this.mMotionRanges.get(i2);
            if (motionRange.mAxis == i) {
                return motionRange;
            }
        }
        return null;
    }

    public MotionRange getMotionRange(int i, int i2) {
        int size = this.mMotionRanges.size();
        for (int i3 = 0; i3 < size; i3++) {
            MotionRange motionRange = this.mMotionRanges.get(i3);
            if (motionRange.mAxis == i && motionRange.mSource == i2) {
                return motionRange;
            }
        }
        return null;
    }

    public List<MotionRange> getMotionRanges() {
        return this.mMotionRanges;
    }

    private void addMotionRange(int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.mMotionRanges.add(new MotionRange(i, i2, f, f2, f3, f4, f5));
    }

    public Vibrator getVibrator() {
        Vibrator vibrator;
        synchronized (this.mMotionRanges) {
            if (this.mVibrator == null) {
                if (this.mHasVibrator) {
                    this.mVibrator = InputManager.getInstance().getInputDeviceVibrator(this.mId);
                } else {
                    this.mVibrator = NullVibrator.getInstance();
                }
            }
            vibrator = this.mVibrator;
        }
        return vibrator;
    }

    public boolean isEnabled() {
        return InputManager.getInstance().isInputDeviceEnabled(this.mId);
    }

    public void enable() {
        InputManager.getInstance().enableInputDevice(this.mId);
    }

    public void disable() {
        InputManager.getInstance().disableInputDevice(this.mId);
    }

    public boolean hasMicrophone() {
        return this.mHasMicrophone;
    }

    public boolean hasButtonUnderPad() {
        return this.mHasButtonUnderPad;
    }

    public void setPointerType(int i) {
        InputManager.getInstance().setPointerIconType(i);
    }

    public void setCustomPointerIcon(PointerIcon pointerIcon) {
        InputManager.getInstance().setCustomPointerIcon(pointerIcon);
    }

    public static final class MotionRange {
        private int mAxis;
        private float mFlat;
        private float mFuzz;
        private float mMax;
        private float mMin;
        private float mResolution;
        private int mSource;

        private MotionRange(int i, int i2, float f, float f2, float f3, float f4, float f5) {
            this.mAxis = i;
            this.mSource = i2;
            this.mMin = f;
            this.mMax = f2;
            this.mFlat = f3;
            this.mFuzz = f4;
            this.mResolution = f5;
        }

        public int getAxis() {
            return this.mAxis;
        }

        public int getSource() {
            return this.mSource;
        }

        public boolean isFromSource(int i) {
            return (getSource() & i) == i;
        }

        public float getMin() {
            return this.mMin;
        }

        public float getMax() {
            return this.mMax;
        }

        public float getRange() {
            return this.mMax - this.mMin;
        }

        public float getFlat() {
            return this.mFlat;
        }

        public float getFuzz() {
            return this.mFuzz;
        }

        public float getResolution() {
            return this.mResolution;
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mId);
        parcel.writeInt(this.mGeneration);
        parcel.writeInt(this.mControllerNumber);
        parcel.writeString(this.mName);
        parcel.writeInt(this.mVendorId);
        parcel.writeInt(this.mProductId);
        parcel.writeString(this.mDescriptor);
        parcel.writeInt(this.mIsExternal ? 1 : 0);
        parcel.writeInt(this.mSources);
        parcel.writeInt(this.mKeyboardType);
        this.mKeyCharacterMap.writeToParcel(parcel, i);
        parcel.writeInt(this.mHasVibrator ? 1 : 0);
        parcel.writeInt(this.mHasMicrophone ? 1 : 0);
        parcel.writeInt(this.mHasButtonUnderPad ? 1 : 0);
        int size = this.mMotionRanges.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            MotionRange motionRange = this.mMotionRanges.get(i2);
            parcel.writeInt(motionRange.mAxis);
            parcel.writeInt(motionRange.mSource);
            parcel.writeFloat(motionRange.mMin);
            parcel.writeFloat(motionRange.mMax);
            parcel.writeFloat(motionRange.mFlat);
            parcel.writeFloat(motionRange.mFuzz);
            parcel.writeFloat(motionRange.mResolution);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Input Device ");
        sb.append(this.mId);
        sb.append(": ");
        sb.append(this.mName);
        sb.append("\n");
        sb.append("  Descriptor: ");
        sb.append(this.mDescriptor);
        sb.append("\n");
        sb.append("  Generation: ");
        sb.append(this.mGeneration);
        sb.append("\n");
        sb.append("  Location: ");
        sb.append(this.mIsExternal ? "external" : "built-in");
        sb.append("\n");
        sb.append("  Keyboard Type: ");
        switch (this.mKeyboardType) {
            case 0:
                sb.append("none");
                break;
            case 1:
                sb.append("non-alphabetic");
                break;
            case 2:
                sb.append("alphabetic");
                break;
        }
        sb.append("\n");
        sb.append("  Has Vibrator: ");
        sb.append(this.mHasVibrator);
        sb.append("\n");
        sb.append("  Has mic: ");
        sb.append(this.mHasMicrophone);
        sb.append("\n");
        sb.append("  Sources: 0x");
        sb.append(Integer.toHexString(this.mSources));
        sb.append(" (");
        appendSourceDescriptionIfApplicable(sb, 257, InputMethodUtils.SUBTYPE_MODE_KEYBOARD);
        appendSourceDescriptionIfApplicable(sb, 513, "dpad");
        appendSourceDescriptionIfApplicable(sb, 4098, "touchscreen");
        appendSourceDescriptionIfApplicable(sb, 8194, "mouse");
        appendSourceDescriptionIfApplicable(sb, 16386, "stylus");
        appendSourceDescriptionIfApplicable(sb, 65540, "trackball");
        appendSourceDescriptionIfApplicable(sb, SOURCE_MOUSE_RELATIVE, "mouse_relative");
        appendSourceDescriptionIfApplicable(sb, SOURCE_TOUCHPAD, "touchpad");
        appendSourceDescriptionIfApplicable(sb, SOURCE_JOYSTICK, "joystick");
        appendSourceDescriptionIfApplicable(sb, 1025, "gamepad");
        sb.append(" )\n");
        int size = this.mMotionRanges.size();
        for (int i = 0; i < size; i++) {
            MotionRange motionRange = this.mMotionRanges.get(i);
            sb.append("    ");
            sb.append(MotionEvent.axisToString(motionRange.mAxis));
            sb.append(": source=0x");
            sb.append(Integer.toHexString(motionRange.mSource));
            sb.append(" min=");
            sb.append(motionRange.mMin);
            sb.append(" max=");
            sb.append(motionRange.mMax);
            sb.append(" flat=");
            sb.append(motionRange.mFlat);
            sb.append(" fuzz=");
            sb.append(motionRange.mFuzz);
            sb.append(" resolution=");
            sb.append(motionRange.mResolution);
            sb.append("\n");
        }
        return sb.toString();
    }

    private void appendSourceDescriptionIfApplicable(StringBuilder sb, int i, String str) {
        if ((this.mSources & i) == i) {
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(str);
        }
    }
}
