module com.jobshunter.processor {
  requires java.compiler;

  exports com.jobshunter.processor;

  provides javax.annotation.processing.Processor with com.jobshunter.processor.PackageUsageProcessor;
}
