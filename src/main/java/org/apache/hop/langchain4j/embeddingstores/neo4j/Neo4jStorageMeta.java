package org.apache.hop.langchain4j.embeddingstores.neo4j;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.gui.plugin.GuiElementType;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.GuiWidgetElement;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.embeddingstores.EmbeddingStoreMeta;
import org.apache.hop.langchain4j.embeddingstores.IStorage;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.neo4j.shared.NeoConnection;
import org.apache.hop.neo4j.shared.NeoConnectionTypeMetadata;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingStore;
import lombok.Getter;
import lombok.Setter;

@GuiPlugin(id = "GUI-Neo4jStorageMeta")
@Getter
@Setter
public class Neo4jStorageMeta implements IStorage {
    private static final Class<?> PKG = Neo4jStorageMeta.class;

    public static final String NAME = BaseMessages.getString(PKG, "Neo4j.label.Name");

    @GuiWidgetElement(id = "neoConnectionName", order = "10", parentId = EmbeddingStoreMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.METADATA, typeMetadata = NeoConnectionTypeMetadata.class, label = "i18n::Neo4j.label.neo4jconnectionname")
    @HopMetadataProperty(key = "neoConnectionName")
    private String neoConnectionName;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public EmbeddingStore<TextSegment> getEmbeddingStore(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables) {
        try {
            NeoConnection neoConnection = metadataProvider.getSerializer(NeoConnection.class)
                    .load(variables.resolve(neoConnectionName));

            return Neo4jEmbeddingStore.builder().driver(neoConnection.getDriver(log, variables))
                    .dimension(1536).databaseName(variables.resolve(neoConnection.getDatabaseName())).build();
        } catch (HopException e) {
            log.logError("Unable to get or create Neo4j database driver for database '"
                    + neoConnectionName + "'", e);
            return null;
        }
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
