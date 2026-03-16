package com.android.camera.util;

import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.TonemapCurve;
import android.util.Pair;
import android.util.Rational;
import com.android.camera.debug.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class CaptureDataSerializer {
    private static final Log.Tag TAG = new Log.Tag("CaptureDataSerilzr");

    private interface Writeable {
        void write(Writer writer) throws IOException;
    }

    public static String toString(String title, CaptureRequest metadata) {
        StringWriter writer = new StringWriter();
        dumpMetadata(title, metadata, writer);
        return writer.toString();
    }

    public static void toFile(String title, CameraMetadata<?> metadata, File file) {
        try {
            FileWriter writer = new FileWriter(file, true);
            if (metadata instanceof CaptureRequest) {
                dumpMetadata(title, (CaptureRequest) metadata, writer);
            } else if (metadata instanceof CaptureResult) {
                dumpMetadata(title, (CaptureResult) metadata, writer);
            } else {
                writer.close();
                throw new IllegalArgumentException("Cannot generate debug data from type " + metadata.getClass().getName());
            }
            writer.close();
        } catch (IOException ex) {
            Log.e(TAG, "Could not write capture data to file.", ex);
        }
    }

    private static void dumpMetadata(final String title, final CaptureRequest metadata, Writer writer) {
        Writeable writeable = new Writeable() {
            @Override
            public void write(Writer writer2) throws IOException {
                List<CaptureRequest.Key<?>> keys = metadata.getKeys();
                writer2.write(title + '\n');
                for (CaptureRequest.Key<?> key : keys) {
                    writer2.write(String.format("    %s\n", key.getName()));
                    writer2.write(String.format("        %s\n", CaptureDataSerializer.metadataValueToString(metadata.get(key))));
                }
            }
        };
        dumpMetadata(writeable, new BufferedWriter(writer));
    }

    private static void dumpMetadata(final String title, final CaptureResult metadata, Writer writer) {
        Writeable writeable = new Writeable() {
            @Override
            public void write(Writer writer2) throws IOException {
                List<CaptureResult.Key<?>> keys = metadata.getKeys();
                writer2.write(String.format(title, new Object[0]));
                for (CaptureResult.Key<?> key : keys) {
                    writer2.write(String.format("    %s\n", key.getName()));
                    writer2.write(String.format("        %s\n", CaptureDataSerializer.metadataValueToString(metadata.get(key))));
                }
            }
        };
        dumpMetadata(writeable, new BufferedWriter(writer));
    }

    private static String metadataValueToString(Object object) {
        if (object == null) {
            return "<null>";
        }
        if (object.getClass().isArray()) {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(object, i);
                builder.append(metadataValueToString(item));
                if (i != length - 1) {
                    builder.append(", ");
                }
            }
            builder.append(']');
            return builder.toString();
        }
        if (object instanceof RggbChannelVector) {
            return toString((RggbChannelVector) object);
        }
        if (object instanceof ColorSpaceTransform) {
            return toString((ColorSpaceTransform) object);
        }
        if (object instanceof TonemapCurve) {
            return toString((TonemapCurve) object);
        }
        if (object instanceof Pair) {
            return toString((Pair<?, ?>) object);
        }
        return object.toString();
    }

    private static void dumpMetadata(Writeable metadata, Writer writer) {
        try {
            try {
                metadata.write(writer);
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        Log.e(TAG, "dumpMetadata - Failed to close writer.", e);
                    }
                }
            } catch (IOException e2) {
                Log.e(TAG, "dumpMetadata - Failed to dump metadata", e2);
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "dumpMetadata - Failed to close writer.", e3);
                    }
                }
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e4) {
                    Log.e(TAG, "dumpMetadata - Failed to close writer.", e4);
                }
            }
            throw th;
        }
    }

    private static String toString(RggbChannelVector vector) {
        return "RggbChannelVector: R:" + vector.getRed() + " G(even):" + vector.getGreenEven() + " G(odd):" + vector.getGreenOdd() + " B:" + vector.getBlue();
    }

    private static String toString(ColorSpaceTransform transform) {
        StringBuilder str = new StringBuilder();
        Rational[] rationals = new Rational[9];
        transform.copyElements(rationals, 0);
        str.append("ColorSpaceTransform: ");
        str.append(Arrays.toString(rationals));
        return str.toString();
    }

    private static String toString(TonemapCurve curve) {
        StringBuilder str = new StringBuilder();
        str.append("TonemapCurve:");
        float[] reds = new float[curve.getPointCount(0) * 2];
        curve.copyColorCurve(0, reds, 0);
        float[] greens = new float[curve.getPointCount(1) * 2];
        curve.copyColorCurve(1, greens, 0);
        float[] blues = new float[curve.getPointCount(2) * 2];
        curve.copyColorCurve(2, blues, 0);
        str.append("\n\nReds: ");
        str.append(Arrays.toString(reds));
        str.append("\n\nGreens: ");
        str.append(Arrays.toString(greens));
        str.append("\n\nBlues: ");
        str.append(Arrays.toString(blues));
        return str.toString();
    }

    private static String toString(Pair<?, ?> pair) {
        return "Pair: " + metadataValueToString(pair.first) + " / " + metadataValueToString(pair.second);
    }
}
