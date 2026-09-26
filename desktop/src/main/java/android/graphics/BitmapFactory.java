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

    public static Bitmap decodeStream(InputStream is) {
        if (is == null) return null;
        try {
            BufferedImage image = ImageIO.read(is);
            return image == null ? null : new Bitmap(image);
        } catch (IOException e) {
            return null;
        }
    }
}
