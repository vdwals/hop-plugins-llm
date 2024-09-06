package org.apache.hop.langchain4j.models.onnx;

import org.apache.hop.core.gui.plugin.ITypeFilename;
import org.apache.hop.i18n.BaseMessages;

public class OnnxFileTypeFilename implements ITypeFilename {
    private static final Class<?> PKG = OnnxFileTypeFilename.class; // For Translator

    @Override
    public String getDefaultFileExtension() {
        return ".onnx";
    }

    @Override
    public String[] getFilterExtensions() {
        return new String[] { "*.onnx", "*" };
    }

    @Override
    public String[] getFilterNames() {
        return new String[] { BaseMessages.getString(PKG, "Onnx.FileType.Onnx"),
                BaseMessages.getString(PKG, "System.FileType.AllFiles") };
    }

}
