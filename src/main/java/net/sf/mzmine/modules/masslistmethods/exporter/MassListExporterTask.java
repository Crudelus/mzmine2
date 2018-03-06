package net.sf.mzmine.modules.masslistmethods.exporter;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.CREATE;

public class MassListExporterTask extends AbstractTask {


    private Logger LOG = LoggerFactory.getLogger(MassListExporterTask.class);

    private final RawDataFile dataFile;
    private final String massListName;
    private final ScanSelection scanSelection;

    private int totalScans;
    private int processedScans;
    private Path path;


    public MassListExporterTask(RawDataFile matchingRawDataFiles, ParameterSet parameterSet) {
        this.dataFile = matchingRawDataFiles;
        this.massListName = parameterSet.getParameter(MassListExporterParameters.massList).getValue();
        scanSelection = parameterSet.getParameter(MassListExporterParameters.scanSelection).getValue();
        File targetDirectory = parameterSet.getParameter(MassListExporterParameters.targetDirectory).getValue();
        int indexOfFileEnding = dataFile.getName().lastIndexOf(".");
        String name = dataFile.getName().substring(0 , indexOfFileEnding);
        path = Paths.get(targetDirectory.getPath());
        path = path.resolve(name);
    }

    @Override
    public String getTaskDescription() {
        return "Export MassList " + massListName + " for " + dataFile.getName();
    }

    @Override
    public double getFinishedPercentage() {
        return processedScans == 0 ? 0 : (double) processedScans / totalScans;
    }

    @Override
    public void run() {
        setStatus(TaskStatus.PROCESSING);
        LOG.info("Starting processing of task {}, status {}", getTaskDescription(), getStatus());
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scan[] scans = scanSelection.getMatchingScans(dataFile);
        totalScans = scans.length;
        for (Scan currentScan : scans) {
            MassList massList = currentScan.
                    getMassList(massListName);
            Objects.requireNonNull(massList);
            DataPointFileWriter fileExporter = new TSDataPointFileWriter(currentScan, path, massList);
            try {
                fileExporter.write();
            } catch (IOException e) {
                e.printStackTrace();
                setStatus(TaskStatus.ERROR);
                final String msg = "Could not access file for scan #"
                        + currentScan.getScanNumber() + "\n"
                        + e.getLocalizedMessage();
                setErrorMessage(msg);
                return;
            }
            processedScans++;
        }
        setStatus(TaskStatus.FINISHED);
    }
}
