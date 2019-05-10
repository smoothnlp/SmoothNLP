global __name__
__name__ = "SmoothNLP"

import smoothnlp.jvm as jvm
from smoothnlp.jvm import initJVMConnection
from smoothnlp.server import smoothNlpRequest
#smoothnlp = jvm.LazyLoadingJClass("com.smoothnlp.nlp.SmoothNLP")
smoothnlp = smoothNlpRequest()