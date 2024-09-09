package org.apache.hop.langchain4j.models;

public interface IModel extends Cloneable {
    public String getName();

    public IModel clone();
}
