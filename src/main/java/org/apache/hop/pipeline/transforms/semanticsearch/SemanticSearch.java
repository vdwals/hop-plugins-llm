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
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.PoolingMode;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.io.IOException;
import java.util.List;
import org.apache.hop.core.IRowSet;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.exception.HopValueException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transforms.semanticsearch.SemanticSearchMeta.SLookupValue;

/**
 * Performs a fuzzy match for each main stream field row An approximative match is done in a lookup
 * stream
 */
public class SemanticSearch extends BaseTransform<SemanticSearchMeta, SemanticSearchData> {
  private static final Class<?> PKG = SemanticSearchMeta.class; // For Translator

  public static void main(String[] args) throws IOException {

    String text =
        "Let's demonstrate that embedding can be done within a Java process and entirely offline.";

    // requires "langchain4j-embeddings-all-minilm-l6-v2" Maven/Gradle dependency, see pom.xml
    EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    Embedding inProcessEmbedding = embeddingModel.embed(text).content();
    System.out.println(inProcessEmbedding);
  }

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

    while (rowData != null) {
      if (firstRun) {
        data.infoMeta = rowSet.getRowMeta().clone();
        // Check lookup field
        int indexOfLookupField = data.infoMeta.indexOfValue(resolve(meta.getLookupField()));
        if (indexOfLookupField < 0) {
          // The field is unreachable !
          throw new HopException(BaseMessages.getString(PKG,
              "SemanticSearch.Exception.CouldnotFindLookField", meta.getLookupField()));
        }
        data.infoCache = new RowMeta();
        IValueMeta keyValueMeta = data.infoMeta.getValueMeta(indexOfLookupField);
        keyValueMeta.setStorageType(IValueMeta.STORAGE_TYPE_NORMAL);
        data.infoCache.addValueMeta(keyValueMeta);
        // Add key
        data.indexOfCachedFields[0] = indexOfLookupField;

        // Check additional fields
        if (data.addAdditionalFields) {
          IValueMeta additionalFieldValueMeta;
          for (int i = 0; i < meta.getLookupValues().size(); i++) {
            SLookupValue lookupValue = meta.getLookupValues().get(i);
            int fi = i + 1;
            data.indexOfCachedFields[fi] = data.infoMeta.indexOfValue(lookupValue.getName());
            if (data.indexOfCachedFields[fi] < 0) {
              // The field is unreachable !
              throw new HopException(BaseMessages.getString(PKG,
                  "FuzzyMatch.Exception.CouldnotFindLookField", lookupValue.getName()));
            }
            additionalFieldValueMeta = data.infoMeta.getValueMeta(data.indexOfCachedFields[fi]);
            additionalFieldValueMeta.setStorageType(IValueMeta.STORAGE_TYPE_NORMAL);
            data.infoCache.addValueMeta(additionalFieldValueMeta);
          }
          data.nrCachedFields += meta.getLookupValues().size();
        }

        firstRun = false;
      }

      if (log.isRowLevel()) {
        logRowlevel(BaseMessages.getString(PKG, "SemanticSearch.Log.ReadLookupRow")
            + rowSet.getRowMeta().getString(rowData));
      }

      // Look up the keys in the source rows
      // and store values in cache

      Object[] storeData = new Object[data.nrCachedFields];
      String textValue = "";
      // Add key field
      if (rowData[data.indexOfCachedFields[0]] != null) {
        IValueMeta fromStreamRowMeta =
            rowSet.getRowMeta().getValueMeta(data.indexOfCachedFields[0]);
        textValue = fromStreamRowMeta.getString(rowData[data.indexOfCachedFields[0]]);
      }
      storeData[0] = textValue;

      // Add additional fields?
      for (int i = 1; i < data.nrCachedFields; i++) {
        IValueMeta fromStreamRowMeta =
            rowSet.getRowMeta().getValueMeta(data.indexOfCachedFields[i]);
        if (fromStreamRowMeta.isStorageBinaryString()) {
          storeData[i] =
              fromStreamRowMeta.convertToNormalStorageType(rowData[data.indexOfCachedFields[i]]);
        } else {
          storeData[i] = rowData[data.indexOfCachedFields[i]];
        }
      }

      if (isDebug()) {
        logDebug(BaseMessages.getString(PKG, "SemanticSearch.Log.AddingValueToCache",
            data.infoCache.getString(storeData)));
      }

      addToVector(textValue, rowData);

      rowData = getRowFrom(rowSet);
    }

    return true;
  }

  private Object[] lookupValues(IRowMeta rowMeta, Object[] row) throws HopException {
    if (first) {
      first = false;

      data.outputRowMeta = getInputRowMeta().clone();
      meta.getFields(data.outputRowMeta, getTransformName(), new IRowMeta[] {data.infoMeta}, null,
          this, metadataProvider);

      // Check lookup field
      data.indexOfMainField = getInputRowMeta().indexOfValue(resolve(meta.getMainStreamField()));
      if (data.indexOfMainField < 0) {
        // The field is unreachable !
        throw new HopException(BaseMessages.getString(PKG,
            "SemanticSearch.Exception.CouldnotFindMainField", meta.getMainStreamField()));
      }
    }
    Object[] add;
    if (row[data.indexOfMainField] == null) {
      add = RowDataUtil.allocateRowData(data.outputRowMeta.size());
    } else {
      try {
        add = getFromVector(row);
      } catch (Exception e) {
        throw new HopTransformException(e);
      }
    }
    return RowDataUtil.addRowData(row, rowMeta.size(), add);
  }

  private void addToVector(String textValue, Object[] lookupRowValues) throws HopException {
    try {
      TextSegment segment = TextSegment.from(textValue);
      Embedding embedding = data.embeddingModel.embed(segment).content();
      String embeddingId = data.embeddingStore.add(embedding);
      data.look.put(embeddingId, lookupRowValues);

    } catch (OutOfMemoryError o) {
      // exception out of memory
      throw new HopException(
          BaseMessages.getString(PKG, "SemanticSearch.Error.JavaHeap", o.toString()));
    }
  }

  private Object[] getFromVector(Object[] keyRow) throws HopValueException {
    if (isDebug()) {
      logDebug(BaseMessages.getString(PKG, "SemanticSearch.Log.ReadingMainStreamRow",
          getInputRowMeta().getString(keyRow)));
    }

    String lookupValueString = getInputRowMeta().getString(keyRow, data.indexOfMainField);

    Embedding queryEmbedding = data.embeddingModel.embed(lookupValueString).content();
    List<EmbeddingMatch<TextSegment>> relevant =
        data.embeddingStore.findRelevant(queryEmbedding, 1);

    EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);
    String embeddingId = embeddingMatch.embeddingId();

    Object[] matchedRow = data.look.get(embeddingId);

    Object[] retval = createRetval(keyRow, matchedRow);

    return retval;
  }

  private Object[] createRetval(Object[] keyRow, Object[] matchedRow) {
    // Reserve room
    Object[] rowData = RowDataUtil.allocateRowData(data.outputRowMeta.size());

    int index = 0;
    rowData[index++] = matchedRow[0];
    // Add additional return values?
    if (data.addAdditionalFields) {
      for (int i = 0; i < meta.getLookupValues().size(); i++) {
        int nr = i + 1;
        int nf = i + index;
        rowData[nf] = matchedRow[nr];
      }
    }
    return rowData;
  }

  @Override
  public boolean processRow() throws HopException {
    if (data.readLookupValues) {
      data.readLookupValues = false;

      // Read values from lookup transform (look)
      if (!readLookupValues()) {
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
      Object[] outputRow = lookupValues(getInputRowMeta(), r);
      if (outputRow == null) {
        setOutputDone(); // signal end to receiver(s)
        return false;
      }
      putRow(data.outputRowMeta, outputRow); // copy row to output rowset(s)

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
    if (Utils.isEmpty(meta.getLookupField())) {
      logError(BaseMessages.getString(PKG, "SemanticSearch.Error.LookupStreamFieldMissing"));
      return false;
    }

    // Checks output fields
    String matchField = resolve(meta.getOutputMatchField());
    if (Utils.isEmpty(matchField)) {
      logError(BaseMessages.getString(PKG, "SemanticSearch.Error.OutputMatchFieldMissing"));
      return false;
    }

    data.readLookupValues = true;

    switch (meta.getEmbeddingModel()) {
      case ONNX_MODEL:
        // Check lookup and main stream field
        if (Utils.isEmpty(meta.getOnnxFilename())) {
          logError(BaseMessages.getString(PKG, "SemanticSearch.Error.OnnxFileMissing"));
          return false;
        }
        if (Utils.isEmpty(meta.getTokenizerFilename())) {
          logError(BaseMessages.getString(PKG, "SemanticSearch.Error.TokenizerFileMissing"));
          return false;
        }
        String onnxFile = resolve(meta.getOnnxFilename());
        String tokenizerFile = resolve(meta.getTokenizerFilename());

        // data.embeddingModel = new OnnxEmbeddingModel(onnxFile, tokenizerFile, PoolingMode.MEAN);
        data.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        break;

      default:
        logError(BaseMessages.getString(PKG, "SemanticSearch.Error.EmbeddingModelInvalid"));
        return false;
    }

    switch (meta.getEmbeddingStore()) {
      case IN_MEMORY:
        data.embeddingStore = new InMemoryEmbeddingStore<TextSegment>();
        break;

      default:
        logError(BaseMessages.getString(PKG, "SemanticSearch.Error.EmbeddingStoreInvalid"));
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
