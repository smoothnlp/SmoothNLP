from .phrase import _find_phrase_connected_rel,adapt_struct,extract_prep_describer_phrase,concat_consecutive_phrases,extract_noun_phrase,extract_all_describer_phrase,_get_rel_map,prettify,_split_conj_sents,extract_verb_phrase
from .helper import *


@adapt_struct
def extract_entity(struct:dict=None,pretty:bool = True, valid_rel:set = {}, with_describer : bool = True):
    """
    参考英文中"主谓宾"的语法, 抽取被谓语动词的受动体
    :param struct:
    :param pretty:
    :return:
    """
    verbs = extract_verb_phrase(struct,pretty=False)
    noun_phrases = extract_noun_phrase(struct=struct,pretty=False,multi_token_only=False,with_describer=with_describer)
    rel_map = _get_rel_map(struct)
    split_indexes = _split_conj_sents(struct)

    object_token_index = []

    for vphrase in verbs:
        rels = _find_phrase_connected_rel(vphrase,rel_map)
        rels = [rel for rel in rels if rel['relationship'] in valid_rel]
        for rel in rels:
            violate_split_condition = False
            for i in split_indexes:
                if (rel['dependentIndex'] < i) != (rel['targetIndex'] < i):
                    violate_split_condition = True
                    break
            if violate_split_condition:
                continue
            object_token_index.append(rel['targetIndex'])


    noun_phrases = [p for p in noun_phrases if sum([t['index'] in object_token_index for t in p])>=1]

    # noun_phrases = concat_consecutive_phrases(noun_phrases)

    if pretty:
        noun_phrases = [prettify(p) for p in noun_phrases]

    return noun_phrases

def extract_object(struct:dict=None,pretty:bool = True,with_describer:bool = True):
    return extract_entity(struct = struct,pretty = pretty,
                          valid_rel={"dobj","range","attr"}, with_describer = with_describer)

def extract_subject(struct:dict=None,pretty:bool = True,with_describer:bool = True):
    subject_entities = extract_entity(struct = struct,pretty =False,
                          valid_rel={"nsubj","top"},with_describer = True)
    describer_phrases = extract_prep_describer_phrase(struct = struct, pretty=False)
    describer_indexes = set([token['index'] for phrase in describer_phrases for token in phrase ])
    # print(describer_indexes)
    # print(subject_entities)
    subject_entities = [e for e in subject_entities if sum([t['index'] in describer_indexes for t in e])==0 ]

    if pretty:
        subject_entities = [prettify(p) for p in subject_entities]

    return subject_entities

def extract_tmod_entity(struct:dict=None,pretty:bool = True,with_describer:bool = True):
    return extract_entity(struct = struct,pretty = pretty,
                          valid_rel={"tmod"},with_describer = with_describer)

def extract_num_entity(struct:dict=None,pretty:bool = True,with_describer:bool = True):
    return extract_entity(struct = struct,pretty = pretty,
                          valid_rel={"range","nummod"},with_describer = with_describer)



_object_rels = {"dobj","range","attr"}
_subject_rels = {"nsubj","top"}
_num_rels = {"range","nummod"}


