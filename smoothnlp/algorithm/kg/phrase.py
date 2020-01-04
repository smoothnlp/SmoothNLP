from ...nlp import nlp
from functools import wraps
prettify = lambda l: "".join([t['token'] for t in l])
phrase_index_range = lambda l: [t['index'] for t in l]


def _find_phrase_connected_rel(phrase, rel_map):
    rels = []
    for token in phrase:
        if token['index'] in rel_map:
            rels += rel_map[token['index']]
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
    split_indexes = []
    for i in range(1,len(tokens)+1):
        if tokens[i-1]['token'] in {",","，"}:
            for pair in conj_pairs:
                if pair[0]<=i<= pair[1]:
                    split_indexes.append(i)
    return split_indexes;

def _get_rel_map(struct):
    rel_map = {}
    rels = struct['dependencyRelationships']
    for rel in rels:
        if rel['dependentIndex'] in rel_map:
            rel_map[rel['dependentIndex']].append(rel)
        else:
            rel_map[rel['dependentIndex']] = [rel]
    return rel_map

def adapt_struct(func):
    @wraps(func)
    def tostruct(struct:dict = None,text:str = None,*arg,**kargs):
        if "struct" in kargs:
            kargs.pop("struct")
        if isinstance(struct,str):
            return func(struct = nlp.analyze(struct),*arg,**kargs)
        if struct is None or isinstance(text,str):
            return func(struct = nlp.analyze(text),*arg,**kargs)
        else:
            return func(struct = struct,*arg,**kargs)
    return tostruct

# @adapt_struct
# def extract_phrase(struct: dict = None,
#                    multi_token_only=True,
#                    pretty=False,
#                    valid_postags={},
#                    invalid_postags={},
#                    valid_rels={},
#                    rm_one_char: bool = False):
#
#     tokens = struct['tokens']
#
#     rel_map = _get_rel_map(struct)
#
#     phrases = []
#     phrase = []
#     for i in range(len(tokens)):
#         index = i + 1
#         token = tokens[i]
#
#         ## required conditions
#         valid_postag_condition = token["postag"] in valid_postags
#         invalid_postag_condition = token["postag"] not in invalid_postags
#         reverse_rel_condition1 = index in rel_map and index - 1 in [rel["targetIndex"] for rel in rel_map[index] if
#                                                                     rel['relationship'] in valid_rels and rel[
#                                                                         'relationship']]
#         reverse_rel_condition2 = index + 1 in rel_map and index in [rel["targetIndex"] for rel in rel_map[index + 1] if
#                                                                     rel['relationship'] in valid_rels and rel[
#                                                                         'relationship']]
#         direct_rel_condition1 = index in rel_map and index + 1 in [rel["targetIndex"] for rel in rel_map[index] if
#                                                                    rel['relationship'] in valid_rels and rel[
#                                                                        'relationship']]
#         direct_rel_condition2 = index - 1 in rel_map and index in [rel["targetIndex"] for rel in rel_map[index - 1] if
#                                                                    rel['relationship'] in valid_rels and rel[
#                                                                        'relationship']]
#
#         # print(index)
#         # print(valid_postag_condition,invalid_postag_condition)
#         # print(reverse_rel_condition1,reverse_rel_condition2)
#         # print(direct_rel_condition1,direct_rel_condition2)
#
#         token['index'] = index
#
#         rel_condition = reverse_rel_condition1 or reverse_rel_condition2 or direct_rel_condition1 or direct_rel_condition2
#
#         ## 检查 index 与 index-1 的 postag 是否一直
#         # neighbor_postag_same_flag = True
#         # # if i>0:
#         # #     neighbor_postag_same_flag =
#
#         if (valid_postag_condition or rel_condition) and invalid_postag_condition:
#             if not phrase:
#                 phrase = [token]
#             else:
#                 phrase.append(token)
#         else:  ## 不符合条件的情况下
#             if phrase:
#                 if (multi_token_only and len(phrase) > 1) or not multi_token_only:
#                     phrases.append(phrase)
#                 phrase = []
#     if phrase:
#         if (multi_token_only and len(phrase) > 1) or not multi_token_only:
#             phrases.append(phrase)
#     if rm_one_char:
#         phrases = [phrase for phrase in phrases if len(phrase) > 1 or len(phrase[0]['token']) > 1]
#     # phrases = [p for p in phrases if not (len(p)==1 and len(p[0]['token'])==1)]  ## 短语不考虑只有一个token, 且token长度为1的情况
#
#     if not pretty:
#         return phrases
#     else:
#         return ["".join([p['token'] for p in phrase]) for phrase in phrases]


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
    phrases.sort(key=lambda x: x[0]['index'])
    for i in range(len(phrases) - 1):
        if phrases[i][-1]["index"] + 1 == phrases[i + 1][0]["index"]:
            p2 = phrases.pop(i + 1)
            p1 = phrases.pop(i)
            phrases.append(p1 + p2)
            return concat_consecutive_phrases(phrases)
    return phrases


# def extract_cc_phrase(text:str=None,struct:dict=None,multi_token_only = True, pretty = False):
#     return extract_phrase(text,struct,multi_token_only,pretty,valid_postags = {"AD","DEG","DEV","DER","AS","SP","ETC","MSP","LOC"},
#                         invalid_postags = {"NR","VC","M","VV","VE"},
#                         valid_rels = {"cc","ccomp"},
#                          rm_one_char = True)

@adapt_struct
def extract_noun_phrase(struct: dict = None,
                        multi_token_only=True,
                        pretty=False,
                        with_describer: bool = True, ## 如果with_desciber 为true, pretty 必须=False
                        ):

    if not with_describer:
        noun_phrases = extract_phrase(struct=struct, multi_token_only = multi_token_only, pretty= pretty,
                                      valid_postags={"NN", "NR", "NT", "LOC", "DT", "JJ", "CTY","OD","DTA"},
                                      invalid_postags={"PU", "M", "VC","VV", "VE" ,"DEG", "DEV", "DER", "AS", "SP","P"},
                                      valid_rels={'nn', "dobj", "dep","range","amod"},
                                      rm_one_char=False,
                                      )
        return noun_phrases
    else:
        ## 抽取带有修饰性的名词
        noun_phrases = extract_noun_phrase(struct = struct, multi_token_only=False, pretty=False, with_describer=False)
        # noun_phrases_indexes = set([token['index'] for p in noun_phrases for token in p if len(p) > 1])
        # describer_phrases = extract_describer_phrase(struct = struct, multi_token_only=False, pretty=False,
        #                                              rm_one_char=True)
        # describer_phrases += extract_hybrid_describer_phrase(struct=struct, multi_token_only=False, pretty=False,
        #                                              rm_one_char=True)  ## 添加组合型形容词
        #
        # describer_phrases = concat_consecutive_phrases(describer_phrases)
        #
        # #         cc_phrases = extract_cc_phrase(text,struct,multi_token_only=False,pretty=False)
        # #         describer_phrases = concat_consecutive_phrases(describer_phrases+cc_phrases)
        # describer_phrases = deduple_phrases(describer_phrases)
        #
        # describer_phrases = [p for p in describer_phrases if sum(
        #     [(token['index'] in noun_phrases_indexes) for token in p]
        # ) == 0]  ## 对 describe_phrase 在 noun_phrase中出现的部分进行去重

        describer_phrases = extract_all_describer_phrase(struct = struct, pretty=False)

        describer_phrases = [p for p in describer_phrases if sum(
            [(p[-1]["index"]+1 == np[0]["index"]
              ) for np in noun_phrases])==1]  ## 只考虑修饰词后紧跟名词短语的情况

        phrases = noun_phrases + describer_phrases
        phrases = deduple_phrases(phrases)
        phrases = concat_consecutive_phrases(phrases)
        if multi_token_only:
            phrases = [p for p in phrases if len(p) > 1]

        if pretty:
            phrases = [prettify(p) for p in phrases]
        return phrases

def extract_all_describer_phrase(struct: dict = None, multi_token_only=True, pretty=False):
    mod_describers = extract_mod_describer_phrase(struct = struct, multi_token_only=False, pretty=False,
                                                     rm_one_char=False)
    vhybrid_describers = extract_vhybrid_describer_phrase(struct = struct, multi_token_only=False, pretty=False,
                                                     rm_one_char=False)
    loc_describers = extract_loc_describer_phrase(struct = struct, multi_token_only=False, pretty=False,
                                                     rm_one_char=False)
    prep_describers = extract_prep_describer_phrase(struct = struct, multi_token_only=False, pretty=False,
                                                     rm_one_char=False)

    phrases = mod_describers+vhybrid_describers+loc_describers+prep_describers
    phrases = concat_consecutive_phrases(phrases)
    phrases = deduple_phrases(phrases)

    if pretty:
        phrases = [prettify(p) for p in phrases]

    return phrases


## todo:
## 抽取: 宁德时代正式成为宝马集团在大中华地区唯一的电池供应商; "名词"与"的"组成的修饰短语

@adapt_struct
def extract_mod_describer_phrase(struct: dict = None, multi_token_only=True, pretty=False,
                             rm_one_char: bool = True):
    """
    目前主要支持抓取以形容词为代表的描述性短语, 如: "最高的". 对"组合形容词"效果不好, 如: "最具创新力的"
    :param text:
    :param struct:
    :param multi_token_only:
    :param pretty:
    :param rm_one_char:
    :return:
    """
    return extract_phrase(struct = struct,multi_token_only = multi_token_only,pretty = pretty,
                          valid_postags = {"DEC","DEV","DER","SP","ETC","MSP","LOC"},
                        invalid_postags = {"NR","VC","M","VV","VE","NN","JJ","CD"},
                        valid_rels = {'dep',"attr","neg","amod","dobj","cpm"},
                         rm_one_char = rm_one_char)

@adapt_struct
def extract_loc_describer_phrase(struct: dict = None, multi_token_only=False, pretty=False,
                             rm_one_char: bool = False):
    phrases = extract_phrase(struct=struct, multi_token_only=multi_token_only, pretty=False,
                             valid_postags={"NN","LC"},
                             invalid_postags={"VC", "M", "JJ", "CD"},
                             valid_rels={"loc"},
                             rm_one_char=False)

    noun_phrases = extract_noun_phrase(struct = struct, multi_token_only = False, pretty = False, with_describer=False)
    phrases = concat_consecutive_phrases(phrases+noun_phrases)

    valid_phrases = []
    for p in phrases:
        for i in range(len(p)):
            if p[i]['postag'] == "LC":
                valid_phrases.append(p[:i + 1])
                break
    phrases = valid_phrases

    if pretty:
        phrases = [prettify(p) for p in phrases]
    return phrases

    return phrases


@adapt_struct
def extract_prep_describer_phrase(struct: dict = None, multi_token_only=False, pretty=False,
                             rm_one_char: bool = True):
    phrases = extract_phrase(struct=struct, multi_token_only=multi_token_only, pretty=False,
                             valid_postags={ "NN", "VA","VV", "P","DEG"},
                             invalid_postags={"VC", "M", "JJ", "CD"},
                             valid_rels={'rcmod', "advmod", "dep", "assmod"},
                             rm_one_char=False)

    phrases = concat_consecutive_phrases(phrases)

    # print(" --- vhybrid base phrases: ",[prettify(p) for p in phrases])

    ## todo
    """
    待解决的case: "欧工国际是目前国内最大的、专业的软装配套设计公司。" -> "最大的"
    """

    valid_phrases = []
    for p in phrases:
        for i in range(len(p)):
            if p[i]['postag'] in {"DEG","DEC"} and p[0]['postag'] == "P":
                valid_phrases.append(p[:i + 1])
                break
    phrases = valid_phrases

    if pretty:
        phrases = [prettify(p) for p in phrases]
    return phrases

@adapt_struct
def extract_vhybrid_describer_phrase(struct: dict = None, multi_token_only=False, pretty=False,
                             rm_one_char: bool = True):
    """
    抽取 "最具创新力的" <- 等, 动名词组合形容词短语
    :param struct:
    :param multi_token_only:
    :param pretty:
    :param rm_one_char:
    :return:
    """
    phrases = extract_phrase(struct=struct, multi_token_only=multi_token_only, pretty=False,
                             valid_postags={"AD","DEC","NR","VV","NN","VA","P","DEG"},
                              invalid_postags={"VC","M","CD"},
                              valid_rels={'rcmod', "advmod","dobj","dep","assmod","assm"},
                              rm_one_char=False)

    phrases = concat_consecutive_phrases(phrases)

    # print(" --- vhybrid base phrases: ",[prettify(p) for p in phrases])

    ## todo
    """
    待解决的case: "欧工国际是目前国内最大的、专业的软装配套设计公司。" -> "最大的"
    """

    valid_phrases = []
    for p in phrases:
        for i in range(len(p)):
            if p[i]['postag'] in {"DEC","DEG"}:
                valid_phrases.append(p[:i+1])
                break
    phrases = valid_phrases

    if pretty:
        phrases = [prettify(p) for p in phrases]
    return phrases



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

@adapt_struct
def extract_verb_phrase(struct:dict=None,pretty:bool = True):
    """
    抽取句子中的谓语
    :param struct:
    :param pretty:
    :return:
    """

    processed_index = set()
    valid_verb_postags = {"VV", "VC", "VE"}
    verb_connected_relationships = {'nsubj', 'dobj', "top", "range", 'attr', "prep"}  ## 谓语可以连接向外的依存关系

    # def extend_verb_phrase(index,tokens,rel_map, phrase = [], extend_dprels = {"ccomp"}):
    #     """
    #     对多动词的组合(ccomp)进行动词短语的补充
    #     :param index:
    #     :param tokens:
    #     :param rel_map:
    #     :param phrase:
    #     :param extend_dprels:
    #     :return:
    #     """
    #     if index in rel_map:
    #         index_rels = set([rel['relationship'] for rel in rel_map[index]])
    #     if index in rel_map and len(extend_dprels.intersection(index_rels))>=1:
    #         for rel in [r for r in rel_map[index] if r['relationship'] in extend_dprels]:
    #             # another_token = tokens[rel['targetIndex']-1]
    #             # another_token['index'] = rel['targetIndex']
    #             token = tokens[index - 1]
    #             token['index'] = index
    #             processed_index.add(index)
    #             phrase.append(token)
    #             another_phrase = phrase.copy()
    #             return extend_verb_phrase(rel['targetIndex'],tokens,rel_map,another_phrase,extend_dprels)
    #     else:
    #         token = tokens[index-1]
    #         token['index'] = index
    #         processed_index.add(index)
    #         phrase.append(token)
    #         return phrase


    tokens = struct['tokens']
    rel_map = _get_rel_map(struct)

    verb_candidate_phrases = []

    for i in range(1, len(tokens) + 1):

        token = tokens[i - 1]
        if token['postag'] in valid_verb_postags and i not in processed_index:
            token['index'] = i
            phrase_candidate = [token]  ## 目前仅考虑单一token为动词短语
            # phrase_candidate = extend_verb_phrase(i,tokens,rel_map,[])
            verb_candidate_phrases.append(phrase_candidate)

    ## 对紧挨的动词进行拼接
    verb_candidate_phrases = concat_consecutive_phrases(verb_candidate_phrases)

    verb_phrases = []

    for vphrase in verb_candidate_phrases:
        rels = _find_phrase_connected_rel(vphrase,rel_map)
        for rel in rels:
            if rel['relationship'] in verb_connected_relationships:
                verb_phrases.append(vphrase)
                break

    rels = struct['dependencyRelationships']
    valid_vphrases  = []
    valid_source_rels = {"root", 'cc', 'conj'}
    for phrase in verb_phrases:
        for token in phrase:
            token_rel = rels[token['index'] - 1]
            if token_rel['relationship'] in valid_source_rels:
                valid_vphrases.append(phrase)
                break
    verb_phrases = valid_vphrases

    if pretty:
        verb_phrases = [ prettify(vphrase) for vphrase in verb_phrases]
    return verb_phrases

# def extract_all_phrases(struct:dict = None, pretty:bool = False):
#     noun_phrases = extract_noun_phrase(struct=struct, pretty=pretty, multi_token_only=False, with_describer=True)
#     return noun_phrases

@adapt_struct
def extract_num_phrase(struct: dict = None,
                        multi_token_only=False,
                        pretty=False,
                        rm_one_char=True,  ## need to impliment
                        ):
    num_phrases = extract_phrase(struct=struct, multi_token_only = False, pretty= False,
                                  valid_postags={"CD","M","DTA","OD"},
                                  invalid_postags={},
                                  valid_rels={"range","nummod","dep"}
                                  ,rm_one_char=False)

    num_phrases = concat_consecutive_phrases(num_phrases)
    num_phrases = [p for p in num_phrases if
                   not (len(p) == 1 and p[0]['postag'] == "M")]  ## 去除只有一个token, 且token为"单位"的case

    if rm_one_char:
        num_phrases = [p for p in num_phrases if not(len(p)==1 and len(p[0]['token']) ==1)]

    if pretty:
        num_phrases = [prettify(p) for p in num_phrases]

    return num_phrases