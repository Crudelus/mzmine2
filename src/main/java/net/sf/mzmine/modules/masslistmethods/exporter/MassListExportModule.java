package net.sf.mzmine.modules.masslistmethods.exporter;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

import javax.annotation.Nonnull;
import java.util.Collection;

public class MassListExportModule implements MZmineProcessingModule {

    @Nonnull
    @Override
    public String getDescription() {
        return "Will write all Mass Lists into CSV Files";
    }

    @Nonnull
    @Override
    public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {
        RawDataFile[] matchingRawDataFiles = parameters
                .getParameter(MassListExporterParameters.dataFiles)
                .getValue()
                .getMatchingRawDataFiles();

        for (RawDataFile dataFile : matchingRawDataFiles) {
            MassListExporterTask massListExporterTask = new MassListExporterTask(
                    dataFile, parameters.cloneParameterSet());
            tasks.add(massListExporterTask);
        }

        return ExitCode.OK;
    }

    @Nonnull
    @Override
    public MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.MASSLISTEXPORT;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Export Mass List";
    }

    @Nonnull
    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
        return MassListExporterParameters.class;
    }
}
