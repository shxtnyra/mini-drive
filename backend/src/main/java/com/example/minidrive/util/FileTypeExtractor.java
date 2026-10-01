package com.example.minidrive.util;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.InputStream;

@Component
public class FileTypeExtractor {
    private final Tika tika = new Tika();

    public String detectFileType(InputStream inputStream) throws Exception {
        if(inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }

        inputStream.mark(2048);
        String mimeType = tika.detect(inputStream);
        inputStream.reset();

        return mimeType;
    }
}
