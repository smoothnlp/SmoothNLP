global __name__
__name__ = "SmoothNLP"

import smoothnlp.jvm as jvm
from smoothnlp.jvm import initJVMConnection

smoothnlp = jvm.LazyLoadingJClass("com.smoothnlp.nlp.SmoothNLP")
