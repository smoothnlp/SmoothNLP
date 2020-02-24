import requests
import time
import re
from ..configurations import config
from multiprocessing.pool import ThreadPool
from multiprocessing import Pool


def _request_single(text, path, counter=0, max_size_limit=200, other_params:dict = {}):
    if len(text) > max_size_limit:
        raise ValueError(
            "text with size over 200 is prohibited. you may use smoothnlp.nlp.split2sentences as preprocessing")
    if counter > 9999:
        raise Exception(
            " exceed maximal attemps for parsing. ")
    content = {"text": text,**other_params}
    r = requests.get(config.HOST + path, params=content)
    result = r.json()
    if r.status_code==429:
        counter += 1
        config.logger.debug("Request QPS exceeds server limit")
        config.logger.info("Request has been tried for {} times".format(counter))
        time.sleep(0.05)  ## 延迟50毫秒再调用
        return _request_single(text, path=path, counter=counter, max_size_limit=max_size_limit)
    elif isinstance(result, dict) and "payload" in result:
        return result['payload']['response']
    else:
        raise Exception(r.text)

def _request_concurent(texts:list,path,max_size_limit=200,other_params:dict = {}):
    if config.POOL_TYPE=="process":
        pool = Pool(config.NUM_THREADS)
    else:
        pool = ThreadPool(config.NUM_THREADS)
    params = [(text,path,0,max_size_limit,other_params) for text in texts]
    result = pool.starmap(_request_single,params)
    pool.close()
    return result

def _request(text, path="/nlp/query", max_size_limit=500, other_params:dict = {}):
    if isinstance(text,list):
        config.logger.info(
            "request parameter: NUM_THREAD = {}, POOL_TYPE = {}".format(config.NUM_THREADS, config.POOL_TYPE))
        return _request_concurent(text,path,max_size_limit,other_params)
    elif isinstance(text,str):
        return _request_single(text,path,counter=0,max_size_limit=max_size_limit,other_params=other_params)
    elif isinstance(text,dict):
        return text
    else:
        TypeError(" Unsupported Type of text input")

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
        if tokens is None or text is None:
            return []
        elif isinstance(text,str) or isinstance(text,dict):
            tokens = [v['token']for v in tokens]
        else:
            tokens = [[v['token']for v in token_set] for token_set in tokens]
        return tokens

    def postag(self,text):
        result = _request(text)
        tokens = extract_meta(result, "tokens")
        return tokens

    def analyze(self, text):
        return _request(text,path=config.NLP_PATH)

    def parse_date(self,givendate,pubdate=None):
        parameters = {"givendate": givendate}
        if pubdate is not None or pubdate != "":
            parameters['pubdate'] = pubdate
        r = requests.get(config.HOST+'/querydate', params=parameters)
        return r.json()['payload']['response']

    def split2sentences(self,text:str):
        split_pattern = "[。;!?！？;\n\rn]+"
        return re.split(split_pattern,text)
        # return _request(text, path = '/split2sentences',max_size_limit=999999)

    def processcorpus(self,text):
        texts = self.split2sentences(text)
        texts = [t for t in texts if len(t)>=3 and len(t)<=200]
        return self.analyze(texts)
        # return _request(text, path='/processcorpus', max_size_limit=999999)


