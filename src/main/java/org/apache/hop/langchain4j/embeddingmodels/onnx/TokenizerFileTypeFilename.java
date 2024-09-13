package org.apache.hop.langchain4j.embeddingmodels.onnx;

import org.apache.hop.core.gui.plugin.ITypeFilename;
import org.apache.hop.i18n.BaseMessages;

public class TokenizerFileTypeFilename implements ITypeFilename {
    private static final Class<?> PKG = TokenizerFileTypeFilename.class; // For Translator

    @Override
    public String getDefaultFileExtension() {
        return ".json";
    }

    @Override
    public String[] getFilterExtensions() {
        return new String[] { "*.json", "*" };
    }

    @Override
    public String[] getFilterNames() {

        return new String[] { BaseMessages.getString(PKG, "System.FileType.JsonFiles"),
                BaseMessages.getString(PKG, "System.FileType.AllFiles") };
    }

}
