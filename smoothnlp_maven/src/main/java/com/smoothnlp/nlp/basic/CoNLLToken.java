package com.smoothnlp.nlp.basic;

public class CoNLLToken extends SToken{

    public int dependentIndex;
    public int selfIndex;
    public String relationship;  // this may updated to static enum later

    public static CoNLLToken ROOT = new CoNLLToken("ROOT","ROOT",0);

    public CoNLLToken(String token, String postag) {
        super(token,postag);
    }

    public CoNLLToken(String token, String postag, int selfIndex){
        super(token,postag);
        this.selfIndex= selfIndex;
    }

    public CoNLLToken(String token, String postag, int selfIndex , int targetIndex, String relationship){
        super(token,postag);
        this.selfIndex = selfIndex;
        this.dependentIndex = targetIndex;
        this.relationship = relationship;
    }

    public String toString(){
        if (this.relationship !=null){
            return UtilFns.join("\t",new String[]{String.valueOf(this.selfIndex), this.token, this.postag , String.valueOf(dependentIndex), this.relationship});
        }else{
            return UtilFns.join("\t",new String[]{String.valueOf(this.selfIndex), this.token, this.postag});
        }
    }

    public int getDependentIndex() {return this.dependentIndex;}

    public void setDependentIndex(int targetIndex){this.dependentIndex = targetIndex;}

    public int getSelfIndex(){return this.selfIndex;}

    public static CoNLLToken parseCoNLLLine(String line){
        /**
         * a static function to parse a line in conllu/x format into CoNLLToken
         * in conll standard, the description for column index:
         *      0: token index (start with 1)
         *      6: dependent token index
         *      7: dependent relationship name
         */
        String[] infos = line.split("\t");
        return new CoNLLToken(infos[1],infos[3], Integer.valueOf(infos[0]),Integer.valueOf(infos[6]),infos[7]);
    }


}
