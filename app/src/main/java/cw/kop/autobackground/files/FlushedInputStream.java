package cw.kop.autobackground.files;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by TheKeeperOfPie on 4/12/2015.
 */
public class FlushedInputStream extends FilterInputStream {

    public FlushedInputStream(InputStream inputStream) {
        super(inputStream);
    }

    @Override
    public long skip(long n) throws IOException {
        long totalBytesSkipped = 0L;
        while (totalBytesSkipped < n) {
            long bytesSkipped = in.skip(n - totalBytesSkipped);
            if (bytesSkipped == 0L) {
                int readByte = read();
                if (readByte < 0) {
                    break;  // we reached EOF
                }
                else {
                    bytesSkipped = 1; // we read one byte
                }
            }
            totalBytesSkipped += bytesSkipped;
        }
        return totalBytesSkipped;
    }
}