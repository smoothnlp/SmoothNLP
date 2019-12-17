# [SmoothNLP](http://www.smoothnlp.com)
[![GitHub release](https://img.shields.io/badge/Version-0.3-green.svg)](https://github.com/zhangruinan/SmoothNLP/releases)
[接口服务](https://market.cloud.tencent.com/products/16368)

****	

| Author | Email | 
| ----- | ------ | 
| Victor | zhangruinan@smoothnlp.com |
| Yinjun | yinjun@smoothnlp.com |
| 海蜇 | yuzhe_wang@smoothnlp.com | 

****

<!-- TOC -->

- [SmoothNLP](#smoothnlp)
    - [Install 安装](#install-安装)
    - [NLP基础Pipelines](#nlp基础pipelines)
        - [1. Tokenize分词](#1-tokenize分词)
        - [2. Postag词性标注](#2-postag词性标注)
        - [3. NER 实体识别](#3-ner-实体识别)
        - [4. 金融实体识别](#4-金融实体识别)
        - [5. 数字实体识别](#5-数字实体识别)
        - [6. 金额识别与结构化](#6-金额识别与结构化)
        - [7. 日期描述结构化](#7-日期描述结构化)
        - [8. 切句](#8-切句)
    - [知识图谱](#知识图谱)
        - [1. 事件抽取](#1-事件抽取)
        - [2. 短语抽取](#2-短语抽取)
            - [纯名词短语](#纯名词短语)
            - [带有修饰的名词短语](#带有修饰的名词短语)
            - [主语抽取](#主语抽取)
    - [无监督学习](#无监督学习)
        - [新词挖掘](#新词挖掘)
        - [事件聚类](#事件聚类)
    - [有监督学习](#有监督学习)
        - [(资讯)事件分类](#资讯事件分类)
    - [Java 支持](#java-支持)
        - [说明&常见问题](#说明常见问题)
    - [彩蛋](#彩蛋)

<!-- /TOC -->


## Install 安装
```shell
pip install smoothnlp>=0.2.20
```

## NLP基础Pipelines

### 1.Tokenize分词
```python
>> import smoothnlp 
>> smoothnlp.segment('欢迎在Python中使用SmoothNLP')
['欢迎', '在', 'Python', '中', '使用', 'SmoothNLP']
```

### 2.Postag词性标注
```python
>> smoothnlp.postag('欢迎使用smoothnlp的Python接口')
[{'token': '欢迎', 'postag': 'VV'},
 {'token': '在', 'postag': 'P'},
 {'token': 'Python', 'postag': 'NN'},
 {'token': '中', 'postag': 'LC'},
 {'token': '使用', 'postag': 'VV'},
 {'token': 'SmoothNLP', 'postag': 'NN'}]
```


### 3.NER 实体识别
```python
>> smoothnlp.ner("中国平安2019年度长期服务计划于2019年5月7日至5月14日通过二级市场完成购股" )
[{'charStart': 0, 'charEnd': 4, 'text': '中国平安', 'nerTag': 'COMPANY_NAME', 'sTokenList': {'1': {'token': '中国平安', 'postag': None}}, 'normalizedEntityValue': '中国平安'},
{'charStart': 4, 'charEnd': 9, 'text': '2019年', 'nerTag': 'NUMBER', 'sTokenList': {'2': {'token': '2019年', 'postag': 'CD'}}, 'normalizedEntityValue': '2019年'},
{'charStart': 17, 'charEnd': 26, 'text': '2019年5月7日', 'nerTag': 'DATETIME', 'sTokenList': {'8': {'token': '2019年5月', 'postag': None}, '9': {'token': '7日', 'postag': None}}, 'normalizedEntityValue': '2019年5月7日'},
{'charStart': 27, 'charEnd': 32, 'text': '5月14日', 'nerTag': 'DATETIME', 'sTokenList': {'11': {'token': '5月', 'postag': None}, '12': {'token': '14日', 'postag': None}}, 'normalizedEntityValue': '5月14日'}]
```


### 4. 金融实体识别
```python
>> smoothnlp.company_recognize("旷视科技预计将在今年9月在港IPO")
[{'charStart': 0,
  'charEnd': 4,
  'text': '旷视科技',
  'nerTag': 'COMPANY_NAME',
  'sTokenList': {'1': {'token': '旷视科技', 'postag': None}},
  'normalizedEntityValue': '旷视科技'}]
```


### 5.数字实体识别
```python
>> smoothnlp.number_recognize("百度移动应用的月活跃设备达11亿台")
[{'charStart': 13,
  'charEnd': 16,
  'text': '11亿',
  'nerTag': 'NUMBER',
  'sTokenList': {'9': {'token': '11亿', 'postag': 'm'}},
  'normalizedEntityValue': '1100000000'}]
```

### 6. 金额识别与结构化
```python
>> smoothnlp.money_recognize("百度市值跌破400亿美元")
[{'charStart': 6,
  'charEnd': 12,
  'text': '400亿美元',
  'nerTag': 'MONEY',
  'sTokenList': {'4': {'token': '400亿', 'postag': 'm'},
   '5': {'token': '美元', 'postag': 'M'}},
  'normalizedEntityValue': '$40000000000'}]
```

### 7. 日期描述结构化
```python
>> smoothnlp.parse_date("2018年一季度")
{'startDate': '2018-01-01', 'endDate': '2018-03-31'}
```

### 8. 切句
```python
smoothnlp.split2sentences("句子1!句子2!")
> ['句子1!', '句子2!']
```

## 知识图谱
> 仅支持SmoothNLP `V0.3.0`以后的版本. 
### 1. 事件抽取

**说明** 
目前支持两类动词主导的事件, 分别是`state`,`action`; 具体定义: 
* `action`: 可以在时间片段上完成的的动作, 如: `ryzen3000系列采用了7nm工艺的处理器`
* `state`: 进入(跨时间状态的)的状态, 如: `特斯拉是全球最大的电动汽车制造商`

**示例:** 
```python
from smoothnlp import kg
kg.extract_all_event("虽然存在很多争议,特斯拉依旧是全球最成功的电动汽车制造商之一")
> [{'subject': '特斯拉',
    'action': '是',
    'object': '全球最成功的电动汽车制造商之一',
    'type': 'state'}]
```

```python
kg.extract_all_event("ryzen3000系列采用了7nm工艺的处理器")
> [{'subject': 'ryzen3000系列',
    'action': '采用',
    'object': '7nm工艺的处理器',
    'type': 'action'}]
```

### 2. 短语抽取
#### 纯名词短语
```python
from smoothnlp import kg
kg.extract_noun_phrase(text="首发|全域保险科技平台“南燕保险科技”完成1500万美元B+轮融资，史带投资领投",pretty=True, with_describer=False)
> ['全域保险科技平台', '南燕保险科技']
```

#### 带有修饰的名词短语
```python
extract_noun_phrase(text="特斯拉是全球最大的电动汽车制造商。",multi_token_only = False, pretty = True, with_describer=False)
> ['特斯拉', '全球最大的电动汽车制造商']
```

#### 主语抽取
```python
from smoothnlp import kg
kg.extract_subject("纺织品B2B平台百布完成1亿美金C2轮融资 老虎环球基金领投")
> ['纺织品B2B平台百布']
```

## 无监督学习
### 新词挖掘
[算法介绍](https://zhuanlan.zhihu.com/p/80385615) | [使用说明](https://github.com/smoothnlp/SmoothNLP/tree/master/tutorials/%E6%96%B0%E8%AF%8D%E5%8F%91%E7%8E%B0)

### 事件聚类
该功能我们目前仅支持商业化的解决方案支持, 与线上服务. 详情可联系  business@smoothnlp.com

**效果演示**
```json
[
  {
    "url": "https://36kr.com/p/5167309",
    "title": "Facebook第三次数据泄露，可能导致680万用户私人照片泄露",
    "pub_ts": 1544832000
  },
  {
    "url": "https://www.pencilnews.cn/p/24038.html",
    "title": "热点 | Facebook将因为泄露700万用户个人照片 面临16亿美元罚款",
    "pub_ts": 1544832000
  },
  {
    "url": "https://finance.sina.com.cn/stock/usstock/c/2018-12-15/doc-ihmutuec9334184.shtml",
    "title": "Facebook再曝新数据泄露 6800万用户或受影响",
    "pub_ts": 1544844120
  }
]
```
> 吐槽: 新浪小编数据错误... 夸大事实, 真实情况Facebook并没有泄露6800万张照片

## 有监督学习
### (资讯)事件分类
该功能我们目前仅支持商业化的解决方案支持, 与线上服务. 详情可联系  business@smoothnlp.com; 线上服务支持[API输出]()

**效果**

| 事件名称 | AUC | 
| --- | -- |
| 投资并购 | 0.996 |
| 企业合作 | 0.977 | 
| 董监高管 | 0.982 |
| 营收报导 | 0.994 |
| 企业签约 | 0.993 |
| 商业拓展 | 0.968 |
| 产品报道 | 0.977 |
| 产业政策 | 0.990 |
| 经营不善 | 0.981 |
| 违规约谈 | 0.951 |

-------

参考文献
* [ASER](https://arxiv.org/abs/1905.00270)
* [HanLP](https://github.com/hankcs/hanlp)

----------

## Java 支持
**SmoothNLP**项目的主要功能都在Java中有实现, 打包好的Jar文件会在[Release页面]定期更新, 或者在提供的[maven](https://github.com/smoothnlp/SmoothNLP/tree/master/smoothnlp_maven)项目代码中, 直接编译即可
```
git clone https://github.com/smoothnlp/SmoothNLP.git
cd smoothnlp_maven
mvn clean package
```
编译好的Jar文件会在 `smoothnlp_maven/target/smoothnlp-*.jar`

### 说明&常见问题
1. Python环境下, SmoothNP的所有功能通过公开的微服务方式对外数据. 对于本地支持,我们采用了基于`Jpype`的方案. 对应的jar包可基于本项目直接打包使用, 或联系我们提供下载链接. 
2.  注意, 在0.2.20版本调整后, 以下基础Pipeline功能仅对字符串长度做出了限制(不超过200). 如对较长corpus进行处理, 请先试用`smoothnlp.split2sentences` 进行切句预处理
3.  如果您使用的Mac,且用anaconda管理python, 可能会碰到报错, 请尝试: 
```
export MACOSX_DEPLOYMENT_TARGET=10.10 CFLAGS='-stdlib=libc++' 
pip install jpype1 
pip install smoothnlp
```
4. 如果您寻求商业化的NLP解决方案, 欢迎邮件至 business@smoothnlp.com

## 彩蛋
1. 如果你对NLP相关算法或引用场景感兴趣, 但是却缺少实现数据, 我们提供免费的数据支持, [下载](https://github.com/smoothnlp/FinancialDatasets). 
2. 如果你是高校学生, 寻求`NLP`或`知识图谱`相关的研究素材, 甚至是实习机会. 欢迎邮件到 contact@smoothnlp.com






