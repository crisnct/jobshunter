package com.jobshunter.processor;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor9;
import javax.tools.Diagnostic;

/**
 * Annotation processor that guards against SQL-injection strings in places where values are known at
 * compile time.
 *
 * <p>It checks {@link SqlInjectionSafe} fields and parameters and validates any compile-time
 * constant or default values using a conservative SQL-injection regex. Non-{@link String} usage is
 * rejected to avoid a false sense of safety.
 */
@SupportedAnnotationTypes("com.jobshunter.processor.SqlInjectionSafe")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SqlInjectionValidatorProcessor extends AbstractProcessor {

  private static final Pattern SQL_INJECTION_PATTERN =
      Pattern.compile(
          "(?i)(\\b(?:select|update|delete|insert|drop|union|alter|truncate|exec|execute)\\b|--|;|/\\*|\\*/|(?:'|\")\\s*(?:or|and)\\b|\\b(?:or|and)\\b\\s*\\d+\\s*=\\s*\\d+|\\b(?:or|and)\\b\\s*'[^']*'\\s*=\\s*'[^']*')",
          Pattern.CASE_INSENSITIVE);

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (annotations.isEmpty()) {
      return false;
    }

    Messager messager = processingEnv.getMessager();
    TypeMirror stringType =
        processingEnv.getElementUtils().getTypeElement(String.class.getCanonicalName()).asType();

    for (Element element : roundEnv.getElementsAnnotatedWith(SqlInjectionSafe.class)) {
      if (!isSupportedElementKind(element)) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "@SqlInjectionSafe can only be applied to fields or parameters",
            element);
        continue;
      }

      if (!processingEnv.getTypeUtils().isSameType(element.asType(), stringType)) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "@SqlInjectionSafe is intended for String-typed elements",
            element);
        continue;
      }

      String candidate = resolveCandidateValue(element);
      if (candidate == null || candidate.isBlank()) {
        continue;
      }

      if (SQL_INJECTION_PATTERN.matcher(candidate).find()) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Potential SQL injection detected in value: \"" + candidate + "\"",
            element);
      }
    }

    // Do not claim the annotation to allow other processors to run.
    return false;
  }

  private boolean isSupportedElementKind(Element element) {
    return element.getKind() == ElementKind.FIELD || element.getKind() == ElementKind.PARAMETER;
  }

  /**
   * Tries to resolve a compile-time string value from the annotated element. This covers literal
   * initializers as well as defaults supplied by other annotations (e.g. {@code defaultValue} on
   * web or configuration annotations).
   */
  private String resolveCandidateValue(Element element) {
    if (element instanceof VariableElement variableElement) {
      Object constantValue = variableElement.getConstantValue();
      if (constantValue instanceof String stringValue) {
        return stringValue;
      }
    }

    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
          mirror.getElementValues().entrySet()) {
        if (!"defaultValue".equals(entry.getKey().getSimpleName().toString())) {
          continue;
        }

        AnnotationValue value = entry.getValue();
        return value.accept(
            new SimpleAnnotationValueVisitor9<>() {
              @Override
              public String visitString(String s, Object unused) {
                return s;
              }
            },
            null);
      }
    }

    return null;
  }
}

