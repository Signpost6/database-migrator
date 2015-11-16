Place .java migration files to be applied

//Template

package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

public class V1_1__some_migration implements JdbcMigration {
    private static final Logger log = LoggerFactory.getLogger(V1_2__dml_load.class);

    @Override
    public void migrate(Connection conn) throws Exception {
        log.info("Executing Migration ...");
    }
}
