package db;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by rdb on 6/3/15.
 */
public class DbManagerTest {

    @Test
    public void shouldParseParametersFromCommandLine() {

    }

//    @Test
    public void shouldParseArgumentsForCommandline() {
        DbMigrate db = new DbMigrate();

        db.parseArgs(Arrays.asList(
//                "--url", "jdbc:oracle:thin:@s:1521:LOCAL",
//                "--username", "rdb",
//                "--password", "rdb",
//                "--schema", "RDB",
//                "--admin-username", "sys as sysdba",
//                "--admin-password", "password",
                "--environment", "dev",
                "info").toArray(new String[0]));
    }

//    @Test
    public void shouldParseFromPropsFile() throws Exception {

        Properties p = new Properties();
        p.setProperty("url", "jdbc:oracle:thin:@localhost:1521:LOCAL");
        p.setProperty("username", "RDB_OWNER");
        p.setProperty("dev.password", "RDB_OWNER");
        p.setProperty("schema", "RDB_OWNER");
        p.setProperty("admin-username", "sys as sysdba");
        p.setProperty("admin-password", "password");
        File tmp = File.createTempFile("prop", "properties");

        p.store(FileUtils.openOutputStream(tmp), null);


        DbMigrate db = new DbMigrate();

        db.parseArgs(Arrays.asList(
                "--props", tmp.getAbsolutePath(),
                "--environment", "dev",
                "info"
        ).toArray(new String[0]));


    }

}