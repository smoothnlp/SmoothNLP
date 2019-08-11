from smoothnlp import logger

from smoothnlp.algorithm.phrase.ngram_utils import sentence_split_by_punc,remove_irregular_chars,get_scores
from collections.abc import Iterable

def extract_phrase(corpus,
                   max_n:int=4,
                   min_freq:int = 0):
    if isinstance(corpus,Iterable):
        corpus_splits = []
        for c in corpus:
            corpus_splits+=sentence_split_by_punc(c)
    elif isinstance(corpus,str):
        corpus_splits = sentence_split_by_punc(corpus)
    return get_scores(corpus_splits,max_n,min_freq)
