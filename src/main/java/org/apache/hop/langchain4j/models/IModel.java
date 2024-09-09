package org.apache.hop.langchain4j.models;

import dev.langchain4j.model.embedding.EmbeddingModel;

public interface IModel extends Cloneable {
    public String getName();

    public IModel clone();

    public EmbeddingModel getEmbeddingModel();
}
