import sys
from .algorithm import kg
from .configurations import config

if sys.version_info[0] != 3:
    # Only support python version 3
    raise EnvironmentError("~~ SmoothNLP supports Python3 ONLY for now ~~~")

__version__ = "0.3.1"
__author__ = "SmoothNLP Organization"


################################
## smoothnlp support function ##
################################

from .server import SmoothNLPClient

nlp = SmoothNLPClient()

def segment(text):
    return nlp.segment(text)

def postag(text):
    return nlp.postag(text)

def ner(text):
    return nlp.ner(text)

def company_recognize(text):
    return nlp.company_recognize(text)

def number_recognize(text):
    return nlp.number_recognize(text)

def money_recognize(text):
    return nlp.money_recognize(text)

def parse_date(givendate,pubdate=None):
    return nlp.parse_date(givendate,pubdate)

def split2sentences(text:str):
    return nlp.split2sentences(text)

def dep_parsing(text:str):
    return nlp.dependencyrelationships(text)

