import pytreebank as pytreebank
from stanfordcorenlp import StanfordCoreNLP
import json
import re

global __nlp__

def initNLP(host="127.0.0.1",port=9000,lang="zh"):
    global __nlp__
    __nlp__ = StanfordCoreNLP(host, port=port,lang=lang)

def lines2labeled_records(lines:list):
    '''

    :param lines:
    :return:
    '''
    def process_stree(stree):
        stree = stree.replace('\n','')
        stree = re.sub('\|prob=\d.\d+','',stree)
        stree = re.sub('[A-Z]+\|sentiment=','',stree)
        return stree
    train = []
    for line in lines:
        line = line.strip('\n')
        prop_senti = {'annotators': 'tokenize, sentiment','pipelineLanguage':'zh', 'outputFormat':'json'}
        sent_result = json.loads(__nlp__.annotate(line, properties=prop_senti))
        stree = sent_result['sentences'][0]['sentimentTree']
        stree = process_stree(stree)
        t = pytreebank.create_tree_from_string(stree)
        lines_transfered = t.to_labeled_lines()
        train.append(lines_transfered)
    return train

def labeled_record2lines(label_record):
    return "\n".join([" ".join([str(r) for r in i]) for i in label_record if i[0]!=None])


def lines2labeled_lines(lines:list,
                        outputFile = None):
    labeledrecords = lines2labeled_records(lines)
    labeled_trees = [labeled_record2lines(r) for r in labeledrecords]
    if outputFile:
        with open(outputFile,'w') as f:
            for l in labeled_trees:
                f.write(l)
                f.write("\n")
                f.write("\n")
    return labeled_trees

def binarize_labeled_lines(lines):
    if isinstance(lines,str):
        lines = lines.split("\n")
    if len(lines)==1:
        return lines[0]
    if not lines:
        return
    parent_line = lines[0]
    parent_content = parent_line[1]
    left_line_start = lines[1]
    left_child_content = left_line_start[1]
    right_child_content = parent_content.replace(left_child_content+' ','')
    for i,l in enumerate(lines):
        if l[1] == right_child_content:
            right_child_index = i
            break
    left_lines = lines[1:right_child_index]
    right_lines = lines[right_child_index:]
    return (parent_line[0], binarize_labeled_lines(left_lines), binarize_labeled_lines(right_lines))

tupleTree2strTree = lambda tree_tuple: repr(tree_tuple).replace(',','').replace("'",'')

def lines2pytreebanks_json(lines):
    labeled_lines = lines2labeled_lines(lines)
    tuple_trees = [binarize_labeled_lines(l) for l in labeled_lines]
    strees = [tupleTree2strTree(t) for t in tuple_trees]
    pytbanks = [pytreebank.create_tree_from_string(st) for st in strees]
    pytbanks_json = [t.to_json() for t in pytbanks]
    return pytbanks_json


if __name__=="__main__":
    print("hello, I am treebanks")
    initNLP("http://192.168.9.179",port=9000)
    lines = ['今天天气不错', "我心情也不错"]
    records = (lines2labeled_records(lines))
    print(records)
    line_chunks = lines2labeled_lines(lines)
    print(line_chunks[0])
    lines2labeled_lines(lines,"examples/sample_out.txt")

    # print(repr(binarize_labeled_lines(records[0])))
    # print(lines2pytreebanks_json(["你好呀",'今天天气不错']))
