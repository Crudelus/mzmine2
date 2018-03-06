package net.sf.mzmine.modules.masslistmethods.exporter;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.Scan;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.CREATE;


public class TSDataPointFileWriter implements DataPointFileWriter{

    public static final Charset CHARSET = Charset.defaultCharset();
    public static final String FILE_ENDING = ".txt";
    public static final String SCAN = "scan";
    public static final String LINE_SEPERATOR = "\r\n";
    private final Scan scan;
    private final Path target;
    private final DataPoint[] dataPoints;
    private final String originalFileName;
    private int peakInScan = 1;

    public TSDataPointFileWriter(Scan scan, Path path, MassList massList) {
        this.scan = scan;

        this.dataPoints = massList.getDataPoints();
        this.originalFileName = path.getFileName().toString();
        this.target = path.resolve(originalFileName + "_" + SCAN + scan.getScanNumber() + FILE_ENDING).toAbsolutePath();
        if (Files.exists(target)) {
            try {
                Files.delete(target);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write() throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(target,
                CHARSET,
                CREATE)) {
            out.write("Scan\tPeak\tm/z\tRel.Intensity\tAbs.Intensity"
                    + LINE_SEPERATOR + "-------------" + LINE_SEPERATOR);

            double highestIntensity = Objects.requireNonNull(scan.getHighestDataPoint()).getIntensity();
            double relIntensity;
            for (DataPoint dataPoint : dataPoints) {
                relIntensity = calcRelativeIntensity(highestIntensity, dataPoint);
                String row = String.format("%1d\t%2d\t%3f\t%4f\t%5f" + LINE_SEPERATOR,
                        scan.getScanNumber(),
                        peakInScan++,
                        dataPoint.getMZ(),
                        relIntensity,
                        dataPoint.getIntensity());
                out.write(row);
            }
        }
    }

    private double calcRelativeIntensity(double highestIntensity, DataPoint dataPoint) {
        double relIntensity;
        if (highestIntensity != 0) {
            relIntensity = dataPoint.getIntensity() / highestIntensity;
        } else {
            relIntensity = 0;
        }
        return relIntensity;
    }
}
