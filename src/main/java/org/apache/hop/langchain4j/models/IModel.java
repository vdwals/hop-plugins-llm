package org.apache.hop.langchain4j.models;

import org.apache.hop.metadata.api.HopMetadataObject;
import java.util.Map;

import dev.langchain4j.model.embedding.EmbeddingModel;

@HopMetadataObject(objectFactory = ModelMetaObjectFactory.class)
public interface IModel extends Cloneable {
    public String getName();

    public IModel clone();

    public EmbeddingModel getEmbeddingModel(Map<String, String> attributes);

    public Map<String, String> getAttributeMap();
}
