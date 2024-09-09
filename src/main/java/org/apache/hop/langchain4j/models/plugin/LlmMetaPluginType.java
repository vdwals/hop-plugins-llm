package org.apache.hop.langchain4j.models.plugin;

import java.util.Map;

import org.apache.hop.core.plugins.BasePluginType;

public class LlmMetaPluginType extends BasePluginType<LlmMetaPlugin> {
    private static LlmMetaPluginType pluginType;

    private LlmMetaPluginType() {
        super(LlmMetaPlugin.class, "LLM", "LLM");
    }

    public static LlmMetaPluginType getInstance() {
        if (pluginType == null) {
            pluginType = new LlmMetaPluginType();
        }
        return pluginType;
    }

    public String[] getNaturalCategoriesOrder() {
        return new String[0];
    }

    @Override
    protected String extractCategory(LlmMetaPlugin annotation) {
        return "";
    }

    @Override
    protected String extractDesc(LlmMetaPlugin annotation) {
        return annotation.typeDescription();
    }

    @Override
    protected String extractID(LlmMetaPlugin annotation) {
        return annotation.type();
    }

    @Override
    protected String extractName(LlmMetaPlugin annotation) {
        return annotation.typeDescription();
    }

    @Override
    protected String extractImageFile(LlmMetaPlugin annotation) {
        return null;
    }

    @Override
    protected boolean extractSeparateClassLoader(LlmMetaPlugin annotation) {
        return false;
    }

    @Override
    protected void addExtraClasses(
            Map<Class<?>, String> classMap, Class<?> clazz, LlmMetaPlugin annotation) {
    }

    @Override
    protected String extractDocumentationUrl(LlmMetaPlugin annotation) {
        return annotation.documentationUrl();
    }

    @Override
    protected String extractCasesUrl(LlmMetaPlugin annotation) {
        return null;
    }

    @Override
    protected String extractForumUrl(LlmMetaPlugin annotation) {
        return null;
    }

    @Override
    protected String extractSuggestion(LlmMetaPlugin annotation) {
        return null;
    }

    @Override
    protected String extractClassLoaderGroup(LlmMetaPlugin annotation) {
        return annotation.classLoaderGroup();
    }
}
