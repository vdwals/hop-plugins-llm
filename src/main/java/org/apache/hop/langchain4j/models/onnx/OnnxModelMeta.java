package org.apache.hop.langchain4j.models.onnx;

import org.apache.hop.core.gui.plugin.GuiElementType;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.GuiWidgetElement;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.models.IModel;
import org.apache.hop.langchain4j.models.ModelMeta;
import org.apache.hop.langchain4j.models.plugin.LlmMetaPlugin;
import org.apache.hop.metadata.api.HopMetadataProperty;

@LlmMetaPlugin(type = "OnnX", typeDescription = "Offline Onnx Model")
@GuiPlugin(id = "GUI-OnnxModelMeta")
public class OnnxModelMeta implements Cloneable, IModel {
    private static final Class<?> PKG = OnnxModelMeta.class;

    public static final String NAME = BaseMessages.getString(PKG, "Onnx.label.Name");

    @GuiWidgetElement(id = "modelPath", order = "10", parentId = ModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.FILENAME, label = "i18n::Onnx.label.ModelPath", typeFilename = OnnxFileTypeFilename.class)
    @HopMetadataProperty(key = "modelPath")
    private String modelPath;

    @GuiWidgetElement(id = "tokenizerPath", order = "20", parentId = ModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.FILENAME, label = "i18n::Onnx.label.TokenizerPath", typeFilename = TokenizerFileTypeFilename.class)
    @HopMetadataProperty(key = "tokenizerPath")
    private String tokenizerPath;

    private String pluginId;

    private String pluginName;

    @Override
    public String getName() {
        return NAME;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getTokenizerPath() {
        return tokenizerPath;
    }

    public void setTokenizerPath(String tokenizerPath) {
        this.tokenizerPath = tokenizerPath;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }
}
