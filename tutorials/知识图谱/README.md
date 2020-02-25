**smoothnlp.kg.kg** 

> 提供了一个知识图谱建立模块，通过二元组的构建。本函数可以用于不同领域的知识图谱的构建"

## 介绍
* 本项目所用数据来自smoothnlp 的开源金融文本数据集，包含：企业工商信息、金融讯息新闻、专栏资讯、投资机构信息

* 数据集获取方式：  
`git clone https://github.com/smoothnlp/FinancialDatasets.git`

* 函数调用方式: 
```python
from smoothnlp import kg
kg.extract(text, pretty:bool=True)
```
* 参数说明
```python
对输入的 text 进行 知识图谱(N元组)抽取
text: 进行知识抽取的文本
        支持格式-1: str, 超过一定长度的text会被自动切句
        支持格式-2: [str], list of str
pretty: 是否对词组结果进行合并, 默认True
        boolean: True/False
```
* 结果说明
```python
return: 知识图谱(N-元组)  -- List
        字段:
            subject: 对象1
            object:  对象2
            aciton: 连接关系
            type:   连接类型
            __conf_score: 置信概率
```


针对百度百科中的单条公司信息进行知识抽取，展示效果。

* 公司描述信息
 上海培罗蒙西服公司成立于1928年，总部位于上海，是一家西服制作有限公司 
 
* 返回结果
```python
[{'_conf_score': 0.92668116,
  'action': '是',
  'object': '有限公司',
  'subject': '上海培罗蒙西服公司',
  'type': 'state'},
 {'_conf_score': 0.87661725,
  'action': '成立',
  'object': '于1928年',
  'subject': '上海培罗蒙西服公司',
  'type': 'prep'},
 {'_conf_score': 0.77132225,
  'action': '是',
  'object': '于上海',
  'subject': '上海培罗蒙西服公司',
  'type': 'prep'}]


```

-----

* 函数调用方式：
```python
kg.rel2graph(rels:list)
```

* 参数说明
```python
依据输入的多条知识图谱N元组构建Networkx 类型的有向图
```

展示
![demo](.//SmoothNLP_KG_Baike.png)


