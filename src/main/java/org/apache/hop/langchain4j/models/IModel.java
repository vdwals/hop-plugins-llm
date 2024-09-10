package org.apache.hop.langchain4j.models;

import org.apache.hop.metadata.api.HopMetadataObject;

import dev.langchain4j.model.embedding.EmbeddingModel;

@HopMetadataObject(objectFactory = ModelMetaObjectFactory.class)
public interface IModel extends Cloneable {
    public String getName();

    public IModel clone();

    public EmbeddingModel getEmbeddingModel();
}
