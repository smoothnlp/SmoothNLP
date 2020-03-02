package com.smoothnlp.nlp.web;

import java.util.UUID;
import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SmoothNLPResult;

import ml.dmlc.xgboost4j.java.XGBoostError;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@Controller
public class SmoothNLPController{

    @GetMapping("/")
    @ResponseBody
    public ResponseWrapper handle(@RequestParam(name="text", required=false, defaultValue="我买了十斤水果") String value) throws XGBoostError {
        SmoothNLPResult result;

        result = SmoothNLP.process(value);
        return new ResponseWrapper(result);

    }

    public class ResponseWrapper {

        public Payload payload ;
        public int status_code ;
        public String msg;

        public ResponseWrapper(Object resonse){
            this.status_code = 0;
            this.msg = "success";
            this.payload = new Payload(resonse);
        }

        public ResponseWrapper(Exception e){
            this.status_code = 1;
            this.msg = "error";
            this.payload = new Payload(e.toString());
        }

        public class Payload{
            public Object response;
            public UUID request_id;
            public Payload(Object response){
                this.response = response;
                this.request_id = UUID.randomUUID();
            }
        }

    }

}
