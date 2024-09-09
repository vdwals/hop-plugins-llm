package org.apache.hop.langchain4j.models;

import org.apache.hop.metadata.api.HopMetadataObject;

@HopMetadataObject(objectFactory = ModelMetaObjectFactory.class)
public interface IModel {
    public String getName();

    public String getPluginId();

    public void setPluginId(String pluginId);

    public String getPluginName();

    public void setPluginName(String name);
}
