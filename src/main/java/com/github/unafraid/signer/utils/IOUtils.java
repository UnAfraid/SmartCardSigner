package com.github.unafraid.signer.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by UnAfraid on 10.7.2015 г..
 */
public class IOUtils {
    public static String streamToByteArray(InputStream stream) throws IOException {
        final char[] buffer = new char[8192];
        final StringBuilder sb = new StringBuilder();
        try (InputStreamReader in = new InputStreamReader(stream)) {
            while (in.read(buffer, 0, buffer.length) > 0) {
                sb.append(buffer);
            }
        }
        return sb.toString();
    }
}
