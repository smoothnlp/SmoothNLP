package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SEntity;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;
import org.hibernate.validator.spi.scripting.ScriptEvaluatorNotFoundException;
import sun.nio.ch.sctp.SendFailed;

import java.util.*;

public class MultiNersPipeline extends BaseEntityRecognizer {

    private BaseEntityRecognizer[] nerPipelines;

    public MultiNersPipeline(BaseEntityRecognizer[]  nerPipelines){
        this.nerPipelines = nerPipelines;
    }

    @Override
    public List<SEntity> process(List<SToken> sTokenList) {
        List<SEntity> entityList = new LinkedList<>();
        for (BaseEntityRecognizer ner: this.nerPipelines){
            entityList.addAll(ner.process(sTokenList));
        }
        entityList = deDupOverlapedEntity(entityList);
        return entityList;
    }

    @Override
    public List<SEntity> process(String inputText) {
        List<SEntity> entityList = new LinkedList<>();
        for (BaseEntityRecognizer ner: this.nerPipelines){
            entityList.addAll(ner.process(inputText));
        }
        entityList = deDupOverlapedEntity(entityList);
        return entityList;
    }

    public List<SEntity> deDupOverlapedEntity(List<SEntity> entities){
        List<SEntity> deDupledList = new LinkedList<>();
        PriorityQueue<SEntity> pqEntities = new PriorityQueue<>();
        for (SEntity entity : entities){
            pqEntities.add(entity);
        }
        List<int[]> trackedRanges = new LinkedList<>();
        while(!pqEntities.isEmpty()){
            SEntity en = pqEntities.poll();
            boolean entityOverlaped = false;
            for (int[] range: trackedRanges){
                if (en.charStart>=range[0] & en.charEnd<=range[1]){
                    entityOverlaped = true;
                    break;
                }
            }
            if (!entityOverlaped){
                int[] newRange ={en.charStart,en.charEnd};
                trackedRanges.add(newRange);
                deDupledList.add(en);
            }
        }


        return deDupledList;

    }

    public static void main(String[] args){
        MultiNersPipeline mner = new MultiNersPipeline(new BaseEntityRecognizer[]{SmoothNLP.NORMALIZED_NER,SmoothNLP.REGEX_NER});
        System.out.println(UtilFns.toJson(mner.process("123万")));
    }

}
