from smoothnlp import logger
from smoothnlp.algorithm.phrase.ngram_utils import sentence_split_by_punc,remove_irregular_chars,get_scores
import sqlalchemy
import _io


def chunk_generator_adapter(obj, chunk_size):
    '''
    返回chunk_size大小的语料preprocessing后的一个list
    :param obj:
    :param chunk_size:
    :return:
    '''
    corpus_chunk = []
    while True:
        try:
            if isinstance(obj,
                          sqlalchemy.engine.result.ResultProxy):  # 输入database connection object = conn.execute(query)
                obj_adapter = list(obj.fetchmany(chunk_size))
            elif isinstance(obj, _io.TextIOWrapper):  # 输入object = open(file_name, 'r', encoding='utf-8')
                obj_adapter = obj.readlines(chunk_size)# list
            elif isinstance(obj, list):  # 输入list
                obj_adapter = obj
            else:
                raise ValueError('Input not supported!')
            corpus_chunk = [remove_irregular_chars(sent) for r in obj_adapter for sent in
                                sentence_split_by_punc(str(r)) if remove_irregular_chars(sent) != 0]
            yield corpus_chunk
            corpus_chunk = []
            break
        except TypeError:
            yield corpus_chunk
            break


def extract_phrase(corpus,
                   top_k: float = 0.05,
                   chunk_size: int = 5000,
                   max_n:int=4,
                   min_freq:int = 0):
    '''
    取前k个new words或前k%的new words
    :param corpus:
    :param top_k:
    :param chunk_size:
    :param max_n:
    :param min_freq:
    :return:
    '''
    if isinstance(corpus,str):
        corpus_splits = [remove_irregular_chars(sent) for sent in sentence_split_by_punc(corpus)]
    else:
        corpus_splits = chunk_generator_adapter(corpus, chunk_size)
    word_info_scores = get_scores(corpus_splits,max_n,chunk_size,min_freq)
    new_words = [item[0] for item in sorted(word_info_scores.items(),key=lambda item:item[1][-1],reverse = True)]
    if top_k >1:              #输出前k个词
        return new_words[:top_k]
    elif top_k <1:            #输出前k%的词
        return new_words[:int(top_k*len(new_words))]



