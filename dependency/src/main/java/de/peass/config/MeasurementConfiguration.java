package de.peass.config;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MeasurementConfiguration implements Serializable {
   
   private static final long serialVersionUID = -6936740902708676182L;

   public static final MeasurementConfiguration DEFAULT = new MeasurementConfiguration(60 * 1000, 30, 0.01, 0.01);

   private final int vms;
   private boolean earlyStop = true;
   private int warmup = 0;
   private int iterations = 1;
   private int repetitions = 1;
   private boolean logFullData = true;
   private boolean removeSnapshots = false;

   // Kieker config
   private boolean useKieker = false;
   private boolean useSourceInstrumentation = false;
   private boolean useSelectiveInstrumentation = false;
   private boolean useSampling = false;
   private boolean useCircularQueue = false;
   private boolean redirectToNull = true;
   private boolean enableAdaptiveMonitoring = false;
   private boolean useGC = true;
   private int kiekerAggregationInterval = 5000;
   private String javaVersion = System.getProperty("java.version");

   public MeasurementConfiguration(final int vms) {
      this.vms = vms;
   }

   public MeasurementConfiguration(final int vms, final int timeoutInMinutes) {
      this.vms = vms;
   }

   public MeasurementConfiguration(final int vms, final String version, final String versionOld) {
      this.vms = vms;
   }

   public MeasurementConfiguration(@JsonProperty("timeout") final int timeout,
         @JsonProperty("vms") final int vms,
         @JsonProperty("type1error") final double type1error,
         @JsonProperty("type2error") final double type2error) {
      this.vms = vms;
   }

   @JsonCreator
   public MeasurementConfiguration(@JsonProperty("timeout") final int timeout,
         @JsonProperty("vms") final int vms,
         @JsonProperty("earlystop") final boolean earlyStop,
         @JsonProperty("version") final String version,
         @JsonProperty("versionOld") final String versionOld) {
      this.vms = vms;
      this.earlyStop = earlyStop;
   }

   /**
    * Copy constructor
    * 
    * @param other Configuration to copy
    */
   public MeasurementConfiguration(final MeasurementConfiguration other) {
      this.vms = other.vms;
      this.earlyStop = other.earlyStop;
      this.warmup = other.warmup;
      this.iterations = other.iterations;
      this.repetitions = other.repetitions;
      this.logFullData = other.logFullData;
      this.removeSnapshots = other.removeSnapshots;
      this.useKieker = other.useKieker;
      this.useSourceInstrumentation = other.useSourceInstrumentation;
      this.useSelectiveInstrumentation = other.useSelectiveInstrumentation;
      this.useSampling = other.useSampling;
      this.useCircularQueue = other.useCircularQueue;
      this.redirectToNull = other.redirectToNull;
      this.enableAdaptiveMonitoring = other.enableAdaptiveMonitoring;
      this.useGC = other.useGC;
      this.kiekerAggregationInterval = other.kiekerAggregationInterval;
      this.javaVersion = other.javaVersion;
   }

   /**
    * Whether to execute a GC before every iteration (bunch of repetitions)
    * 
    * @return
    */
   public boolean isUseGC() {
      return useGC;
   }

   public void setUseGC(final boolean useGC) {
      this.useGC = useGC;
   }

   public boolean isRedirectToNull() {
      return redirectToNull;
   }

   public void setRedirectToNull(final boolean redirectToNull) {
      this.redirectToNull = redirectToNull;
   }

   public int getVms() {
      return vms;
   }

   public boolean isEarlyStop() {
      return earlyStop;
   }

   public void setEarlyStop(final boolean earlyStop) {
      this.earlyStop = earlyStop;
   }

   public int getWarmup() {
      return warmup;
   }

   public void setWarmup(final int warmup) {
      this.warmup = warmup;
   }

   public int getIterations() {
      return iterations;
   }

   public void setIterations(final int iterations) {
      this.iterations = iterations;
   }

   public int getRepetitions() {
      return repetitions;
   }

   public void setRepetitions(final int repetitions) {
      this.repetitions = repetitions;
   }

   public boolean isLogFullData() {
      return logFullData;
   }

   public void setLogFullData(final boolean logFullData) {
      this.logFullData = logFullData;
   }

   public boolean isUseKieker() {
      return useKieker;
   }

   public void setUseKieker(final boolean useKieker) {
      this.useKieker = useKieker;
   }

   public int getKiekerAggregationInterval() {
      return kiekerAggregationInterval;
   }

   public void setKiekerAggregationInterval(final int kiekerAggregationInterval) {
      this.kiekerAggregationInterval = kiekerAggregationInterval;
   }

   public String getJavaVersion() {
      return javaVersion;
   }

   public void setJavaVersion(final String javaVersion) {
      this.javaVersion = javaVersion;
   }

   public boolean isUseSourceInstrumentation() {
      return useSourceInstrumentation;
   }

   public void setUseSourceInstrumentation(final boolean useSourceInstrumentation) {
      this.useSourceInstrumentation = useSourceInstrumentation;
   }

   public boolean isUseCircularQueue() {
      return useCircularQueue;
   }

   public void setUseCircularQueue(final boolean useCircularQueue) {
      this.useCircularQueue = useCircularQueue;
   }

   public boolean isUseSelectiveInstrumentation() {
      return useSelectiveInstrumentation;
   }

   public void setUseSelectiveInstrumentation(final boolean useSelectiveInstrumentation) {
      this.useSelectiveInstrumentation = useSelectiveInstrumentation;
   }

   public boolean isUseSampling() {
      return useSampling;
   }

   public void setUseSampling(final boolean useSampling) {
      this.useSampling = useSampling;
   }

   public boolean isEnableAdaptiveConfig() {
      return enableAdaptiveMonitoring;
   }

   public void setEnableAdaptiveConfig(final boolean allowAdaptiveConfig) {
      this.enableAdaptiveMonitoring = allowAdaptiveConfig;
   }

   /**
    * Returns the warmup that should be ignored when individual nodes are measured
    * 
    * @return
    */
   @JsonIgnore
   public int getNodeWarmup() {
      final int samplingfactor = this.isUseSampling() ? 1000 : 1;
      final int warmup = this.getWarmup() * this.getRepetitions() / samplingfactor;
      return warmup;
   }

   public void setRemoveSnapshots(final boolean removeSnapshots) {
      this.removeSnapshots = removeSnapshots;
   }

   public boolean isRemoveSnapshots() {
      return removeSnapshots;
   }

}