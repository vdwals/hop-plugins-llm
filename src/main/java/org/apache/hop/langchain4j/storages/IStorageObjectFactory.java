package org.apache.hop.langchain4j.storages;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.langchain4j.storages.inmemory.InMemoryStorageMeta;
import org.apache.hop.langchain4j.storages.neo4j.Neo4jStorageMeta;
import org.apache.hop.metadata.api.IHopMetadataObjectFactory;

public class IStorageObjectFactory implements IHopMetadataObjectFactory {

    @Override
    public Object createObject(String id, Object parentObject) throws HopException {
        if (id.equals(Neo4jStorageMeta.NAME))
            return new Neo4jStorageMeta();
        else if (id.equals(InMemoryStorageMeta.NAME))
            return new InMemoryStorageMeta();
        return null;
    }

    @Override
    public String getObjectId(Object object) throws HopException {
        if (object instanceof IStorage)
            return ((IStorage) object).getName();
        return null;
    }

}