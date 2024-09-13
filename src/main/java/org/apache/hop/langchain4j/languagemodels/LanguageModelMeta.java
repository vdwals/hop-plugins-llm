package org.apache.hop.langchain4j.languagemodels;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.langchain4j.languagemodels.ollama.OllamaModel;
import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@HopMetadata(key = "languagemodel", name = "Language Model", description = "Central setting for connection to language Models", image = "LanguageModel.svg")
@Getter
@Setter
@NoArgsConstructor
public class LanguageModelMeta extends HopMetadataBase implements Cloneable {
    public static final String GUI_PLUGIN_ELEMENT_PARENT_ID = "LanguageModelMeta-PluginSpecific-Options";
    private final LanguageModelObjectFactory languageModelFactory = new LanguageModelObjectFactory();

    @HopMetadataProperty(key = "model")
    private ILanguageModel model;

    public LanguageModelMeta(String name) {
        this.name = name;
    }

    public LanguageModelMeta(LanguageModelMeta llmMeta) {
        this(llmMeta.getName());
        this.model = llmMeta.model.clone();
    }

    public ILanguageModel getModel() {
        if (model == null)
            setModel(new OllamaModel());
        return model;
    }

    public void setModelByType(String newTypeName) throws HopException {
        Object model = languageModelFactory.createObject(newTypeName, null);
        if (model != null)
            setModel((ILanguageModel) model);
    }

    public ChatLanguageModel getChatModel(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables) {
        return getModel().getChatModel(metadataProvider, log, variables);
    }
}
