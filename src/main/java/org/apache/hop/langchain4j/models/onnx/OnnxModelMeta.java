package org.apache.hop.langchain4j.models.onnx;

import org.apache.commons.lang3.ClassLoaderUtils;
import org.apache.hop.core.gui.plugin.GuiElementType;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.GuiWidgetElement;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.models.IModel;
import org.apache.hop.langchain4j.models.ModelMeta;
import org.apache.hop.metadata.api.HopMetadataProperty;
import java.util.Map;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import lombok.Getter;
import lombok.Setter;

@GuiPlugin(id = "GUI-OnnxModelMeta")
@Getter
@Setter
public class OnnxModelMeta implements IModel {
    private static final Class<?> PKG = OnnxModelMeta.class;

    private static final String KEY_ONNX_PATH = "onnx_path";
    private static final String KEY_TOKEN_PATH = "token_path";

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

    public EmbeddingModel getEmbeddingModel(Map<String, String> attributes) {

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(ClassLoaderUtils.class.getClassLoader());
        EmbeddingModel onnxEmbeddingModel = new OnnxEmbeddingModel(attributes.get(KEY_ONNX_PATH),
                attributes.get(KEY_TOKEN_PATH), PoolingMode.MEAN);

        Thread.currentThread().setContextClassLoader(contextClassLoader);

        return onnxEmbeddingModel;
    }

    public Map<String, String> getAttributeMap() {
        return Map.of(KEY_ONNX_PATH, modelPath, KEY_TOKEN_PATH, tokenizerPath);
    }
}
