package com.jobshunter.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.jobshunter.model.AiSchemaType;
import com.jobshunter.model.PromptType;
import jakarta.annotation.PostConstruct;
import java.io.StringWriter;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TemplateRenderer {

  private final MustacheFactory factory = new DefaultMustacheFactory();

  @PostConstruct
  private void validate(){
    // Validate that all templates are present
    for (PromptType type : PromptType.values()) {
      getPrompt(type);
    }
    for (AiSchemaType type : AiSchemaType.values()) {
      getSchema(type);
    }
  }

  public String getPrompt(PromptType type, Map<String, Object> vars) {
    return getString("prompts/", type.name(), ".mustache", vars);
  }

  public String getSchema(AiSchemaType type, Map<String, Object> vars) {
    return getString("schema/", type.name(), ".json", vars);
  }

  public String getSchema(AiSchemaType type) {
    return getSchema(type, Map.of());
  }

  public String getPrompt(PromptType type) {
    return getPrompt(type, Map.of());
  }

  public String getPrompt(PromptType type, String param1, String value1) {
    return getPrompt(type, Map.of(param1, value1));
  }

  public String getPrompt(PromptType type, String param1, String value1, String param2, String value2) {
    return getPrompt(type, Map.of(param1, value1, param2, value2));
  }

  public String getPrompt(PromptType type, String param1, String value1, String param2, String value2, String param3, String value3) {
    return getPrompt(type, Map.of(param1, value1, param2, value2, param3, value3));
  }

  private String getString(String path, String type, String extension, Map<String, Object> vars) {
    Mustache mustache = factory.compile(path + type.toLowerCase() + extension);
    StringWriter writer = new StringWriter();
    mustache.execute(writer, vars);
    return writer.toString();
  }

}
