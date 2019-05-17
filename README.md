# [SmoothNLP](http://www.smoothnlp.com)
[![GitHub release](https://img.shields.io/badge/Version-0.2-green.svg)](https://github.com/zhangruinan/SmoothNLP/releases)
****	
|Author|Victor|Yinjun
|---|---|----
|E-mail|zhangruinan@smoothnlp.com|yinjun@smoothnlp.com
****


<!-- ----------- -->

## Installation
### Python
python interfaces for SmoothNLP 的 Python 接口， 支持自动下载 ，目前支持Python 3

```shell
pip3 install smoothnlp
```
请注意使用`python3`安装smoothnlp项目，当前版本 version=0.2.4

#### API 
通过smoothnlp 调用接口

```python

import smoothnlp 

# 分词
smoothnlp.segment('欢迎使用smoothnlp的Python接口')
['欢迎', '使用', 'smoothnlp', '的', 'Python', '接口']

# 词性标注
smoothnlp.postag('欢迎使用smoothnlp的Python接口')
[{'postag': 'VV', 'token': '欢迎'}, {'postag': 'VV', 'token': '使用'}, {'postag': 'NN', 'token': 'smoothnlp'}, {'postag': 'DEC', 'token': '的'}, {'postag': 'NN', 'token': 'Python'}, {'postag': 'NN', 'token': '接口'}]

# ner 实体识别
smoothnlp.ner("中国平安2019年度长期服务计划于2019年5月7日至5月14日通过二级市场完成购股，" \
              "共购得本公司A股股票5429.47万股，占总股本的比例为0.297%，" \
              "成交金额合计42.96亿元（含费用），成交均价约为79.10元/股")
[{'charEnd': 4, 'charStart': 0, 'nerTag': 'financial_agency', 'normalizedEntityValue': '中国平安', 'sTokenList': None, 'text': '中国平安'},
 {'charEnd': 9, 'charStart': 4, 'nerTag': 'datetime', 'normalizedEntityValue': '2019年', 'sTokenList': None, 'text': '2019年'}, 
 {'charEnd': 26, 'charStart': 17, 'nerTag': 'datetime', 'normalizedEntityValue': '2019年5月7日', 'sTokenList': None, 'text': '2019年5月7日'},
 {'charEnd': 29, 'charStart': 27, 'nerTag': 'datetime', 'normalizedEntityValue': '5月', 'sTokenList': None, 'text': '5月'}, 
 {'charEnd': 51, 'charStart': 49, 'nerTag': 'financial_metric', 'normalizedEntityValue': 'A股', 'sTokenList': None, 'text': 'A股'}, 
 {'charEnd': 53, 'charStart': 51, 'nerTag': 'financial_metric', 'normalizedEntityValue': '股票', 'sTokenList': None, 'text': '股票'}, 
 {'charEnd': 61, 'charStart': 53, 'nerTag': 'NUMBER', 'normalizedEntityValue': '54294700', 'sTokenList': {'25': {'postag': 'NN', 'token': '5429.47万'}}, 'text': '5429.47万'}, 
 {'charEnd': 67, 'charStart': 64, 'nerTag': 'organization_metric', 'normalizedEntityValue': '总股本', 'sTokenList': None, 'text': '总股本'}, 
 {'charEnd': 77, 'charStart': 71, 'nerTag': 'PERCENT', 'normalizedEntityValue': '0.297%', 'sTokenList': {'33': {'postag': 'NN', 'token': '0.297%'}}, 'text': '0.297%'}, 
 {'charEnd': 91, 'charStart': 84, 'nerTag': 'MONEY', 'normalizedEntityValue': '¥4296000000', 'sTokenList': {'38': {'postag': 'CD', 'token': '42.96亿'}, '39': {'postag': 'M', 'token': '元'}}, 'text': '42.96亿元'}, 
 {'charEnd': 109, 'charStart': 103, 'nerTag': 'MONEY', 'normalizedEntityValue': '¥79.1', 'sTokenList': {'49': {'postag': 'CD', 'token': '79.10'}, '50': {'postag': 'M', 'token': '元'}}, 'text': '79.10元'}]

# 金融实体识别
smoothnlp.financial_agency_recognize("中国平安2019年度长期服务计划于2019年5月7日至5月14日通过二级市场完成购股")
[{'charEnd': 4, 'charStart': 0, 'nerTag': 'financial_agency', 'normalizedEntityValue': '中国平安', 'sTokenList': None, 'text': '中国平安'}]

# 数字实体识别
smoothnlp.number_recognize("百度移动应用的月活跃设备达11亿台")
#-- output
[{'charEnd': 16, 'charStart': 13, 'nerTag': 'NUMBER', 'normalizedEntityValue': '1100000000', 'sTokenList': {'9': {'postag': 'CD', 'token': '11亿'}}, 'text': '11亿'}]

```
更多功能请阅读pySmoothnlp项目文档

### Java Maven-Project
**SmoothNLP**项目的主要功能都在Java中有实现, 打包好的Jar文件会在[Release页面]定期更新, 或者在提供的[maven](https://github.com/zhangruinan/SmoothNLP/tree/master/smoothnlp_maven)项目代码中, 直接编译即可
```
git clone https://github.com/zhangruinan/SmoothNLP.git
cd smoothnlp_maven
mvn clean package
```
编译好的Jar文件会在 `smoothnlp_maven/target/smoothnlp-*.jar`

------------

### Usage 调用 
#### Initialize 启动
目前在*0.1*版本中, python的功能支持, 还是通过`jpype`将thread添加到一个运行的jvm上,原理类似于 `pyhanlp` 与 `HanLP`, 在*0.2*版本中, 将支持 smoothnlp-*.jar 通过脚本从Release中自动下载的功能. 
```python
import smoothnlp
smoothnlp.initJVMConnection("/smoothnlp_maven/target/smoothnlp-0.2-jar-with-dependencies.jar")  
```

#### 1. Tokenize 分词

#### 2. Postag 词性标注

#### 3. Dependency Parsing 依存句法分析

#### 4. NER 命名实体识别

#### 5. Other 其他




