package io.github.spirin87.bitbucket_backup;

import java.io.File;

import org.apache.log4j.Logger;

/**
 * @author spirin87@gmail.com
 * <p>
 * Nov 8, 2015, 10:37:48 AM
 */
public class Launcher {

    private static final Logger log = Logger.getLogger(Launcher.class);

    public static void main(String[] args) {
        if (args.length != 3) {
            log.error("arguments: <user> <password> <folder>");
            System.exit(1);
        }
        File folder = new File(args[2]);
        if (!folder.exists() || !folder.isDirectory()) {
            log.error(args[3] + " folder is not exists");
            System.exit(1);
        }
        Worker worker = new Worker(args[0], args[1], folder);
        try {
            worker.cloneRepos();
        } catch (Throwable t) {
            log.error("fatal error:", t);
            System.exit(1);
        }
    }
}
