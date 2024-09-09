package org.apache.hop.langchain4j.models;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.plugins.IPlugin;
import org.apache.hop.core.plugins.PluginRegistry;
import org.apache.hop.langchain4j.models.plugin.LlmMetaPluginType;
import org.apache.hop.metadata.api.IHopMetadataObjectFactory;

public class ModelMetaObjectFactory implements IHopMetadataObjectFactory {

    @Override
    public Object createObject(String id, Object parentObject) throws HopException {
        PluginRegistry registry = PluginRegistry.getInstance();
        IPlugin plugin = registry.findPluginWithId(LlmMetaPluginType.class, id);
        IModel iModel = (IModel) registry.loadClass(plugin);
        return iModel;
    }

    @Override
    public String getObjectId(Object object) throws HopException {
        if (!(object instanceof IModel)) {
            throw new HopException(
                    "Object is not of class IModel but of " + object.getClass().getName() + "'");
        }
        return ((IModel) object).getPluginId();
    }

}
