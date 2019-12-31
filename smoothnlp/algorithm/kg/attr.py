from .phrase import extract_describer_phrase,prettify,extract_noun_phrase,extract_phrase,_get_rel_map,adapt_struct

## todo
"""
邯郸市通达机械制造有限公司建于一九八九年，位于河北永年高新技术工业园区 拥有固定资产1200万元，现有职工280名，其中专业技术人员80名，高级工程师两名，年生产能力10000吨，产值8000万元。先进冷镦设备50多台，
--> 抽取对应的标签和数字描述, 如 产值-->8000万
"""

@adapt_struct
def extract_attr_de(struct: dict = None, pretty: bool = True,
                  attr_type:str = "attr"):
    """

    :param struct:
    :param pretty:
    :param attr_type:
    :return:
    """
    tokens = struct['tokens']
    rel_map = _get_rel_map(struct)

    de = None
    index = 1
    for token in tokens:
        if token['token'] == "的" and token['postag'] in ['DEG',"DEC"]:
            de = token
            de['index'] = index
        index+=1
    if de is None:
        return []

    consecutive_noun = extract_noun_phrase(struct=struct, multi_token_only=False, pretty=True, with_describer=False)
    print("consecutive_noun: ", consecutive_noun)

    noun_phrases = extract_noun_phrase(struct=struct, multi_token_only=False, pretty=True, with_describer=False)
    print("NOUN: ",noun_phrases)


