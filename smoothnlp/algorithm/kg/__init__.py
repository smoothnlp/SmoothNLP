from .phrase import adapt_struct,extract_noun_phrase,extract_verb_phrase,_get_rel_map
from .event import extract_all_event,extract_action_event,extract_state_event
from .entity import extract_subject,extract_object
from .attr import extract_all_attr
from ...nlp import nlp
from ...configurations import config
from functools import wraps
import networkx as nx

@adapt_struct
def smart_split2sentence(struct:dict):
    tokens = struct['tokens']
    rels = struct['dependencyRelationships']
    sents = []
    sent = []
    # print(tokens)
    for i in range(len(tokens)-1):
        index = i+1
        if tokens[i]['postag'] == "PU" and rels[index]['relationship'] in {"conj"}:
            sents.append(sent)
            sent = []
        else:
            sent.append(tokens[i])
    sents.append(sent)
    # sents = ["".join(sent) for sent in sents]
    if len(sents) ==1:
        return struct

    first_sentence = "".join([t['token'] for t in sents[0]])
    print(first_sentence)
    first_struct = nlp.analyze(first_sentence)
    current_subjects = extract_subject(first_struct, pretty=True)
    if len(current_subjects)!=1:
        return struct
    new_structs = [first_struct]
    current_subject = current_subjects[0]
    for sent in sents[1:]:
        sent_first_token = sent[0]
        sent_str = "".join([t['token'] for t in sent])
        if sent_first_token['postag'][0] =="V":
            sent_str  = current_subject + sent_str
        print(sent_str)
        new_structs.append(nlp.analyze(sent_str))
    return new_structs


def adapt_smart_split2sentences(func):
    @wraps(func)
    def smart_concat(*arg,**karg):
        struct = karg.pop('struct')
        structs = smart_split2sentence(struct)
        outputs = [func(struct = struct,*arg,**karg) for struct in structs]
        outputs = [oi for o in outputs for oi in o]
        # outputs += func(struct)
        return outputs
    return smart_concat

@adapt_struct
def extract_all_debug(struct:dict):
    print(" ~~~~~ phrase ~~~~~~~")
    print("noun(no describer): ", extract_noun_phrase(struct=struct, pretty=True, multi_token_only=False, with_describer=False))
    print("noun(with describer): ",
          extract_noun_phrase(struct=struct, pretty=True, multi_token_only=False, with_describer=True))
    # print("describer: ", phrase.extract_mod_describer_phrase(struct = struct, pretty= True))
    # print("vhybrid describer: ", phrase.extract_vhybrid_describer_phrase(struct=struct, pretty=True))
    # print("loc describer: ", phrase.extract_loc_describer_phrase(struct=struct, pretty=True))
    print("prep describer: ",phrase.extract_prep_describer_phrase(struct=struct, pretty=True))
    # print("mod describer: ", phrase.extract_mod_describer_phrase(struct=struct, pretty=True))

    print("all describer: ", phrase.extract_all_describer_phrase(struct=struct, pretty=True))

    print("verb: ", extract_verb_phrase(struct= struct, pretty=True))
    print("num phrase: ",phrase.extract_num_phrase(struct = struct, pretty=True))

    print(" ~~~~~ entity ~~~~~~~")
    print("subject:", entity.extract_subject(struct=struct, pretty=True))
    print("object: ",entity.extract_object(struct=struct,pretty=True))
    print("tmod entity:", entity.extract_tmod_entity(struct, pretty=True))

    print("~~~~~~~ attr ~~~~~~~~")
    print("num attrs: ",attr.extract_attr_num(struct,pretty=True))

    print("~~~~~~~~ event  ~~~~~~~~~")
    print("subject&verb:", event.extract_subj_and_verb(struct=struct, pretty=True))
    # print("prep event: ", event.extract_prep_event(struct=struct, pretty=True))
    # print("tmod event: ", event.extract_tmod_event(struct=struct, pretty=True))
    print("action event: ", event.extract_action_event(struct=struct, pretty=True))
    print("state event: ", event.extract_state_event(struct=struct, pretty=True))
    print("attr event: ", event.extract_attr_event(struct=struct, pretty=True))

    print("~~~~~~~~ all  ~~~~~~~~~")
    print("all: ", extract_all_kg(struct=struct, pretty=True))


@adapt_struct
def extract_all_kg(struct:dict, pretty:bool=True):
    if (struct['dependencyRelationships'] is None):
        config.logger.info(" sentence is too long for knowledge extraction ")
        return []
    events = extract_all_event(struct = struct, pretty = pretty)
    return events

def extract(text):
    if len(text)>=50:
        sents = nlp.split2sentences(text)
    elif isinstance(text,list):
        sents = text
    else:
        sents = [text]
    structs = [nlp.analyze(t) for t in sents]
    # structs = nlp.analyze(sents)
    all_kgs = []
    for struct in structs:
        all_kgs+=extract_all_kg(struct)
    return all_kgs

def rel2graph(rels:list):
    g = nx.DiGraph()
    for rel in rels:
        g.add_node(rel['subject'])
        g.add_node(rel['object'])
        if rel['type'] == "state":
            label = "状态修饰\n({})".format(rel['action'])
        elif rel['type'] == "num":
            label = "数字修饰\n({})".format(rel['action'])
        elif rel['type'] == "prep":
            label = "条件修饰\n({})".format(rel['action'])
        else:
            label = "动作\n({})".format(rel['action'])
        g.add_edge(rel['subject'], rel['object'], label=label, type=rel['type'], action=rel['action'])
    return g

def graph2fig(g,x:int=800,y:int=60):
    import matplotlib.pyplot as plt
    pos = nx.drawing.layout.kamada_kawai_layout(g)
    fig = plt.figure()
    fig.set_figwidth(int(x/fig.dpi))
    fig.set_figheight(int(y/fig.dpi))

    def label_modification(label):
        length = len(label)
        if 0 < length <= 6:
            return label
        elif length <= 12:
            return label[:length // 2] + "\n" + label[length // 2:]
        else:
            return label[:length // 3] + "\n" + label[length // 3:2 * length // 3] + "\n" + label[2 * length // 3:]

    node_labels = {k: label_modification(k) for k in g.nodes}

    nx.draw(g, pos, labels=node_labels,
            with_labels=True,
            node_color="lightblue",
            edge_color="grey",
            node_size=[min(len(n), 6) * 1200 for n in g.nodes],
            alpha=1.0,
            font_color="white",
            font_size=14,
            width=3.0,
            font_family="SimHei")

    nx.draw_networkx_edge_labels(g,
                                 pos,
                                 edge_labels=nx.get_edge_attributes(g, "label"),
                                 font_color='Black',
                                 font_size=16,
                                 font_family="SimHei")
    return fig


struct2tokens = lambda struct: [t['token'] for t in struct['tokens']]

@adapt_struct
def extract_related(struct,ners= {"GS","COMPANY_REGISTR"},keywords=[],pretty=True):
    all_kgs = extract_all_kg(struct = struct,pretty=False)
    entities = struct['entities']
    keywords = set(keywords)
    tokens = struct['tokens']
    keyword_index = set([t['index'] for t in tokens if t['token'] in keywords])
    related_indexes = set([int(index) for ent in entities for index in ent['sTokenList'] if ent['nerTag'] in ners])
    related_indexes=keyword_index.union(related_indexes)
    related_kgs = []
    for kg_piece in all_kgs:
        val = kg_piece['subject']
        # for val in kg_piece.values():
        val_indexes = set([v['index'] for v in val])
        if len(val_indexes.intersection(related_indexes))>0:
            if pretty:
                for k in kg_piece.keys():
                    kg_piece[k] = phrase.prettify(kg_piece[k])
            related_kgs.append(kg_piece)
            break
    return related_kgs


@adapt_struct
def get_paths(struct:dict=None):
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