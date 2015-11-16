package db;


import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;

/**
 * Created by rdb on 6/3/15.
 */
public class FlywayStoredProceduresCallback implements FlywayCallback {
    private static final Logger log = LoggerFactory.getLogger(FlywayStoredProceduresCallback.class);
    private static final String DB_PACKAGES_PATH = "db/packages";

    @Override
    public void beforeClean(Connection connection) {
        executeScript(connection, "/db/beforeClean.sql");
    }

    @Override
    public void afterClean(Connection connection) {
        executeScript(connection, "/db/afterClean.sql");
    }

    @Override
    public void beforeMigrate(Connection connection) {
        log.debug("Applying Pre-Migration Stored Procedures from {} ...", DB_PACKAGES_PATH + "/pre");
        applyStoredProcedures(connection,  "pre");

        executeScript(connection, "/db/beforeMigrate.sql");

    }

    @Override
    public void afterMigrate(Connection connection) {
        log.debug("Applying Post-Migration Stored Procedures from {} ...", DB_PACKAGES_PATH + "/post");
        applyStoredProcedures(connection, "post");

        executeScript(connection, "/db/afterMigrate.sql");

    }

    @Override
    public void beforeEachMigrate(Connection connection, MigrationInfo migrationInfo) {
        executeScript(connection, "/db/beforeEachMigrate.sql");

    }

    @Override
    public void afterEachMigrate(Connection connection, MigrationInfo migrationInfo) {
        executeScript(connection, "/db/afterEachMigrate.sql");

    }

    @Override
    public void beforeValidate(Connection connection) {
        executeScript(connection, "/db/beforeValidate.sql");

    }

    @Override
    public void afterValidate(Connection connection) {
        executeScript(connection, "/db/afterValidate.sql");

    }

    @Override
    public void beforeBaseline(Connection connection) {
        executeScript(connection, "/db/beforeBaseline.sql");
    }

    @Override
    public void afterBaseline(Connection connection) {
        executeScript(connection, "/db/afterBaseline.sql");
    }

    @Override
    public void beforeInit(Connection connection) {
//        Will be deprecated baseline is used
    }

    @Override
    public void afterInit(Connection connection) {
//        Will be deprecated baseline is used
    }

    @Override
    public void beforeRepair(Connection connection) {
        executeScript(connection, "/db/beforeRepair.sql");

    }

    @Override
    public void afterRepair(Connection connection) {
        executeScript(connection, "/db/afterRepair.sql");

    }

    @Override
    public void beforeInfo(Connection connection) {
        executeScript(connection, "/db/beforeInfo.sql");

    }

    @Override
    public void afterInfo(Connection connection) {
        executeScript(connection, "/db/afterInfo.sql");

    }

    void executeScript(Connection connection, String script) {
        try {
            ClassPathResource classPathResource = new ClassPathResource(script);
            ScriptUtils.executeSqlScript(connection, classPathResource);
            log.info("Successfully executed script - {}", script);
        } catch (Exception e) {
            if (ExceptionUtils.getRootCause(e) instanceof FileNotFoundException) {
                //Skip it if it doesnt exist
                return;
            }
            throw new RuntimeException(e.getMessage() + "(" + script +")");
        }
    }

    void applyStoredProcedures_old(Connection connection, String path) {

        Resource packageFolder = null;
        try {
            packageFolder = new ClassPathResource(DB_PACKAGES_PATH + "/" + path);
        } catch (Exception e) {
            log.info("Could not locate any Package resources in the " + DB_PACKAGES_PATH + "/" + path + " path or the " + DB_PACKAGES_PATH + "/" + path + " path does not exist");
            return;
        }
        File f = null;

        try {
            List<File> files = new ArrayList<File>(
                    FileUtils.listFiles(packageFolder.getFile(), new String[]{"sql"}, true));

            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File file, File t1) {
                    return file.getName().compareTo(t1.getName());
                }
            });
            for (File file : files) {
                f = file;
                log.info("Executing [{}]", file.getAbsolutePath());
//                ScriptUtils.executeSqlScript(connection, new FileSystemResource(file));
                Statement st = connection.createStatement();
                st.execute(FileUtils.readFileToString(file));
                if (st.getWarnings() != null) {
                    StringBuilder sb = new StringBuilder();
//                    sb.append("Stored Procedure produce warnings:").append("\n");
                    SQLWarning warn = st.getWarnings();
                    while (warn != null) {
                        sb.append(warn.getMessage());
                        sb.append("\n");
                        warn = warn.getNextWarning();
                    }
                    throw new SQLException(sb.toString().trim());
                }
            }
        } catch (FileNotFoundException fileNotFoundException) {
            log.info("Could not locate any Package resources in the " + DB_PACKAGES_PATH + "/" + path + " path or the " + DB_PACKAGES_PATH + "/" + path + " path does not exist");
            return;
        } catch (Exception e) {
            log.error("Failed to Apply Packages - {}", f);

            log.debug(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    void applyStoredProcedures(Connection connection, String path) {
        String fullPath = DB_PACKAGES_PATH + "/" + path;

        log.debug("Applying Stored Procedures from {} as Pre Migration Task...", fullPath);
        List<String> files = new ArrayList<String>();

        try {
            for (String s : ClassPathUtils.getResourceListing(FlywayStoredProceduresCallback.class, fullPath)) {
                if (s.endsWith(".sql")) {
                    files.add("/" + s);
                }
            }
        } catch (Exception e) {
            log.info("Could not locate any Package resources in the " + fullPath + " path or the " + fullPath + " path does not exist");
            return;
        }
        File f = null;

        Collections.sort(files);

        try {

            for (String file : files) {
//                ScriptUtils.executeSqlScript(connection, new FileSystemResource(file));
                Statement st = connection.createStatement();
                st.execute(org.apache.commons.io.IOUtils.toString(FlywayStoredProceduresCallback.class.getResourceAsStream(file)));
                if (st.getWarnings() != null) {
                    StringBuilder sb = new StringBuilder();
//                    sb.append("Stored Procedure produce warnings:").append("\n");
                    SQLWarning warn = st.getWarnings();
                    while (warn != null) {
                        sb.append(warn.getMessage());
                        sb.append("\n");
                        warn = warn.getNextWarning();
                    }
                    throw new SQLException(sb.toString().trim());
                }
                log.info("Successfully Applied  - {}", file);
            }
        } catch (FileNotFoundException fileNotFoundException) {
            log.info("Could not locate any Package resources in the " + fullPath + " path or the " + fullPath + " path does not exist");
            return;
        } catch (Exception e) {
            log.error("Failed to Apply - {}", f);

            log.debug(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
