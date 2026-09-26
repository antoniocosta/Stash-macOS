package androidx.media3.datasource;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.DataReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Desktop port of media3 1.9.2 {@code DataSource}. Java (not Kotlin) because upstream both
 * overrides {@code getUri()} as a function and reads it as the synthetic property {@code .uri},
 * which only a Java getter allows.
 */
public interface DataSource extends DataReader {

    /** A factory for {@link DataSource} instances. */
    interface Factory {
        DataSource createDataSource();
    }

    void addTransferListener(TransferListener transferListener);

    long open(DataSpec dataSpec) throws IOException;

    @Nullable
    Uri getUri();

    default Map<String, List<String>> getResponseHeaders() {
        return Collections.emptyMap();
    }

    void close() throws IOException;
}
