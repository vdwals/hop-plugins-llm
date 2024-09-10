package org.apache.hop.langchain4j.models;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.langchain4j.models.onnx.OnnxModelMeta;
import org.apache.hop.metadata.api.IHopMetadataObjectFactory;

public class ModelMetaObjectFactory implements IHopMetadataObjectFactory {

    @Override
    public Object createObject(String id, Object parentObject) throws HopException {
        if (id.equals(OnnxModelMeta.NAME))
            return new OnnxModelMeta();
        return null;
    }

    @Override
    public String getObjectId(Object object) throws HopException {
        if (object instanceof IModel)
            return ((IModel) object).getName();
        return null;
    }

}