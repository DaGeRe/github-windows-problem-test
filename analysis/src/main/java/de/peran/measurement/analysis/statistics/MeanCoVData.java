package de.peran.measurement.analysis.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.TestcaseType;

/**
 * Saves all data, its means and its coefficient of variation (CoV) for different executions of one test in one version.
 * 
 * @author reichelt
 *
 */
public class MeanCoVData {

   public static final NumberFormat FORMAT = NumberFormat.getInstance();

   static {
      FORMAT.setGroupingUsed(false);
   }
   
   private final int avgCount;

   protected final List<DescriptiveStatistics> allMeans = new LinkedList<>();
   protected final List<DescriptiveStatistics> allCoVs = new LinkedList<>();

   public List<DescriptiveStatistics> getAllMeans() {
      return allMeans;
   }

   public List<DescriptiveStatistics> getAllCoVs() {
      return allCoVs;
   }

   protected final String testMethodName;
   protected final List<Result> results;
   // final TestcaseType testcase;

   public MeanCoVData(final TestcaseType testcase, int avg_count) {
      this.testMethodName = testcase.getName();
      this.results = testcase.getDatacollector().get(0).getResult();
      this.avgCount = avg_count;
      // this.testcase = testcase;
      addTestcaseData();
   }

   public MeanCoVData(final String name, final List<Result> results) {
      this.testMethodName = name;
      this.results = results;
      avgCount = 10;
      addTestcaseData();
   }

   public int getAvgCount() {
      return avgCount;
   }

   private void addTestcaseData() {
      for (final Result result : results) {
         DescriptiveStatistics statistics = new DescriptiveStatistics();
         int index = 0;
         for (final Value value : result.getFulldata().getValue()) {
            // writer.write(value.getValue() + "\n");
            statistics.addValue(Double.parseDouble(value.getValue()));
            if (statistics.getValues().length == avgCount) {
               final double cov = statistics.getVariance() / statistics.getMean();
               addValue(index, statistics.getMean(), allMeans);
               addValue(index, cov, allCoVs);
               index++;
               statistics = new DescriptiveStatistics();
            }
         }
      }
   }

   private void addValue(final int index, final double value, final List<DescriptiveStatistics> statistics) {
      DescriptiveStatistics meanSummary;
      if (statistics.size() <= index) {
         meanSummary = new DescriptiveStatistics();
         statistics.add(meanSummary);
      } else {
         meanSummary = statistics.get(index);
      }
      meanSummary.addValue(value);
   }

   public void printTestcaseData(final File folder) throws IOException {
      for (final Result result : results) {
         final File csvFile = new File(folder, "result_" + testMethodName + "_" + result.getDate() + ".csv");
         printResult(result, csvFile);
      }
      System.out.println();
   }

   protected void printResult(final Result result, final File csvFile) throws IOException {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
         DescriptiveStatistics statistics = new DescriptiveStatistics();
         for (final Value value : result.getFulldata().getValue()) {
            // writer.write(value.getValue() + "\n");
            statistics.addValue(Double.parseDouble(value.getValue()));
            if (statistics.getValues().length == avgCount) {
               final double cov = statistics.getVariance() / statistics.getMean();
               writer.write(FORMAT.format(statistics.getMean()) + ";" + FORMAT.format(cov) + "\n");
               statistics = new DescriptiveStatistics();
            }
         }
         writer.flush();
         // System.out.println("set title 'Mean and Coefficient of Variation for " + clazzname + "." + testcase.getName() + "'");
         // System.out.println("set y2range [0:5]");
         // System.out.println("set y2tics");
         // System.out.println("set datafile separator ';'");
         // System.out.println("plot '" + csvFile.getName() + "' u ($0*" + AVG_COUNT + "):1 title 'Mean', '" + csvFile.getName() + "' u ($0*" + AVG_COUNT + "):2 title 'CoV' axes
         // x1y2");
         System.out.print(", '" + csvFile.getName() + "' u ($0*" + avgCount + "):1 title 'Mean'");
         // System.out.println();
      }
   }

   /**
    * Writes the average values to a csv-file in the given folder and returns the filename.
    * 
    * @param folder Destination folder for the result-csv
    * @param clazzname Name of the class
    * @return Written CSV
    * @throws IOException
    */
   public File printAverages(final File folder, final String clazzname) throws IOException {
      final File summaryFile = new File(folder, "result_" + clazzname + "_" + testMethodName + "_all.csv");
      printAverages(summaryFile);

      System.out.println("set title 'Mean Mean and Mean Coefficient of Variation for " + clazzname + "." + testMethodName + "'");
      System.out.println(
            "plot '" + summaryFile.getName() + "' u ($0*" + avgCount + "):1 title 'Mean', '" + summaryFile.getName() + "' u ($0*" + avgCount + "):2 title 'CoV' axes x1y2");
      System.out.println();
      return summaryFile;
   }

   /**
    * Writes the average values to a csv-file in the given folder and returns the filename.
    * 
    * @param folder Destination folder for the result-csv
    * @param clazzname Name of the class
    * @return Written CSV
    * @throws IOException
    */
   public File printAverages(final File summaryFile) throws IOException {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(summaryFile))) {
         for (int i = 0; i < allMeans.size(); i++) {
            writer.write(FORMAT.format(allMeans.get(i).getMean()) + ";" + FORMAT.format(allCoVs.get(i).getMean()) + "\n");
         }
         writer.flush();
      }
      return summaryFile;
   }
}