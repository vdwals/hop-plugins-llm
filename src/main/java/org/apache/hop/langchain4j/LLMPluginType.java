package org.apache.hop.langchain4j;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.core.Const;
import org.apache.hop.core.plugins.BasePluginType;

/** This class represents the transform plugin type. */
public class LLMPluginType extends BasePluginType<LLMMetaPlugin> {
    private static LLMPluginType pluginType;
  
    private LLMPluginType() {
      super(LLMMetaPlugin.class, "LLM", "LLMPlugin");
  
      String sharedJdbcFolders =
          Const.NVL(System.getProperty(Const.HOP_SHARED_JDBC_FOLDERS), "lib/jdbc");
      if (StringUtils.isNotEmpty(sharedJdbcFolders)) {
        for (String sharedJdbcFolder : sharedJdbcFolders.split(",")) {
          getExtraLibraryFolders().add(sharedJdbcFolder.trim());
        }
      }
    }
  
    public static LLMPluginType getInstance() {
      if (pluginType == null) {
        pluginType = new LLMPluginType();
      }
      return pluginType;
    }
  
    public String[] getNaturalCategoriesOrder() {
      return new String[0];
    }
  
    @Override
    protected String extractCategory(LLMMetaPlugin annotation) {
      return "";
    }
  
    @Override
    protected String extractDesc(LLMMetaPlugin annotation) {
      return annotation.typeDescription();
    }
  
    @Override
    protected String extractID(LLMMetaPlugin annotation) {
      return annotation.type();
    }
  
    @Override
    protected String extractName(LLMMetaPlugin annotation) {
      return annotation.typeDescription();
    }
  
    @Override
    protected String extractImageFile(LLMMetaPlugin annotation) {
      return null;
    }
  
    @Override
    protected boolean extractSeparateClassLoader(LLMMetaPlugin annotation) {
      return false;
    }
  
    @Override
    protected void addExtraClasses(
        Map<Class<?>, String> classMap, Class<?> clazz, LLMMetaPlugin annotation) {}
  
    @Override
    protected String extractDocumentationUrl(LLMMetaPlugin annotation) {
      return annotation.documentationUrl();
    }
  
    @Override
    protected String extractCasesUrl(LLMMetaPlugin annotation) {
      return null;
    }
  
    @Override
    protected String extractForumUrl(LLMMetaPlugin annotation) {
      return null;
    }
  
    @Override
    protected String extractSuggestion(LLMMetaPlugin annotation) {
      return null;
    }
  
    @Override
    protected String extractClassLoaderGroup(LLMMetaPlugin annotation) {
      return annotation.classLoaderGroup();
    }
  }