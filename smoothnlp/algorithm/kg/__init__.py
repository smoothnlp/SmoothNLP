from .phrase import extract_noun_phrase,extract_subject,extract_describer_phrase,_get_rel_map,extract_hybrid_describer_phrase
from .event import extract_noun_phrase,extract_subject,extract_describer_phrase,extract_all_event,extract_action_event,extract_state_event
from ...nlp import nlp


def get_paths(text:str=None,struct:dict=None):
    if struct is None:
        struct = nlp.analyze(text)
    rel_map = _get_rel_map(struct)
    paths = []
    def get_paths_helper(path,index=0):
        if index not in rel_map:
            paths.append(path)
            return
        rels = rel_map[index]
        for rel in rels:
            tem_path = path.copy()
            tem_path.append(rel)
            get_paths_helper(tem_path,rel['targetIndex'])
    get_paths_helper([],0)
    return paths