package org.apache.hop.langchain4j.storages;

import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.HopMetadataObject;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

@HopMetadataObject(objectFactory = IStorageObjectFactory.class)
public interface IStorage {

    String getName();

    public IStorage clone();

    EmbeddingStore<TextSegment> getEmbeddingStore(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables);
}
