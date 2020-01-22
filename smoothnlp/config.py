import logging
HOST = "http://kong.smoothnlp.com/nlp"
NUM_THREADS = 2
LOG_LEVEL = 30

def setLogLevel(level):
    logging.basicConfig(level=LOG_LEVEL)

setLogLevel(LOG_LEVEL)
logger = logging.getLogger("SmoothNLP")