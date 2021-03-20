package io.roach.pipe.io;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.lang.Nullable;

public class JdbcCursorReader {
    private static final int UNDEFINED = -1;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private DataSource dataSource;

    private String query;

    private int fetchSize = UNDEFINED;

    private int rowOffset = 0;

    private int maxRows = UNDEFINED;

    private int queryTimeout = UNDEFINED;

    private boolean ignoreWarnings = true;

    private SQLExceptionTranslator exceptionTranslator;

    public JdbcCursorReader() {
    }

    public JdbcCursorReader setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public JdbcCursorReader setQuery(String sql) {
        this.query = sql;
        return this;
    }

    public JdbcCursorReader setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
        return this;
    }

    public JdbcCursorReader setRowOffset(int rowOffset) {
        this.rowOffset = rowOffset;
        return this;
    }

    public JdbcCursorReader setMaxRows(int maxRows) {
        this.maxRows = maxRows;
        return this;
    }

    public JdbcCursorReader setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
        return this;
    }

    public JdbcCursorReader setIgnoreWarnings(boolean ignoreWarnings) {
        this.ignoreWarnings = ignoreWarnings;
        return this;
    }

    public JdbcCursorReader setExceptionTranslator(
            SQLExceptionTranslator exceptionTranslator) {
        this.exceptionTranslator = exceptionTranslator;
        return this;
    }

    @Nullable
    public <T> void read(RowMapper<T> rowMapper, RowWriter<T> writer) {
        if (maxRows != UNDEFINED && rowOffset >= maxRows) {
            return;
        }

        if (this.exceptionTranslator == null) {
            this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
        }

        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = openCursor(connection);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            handleWarnings(preparedStatement);

            if (rowOffset > 0) {
                skipToRow(resultSet, rowOffset);
            }

            while (resultSet.next()) {
                rowOffset++;
                T item = rowMapper.mapRow(resultSet, this.rowOffset);
                if (this.rowOffset != resultSet.getRow()) {
                    throw new IllegalStateException("Unexpected cursor position");
                }
                writer.write(item);
            }

            DataSourceUtils.releaseConnection(connection, dataSource);
        } catch (SQLException ex) {
            throw exceptionTranslator.translate("Executing query", this.query, ex);
        } catch (IOException ex) {
            throw new DataAccessResourceFailureException("I/O error", ex);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        connection.setAutoCommit(true);
        connection.setReadOnly(true);
        return connection;
    }

    private PreparedStatement openCursor(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection
                .prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY,
                        ResultSet.HOLD_CURSORS_OVER_COMMIT);
        if (this.fetchSize != UNDEFINED) {
            preparedStatement.setFetchSize(this.fetchSize);
            preparedStatement.setFetchDirection(ResultSet.FETCH_FORWARD);
        }
        if (this.maxRows != UNDEFINED) {
            preparedStatement.setMaxRows(this.maxRows);
        }
        if (this.queryTimeout != UNDEFINED) {
            preparedStatement.setQueryTimeout(this.queryTimeout);
        }
        return preparedStatement;
    }

    private void skipToRow(ResultSet resultSet, int row) {
        try {
            resultSet.absolute(row);
        } catch (SQLException e) {
            logger.warn("The JDBC driver does not support ResultSet.absolute(), fast forwarding instead", e);
            try {
                int count = 0;
                while (row != count && resultSet.next()) {
                    count++;
                }
            } catch (SQLException se) {
                throw exceptionTranslator
                        .translate("Attempted to forward result to row number", this.query, se);
            }
        }
    }

    private void handleWarnings(Statement statement) throws SQLWarningException, SQLException {
        SQLWarning warning = statement.getWarnings();
        if (ignoreWarnings) {
            while (warning != null) {
                logger.warn("SQLWarning ignored: SQL state '{}', error code '{}', message [{}]",
                        warning.getSQLState(), warning.getErrorCode(), warning.getMessage());
                warning = warning.getNextWarning();
            }
        } else {
            if (warning != null) {
                throw new SQLWarningException("Warning not ignored", warning);
            }
        }
    }
}
