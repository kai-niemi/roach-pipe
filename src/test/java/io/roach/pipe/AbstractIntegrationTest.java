package io.roach.pipe;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = Application.class)
@ActiveProfiles({"integrationtest"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractIntegrationTest {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DataSource dataSource;

    protected void executeScripts(String... paths) {
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        Arrays.stream(paths).sequential().forEach(p -> {
            databasePopulator.addScript(new ClassPathResource(p));
        });
        databasePopulator.setCommentPrefix("--");
        databasePopulator.setIgnoreFailedDrops(true);
        DatabasePopulatorUtils.execute(databasePopulator, dataSource);
    }
}
