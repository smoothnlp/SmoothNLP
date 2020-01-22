import requests
import time
from smoothnlp.config import HOST,NUM_THREADS
from multiprocessing.pool import ThreadPool


def _request_single(text, path="/query", counter=0, max_size_limit=200):
    if len(text) > max_size_limit:
        raise ValueError(
            "text with size over 200 is prohibited. you may use smoothnlp.nlp.split2sentences as preprocessing")
    if counter > 10:
        raise Exception(
            " exceed maximal attemps for parsing. ")
    content = {"text": text}
    r = requests.get(HOST + path, params=content)
    result = r.json()
    if isinstance(result, dict) and "payload" in result:
        return result['payload']['response']
    else:
        counter += 1
        time.sleep(0.05)  ## 延迟50毫秒再调用
        return _request_single(text, path=path, counter=counter, max_size_limit=max_size_limit)

def _request_concurent(texts:list,path,max_size_limit=200):
    pool = ThreadPool(NUM_THREADS)
    params = [(text,path,0,max_size_limit) for text in texts]
    result = pool.map(_request_single,params)
    pool.close()
    return result

def _request(text, path="/query", max_size_limit=200):
    if isinstance(text,list):
        return _request_concurent(text,path,max_size_limit)
    if isinstance(text,str):
        return _request_single(text,path,counter=0,max_size_limit=max_size_limit)
    else:
        TypeError(" Unsupported Type of ")

def extract_meta(meta,key):
    if isinstance(meta,dict):
        return meta[key]
    elif isinstance(meta,list):
        return [m[key] for m in meta]
    else:
        raise ValueError("Meta Extraction Failure")

class SmoothNLPRequest(object):
    def __init__(self):
        pass

    def dependencyrelationships(self,text):
        result = _request(text)
        return extract_meta(result,"dependencyRelationships")

    def ner(self,text):
        result = _request(text)
        return extract_meta(result,"entities")

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
        result = _request(text)
        tokens = extract_meta(result, "tokens")
        tokens = [[v['token']for v in token_set] for token_set in tokens]
        return tokens

    def postag(self,text):
        result = _request(text)
        tokens = extract_meta(result, "tokens")
        return tokens

    def analyze(self, text):
        return _request(text)

    def parse_date(self,givendate,pubdate=None):
        parameters = {"givendate": givendate}
        if pubdate is not None or pubdate != "":
            parameters['pubdate'] = pubdate
        r = requests.get(HOST+'/querydate', params=parameters)
        return r.json()['payload']['response']

    def split2sentences(self,text:str):
        return _request(text, path = '/split2sentences',max_size_limit=999999)

    def processcorpus(self,text):
        return _request(text, path='/processcorpus', max_size_limit=999999)


