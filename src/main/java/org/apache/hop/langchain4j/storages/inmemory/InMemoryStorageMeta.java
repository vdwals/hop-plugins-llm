package org.apache.hop.langchain4j.storages.inmemory;

import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.storages.IStorage;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.Getter;
import lombok.Setter;

@GuiPlugin(id = "GUI-InMemoryStorageMeta")
@Getter
@Setter
public class InMemoryStorageMeta implements IStorage {
    private static final Class<?> PKG = InMemoryStorageMeta.class;

    public static final String NAME = BaseMessages.getString(PKG, "InMemory.label.Name");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public EmbeddingStore<TextSegment> getEmbeddingStore(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables) {
        return new InMemoryEmbeddingStore<TextSegment>();
    }

    public IStorage clone() {
        try {
            return (IStorage) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

}
