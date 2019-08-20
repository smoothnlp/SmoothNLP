from smoothnlp import logger

from smoothnlp.algorithm.phrase.ngram_utils import sentence_split_by_punc,remove_irregular_chars,get_scores
from collections.abc import Iterable

def extract_phrase(corpus,
                   top_k: float = 0.05,
                   chunk_size: int = 5000,
                   max_n:int=4,
                   min_freq:int = 0):
    if isinstance(corpus,Iterable):
        corpus_splits = []
        for c in corpus:
            corpus_splits+=sentence_split_by_punc(c)
    elif isinstance(corpus,str):
        corpus_splits = sentence_split_by_punc(corpus)
    new_words = [item[0] for item in sorted(get_scores(corpus_splits,chunk_size,max_n,min_freq).items(),key=lambda item:item[1][-1],reverse = True)]
    if top_k >1:              #输出前k个词
        return new_words[:top_k]
    elif top_k <1:            #输出前k%的词
        return new_words[:int(top_k*len(new_words))]
