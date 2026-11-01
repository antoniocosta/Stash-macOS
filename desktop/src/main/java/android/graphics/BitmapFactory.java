package android.graphics;

import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Desktop shim for android.graphics.BitmapFactory: decodes via javax.imageio (JPEG/PNG/GIF/BMP),
 * returning null when undecodable, like Android. Java so the result keeps Android's platform type.
 */
public final class BitmapFactory {
    private BitmapFactory() {}

    /** Android's BitmapFactory.Options: bounds-only decoding and power-of-two subsampling. */
    public static class Options {
        public boolean inJustDecodeBounds;
        public int inSampleSize;
        public int outWidth;
        public int outHeight;
        public String outMimeType;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public boolean inMutable;
    }

    public static Bitmap decodeStream(InputStream is) {
        return decodeStream(is, null, null);
    }

    public static Bitmap decodeStream(InputStream is, Object outPadding, Options opts) {
        if (is == null) return null;
        try {
            BufferedImage image = ImageIO.read(is);
            if (image == null) return null;
            if (opts != null) {
                opts.outWidth = image.getWidth();
                opts.outHeight = image.getHeight();
                if (opts.inJustDecodeBounds) return null;
                int sample = Integer.highestOneBit(Math.max(1, opts.inSampleSize));
                if (sample > 1) {
                    int w = Math.max(1, image.getWidth() / sample);
                    int h = Math.max(1, image.getHeight() / sample);
                    BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g = scaled.createGraphics();
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(image, 0, 0, w, h, null);
                    g.dispose();
                    image = scaled;
                    opts.outWidth = w;
                    opts.outHeight = h;
                }
            }
            return new Bitmap(image);
        } catch (IOException e) {
            return null;
        }
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return decodeByteArray(data, offset, length, null);
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options opts) {
        return decodeStream(new java.io.ByteArrayInputStream(data, offset, length), null, opts);
    }

    public static Bitmap decodeFile(String pathName) {
        return decodeFile(pathName, null);
    }

    public static Bitmap decodeFile(String pathName, Options opts) {
        try (InputStream in = new java.io.FileInputStream(pathName)) {
            return decodeStream(in, null, opts);
        } catch (IOException e) {
            return null;
        }
    }
}
