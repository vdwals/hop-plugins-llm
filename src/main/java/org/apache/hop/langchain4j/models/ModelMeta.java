package org.apache.hop.langchain4j.models;

import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.langchain4j.models.onnx.OnnxModelMeta;

@HopMetadata(key = "llmModel", name = "LLM Model", description = "Central setting for connection to diverse LLM provider")
public class ModelMeta extends HopMetadataBase implements Cloneable {
    public static final String GUI_PLUGIN_ELEMENT_PARENT_ID = "ModelMeta-PluginSpecific-Options";

    private IModel model;

    public IModel getModel() {
        if (model == null)
            return new OnnxModelMeta();
        return model;
    }

    public void setModel(IModel model) {
        this.model = model;
    }

    private String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void setModelType(String newTypeName) {
        if (newTypeName.equals(OnnxModelMeta.NAME))
            this.model = new OnnxModelMeta();
    }

    public Class<? extends IModel> getModel(String newTypeName) {
        if (newTypeName.equals(OnnxModelMeta.NAME))
            return OnnxModelMeta.class;

        return null;
    }

    public String getPluginId() {
        return model.getPluginId();
    }

    public void setPluginId(String pluginId) {
        model.setPluginId(pluginId);
    }
}
