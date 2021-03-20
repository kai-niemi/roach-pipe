package io.roach.pipe.io;

import java.io.IOException;

public interface RowWriter<T> {
    void write(T item) throws IOException;
}
