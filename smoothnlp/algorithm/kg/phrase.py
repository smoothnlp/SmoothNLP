from ...nlp import nlp
from functools import wraps
prettify = lambda l: "".join([t['token'] for t in l])
phrase_index_range = lambda l: [t['index'] for t in l]

def _split_conj_sents(struct:dict = None):
    tokens = struct['tokens']
    rels = struct['dependencyRelationships']

    conj_pairs = [(rel['dependentIndex'],rel['targetIndex']) for rel in rels if rel['relationship'] == 'conj']

    split_indexes = []
    for i in range(1,len(tokens)+1):
        if tokens[i-1]['postag'] == "PU":
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

@adapt_struct
def extract_phrase(struct: dict = None,
                   multi_token_only=True,
                   pretty=False,
                   valid_postags={},
                   invalid_postags={},
                   valid_rels={},
                   rm_one_char: bool = True):

    tokens = struct['tokens']

    rel_map = _get_rel_map(struct)

    phrases = []
    phrase = []
    for i in range(len(tokens)):
        index = i + 1
        token = tokens[i]

        ## required conditions
        valid_postag_condition = token["postag"] in valid_postags
        invalid_postag_condition = token["postag"] not in invalid_postags
        reverse_rel_condition1 = index in rel_map and index - 1 in [rel["targetIndex"] for rel in rel_map[index] if
                                                                    rel['relationship'] in valid_rels and rel[
                                                                        'relationship']]
        reverse_rel_condition2 = index + 1 in rel_map and index in [rel["targetIndex"] for rel in rel_map[index + 1] if
                                                                    rel['relationship'] in valid_rels and rel[
                                                                        'relationship']]
        direct_rel_condition1 = index in rel_map and index + 1 in [rel["targetIndex"] for rel in rel_map[index] if
                                                                   rel['relationship'] in valid_rels and rel[
                                                                       'relationship']]
        direct_rel_condition2 = index - 1 in rel_map and index in [rel["targetIndex"] for rel in rel_map[index - 1] if
                                                                   rel['relationship'] in valid_rels and rel[
                                                                       'relationship']]

        # print(index)
        # print(valid_postag_condition,invalid_postag_condition)
        # print(reverse_rel_condition1,reverse_rel_condition2)
        # print(direct_rel_condition1,direct_rel_condition2)

        token['index'] = index

        rel_condition = reverse_rel_condition1 or reverse_rel_condition2 or direct_rel_condition1 or direct_rel_condition2

        if (valid_postag_condition or rel_condition) and invalid_postag_condition:

            if not phrase:
                phrase = [token]
            else:
                phrase.append(token)

        else:  ## 不符合条件的情况下
            if phrase:
                if (multi_token_only and len(phrase) > 1) or not multi_token_only:
                    phrases.append(phrase)
                phrase = []
    if phrase:
        if (multi_token_only and len(phrase) > 1) or not multi_token_only:
            phrases.append(phrase)
    if rm_one_char:
        phrases = [phrase for phrase in phrases if len(phrase) > 1 or len(phrase[0]['token']) > 1]

    phrases = [p for p in phrases if not (len(p)==1 and len(p[0]['token'])==1)]  ## 短语不考虑只有一个token, 且token长度为1的情况

    if not pretty:
        return phrases
    else:
        return ["".join([p['token'] for p in phrase]) for phrase in phrases]


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
                                      valid_postags={"NN", "NR", "LOC", "DT", "JJ", "CTY","OD"},
                                      invalid_postags={"PU", "M", "VV", "VC", "DEG", "DEV", "DER", "AS", "SP"},
                                      valid_rels={'nn', "dobj", "dep","range"}
                                      )
        return noun_phrases
    else:
        ## 抽取带有修饰性的名词
        noun_phrases = extract_noun_phrase(struct = struct, multi_token_only=False, pretty=False, with_describer=False)
        noun_phrases_indexes = set([token['index'] for p in noun_phrases for token in p if len(p) > 1])
        describer_phrases = extract_describer_phrase(struct = struct, multi_token_only=False, pretty=False,
                                                     rm_one_char=False)
        describer_phrases += extract_hybrid_describer_phrase(struct=struct, multi_token_only=False, pretty=False,
                                                     rm_one_char=False)  ## 添加组合型形容词

        describer_phrases = concat_consecutive_phrases(describer_phrases)

        #         cc_phrases = extract_cc_phrase(text,struct,multi_token_only=False,pretty=False)
        #         describer_phrases = concat_consecutive_phrases(describer_phrases+cc_phrases)
        describer_phrases = deduple_phrases(describer_phrases)

        describer_phrases = [p for p in describer_phrases if sum(
            [(token['index'] in noun_phrases_indexes) for token in p]
        ) == 0]  ## 对 describe_phrase 在 noun_phrase中出现的部分进行去重

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

@adapt_struct
def extract_describer_phrase(struct: dict = None, multi_token_only=True, pretty=False,
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
                        valid_rels = {'dep',"advmod","attr","neg","amod","dobj","cpm"},
                         rm_one_char = rm_one_char)

@adapt_struct
def extract_hybrid_describer_phrase(struct: dict = None, multi_token_only=True, pretty=False,
                             rm_one_char: bool = True):
    phrases = extract_phrase(struct=struct, multi_token_only=multi_token_only, pretty=False,
                             valid_postags={"AD","DEC","NR"},
                              invalid_postags={"VC","VV","M","JJ","CD"},
                              valid_rels={'rcmod', "advmod","dobj"},
                              rm_one_char=rm_one_char)
    phrases = [p for p in phrases if p[-1]['postag']=="DEC"]
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
def extract_verbs(struct:dict=None,pretty:bool = True):
    """
    抽取句子中的谓语
    :param struct:
    :param pretty:
    :return:
    """

    valid_verb_postags = {"VV", "VC", "VE", "VA"}
    verb_connected_relationships = {'nsubj', 'dobj',"top","range",'attr'}  ## 谓语可以连接向外的依存关系

    tokens = struct['tokens']
    rel_map = _get_rel_map(struct)

    verb_candidate_tokens = []

    for i in range(1, len(tokens) + 1):
        token = tokens[i - 1]
        if token['postag'] in valid_verb_postags:
            token['index'] = i
            verb_candidate_tokens.append(token)

    verb_tokens = []
    for vtoken in verb_candidate_tokens:
        if vtoken['index'] not in rel_map:
            continue
        rels = rel_map[vtoken['index']]
        for rel in rels:
            if rel['relationship'] in verb_connected_relationships:  ## 检查该动词是否连接一个主语或者宾语
                verb_tokens.append(vtoken)
                break
    if pretty:
        verb_tokens = [t['token'] for t in verb_tokens]
    return verb_tokens