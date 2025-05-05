import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;


public class Log4jLostFilesTest {

    // logs folder(configured in log4j2.xml)
    private static final Path LOG_DIRECTORY = Paths.get("log");
    // writing logs duration
    private static final long SECOND_DURATION = 10;
    // log file contents -> larger size -> higher chance of losing gz files (depends on machine/disk perf)
    private static final String THIS_TEXT_MAKES_GZ_LONGER = "0000".repeat(4096);


    @Test
    public void shouldContainAllLogRecords() throws IOException {
        final long deadline = System.currentTimeMillis() + SECOND_DURATION * 1000;

        try { // cleanup
            FileUtils.forceDelete(LOG_DIRECTORY.toFile());
        } catch (Exception e) {
            System.err.println("it is not possible to delete the folder automatically (you need to delete it manually), cause " + e);
        }

        // initialize here -> logger can block the cleaning process
        final Logger log = org.slf4j.LoggerFactory.getLogger(Log4jLostFilesTest.class);

        long countOfLog4jRecords = 0;
        { // writing logs
            while (deadline > System.currentTimeMillis()) {
                log.info("{}  -  {}", ++countOfLog4jRecords, THIS_TEXT_MAKES_GZ_LONGER);
            }
        }

        long countOfLogFileRecords = 0;
        { // count records in log files
            try (Stream<Path> files = Files.walk(LOG_DIRECTORY)) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    if (Files.isRegularFile(file)) {
                        String fileName = file.toString();
                        if (fileName.endsWith(".log")) {
                            try (BufferedReader reader = Files.newBufferedReader(file)) {
                                countOfLogFileRecords += reader.lines().count();
                            }
                        } else if (fileName.endsWith(".gz")) {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(file))))) {
                                countOfLogFileRecords += reader.lines().count();
                            }
                        }
                    }
                }
            }
        }

        Assert.assertEquals(countOfLog4jRecords,countOfLogFileRecords);
    }
}
