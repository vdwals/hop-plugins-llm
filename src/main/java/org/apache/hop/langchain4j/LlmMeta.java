package org.apache.hop.langchain4j;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.langchain4j.models.IModel;
import org.apache.hop.langchain4j.models.IModelObjectFactory;
import org.apache.hop.langchain4j.models.onnx.OnnxModelMeta;
import org.apache.hop.langchain4j.storages.IStorage;
import org.apache.hop.langchain4j.storages.IStorageObjectFactory;
import org.apache.hop.langchain4j.storages.inmemory.InMemoryStorageMeta;
import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@HopMetadata(key = "llm", name = "Large Language Model", description = "Central setting for connection to diverse large language Models", image = "LlmMeta.svg")
@Getter
@Setter
@NoArgsConstructor
public class LlmMeta extends HopMetadataBase implements Cloneable {
    public static final String GUI_PLUGIN_ELEMENT_PARENT_ID = "LlmMeta-PluginSpecific-Options";
    private final IModelObjectFactory modelFactory = new IModelObjectFactory();
    private final IStorageObjectFactory storageFactory = new IStorageObjectFactory();

    @HopMetadataProperty(key = "storage")
    private IStorage storage;

    @HopMetadataProperty(key = "model")
    private IModel model;

    public LlmMeta(String name) {
        this.name = name;
    }

    public LlmMeta(LlmMeta llmMeta) {
        this(llmMeta.getName());
        this.storage = llmMeta.storage.clone();
        this.model = llmMeta.model.clone();
    }

    public IStorage getStorage() {
        if (storage == null)
            setStorage(new InMemoryStorageMeta());
        return storage;
    }

    public IModel getModel() {
        if (model == null)
            setModel(new OnnxModelMeta());
        return model;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStorage(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables) {
        return getStorage().getEmbeddingStore(metadataProvider, log, variables);
    }

    public EmbeddingModel getEmbeddingModel(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables) {
        return getModel().getEmbeddingModel(metadataProvider, log, variables);
    }

    public void setModelByType(String newTypeName) throws HopException {
        Object model = modelFactory.createObject(newTypeName, null);
        if (model != null)
            setModel((IModel) model);
    }

    public void setStorageByType(String newTypeName) throws HopException {
        Object storage = storageFactory.createObject(newTypeName, null);
        if (storage != null)
            setStorage((IStorage) storage);
    }
}
