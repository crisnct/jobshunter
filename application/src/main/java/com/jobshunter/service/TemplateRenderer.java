package com.jobshunter.service;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.codes.ValueCode;
import com.jobshunter.model.AiSchemaType;
import com.jobshunter.model.PromptType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TemplateRenderer {

  private final MustacheFactory factory = new DefaultMustacheFactory();

  @PostConstruct
  private void validate() {
    // Validate that all templates are present
    for (PromptType type : PromptType.values()) {
      getString("prompts/", type.name(), ".mustache", false, Map.of());
    }
    for (AiSchemaType type : AiSchemaType.values()) {
      getString("schema/", type.name(), ".json", false, Map.of());
    }
  }

  public String getPrompt(PromptType type, Map<String, Object> vars) {
    return getString("prompts/", type.name(), ".mustache", true, vars);
  }

  public String getSchema(AiSchemaType type, Map<String, Object> vars) {
    return getString("schema/", type.name(), ".json", true, vars);
  }

  public String getSchema(AiSchemaType type) {
    return getSchema(type, Map.of());
  }

  public String getPrompt(PromptType type) {
    return getPrompt(type, Map.of());
  }

  public String getPrompt(PromptType type, String param1, Object value1) {
    return getPrompt(type, Map.of(param1, value1.toString()));
  }

  public String getPrompt(PromptType type, String param1, Object value1, String param2, Object value2) {
    return getPrompt(type, Map.of(param1, value1.toString(), param2, value2.toString()));
  }

  public String getPrompt(PromptType type, String param1, Object value1, String param2, Object value2, String param3, Object value3) {
    return getPrompt(type, Map.of(param1, value1, param2, value2, param3, value3));
  }

  private String getString(String path, String type, String extension, boolean validateParameters, Map<String, Object> vars) {
    Mustache mustache = factory.compile(path + type.toLowerCase() + extension);
    if (validateParameters) {
      Set<String> paramsFromFile = Arrays.stream(mustache.getCodes())
          .filter(p -> p instanceof ValueCode)
          .map(Code::getName)
          .collect(Collectors.toSet());
      if (paramsFromFile.size() != vars.size()) {
        throw new IllegalArgumentException("TemplateRenderer is not called for prompt " + type + " with all the parameters!!!");
      }
    }
    StringWriter writer = new StringWriter();
    mustache.execute(writer, this.manipulateVars(vars));
    return writer.toString();
  }

  @Nonnull
  private Map<String, Object> manipulateVars(Map<String, Object> vars) {
    final Map<String, Object> modifiedVars = new HashMap<>();
    for (Entry<String, Object> entry : vars.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof Collection<?> collection) {
        value = collection.stream().map(Object::toString).collect(Collectors.joining(", "));
      }
      modifiedVars.put(key, value);
    }
    return modifiedVars;
  }

}
