from ...nlp import nlp
from functools import wraps
from copy import deepcopy
from .helper import *

_object_rels = {"dobj","range","attr"}
_subject_rels = {"nsubj","top"}
_num_rels = {"range","nummod"}


phrase_index_range = lambda l: [t['index'] for t in l]


def _find_phrase_connected_rel(phrase, rel_map, valid_rels = set()):
    rels = []
    for token in phrase:
        if token['index'] in rel_map:
            rels += rel_map[token['index']]
    if len(valid_rels)>0:
        rels = [rel for rel in rels if rel['relationship'] in valid_rels]
    return rels

def _split_conj_sents(struct:dict = None):
    """
    如果动词之间出现并列关系, 且中间出现逗号, 进行隔句处理
    :param struct:
    :return:
    """
    tokens = struct['tokens']
    rels = struct['dependencyRelationships']
    conj_pairs = [(rel['dependentIndex'],rel['targetIndex']) for rel in rels if rel['relationship'] == 'conj']
    split_indexes = [0]
    for i in range(1,len(tokens)+1):
        if tokens[i-1]['postag'] == "PU" \
                and tokens[i-1]['postag'] not in {"“","”"}:
            for pair in conj_pairs:
                if pair[0]<=i<= pair[1]:
                    split_indexes.append(i)

    for i in range(len(split_indexes)-1,0,-1):
        if split_indexes[i] - split_indexes[i-1]<15:

            split_indexes.pop(i)
    split_indexes.pop(0)
    return split_indexes

def extend_valid_rel(rels):
    """
    基于 "conj" 对现有的dependency 做补充
    :param rels:
    :return:
    """
    output_rels = deepcopy(rels)
    for dindex in set([rel['dependentIndex'] for rel in rels]):
        index_rels = [r for r in rels if r['dependentIndex']==dindex]  ## 找出由dindex出发的relationship
        drels = [r for r in index_rels if r['relationship']=="conj" and r['_tag_score']>0.8 and r['_edge_score']>0.5]  ## 找出存在 conj 并列关系的case

        index_rels = [r for r in index_rels if r['relationship'] != "conj"]  ## 可以被拓展的关联 dindex 开始的关联
        index_rels_relationship = set([ r['relationship'] for r in index_rels ])
        for drel in drels:  ## 对每一个拓展词做处理
            extra_rels = deepcopy(index_rels)

            ## 对于 conj 两边词汇公有的 rel, 不做拓展
            drel_targetIndex = drel['targetIndex']
            target_covered_rels_relationship = set([r['relationship'] for r in rels if r['dependentIndex']==drel_targetIndex])
            for rel_set in [_object_rels,_subject_rels,_num_rels]:  ## 拓展 主语,宾语, 数字 rel,
                if len(target_covered_rels_relationship.intersection(rel_set))>=1:
                    target_covered_rels_relationship = target_covered_rels_relationship.union(rel_set)

            extendable_rel = _object_rels.union(_subject_rels).union(_num_rels)
            extendable_rel = extendable_rel - target_covered_rels_relationship

            extra_rels = [r for r in extra_rels if r['relationship'] in extendable_rel]  ## 过滤掉已经被拓展的rel

            # subject_valid = True   ### 检查要跳转的词前面有没有主语, 如果有, 不跳转
            # drel_targetIndex = drel['targetIndex']
            # for i in range(max(drel_targetIndex-5,0),drel_targetIndex-1):
            #     if rels[i]['relationship'] in {"nsubj","top"}:
            #         subject_valid = False
            #         break
            # if not subject_valid:
            #     continue


            for erel in extra_rels:
                erel['dependentIndex'] = drel['targetIndex']
                # erel["dependentToken"] = drel['targetToken']
            output_rels+=extra_rels
    return output_rels

def _get_rel_map(struct):
    rel_map = {}
    # rels = extend_valid_rel(struct['dependencyRelationships'])
    rels = struct['dependencyRelationships']
    for rel in rels:
        if rel['dependentIndex'] in rel_map:
            rel_map[rel['dependentIndex']].append(rel)
        else:
            rel_map[rel['dependentIndex']] = [rel]
    return rel_map


def _get_reverse_rel_map(struct):
    rel_map = {}
    # rels = extend_valid_rel(struct['dependencyRelationships'])
    rels = struct['dependencyRelationships']
    for rel in rels:
        if rel['targetIndex'] in rel_map:
            rel_map[rel['targetIndex']].append(rel)
        else:
            rel_map[rel['targetIndex']] = [rel]
    return rel_map

def adapt_struct(func):
    @wraps(func)
    def tostruct(struct:dict = None,text:str = None,*arg,**kargs):
        if "struct" in kargs:
            kargs.pop("struct")
        if isinstance(struct,str):
            struct = nlp.analyze(struct)
        if struct is None or isinstance(text,str):
            struct = nlp.analyze(text)
        for i in range(len(struct['tokens'])):
            struct['tokens'][i]['index'] = i+1
        return func(struct = struct,*arg,**kargs)
    return tostruct

@adapt_struct
def extract_phrase(struct: dict = None,
                   multi_token_only=True,
                   pretty=False,
                   valid_postags={},
                   invalid_postags={},
                   valid_rels={},
                   rm_one_char: bool = False):

    tokens = struct['tokens']
    rel_map = _get_rel_map(struct)

    phrases = []
    for i in range(len(tokens)):
        index = i + 1
        tokens[i]['index'] = index
        if tokens[i]['postag'] in valid_postags:
            phrases.append([tokens[i]])


    def extend_phrase_by_rel(phrases,start_index:int,rel_map, valid_rels:set = {}):
        if start_index>= len(phrases)-1:
            return phrases

        p1 = phrases.pop(start_index)
        p2 = phrases.pop(start_index)
        if p1[-1]['index'] + 1 != p2[0]['index']:  ## 检查p1, p2 是否相邻
            phrases.insert(start_index, p2)
            phrases.insert(start_index, p1)
            start_index += 1
            return extend_phrase_by_rel(phrases, start_index=start_index, rel_map=rel_map, valid_rels=valid_rels)

        p1_indexes = set([t['index'] for t in p1])
        p2_indexes = set([t['index'] for t in p2])
        p1_rels_targetIndexes = set([rel['targetIndex'] for rel in _find_phrase_connected_rel(phrase=p1,rel_map=rel_map) if rel['relationship'] in valid_rels])
        p2_rels_targetIndexes = set([rel['targetIndex'] for rel in _find_phrase_connected_rel(phrase=p2,rel_map=rel_map) if rel['relationship'] in valid_rels])

        if len(p1_indexes.intersection(p2_rels_targetIndexes)) >= 1 or len(
                p2_indexes.intersection(p1_rels_targetIndexes)) >= 1 or \
               p1[-1]['postag'] == p2[0]['postag']: ## share 相同postag 合并

            ## todo: share 相同的 targetRelationship , 合并

            # print("p1: ", prettify(p1))
            # print("p2: ", prettify(p2))
            # print("merge p1,p2")
            new_p = p1+p2
            phrases.insert(start_index,new_p)
        else:
            phrases.insert(start_index,p2)
            phrases.insert(start_index,p1)
            start_index += 1
        return extend_phrase_by_rel(phrases,start_index=start_index,rel_map=rel_map,valid_rels=valid_rels)

    phrases = extend_phrase_by_rel(phrases,start_index=0,rel_map=rel_map,valid_rels=valid_rels)
    # print(" -- output phrases: ",phrases)

    if multi_token_only:
        phrases = [phrase for phrase in phrases if len(phrase) > 1 ]

    if rm_one_char:
        phrases = [phrase for phrase in phrases if len(phrase) == 1 or len(phrase[0]['token']) > 1]

    if pretty:
        phrases = [prettify(p) for p in phrases]
    return phrases


def deduple_phrases(phrases):
    phrases.sort(key=lambda x: len(x), reverse=True)
    covered_indexes = set()
    topop_phrases = []
    for p in phrases:
        if sum([(token['index'] in covered_indexes) for token in p]) == 0:
            for token in p:
                covered_indexes.add(token['index'])
        else:
            topop_phrases.append(p)
    for p in topop_phrases:
        phrases.remove(p)
    return phrases


def concat_consecutive_phrases(phrases):
    tokens = {t['index']:t for p in phrases for t in p}
    tokens = list(tokens.values())
    tokens.sort(key=lambda x: x['index'])
    phrases = []
    if (len(tokens)<1):
        return phrases
    phrase = [tokens[0]]
    for token in tokens[1:]:
        if (phrase[-1]['index']+1 == token['index']):
            phrase.append(token)
        else:
            phrases.append(phrase)
            phrase = [token]
    if len(phrase)>0:
        phrases.append(phrase)
    return phrases



# @adapt_struct
# def extract_noun_phrase(struct: dict = None,
#                         multi_token_only=True,
#                         pretty=False,
#                         with_describer: bool = True, ## 如果with_desciber 为true, pretty 必须=False
#                         ):
#
#     if not with_describer:
#         noun_phrases = extract_phrase(struct=struct, multi_token_only = multi_token_only, pretty= pretty,
#                                       valid_postags={"NN", "NR", "NT", "LOC", "DT", "JJ", "CTY","OD","DTA",
#                                                      "CC"  ## "和"
#                                                      },
#                                       invalid_postags={"PU", "M", "VC","VV", "VE" ,"DEG", "DEV", "DER", "AS", "SP","P"},
#                                       valid_rels={'nn', "dobj", "dep","range","amod","cc"},
#                                       rm_one_char=False,
#                                       )
#         return noun_phrases
#     else:
#         ## 抽取带有修饰性的名词
#         noun_phrases = extract_noun_phrase(struct = struct, multi_token_only=False, pretty=False, with_describer=False)
#         describer_phrases = extract_all_describer_phrase(struct = struct, pretty=False)
#
#         describer_phrases = [p for p in describer_phrases if sum(
#             [(p[-1]["index"]+1 == np[0]["index"]
#               ) for np in noun_phrases])==1]  ## 只考虑修饰词后紧跟名词短语的情况
#
#         phrases = noun_phrases + describer_phrases
#         phrases = deduple_phrases(phrases)
#         phrases = concat_consecutive_phrases(phrases)
#         if multi_token_only:
#             phrases = [p for p in phrases if len(p) > 1]
#
#         if pretty:
#             phrases = [prettify(p) for p in phrases]
#         return phrases


@adapt_struct
def extract_noun_phrase(struct: dict = None,
                        multi_token_only=True,
                        pretty=False,
                        with_describer: bool = True, ## 如果with_desciber 为true, pretty 必须=False
                        rm_one_char:bool = True
                        ):
    valid_noun_rels = {"nsubj","dobj","top","attr","nn","pobj"}
    valid_noun_describe_rels = {"amod","rcmod","cpm","nummod","clf"}
    invalid_rels = {"punct","prep"}
    if not with_describer:
        phrase= extract_phrase_by_rel(struct=struct,
                                   valid_rel_set=valid_noun_rels ,
                                   pretty = pretty,
                                   recursive_invalid_rels=invalid_rels|valid_noun_describe_rels,rm_one_char=rm_one_char)
    else:
        phrase = extract_phrase_by_rel(struct=struct,
                                       valid_rel_set=valid_noun_rels|valid_noun_describe_rels,
                                       pretty=pretty,
                                       recursive_invalid_rels=invalid_rels, rm_one_char=rm_one_char)
    return phrase

def recursively_get_path(rel_map,
                         source_indexes = set(),
                         covered_indexes = set(),
                         valid_rels = set(),
                         invalid_rels = set()):
    new_source_indexes = set()
    for si in source_indexes:
        covered_indexes.add(si)
        if si in rel_map:
            si_rels =  rel_map[si]
            if len(valid_rels)>=1:
                si_rels = [r for r in si_rels if r['relationship'] in valid_rels]
            if len(invalid_rels)>=1:
                si_rels = [r for r in si_rels if r['relationship'] not in invalid_rels]
            new_indexes = set([r['targetIndex'] for r in si_rels]) - covered_indexes
            new_source_indexes = new_source_indexes.union(new_indexes)
    if len(new_source_indexes)==0:
        covered_indexes = list(covered_indexes)
        covered_indexes.sort()
        return covered_indexes
    return recursively_get_path(rel_map = rel_map,
                                source_indexes = new_source_indexes,
                                covered_indexes = covered_indexes,
                                valid_rels = valid_rels,
                                invalid_rels = invalid_rels)

@adapt_struct
def extract_phrase_by_rel(struct: dict = None, valid_rel_set = {"assmod"} ,multi_token_only=False, pretty=False,
                             rm_one_char=False, recursive_valid_rels:set = set(), recursive_invalid_rels:set = set()):
    rel_map = _get_rel_map(struct=struct)
    tokens = struct['tokens']
    rels = struct['dependencyRelationships']
    modifier_rels = [rel for rel in rels if rel['relationship'] in valid_rel_set]
    mod_core_token_indexes = [rel['targetIndex'] for rel in modifier_rels]

    # print(mod_core_token_indexes)

    phrases = []

    for mod_core_token_index in set(mod_core_token_indexes):
        phrase_indexes = recursively_get_path(rel_map, 
                                              source_indexes=set([mod_core_token_index]),
                                              covered_indexes=set(),      ## 在recursion中track 命中的 token index
                                              invalid_rels=recursive_invalid_rels,
                                              valid_rels=recursive_valid_rels)
        phrases.append([tokens[i - 1] for i in phrase_indexes])

    # phrases = deduple_phrases(phrases)
    phrases = concat_consecutive_phrases(phrases)

    if multi_token_only:
        phrases = [p for p in phrases if len(p) > 1]

    if rm_one_char:
        phrases = [p for p in phrases if not (len(p)==1 and len(p[0]['token'])==1)]

    if pretty:
        phrases = [prettify(p) for p in phrases]
    return phrases

@adapt_struct
def extract_all_describer_phrase(struct: dict = None, multi_token_only=False, pretty=False, rm_one_char=True):
    rel_map = _get_rel_map(struct=struct)
    tokens = struct['tokens'];
    rels = struct['dependencyRelationships']

    valid_describer_rels =   {"dep","dvpm","dvpmod"}
    invalid_describer_rels = {"punct","prep","nsubj","nn","attr"}

    modifier_rels = [rel for rel in rels if "mod" in rel['relationship'] or rel['relationship'] in valid_describer_rels]
    mod_core_token_indexes = [rel['targetIndex'] for rel in modifier_rels]
    phrases = []

    # print(mod_core_token_indexes)

    for mod_core_token_index in set(mod_core_token_indexes):
        phrase_indexes = recursively_get_path(rel_map,set([mod_core_token_index]),set(),
                                              invalid_rels=invalid_describer_rels,
                                              )
        phrases.append([tokens[i-1] for i in phrase_indexes])

    # print(" --- candidate desciber phrases: ", [prettify(p) for p in phrases])

    phrases = deduple_phrases(phrases)

    # print(" --- candidate desciber phrases: ", [prettify(p) for p in phrases])


    phrases = concat_consecutive_phrases(phrases)


    if multi_token_only:
        phrases = [p for p in phrases if len(p)>1]

    if pretty:
        phrases = [prettify(p) for p in phrases]
    return phrases

    # print("all modifier rels: ",mod_core_token_index)

@adapt_struct
def extract_prep_describer_phrase(struct: dict = None, multi_token_only=True, pretty=False,
                             rm_one_char: bool = True):
    return extract_phrase_by_rel(struct=struct,pretty=pretty,
                                 multi_token_only=multi_token_only,
                                 valid_rel_set={"prep"},
                                 recursive_invalid_rels={"punct"},
                                 rm_one_char=rm_one_char)


# todo:
## 抽取: 宁德时代正式成为宝马集团在大中华地区唯一的电池供应商; "名词"与"的"组成的修饰短语



@adapt_struct
def get_dp_rel(struct:dict=None,rel:str="nsubj"):
    tokens = struct['tokens']
    dprelations = struct['dependencyRelationships']
    target_tokens = []
    for dprel in dprelations:
        if dprel['relationship']==rel:
            target_index = dprel['targetIndex']
            tokens[target_index-1]['index'] = target_index
            target_tokens.append(tokens[target_index-1])
    return target_tokens


@options
@adapt_struct
def extract_verb_phrase(struct:dict=None,
                        with_describer:bool=True
                        ):
    rels = struct["dependencyRelationships"]
    valid_verb_postags = {"VV", "VC", "VE"}
    # verb_connected_relationships = {'nsubj', 'dobj', "top", "range", 'attr', "prep"}  ## 谓语可以连接向外的依存关系

    v_valid_rels = {"root","ccomp","conj","mmod"}
    v_describe_rels = {"advmod","rcomp","dep"}

    if with_describer:
        verb_phrases = extract_phrase_by_rel(struct=struct, pretty=False, multi_token_only=False,
                                    valid_rel_set={"root"}, rm_one_char=False, recursive_valid_rels=v_valid_rels|v_describe_rels)
    else:
        verb_phrases = extract_phrase_by_rel(struct=struct, pretty=False, multi_token_only=False,
                                                valid_rel_set={"root"}, rm_one_char=False,
                                                recursive_valid_rels=v_valid_rels)
    verb_phrases = [phrase for phrase in verb_phrases if sum([token['postag'] in valid_verb_postags for token in phrase])>=0]
    ## 对于和主要verb不仅靠的verb作为从句补充ccomp, 过滤掉他们作为动词的可能
    verb_phrases = [phrase for phrase in verb_phrases if {"root", "conj"}.intersection(set([rel['relationship'] for rel in rels if rel['targetIndex'] in [t['index'] for t in phrase]])) ]
    return verb_phrases


@options
@adapt_struct
def extract_num_phrase(struct: dict = None,
                        multi_token_only=False,
                       pretty:bool = False,
                        rm_one_char=True,  ## need to impliment
                        ):

    phrases = extract_phrase_by_rel(struct = struct,
                                   valid_rel_set={"nummod","range"},
                                   multi_token_only=multi_token_only,
                                   rm_one_char= True,
                                   pretty = False)
    for i in range(len(phrases)):
        if phrases[i][0]['postag']=="PU":
            phrases[i] = phrases[i][1:]
        if phrases[i][-1]['postag'] == "PU":
            phrases[i] = phrases[i][:-1]
    phrases = [p for p in phrases if len(p)>=1]
    return phrases