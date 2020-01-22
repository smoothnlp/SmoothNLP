from .server import SmoothNLPRequest

global MODE, nlp
MODE = 'server'

class SmoothNLP(object):
    def __init__(self, mode: str = 'server'):
        self.mode = mode
        if self.mode == 'local':
            from smoothnlp.static.jvm import _start_jvm_for_smoothnlp
            from smoothnlp.static.jvm import SafeJClass
            _start_jvm_for_smoothnlp()
            self.nlp = SafeJClass('com.smoothnlp.nlp.SmoothNLP')
        else:
            self.nlp= SmoothNLPRequest()

    def set_mode(self,mode):
        self = SmoothNLP(mode)

def set_mode(mode):
    """
    This Will be decrete
    :param mode:
    :return:
    """
    MODE = mode
    nlp = SmoothNLP(MODE).nlp


nlp = SmoothNLP(MODE).nlp
