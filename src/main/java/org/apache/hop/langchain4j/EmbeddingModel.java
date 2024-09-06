package org.apache.hop.langchain4j;

import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.IEnumHasCode;
import org.apache.hop.metadata.api.IEnumHasCodeAndDescription;

public enum EmbeddingModel implements IEnumHasCodeAndDescription {
    ONNX_MODEL("onnx", BaseMessages.getString(EmbeddingModel.class, "SemanticSearchMeta.embeddingmodel.onnx")),
    OPEN_AI("openai", BaseMessages.getString(EmbeddingModel.class, "SemanticSearchMeta.embeddingmodel.openai"));

    private final String code;
    private final String description;

    EmbeddingModel(String code, String description) {
      this.code = code;
      this.description = description;
    }

    public static String[] getDescriptions() {
        return IEnumHasCodeAndDescription.getDescriptions(EmbeddingModel.class);
    }

    public static EmbeddingModel lookupDescription(String description) {
        return IEnumHasCodeAndDescription.lookupDescription(EmbeddingModel.class, description,
                ONNX_MODEL);
    }

    public static EmbeddingModel lookupCode(String code) {
        return IEnumHasCode.lookupCode(EmbeddingModel.class, code, ONNX_MODEL);
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
