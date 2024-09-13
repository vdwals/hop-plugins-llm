package org.apache.hop.langchain4j.embeddingmodels.onnx;

import org.apache.commons.lang3.ClassLoaderUtils;
import org.apache.hop.core.gui.plugin.GuiElementType;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.GuiWidgetElement;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.embeddingmodels.EmbeddingModelMeta;
import org.apache.hop.langchain4j.embeddingmodels.IModel;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;

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

    public static final String NAME = BaseMessages.getString(PKG, "Onnx.label.Name");

    @GuiWidgetElement(id = "modelPath", order = "10", parentId = EmbeddingModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.FILENAME, label = "i18n::Onnx.label.ModelPath", typeFilename = OnnxFileTypeFilename.class)
    @HopMetadataProperty(key = "modelPath")
    private String modelPath;

    @GuiWidgetElement(id = "tokenizerPath", order = "20", parentId = EmbeddingModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.FILENAME, label = "i18n::Onnx.label.TokenizerPath", typeFilename = TokenizerFileTypeFilename.class)
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

    public EmbeddingModel getEmbeddingModel(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables) {

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(ClassLoaderUtils.class.getClassLoader());
        EmbeddingModel onnxEmbeddingModel = new OnnxEmbeddingModel(variables.resolve(modelPath),
                variables.resolve(tokenizerPath), PoolingMode.MEAN);

        Thread.currentThread().setContextClassLoader(contextClassLoader);

        return onnxEmbeddingModel;
    }
}
