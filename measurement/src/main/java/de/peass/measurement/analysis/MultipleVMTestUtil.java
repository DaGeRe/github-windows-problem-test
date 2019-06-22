package de.peass.measurement.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.kopeme.generated.Versioninfo;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.statistics.ConfidenceInterval;

/**
 * Provides utilities for reading KoPeMe-data from multiple runs which should be summarized into one file.
 * 
 * @author reichelt
 *
 */
public class MultipleVMTestUtil {
	private static final Logger LOG = LogManager.getLogger(MultipleVMTestUtil.class);

	public static void main(final String[] args) throws JAXBException {
		final File resultFile = new File(args[0]);
		analyseOneRun(resultFile);
	}

	public static void analyseOneRun(final File resultFile) throws JAXBException {
		final DescriptiveStatistics st = getStatistics(resultFile);
		LOG.info("Durchschnitt: " + st.getMean());
	}

	public static DescriptiveStatistics getStatistics(final File resultFile) throws JAXBException {
		final XMLDataLoader fullDataLoader = new XMLDataLoader(resultFile);
		final Kopemedata fullResultData = fullDataLoader.getFullData();
		final Datacollector oneRunDatacollector = getTimeDataCollector(fullResultData);
		final DescriptiveStatistics st = new DescriptiveStatistics();
		for (final Result r : oneRunDatacollector.getResult()) {
			st.addValue(r.getValue());
		}
		return st;
	}

	public static Datacollector getTimeDataCollector(final Kopemedata fullResultData) {
		Datacollector oneRunDatacollector = null;
		for (final Datacollector collector : fullResultData.getTestcases().getTestcase().get(0).getDatacollector()) {
			if (collector.getName().equals(TimeDataCollector.class.getName())) {
				oneRunDatacollector = collector;
			}
		}
		if (oneRunDatacollector == null) {
			throw new RuntimeException("Achtung: Kein " + TimeDataCollector.class.getName() + " gefunden");
		}
		return oneRunDatacollector;
	}

	/**
	 * Takes the given result and the given version and creates a file containing the aggregated result.
	 * 
	 * @param summaryResultFile
	 * @param oneRunData
	 * @param version
	 * @throws JAXBException
	 */
	public static void saveSummaryData(final File summaryResultFile, final TestcaseType oneRunData, final TestCase testcase, final String version, final long currentChunkStart) throws JAXBException {
	   LOG.info("Writing to merged result file: {}", summaryResultFile);
	   final XMLDataLoader fullDataLoader = new XMLDataLoader(summaryResultFile);
		final Kopemedata fullResultData = fullDataLoader.getFullData();
		if (fullResultData.getTestcases().getTestcase().size() == 0) {
			fullResultData.getTestcases().setClazz(testcase.getClazz());
			fullResultData.getTestcases().getTestcase().add(new TestcaseType());
			fullResultData.getTestcases().getTestcase().get(0).setName(testcase.getMethod());
		}
		Datacollector oneRunDatacollector = getOnedataCollector(oneRunData);
		Chunk realChunk = findChunk(currentChunkStart, fullResultData, oneRunDatacollector);

		final Result oneResult = oneRunDatacollector.getResult().get(0);
		final Result cleaned = ConfidenceInterval.shortenValues(oneResult);
      final Fulldata realData = cleaned.getFulldata();
		if (realData != null && realData.getValue() != null && realData.getValue().size() > 0) {
			final SummaryStatistics st = new SummaryStatistics();
			createStatistics(st, realData);
			final Result result = createResultFromStatistic(version, st, cleaned.getRepetitions());
			result.setDate(cleaned.getDate());
			result.setWarmupExecutions(cleaned.getWarmupExecutions());

			realChunk.getResult().add(result);
			XMLDataStorer.storeData(summaryResultFile, fullResultData);
		} else {
			LOG.error("Achtung: Fulldata von " + summaryResultFile + " leer!");
		}
	}

   public static Datacollector getOnedataCollector(final TestcaseType oneRunData) {
      Datacollector oneRunDatacollector = null;
		for (final Datacollector collector : oneRunData.getDatacollector()) {
			if (collector.getName().equals(TimeDataCollector.class.getName())) {
				oneRunDatacollector = collector;
			}
		}
		if (oneRunDatacollector == null) {
			throw new RuntimeException("Achtung: Kein " + TimeDataCollector.class.getName() + " gefunden");
		}
      return oneRunDatacollector;
   }

   public static Chunk findChunk(final long currentChunkStart, final Kopemedata fullResultData, Datacollector oneRunDatacollector) {
      final List<Datacollector> fullResultFileDatacollectorList = fullResultData.getTestcases().getTestcase().get(0).getDatacollector();
		if (fullResultFileDatacollectorList.size() == 0) {
			fullResultFileDatacollectorList.add(new Datacollector());
			fullResultFileDatacollectorList.get(0).setName(oneRunDatacollector.getName());
		}
		final Datacollector fullFileDatacollector = fullResultFileDatacollectorList.get(0);
		Chunk realChunk = findChunk(currentChunkStart, fullFileDatacollector);
      if (realChunk == null) {
         realChunk = new Chunk();
         realChunk.setChunkStartTime(currentChunkStart);
         fullFileDatacollector.getChunk().add(realChunk);
      }
      return realChunk;
   }

   public static Chunk findChunk(final long currentChunkStart, final Datacollector fullFileDatacollector) {
      Chunk realChunk = null;
      for (final Chunk chunk : fullFileDatacollector.getChunk()) {
         if (chunk.getChunkStartTime() == currentChunkStart) {
            realChunk = chunk;
            break;
         }
      }
      return realChunk;
   }
   
   public static DescriptiveStatistics getChunkData(Chunk chunk, String version) {
      final DescriptiveStatistics desc1 = new DescriptiveStatistics();
      for (final Result result : chunk.getResult()) {
         if (result.getVersion().getGitversion().equals(version) && !Double.isNaN(result.getValue())) {
            desc1.addValue(result.getValue());
         }
      }
      return desc1;
   }
   
   public static long getMinExecutionCount(final List<Result> results) {
      long minExecutionTime = Long.MAX_VALUE;
      for (final Result result : results) {
         final long currentResultSize = result.getExecutionTimes();
         minExecutionTime = Long.min(minExecutionTime, currentResultSize);
      }
      return minExecutionTime;
   }

	private static Result createResultFromStatistic(final String version, final SummaryStatistics st, final long repetitions) {
		final Result result = new Result();
		result.setValue(st.getMean());
		result.setMin((long) st.getMin());
		result.setMax((long) st.getMax());
		result.setVersion(new Versioninfo());
		result.getVersion().setGitversion(version);
		result.setDeviation(st.getStandardDeviation());
		result.setExecutionTimes(st.getN());
		result.setRepetitions(repetitions);
		return result;
	}

	private static double[] createStatistics(final SummaryStatistics st, final Fulldata realData) {
		final double[] values = new double[realData.getValue().size()];
		int i = 0;
		for (final Value value : realData.getValue()) {
			final long parseDouble = Long.parseLong(value.getValue());
			st.addValue(parseDouble);
			values[i++] = parseDouble;
		}
		return values;
	}

	public static List<Double> getAverages(final List<Result> before) {
		return before.stream()
				.mapToDouble(beforeVal -> beforeVal.getFulldata().getValue().stream()
						.mapToDouble(val -> Double.parseDouble(val.getValue())).sum()
						/ beforeVal.getFulldata().getValue().size())
				.boxed().sorted().collect(Collectors.toList());
	}

	public static SummaryStatistics getStatistic(final List<Result> results) {
		final SummaryStatistics statistisc = new SummaryStatistics();
		results.forEach(result -> statistisc.addValue(result.getValue()));
		return statistisc;
	}

	public static int compareDouble(final List<Double> before, final List<Double> after) {
		final boolean change = TestUtils.tTest(ArrayUtils.toPrimitive(before.toArray(new Double[0])), ArrayUtils.toPrimitive(after.toArray(new Double[0])), 0.05);
		final SummaryStatistics statisticBefore = new SummaryStatistics();
		before.forEach(result -> statisticBefore.addValue(result));

		final SummaryStatistics statisticAfter = new SummaryStatistics();
		after.forEach(result -> statisticAfter.addValue(result));
		if (change) {
			if (statisticBefore.getMean() < statisticAfter.getMean())
				return -1;
			else
				return 1;
		} else {
			return 0;
		}
	}

}