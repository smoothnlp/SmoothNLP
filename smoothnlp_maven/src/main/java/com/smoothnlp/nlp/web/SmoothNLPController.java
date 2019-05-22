package com.smoothnlp.nlp.web;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SmoothNLPResult;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@Controller
public class SmoothNLPController{

    @GetMapping("/")
    @ResponseBody
    public SmoothNLPResult handle(@RequestParam(name="text", required=false, defaultValue="我买了十斤水果") String value){
        SmoothNLPResult result;
        try {
            result = SmoothNLP.process(value);
            return result;
        } catch (Exception e) {
            System.out.println(e);
            return new SmoothNLPResult(e.toString());
        }
    }

}
