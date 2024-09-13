package org.apache.hop.langchain4j.embeddingstores;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.langchain4j.embeddingstores.inmemory.InMemoryStorageMeta;
import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@HopMetadata(key = "embeddingstore", name = "Embedding Store", description = "Central setting for connection to embedding Stores", image = "EmbeddingStore.svg")
@Getter
@Setter
@NoArgsConstructor
public class EmbeddingStoreMeta extends HopMetadataBase implements Cloneable {
    public static final String GUI_PLUGIN_ELEMENT_PARENT_ID = "EmbeddingStoreMeta-PluginSpecific-Options";
    private final StorageObjectFactory storageFactory = new StorageObjectFactory();

    @HopMetadataProperty(key = "storage")
    private IStorage storage;

    public EmbeddingStoreMeta(String name) {
        this.name = name;
    }

    public EmbeddingStoreMeta(EmbeddingStoreMeta llmMeta) {
        this(llmMeta.getName());
        this.storage = llmMeta.storage.clone();
    }

    public IStorage getStorage() {
        if (storage == null)
            setStorage(new InMemoryStorageMeta());
        return storage;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables) {
        return getStorage().getEmbeddingStore(metadataProvider, log, variables);
    }

    public void setStorageByType(String newTypeName) throws HopException {
        Object storage = storageFactory.createObject(newTypeName, null);
        if (storage != null)
            setStorage((IStorage) storage);
    }
}
