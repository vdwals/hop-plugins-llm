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

import java.util.List;
import java.util.Collections;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.util.Utils;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.languagemodels.LanguageModelMeta;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;

/**
 * Performs a fuzzy match for each main stream field row An approximative match
 * is done in a lookup
 * stream
 */
public class TableExtraction extends BaseTransform<TableExtractionMeta, TableExtractionData> {
  private static final Class<?> PKG = TableExtractionMeta.class; // For Translator

  public TableExtraction(TransformMeta transformMeta, TableExtractionMeta meta,
      TableExtractionData data, int copyNr, PipelineMeta pipelineMeta, Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  public boolean processRow() throws HopException {

    Object[] r = getRow(); // Get row from input rowset & set row busy!
    if (r == null) {
      // no more input to be expected...
      if (isDetailed()) {
        logDetailed(BaseMessages.getString(PKG, "SemanticSearch.Log.StoppedProcessingWithEmpty",
            getLinesRead()));
      }
      setOutputDone();
      return false;
    }

    try {

      // Do the actual lookup in the hastable.
      List<Object[]> outputRow = Collections.emptyList();
      if (outputRow == null || outputRow.isEmpty() || outputRow.get(0) == null) {
        setOutputDone(); // signal end to receiver(s)
        return false;
      }
      for (Object[] output : outputRow) {
        putRow(data.outputRowMeta, output); // copy row to output rowset(s)
      }

      if (checkFeedback(getLinesRead()) && log.isBasic()) {
        logBasic(BaseMessages.getString(PKG, "SemanticSearch.Log.LineNumber") + getLinesRead());
      }
    } catch (HopException e) {
      if (getTransformMeta().isDoingErrorHandling()) {
        // Send this row to the error handling transform
        putError(getInputRowMeta(), r, 1, e.toString(), meta.getTextField(),
            "SemanticSearch001");
      } else {
        logError(BaseMessages.getString(PKG, "SemanticSearch.Log.ErrorInTransformRunning")
            + e.getMessage());
        setErrors(1);
        stopAll();
        setOutputDone(); // signal end to receiver(s)
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean init() {
    if (!super.init()) {
      return false;
    }

    // Check lookup and main stream field
    if (Utils.isEmpty(meta.getTextField())) {
      logError(BaseMessages.getString(PKG, "SemanticSearch.Error.LookupStreamFieldMissing"));
      return false;
    }

    if (Utils.isEmpty(meta.getLanguageModelName())) {
      logError(BaseMessages.getString(PKG, "SemanticSearch.Error.llModelMissing"));
      return false;
    }
    try {
      LanguageModelMeta modelMeta = metadataProvider.getSerializer(LanguageModelMeta.class)
          .load(resolve(meta.getLanguageModelName()));

      data.chatModel = modelMeta.getChatModel(metadataProvider, log, variables);
    } catch (Exception e) {
      log.logError("Could not get LL-Model '"
          + resolve(meta.getLanguageModelName()) + "' from the metastore", e);
      return false;
    }

    return true;
  }

}
