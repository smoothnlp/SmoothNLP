from ...nlp import nlp
from .entity import extract_subject,extract_object
from .phrase import extract_all_phrases,extract_verbs,phrase_index_range,extract_noun_phrase,prettify,_get_rel_map,adapt_struct
from .phrase import _split_conj_sents

## Deprecated Function
# @adapt_struct
# def extract_event(struct: dict = None, pretty: bool = True,
#                   valid_subject_rel={"nsubj", "top"},
#                   valid_object_rel={"dobj"},
#                   allow_multiple_verb: bool = True,
#                   event_type:str = ""):
#
#     split_indexes = _split_conj_sents(struct)
#
#     events = []
#     valid_verb_postags = {"VV", "VC"}
#
#     tokens = struct['tokens']
#     rel_map = _get_rel_map(struct)
#     first_index = rel_map[0][0]["targetIndex"]
#     verb_token = tokens[first_index - 1]
#
#     verb_indexes = []    ## 解决多动词句子
#     verb_indexes.append(first_index)
#     valid_verb_flag = False
#     if verb_token['postag'] in valid_verb_postags:
#         valid_verb_flag = True
#
#     while not valid_verb_flag:
#
#         original_index = verb_indexes.copy()
#         for index in verb_indexes:
#             if index not in rel_map:
#                 continue
#             for rel in rel_map[index]:
#                 verb_indexes.append(rel['targetIndex'])
#                 if tokens[rel['targetIndex']-1]['postag'] in valid_verb_postags:
#                     valid_verb_flag = True
#         for i in original_index:
#             verb_indexes.remove(i)
#
#
#     if allow_multiple_verb:
#         for rel in rel_map[first_index]:
#             if rel['relationship'] == 'conj' and tokens[rel['targetIndex'] - 1]['postag'] in valid_verb_postags:
#                 verb_indexes.append(rel['targetIndex'])
#
#
#     noun_phrases = extract_noun_phrase(struct=struct, multi_token_only=False, pretty=False, with_describer=True)
#     subject_candidates = extract_subject(struct=struct, pretty=False)
#     for verb_index in verb_indexes:
#
#         verb_token = tokens[verb_index - 1]
#         verb_token['index'] = verb_index
#
#         if verb_token['postag'] not in valid_verb_postags:
#             continue
#
#         if not subject_candidates:
#             continue
#
#         for subject_candidate in subject_candidates:
#             subject_candidate_indexes = set([t['index'] for t in subject_candidate])
#             subject = None
#             if verb_index not in rel_map:
#                 continue
#             for rel in rel_map[verb_index]:
#                 if rel['relationship'] in valid_subject_rel and rel['targetIndex'] in subject_candidate_indexes:
#                     subject = subject_candidate
#             if subject is None:
#                 continue
#             for noun in noun_phrases:
#                 noun_indexes = set([t['index'] for t in noun])
#                 for rel in rel_map[verb_index]:
#                     if rel['relationship'] in valid_object_rel and rel[
#                         'targetIndex'] not in subject_candidate_indexes and rel['targetIndex'] in noun_indexes:
#
#                         ## 添加event之前检查是否跨句
#
#                         subj_index = phrase_index_range(subject)[0]
#                         obj_index = phrase_index_range(noun)[0]
#
#                         ## ~~~~~~~~~  对于跨并列句的情况进行检查 ~~~~~~~~
#                         ## ~~~ 如: 中美一阶段协议达成,货币政策空间加大  ~~~
#                         violate_split_condition = False
#                         for i in split_indexes:
#                             if (subj_index < i) != (obj_index <i):
#                                 violate_split_condition = True
#                                 break
#                         if violate_split_condition:
#                             continue
#                         ## ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#
#                         events.append({
#                             "subject": subject,
#                             "action": verb_token,
#                             "object": noun
#                         })
#                         break
#
#     prettify = lambda l: "".join([t['token'] for t in l])
#     if pretty:
#         for event in events:
#             event['subject'] = prettify(event['subject'])
#             event['action'] = event['action']['token']
#             event['object'] = prettify(event['object'])
#             event['type'] = event_type
#     return events


# @adapt_struct
# def extract_event(struct: dict = None, pretty: bool = True,
#                   valid_subject_rel={"nsubj", "top"},
#                   valid_object_rel={"dobj"},
#                   event_type:str = ""):
#     """
#     抽取三元组事件
#     :param struct:
#     :param pretty:
#     :param valid_subject_rel:
#     :param valid_object_rel:
#     :param event_type:
#     :return:
#     """
#     split_indexes = _split_conj_sents(struct)
#     events = []
#     rel_map = _get_rel_map(struct)
#     verbs = extract_verbs(struct,pretty=False)
#     subject_candidates = extract_subject(struct=struct, pretty=False)
#     object_candidates = extract_object(struct = struct, pretty=False)
#     for verb in verbs:  ## loop 每一个动词
#         verb_index = verb['index']
#         for subject_candidate in subject_candidates: ## loop 每一个主语
#             subject_candidate_indexes = set([t['index'] for t in subject_candidate])
#             subject = None
#             for rel in rel_map[verb_index]:
#                 if rel['relationship'] in valid_subject_rel and rel['targetIndex'] in subject_candidate_indexes:
#                     subject = subject_candidate
#                     break
#             if subject is None:
#                 continue
#
#             # if len(object_candidates)
#
#             for object_candidate in object_candidates:
#                 object_indexes = set([t['index'] for t in object_candidate])
#                 for rel in rel_map[verb_index]:
#                     if rel['relationship'] in valid_object_rel and rel[
#                         'targetIndex'] not in subject_candidate_indexes and rel['targetIndex'] in object_indexes:
#                         object = object_candidate
#                         ## 添加event之前检查是否跨句
#
#                         subj_index = phrase_index_range(subject)[0]
#                         obj_index = phrase_index_range(object)[0]
#
#                         ## ~~~~~~~~~  对于跨并列句的情况进行检查 ~~~~~~~~
#                         ## ~~~ 如: 中美一阶段协议达成,货币政策空间加大  ~~~
#                         violate_split_condition = False
#                         for i in split_indexes:
#                             if (subj_index < i) != (obj_index < i):
#                                 violate_split_condition = True
#                                 break
#                         if violate_split_condition:
#                             continue
#                         ## ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#                         events.append({
#                             "subject": subject,
#                             "action": verb,
#                             "object": object
#                         })
#                         break
#     if pretty:
#         for event in events:
#             event['subject'] = prettify(event['subject'])
#             event['action'] = event['action']['token']
#             event['object'] = prettify(event['object'])
#             event['type'] = event_type
#     return events


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
    rel_map = _get_rel_map(struct)
    verbs = extract_verbs(struct, pretty=False)
    subject_candidates = extract_subject(struct=struct, pretty=False)

    for verb in verbs:  ## loop 每一个动词
        verb_index = verb['index']
        for subject_candidate in subject_candidates:  ## loop 每一个主语
            subject_candidate_indexes = set([t['index'] for t in subject_candidate])
            subject = None
            for rel in rel_map[verb_index]:
                if rel['relationship'] in valid_subject_rel and rel['targetIndex'] in subject_candidate_indexes:
                    subject = subject_candidate
                    break
            if subject is None:
                continue
            events.append({
                "subject": subject,
                "action": verb,
            })
    return events

@adapt_struct
def extract_obj_event(struct: dict = None,
                  pretty: bool = True,
                  valid_object_rel={"dobj"},
                  event_type:str = ""):
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
    object_candidates = extract_object(struct = struct, pretty=False)

    event_candidates = extract_subj_and_verb(struct)

    for event_cand in event_candidates:
        verb_index = event_cand['action']['index']
        subject_candidate_indexes = set([subtoken['index'] for subtoken in event_cand['subject']])
        subject = event_cand['subject']
        verb = event_cand['action']

        for object_candidate in object_candidates:
            object_indexes = set([t['index'] for t in object_candidate])
            for rel in rel_map[verb_index]:
                if rel['relationship'] in valid_object_rel and rel[
                    'targetIndex'] not in subject_candidate_indexes and rel['targetIndex'] in object_indexes:
                    object = object_candidate
                    ## 添加event之前检查是否跨句

                    subj_index = phrase_index_range(subject)[0]
                    obj_index = phrase_index_range(object)[0]

                    ## ~~~~~~~~~  对于跨并列句的情况进行检查 ~~~~~~~~
                    ## ~~~ 如: 中美一阶段协议达成,货币政策空间加大  ~~~
                    violate_split_condition = False
                    for i in split_indexes:
                        if (subj_index < i) != (obj_index < i):
                            violate_split_condition = True
                            break
                    if violate_split_condition:
                        continue
                    ## ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    events.append({
                        "subject": subject,
                        "action": verb,
                        "object": object
                    })
                    break
    if pretty:
        for event in events:
            event['subject'] = prettify(event['subject'])
            event['action'] = event['action']['token']
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
    object_candidates = extract_object(struct=struct, pretty=False)

    event_candidates = extract_subj_and_verb(struct)

    # all_phrases = extract_all_phrases(struct,pretty=False)

    for event_cand in event_candidates:
        verb_index = event_cand['action']['index']
        prep_tokens = [t for t  in rel_map[verb_index] if t['relationship'] in {"prep"}]
        for prel in prep_tokens:
            ptoken_index = prel['targetIndex']
            prep_token = tokens[ptoken_index-1]
            prep_token['index'] = ptoken_index
            if ptoken_index not in rel_map:
                continue
            for ptargetTokens in rel_map[ptoken_index]:
                event = event_cand.copy()
                ## todo 用 phrase/ner 短语替代
                vmod_token = tokens[ptargetTokens['targetIndex']-1]
                vmod_token['index'] = ptargetTokens['targetIndex']
                event['mod'] = [prep_token,vmod_token]
                events.append(event)
    if pretty:
        for event in events:
            event['subject'] = prettify(event['subject'])
            event['action'] = event['action']['token']
            event['mod'] = prettify(event['mod'])
            event['type'] = event_type
    return events


@adapt_struct
def extract_action_event(struct: dict = None, pretty: bool = True, allow_multiple_verb: bool = True):
    return extract_obj_event( struct=struct, pretty=pretty,
                         valid_object_rel={"dobj","range"},
                         event_type="action")

@adapt_struct
def extract_state_event(struct: dict = None, pretty: bool = True, allow_multiple_verb: bool = True):
    return extract_obj_event( struct=struct, pretty=pretty,
                         valid_object_rel={"attr"},
                         event_type="state")

@adapt_struct
def extract_all_event( struct: dict = None, pretty: bool = True, allow_multiple_verb: bool = True):
    ea = extract_action_event(struct =  struct,
                                pretty = pretty,
                                allow_multiple_verb = allow_multiple_verb)
    es = extract_state_event( struct=struct, pretty=pretty,allow_multiple_verb=allow_multiple_verb)
    ep = extract_prep_event(struct = struct, pretty=pretty)
    return ea+es+ep
