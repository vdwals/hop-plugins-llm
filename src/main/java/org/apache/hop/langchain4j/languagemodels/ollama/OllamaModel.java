package org.apache.hop.langchain4j.languagemodels.ollama;

import java.time.Duration;
import java.util.List;

import org.apache.hop.core.gui.plugin.GuiElementType;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.GuiWidgetElement;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.languagemodels.ILanguageModel;
import org.apache.hop.langchain4j.languagemodels.LanguageModelMeta;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.Getter;
import lombok.Setter;

@GuiPlugin(id = "GUI-OllamaMeta")
@Getter
@Setter
public class OllamaModel implements ILanguageModel {
    private static final Class<?> PKG = OllamaModel.class;
    private static final String URL_TEMPLATE = "%s://%s:%s";
    public static final String NAME = BaseMessages.getString(PKG, "Ollama.label.Name");

    @Override
    public String getName() {
        return NAME;
    }

    @GuiWidgetElement(id = "protocol", order = "5", parentId = LanguageModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.COMBO, variables = false, comboValuesMethod = "getProtocols", label = "i18n::Ollama.label.protocol")
    @HopMetadataProperty(key = "protocol")
    private String protocol;

    @GuiWidgetElement(id = "host", order = "10", parentId = LanguageModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.TEXT, label = "i18n::Ollama.label.host")
    @HopMetadataProperty(key = "host")
    private String host = "localhost";

    @GuiWidgetElement(id = "port", order = "20", parentId = LanguageModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.TEXT, label = "i18n::Ollama.label.port")
    @HopMetadataProperty(key = "port")
    private String port = "11434";

    @GuiWidgetElement(id = "modelName", order = "30", parentId = LanguageModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID, type = GuiElementType.TEXT, label = "i18n::Ollama.label.modelName")
    @HopMetadataProperty(key = "modelName")
    private String modelName = "llama3.1";

    public ILanguageModel clone() {
        try {
            return (ILanguageModel) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ChatLanguageModel getChatModel(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables) {
        String baseUrl = String.format(URL_TEMPLATE, protocol, host, port);

        return OllamaChatModel.builder().baseUrl(baseUrl).modelName(modelName).timeout(Duration.ofMinutes(10)).build();
    }

    public List<String> getProtocols(ILogChannel log, IHopMetadataProvider metadataProvider) {
        return List.of("http", "https");
    }
}
