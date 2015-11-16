package db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by rdb on 7/30/15.
 */
public class ClassPathUtils {
    private static final Logger log = LoggerFactory.getLogger(ClassPathUtils.class);

    public static String[] getResourceListing(Class clazz, String path) throws URISyntaxException, IOException {
        log.debug("Looking for resources in path - {}", path);
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
        /* A file path: easy enough */
            String[] l = new File(dirURL.toURI()).list();

            String[] r = new String[l.length];
            for (int x=0; x< l.length; x++) {
                r[x] = path + "/" + l[x];
            }

            return r;
        }

        if (dirURL == null) {
        /*
         * In case of a jar file, we can't actually find a directory.
         * Have to assume the same jar as clazz.
         */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        if (dirURL.getProtocol().equals("jar")) {
        /* A JAR path */
            String jarPath = dirURL.getPath().substring(0, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            URL jar = new URL(jarPath);

            ZipInputStream zip = new ZipInputStream(jar.openStream());
            ZipEntry ze = null;
            Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
            while ((ze = zip.getNextEntry()) != null) {
                String entryName = ze.getName();

                if (entryName.startsWith(path) && entryName.endsWith("/") == false) { //filter according to the path
                    log.info("Adding Entry - {}", entryName);
                    result.add(entryName);
                }
            }

            return result.toArray(new String[result.size()]);
        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }
}
