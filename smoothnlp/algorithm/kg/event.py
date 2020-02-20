from ...nlp import nlp
from .entity import extract_subject,extract_object,extract_prep_describer_phrase
from .phrase import extract_verb_phrase,phrase_index_range,extract_noun_phrase,extract_all_describer_phrase,_get_rel_map,extract_num_phrase
from .phrase import _split_conj_sents,_find_phrase_connected_rel
from .phrase import recursively_get_path
from copy import deepcopy
from .helper import *


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
    events = []
    rels = struct['dependencyRelationships']
    rel_map = _get_rel_map(struct)
    verbs = extract_verb_phrase(struct, pretty=False,with_describer=True)
    subject_candidates = extract_subject(struct=struct, pretty=False)

    ## todo: 解决从句的知识抽取:
    #  如: "创胜集团共同创始人和执行董事长赵奕宁博士表示：“这是创胜集团合并后的首次成功融资，特别感谢各位新投资者对我们的信任。"
    # "合并"作为ccomp 不可指向主语

    recursively_get_path(rel_map)

    def get_subject4vphrase(vphrase,subject_candidates,rel_map):
        v_rels = _find_phrase_connected_rel(vphrase, rel_map)
        v_rels = [rel for rel in v_rels if rel['relationship'] in valid_subject_rel]

        # print("vpharase:", prettify(vphrase),[vrel['targetIndex'] for vrel in v_rels])

        subjects = []

        for subject_candidate in subject_candidates:  ## loop 每一个主语
            subject_candidate_indexes = set([t['index'] for t in subject_candidate])
            subject = None
            for rel in v_rels:
                if rel['relationship'] in valid_subject_rel and rel['targetIndex'] in subject_candidate_indexes:
                    subject = subject_candidate
                    break
            if subject is None:
                continue
            subjects.append(subject)
        return subjects

    for vphrase in verbs:  ## loop 每一个动词短语
        subjects = get_subject4vphrase(vphrase,subject_candidates,rel_map)
        if len(subjects)==0:
            v_valid_indexes = set(vtoken['index'] for vtoken in vphrase)
            ## extend 那些由conj连接的动词
            extra_valid_indexes = set(rel['dependentIndex'] for rel in rels if rel['targetIndex'] in v_valid_indexes and rel['relationship'] in {"conj","cc"})
            while len(extra_valid_indexes)>=1 and set(extra_valid_indexes)-set(v_valid_indexes):
                v_valid_indexes = v_valid_indexes|extra_valid_indexes
                extra_valid_indexes = set(rel['dependentIndex'] for rel in rels if
                                       rel['targetIndex'] in v_valid_indexes and rel['relationship'] in {"conj", "cc"})

            v_income_indexs = [rel['dependentIndex'] for rel in rels if rel['targetIndex'] in v_valid_indexes
                               and rel['relationship'] in {"conj","cc"}]
            if len(v_income_indexs)==0:
                continue
            v_income_index = v_income_indexs[0]
            for new_vphrase in verbs:
                if v_income_index in [t['index'] for t in new_vphrase]:
                    subjects+=get_subject4vphrase(new_vphrase,subject_candidates,rel_map)
        for subj in subjects:
            events.append({
                "subject": subj,
                "action": vphrase,
            })
    if pretty:
        for event in events:
            event['subject'] = prettify(event['subject'])
            event['action'] = prettify(event['action'])
    return events

@options
@adapt_struct
def extract_obj_event(struct: dict = None,
                      valid_object_rel={},
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

    def add_event(subject,vphrase,object,event_type):
        events.append({
            "subject": subject,
            "action": vphrase,
            "object": object,
            'type': event_type,
        })

    for event_cand in event_candidates:
        # verb_index = event_cand['action']['index']
        subject_candidate_indexes = set([subtoken['index'] for subtoken in event_cand['subject']])
        subject = event_cand['subject']
        vphrase = event_cand['action']
        vphrase_indexes = {vp['index'] for vp in vphrase}
        v_rels = _find_phrase_connected_rel(vphrase,rel_map)

        for object_candidate in object_candidates:
            object_indexes = {t['index'] for t in object_candidate}
            if len(object_indexes & vphrase_indexes) !=0:  ## 如果动词与object重叠, 跳过
                continue
            for rel in v_rels:
                if rel['relationship'] in valid_object_rel and rel[
                    'targetIndex'] not in subject_candidate_indexes and rel['targetIndex'] in object_indexes:

                    object = object_candidate
                    add_event(subject, vphrase, object, event_type)

                    ## 添加与object有并列关系的词
                    conj_rels = _find_phrase_connected_rel(object,rel_map,{"conj"})
                    conj_indexes = {rel['targetIndex'] for rel in conj_rels if rel['targetIndex'] not in object_indexes}

                    for other_obj in object_candidates:
                        other_obj_indexes = set([t['index'] for t in other_obj])
                        if len(conj_indexes & other_obj_indexes)>=1:
                           add_event(subject,vphrase,other_obj,event_type)

                    break
    return events

@adapt_struct
def extend_prep4event( events ,struct:dict = None,pretty:bool = True, event_type = "prep"):
    rel_map = _get_rel_map(struct)
    perp_phrases = extract_prep_describer_phrase(struct = struct, pretty = False)
    for event in events:
        keys = deepcopy(list(event.keys()))
        for k in keys:
            phrase = event[k]
            if not isinstance(phrase,list):
                continue
            phrase_linked_rels_index = set([rel['targetIndex'] for rel in _find_phrase_connected_rel(phrase,rel_map=rel_map,valid_rels={"prep"})])
            for perp_phrase in perp_phrases:
                perp_index_set = set([token['index'] for token in perp_phrase])
                if len(perp_index_set.intersection(phrase_linked_rels_index))>0:
                    event[k+"_prep"] = perp_phrase
    if pretty:
        for event in events:
            for key in event.keys():
                event[key] = prettify(event[key])
    return events

@options
@adapt_struct
def extract_action_event(struct: dict = None):
    return extract_obj_event( struct=struct,
                         valid_object_rel={"dobj"},
                         event_type="action",
                              object_extract_func=lambda struct,pretty: extract_noun_phrase(struct = struct,pretty=False,with_describer=True))

@options
@adapt_struct
def extract_state_event(struct: dict = None):
    return extract_obj_event( struct=struct,
                         valid_object_rel={"attr","dep"},
                         event_type="state" ,
                         object_extract_func= lambda struct,pretty: extract_noun_phrase(struct = struct,pretty=False,with_describer=True))

# @options
# @adapt_struct
# def extract_state_event(struct: dict = None):
#     return extract_obj_event( struct=struct,
#                          valid_object_rel={"dep"},
#                          event_type="state" ,
#                          object_extract_func= lambda struct,pretty: extract_noun_phrase(struct = struct,pretty=False,with_describer=True))




@options
@adapt_struct
def extract_attr_event(struct: dict = None):
    return extract_obj_event( struct=struct,
                         valid_object_rel={"attr","dep"},  ## 是否加入 "dep"?? Good Case: 国产特斯拉Model3宣布降价至29.9万元
                         event_type="attr" ,
                         object_extract_func= lambda struct,pretty: extract_all_describer_phrase(struct = struct))


@options
@adapt_struct
def extract_num_event(struct: dict = None, pretty: bool = False):
    return extract_obj_event( struct=struct,
                         valid_object_rel={"range"},
                         event_type="num",
                         object_extract_func= lambda struct,pretty: extract_num_phrase(struct = struct))

@options
@adapt_struct
def extract_prep_event(struct: dict = None, pretty: bool = False):
    return extract_obj_event( struct=struct,
                         valid_object_rel={"prep"},
                         event_type="prep",
                         object_extract_func= lambda struct,pretty: extract_prep_describer_phrase(struct = struct))



@options
@adapt_struct
def extract_all_event(struct: dict = None,_with_conf_score=True):
    ea = extract_action_event(struct =  struct,
                              _with_conf_score=True,
                            pretty = False)
    es = extract_state_event(struct=struct,_with_conf_score=_with_conf_score)
    en = extract_num_event(struct = struct,_with_conf_score = _with_conf_score)
    e_attr = extract_attr_event(struct=struct,_with_conf_score = _with_conf_score)
    # events = extend_prep4event(struct=struct,events=ea+es+en+e_attr)
    events = ea+es+en+e_attr
    e_prep = extract_prep_event(struct = struct, _with_conf_score = _with_conf_score)
    events += e_prep
    # ep = extract_prep_event(struct = struct, pretty=pretty)
    # et = extract_tmod_event(struct = struct, pretty= pretty)
    return events
