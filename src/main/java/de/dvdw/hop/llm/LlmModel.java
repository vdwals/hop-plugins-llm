package de.dvdw.hop.llm;

import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.IEnumHasCode;
import org.apache.hop.metadata.api.IEnumHasCodeAndDescription;

public enum LlmModel implements IEnumHasCodeAndDescription {
  ONNX_MODEL("onnx", BaseMessages.getString(LlmModel.class, "LlmModel.onnx")), OPEN_AI("openai",
      BaseMessages.getString(LlmModel.class, "LlmModel.openai"));

  private final String code;
  private final String description;

  LlmModel(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public static String[] getDescriptions() {
    return IEnumHasCodeAndDescription.getDescriptions(LlmModel.class);
  }

  public static LlmModel lookupDescription(String description) {
    return IEnumHasCodeAndDescription.lookupDescription(LlmModel.class, description, ONNX_MODEL);
  }

  public static LlmModel lookupCode(String code) {
    return IEnumHasCode.lookupCode(LlmModel.class, code, ONNX_MODEL);
  }

  /**
   * Gets code
   *
   * @return value of code
   */
  @Override
  public String getCode() {
    return code;
  }

  /**
   * Gets description
   *
   * @return value of description
   */
  @Override
  public String getDescription() {
    return description;
  }
}
