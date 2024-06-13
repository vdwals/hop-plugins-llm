package de.dvdw.hop.llm;

import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransform;
import org.apache.hop.pipeline.transform.ITransformData;

public abstract class LlmPluginMeta<Main extends ITransform, Data extends ITransformData> extends BaseTransformMeta<Main, Data>
{
  /** llm type */
  @HopMetadataProperty(key = "llm", storeWithCode = true)
  private LlmModel llmModel;
  
  @HopMetadataProperty(key = "onnxfilename")
  private String onnxFilename;

  @HopMetadataProperty(key = "tokenizerfilename")
  private String tokenizerFilename;

  @HopMetadataProperty(key = "openaiapikey")
  private String openAiApiKey;
  
  public LlmPluginMeta() {
    super();
    setDefault();
  }
  
  public LlmPluginMeta(LlmPluginMeta<Main, Data> m) {
    this();
    setLlmModel(m.getLlmModel());
    setOnnxFilename(m.getOnnxFilename());
    setTokenizerFilename(m.getTokenizerFilename());
    setOpenAiApiKey(m.getOpenAiApiKey());
  }

  @Override
  public void setDefault() {
    setLlmModel(LlmModel.ONNX_MODEL);
    setOnnxFilename(null);
    setTokenizerFilename(null);
    setOpenAiApiKey(null);
  }

  public LlmModel getLlmModel() {
    return llmModel;
  }

  public void setLlmModel(LlmModel llmModel) {
    this.llmModel = llmModel;
  }

  public String getOnnxFilename() {
    return onnxFilename;
  }

  public void setOnnxFilename(String onnxFilename) {
    this.onnxFilename = onnxFilename;
  }

  public String getTokenizerFilename() {
    return tokenizerFilename;
  }

  public void setTokenizerFilename(String tokenizerFilename) {
    this.tokenizerFilename = tokenizerFilename;
  }

  public String getOpenAiApiKey() {
    return openAiApiKey;
  }

  public void setOpenAiApiKey(String openAiApiKey) {
    this.openAiApiKey = openAiApiKey;
  }
}
