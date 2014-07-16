/*
 * Copyright 2006-2014 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.peaklistmethods.identification.formulapredictionpeaklist;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.data.*;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.FormulaGenerator;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.ResultFormula;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementRule;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScore;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.Range;
import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaPredictionPeakListTask extends AbstractTask {

    private List<ResultFormula> ResultingFormulas;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private Range massRange;
    private ElementRule elementRules[];
    private FormulaGenerator generator;
    private int foundFormulas = 0;
    private IonizationType ionType;
    private double searchedMass;
    private int charge;
    private PeakList peakList;
    private boolean checkIsotopes, checkMSMS, checkRatios, checkRDBE;
    private ParameterSet isotopeParameters, msmsParameters, ratiosParameters,
            rdbeParameters;
    private MZTolerance mzTolerance;
    private String elements;
    private String message;
    private float progress = 0.0f;

    /**
     *
     * @param parameters
     * @param peakList
     * @param peakListRow
     * @param peak
     */
    FormulaPredictionPeakListTask(PeakList peakList, ParameterSet parameters) {

        /*
         * searchedMass = parameters.getParameter(
         * FormulaPredictionPeakListParameters.neutralMass).getValue();
         */
        this.peakList = peakList;
        charge = parameters.getParameter(
                FormulaPredictionPeakListParameters.charge).getValue();
        ionType = (IonizationType) parameters.getParameter(
                FormulaPredictionPeakListParameters.ionization).getValue();
        mzTolerance = parameters.getParameter(
                FormulaPredictionPeakListParameters.mzTolerance).getValue();
        elements = parameters.getParameter(
                FormulaPredictionPeakListParameters.elements).getValue();

        checkIsotopes = parameters.getParameter(
                FormulaPredictionPeakListParameters.isotopeFilter).getValue();
        isotopeParameters = parameters.getParameter(
                FormulaPredictionPeakListParameters.isotopeFilter).getEmbeddedParameters();

        checkMSMS = parameters.getParameter(
                FormulaPredictionPeakListParameters.msmsFilter).getValue();
        msmsParameters = parameters.getParameter(
                FormulaPredictionPeakListParameters.msmsFilter).getEmbeddedParameters();

        checkRDBE = parameters.getParameter(
                FormulaPredictionPeakListParameters.rdbeRestrictions).getValue();
        rdbeParameters = parameters.getParameter(
                FormulaPredictionPeakListParameters.rdbeRestrictions).getEmbeddedParameters();

        checkRatios = parameters.getParameter(
                FormulaPredictionPeakListParameters.elementalRatios).getValue();
        ratiosParameters = parameters.getParameter(
                FormulaPredictionPeakListParameters.elementalRatios).getEmbeddedParameters();
        
        message = "Formula Prediction";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        return progress;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return message;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);

        double total = peakList.getNumberOfRows();
        double done = 0.0;
        for (PeakListRow row : peakList.getRows()) {
            if (row.getPeakIdentities().length > 0) {
                continue;
            }

            this.ResultingFormulas = new ArrayList<ResultFormula>();

            this.searchedMass = (row.getAverageMZ() - ionType.getAddedMass()) * charge;

            message = "Formula prediction for " + MZmineCore.getConfiguration().getMZFormat().format(searchedMass);

            Set<ElementRule> rulesSet = new HashSet<ElementRule>();

            massRange = mzTolerance.getToleranceRange(searchedMass);


            String elementsArray[] = elements.split(",");
            for (String elementEntry : elementsArray) {

                try {
                    ElementRule rule = new ElementRule(elementEntry);

                    // We can ignore elements with max 0 atoms
                    if (rule.getMaxCount() == 0) {
                        continue;
                    }

                    // Adjust the maximum numbers according to the mass we are
                    // searching
                    int maxCountAccordingToMass = (int) (massRange.getMax() / rule.getMass());
                    if (rule.getMaxCount() > maxCountAccordingToMass) {
                        rule.setMaxCount(maxCountAccordingToMass);
                    }

                    rulesSet.add(rule);

                } catch (IllegalArgumentException e) {
                    logger.log(Level.WARNING, "Invald element rule format", e);
                    continue;
                }

            }

            elementRules = rulesSet.toArray(new ElementRule[0]);




            IsotopePattern detectedPattern = row.getBestIsotopePattern();
            if ((checkIsotopes) && (detectedPattern == null)) {
                final String msg = "Cannot calculate isotope pattern scores, because selected"
                        + " peak does not have any isotopes. Have you run the isotope peak grouper?";
                MZmineCore.getDesktop().displayMessage(msg);
            }

            generator = new FormulaGenerator(massRange, elementRules);

            while (true) {

                if (isCanceled()) {
                    return;
                }

                MolecularFormula cdkFormula = generator.getNextFormula();

                if (cdkFormula == null) {
                    break;
                }

                // Mass is ok, so test other constraints
                checkConstraints(cdkFormula, row);

            }

            if (isCanceled()) {
                break;
            }

            for (ResultFormula f : this.ResultingFormulas) {
                SimplePeakIdentity newIdentity = new SimplePeakIdentity(
                        f.getFormulaAsString());
                row.addPeakIdentity(newIdentity, false);
            }
            done++;
            progress = (float) (done / total);
        }

        logger.finest("Finished formula search for all the peaks");        

        setStatus(TaskStatus.FINISHED);

    }

    private void checkConstraints(MolecularFormula cdkFormula, PeakListRow peakListRow) {

        // Check elemental ratios
        if (checkRatios) {
            boolean check = ElementalHeuristicChecker.checkFormula(cdkFormula,
                    ratiosParameters);
            if (!check) {
                return;
            }
        }

        Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

        // Check RDBE condition
        if (checkRDBE && (rdbeValue != null)) {
            boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue,
                    rdbeParameters);
            if (!check) {
                return;
            }
        }

        // Calculate isotope similarity score
        IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
        IsotopePattern predictedIsotopePattern = null;
        Double isotopeScore = null;
        if ((checkIsotopes) && (detectedPattern != null)) {

            String stringFormula = MolecularFormulaManipulator.getString(cdkFormula);

            String adjustedFormula = FormulaUtils.ionizeFormula(stringFormula,
                    ionType, charge);

            final double isotopeNoiseLevel = isotopeParameters.getParameter(
                    IsotopePatternScoreParameters.isotopeNoiseLevel).getValue();

            final double detectedPatternHeight = detectedPattern.getHighestIsotope().getIntensity();

            final double minPredictedAbundance = isotopeNoiseLevel
                    / detectedPatternHeight;

            predictedIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(adjustedFormula,
                    minPredictedAbundance, charge,
                    ionType.getPolarity());

            isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(detectedPattern,
                    predictedIsotopePattern, isotopeParameters);

            final double minScore = isotopeParameters.getParameter(
                    IsotopePatternScoreParameters.isotopePatternScoreThreshold).getValue();

            if (isotopeScore < minScore) {
                return;
            }

        }

        // MS/MS evaluation is slowest, so let's do it last
        Double msmsScore = null;
        ChromatographicPeak bestPeak = peakListRow.getBestPeak();
        RawDataFile dataFile = bestPeak.getDataFile();
        Map<DataPoint, String> msmsAnnotations = null;
        int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();

        if ((checkMSMS) && (msmsScanNumber > 0)) {
            Scan msmsScan = dataFile.getScan(msmsScanNumber);
            String massListName = msmsParameters.getParameter(
                    MSMSScoreParameters.massList).getValue();
            MassList ms2MassList = msmsScan.getMassList(massListName);
            if (ms2MassList == null) {
                setStatus(TaskStatus.ERROR);
                this.errorMessage = "The MS/MS scan #" + msmsScanNumber
                        + " in file " + dataFile.getName()
                        + " does not have a mass list called '" + massListName
                        + "'";
                return;
            }

            MSMSScore score = MSMSScoreCalculator.evaluateMSMS(cdkFormula,
                    msmsScan, msmsParameters);

            double minMSMSScore = msmsParameters.getParameter(
                    MSMSScoreParameters.msmsMinScore).getValue();

            if (score != null) {
                msmsScore = score.getScore();
                msmsAnnotations = score.getAnnotation();

                // Check the MS/MS condition
                if (msmsScore < minMSMSScore) {
                    return;
                }
            }

        }

        // Create a new formula entry
        final ResultFormula resultEntry = new ResultFormula(cdkFormula,
                predictedIsotopePattern, rdbeValue, isotopeScore, msmsScore,
                msmsAnnotations);

        // Add the new formula entry
        ResultingFormulas.add(resultEntry);

        foundFormulas++;

    }

    @Override
    public void cancel() {
        super.cancel();

        // We need to cancel the formula generator, because searching for next
        // candidate formula may take a looong time
        if (generator != null) {
            generator.cancel();
        }

    }
}