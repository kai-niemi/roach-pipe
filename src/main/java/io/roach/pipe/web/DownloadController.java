package io.roach.pipe.web;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.roach.pipe.config.DataSourceFactory;
import io.roach.pipe.io.JdbcCursorReader;
import io.roach.pipe.io.ResourceResolver;

@RestController
@RequestMapping("/download")
public class DownloadController {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @GetMapping
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam Map<String, String> allParams)
            throws IOException {
        final String url = allParams.get("url");
        if (url == null) {
            throw new BadRequestException("Missing required param [url]");
        }

        final StreamingResponseBody responseBody;
        if (ResourceResolver.isJdbcUrl(url)) {
            responseBody = copyQueryResult(allParams);
        } else if (ResourceResolver.isSupportedUrl(url)) {
            responseBody = copyInputStream(ResourceResolver.resolve(url, allParams));
        } else {
            throw new BadRequestException("Unsupported url: " + url);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(responseBody);
    }

    private StreamingResponseBody copyInputStream(Resource input) {
        logger.info("Copying from [{}]", input.getFilename());
        return outputStream -> {
            try (InputStream in = input.getInputStream()) {
                FileCopyUtils.copy(in, new BufferedOutputStream(outputStream));
            }
        };
    }

    private final String quoteChar = "\"";

    private final String escapeChar = "\"\"";

    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    private StreamingResponseBody copyQueryResult(Map<String, String> allParams) {
        final String url = allParams.get("url");
        final int maxRows = Integer.parseInt(allParams.getOrDefault("maxRows", "-1"));
        final int rowOffset = Integer.parseInt(allParams.getOrDefault("rowOffset", "0"));
        final int fetchSize = Integer.parseInt(allParams.getOrDefault("fetchSize", "256"));

        final String query;
        if (allParams.containsKey("query")) {
            query = allParams.get("query");
        } else {
            final String table = allParams.get("table");
            if (table == null) {
                throw new BadRequestException("Missing both [table] and [query]");
            }
            query = "select * from " + table;
        }

        final DataSource dataSource = dataSourceCache
                .computeIfAbsent(url, k -> dataSourceFactory.createDataSource(allParams));

        logger.info("Connecting to source database [{}] to copy [{}] from offset {} to limit {} with fetch size {}",
                dataSourceFactory.databaseVersion(dataSource), query, rowOffset, maxRows, fetchSize);

        final JdbcCursorReader reader = new JdbcCursorReader()
                .setDataSource(dataSource)
                .setQuery(query)
                .setRowOffset(rowOffset)
                .setMaxRows(maxRows)
                .setFetchSize(fetchSize);

        return outputStream -> {
            final OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outputStream));
            reader.read((rs, rowNum) -> {
                // From the source DB
                List<Object> fields = new ArrayList<>();
                int cols = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= cols; i++) {
                    fields.add(rs.getObject(i));
                }
                return fields;
            }, values -> {
                // To the target DB
                boolean first = true;
                for (Object field : values) {
                    if (!first) {
                        writer.write(",");
                    } else {
                        first = false;
                    }
                    if (field instanceof String) {
                        String str = ((String) field);
                        if (str.matches(".*[,\"].*")) {
                            writer.write(quoteChar);
                        }
                        writer.write(str.replaceAll("\"", escapeChar));
                        if (str.matches(".*[,\"].*")) {
                            writer.write(quoteChar);
                        }
                    } else {
                        writer.write(String.valueOf(field));
                    }
                }
                writer.write("\n");
            });

            writer.flush();
        };
    }
}
