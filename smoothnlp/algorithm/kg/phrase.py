from ...nlp import nlp

prettify = lambda l: "".join([t['token'] for t in l])

def _get_rel_map(struct):
    rel_map = {}
    rels = struct['dependencyRelationships']
    for rel in rels:
        if rel['dependentIndex'] in rel_map:
            rel_map[rel['dependentIndex']].append(rel)
        else:
            rel_map[rel['dependentIndex']] = [rel]
    return rel_map


def extract_phrase(text: str = None, struct: dict = None,
                   multi_token_only=True,
                   pretty=False,
                   valid_postags={},
                   invalid_postags={},
                   valid_rels={},
                   rm_one_char: bool = True, ):
    if struct is None:
        struct = nlp.analyze(text)
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

        #         print(index)
        #         print(valid_postag_condition,invalid_postag_condition)
        #         print(reverse_rel_condition1,reverse_rel_condition2)
        #         print(direct_rel_condition1,direct_rel_condition2)

        token['index'] = index
        if (valid_postag_condition
            or reverse_rel_condition1 or reverse_rel_condition2
            or direct_rel_condition1 or direct_rel_condition2
        ) and invalid_postag_condition:
            if not phrase:
                phrase = [token]
            else:
                phrase.append(token)
            continue
        else:
            if phrase:
                if (multi_token_only and len(phrase) > 1) or not multi_token_only:
                    phrases.append(phrase)
                phrase = []
    if phrase:
        if (multi_token_only and len(phrase) > 1) or not multi_token_only:
            phrases.append(phrase)

    if rm_one_char:
        phrases = [phrase for phrase in phrases if len(phrase) > 1 or len(phrase[0]['token']) > 1]

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


prettify = lambda l: "".join([t['token'] for t in l])


# def extract_cc_phrase(text:str=None,struct:dict=None,multi_token_only = True, pretty = False):
#     return extract_phrase(text,struct,multi_token_only,pretty,valid_postags = {"AD","DEG","DEV","DER","AS","SP","ETC","MSP","LOC"},
#                         invalid_postags = {"NR","VC","M","VV","VE"},
#                         valid_rels = {"cc","ccomp"},
#                          rm_one_char = True)

def extract_noun_phrase(text: str = None,
                        struct: dict = None,
                        multi_token_only=True,
                        pretty=False,
                        with_describer: bool = True):  ## 如果with_desciber 为true, pretty 必须=False

    if not with_describer:
        noun_phrases = extract_phrase(text, struct, multi_token_only, pretty,
                                      valid_postags={"NN", "NR", "LOC", "DT", "JJ", "CTY"},
                                      invalid_postags={"PU", "CD", "M", "VV", "VC", "DEG", "DEV", "DER", "AS", "SP"},
                                      valid_rels={'nn', "dobj", "dep"})
        return noun_phrases
    else:
        noun_phrases = extract_noun_phrase(text, struct, multi_token_only=False, pretty=False, with_describer=False)
        noun_phrases_indexes = set([token['index'] for p in noun_phrases for token in p if len(p) > 1])
        describer_phrases = extract_describer_phrase(text, struct, multi_token_only=False, pretty=False,
                                                     rm_one_char=False)

        #         cc_phrases = extract_cc_phrase(text,struct,multi_token_only=False,pretty=False)
        #         describer_phrases = concat_consecutive_phrases(describer_phrases+cc_phrases)
        describer_phrases = deduple_phrases(describer_phrases)

        describer_phrases = [p for p in describer_phrases if sum([(token['index'] in noun_phrases_indexes) for token in
                                                                  p]) == 0]  ## 对 describe_phrase 在 noun_phrase中出现的部分进行去重
        phrases = noun_phrases + describer_phrases
        phrases = deduple_phrases(phrases)
        phrases = concat_consecutive_phrases(phrases)
        if multi_token_only:
            phrases = [p for p in phrases if len(p) > 1]

        if pretty:
            phrases = [prettify(p) for p in phrases]
        return phrases


def extract_describer_phrase(text: str = None, struct: dict = None, multi_token_only=True, pretty=False,
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
    return extract_phrase(text,struct,multi_token_only,pretty,valid_postags = {"DEC","DEV","DER","SP","ETC","MSP","LOC"},
                        invalid_postags = {"NR","VC","M","VV","VE","NN","JJ","CD"},
                        valid_rels = {'dep',"advmod","attr","neg","amod","dobj","cpm"},
                         rm_one_char = rm_one_char)

def get_dp_rel(text:str=None,struct:dict=None,rel:str="nsubj"):
    if struct is None:
        struct = nlp.analyze(text)
    tokens = struct['tokens']
    dprelations = struct['dependencyRelationships']
    target_tokens = []
    for dprel in dprelations:
        if dprel['relationship']==rel:
            target_index = dprel['targetIndex']
            tokens[target_index-1]['index'] = target_index
            target_tokens.append(tokens[target_index-1])
    return target_tokens

def extract_subject(text:str=None,struct:dict=None,pretty:bool = True):
    """
    返回一段句子中的主语
    :param text:
    :param struct:
    :param pretty:
    :return:
    """
    if struct is None:
        struct = nlp.analyze(text)
    phrases = extract_noun_phrase(struct=struct,pretty=False,multi_token_only=False,with_describer=False)
    subject_tokens = get_dp_rel(struct=struct,rel = "nsubj")+get_dp_rel(struct=struct,rel = "top")
    subject_phrase = list()
    added_phrase_index = set()
    for index in [t['index'] for t in subject_tokens]:
        for j in range(len(phrases)):
            phrase = phrases[j]
            if index in [t['index'] for t in phrase] and j not in added_phrase_index:
                added_phrase_index.add(j)
                if pretty:
                    subject_phrase.append("".join([p['token'] for p in phrase]))
                else:
                    subject_phrase.append(phrase)
    return  subject_phrase