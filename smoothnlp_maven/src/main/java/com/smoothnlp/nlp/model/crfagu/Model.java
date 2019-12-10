package com.smoothnlp.nlp.model.crfagu;

/**
 * Created by zhifac on 2017/4/3.
 */
public abstract class Model {

    public boolean open(String[] args) {
        return true;
    }

    public boolean open(String arg) {
        return true;
    }

    public boolean close() {
        return true;
    }

    public Tagger createTagger() {
        return null;
    }
}
