package android.media;

import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Size;
import java.nio.ByteBuffer;
import libcore.io.Memory;

class ImageUtils {
    ImageUtils() {
    }

    public static int getNumPlanesForFormat(int i) {
        switch (i) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 20:
            case 32:
            case 36:
            case 37:
            case 38:
            case 256:
            case 257:
            case 4098:
            case ImageFormat.Y8:
            case ImageFormat.Y16:
            case ImageFormat.DEPTH16:
                return 1;
            case 16:
                return 2;
            case 17:
            case 35:
            case ImageFormat.YV12:
                return 3;
            case 34:
                return 0;
            default:
                throw new UnsupportedOperationException(String.format("Invalid format specified %d", Integer.valueOf(i)));
        }
    }

    public static void imageCopy(Image image, Image image2) {
        int iRemaining;
        if (image == null || image2 == null) {
            throw new IllegalArgumentException("Images should be non-null");
        }
        if (image.getFormat() != image2.getFormat()) {
            throw new IllegalArgumentException("Src and dst images should have the same format");
        }
        if (image.getFormat() == 34 || image2.getFormat() == 34) {
            throw new IllegalArgumentException("PRIVATE format images are not copyable");
        }
        if (image.getFormat() == 36) {
            throw new IllegalArgumentException("Copy of RAW_OPAQUE format has not been implemented");
        }
        if (image.getFormat() == 4098) {
            throw new IllegalArgumentException("Copy of RAW_DEPTH format has not been implemented");
        }
        if (!(image2.getOwner() instanceof ImageWriter)) {
            throw new IllegalArgumentException("Destination image is not from ImageWriter. Only the images from ImageWriter are writable");
        }
        Size size = new Size(image.getWidth(), image.getHeight());
        Size size2 = new Size(image2.getWidth(), image2.getHeight());
        if (!size.equals(size2)) {
            throw new IllegalArgumentException("source image size " + size + " is different with destination image size " + size2);
        }
        Image.Plane[] planes = image.getPlanes();
        Image.Plane[] planes2 = image2.getPlanes();
        for (int i = 0; i < planes.length; i++) {
            int rowStride = planes[i].getRowStride();
            int rowStride2 = planes2[i].getRowStride();
            ByteBuffer buffer = planes[i].getBuffer();
            ByteBuffer buffer2 = planes2[i].getBuffer();
            if (!buffer.isDirect() || !buffer2.isDirect()) {
                throw new IllegalArgumentException("Source and destination ByteBuffers must be direct byteBuffer!");
            }
            if (planes[i].getPixelStride() != planes2[i].getPixelStride()) {
                throw new IllegalArgumentException("Source plane image pixel stride " + planes[i].getPixelStride() + " must be same as destination image pixel stride " + planes2[i].getPixelStride());
            }
            int iPosition = buffer.position();
            buffer.rewind();
            buffer2.rewind();
            if (rowStride == rowStride2) {
                buffer2.put(buffer);
            } else {
                int iPosition2 = buffer.position();
                int iPosition3 = buffer2.position();
                Size effectivePlaneSizeForImage = getEffectivePlaneSizeForImage(image, i);
                int width = effectivePlaneSizeForImage.getWidth() * planes[i].getPixelStride();
                int i2 = iPosition3;
                int i3 = iPosition2;
                for (int i4 = 0; i4 < effectivePlaneSizeForImage.getHeight(); i4++) {
                    if (i4 == effectivePlaneSizeForImage.getHeight() - 1 && width > (iRemaining = buffer.remaining() - i3)) {
                        width = iRemaining;
                    }
                    directByteBufferCopy(buffer, i3, buffer2, i2, width);
                    i3 += rowStride;
                    i2 += rowStride2;
                }
            }
            buffer.position(iPosition);
            buffer2.rewind();
        }
    }

    public static int getEstimatedNativeAllocBytes(int i, int i2, int i3, int i4) {
        double d;
        switch (i3) {
            case 1:
            case 2:
                d = 4.0d;
                break;
            case 3:
                d = 3.0d;
                break;
            case 4:
            case 16:
            case 20:
            case 32:
            case 36:
            case 4098:
            case ImageFormat.Y16:
            case ImageFormat.DEPTH16:
                d = 2.0d;
                break;
            case 17:
            case 34:
            case 35:
            case 38:
            case ImageFormat.YV12:
                d = 1.5d;
                break;
            case 37:
                d = 1.25d;
                break;
            case 256:
            case 257:
                d = 0.3d;
                break;
            case ImageFormat.Y8:
                d = 1.0d;
                break;
            default:
                throw new UnsupportedOperationException(String.format("Invalid format specified %d", Integer.valueOf(i3)));
        }
        return (int) (((double) (i * i2)) * d * ((double) i4));
    }

    private static Size getEffectivePlaneSizeForImage(Image image, int i) {
        switch (image.getFormat()) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 20:
            case 32:
            case 37:
            case 38:
            case 256:
            case 4098:
            case ImageFormat.Y8:
            case ImageFormat.Y16:
                return new Size(image.getWidth(), image.getHeight());
            case 16:
                if (i == 0) {
                    return new Size(image.getWidth(), image.getHeight());
                }
                return new Size(image.getWidth(), image.getHeight() / 2);
            case 17:
            case 35:
            case ImageFormat.YV12:
                if (i == 0) {
                    return new Size(image.getWidth(), image.getHeight());
                }
                return new Size(image.getWidth() / 2, image.getHeight() / 2);
            case 34:
                return new Size(0, 0);
            default:
                throw new UnsupportedOperationException(String.format("Invalid image format %d", Integer.valueOf(image.getFormat())));
        }
    }

    private static void directByteBufferCopy(ByteBuffer byteBuffer, int i, ByteBuffer byteBuffer2, int i2, int i3) {
        Memory.memmove(byteBuffer2, i2, byteBuffer, i, i3);
    }
}
