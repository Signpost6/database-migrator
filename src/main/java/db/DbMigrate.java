package db;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by rdb on 6/3/15.
 */
public class DbMigrate {
    private static final Logger log = LoggerFactory.getLogger(DbMigrate.class);

    public static void main(String[] args) {
        new DbMigrate().parseArgs(args);
        //Exit cleanly
        System.exit(0);
    }

    void parseArgs(String[] args) {
        DBOptions bean = new DBOptions();

        CmdLineParser parser = new CmdLineParser(bean);
        parser.setUsageWidth(120);
        try {
            parser.parseArgument(args);
            if (!StringUtils.isBlank(bean.props)) {
                Properties p = new Properties();
                p.load(FileUtils.openInputStream(new File(bean.props)));
                bean.parseProps(p);
            } else {
                try {
                    //load the default properties from the archive
                    Properties p = new Properties();
                    p.load(DbMigrate.class.getResourceAsStream("/database.properties"));
                    bean.parseProps(p);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    System.err.println(e.getMessage());
                }
            }
            bean.validate();
            if (bean.getArguments().isEmpty()) {
                throw new Exception("Required Arguments missing (provision|destroy|clean|baseline|migrate|repair)");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("java -jar DbMigrate.jar [options...] arguments...");
            parser.printUsage(System.err);
            System.exit(-1);
        }
        try {
            //order is important
            if (bean.getArguments().contains("destroy")) {
                destroy(bean);
            }
            if (bean.getArguments().contains("provision")) {
                provision(bean);
            }
            if (bean.getArguments().contains("info")) {
                info(bean);
            }
            if (bean.getArguments().contains("clean")) {
                clean(bean);
            }
            if (bean.getArguments().contains("baseline")) {
                baseline(bean);
            }
            if (bean.getArguments().contains("repair")) {
                repair(bean);
            }
            if (bean.getArguments().contains("migrate")) {
                migrate(bean);
            }
        } catch (Exception e) {
            e.printStackTrace();

            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }


    void destroy(DBOptions dbOptions) {

        log.info("Executing 'destroy' phase...");
        Flyway flyway = flyway(dbOptions);
        flyway.clean();

        Connection conn = null;
        Properties connectionProps = new Properties();
        if (!StringUtils.isBlank(dbOptions.getAdminUsername())) {
            connectionProps.put("user", dbOptions.getAdminUsername());
        }
        if (!StringUtils.isBlank(dbOptions.getAdminPassword())) {
            connectionProps.put("password", dbOptions.getAdminPassword());
        }

        try {
            conn = DriverManager.getConnection(
                    dbOptions.getUrl(), connectionProps);

            ClassPathResource classPathResource = new ClassPathResource("/db/destroy.sql");

            log.info("Executing /db/db_destroy.sql script");
            //Make sure we execute as much of this as possible (continue on error)
            ScriptUtils.executeSqlScript(conn,
                    new EncodedResource(classPathResource),
                    true,
                    true,
                    ScriptUtils.DEFAULT_COMMENT_PREFIX,
                    ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
                    ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                    ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
            conn.close();
        } catch (Exception e) {
            if (e.getMessage().contains("script must not be null or empty")) {
                //swallow this one we dont care really if theres nothing in it
            } else {
                throw new RuntimeException(e.getMessage());
            }
//            log.warn(e.getMessage());
        }

    }

    void provision(DBOptions dbOptions) {
//        if (dbOptions.getAdminUsername() == null) {
//            throw new IllegalArgumentException("admin-username parameter required for Provisioning");
//        }
//        if (dbOptions.getAdminPassword() == null) {
//            throw new IllegalArgumentException("admin-password parameter required for Provisioning");
//        }

        log.info("Executing 'provision' phase...");
        Connection conn = null;
        Properties connectionProps = new Properties();
        if (!StringUtils.isBlank(dbOptions.getAdminUsername())) {
            connectionProps.put("user", dbOptions.getAdminUsername());
        }
        if (!StringUtils.isBlank(dbOptions.getAdminPassword())) {
            connectionProps.put("password", dbOptions.getAdminPassword());
        }


        try {
            conn = DriverManager.getConnection(
                    dbOptions.getUrl(), connectionProps);

            ClassPathResource classPathResource = new ClassPathResource("/db/provision.sql");

            log.info("Executing /db/db_provision.sql script");
            ScriptUtils.executeSqlScript(conn, classPathResource);
            conn.close();
        } catch (Exception e) {
            if (e.getMessage().contains("script must not be null or empty")) {
                //swallow this one we dont care really if theres nothing in it
            } else {
                throw new RuntimeException(e.getMessage());
            }
        }
    }


    void clean(DBOptions dbOptions) {
        log.info("Executing clean...");
        Flyway flyway = flyway(dbOptions);
        flyway.clean();

    }

    void baseline(DBOptions dbOptions) {
        log.info("Executing Baseline...");
        Flyway flyway = flyway(dbOptions);
        flyway.baseline();

    }

    void migrate(DBOptions dbOptions) {
        log.info("Executing Migrations...");

        // Create the Flyway instance
        Flyway flyway = flyway(dbOptions);

        // Start the migration
        flyway.migrate();
    }

    void repair(DBOptions dbOptions) {
        log.info("Executing Repair...");
        // Create the Flyway instance
        Flyway flyway = flyway(dbOptions);
        // Start the migration
        flyway.repair();
    }


    void info(DBOptions dbOptions) {
        log.info("Getting DB Info...");
        Flyway flyway = flyway(dbOptions);

        MigrationInfoService inf = flyway.info();
        System.out.println("--- Applied Migrations ---");
        for (MigrationInfo mi : inf.applied()) {
            System.out.println("\t" + mi.getVersion().getVersion() + " " + mi.getDescription());
        }

    }


    Flyway flyway(DBOptions dbOptions) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dbOptions.getUrl(), dbOptions.getUsername(), dbOptions.getPassword());


        //Run the stored procs as well
        flyway.setCallbacks(new FlywayStoredProceduresCallback());
        flyway.setLocations("db/migration");
        flyway.setValidateOnMigrate(true);

        if (!StringUtils.isBlank(dbOptions.getSchema())) {
            flyway.setSchemas(dbOptions.getSchema());
        }

        return flyway;

    }

    public static class DBOptions {
        @Option(name = "--props", usage = "Take parameters from Properties file", required = false)
        private String props;
        @Option(name = "--url", usage = "JDBC Url (e.g. jdbc:<driver>://<host>/<database>)", required = false)
        private String url;
        @Option(name = "--username", usage = "Database username used for Authentication", required = false)
        private String username;
        @Option(name = "--password", usage = "Database password used for Authentication", required = false)
        private String password;
        @Option(name = "--schema", usage = "Schema to use on datasource", required = false)
        private String schema;
        @Option(name = "--admin-username", usage = "Admin username to supply for datasource (used for init and drop to create database local dev only)", required = false)
        private String adminUsername;
        @Option(name = "--admin-password", usage = "Admin username to supply for datasource (used for init and drop to create database local dev only)", required = false)
        private String adminPassword;
        @Option(name = "--environment", usage = "Which environment properties to use from the props file", required = false)
        private String environment;

        @Argument(usage = "drop - Drop the entire schema from database\nclean - Cleans the contents of the database leaving an empty database\ninit - Create new Database, Schema, Users and Tablespaces\nmigrate - Runs database migrations and packages")
        private List<String> arguments = new ArrayList<String>();


        public void parseProps(Properties p) throws Exception {
            //dont override any values that were passed in as parameters
            if (StringUtils.isBlank(this.url)) {
                this.url = getPropertyValue(p, "database.url");
            }
            if (StringUtils.isBlank(this.username)) {
                this.username = getPropertyValue(p, "database.username");
            }
            if (StringUtils.isBlank(this.password)) {
                this.password = getPropertyValue(p, "database.password");
            }
            if (StringUtils.isBlank(this.schema)) {
                this.schema = getPropertyValue(p, "database.schema");
            }
            if (StringUtils.isBlank(this.adminUsername)) {
                this.adminUsername = getPropertyValue(p, "database.admin-username");
            }
            if (StringUtils.isBlank(this.adminPassword)) {
                this.adminPassword = getPropertyValue(p, "database.admin-password");
            }
        }

        String getPropertyValue(Properties p, String prop) {
            String prefix = "";
            if (!StringUtils.isBlank(environment)) {
                prefix = environment + ".";
            }
            String value = p.getProperty(prefix + prop);
            if (StringUtils.isBlank(value)) {
                //default to the non environment variable
                value = p.getProperty(prop);
            }
            return value;
        }

        public void validate() {
            Validate.notBlank(url, "URL cannot be empty");
//            Validate.notBlank(username, "username cannot be empty");
//            Validate.notBlank(password, "password cannot be empty");
//            Validate.notBlank(schema, "schema cannot be empty");

        }

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }

        public List<String> getArguments() {
            return arguments;
        }

        public void setArguments(List<String> arguments) {
            this.arguments = arguments;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }
    }
}
