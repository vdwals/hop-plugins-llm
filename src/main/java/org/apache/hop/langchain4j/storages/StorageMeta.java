package org.apache.hop.langchain4j.storages;

import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.langchain4j.storages.inmemory.InMemoryStorageMeta;
import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@HopMetadata(key = "vectorstorage", name = "Vector Storage", description = "Central setting for connection to diverse Vector Storages")
@Getter
@Setter
@NoArgsConstructor
public class StorageMeta extends HopMetadataBase implements Cloneable {
    public static final String GUI_PLUGIN_ELEMENT_PARENT_ID = "StorageMeta-PluginSpecific-Options";

    @HopMetadataProperty(key = "storage")
    private IStorage storage;

    public StorageMeta(String name) {
        this.name = name;
    }

    public StorageMeta(StorageMeta storageMeta) {
        this(storageMeta.getName());
        this.storage = storageMeta.storage.clone();
    }

    public IStorage getStorage() {
        if (storage == null)
            setStorage(new InMemoryStorageMeta());
        return storage;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStorage(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables) {
        return getStorage().getEmbeddingStore(metadataProvider, log, variables);
    }
}
