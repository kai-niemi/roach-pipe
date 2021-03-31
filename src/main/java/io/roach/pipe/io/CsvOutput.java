package io.roach.pipe.io;

import java.io.Closeable;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Locale;

import org.springframework.util.Assert;

/**
 * CSV output formatter and writer.
 * <p>
 * https://en.wikipedia.org/wiki/Comma-separated_values
 */
public class CsvOutput implements RowWriter<List<Object>>, Closeable {
    private static final String ESCAPE_REGEX = ".*[,\"].*";

    private final PrintWriter writer;

    private String delimiter = ",";

    private String quoteChar = "\"";

    private String escapeChar = "\"\"";

    private boolean printHeader = false;

    private boolean printQuotes = false;

    public CsvOutput(PrintWriter writer) {
        Assert.notNull(writer, "writer is null");
        this.writer = writer;
    }

    public CsvOutput setDelimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    public CsvOutput setQuoteChar(String quoteChar) {
        this.quoteChar = quoteChar;
        return this;
    }

    public CsvOutput setPrintQuotes(boolean printQuotes) {
        this.printQuotes = printQuotes;
        return this;
    }

    public CsvOutput setEscapeChar(String escapeChar) {
        this.escapeChar = escapeChar;
        return this;
    }

    public CsvOutput setPrintHeader(boolean printHeader) {
        this.printHeader = printHeader;
        return this;
    }

    @Override
    public void write(ResultSet rs, List<Object> items) throws SQLException {
        if (printHeader) {
            printHeader = false;
            writeHeader(rs.getMetaData());
        }

        boolean first = true;

        ResultSetMetaData metaData = rs.getMetaData();

        int row = 1;
        for (Object item : items) {
            if (!first) {
                writer.write(delimiter);
            } else {
                first = false;
            }

            int type = metaData.getColumnType(row++);
            switch (type) {
                case Types.ARRAY:
                case Types.BIGINT:
                case Types.VARCHAR:
                case Types.BINARY:
                case Types.BIT:
                case Types.BLOB:
                case Types.STRUCT:
            }

            if (item instanceof byte[]) {
                if (printQuotes) {
                    writer.print(quoteChar);
                }
                writer.printf(Locale.US, "\\x%s", new String(Hex.encode((byte[]) item)));
                if (printQuotes) {
                    writer.print(quoteChar);
                }
            } else if (item != null) {
                if (printQuotes) {
                    writer.print(quoteChar);
                }
                String str = item.toString();
                if (str.matches(ESCAPE_REGEX)) {
                    writer.print(quoteChar);
                }
                writer.printf(Locale.getDefault(), str.replaceAll("\"", escapeChar));
                if (str.matches(ESCAPE_REGEX)) {
                    writer.print(quoteChar);
                }
                if (printQuotes) {
                    writer.print(quoteChar);
                }
            } else {
                writer.print("");
            }
        }

        writer.println();
    }

    private void writeHeader(ResultSetMetaData metaData) throws SQLException {
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (i > 1) {
                writer.printf(delimiter);
            }
            writer.printf(metaData.getColumnName(i));
        }
        writer.println();
    }

    @Override
    public void close() {
        writer.close();
    }
}

