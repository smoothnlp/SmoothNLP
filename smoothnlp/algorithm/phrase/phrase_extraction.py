from smoothnlp.algorithm.phrase.ngram_utils import sentence_split_by_punc,remove_irregular_chars,get_scores
from datetime import datetime
import _io
from smoothnlp import config
import types


def chunk_generator_adapter(obj, chunk_size):
    '''
    返回chunk_size大小的语料preprocessing后的一个list
    :param obj:
    :param chunk_size:
    :return:
    '''
    tstart = datetime.now()
    while True:
        import sqlalchemy
        if isinstance(obj,sqlalchemy.engine.result.ResultProxy):  # 输入database connection object = conn.execute(query)
            obj_adapter = list(obj.fetchmany(chunk_size))
        elif isinstance(obj, _io.TextIOWrapper):     # 输入object = open(file_name, 'r', encoding='utf-8')
            obj_adapter = obj.readlines(chunk_size)  # list of str
        elif isinstance(obj,types.GeneratorType):
            obj_adapter = list(next(obj,''))
        else:
            raise ValueError('Input not supported!')
        if obj_adapter != None and obj_adapter != []:
            corpus_chunk = [remove_irregular_chars(sent) for r in obj_adapter for sent in
                                sentence_split_by_punc(str(r))]
            yield corpus_chunk
        else:
            tend = datetime.now()
            sec_used = (tend-tstart).seconds
            config.logger.info('~~~ Time used for data processing: {} seconds'.format(sec_used))
            break


def extract_phrase(corpus,
                   top_k: float = 200,
                   chunk_size: int = 1000000,
                   min_n:int = 2,
                   max_n:int=4,
                   min_freq:int = 5):
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
    elif isinstance(corpus,list):
        corpus_splits = [remove_irregular_chars(sent) for news in corpus for sent in
                                sentence_split_by_punc(str(news)) if remove_irregular_chars(sent) != 0]
    else:
        corpus_splits = chunk_generator_adapter(corpus, chunk_size)
    word_info_scores = get_scores(corpus_splits,min_n,max_n,chunk_size,min_freq)
    new_words = [item[0] for item in sorted(word_info_scores.items(),key=lambda item:item[1][-1],reverse = True)]
    if top_k > 1:              #输出前k个词
        return new_words[:top_k]
    elif top_k < 1:            #输出前k%的词
        return new_words[:int(top_k*len(new_words))]



