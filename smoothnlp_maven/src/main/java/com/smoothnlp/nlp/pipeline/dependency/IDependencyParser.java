package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.basic.SToken;

import java.util.List;

public interface IDependencyParser {

    /**
     * For Now, the dependency interface are allowed to throw error, this may be updated later
     * @param input
     * @return
     * @throws Error
     */

    public DependencyRelationship[] parse(String input) throws Exception;

    public DependencyRelationship[] parse(List<SToken> stokens) throws Exception;

}
