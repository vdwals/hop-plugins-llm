package org.apache.hop.langchain4j.languagemodels;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.langchain4j.languagemodels.ollama.OllamaModel;
import org.apache.hop.metadata.api.IHopMetadataObjectFactory;

public class LanguageModelObjectFactory implements IHopMetadataObjectFactory {

    @Override
    public Object createObject(String id, Object parentObject) throws HopException {
        if (id.equals(OllamaModel.NAME))
            return new OllamaModel();
        return null;
    }

    @Override
    public String getObjectId(Object object) throws HopException {
        if (object instanceof ILanguageModel)
            return ((ILanguageModel) object).getName();
        return null;
    }

}