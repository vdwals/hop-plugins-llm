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

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.hop.core.Const;
import org.apache.hop.core.IRowSet;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopValueException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.LlmMeta;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transforms.semanticsearch.SemanticSearchMeta.SLookupValue;

/**
 * Performs a fuzzy match for each main stream field row An approximative match
 * is done in a lookup
 * stream
 */
public class SemanticSearch extends BaseTransform<SemanticSearchMeta, SemanticSearchData> {
  private static final Class<?> PKG = SemanticSearchMeta.class; // For Translator

  public SemanticSearch(TransformMeta transformMeta, SemanticSearchMeta meta,
      SemanticSearchData data, int copyNr, PipelineMeta pipelineMeta, Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  /**
   * Reads values from lookup and prepares semantic vector search
   *
   * @return successfull read of lookup values
   * @throws HopException
   */
  private boolean readLookupValues() throws HopException {
    data.infoStream = meta.getTransformIOMeta().getInfoStreams().get(0);
    if (data.infoStream.getTransformMeta() == null) {
      logError(BaseMessages.getString(PKG, "SemanticSearch.Log.NoLookupTransformSpecified"));
      return false;
    }

    if (isDetailed()) {
      logDetailed(BaseMessages.getString(PKG, "SemanticSearch.Log.ReadingFromStream")
          + data.infoStream.getTransformName() + "]");
    }

    boolean firstRun = true;
    // Which row set do we read from?
    IRowSet rowSet = findInputRowSet(data.infoStream.getTransformName());
    Object[] rowData = getRowFrom(rowSet); // rows are originating from "lookup_from"

    int indexOfLookupTextField = -1;
    int indexOfLookupKeyField = -1;

    while (rowData != null) {
      if (firstRun) {
        data.infoMeta = rowSet.getRowMeta().clone();
        // Check lookup field
        indexOfLookupTextField = data.infoMeta.indexOfValue(resolve(meta.getLookupTextField()));
        if (indexOfLookupTextField < 0) {
          // The field is unreachable !
          throw new HopException(BaseMessages.getString(PKG,
              "SemanticSearch.Exception.CouldnotFindLookField", meta.getLookupTextField()));
        }
        indexOfLookupKeyField = data.infoMeta.indexOfValue(resolve(meta.getLookupKeyField()));
        if (indexOfLookupKeyField < 0) {
          // The field is unreachable !
          throw new HopException(BaseMessages.getString(PKG,
              "SemanticSearch.Exception.CouldnotFindLookField", meta.getLookupKeyField()));
        }
        if (data.returnMatchValue) {
          // Text value needs to be cached separately
          data.infoCache = new RowMeta();
          IValueMeta textValueMeta = data.infoMeta.getValueMeta(indexOfLookupTextField);
          textValueMeta.setStorageType(IValueMeta.STORAGE_TYPE_NORMAL);
          data.infoCache.addValueMeta(textValueMeta);
        }

        if (data.returnKeyValue) {
          // Keyvalue does not need to be cached separately
          IValueMeta keyValueMeta = data.infoMeta.getValueMeta(indexOfLookupKeyField);
          keyValueMeta.setStorageType(IValueMeta.STORAGE_TYPE_NORMAL);
          data.infoCache.addValueMeta(keyValueMeta);
        }

        // Check additional fields
        if (data.addAdditionalFields) {
          for (int i = 0; i < meta.getLookupValues().size(); i++) {
            SLookupValue lookupValue = meta.getLookupValues().get(i);
            int additionalFieldIndex = data.infoMeta
                .indexOfValue(lookupValue.getName());
            if (additionalFieldIndex < 0) {
              // The field is unreachable !
              throw new HopException(BaseMessages.getString(PKG,
                  "SemanticSearch.Exception.CouldnotFindLookField", lookupValue.getName()));
            }
            int cachedFieldIndex = i + SemanticSearchData.ADDITIONAL_VALUES_START_INDEX;
            data.indexOfCachedFields[cachedFieldIndex] = additionalFieldIndex;

            IValueMeta additionalFieldValueMeta = data.infoMeta.getValueMeta(additionalFieldIndex);
            additionalFieldValueMeta.setStorageType(IValueMeta.STORAGE_TYPE_NORMAL);
            data.infoCache.addValueMeta(additionalFieldValueMeta);
          }
        }

        firstRun = false;
      }

      if (log.isRowLevel()) {
        logRowlevel(BaseMessages.getString(PKG, "SemanticSearch.Log.ReadLookupRow")
            + rowSet.getRowMeta().getString(rowData));
      }

      // Look up the keys in the source rows
      // and store values in cache

      // Store textfield
      String textValue = storeFieldValue(rowSet, rowData, indexOfLookupTextField);
      // Store keyfield
      Object keyValue = rowData[indexOfLookupKeyField];

      Object[] storeData = new Object[data.indexOfCachedFields.length];
      storeData[SemanticSearchData.VALUE_INDEX] = textValue;
      storeData[SemanticSearchData.KEY_INDEX] = keyValue;

      // Add additional fields?
      for (int i = SemanticSearchData.ADDITIONAL_VALUES_START_INDEX; i < data.indexOfCachedFields.length; i++) {
        IValueMeta fromStreamRowMeta = rowSet.getRowMeta().getValueMeta(data.indexOfCachedFields[i]);
        if (fromStreamRowMeta.isStorageBinaryString()) {
          storeData[i] = fromStreamRowMeta.convertToNormalStorageType(rowData[data.indexOfCachedFields[i]]);
        } else {
          storeData[i] = rowData[data.indexOfCachedFields[i]];
        }
      }

      if (isDebug()) {
        logDebug(BaseMessages.getString(PKG, "SemanticSearch.Log.AddingValueToCache",
            data.infoCache.getString(storeData)));
      }

      addToVector(textValue, keyValue, storeData);

      rowData = getRowFrom(rowSet);
    }

    return true;
  }

  private String storeFieldValue(IRowSet rowSet, Object[] rowData, int extractionIndex)
      throws HopValueException {
    String textValue = "";
    // Add text field
    if (rowData[extractionIndex] != null) {
      IValueMeta fromStreamRowMeta = rowSet.getRowMeta().getValueMeta(extractionIndex);
      textValue = fromStreamRowMeta.getString(rowData[extractionIndex]);
    }

    return textValue;
  }

  private List<Object[]> lookupValues(IRowMeta rowMeta, Object[] row) throws HopException {
    if (first) {
      first = false;

      data.outputRowMeta = getInputRowMeta().clone();
      meta.getFields(data.outputRowMeta, getTransformName(), new IRowMeta[] { data.infoMeta }, null,
          this, metadataProvider);

      // Check lookup field
      data.indexOfMainField = getInputRowMeta().indexOfValue(resolve(meta.getMainStreamField()));
      if (data.indexOfMainField < 0) {
        // The field is unreachable !
        throw new HopException(BaseMessages.getString(PKG,
            "SemanticSearch.Exception.CouldnotFindMainField", meta.getMainStreamField()));
      }
    }

    if (row[data.indexOfMainField] == null) {
      ArrayList<Object[]> arrayList = new ArrayList<Object[]>(1);
      arrayList.add(RowDataUtil.addRowData(row, rowMeta.size(),
          RowDataUtil.allocateRowData(data.outputRowMeta.size())));
      return arrayList;
    }

    return getFromVector(row).stream()
        .map(retval -> RowDataUtil.addRowData(row, rowMeta.size(), retval))
        .collect(Collectors.toList());
  }

  private void addToVector(String textValue, Object keyValue, Object[] lookupRowValues)
      throws HopException {
    try {
      TextSegment segment = TextSegment.from(textValue);
      Embedding embedding = data.embeddingModel.embed(segment).content();
      String keyString = String.valueOf(keyValue);
      data.embeddingStore.add(keyString, embedding);

      data.look.put(keyString, lookupRowValues);

    } catch (OutOfMemoryError o) {
      // exception out of memory
      throw new HopException(
          BaseMessages.getString(PKG, "SemanticSearch.Error.JavaHeap", o.toString()));
    }
  }

  private List<Object[]> getFromVector(Object[] keyRow) throws HopValueException {
    if (isDebug()) {
      logDebug(BaseMessages.getString(PKG, "SemanticSearch.Log.ReadingMainStreamRow",
          getInputRowMeta().getString(keyRow)));
    }

    String lookupValueString = getInputRowMeta().getString(keyRow, data.indexOfMainField);

    Embedding queryEmbedding = data.embeddingModel.embed(lookupValueString).content();
    EmbeddingSearchRequest esr = new EmbeddingSearchRequest(queryEmbedding, data.maxResults, null, null);
    EmbeddingSearchResult<TextSegment> relevant = data.embeddingStore.search(esr);

    List<Object[]> retList = new ArrayList<Object[]>(data.maxResults);

    for (EmbeddingMatch<TextSegment> embeddingMatch : relevant.matches()) {
      String key = embeddingMatch.embeddingId();
      Double score = embeddingMatch.score();

      Object[] matchedRow = data.look.get(key);
      Object resultKey = matchedRow[SemanticSearchData.KEY_INDEX];
      String value = (String) matchedRow[SemanticSearchData.VALUE_INDEX];

      // Reserve room
      Object[] retval = RowDataUtil.allocateRowData(data.outputRowMeta.size());

      int insertIndex = 0;
      if (data.returnMatchValue)
        retval[insertIndex++] = value;

      if (data.returnKeyValue)
        retval[insertIndex++] = resultKey;

      if (data.returnDistanceValue)
        retval[insertIndex++] = score;

      // Add additional return values?
      if (data.addAdditionalFields) {
        for (int i = 0; i < meta.getLookupValues().size(); i++) {
          int nf = i + insertIndex;
          retval[nf] = matchedRow[i + SemanticSearchData.ADDITIONAL_VALUES_START_INDEX];
        }
      }

      retList.add(retval);
    }

    return retList;
  }

  @Override
  public boolean processRow() throws HopException {
    if (data.readLookupValues) {
      data.readLookupValues = false;

      // Read values from lookup transform (look)
      boolean lookupValuesSucceded = readLookupValues();
      if (!lookupValuesSucceded) {
        logError(
            BaseMessages.getString(PKG, "SemanticSearch.Log.UnableToReadDataFromLookupStream"));
        setErrors(1);
        stopAll();
        return false;
      }
      if (isDetailed()) {
        logDetailed(
            BaseMessages.getString(PKG, "SemanticSearch.Log.ReadValuesInMemory", data.look.size()));
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

      // Do the actual lookup in the hastable.
      List<Object[]> outputRow = lookupValues(getInputRowMeta(), r);
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
        putError(getInputRowMeta(), r, 1, e.toString(), meta.getMainStreamField(),
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
    if (Utils.isEmpty(meta.getMainStreamField())) {
      logError(BaseMessages.getString(PKG, "SemanticSearch.Error.MainStreamFieldMissing"));
      return false;
    }
    if (Utils.isEmpty(meta.getLookupTextField())) {
      logError(BaseMessages.getString(PKG, "SemanticSearch.Error.LookupStreamFieldMissing"));
      return false;
    }
    if (Utils.isEmpty(meta.getLookupKeyField())) {
      logError(BaseMessages.getString(PKG, "SemanticSearch.Error.LookupStreamKeyFieldMissing"));
      return false;
    }

    data.returnMatchValue = !Utils.isEmpty(resolve(meta.getOutputMatchField()));
    data.returnKeyValue = !Utils.isEmpty(resolve(meta.getOutputKeyField()));
    data.returnDistanceValue = !Utils.isEmpty(resolve(meta.getOutputDistanceField()));

    // Set the number of fields to cache default value is one
    int nrFields = SemanticSearchData.ADDITIONAL_VALUES_START_INDEX;

    if (!meta.getLookupValues().isEmpty()) {
      // cache also additional fields
      data.addAdditionalFields = true;
      nrFields += meta.getLookupValues().size();
    }
    data.indexOfCachedFields = new int[nrFields];

    data.readLookupValues = true;

    data.maxResults = Const.toInt(resolve(meta.getMaximalValue()), 1);

    if (Utils.isEmpty(meta.getLlModelName())) {
      logError(BaseMessages.getString(PKG, "SemanticSearch.Error.llModelMissing"));
      return false;
    }
    try {
      LlmMeta modelMeta = metadataProvider.getSerializer(LlmMeta.class).load(resolve(meta.getLlModelName()));

      data.embeddingModel = modelMeta.getEmbeddingModel(metadataProvider, log, this);
      data.embeddingStore = modelMeta.getEmbeddingStorage(metadataProvider, log, this);
    } catch (Exception e) {
      log.logError("Could not get LL-Model '"
          + resolve(meta.getLlModelName()) + "' from the metastore", e);
      return false;
    }

    
    return true;
  }

  @Override
  public void dispose() {
    data.look.clear();
    super.dispose();
  }
}
