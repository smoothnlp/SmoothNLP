from .phrase import extract_describer_phrase,prettify,_find_phrase_connected_rel,extract_noun_phrase,extract_num_phrase,_get_rel_map,adapt_struct
from .entity import extract_subject

## todo
"""
邯郸市通达机械制造有限公司建于一九八九年，位于河北永年高新技术工业园区 拥有固定资产1200万元，现有职工280名，其中专业技术人员80名，高级工程师两名，年生产能力10000吨，产值8000万元。先进冷镦设备50多台，
--> 抽取对应的标签和数字描述, 如 产值-->8000万
"""

def check_2phrase_connected(phrase1,phrase2,rel_map,valid_rels={}):

    p1_indexes = set([t['index'] for t in phrase1])
    p1_rels_targetIndexes = set([rel['targetIndex'] for rel in _find_phrase_connected_rel(rel_map=rel_map, phrase=phrase1)])
    p2_indexes = set([t['index'] for t in phrase2])
    p2_rels_targetIndexes = set([rel['targetIndex'] for rel in _find_phrase_connected_rel(rel_map=rel_map, phrase=phrase2)])
    if len(p1_indexes.intersection(p2_rels_targetIndexes)) >= 1 or len(
        p2_indexes.intersection(p1_rels_targetIndexes)) >= 1:
        return True
    else:
        return False


@adapt_struct
def extract_attr_num(struct: dict = None, pretty: bool = True):
    rel_map = _get_rel_map(struct)
    attrs = []
    noun_phrases = extract_noun_phrase(struct = struct, pretty = False, multi_token_only=False,with_describer=False)
    num_phrases = extract_num_phrase(struct = struct, pretty = False)
    subject_entities = extract_subject(struct = struct, pretty = False)
    # print(len(subject_entities))
    # print(len(subject_entities[0]))
    # print("subj:", extract_subject(struct = struct, pretty = True))
    # print("subj:", extract_subject(struct=struct, pretty=True))
    # if len(subject_entities)>1:  ## 暂时对多个主语的case不做抽取， 避免一定错误率
    #     return attrs

    for subject in subject_entities:
        for num_phrase in num_phrases:
            for noun_phrase in noun_phrases:
                # print(prettify(num_phrase),prettify(noun_phrase),":",check_2phrase_connected(num_phrase,noun_phrase,rel_map))
                if check_2phrase_connected(num_phrase,noun_phrase,rel_map):
                    ## todo: 检查subject 到 noun—phrase 或者 num-phrase 是否有dp连接， 注意连接可以是动词
                    attrs.append({
                        "subject":subject,
                        "attr":noun_phrase,
                        "value":num_phrase
                    })
    if pretty:
        keys = ['subject','attr',"value"]
        for attr in attrs:
            for k in keys:
                attr[k] = prettify(attr.pop(k))
    return attrs


@adapt_struct
def extract_attr_de(struct: dict = None, pretty: bool = True, attr_type:str = "attr"):
    """
    :param struct:
    :param pretty:
    :param attr_type:
    :return:
    """
    tokens = struct['tokens']
    rel_map = _get_rel_map(struct)

    de = None
    index = 1
    for token in tokens:
        if token['token'] == "的" and token['postag'] in ['DEG',"DEC"]:
            de = token
            de['index'] = index
        index+=1
    if de is None:
        return []

    consecutive_noun = extract_noun_phrase(struct=struct, multi_token_only=False, pretty=True, with_describer=False)
    print("consecutive_noun: ", consecutive_noun)

    noun_phrases = extract_noun_phrase(struct=struct, multi_token_only=False, pretty=True, with_describer=False)
    print("NOUN: ",noun_phrases)


