package com.smoothnlp.nlp;

import com.smoothnlp.nlp.SmoothNLP;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SmoothNLPController{
    private static final String template = "%s";

    @GetMapping("/payload")
    @ResponseBody
    public Payload handle(@RequestParam(name="value", required=false, defaultValue="我买了十斤水果") String value){
        String result;
        try {
            result = SmoothNLP.process(value);
            return new Payload(String.format(template, result)) ;
        } catch (Exception e) {
            System.out.println(e);
        }

        return new Payload(String.format(template, "error occurs."));


    }

}
