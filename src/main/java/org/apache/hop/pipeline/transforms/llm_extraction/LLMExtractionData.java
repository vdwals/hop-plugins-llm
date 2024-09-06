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

package org.apache.hop.pipeline.transforms.llm_extraction;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.HashMap;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.stream.IStream;

public class LLMExtractionData extends BaseTransformData {
  public IRowMeta previousRowMeta;
  public IRowMeta outputRowMeta;

  /** used to store values in used to look up things */
  public HashMap<String, Object[]> look;

  public EmbeddingStore<TextSegment> embeddingStore;

  public EmbeddingModel embeddingModel;

  public boolean readLookupValues;
  
  public int maxResults;

  /** index of main stream field */
  public int indexOfMainField;

  public IRowMeta infoMeta;

  public IStream infoStream;

  public boolean addAdditionalFields;

  /** index of return fields from lookup stream */
  public int[] indexOfCachedFields;

  public IRowMeta infoCache;

  public boolean returnMatchValue;
  public boolean returnKeyValue;
  public boolean returnDistanceValue;

  public LLMExtractionData() {
    super();
    this.look = new HashMap<>();
    this.indexOfMainField = -1;
    this.addAdditionalFields = false;
    this.maxResults = 1;
  }
}