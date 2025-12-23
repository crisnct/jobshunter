module com.jobshunter.processor {
  requires transitive java.compiler;

  exports com.jobshunter.processor;

  provides javax.annotation.processing.Processor with com.jobshunter.processor.PackageUsageProcessor,
      com.jobshunter.processor.SqlInjectionValidatorProcessor;
}
