from ...nlp import nlp
from .entity import extract_subject,extract_object,extract_tmod_entity,_subject_rels,_object_rels,_num_rels
from .phrase import extract_verb_phrase,phrase_index_range,extract_noun_phrase,prettify,_get_rel_map,adapt_struct
from .phrase import _split_conj_sents,_find_phrase_connected_rel
from copy import deepcopy

@adapt_struct
def extract_subj_and_verb(struct: dict = None,
                   pretty: bool = False,
                   valid_subject_rel={"nsubj", "top"}):
    """
    :param struct:
    :param pretty: 该function为中间件function, 故pretty不使用
    :param valid_subject_rel:
    :return:
    """
    # split_indexes = _split_conj_sents(struct)
    events = []

    ## todo ： 迁移到_get_rel_map， 尚未解决：腾讯进军印度保险市场：花15亿元收购一公司10%股份 --> {'subject': '腾讯', 'action': '花', 'object': '印度保险市场', 'type': 'action'}]
    # struct['dependencyRelationships'] = extend_valid_rel(struct['dependencyRelationships'])

    rel_map = _get_rel_map(struct)
    verbs = extract_verb_phrase(struct, pretty=False)
    subject_candidates = extract_subject(struct=struct, pretty=False)


    for vphrase in verbs:  ## loop 每一个动词短语
        v_rels = _find_phrase_connected_rel(vphrase,rel_map)

        v_rels = [rel for rel in v_rels if rel['relationship'] in valid_subject_rel]

        for subject_candidate in subject_candidates:  ## loop 每一个主语
            subject_candidate_indexes = set([t['index'] for t in subject_candidate])
            subject = None
            for rel in v_rels:
                if rel['relationship'] in valid_subject_rel and rel['targetIndex'] in subject_candidate_indexes:
                    subject = subject_candidate
                    break
            if subject is None:
                continue
            events.append({
                "subject": subject,
                "action": vphrase,
            })
    if pretty:
        for event in events:
            event['subject'] = prettify(event['subject'])
            event['action'] = prettify(event['action'])
    return events

@adapt_struct
def extract_obj_event(struct: dict = None,
                  pretty: bool = True,
                  valid_object_rel={"dobj"},
                  event_type:str = "",
                    object_extract_func = extract_object):
    """
    抽取包含(宾语)的三元组事件
    :param struct:
    :param pretty:
    :param valid_subject_rel:
    :param valid_object_rel:
    :param event_type:
    :return:
    """
    split_indexes = _split_conj_sents(struct)

    events = []
    rel_map = _get_rel_map(struct)
    object_candidates = object_extract_func(struct = struct, pretty=False)

    # object_candidates = extract_noun_phrase(struct = struct,pretty = False,multi_token_only=False,with_describer=True)

    event_candidates = extract_subj_and_verb(struct)

    for event_cand in event_candidates:
        # verb_index = event_cand['action']['index']
        subject_candidate_indexes = set([subtoken['index'] for subtoken in event_cand['subject']])
        subject = event_cand['subject']
        vphrase = event_cand['action']

        v_rels = _find_phrase_connected_rel(vphrase,rel_map)

        for object_candidate in object_candidates:
            object_indexes = set([t['index'] for t in object_candidate])
            for rel in v_rels:
                if rel['relationship'] in valid_object_rel and rel[
                    'targetIndex'] not in subject_candidate_indexes and rel['targetIndex'] in object_indexes:
                    object = object_candidate
                    ## 添加event之前检查是否跨句

                    subj_index = phrase_index_range(subject)[0]
                    obj_index = phrase_index_range(object)[0]

                    ## ~~~~~~~~~  对于跨并列句的情况进行检查 ~~~~~~~~
                    ## ~~~ 如: 中美一阶段协议达成,货币政策空间加大  ~~~

                    ## todo: 如果第二句话有助于的情况下; 否则不检查
                    ## todo: 如果 主语与动词在同一个句子下, 检查, 否则, 不检查
                    violate_split_condition = False
                    # print(" -- subject: ", prettify(subject), " object: ", prettify(object))
                    for i in split_indexes:
                        # print("subject: ",prettify(subject)," object: ",prettify(object))
                        # print("condition",(subj_index < i) != (obj_index < i))
                        if (subj_index < i) != (obj_index < i):
                            violate_split_condition = True
                            break
                    if violate_split_condition:
                        continue
                    ## ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    events.append({
                        "subject": subject,
                        "action": vphrase,
                        "object": object
                    })
                    break
    if pretty:
        for event in events:
            event['subject'] = prettify(event['subject'])
            event['action'] = prettify(event['action'])
            event['object'] = prettify(event['object'])
            event['type'] = event_type
    return events

@adapt_struct
def extract_prep_event(struct:dict = None, pretty:bool = True, event_type = "prep"):
    """
    带有(介词)做谓语修饰的三元组
    :param struct:
    :param pretty:
    :return:
    """
    events = []
    rel_map = _get_rel_map(struct)
    tokens = struct['tokens']
    # object_candidates = extract_object(struct=struct, pretty=False)

    event_candidates = extract_subj_and_verb(struct)

    # all_phrases = extract_all_phrases(struct,pretty=False)

    for event_cand in event_candidates:
        v_rels = _find_phrase_connected_rel(event_cand['action'],rel_map)
        ## 直接介词连接的情况
        prep_tokens = [t for t in v_rels if t['relationship'] in {"prep"}]
        for prel in prep_tokens:
            ptoken_index = prel['targetIndex']
            prep_token = tokens[ptoken_index-1]
            prep_token['index'] = ptoken_index
            if ptoken_index not in rel_map:
                continue
            for ptargetTokens in rel_map[ptoken_index]:
                if ptargetTokens['relationship'] not in {"pobj"}:
                    continue
                event = event_cand.copy()
                ## todo 用 phrase/ner 短语替代
                vmod_token = tokens[ptargetTokens['targetIndex']-1]
                vmod_token['index'] = ptargetTokens['targetIndex']
                event['mod'] = [prep_token,vmod_token]
                events.append(event)
    if pretty:
        for event in events:
            event['subject'] = prettify(event['subject'])
            event['action'] = prettify(event['action'])
            event['mod'] = prettify(event['mod'])
            event['type'] = event_type
    return events

@adapt_struct
def extract_tmod_event(struct: dict = None, pretty: bool = True):
    events = extract_obj_event( struct=struct, pretty=pretty,
                         valid_object_rel={"tmod"},
                         event_type="tmod",
                         object_extract_func = extract_tmod_entity)
    for event in events:
        event['mod'] = event.pop("object")
    return events

@adapt_struct
def extract_action_event(struct: dict = None, pretty: bool = True):
    return extract_obj_event( struct=struct, pretty=pretty,
                         valid_object_rel={"dobj","range"},
                         event_type="action")

@adapt_struct
def extract_state_event(struct: dict = None, pretty: bool = True):
    return extract_obj_event( struct=struct, pretty=pretty,
                         valid_object_rel={"attr","dep"},
                         event_type="state" ,
                              object_extract_func= lambda struct,pretty: extract_noun_phrase(struct = struct,pretty = pretty,with_describer=True) )

@adapt_struct
def extract_all_event( struct: dict = None, pretty: bool = True):
    ea = extract_action_event(struct =  struct,
                                pretty = pretty)
    es = extract_state_event(struct=struct, pretty=pretty)
    ep = extract_prep_event(struct = struct, pretty=pretty)
    et = extract_tmod_event(struct = struct, pretty= pretty)
    return ea+es+ep+et
