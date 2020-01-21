import requests
import time
from smoothnlp import HOST_URL

class smoothNlpRequest(object):
    def __init__(self):
        self.url = HOST_URL

    def set_url(self,url):
        self.url = url

    def __call__(self,text,path = "/query",counter=0,max_size_limit = 200):
        if len(text)>max_size_limit:
            raise ValueError("text with size over 200 is prohibited. you may use smoothnlp.nlp.split2sentences as preprocessing")
        if counter > 10:
            raise Exception(
                " exceed maximal attemps for parsing. ")
        content = {"text":text}
        r = requests.get(self.url+path, params=content)
        try:
            self.result = r.json()
            if isinstance(self.result,dict):
                self.result = self.result['payload']['response']
            return self.result
        except KeyError:
            counter+=1
            time.sleep(0.05) ## 延迟50毫秒再调用
            return self.__call__(text,path = path, counter = counter, max_size_limit=max_size_limit)

    def dependencyrelationships(self,text):
        self.__call__(text)
        return self.result['dependencyRelationships']

    def ner(self,text):
        self.__call__(text)
        return self.result['entities']

    def number_recognize(self,text):
        entities = self.ner(text)
        if entities is None :
            return
        numbers = []
        for entity in entities:
            if entity['nerTag'].lower() == "number":
                numbers.append(entity)
        return numbers

    def money_recognize(self,text):
        entities = self.ner(text)
        if entities is None :
            return
        money = []
        for entity in entities:
            if entity['nerTag'].lower() == "money":
                money.append(entity)
        return money

    def company_recognize(self, text):
        entities = self.ner(text)
        if entities is None:
            return
        financial_agency = []
        for entity in entities:
            if entity['nerTag'].lower() == "company_name":
                financial_agency.append(entity)
        return financial_agency

    def segment(self,text):
        self.__call__(text)
        return [v['token'] for v in self.result['tokens']]

    def postag(self,text):
        self.__call__(text)
        return self.result['tokens']

    def analyze(self, text):
        self.__call__(text)
        return self.result

    def parse_date(self,givendate,pubdate=None):
        parameters = {"givendate": givendate}
        if pubdate is not None or pubdate != "":
            parameters['pubdate'] = pubdate
        r = requests.get(self.url+'/querydate', params=parameters)
        return r.json()['payload']['response']

    def split2sentences(self,text:str):
        return self.__call__(text, path = '/split2sentences',max_size_limit=999999)

    def processcorpus(self,text):
        return self.__call__(text, path='/processcorpus', max_size_limit=999999)


