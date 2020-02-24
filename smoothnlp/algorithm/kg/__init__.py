from ...nlp import nlp
import networkx as nx
from ...server import _request

def extract_all_kg(text,pretty:bool = True):
    """
    单一文本的知识图谱抽取
    :param text:
    :param pretty:
    :return:
    """
    kg_result = _request(text,path="/kg/query",other_params={'pretty':pretty})
    return kg_result

def extract(text,
            pretty:bool = True):
    """
    对输入的 text 进行 知识图谱(N元组)抽取
    :param text: 进行知识抽取的文本
        支持格式-1: str, 超过一定长度的text会被自动切句
        支持格式-2: [str], list of str
    :param pretty: 是否对词组结果进行合并, 默认True
        boolean: True/False
    :return: 知识图谱(N-元组)  -- List
        字段:
            subject: 对象1
            object:  对象2
            aciton: 连接关系
            type:   连接类型
            __conf_score: 置信概率


    Example:
        extract("SmoothNLP在V0.3版本中正式推出知识抽取功能")
        >> [{'_conf_score': 0.9187054, 'action': '正式推出', 'object': '知识抽取功能', 'subject': 'SmoothNLP', 'type': 'action'}]

    Todo: 支持nlp.analyze的raw结果作为Input

    """
    if isinstance(text,str):
        sents = nlp.split2sentences(text)
    elif isinstance(text,list):
        sents = text
    else:
        raise TypeError(" Unsupported type for text parameter: {}".format(text))
    all_kgs = []
    sentkgs = extract_all_kg(text = sents, pretty = pretty)
    for sentkg in sentkgs:
        all_kgs+=sentkg
    return all_kgs

def rel2graph(rels:list):
    """
    依据多条知识图谱N元组构建Networkx类型的有向图
    :param rels:
    :return: nx.DiGraph
    """
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

def graph2fig(g,x:int=800,y:int=600):
    """
    用matplotlib对有向图进行可视化
    :param g: nx.DiGraph
    :param x: 像素
    :param y: 像素
    :return:
    """
    if len(g)<=0: ## 处理空的Graph
        return
    import matplotlib.pyplot as plt
    pos = nx.drawing.layout.kamada_kawai_layout(g)
    fig = plt.figure(figsize = (x/100,y/100),dpi=100)
    plt.title('SmoothNLP开源工具生成的知识图谱', fontdict={"fontsize": 14})

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
            node_size=[min(len(n), 5) * 1500 for n in g.nodes],
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
