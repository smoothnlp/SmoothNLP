import sys
from .algorithm import kg
from .configurations import config

if sys.version_info[0] != 3:
    # Only support python version 3
    raise EnvironmentError("~~ SmoothNLP supports Python3 ONLY for now ~~~")

__version__ = "0.3.0"
__author__ = "SmoothNLP Organization"


################################
## smoothnlp support function ##
################################

from .utils import requestTimeout,convert,localSupportCatch
from .nlp import nlp

@requestTimeout
@convert
def segment(text):
    return nlp.segment(text)

@requestTimeout
@convert
def postag(text):
    return nlp.postag(text)

@requestTimeout
@convert
def ner(text):
    return nlp.ner(text)

@localSupportCatch
def company_recognize(text):
    return nlp.company_recognize(text)

@localSupportCatch
def number_recognize(text):
    return nlp.number_recognize(text)

@localSupportCatch
def money_recognize(text):
    return nlp.money_recognize(text)

@localSupportCatch
def parse_date(givendate,pubdate=None):
    return nlp.parse_date(givendate,pubdate)

@localSupportCatch
def split2sentences(text:str):
    return nlp.split2sentences(text)

@localSupportCatch
def dep_parsing(text:str):
    return nlp.dependencyrelationships(text)



