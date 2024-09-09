package org.apache.hop.langchain4j.models.onnx;

import org.apache.hop.core.gui.plugin.GuiElementType;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.GuiWidgetElement;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.models.IModel;
import org.apache.hop.langchain4j.models.ModelMeta;
import org.apache.hop.metadata.api.HopMetadataProperty;

import lombok.Getter;
import lombok.Setter;

@GuiPlugin(id = "GUI-OnnxModelMeta")
@Getter
@Setter
public class OnnxModelMeta implements IModel {
    private static final Class<?> PKG = OnnxModelMeta.class;

    public static final String NAME = BaseMessages.getString(PKG, "Onnx.label.Name");

    @GuiWidgetElement(id = "modelPath", order = "10", parentId = ModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.FILENAME, label = "i18n::Onnx.label.ModelPath", typeFilename = OnnxFileTypeFilename.class)
    @HopMetadataProperty(key = "modelPath")
    private String modelPath;

    @GuiWidgetElement(id = "tokenizerPath", order = "20", parentId = ModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.FILENAME, label = "i18n::Onnx.label.TokenizerPath", typeFilename = TokenizerFileTypeFilename.class)
    @HopMetadataProperty(key = "tokenizerPath")
    private String tokenizerPath;

    public String getName() {
        return NAME;
    }

    public IModel clone() {
        try {
            return (IModel) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
