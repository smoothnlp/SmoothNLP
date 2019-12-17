from ...nlp import nlp

from .phrase import extract_describer_phrase,extract_subject,extract_noun_phrase,extract_phrase,_get_rel_map

def extract_event(text: str = None, struct: dict = None, pretty: bool = True,
                  valid_subject_rel={"nsubj", "top"},
                  valid_object_rel={"dobj"},
                  allow_multiple_verb: bool = True,
                  event_type:str = ""):
    events = []
    valid_verb_postags = {"VV", "VC"}
    if struct is None:
        struct = nlp.analyze(text)
    tokens = struct['tokens']
    rel_map = _get_rel_map(struct)
    first_index = rel_map[0][0]["targetIndex"]
    verb_token = tokens[first_index - 1]

    verb_indexes = [first_index]    ## 解决多动词句子
    while len(verb_indexes)<1:
        for index in verb_indexes:
            for rel in rel_map[index]:
                if tokens[rel['targetIndex']-1]['postag'] in valid_verb_postags:
                    verb_indexes.append(rel['targetIndex'])

    if allow_multiple_verb:
        for rel in rel_map[first_index]:
            if rel['relationship'] == 'conj' and tokens[rel['targetIndex'] - 1]['postag'] in valid_verb_postags:
                verb_indexes.append(rel['targetIndex'])


    noun_phrases = extract_noun_phrase(struct=struct, multi_token_only=False, pretty=False, with_describer=True)
    subject_candidates = extract_subject(struct=struct, pretty=False)
    for verb_index in verb_indexes:

        verb_token = tokens[verb_index - 1]
        verb_token['index'] = verb_index

        if verb_token['postag'] not in valid_verb_postags:
            continue

        if not subject_candidates:
            continue

        for subject_candidate in subject_candidates:
            subject_candidate_indexes = set([t['index'] for t in subject_candidate])
            subject = None
            for rel in rel_map[verb_index]:
                if rel['relationship'] in valid_subject_rel and rel['targetIndex'] in subject_candidate_indexes:
                    subject = subject_candidate
            if subject is None:
                continue
            for noun in noun_phrases:
                noun_indexes = set([t['index'] for t in noun])
                for rel in rel_map[verb_index]:
                    if rel['relationship'] in valid_object_rel and rel[
                        'targetIndex'] not in subject_candidate_indexes and rel['targetIndex'] in noun_indexes:
                        events.append({
                            "subject": subject,
                            "action": verb_token,
                            "object": noun
                        })
                        break
    prettify = lambda l: "".join([t['token'] for t in l])
    if pretty:
        for event in events:
            event['subject'] = prettify(event['subject'])
            event['action'] = event['action']['token']
            event['object'] = prettify(event['object'])
            event['type'] = event_type

    return events


def extract_action_event(text: str = None, struct: dict = None, pretty: bool = True, allow_multiple_verb: bool = True):
    return extract_event(text=text, struct=struct, pretty=pretty,
                         valid_subject_rel={"nsubj", "top"},
                         valid_object_rel={"dobj"},
                         allow_multiple_verb=allow_multiple_verb,
                         event_type="action")


def extract_state_event(text: str = None, struct: dict = None, pretty: bool = True, allow_multiple_verb: bool = True):
    return extract_event(text=text, struct=struct, pretty=pretty,
                         valid_subject_rel={"nsubj", "top"},
                         valid_object_rel={"attr"}, allow_multiple_verb=allow_multiple_verb,
                         event_type="state")


def extract_all_event(text: str = None, struct: dict = None, pretty: bool = True, allow_multiple_verb: bool = True):
    return extract_action_event(text, struct, pretty, allow_multiple_verb) + extract_state_event(text, struct, pretty,
                                                                                                 allow_multiple_verb)