from .phrase import prettify,_find_phrase_connected_rel,extract_noun_phrase,extract_num_phrase,_get_rel_map,adapt_struct
from .entity import extract_subject

def check_2phrase_connected(phrase1,phrase2,rel_map,valid_rels={}):

    p1_indexes = set([t['index'] for t in phrase1])
    p1_rels_targetIndexes = set([rel['targetIndex'] for rel in _find_phrase_connected_rel(rel_map=rel_map, phrase=phrase1)])
    p2_indexes = set([t['index'] for t in phrase2])
    p2_rels_targetIndexes = set([rel['targetIndex'] for rel in _find_phrase_connected_rel(rel_map=rel_map, phrase=phrase2)])
    distance = min([abs(min(p1_indexes) - max(p2_indexes)), abs(min(p2_indexes) - max(p1_indexes))])
    if len(p1_indexes.intersection(p2_rels_targetIndexes)) >= 1 or len(
        p2_indexes.intersection(p1_rels_targetIndexes)) >= 1 \
            or distance ==1:  ## 如果两个phrase紧挨或者有依存关联
        return True,distance
    else:
        return False,-1

@adapt_struct
def extract_attr_num(struct: dict = None, pretty: bool = True):
    rel_map = _get_rel_map(struct)
    attrs = []
    noun_phrases = extract_noun_phrase(struct = struct, pretty = False, multi_token_only=False,with_describer=False)
    num_phrases = extract_num_phrase(struct = struct, pretty = False)
    subject_entities = extract_subject(struct = struct, pretty = False)
    for subject in subject_entities:
        for num_phrase in num_phrases:
            min_distance = 9999
            for noun_phrase in noun_phrases:
                num_noun_flag,distance = check_2phrase_connected(num_phrase,noun_phrase,rel_map)
                if num_noun_flag and distance < min_distance:
                    ## todo: 检查subject 到 noun—phrase 或者 num-phrase 是否有dp连接， 注意连接可以是动词
                    attr = {
                        "subject":subject,
                        "attr":noun_phrase,
                        "value":num_phrase
                    }
                    min_distance = distance
            if min_distance!=9999:
                attrs.append(attr)
    if pretty:
        keys = ['subject','attr',"value"]
        for attr in attrs:
            for k in keys:
                attr[k] = prettify(attr.pop(k))
    return attrs


@adapt_struct
def extract_all_attr(struct: dict = None, pretty: bool = True):
    num_attrs = extract_attr_num(struct = struct, pretty = pretty)
    return num_attrs


# @adapt_struct
# def extract_attr_de(struct: dict = None, pretty: bool = True, attr_type:str = "attr"):
#     """
#     :param struct:
#     :param pretty:
#     :param attr_type:
#     :return:
#     """
#     tokens = struct['tokens']
#     rel_map = _get_rel_map(struct)
#     de = None
#     index = 1
#     for token in tokens:
#         if token['token'] == "的" and token['postag'] in ['DEG',"DEC"]:
#             de = token
#             de['index'] = index
#         index+=1
#     if de is None:
#         return []
#
#     consecutive_noun = extract_noun_phrase(struct=struct, multi_token_only=False, pretty=True, with_describer=False)
#     print("consecutive_noun: ", consecutive_noun)
#
#     noun_phrases = extract_noun_phrase(struct=struct, multi_token_only=False, pretty=True, with_describer=False)
#     print("NOUN: ",noun_phrases)


