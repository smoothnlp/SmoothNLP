import re
from ...nlp import nlp
from functools import wraps
from ...configurations import config
from itertools import combinations,product
from copy import copy,deepcopy

prettify = lambda l: "".join([t['token'] for t in l])

def prettify(output):
    if isinstance(output,list):
         return  "".join([t['token'] for t in output])
    elif isinstance(output,dict):
        for k in list(output.keys()):
            output[k] = prettify(output[k])
    return output

def adapt_struct(func):
    @wraps(func)
    def tostruct(struct:dict = None,text:str = None,*arg,**kargs):
        if "struct" in kargs:
            kargs.pop("struct")
        if isinstance(struct,str):
            struct = re.sub(u"(\([\w]+\)|（[\w]+）)","",struct)
            struct = nlp.analyze(struct)
        if struct is None or isinstance(text,str):
            text = re.sub(u"(\([\w]+\)|（[\w]+）)","",text)
            struct = nlp.analyze(text)
        for i in range(len(struct['tokens'])):
            struct['tokens'][i]['index'] = i+1
        if "dependencyRelationships" not in struct:
            config.logger.info("目前SmoothNLP中的知识抽取功能暂不支持长度过长的句子. 谢谢理解. ")
            nf = lambda struct : None
            return nf
        return func(struct = struct,*arg,**kargs)
    return tostruct

def options(func):
    def options_wrapper(*arg,**kargs):
        if "struct" in kargs:
            struct = kargs['struct']
        else:
            struct = arg[0]
        rels = struct['dependencyRelationships']

        def _conf_score(kgpiece):
            targets = [kgpiece['subject'],kgpiece['object']]
            _scores = []
            for target_phrase in targets:
                target_indexes = set([t['index'] for t in target_phrase])
                _pair_scores = [rel['_edge_score'] for rel in rels if rel['targetIndex'] in target_indexes and
                                rel['dependentIndex'] not in target_indexes]
                _scores.append(min(_pair_scores))
            return min(_scores)

            # _scores = []
            # for p1,p2 in combinations([v for v in kgpiece.values() if isinstance(v,list)],2):
            #     p1_indexes = [t['index'] for t in p1]
            #     p2_indexes = [t['index'] for t in p2]
            #     print("p1&p2:",p1_indexes,p2_indexes)
            #     _pair_scores = []
            #     for p1_index, p2_index in product(p1_indexes,p2_indexes):
            #        if (p1_index,p2_index) in rel2edge_score:
            #            print("add:",(p1_index,p2_index))
            #            _pair_scores.append(rel2edge_score.pop((p1_index,p2_index)))
            #        if (p2_index,p1_index) in rel2edge_score:
            #            print("add:",(p2_index,p1_index))
            #            _pair_scores.append(rel2edge_score.pop((p2_index,p1_index)))
            #
            #     if len(_pair_scores)>=1:
            #         print("_pair score",_pair_scores)
            #         _scores.append(min(_pair_scores))
            # print("_scores:",_scores)
            # return sum(_scores)/len(_scores)

        _pretty = kargs.pop("pretty") if "pretty" in kargs else False
        _with_conf_score = kargs.pop("_with_conf_score") if "_with_conf_score" in kargs else False

        func_output = func(*arg, **kargs)

        if _with_conf_score:
            rel2edge_score = {(rel['dependentIndex'], rel['targetIndex']): rel['_edge_score'] for rel in rels}
            _scores = [_conf_score(output) for output in func_output]
            for i in range(len(func_output)):
                func_output[i]['_conf_score'] = _scores[i]

        if _pretty:
            if not isinstance(func_output,list):
                raise TypeError("Function Output should be list of list of dictionary")
            # _scores = [_conf_score(rels,output) for output in func_output]
            func_output = [prettify(output) for output in func_output]
            return func_output
        else:
            return func_output
    return options_wrapper