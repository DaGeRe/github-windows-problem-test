package de.peass.measurement.rca;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.AdaptiveTester;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.analysis.EarlyBreakDecider;
import de.peass.measurement.organize.ResultOrganizer;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.testtransformation.JUnitTestTransformer;
import kieker.analysis.exception.AnalysisConfigurationException;

/**
 * Measures method calls adaptively instrumented by Kieker
 * 
 * @author reichelt
 *
 */
public class CauseTester extends AdaptiveTester {

   private static final Logger LOG = LogManager.getLogger(CauseTester.class);

   private Set<CallTreeNode> includedNodes;
   private final TestCase testcase;
   private final CauseSearcherConfig causeConfig;
   private final CauseSearchFolders folders;
   private int adaptiveId = 0;
   private boolean considerNodePosition = false;

   public CauseTester(final CauseSearchFolders project, final JUnitTestTransformer testgenerator, final CauseSearcherConfig causeConfig)
         throws IOException {
      super(project, testgenerator);
      this.testcase = causeConfig.getTestCase();
      this.causeConfig = causeConfig;
      this.folders = project;
      testgenerator.setAdaptiveExecution(true);
      testgenerator.setAggregatedWriter(causeConfig.isUseAggregation());
      testgenerator.setIgnoreEOIs(causeConfig.isIgnoreEOIs());
   }

   public void setConsiderNodePosition(final boolean considerNodePosition) {
      this.considerNodePosition = considerNodePosition;
   }

   public void measureVersion(final List<CallTreeNode> nodes)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      includedNodes = prepareNodes(nodes);
      evaluate(causeConfig.getTestCase());
      getDurations(adaptiveId);
      cleanup(adaptiveId);
      adaptiveId++;
   }

   private Set<CallTreeNode> prepareNodes(final List<CallTreeNode> nodes) {
      final Set<CallTreeNode> includedNodes = new HashSet<CallTreeNode>();
      includedNodes.addAll(nodes);
      nodes.forEach(node -> node.setVersions(testTransformer.getConfig().getVersion(), testTransformer.getConfig().getVersionOld()));
      return includedNodes;
   }

   @Override
   public void evaluate(final TestCase testcase) throws IOException, InterruptedException, JAXBException {
      includedNodes.forEach(node -> node.setWarmup(testTransformer.getConfig().getIterations() / 2));

      LOG.debug("Adaptive execution: " + includedNodes);

      super.evaluate(testcase);
   }

   @Override
   protected void runOnce(final TestCase testcase, final String version, final int vmid, final File logFolder) throws IOException, InterruptedException, JAXBException {
      final Set<String> includedPattern = new HashSet<>();
      if (configuration.getVersionOld().equals(version)) {
         includedNodes.forEach(node -> includedPattern.add(node.getKiekerPattern()));
      } else {
         System.out.println("Searching other: " + version);
         includedNodes.forEach(node -> {
            System.out.println(node);
            includedPattern.add(node.getOtherVersionNode().getKiekerPattern());
         });
      }
      testExecutor.setIncludedMethods(includedPattern);
      currentOrganizer = new ResultOrganizer(folders, currentVersion, currentChunkStart, testTransformer.getConfig().isUseKieker(), causeConfig.isSaveAll(), testcase);
      super.runOnce(testcase, version, vmid, logFolder);
   }

   @Override
   public boolean checkIsDecidable(final TestCase testcase, final int vmid) throws JAXBException {
      try {
         getDurationsVersion(configuration.getVersion());
         getDurationsVersion(configuration.getVersionOld());
         boolean allDecidable = super.checkIsDecidable(testcase, vmid);
         LOG.debug("Super decidable: {}", allDecidable);
         for (final CallTreeNode includedNode : includedNodes) {
            allDecidable &= checkLevelDecidable(vmid, allDecidable, includedNode);
         }
         LOG.debug("Level decideable: {}", allDecidable);
         return allDecidable;
      } catch (ViewNotFoundException | AnalysisConfigurationException e) {
         throw new RuntimeException(e);
      }
   }

   private boolean checkLevelDecidable(final int vmid, boolean allDecidable, final CallTreeNode includedNode) throws JAXBException {
      final SummaryStatistics statisticsOld = includedNode.getStatistics(configuration.getVersionOld());
      final SummaryStatistics statistics = includedNode.getStatistics(configuration.getVersion());
      final EarlyBreakDecider decider = new EarlyBreakDecider(configuration, statisticsOld, statistics);
      final boolean nodeDecidable = decider.isBreakPossible(vmid);
      LOG.debug("{} decideable: {}", includedNode.getKiekerPattern(), allDecidable);
      LOG.debug("Old: {} {} Current: {} {}", statisticsOld.getMean(), statisticsOld.getStandardDeviation(), 
            statistics.getMean(), statistics.getStandardDeviation());
      return nodeDecidable;
   }

   @Override
   protected void handleKiekerResults(final String version, final File versionResultFolder) {
      final KiekerResultReader kiekerResultReader = new KiekerResultReader(causeConfig.isUseAggregation(), includedNodes, version, versionResultFolder, testcase,
            version.equals(configuration.getVersion()));
      kiekerResultReader.setConsiderNodePosition(considerNodePosition);
      kiekerResultReader.readResults();
   }

   public void setIncludedMethods(final Set<CallTreeNode> children) {
      includedNodes = children;
   }

   public void getDurations(final int adaptiveId)
         throws FileNotFoundException, IOException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException {
      getDurationsVersion(configuration.getVersion());
      getDurationsVersion(configuration.getVersionOld());
   }

   public void cleanup(final int adaptiveId) {
      organizeMeasurements(adaptiveId, configuration.getVersion(), configuration.getVersion());
      organizeMeasurements(adaptiveId, configuration.getVersion(), configuration.getVersionOld());
   }

   private void organizeMeasurements(final int adaptiveId, final String mainVersion, final String version) {
      final File testcaseFolder = folders.getFullResultFolder(testcase, mainVersion, version);
      final File versionFolder = new File(folders.getArchiveResultFolder(mainVersion, testcase), version);
      if (!versionFolder.exists()) {
         versionFolder.mkdir();
      }
      final File adaptiveRunFolder = new File(versionFolder, "" + adaptiveId);
      if (!testcaseFolder.renameTo(adaptiveRunFolder)) {
         LOG.error("Could not rename {}", testcaseFolder);
      }
   }

   private void getDurationsVersion(final String version) throws ViewNotFoundException, AnalysisConfigurationException {
      includedNodes.forEach(node -> node.createStatistics(version));
   }

   public static void main(final String[] args) throws IOException, XmlPullParserException, InterruptedException, JAXBException {
      final File projectFolder = new File("../../projekte/commons-fileupload");
      final String version = "4ed6e923cb2033272fcb993978d69e325990a5aa";
      final TestCase test = new TestCase("org.apache.commons.fileupload.ServletFileUploadTest", "testFoldedHeaders");

      final MeasurementConfiguration config = new MeasurementConfiguration(15 * 1000 * 60, 15, 0.01, 0.05, true, version, version + "~1");
      config.setUseKieker(true);
      final CauseSearcherConfig causeConfig = new CauseSearcherConfig(test, false, true, 5, false, 0.01, false, false);
      final CauseTester manager = new CauseTester(new CauseSearchFolders(projectFolder), new JUnitTestTransformer(projectFolder, config), causeConfig);

      final CallTreeNode node = new CallTreeNode("FileUploadTestCase#parseUpload",
            "protected java.util.List org.apache.commons.fileupload.FileUploadTestCase.parseUpload(byte[],java.lang.String)");
      node.setOtherVersionNode(node);
      final Set<CallTreeNode> nodes = new HashSet<>();
      nodes.add(node);
      manager.setIncludedMethods(nodes);
      manager.runOnce(test, version, 0, new File("log"));
      // manager.evaluate(test);

   }
}