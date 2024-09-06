package org.apache.hop.langchain4j;

import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.IEnumHasCode;
import org.apache.hop.metadata.api.IEnumHasCodeAndDescription;

public enum EmbeddingStore implements IEnumHasCodeAndDescription {
    IN_MEMORY("inmemory",
            BaseMessages.getString(EmbeddingStore.class, "SemanticSearchMeta.embeddingstore.inmemory")),
    NEO4J("neo4j",
            BaseMessages.getString(EmbeddingStore.class, "SemanticSearchMeta.embeddingstore.neo4j")),
    CHROMA("chroma",
            BaseMessages.getString(EmbeddingStore.class, "SemanticSearchMeta.embeddingstore.chroma"));

    private final String code;
    private final String description;

    EmbeddingStore(String code, String description) {
      this.code = code;
      this.description = description;
    }

    public static String[] getDescriptions() {
        return IEnumHasCodeAndDescription.getDescriptions(EmbeddingStore.class);
    }

    public static EmbeddingStore lookupDescription(String description) {
        return IEnumHasCodeAndDescription.lookupDescription(EmbeddingStore.class, description,
                IN_MEMORY);
    }

    public static EmbeddingStore lookupCode(String code) {
        return IEnumHasCode.lookupCode(EmbeddingStore.class, code, IN_MEMORY);
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
