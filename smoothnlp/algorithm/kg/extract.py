from ...nlp import nlp

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
                   valid_postags={"NN", "NR", "LOC", "DT", "JJ"},
                   invalid_postags={"PU", "CD", "M", "VV"},
                   valid_rels={'nn', "dobj", "dep"},
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
                                                                    rel['relationship'] in valid_rels]
        reverse_rel_condition2 = index + 1 in rel_map and index in [rel["targetIndex"] for rel in rel_map[index + 1] if
                                                                    rel['relationship'] in valid_rels]
        direct_rel_condition1 = index in rel_map and index + 1 in [rel["targetIndex"] for rel in rel_map[index] if
                                                                   rel['relationship'] in valid_rels]
        direct_rel_condition2 = index - 1 in rel_map and index in [rel["targetIndex"] for rel in rel_map[index - 1] if
                                                                   rel['relationship'] in valid_rels]

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


def extract_noun_phrase(text: str = None, struct: dict = None, multi_token_only=True, pretty=False):
    return extract_phrase(text, struct, multi_token_only, pretty, valid_postags={"NN", "NR", "LOC", "DT", "JJ"},
                          invalid_postags={"PU", "CD", "M", "VV"},
                          valid_rels={'nn', "dobj", "dep"})


def extract_describer_phrase(text: str = None, struct: dict = None, multi_token_only=True, pretty=False):
    return extract_phrase(text, struct, multi_token_only, pretty,
                          valid_postags={"DEC", "AD", "DEG", "DEV", "DER", "AS", "SP", "ETC", "MSP", "LOC"},
                          invalid_postags={"NR", "VC", "M"},
                          valid_rels={'dep', "advmod", "dobj", "attr", "neg", "amod"})


def get_dp_rel(text:str=None,struct:dict=None,rel:str="nsubj"):
    """
    返回所有dependency relationship in a map, key 是depedent index
    :param text:
    :param struct:
    :param rel:
    :return:
    """
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
    phrases = extract_noun_phrase(struct=struct,pretty=False,multi_token_only=False)
    subject_tokens = get_dp_rel(struct=struct,rel = "nsubj")+get_dp_rel(struct=struct,rel = "top")
    subject_phrase = set()
    for index in [t['index'] for t in subject_tokens]:
        for phrase in phrases:
            if index in [t['index'] for t in phrase]:
                subject_phrase.add("".join([p['token'] for p in phrase]))
    return  subject_phrase