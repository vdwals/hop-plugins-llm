package org.apache.hop.langchain4j.models;

import org.apache.hop.core.gui.plugin.GuiElementType;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.GuiWidgetElement;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.HopMetadataProperty;

@GuiPlugin(id = "GUI-OnnxModelMeta")
public class OnnxModelMeta implements Cloneable, IModel {
    private static final Class<?> PKG = OnnxModelMeta.class;

    @GuiWidgetElement(id = "modelPath", order = "10", parentId = ModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.FILENAME, label = "i18n::Onnx.label.ModelPath")
    @HopMetadataProperty(key = "modelPath")
    private String modelPath;

    @GuiWidgetElement(id = "tokenizerPath", order = "20", parentId = ModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.FILENAME, label = "i18n::Onnx.label.TokenizerPath")
    @HopMetadataProperty(key = "tokenizerPath")
    private String tokenizerPath;

    @Override
    public String getName() {
        return BaseMessages.getString(PKG, "Onnx.label.Name");
    }
}
