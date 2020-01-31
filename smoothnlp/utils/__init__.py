from functools import wraps
import requests
import json

from smoothnlp import config
from ..nlp import MODE
from ..nlp import set_mode
import re


########################
## attribute function ##
########################
def localSupportCatch(func):
    @wraps(func)
    def trycatch(text):
        if MODE !="server":
            config.logger.error("This function does not support local mode : %s " % func.__name__)
            raise AttributeError("This function does not support local mode : %s ")
        return func(text)
    return trycatch

def requestTimeout(func):
    @wraps(func)
    def trycatch(text):
        try:
            return func(text)
        except requests.exceptions.Timeout:
            set_mode('local')
            return func(text)
    return trycatch

def convert(func):
    @wraps(func)
    def toJson(text):
        res = func(text)
        if(isinstance(res, list)):
            return res
        else:
            return json.loads(res)
    return toJson

def remove_nonchinese(text):
    return " ".join(re.findall(r'[\u4e00-\u9fff]+', text))