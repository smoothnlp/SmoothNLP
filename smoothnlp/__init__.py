import sys
import logging

global MODE, nlp
MODE = 'server'
HOST_URL = "http://data.service.nlp.smoothnlp.com"
logger = logging.getLogger()

from smoothnlp.server import smoothNlpRequest
import smoothnlp.algorithm


if sys.version_info[0] != 3:
    # now support python version 3
    raise EnvironmentError("~~ SmoothNLP supports Python3 ONLY for now ~~~")


class Smoothnlp(object):
    def __init__(self, mode: str = 'server'):
        self.mode = mode
        if self.mode == 'local':
            from smoothnlp.static.jvm import _start_jvm_for_smoothnlp
            from smoothnlp.static.jvm import SafeJClass
            _start_jvm_for_smoothnlp()
            self.nlp = SafeJClass('com.smoothnlp.nlp.SmoothNLP')
        else:
            self.nlp= smoothNlpRequest()

    def set_mode(self,mode):
        self = Smoothnlp(mode)

def set_mode(mode):
    """
    This Will be decrete
    :param mode:
    :return:
    """
    MODE = mode
    nlp = Smoothnlp(MODE).nlp


nlp = Smoothnlp(MODE).nlp


################################
## smoothnlp support function ##
################################

from smoothnlp.utils import requestTimeout,convert,localSupportCatch

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

