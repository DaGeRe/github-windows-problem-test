package de.peass.measurement.rca.data;

import org.hamcrest.number.IsNaN;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;

public class TestCallTreeStatistics {
   @Test
   public void testStatistics() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()", (MeasurementConfiguration) null);
      final CallTreeNode otherVersionNode = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()", (MeasurementConfiguration) null);
      node.setOtherVersionNode(otherVersionNode);

      node.setVersions("A", "B");
      for (int vm = 0; vm < 10; vm++) {
         addVMMeasurements("A", node);
         addVMMeasurements("B", node);
      }
      node.createStatistics("A");
      node.createStatistics("B");

      Assert.assertEquals(15, node.getStatistics("A").getMean(), 0.01);
      Assert.assertEquals(15, node.getStatistics("B").getMean(), 0.01);

      Assert.assertEquals(10, node.getTestcaseStatistic().getVMs());
      Assert.assertEquals(150, node.getTestcaseStatistic().getCallsOld());
      Assert.assertEquals(150, node.getTestcaseStatistic().getCalls());
   }

   @Test
   public void testStatisticsADDED() {
      final CallTreeNode node = new CallTreeNode(CauseSearchData.ADDED, CauseSearchData.ADDED, "public void de.mypackage.Test.callMethod()", (MeasurementConfiguration) null);
      final CallTreeNode otherVersionNode = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", CauseSearchData.ADDED, (MeasurementConfiguration) null);
      node.setOtherVersionNode(otherVersionNode);

      node.setVersions("A", "B");
      for (int vm = 0; vm < 10; vm++) {
         addVMMeasurements("A", node);
      }
      node.createStatistics("A");
      node.createStatistics("B");

      Assert.assertEquals(15, node.getStatistics("A").getMean(), 0.01);
      Assert.assertThat(node.getStatistics("B").getMean(), IsNaN.notANumber());

      Assert.assertEquals(10, node.getStatistics("A").getN());
      Assert.assertEquals(0, node.getStatistics("B").getN());

      Assert.assertEquals(10, node.getPartialTestcaseStatistic().getVMs());
      Assert.assertEquals(0, node.getPartialTestcaseStatistic().getCallsOld());
      Assert.assertEquals(150, node.getPartialTestcaseStatistic().getCalls());
   }

   @Test
   public void testStatisticsADDEDNew() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", CauseSearchData.ADDED, (MeasurementConfiguration) null);
      final CallTreeNode otherVersionNode = new CallTreeNode(CauseSearchData.ADDED, CauseSearchData.ADDED, "public void de.mypackage.Test.callMethod()", (MeasurementConfiguration) null);
      node.setOtherVersionNode(otherVersionNode);

      node.setVersions("A", "B");
      for (int vm = 0; vm < 10; vm++) {
         addVMMeasurements("B", node);
      }
      node.createStatistics("A");
      node.createStatistics("B");

      Assert.assertEquals(15, node.getStatistics("B").getMean(), 0.01);
      Assert.assertThat(node.getStatistics("A").getMean(), IsNaN.notANumber());

      Assert.assertEquals(10, node.getStatistics("B").getN());
      Assert.assertEquals(0, node.getStatistics("A").getN());

      Assert.assertEquals(10, node.getPartialTestcaseStatistic().getVMs());
      Assert.assertEquals(150, node.getPartialTestcaseStatistic().getCallsOld());
      Assert.assertEquals(0, node.getPartialTestcaseStatistic().getCalls());
   }

   private void addVMMeasurements(String version, final CallTreeNode node) {
      node.newVM(version);
      for (int i = 0; i < 15; i++) {
         node.addMeasurement(version, 15L);
      }
   }
}
