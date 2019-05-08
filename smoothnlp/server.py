import requests

class smoothNlpRequest(object):
    def __init__(self, url:str="http://api.smoothnlp.com/query"):
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

    def financial_agency_recognize(self, text):
        entities = self.ner(text)
        if entities is None:
            return
        financial_agency = []
        for entity in entities:
            if entity['nerTag'].lower() == "financial_agency":
                financial_agency.append(entity)
        return financial_agency

    def segment(self,text):
        self.__call__(text)
        return self.result['tokens']

    def analyze(self, text):
        return self.__call__(text)