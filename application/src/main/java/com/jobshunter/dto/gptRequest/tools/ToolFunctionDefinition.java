package com.jobshunter.dto.gptRequest.tools;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Wrapper pentru tool/function de tip OpenAI.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolFunctionDefinition extends Tools{

private FunctionDefinition function;

public ToolFunctionDefinition(FunctionDefinition function) {
  super("function");
  this.function = function;
}

}
