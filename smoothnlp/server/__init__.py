import requests
from smoothnlp import HOST_URL

class smoothNlpRequest(object):
    def __init__(self):
        self.url = HOST_URL

    def set_url(self,url):
        self.url = url

    def __call__(self,text):
        content = {"text":text}
        r = requests.get(self.url, params=content)
        self.result = r.json()['payload']['response']

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
        r = requests.get(self.url+'/parsedate', params=parameters)
        return r.json()['payload']['response']

