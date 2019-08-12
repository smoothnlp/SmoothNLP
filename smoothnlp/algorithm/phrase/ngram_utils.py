from smoothnlp import logger
from collections import Counter
import io
import re
import types
from multiprocessing import cpu_count,Pool
import math
from collections.abc import Iterable
from operator import mul
from functools import reduce
from pygtrie import Trie

CPU_COUNT = 1

def union_word_freq(dic1,dic2):
    '''
    word_freq合并
    :param dic1:{'你':200,'还':2000,....}:
    :param dic2:{'你':300,'是':1000,....}:
    :return:{'你':500,'还':2000,'是':1000,....}
    '''
    keys = (dic1.keys()) | (dic2.keys())
    total = {}
    for key in keys:
        total[key] = sum([dic.get(key, 0) for dic in [dic1, dic2]])
    return total

def sentence_split_by_punc(corpus:str):
    return re.split(r'[;；.。，,！\n!?？]',corpus)

def remove_irregular_chars(corpus):
    return re.sub(u"([^\u4e00-\u9fa5\u0030-\u0039\u0041-\u005a\u0061-\u007a])", "", corpus)

def generate_ngram(corpus,n:int=2):
    """
    对一句话生成ngram并统计词频字典，n=token_length,
    返回: generator (节省内存)
    :param corpus:
    :param n:
    :return:
    """
    def generate_ngram_str(text:str,n):
        for i in range(0,len(text)-n):
            yield text[i:i+n]
    if isinstance(corpus,str):
        for ngram in generate_ngram_str(corpus,n):
            yield ngram
    elif isinstance(corpus,list) or isinstance(corpus,types.GeneratorType):
        for text in corpus:
            for ngram in generate_ngram_str(text,n):
                yield ngram

def get_ngram_freq_info(corpus, ## list or generator
                         max_n:int=4,
                         chunk_size:int=5000,
                         min_freq:int=2,
                         ):

    ngram_freq_total = {}  ## 记录词频
    ngram_keys = {i: set() for i in range(1, max_n + 2)}  ## 用来存储N=?时, 都有哪些词

    def _process_corpus_chunk(corpus_chunk):
        ngram_freq = {}
        for ni in range(1, max_n + 2):
            ngram_generator = generate_ngram(corpus_chunk, ni)
            nigram_freq = dict(Counter(ngram_generator))
            ngram_keys[ni] = (ngram_keys[ni] | nigram_freq.keys())
            ngram_freq = {**nigram_freq, **ngram_freq}
        ngram_freq = {k: v for k, v in ngram_freq.items() if v >= min_freq}  ## 每个chunk的ngram频率统计
        return ngram_freq

    if isinstance(corpus,types.GeneratorType):
        for corpus_chunk in corpus:
            ngram_freq = _process_corpus_chunk(corpus_chunk)
            ngram_freq_total = union_word_freq(ngram_freq, ngram_freq_total)
    elif isinstance(corpus,list):
        len_corpus = len(corpus)
        for i in range(0,len_corpus,chunk_size):
            corpus_chunk = corpus[i:min(len_corpus,i+chunk_size)]
            ngram_freq = _process_corpus_chunk(corpus_chunk)
            ngram_freq_total = union_word_freq(ngram_freq,ngram_freq_total)
    for k in ngram_keys:
        ngram_keys[k] = ngram_keys[k] & ngram_freq_total.keys()
    return ngram_freq_total,ngram_keys

def _ngram_entropy_scorer(parent_ngrams_freq):
    """
    根据一个candidate的neighbor的出现频率, 计算Entropy
    :param parent_ngrams_freq:
    :return:
    """
    _total_count = sum(parent_ngrams_freq)
    _parent_ngram_probas = map(lambda x: x/_total_count,parent_ngrams_freq)
    _entropy = sum(map(lambda x: -1 * x * math.log(x,2),_parent_ngram_probas))
    return _entropy

def _calc_ngram_entropy(ngram_freq,
                        ngram_keys,
                        n,
                        dir:int=1):

    if isinstance(n,Iterable): ## 一次性计算 len(N)>1 的 ngram
        entropy = {}
        for ni in n:
            entropy = {**entropy,**_calc_ngram_entropy(ngram_freq,ngram_keys,ni,dir)}
        return entropy

    ngram_entropy = {}
    target_ngrams = ngram_keys[n]
    parent_candidates = ngram_keys[n+1]

    if CPU_COUNT == 1:
        ## 对 n+1 gram 进行建Trie处理
        left_neighbors = Trie()
        right_neighbors = Trie()
        for parent_candidate in parent_candidates:
            right_neighbors[parent_candidate] = ngram_freq[parent_candidate]
            left_neighbors[parent_candidate[1:]+parent_candidate[0]] = ngram_freq[parent_candidate]
        ## 计算
        for target_ngram in target_ngrams:
            try:
                right_neighbor_counts = (right_neighbors.values(target_ngram))
                right_entropy = _ngram_entropy_scorer(right_neighbor_counts)
            except KeyError:
                right_entropy = 0
            try:
                left_neighbor_counts = (left_neighbors.values(target_ngram))
                left_entropy = _ngram_entropy_scorer(left_neighbor_counts)
            except KeyError:
                left_entropy = 0
            ngram_entropy[target_ngram] = (left_entropy,right_entropy)
        return ngram_entropy
    else:
        ## TODO 多进程计算
        pass

def _calc_ngram_pmi(ngram_freq,ngram_keys,n):
    """
    计算 Pointwise Mutual Information
    :param ngram_freq:
    :param ngram_keys:
    :param n:
    :return:
    """
    if isinstance(n,Iterable):
        mi = {}
        for ni in n:
            mi = {**mi,**_calc_ngram_pmi(ngram_freq,ngram_keys,ni)}
        return mi
    n1_totalcount = sum([ngram_freq[k] for k in ngram_keys[1] if k in ngram_freq])
    target_n_total_count = sum([ngram_freq[k] for k in ngram_keys[n] if k in ngram_freq])
    mi = {}
    for target_ngram in ngram_keys[n]:
        target_ngrams_freq = ngram_freq[target_ngram]
        joint_proba = target_ngrams_freq/target_n_total_count
        indep_proba = reduce(mul,[ngram_freq[char] for char in target_ngram])/((n1_totalcount)**n)
        mi[target_ngram] = math.log(joint_proba/indep_proba,2)
    return mi



def get_scores(corpus,
               chunk_size:int=5000,
               max_n:int=4,
               min_freq:int=0):
    ngram_freq, ngram_keys = get_ngram_freq_info(corpus,max_n,
                                                 chunk_size=chunk_size,
                                                 min_freq=min_freq)
    left_right_entropy = _calc_ngram_entropy(ngram_freq,ngram_keys,range(2,max_n+1))
    mi = _calc_ngram_pmi(ngram_freq,ngram_keys,range(2,max_n+1))
    joint_phrase = mi.keys() & left_right_entropy.keys()
    scores = {k:(mi[k],
                 left_right_entropy[k][0],
                 left_right_entropy[k][1]
                 )
              for k in joint_phrase}
    return scores


# print(sentence_split_by_punc("你好,我叫Victor"))
#
# print(list(generate_ngram("你好,我叫Victor")))
# print(generate_ngram(["你好,我叫Victor"]))
#
# corpus = ["你好,我叫Victor","你好,我叫Jacinda","你好,我叫Tracy"]
# ngram_freq,ngram_keys = get_ngram_freq_info(corpus,min_freq=0)
#
# print(get_scores(corpus))
#
# print(get_scores(corpus_iterator(corpus),chunk_size=1))
