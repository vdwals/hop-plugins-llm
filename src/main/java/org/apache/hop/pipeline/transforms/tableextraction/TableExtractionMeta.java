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

package org.apache.hop.pipeline.transforms.tableextraction;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.CheckResult;
import org.apache.hop.core.Const;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.exception.HopPluginException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.injection.InjectionDeep;
import org.apache.hop.core.injection.InjectionSupported;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transform.stream.IStream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Transform(id = "TableExtraction", image = "TableExtraction.svg", name = "Extract to Table", description = "i18n::TableExtraction.Description", categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Transform", keywords = "i18n::TableExtraction.keyword")
@Getter
@Setter
@InjectionSupported(localizationPrefix = "TableExtraction.Injection.")
public class TableExtractionMeta extends BaseTransformMeta<TableExtraction, TableExtractionData> {
  private static final Class<?> PKG = TableExtractionMeta.class; // For Translator

  /** Embedding model type */
  @HopMetadataProperty(key = "llmodel", injectionKey = "llmodel", injectionKeyDescription = "TableExtraction.Injection.llmodel")
  private String llModelName;

  /** field in input stream which to extract data from */
  @HopMetadataProperty(key = "textfield")
  private String textField;

  /** return these field values from lookup */
  @InjectionDeep
  private List<TargetColumn> targetColumns;

  public TableExtractionMeta() {
    super();
    this.targetColumns = new ArrayList<>();
  }

  public TableExtractionMeta(TableExtractionMeta m) {
    this();
    this.setLlModelName(m.getLlModelName());
    this.textField = m.textField;
    m.targetColumns.forEach(v -> this.targetColumns.add(new TargetColumn(v)));
  }

  @Override
  public TableExtractionMeta clone() {
    return new TableExtractionMeta(this);
  }

  @Override
  public void setDefault() {
    setLlModelName(null);
    textField = null;
    targetColumns.clear();
  }

  @Override
  public void getFields(IRowMeta inputRowMeta, String name, IRowMeta[] info,
      TransformMeta nextTransform, IVariables variables, IHopMetadataProvider metadataProvider)
      throws HopTransformException {

    for (TargetColumn targetColumn : targetColumns) {
      IValueMeta v;
      try {
        v = ValueMetaFactory.createValueMeta(targetColumn.getName(), targetColumn.getType());
        v.setOrigin(this.getName());
        inputRowMeta.addValueMeta(v);
      } catch (HopPluginException e) {
        e.printStackTrace();
      }
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
              "TableExtraction.CheckResult.TransformReceivingFields", prev.size() + ""),
          transformMeta);
      remarks.add(cr);

      // Starting from selected fields in ...
      // Check the fields from the previous stream!
      String mainField = variables.resolve(getTextField());
      int idx = prev.indexOfValue(mainField);
      if (idx < 0) {
        cr = new CheckResult(ICheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG,
            "TableExtraction.CheckResult.MainFieldNotFound", mainField), transformMeta);
      } else {
        cr = new CheckResult(ICheckResult.TYPE_RESULT_OK,
            BaseMessages.getString(PKG, "TableExtraction.CheckResult.MainFieldFound", mainField),
            transformMeta);
      }
      remarks.add(cr);

    } else {
      cr = new CheckResult(ICheckResult.TYPE_RESULT_ERROR,
          BaseMessages.getString(PKG,
              "TableExtraction.CheckResult.CouldNotFindFieldsFromPreviousTransforms"),
          transformMeta);
      remarks.add(cr);
    }

    if (info != null && info.size() > 0) {
      remarks.add(new CheckResult(
          ICheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG,
              "TableExtraction.CheckResult.TransformReceivingLookupData", info.size() + ""),
          transformMeta));

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
    if (input.length >= 1) {
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

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static final class TargetColumn {
    @HopMetadataProperty(key = "name")
    private String name;

    @HopMetadataProperty(key = "type")
    private int type;

    public TargetColumn(TargetColumn v) {
      this.name = v.name;
      this.type = v.type;
    }

  }
}
