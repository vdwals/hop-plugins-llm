/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.semanticsearch;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.CheckResult;
import org.apache.hop.core.Const;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IEnumHasCode;
import org.apache.hop.metadata.api.IEnumHasCodeAndDescription;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformIOMeta;
import org.apache.hop.pipeline.transform.TransformIOMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transform.stream.IStream;
import org.apache.hop.pipeline.transform.stream.IStream.StreamType;
import org.apache.hop.pipeline.transform.stream.Stream;
import org.apache.hop.pipeline.transform.stream.StreamIcon;

@Transform(
    id = "SemanticSearch",
    image = "SemanticSearch.svg",
    name = "Semantic Search",
    description = "i18n::SemanticSearch.Description",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Lookup",
    keywords = "i18n::SemanticSearchMeta.keyword")
public class SemanticSearchMeta extends BaseTransformMeta<SemanticSearch, SemanticSearchData> {
  private static final Class<?> PKG = SemanticSearchMeta.class; // For Translator

  /** Embedding store type */
  @HopMetadataProperty(key = "embeddingStore", storeWithCode = true)
  private SEmbeddingStore embeddingStore;

  /** Embedding model type */
  @HopMetadataProperty(key = "embeddingModel", storeWithCode = true)
  private SEmbeddingModel embeddingModel;
  
  @HopMetadataProperty(key = "onnxFileName")
  private String onnxFilename;

  @HopMetadataProperty(key = "tokenizerFileName")
  private String tokenizerFilename;

  @HopMetadataProperty(key = "from")
  private String lookupTransformName;

  /** field in lookup stream with which we look up values */
  @HopMetadataProperty(key = "lookupfield")
  private String lookupField;

  /** field in input stream for which we lookup values */
  @HopMetadataProperty(key = "mainstreamfield")
  private String mainStreamField;

  /** output match fieldname */
  @HopMetadataProperty(key = "outputmatchfield")
  private String outputMatchField;

  /** return these field values from lookup */
  @HopMetadataProperty(groupKey = "lookup", key = "value")
  private List<SLookupValue> lookupValues;

  public SemanticSearchMeta() {
    super();
    this.setEmbeddingStore(SEmbeddingStore.IN_MEMORY);
    this.setEmbeddingModel(SEmbeddingModel.ONNX_MODEL);
    this.lookupValues = new ArrayList<>();
  }

  public SemanticSearchMeta(SemanticSearchMeta m) {
    this();
    this.setEmbeddingStore(m.getEmbeddingStore());
    this.setEmbeddingModel(m.getEmbeddingModel());
    this.lookupField = m.lookupField;
    this.mainStreamField = m.mainStreamField;
    this.outputMatchField = m.outputMatchField;
    m.lookupValues.forEach(v -> this.lookupValues.add(new SLookupValue(v)));
  }

  @Override
  public SemanticSearchMeta clone() {
    return new SemanticSearchMeta(this);
  }

  @Override
  public void setDefault() {
    setEmbeddingStore(SEmbeddingStore.IN_MEMORY);
    setEmbeddingModel(SEmbeddingModel.ONNX_MODEL);
    lookupField = null;
    mainStreamField = null;
    outputMatchField = BaseMessages.getString(PKG, "SemanticSearchMeta.OutputMatchFieldname");
  }

  @Override
  public void getFields(
      IRowMeta inputRowMeta,
      String name,
      IRowMeta[] info,
      TransformMeta nextTransform,
      IVariables variables,
      IHopMetadataProvider metadataProvider)
      throws HopTransformException {
    // Add match field
    IValueMeta v = new ValueMetaString(variables.resolve(getOutputMatchField()));
    v.setOrigin(name);
    v.setStorageType(IValueMeta.STORAGE_TYPE_NORMAL);
    inputRowMeta.addValueMeta(v);

    for (SLookupValue lookupValue : lookupValues) {
      v = new ValueMetaString(lookupValue.getName());
      v.setOrigin(name);
      inputRowMeta.addValueMeta(v);
    }
  }

  @Override
  public void check(
      List<ICheckResult> remarks,
      PipelineMeta pipelineMeta,
      TransformMeta transformMeta,
      IRowMeta prev,
      String[] input,
      String[] output,
      IRowMeta info,
      IVariables variables,
      IHopMetadataProvider metadataProvider) {
    CheckResult cr;

    if (prev != null && prev.size() > 0) {
      cr =
          new CheckResult(
              ICheckResult.TYPE_RESULT_OK,
              BaseMessages.getString(
                  PKG, "SemanticSearchMeta.CheckResult.TransformReceivingFields", prev.size() + ""),
              transformMeta);
      remarks.add(cr);

      // Starting from selected fields in ...
      // Check the fields from the previous stream!
      String mainField = variables.resolve(getMainStreamField());
      int idx = prev.indexOfValue(mainField);
      if (idx < 0) {
        cr =
            new CheckResult(
                ICheckResult.TYPE_RESULT_ERROR,
                BaseMessages.getString(
                    PKG, "SemanticSearchMeta.CheckResult.MainFieldNotFound", mainField),
                transformMeta);
      } else {
        cr =
            new CheckResult(
                ICheckResult.TYPE_RESULT_OK,
                BaseMessages.getString(
                    PKG, "SemanticSearchMeta.CheckResult.MainFieldFound", mainField),
                transformMeta);
      }
      remarks.add(cr);

    } else {
      cr =
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(
                  PKG, "SemanticSearchMeta.CheckResult.CouldNotFindFieldsFromPreviousTransforms"),
              transformMeta);
      remarks.add(cr);
    }

    if (info != null && info.size() > 0) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_OK,
              BaseMessages.getString(
                  PKG,
                  "SemanticSearchMeta.CheckResult.TransformReceivingLookupData",
                  info.size() + ""),
              transformMeta));

      // Check the fields from the lookup stream!
      String realLookupField = variables.resolve(getLookupField());

      int idx = info.indexOfValue(realLookupField);
      if (idx < 0) {
        remarks.add(
            new CheckResult(
                ICheckResult.TYPE_RESULT_ERROR,
                BaseMessages.getString(
                    PKG,
                    "SemanticSearchMeta.CheckResult.FieldNotFoundInLookupStream",
                    realLookupField),
                transformMeta));
      } else {
        remarks.add(
            new CheckResult(
                ICheckResult.TYPE_RESULT_OK,
                BaseMessages.getString(
                    PKG,
                    "SemanticSearchMeta.CheckResult.FieldFoundInTheLookupStream",
                    realLookupField),
                transformMeta));
      }

      StringBuilder errorMessage = new StringBuilder();
      boolean errorFound = false;

      // Check the values to retrieve from the lookup stream!
      for (SLookupValue lookupValue : lookupValues) {
        idx = info.indexOfValue(lookupValue.getName());
        if (idx < 0) {
          errorMessage.append("\t\t").append(lookupValue.getName()).append(Const.CR);
          errorFound = true;
        }
      }
      if (errorFound) {
        errorMessage.insert(
            0,
            BaseMessages.getString(
                    PKG, "SemanticSearchMeta.CheckResult.FieldsNotFoundInLookupStream2")
                + Const.CR
                + Const.CR);

        remarks.add(
            new CheckResult(
                ICheckResult.TYPE_RESULT_ERROR, errorMessage.toString(), transformMeta));
      } else {
        remarks.add(
            new CheckResult(
                ICheckResult.TYPE_RESULT_OK,
                BaseMessages.getString(
                    PKG, "SemanticSearchMeta.CheckResult.AllFieldsFoundInTheLookupStream2"),
                transformMeta));
      }
    } else {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(
                  PKG, "SemanticSearchMeta.CheckResult.FieldsNotFoundFromInLookupSep"),
              transformMeta));
    }

    // See if the source transform is filled in!
    IStream infoStream = getTransformIOMeta().getInfoStreams().get(0);
    if (infoStream.getTransformMeta() == null) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(
                  PKG, "SemanticSearchMeta.CheckResult.SourceTransformNotSelected"),
              transformMeta));
    } else {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_OK,
              BaseMessages.getString(
                  PKG, "SemanticSearchMeta.CheckResult.SourceTransformIsSelected"),
              transformMeta));

      // See if the transform exists!
      //
      if (info != null) {
        remarks.add(
            new CheckResult(
                ICheckResult.TYPE_RESULT_OK,
                BaseMessages.getString(
                    PKG,
                    "SemanticSearchMeta.CheckResult.SourceTransformExist",
                    infoStream.getTransformName() + ""),
                transformMeta));
      } else {
        remarks.add(
            new CheckResult(
                ICheckResult.TYPE_RESULT_ERROR,
                BaseMessages.getString(
                    PKG,
                    "SemanticSearchMeta.CheckResult.SourceTransformDoesNotExist",
                    infoStream.getTransformName() + ""),
                transformMeta));
      }
    }

    // See if we have input streams leading to this transform!
    if (input.length >= 2) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_OK,
              BaseMessages.getString(
                  PKG,
                  "SemanticSearchMeta.CheckResult.TransformReceivingInfoFromInputTransforms",
                  input.length + ""),
              transformMeta));
    } else {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(
                  PKG,
                  "SemanticSearchMeta.CheckResult.NeedAtLeast2InputStreams",
                  Const.CR,
                  Const.CR),
              transformMeta));
    }
  }

  @Override
  public void searchInfoAndTargetTransforms(List<TransformMeta> transforms) {
    List<IStream> infoStreams = getTransformIOMeta().getInfoStreams();
    for (IStream stream : infoStreams) {
      stream.setTransformMeta(TransformMeta.findTransform(transforms, stream.getSubject()));
    }
  }

  @Override
  public boolean excludeFromRowLayoutVerification() {
    return true;
  }

  @Override
  public boolean supportsErrorHandling() {
    return true;
  }

  public SEmbeddingStore getEmbeddingStore() {
    return embeddingStore;
  }

  public void setEmbeddingStore(SEmbeddingStore embeddingStore) {
    this.embeddingStore = embeddingStore;
  }

  public SEmbeddingModel getEmbeddingModel() {
    return embeddingModel;
  }

  public void setEmbeddingModel(SEmbeddingModel embeddingModel) {
    this.embeddingModel = embeddingModel;
  }

  /**
   * Returns the Input/Output metadata for this transform. The generator transform only produces
   * output, does not accept input!
   */
  @Override
  public ITransformIOMeta getTransformIOMeta() {
    ITransformIOMeta ioMeta = super.getTransformIOMeta(false);
    if (ioMeta == null) {

      ioMeta = new TransformIOMeta(true, true, false, false, false, false);

      IStream stream =
          new Stream(
              StreamType.INFO,
              null,
              BaseMessages.getString(PKG, "SemanticSearchMeta.InfoStream.Description"),
              StreamIcon.INFO,
              lookupTransformName);
      ioMeta.addStream(stream);
      setTransformIOMeta(ioMeta);
    }

    return ioMeta;
  }

  /**
   * Gets lookupTransformName
   *
   * @return value of lookupTransformName
   */
  public String getLookupTransformName() {
    return lookupTransformName;
  }

  /**
   * Sets lookupTransformName
   *
   * @param lookupTransformName value of lookupTransformName
   */
  public void setLookupTransformName(String lookupTransformName) {
    this.lookupTransformName = lookupTransformName;
  }

  /**
   * Gets lookupField
   *
   * @return value of lookupField
   */
  public String getLookupField() {
    return lookupField;
  }

  /**
   * Sets lookupField
   *
   * @param lookupField value of lookupField
   */
  public void setLookupField(String lookupField) {
    this.lookupField = lookupField;
  }

  /**
   * Gets mainStreamField
   *
   * @return value of mainStreamField
   */
  public String getMainStreamField() {
    return mainStreamField;
  }

  /**
   * Sets mainStreamField
   *
   * @param mainStreamField value of mainStreamField
   */
  public void setMainStreamField(String mainStreamField) {
    this.mainStreamField = mainStreamField;
  }

  /**
   * Gets outputmatchfield
   *
   * @return value of outputmatchfield
   */
  public String getOutputMatchField() {
    return outputMatchField;
  }

  /**
   * Sets outputmatchfield
   *
   * @param outputMatchField value of outputmatchfield
   */
  public void setOutputMatchField(String outputMatchField) {
    this.outputMatchField = outputMatchField;
  }

  /**
   * Gets lookupValues
   *
   * @return value of lookupValues
   */
  public List<SLookupValue> getLookupValues() {
    return lookupValues;
  }

  public String getOnnxFilename() {
    return onnxFilename;
  }

  public void setOnnxFilename(String filename) {
    this.onnxFilename = filename;
  }

  public String getTokenizerFilename() {
    return tokenizerFilename;
  }

  public void setTokenizerFilename(String tokenizerFilename) {
    this.tokenizerFilename = tokenizerFilename;
  }

  /**
   * Sets lookupValues
   *
   * @param lookupValues value of lookupValues
   */
  public void setLookupValues(List<SLookupValue> lookupValues) {
    this.lookupValues = lookupValues;
  }

  public enum SEmbeddingStore implements IEnumHasCodeAndDescription {
    IN_MEMORY("inmemory", BaseMessages.getString(PKG, "SemanticSearchMeta.embeddingstore.inmemory"));
    private final String code;
    private final String description;

    SEmbeddingStore(String code, String description) {
      this.code = code;
      this.description = description;
    }

    public static String[] getDescriptions() {
      return IEnumHasCodeAndDescription.getDescriptions(SEmbeddingStore.class);
    }

    public static SEmbeddingStore lookupDescription(String description) {
      return IEnumHasCodeAndDescription.lookupDescription(
          SEmbeddingStore.class, description, IN_MEMORY);
    }

    public static SEmbeddingStore lookupCode(String code) {
      return IEnumHasCode.lookupCode(SEmbeddingStore.class, code, IN_MEMORY);
    }

    /**
     * Gets code
     *
     * @return value of code
     */
    @Override
    public String getCode() {
      return code;
    }

    /**
     * Gets description
     *
     * @return value of description
     */
    @Override
    public String getDescription() {
      return description;
    }
  }

  public enum SEmbeddingModel implements IEnumHasCodeAndDescription {
    ONNX_MODEL(
        "onnx", BaseMessages.getString(PKG, "SemanticSearchMeta.embeddingmodel.onnx"));
    private final String code;
    private final String description;

    SEmbeddingModel(String code, String description) {
      this.code = code;
      this.description = description;
    }

    public static String[] getDescriptions() {
      return IEnumHasCodeAndDescription.getDescriptions(SEmbeddingModel.class);
    }

    public static SEmbeddingModel lookupDescription(String description) {
      return IEnumHasCodeAndDescription.lookupDescription(
          SEmbeddingModel.class, description, ONNX_MODEL);
    }

    public static SEmbeddingModel lookupCode(String code) {
      return IEnumHasCode.lookupCode(SEmbeddingModel.class, code, ONNX_MODEL);
    }

    /**
     * Gets code
     *
     * @return value of code
     */
    @Override
    public String getCode() {
      return code;
    }

    /**
     * Gets description
     *
     * @return value of description
     */
    @Override
    public String getDescription() {
      return description;
    }
  }

  public static final class SLookupValue {
    @HopMetadataProperty(key = "name")
    private String name;

    @HopMetadataProperty(key = "rename")
    private String rename;

    public SLookupValue() {}

    public SLookupValue(SLookupValue v) {
      this.name = v.name;
      this.rename = v.rename;
    }

    public SLookupValue(String name, String rename) {
      this.name = name;
      this.rename = rename;
    }

    /**
     * Gets name
     *
     * @return value of name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets name
     *
     * @param name value of name
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Gets rename
     *
     * @return value of rename
     */
    public String getRename() {
      return rename;
    }

    /**
     * Sets rename
     *
     * @param rename value of rename
     */
    public void setRename(String rename) {
      this.rename = rename;
    }
  }
}
