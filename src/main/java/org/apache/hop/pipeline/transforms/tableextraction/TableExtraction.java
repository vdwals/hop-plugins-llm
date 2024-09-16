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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.exception.HopValueException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.util.Utils;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.languagemodels.LanguageModelMeta;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;

/**
 * Performs a fuzzy match for each main stream field row An approximative match
 * is done in a lookup
 * stream
 */
public class TableExtraction extends BaseTransform<TableExtractionMeta, TableExtractionData> {
  private static final Class<?> PKG = TableExtractionMeta.class; // For Translator
  private static final String SYSTEM_MESSAGE = "Extract data from the user message.\n" +
      "If one field has multiple matches, list only one match per JSON and return multiple JSONs as array, one for each match.\n"
      + "Return only the result, no explanaitions.\n"
      +
      "The JSON structure for extraction is: [{%s}]";

  private static final String JSON_DELIMITER = ",";
  private static final String JSON_STRUCTURE = "\"%s\":\"%s as datatype %s\"";
  private static final String JSON_STRUCTURE_WITH_FORMAT = "\"%s\":\"%s as datatype %s in format %s\"";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public TableExtraction(TransformMeta transformMeta, TableExtractionMeta meta,
      TableExtractionData data, int copyNr, PipelineMeta pipelineMeta, Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  public boolean processRow() throws HopException {
    if (data.outputRowMeta == null) {
      IRowMeta rowMeta = getInputRowSets().get(0).getRowMeta();

      data.indexOfTextField = rowMeta.indexOfValue(resolve(meta.getTextField()));
      data.outputRowMeta = rowMeta.clone();
      data.indexOfReturnFields = rowMeta.size();
      data.numberOfReturnFields = meta.getTargetColumns().size();
      try {
        meta.getFields(data.outputRowMeta, getTransformName(), new IRowMeta[] { rowMeta }, null,
            this, metadataProvider);
      } catch (HopTransformException e) {
        e.printStackTrace();
      }
    }

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

      String textToAnalyse = getInputRowMeta().getString(r, data.indexOfTextField);
      // Do the actual lookup in the hastable.
      List<Object[]> outputRow = getExtractedValues(textToAnalyse);
      if (outputRow == null || outputRow.isEmpty() || outputRow.get(0) == null) {
        setOutputDone(); // signal end to receiver(s)
        return false;
      }
      for (Object[] output : outputRow) {
        // Create a new array to hold the combined elements of r and output
        Object[] combinedRow = new Object[r.length + output.length];

        // Copy the elements of r and output into the new array
        System.arraycopy(r, 0, combinedRow, 0, data.indexOfReturnFields);
        System.arraycopy(output, 0, combinedRow, data.indexOfReturnFields, output.length);

        // Pass the combined row to the putRow function
        putRow(data.outputRowMeta, combinedRow); // copy row to output rowset(s)
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

  private List<Object[]> getExtractedValues(String textToAnalyse) {
    UserMessage um = new UserMessage(textToAnalyse);

    Response<AiMessage> response = data.chatModel.generate(data.systemMessage, um);

    List<Map<String, String>> values = Collections.emptyList();
    try {
      values = objectMapper.readValue(response.content().text(),
          new TypeReference<List<Map<String, String>>>() {
          });
    } catch (JsonProcessingException e) {
      logError("Could not parse the answer:"
          + response, e);
    }

    String[] outputRowFieldNames = data.outputRowMeta.getFieldNames();

    return values.stream().map(map -> {
      Object[] extracts = new Object[data.numberOfReturnFields];
      for (int insertIndex = 0; insertIndex < data.numberOfReturnFields; insertIndex++) {
        int outputRowIndex = data.indexOfReturnFields + insertIndex;
        String value = map.get(outputRowFieldNames[outputRowIndex]);
        IValueMeta valueMeta = data.outputRowMeta.getValueMeta(outputRowIndex);
        try {
          extracts[insertIndex] = valueMeta.convertData(valueMeta, value);
        } catch (HopValueException e) {
          e.printStackTrace();
        }
      }
      return extracts;
    }).collect(Collectors.toList());
  }

  @Override
  public boolean init() {
    if (!super.init()) {
      return false;
    }

    // TODO Without input step could be used to generate data instead.

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
    if (meta.getTargetColumns().isEmpty()) {
      log.logError("Target columns missing");
      return false;
    } else {

      String jsonContent = meta.getTargetColumns().stream().map(tc -> {
        String format = JSON_STRUCTURE;
        if (tc.getFormat() != null) {
          format = JSON_STRUCTURE_WITH_FORMAT;
        }
        String type = ValueMetaFactory.getValueMetaName(tc.getType());
        return String.format(format, tc.getName(), tc.getDescription(), type, tc.getFormat());
      }).collect(Collectors.joining(JSON_DELIMITER));

      data.systemMessage = new SystemMessage(String.format(SYSTEM_MESSAGE, jsonContent));
    }

    return true;
  }

}
