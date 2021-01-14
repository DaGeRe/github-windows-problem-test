package de.peass.measurement.rca.reading;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.rca.KiekerResultReader;
import de.peass.measurement.rca.data.CallTreeNode;

public class TestRealWorld {

   private static final String version = "bcf50e2832b63a1ad44c5627862ef62e92ea605a";
   private static final String predecessor = version+"~1";
   
   @Test
   public void callMe() throws JsonParseException, JsonMappingException, IOException {
      final MeasurementConfiguration config = new MeasurementConfiguration(5);
      CallTreeNode rootNode = new CallTreeNode("de.peass.MainTest#testMe", "public void de.peass.MainTest.testMe()", "public void de.peass.MainTest.testMe()",  config);
      CallTreeNode rootNode2 = new CallTreeNode("de.peass.MainTest#testMe", "public void de.peass.MainTest.testMe()", "public void de.peass.MainTest.testMe()",  config);
      rootNode.setOtherVersionNode(rootNode2);
      rootNode2.setOtherVersionNode(rootNode);
      rootNode.setVersions(version, predecessor);
      rootNode2.setVersions(version, predecessor);

      Set<CallTreeNode> includedNodes = new HashSet<>();
      includedNodes.add(rootNode);

      File parentFolder = new File("src/test/resources/rcaDataExample/testMe/");
      read(parentFolder, version, includedNodes);
      read(parentFolder, predecessor, includedNodes);

      rootNode.createStatistics(version);
      rootNode.createStatistics(predecessor);
      Assert.assertEquals(-3.05396, rootNode.getTestcaseStatistic().getTvalue(), 0.01);
      
      Assert.assertEquals(2.04496, rootNode.getStatistics(version).getMean(), 0.01);
      Assert.assertEquals(2.03272, rootNode.getStatistics(predecessor).getMean(), 0.01);
      
      Assert.assertEquals(10, rootNode.getStatistics("bcf50e2832b63a1ad44c5627862ef62e92ea605a").getN());
      Assert.assertEquals(10, rootNode.getStatistics("bcf50e2832b63a1ad44c5627862ef62e92ea605a~1").getN());
   }

   private void read(File parentFolder, final String version, Set<CallTreeNode> includedNodes) throws JsonParseException, JsonMappingException, IOException {
      final boolean isOtherVersionNode = !version.equals(version);
      KiekerResultReader reader = new KiekerResultReader(true, includedNodes,
            version, new TestCase("de.peass.MainTest#testMe"), isOtherVersionNode);
      File currentFolder = new File(parentFolder, version + "/0");
      for (File kiekerTraceContainingFolder : currentFolder.listFiles((FileFilter) new WildcardFileFilter("16*"))) {
         File kiekerTraceFolder = kiekerTraceContainingFolder.listFiles()[0].listFiles()[0];
         reader.readAggregatedData(kiekerTraceFolder);
      }
   }
}
