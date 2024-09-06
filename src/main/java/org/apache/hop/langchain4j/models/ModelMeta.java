package org.apache.hop.langchain4j.models;

import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;

@HopMetadata(key = "llmModel", name = "LLM Model", description = "Central setting for connection to diverse LLM provider")
public class ModelMeta extends HopMetadataBase implements Cloneable {
    public static final String GUI_PLUGIN_ELEMENT_PARENT_ID = "ModelMeta-PluginSpecific-Options";

    private IModel model;

    public IModel getModel() {
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
        this.model = new OnnxModelMeta();
    }

    public String getPluginName() {
        if (model == null)
            return "";
        return model.getName();
    }
}
