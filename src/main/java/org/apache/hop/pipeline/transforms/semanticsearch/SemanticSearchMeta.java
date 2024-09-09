/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
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
import org.apache.hop.core.row.value.ValueMetaNumber;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.EmbeddingStore;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformIOMeta;
import org.apache.hop.pipeline.transform.TransformIOMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transform.stream.IStream;
import org.apache.hop.pipeline.transform.stream.IStream.StreamType;

import lombok.Getter;
import lombok.Setter;

import org.apache.hop.pipeline.transform.stream.Stream;
import org.apache.hop.pipeline.transform.stream.StreamIcon;

@Transform(id = "SemanticSearch", image = "SemanticSearch.svg", name = "Semantic Search", description = "i18n::SemanticSearch.Description", categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Lookup", keywords = "i18n::SemanticSearchMeta.keyword")
@Getter
@Setter
public class SemanticSearchMeta extends BaseTransformMeta<SemanticSearch, SemanticSearchData> {
  private static final Class<?> PKG = SemanticSearchMeta.class; // For Translator

  /** Embedding store type */
  @HopMetadataProperty(key = "embeddingstore", storeWithCode = true)
  private EmbeddingStore embeddingStore;

  /** Embedding model type */
  @HopMetadataProperty(key = "llmodel", injectionKey = "llmodel", injectionKeyDescription = "SemanticSearchMeta.Injection.llmodel")
  private String llModelName;

  @HopMetadataProperty(key = "from")
  private String lookupTransformName;

  /** maximal number of results to return */
  @HopMetadataProperty(key = "maximalValue")
  private String maximalValue;

  /** field in lookup stream with which we look up values */
  @HopMetadataProperty(key = "lookuptextfield")
  private String lookupTextField;

  /** field in lookup stream with which we look up values */
  @HopMetadataProperty(key = "lookupkeyfield")
  private String lookupKeyField;

  /** field in input stream for which we lookup values */
  @HopMetadataProperty(key = "mainstreamfield")
  private String mainStreamField;

  /** output match fieldname */
  @HopMetadataProperty(key = "outputmatchfield")
  private String outputMatchField;

  /** output key fieldname */
  @HopMetadataProperty(key = "outputkeyfield")
  private String outputKeyField;

  /** output distance fieldname */
  @HopMetadataProperty(key = "outputdistancefield")
  private String outputDistanceField;

  /** return these field values from lookup */
  @HopMetadataProperty(groupKey = "lookup", key = "value")
  private List<SLookupValue> lookupValues;

  @HopMetadataProperty(key = "neo4jConnection", injectionKey = "neo4jConnection", injectionKeyDescription = "SemanticSearchMeta.Injection.connection")
  private String neo4JConnectionName;

  @HopMetadataProperty(key = "chromaurl")
  private String chromaUrl;

  public SemanticSearchMeta() {
    super();
    this.setEmbeddingStore(EmbeddingStore.IN_MEMORY);
    this.lookupValues = new ArrayList<>();
  }

  public SemanticSearchMeta(SemanticSearchMeta m) {
    this();
    this.setNeo4JConnectionName(m.getNeo4JConnectionName());
    this.setEmbeddingStore(m.getEmbeddingStore());
    this.setLlModelName(m.getLlModelName());
    this.lookupTextField = m.lookupTextField;
    this.setLookupKeyField(m.getLookupKeyField());
    this.mainStreamField = m.mainStreamField;
    this.outputMatchField = m.outputMatchField;
    this.setOutputKeyField(m.getOutputKeyField());
    m.lookupValues.forEach(v -> this.lookupValues.add(new SLookupValue(v)));
    this.maximalValue = m.maximalValue;
    this.setOutputDistanceField(m.getOutputDistanceField());
  }

  @Override
  public SemanticSearchMeta clone() {
    return new SemanticSearchMeta(this);
  }

  @Override
  public void setDefault() {
    setEmbeddingStore(EmbeddingStore.IN_MEMORY);
    setLlModelName(null);
    setNeo4JConnectionName(null);
    lookupTextField = null;
    setLookupKeyField(null);
    mainStreamField = null;
    maximalValue = null;
    outputMatchField = BaseMessages.getString(PKG, "SemanticSearchMeta.OutputMatchFieldname");
    setOutputKeyField(BaseMessages.getString(PKG, "SemanticSearchMeta.OutputKeyFieldname"));
    setOutputDistanceField(
        BaseMessages.getString(PKG, "SemanticSearchMeta.OutputDistanceFieldname"));
  }

  @Override
  public void getFields(IRowMeta inputRowMeta, String name, IRowMeta[] info,
      TransformMeta nextTransform, IVariables variables, IHopMetadataProvider metadataProvider)
      throws HopTransformException {

    if (!Utils.isEmpty(outputMatchField)) {
      // Add match field
      IValueMeta v = new ValueMetaString(variables.resolve(getOutputMatchField()));
      v.setOrigin(name);
      v.setStorageType(IValueMeta.STORAGE_TYPE_NORMAL);
      inputRowMeta.addValueMeta(v);
    }

    if (!Utils.isEmpty(outputKeyField)) {
      // Add distance field
      IValueMeta v = new ValueMetaString(variables.resolve(getOutputKeyField()));
      v.setOrigin(name);
      v.setStorageType(IValueMeta.STORAGE_TYPE_NORMAL);
      inputRowMeta.addValueMeta(v);
    }

    if (!Utils.isEmpty(outputDistanceField)) {
      // Add distance field
      IValueMeta v = new ValueMetaNumber(variables.resolve(getOutputDistanceField()));
      v.setOrigin(name);
      v.setStorageType(IValueMeta.STORAGE_TYPE_NORMAL);
      inputRowMeta.addValueMeta(v);
    }

    for (SLookupValue lookupValue : lookupValues) {
      IValueMeta v = new ValueMetaString(lookupValue.getName());
      v.setOrigin(name);
      inputRowMeta.addValueMeta(v);
    }
  }

  @Override
  public void check(List<ICheckResult> remarks, PipelineMeta pipelineMeta,
      TransformMeta transformMeta, IRowMeta prev, String[] input, String[] output, IRowMeta info,
      IVariables variables, IHopMetadataProvider metadataProvider) {
    CheckResult cr;

    if (prev != null && prev.size() > 0) {
      cr = new CheckResult(
          ICheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG,
              "SemanticSearchMeta.CheckResult.TransformReceivingFields", prev.size() + ""),
          transformMeta);
      remarks.add(cr);

      // Starting from selected fields in ...
      // Check the fields from the previous stream!
      String mainField = variables.resolve(getMainStreamField());
      int idx = prev.indexOfValue(mainField);
      if (idx < 0) {
        cr = new CheckResult(ICheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG,
            "SemanticSearchMeta.CheckResult.MainFieldNotFound", mainField), transformMeta);
      } else {
        cr = new CheckResult(ICheckResult.TYPE_RESULT_OK,
            BaseMessages.getString(PKG, "SemanticSearchMeta.CheckResult.MainFieldFound", mainField),
            transformMeta);
      }
      remarks.add(cr);

    } else {
      cr = new CheckResult(ICheckResult.TYPE_RESULT_ERROR,
          BaseMessages.getString(PKG,
              "SemanticSearchMeta.CheckResult.CouldNotFindFieldsFromPreviousTransforms"),
          transformMeta);
      remarks.add(cr);
    }

    if (info != null && info.size() > 0) {
      remarks.add(new CheckResult(
          ICheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG,
              "SemanticSearchMeta.CheckResult.TransformReceivingLookupData", info.size() + ""),
          transformMeta));

      // Check the fields from the lookup stream!
      String realLookupTextField = variables.resolve(getLookupTextField());

      int idx = info.indexOfValue(realLookupTextField);
      if (idx < 0) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR,
            BaseMessages.getString(PKG,
                "SemanticSearchMeta.CheckResult.FieldNotFoundInLookupStream", realLookupTextField),
            transformMeta));
      } else {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_OK,
            BaseMessages.getString(PKG,
                "SemanticSearchMeta.CheckResult.FieldFoundInTheLookupStream", realLookupTextField),
            transformMeta));
      }

      // Check the fields from the lookup stream!
      String realLookupKeyField = variables.resolve(getLookupKeyField());

      idx = info.indexOfValue(realLookupKeyField);
      if (idx < 0) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR,
            BaseMessages.getString(PKG,
                "SemanticSearchMeta.CheckResult.FieldNotFoundInLookupStream", realLookupKeyField),
            transformMeta));
      } else {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_OK,
            BaseMessages.getString(PKG,
                "SemanticSearchMeta.CheckResult.FieldFoundInTheLookupStream", realLookupKeyField),
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
        errorMessage.insert(0,
            BaseMessages.getString(PKG,
                "SemanticSearchMeta.CheckResult.FieldsNotFoundInLookupStream2") + Const.CR
                + Const.CR);

        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, errorMessage.toString(),
            transformMeta));
      } else {
        remarks
            .add(new CheckResult(ICheckResult.TYPE_RESULT_OK,
                BaseMessages.getString(PKG,
                    "SemanticSearchMeta.CheckResult.AllFieldsFoundInTheLookupStream2"),
                transformMeta));
      }
    } else {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG,
          "SemanticSearchMeta.CheckResult.FieldsNotFoundFromInLookupSep"), transformMeta));
    }

    // See if the source transform is filled in!
    IStream infoStream = getTransformIOMeta().getInfoStreams().get(0);
    if (infoStream.getTransformMeta() == null) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR,
          BaseMessages.getString(PKG, "SemanticSearchMeta.CheckResult.SourceTransformNotSelected"),
          transformMeta));
    } else {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_OK,
          BaseMessages.getString(PKG, "SemanticSearchMeta.CheckResult.SourceTransformIsSelected"),
          transformMeta));

      // See if the transform exists!
      //
      if (info != null) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_OK,
            BaseMessages.getString(PKG, "SemanticSearchMeta.CheckResult.SourceTransformExist",
                infoStream.getTransformName() + ""),
            transformMeta));
      } else {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR,
            BaseMessages.getString(PKG,
                "SemanticSearchMeta.CheckResult.SourceTransformDoesNotExist",
                infoStream.getTransformName() + ""),
            transformMeta));
      }
    }

    // See if we have input streams leading to this transform!
    if (input.length >= 2) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_OK,
          BaseMessages.getString(PKG,
              "SemanticSearchMeta.CheckResult.TransformReceivingInfoFromInputTransforms",
              input.length + ""),
          transformMeta));
    } else {
      remarks.add(new CheckResult(
          ICheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG,
              "SemanticSearchMeta.CheckResult.NeedAtLeast2InputStreams", Const.CR, Const.CR),
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

  /**
   * Returns the Input/Output metadata for this transform. The generator transform
   * only produces
   * output, does not accept input!
   */
  @Override
  public ITransformIOMeta getTransformIOMeta() {
    ITransformIOMeta ioMeta = super.getTransformIOMeta(false);
    if (ioMeta == null) {

      ioMeta = new TransformIOMeta(true, true, false, false, false, false);

      IStream stream = new Stream(StreamType.INFO, null,
          BaseMessages.getString(PKG, "SemanticSearchMeta.InfoStream.Description"), StreamIcon.INFO,
          lookupTransformName);
      ioMeta.addStream(stream);
      setTransformIOMeta(ioMeta);
    }

    return ioMeta;
  }

  public static final class SLookupValue {
    @HopMetadataProperty(key = "name")
    private String name;

    @HopMetadataProperty(key = "rename")
    private String rename;

    public SLookupValue() {
    }

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
