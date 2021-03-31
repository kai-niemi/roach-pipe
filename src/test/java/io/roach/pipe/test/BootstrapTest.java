package io.roach.pipe.test;

import org.junit.jupiter.api.Test;

import io.roach.pipe.AbstractIntegrationTest;

public class BootstrapTest extends AbstractIntegrationTest {
    @Test
    public void createSchema() {
        executeScripts("db/drop.sql", "db/psql-create.sql");
    }
}
