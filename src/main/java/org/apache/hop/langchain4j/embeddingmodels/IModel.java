package org.apache.hop.langchain4j.embeddingmodels;

import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.HopMetadataObject;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import dev.langchain4j.model.embedding.EmbeddingModel;

@HopMetadataObject(objectFactory = ModelObjectFactory.class)
public interface IModel extends Cloneable {
    public String getName();

    public IModel clone();

    public EmbeddingModel getEmbeddingModel(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables);
}
