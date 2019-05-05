package com.smoothnlp.nlp.basic;

import java.util.List;

public abstract class SDictionarySupported {
    protected List<String> libraryNames;
    public void setActiveDictionaries(List<String> libraryNames){
        this.libraryNames = libraryNames;
    };

}
