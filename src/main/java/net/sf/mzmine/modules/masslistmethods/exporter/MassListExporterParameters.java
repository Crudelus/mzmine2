package net.sf.mzmine.modules.masslistmethods.exporter;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import net.sf.mzmine.util.ExitCode;

public class MassListExporterParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final DirectoryParameter targetDirectory = new DirectoryParameter("Mass List Target Folder",
            "The Mass Lists will be stored in the selected folder");

    public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
            new ScanSelection(1));

    public static final MassListParameter massList = new MassListParameter("masses");


    public MassListExporterParameters() {
        super(new Parameter[] { dataFiles, scanSelection, massList, targetDirectory });
   }
}
