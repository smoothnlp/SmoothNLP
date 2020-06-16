from ...server import nlp
import networkx as nx
from ...server import _request


def extract_ngram(text,with_node_type:bool = True):
    if isinstance(text,str):
        sents = nlp.split2sentences(text)
    elif isinstance(text,list):
        sents = text
    else:
        raise TypeError(" Unsupported type for text parameter: {}".format(text))
    ngrams = []
    for sent in sents:
        ngrams += _request(sent, path="/kg/parsengram", other_params={'node_type': with_node_type})
    return ngrams

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

   
def shortest_path_length(G):
    from heapq import heappush, heappop
    def _dijkstra(G, node):
        """使用dijkstra算法计算 node 与 图中其它节点 之间的最短路径"""
        G_succ = {k: list(G._adj[k].keys()) for k in G._adj.keys()}
        for u,v in G.edges():
            G_succ[v] += [u]  # 保存全部邻接关系
        push = heappush
        pop = heappop
        dist = {}  # dictionary of final distances
        seen = {}
        fringe = []
        if node not in G:
            raise nx.NodeNotFound(f"Node {node} not in G")
        seen[node] = 0
        push(fringe, (0, node))
        while fringe:
            (d, v) = pop(fringe)
            if v in dist:
                continue  # already searched this node.
            dist[v] = d
            for u in G_succ[v]:
                vu_dist = dist[v] + 1
                if u in dist:
                    if vu_dist < dist[u]:
                        raise ValueError('Contradictory paths found:','negative weights?')
                elif u not in seen or vu_dist < seen[u]:
                    seen[u] = vu_dist
                    push(fringe, (vu_dist, u))
        return dist
    return {n:_dijkstra(G, n) for n in G}   

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
    import pkg_resources
    import matplotlib.pyplot as plt
    import matplotlib.font_manager as font_manager
    
    font_files = [pkg_resources.resource_filename('smoothnlp', 'resources/simhei/simhei.ttf')]
    font_list = font_manager.createFontList(font_files)
    font_manager.fontManager.ttflist.extend(font_list)
    plt.rcParams['font.family'] = "SimHei"
    if len(g)<=0: ## 处理空的Graph
        return
    if len(g)==2:
        pos = nx.drawing.planar_layout(g)
    else:
        dists = shortest_path_length(g)
        pos = nx.drawing.kamada_kawai_layout(g,dist=dists)
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
    edge_labels = {k:label for k,label in nx.get_edge_attributes(g, "label").items()}


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
                                 edge_labels=edge_labels,
                                 font_color='Black',
                                 font_size=16,
                                 font_family="SimHei")
    return fig
