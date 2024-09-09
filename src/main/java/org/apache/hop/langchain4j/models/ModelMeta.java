package org.apache.hop.langchain4j.models;

import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.hop.langchain4j.models.onnx.OnnxModelMeta;

@HopMetadata(key = "ll-model", name = "LL-Model", description = "Central setting for connection to diverse LLM provider")
@Getter
@Setter
@NoArgsConstructor
public class ModelMeta extends HopMetadataBase implements Cloneable {
    public static final String GUI_PLUGIN_ELEMENT_PARENT_ID = "ModelMeta-PluginSpecific-Options";

    @HopMetadataProperty(key = "model")
    private IModel model;

    private String type;

    public ModelMeta(String name, String type) {
        this.name = name;
        setModelType(type);
    }

    public ModelMeta(ModelMeta modelMeta) {
        this(modelMeta.getName(), modelMeta.type);
        this.model = modelMeta.model.clone();
    }

    public void setModelType(String newTypeName) {
        if (newTypeName.equals(OnnxModelMeta.NAME))
            this.model = new OnnxModelMeta();

        if (this.model != null) {
            this.type = newTypeName;
        }
    }

    public Class<? extends IModel> getModel(String newTypeName) {
        if (newTypeName.equals(OnnxModelMeta.NAME))
            return OnnxModelMeta.class;

        return null;
    }

    public IModel getModel() {
        if (model == null)
            return new OnnxModelMeta();
        return model;
    }

    public void setModel(IModel model) {
        this.type = model.getName();
        this.model = model;
    }
}
