package io.roach.pipe.io;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface RowWriter<T> {
    void write(ResultSet rs, T item) throws IOException, SQLException;
}
