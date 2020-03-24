import requests
requests.adapters.DEFAULT_RETRIES = 10
import time
import re
from ..configurations import config
from multiprocessing.pool import ThreadPool
from multiprocessing import Pool
from functools import wraps
from requests.exceptions import Timeout
from requests.exceptions import ConnectionError

def pro_only(func):
    @wraps(func)
    def check_apikey(*arg,**kargs):
        print(config.apikey)
        if config.apikey is None:
            config.logger.critical("{} 仅仅支持 smoothnlp pro 版本. 请联系contact@smoothnlp.com 获得license".format(func.__name__))
            raise ValueError("{} is a pro only function. set coorect apikey or email contact@smoothnlp.com to get a pro license".format(func.__name__))
        return func(*arg,**kargs)
    return check_apikey()

def _request_single(text, path, counter=0, max_size_limit=200, other_params:dict = {}):
    if len(text) > max_size_limit:
        raise ValueError(
            "text with size over 200 is prohibited. you may use smoothnlp.nlp.split2sentences as preprocessing")
    if counter > 99:
        raise Exception(
            " exceed maximal attemps for parsing. ")
    if config.apikey is not None and isinstance(config.apikey,str):   ## pro 版本支持 apikey 调用
        other_params['apikey'] = config.apikey
    content = {"text": text,**other_params}
    try:
        r = requests.get(config.HOST + path, params=content,timeout=120)
    except (Timeout) as e:
        config.logger.critical(str(e))
        return None
    except (ConnectionError) as e:
        config.logger.warn(str(e))
        time.sleep(0.5)  ## 延迟50毫秒再调用
        counter += 10
        return
    config.logger.debug("sending request to {} with parameter={}".format(config.HOST + path,content))
    result = r.json()
    if r.status_code==429:  ## qps超限制
        counter += 1
        config.logger.debug("Request QPS exceeds server limit")
        config.logger.info("Request has been tried for {} times".format(counter))
        time.sleep(0.05)  ## 延迟50毫秒再调用
        return _request_single(text, path=path, counter=counter, max_size_limit=max_size_limit)
    elif isinstance(result, dict) and "payload" in result:
        response = result['payload']['response']
        return response
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

def _request(text, path = config.NLP_PATH, max_size_limit=500, other_params:dict = {}):
    if isinstance(text,list):
        config.logger.info(
            "request parameter: NUM_THREAD = {}, POOL_TYPE = {}".format(config.NUM_THREADS, config.POOL_TYPE))
        return _request_concurent(text,path,max_size_limit,other_params)
    elif isinstance(text,str):
        return _request_single(text,path = path,counter=0,max_size_limit=max_size_limit,other_params=other_params)
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

class SmoothNLPClient(object):
    def __init__(self):
        pass

    def dependencyrelationships(self,text):
        """
        依存句法分析
        :param text:
        :return:
        """
        result = _request(text)
        return extract_meta(result,"dependencyRelationships")

    def ner(self,text):
        """
        实体识别
        :param text:
        :return:
        """
        result = _request(text,path=config.NLP_PATH)
        return extract_meta(result,"entities")

    def number_recognize(self,text):
        """
        将在smoothnlp V0.3.1 中deprecate
        :param text:
        :return:
        """
        entities = self.ner(text)
        if entities is None :
            return
        numbers = []
        for entity in entities:
            if entity['nerTag'].lower() == "number":
                numbers.append(entity)
        return numbers

    def money_recognize(self,text):
        """
        将在smoothnlp V0.3.1 中deprecate
        :param text:
        :return:
        """
        entities = self.ner(text)
        if entities is None :
            return
        money = []
        for entity in entities:
            if entity['nerTag'].lower() == "money":
                money.append(entity)
        return money

    def company_recognize(self, text):
        """
        将在smoothnlp V0.3.1 中deprecate
        :param text:
        :return:
        """
        entities = self.ner(text)
        if entities is None:
            return
        financial_agency = []
        for entity in entities:
            if entity['nerTag'].lower() in {"company_name","gs"}:
                financial_agency.append(entity)
        return financial_agency

    def segment(self,text):
        """
        切词
        :param text:
        :return:
        """
        result = _request(text,path=config.NLP_PATH)
        tokens = extract_meta(result, "tokens")
        if tokens is None or text is None:
            return []
        elif isinstance(text,str) or isinstance(text,dict):
            tokens = [v['token']for v in tokens]
        else:
            tokens = [[v['token']for v in token_set] for token_set in tokens]
        return tokens

    def postag(self,text):
        """
        词性标注
        :param text:
        :return:
        """
        result = _request(text,path=config.NLP_PATH)
        tokens = extract_meta(result, "tokens")
        return tokens

    def analyze(self, text):
        return _request(text,path=config.NLP_PATH)

    def parseq(self, text, parseq_url = "/pro/kg/parseq"):
        return _request(text, path= parseq_url)

    def process_corpus(self,corpus,path):
        return _request(text = corpus, path = path, max_size_limit = 999999)

    def parse_date(self,givendate,pubdate=None):
        """
        (根据绝对日期) , 解析日期对应的真实日期范围
        :param givendate:
        :param pubdate:
        :return:
        """
        parameters = {"givendate": givendate}
        if pubdate is not None or pubdate != "":
            parameters['pubdate'] = pubdate
        r = requests.get(config.HOST+'/querydate', params=parameters)
        return r.json()['payload']['response']

    def split2sentences(self,text:str):
        """
        依据标点正则切句
        :param text:
        :return:
        """
        split_pattern = "(.+[。!?！？；;……\n\r]+)"
        return [s.strip() for s in re.split(split_pattern,text) if s]

nlp = SmoothNLPClient()





