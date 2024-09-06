package org.apache.hop.langchain4j.models;

import java.util.Map;

import org.apache.hop.core.changed.ChangedFlag;
import org.apache.hop.core.exception.HopValueException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.IHopMetadata;

@HopMetadata(key = "llmModel", name = "LLM Model", description = "Central setting for connection to diverse LLM provider")
public class ModelMeta extends ChangedFlag implements Cloneable, IVariables, IHopMetadata {
    public static final String GUI_PLUGIN_ELEMENT_PARENT_ID = "ModelMeta-PluginSpecific-Options";

    private IModel model;

    public IModel getModel() {
        return model;
    }

    public void setModel(IModel model) {
        this.model = model;
    }

    private String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void setModelType(String newTypeName) {
        this.model = new OnnxModelMeta();
    }

    public String getPluginName() {
        if (model == null)
            return "";
        return model.getName();
    }

    private IVariables variables = new Variables();

    public void copyFrom(IVariables variables) {
        this.variables.copyFrom(variables);
    }

    public IVariables getParentVariables() {
        return variables.getParentVariables();
    }

    public void setParentVariables(IVariables parent) {
        variables.setParentVariables(parent);
    }

    public String getVariable(String variableName, String defaultValue) {
        return variables.getVariable(variableName, defaultValue);
    }

    public String getVariable(String variableName) {
        return variables.getVariable(variableName);
    }

    public boolean getBooleanValueOfVariable(String variableName, boolean defaultValue) {
        if (!Utils.isEmpty(variableName)) {
            String value = resolve(variableName);
            if (!Utils.isEmpty(value)) {
                return ValueMetaString.convertStringToBoolean(value);
            }
        }
        return defaultValue;
    }

    public void initializeFrom(IVariables parent) {
        variables.initializeFrom(parent);
    }

    public String[] getVariableNames() {
        return variables.getVariableNames();
    }

    public void setVariable(String variableName, String variableValue) {
        variables.setVariable(variableName, variableValue);
    }

    public void shareVariablesWith(IVariables variables) {
        this.variables = variables;
    }

    public void setVariables(Map<String, String> prop) {
        variables.setVariables(prop);
    }

    @Override
    public String getMetadataProviderName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMetadataProviderName'");
    }

    @Override
    public void setMetadataProviderName(String metadataProviderName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setMetadataProviderName'");
    }

    @Override
    public void shareWith(IVariables variables) {
        variables.shareWith(variables);
    }

    @Override
    public boolean getVariableBoolean(String variableName, boolean defaultValue) {
        return variables.getVariableBoolean(variableName, defaultValue);
    }

    @Override
    public String resolve(String aString) {
        return variables.resolve(aString);
    }

    @Override
    public String[] resolve(String[] string) {
        return variables.resolve(string);
    }

    @Override
    public String resolve(String aString, IRowMeta rowMeta, Object[] rowData) throws HopValueException {
        return variables.resolve(aString, rowMeta, rowData);
    }
}
