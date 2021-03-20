package io.roach.pipe.io;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public abstract class ResourceResolver {
    private ResourceResolver() {
    }

    public static boolean isJdbcUrl(String url) {
        return Stream.of("jdbc:")
                .anyMatch(url::startsWith);
    }

    public static boolean isSupportedUrl(String url) {
        return Stream.of("classpath:", "file:", "http:")
                .anyMatch(url::startsWith);
    }

    public static Resource resolve(String url, Map<String, String> allParams) throws IOException {
        if (url.startsWith("classpath:")) {
            return new ClassPathResource(url.substring("classpath:".length()));
        }
        if (url.startsWith("file:")) {
            return new FileSystemResource(url);
        }
        if (url.startsWith("http:")) {
            return new UrlResource(url);
        }
        throw new IllegalArgumentException("No resource matching scheme: " + url);
    }

}
