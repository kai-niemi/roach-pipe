package io.roach.pipe.web;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.roach.pipe.config.DataSourceFactory;
import io.roach.pipe.io.CsvOutput;
import io.roach.pipe.io.JdbcCursorReader;
import io.roach.pipe.io.ResourceResolver;

@RestController
public class CopyController {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @GetMapping(value = {"/copy", "/download"})
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

    private String toDataSourceKey(String url) {
        if (url.startsWith("jdbc:")) {
            URI u = URI.create(url.substring(5));
            return u.getScheme() + ":" + u.getHost() + ":" + u.getPort();
        }
        return url;
    }

    private int toNumber(String numStr) {
        return Integer.parseInt(numStr.replace("_", ""));
    }

    private StreamingResponseBody copyQueryResult(Map<String, String> allParams) {
        final String url = allParams.get("url");
        final int maxRows = toNumber(allParams.getOrDefault("maxRows", "-1"));
        final int rowOffset = toNumber(allParams.getOrDefault("rowOffset", "0"));
        final int fetchSize = toNumber(allParams.getOrDefault("fetchSize", "256"));

        final String delimiter = allParams.getOrDefault("delimiter", ",");
        final String quoteChar = allParams.getOrDefault("quoteChar", "\"");
        final String escapeChar = allParams.getOrDefault("escapeChar", "\"\"");
        final boolean printHeader = Boolean.parseBoolean(allParams.getOrDefault("printHeader", "false"));
        final boolean printQuotes = Boolean.parseBoolean(allParams.getOrDefault("printQuotes", "false"));

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
                .computeIfAbsent(toDataSourceKey(url), k -> dataSourceFactory.createDataSource(allParams));

        logger.info("Connecting to source database [{}] to copy [{}] from offset {} to limit {} with fetch size {}",
                dataSourceFactory.databaseVersion(dataSource), query, rowOffset, maxRows, fetchSize);

        final JdbcCursorReader reader = new JdbcCursorReader()
                .setDataSource(dataSource)
                .setQuery(query)
                .setRowOffset(rowOffset)
                .setMaxRows(maxRows)
                .setFetchSize(fetchSize);

        return outputStream -> {
            final CsvOutput csvOutput = new CsvOutput(
                    new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(outputStream))))
                    .setDelimiter(delimiter)
                    .setEscapeChar(escapeChar)
                    .setPrintHeader(printHeader)
                    .setPrintQuotes(printQuotes)
                    .setQuoteChar(quoteChar);

            reader.read((rs, rowNum) -> {
                List<Object> fields = new ArrayList<>();
                int cols = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= cols; i++) {
                    fields.add(rs.getObject(i));
                }
                return fields;
            }, csvOutput);

            csvOutput.close();
        };
    }
}
