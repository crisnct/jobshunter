package com.jobshunter.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner14;
import javax.tools.Diagnostic;

/**
 * Annotation processor that enforces classes annotated with {@link PackageExpected} are only
 * referenced from the expected package. It scans all fields whose type is the annotated class and
 * raises a compilation error if the enclosing class belongs to a different package.
 */
@SupportedAnnotationTypes("com.jobshunter.processor.PackageExpected")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class PackageUsageProcessor extends AbstractProcessor {

  private final Map<TypeMirror, String> expectedPackages = new HashMap<>();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Messager messager = processingEnv.getMessager();

    // Collect annotated types and their expected packages
    for (Element element : roundEnv.getElementsAnnotatedWith(PackageExpected.class)) {
      if (!(element instanceof TypeElement typeElement)) {
        continue;
      }
      PackageExpected annotation = element.getAnnotation(PackageExpected.class);
      if (annotation != null) {
        expectedPackages.put(typeElement.asType(), annotation.value());
      }
    }

    if (expectedPackages.isEmpty()) {
      return false;
    }

    // Scan all root elements for fields using the annotated types
    ElementScanner14<Void, Void> scanner = new ElementScanner14<>() {
      @Override
      public Void visitVariable(VariableElement e, Void unused) {
        if (e.getKind() == ElementKind.FIELD) {
          TypeMirror fieldType = e.asType();
          expectedPackages.forEach((annotatedType, expectedPackage) -> {
            if (processingEnv.getTypeUtils().isSameType(fieldType, annotatedType)) {
              PackageElement ownerPackage = processingEnv.getElementUtils().getPackageOf(e);
              String actualPackage = ownerPackage.getQualifiedName().toString();
              if (!isPackageAllowed(expectedPackage, actualPackage)) {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Field type " + annotatedType.toString()
                        + " is expected to be used only from package '" + expectedPackage
                        + "' but is used in package '" + actualPackage + "'",
                    e
                );
              }
            }
          });
        }
        return super.visitVariable(e, unused);
      }
    };

    for (Element root : roundEnv.getRootElements()) {
      scanner.scan(root);
    }

    // return false to allow other processors to run
    return false;
  }

  private boolean isPackageAllowed(String expectedPackage, String actualPackage) {
    return expectedPackage.equals(actualPackage)
        || actualPackage.startsWith(expectedPackage + ".");
  }
}
