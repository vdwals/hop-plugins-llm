package org.apache.hop.langchain4j.languagemodels;

import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.HopMetadataObject;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import dev.langchain4j.model.chat.ChatLanguageModel;

@HopMetadataObject(objectFactory = LanguageModelObjectFactory.class)
public interface ILanguageModel {

    String getName();

    public ILanguageModel clone();

    public ChatLanguageModel getChatModel(IHopMetadataProvider metadataProvider, ILogChannel log,
            IVariables variables);
}
