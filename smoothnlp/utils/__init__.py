from functools import wraps
import requests
import json

from smoothnlp import logger


########################
## attribute function ##
########################
def localSupportCatch(func):
    @wraps(func)
    def trycatch(text):
        try:
            return func(text)
        except AttributeError:
            logger.error("This function does not support local mode : %s " % func.__name__)
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