package org.apache.hop.langchain4j.embeddingmodels;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.langchain4j.embeddingmodels.onnx.OnnxModelMeta;
import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@HopMetadata(key = "embeddingModel", name = "Embedding Model", description = "Central setting for embedding Models", image = "EmbeddingModel.svg")
@Getter
@Setter
@NoArgsConstructor
public class EmbeddingModelMeta extends HopMetadataBase implements Cloneable {
    public static final String GUI_PLUGIN_ELEMENT_PARENT_ID = "EmbeddingModel-PluginSpecific-Options";
    private final ModelObjectFactory modelFactory = new ModelObjectFactory();

    @HopMetadataProperty(key = "model")
    private IModel model;

    public EmbeddingModelMeta(String name) {
        this.name = name;
    }

    public EmbeddingModelMeta(EmbeddingModelMeta llmMeta) {
        this(llmMeta.getName());
        this.model = llmMeta.model.clone();
    }

    public IModel getModel() {
        if (model == null)
            setModel(new OnnxModelMeta());
        return model;
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
}
