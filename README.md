# [SmoothNLP](http://www.smoothnlp.com)
[![GitHub release](https://img.shields.io/badge/Version-0.2-green.svg)](https://github.com/zhangruinan/SmoothNLP/releases)
****	

| Author | Email | 
| ----- | ------ | 
| Victor | zhangruinan@smoothnlp.com |
| Yinjun | yinjun@smoothnlp.com |
| 海蜇 | yuzhe_wang@smoothnlp.com | 

****
    

### Install 安装
```shell
pip install smoothnlp>=0.2.16
```


#### 1.Tokenize分词
```python
>> import smoothnlp 
>> smoothnlp.segment('欢迎在Python中使用SmoothNLP')
['欢迎', '在', 'Python', '中', '使用', 'SmoothNLP']
```


#### 2.Postag词性标注
```python
>> smoothnlp.postag('欢迎使用smoothnlp的Python接口')
[{'token': '欢迎', 'postag': 'VV'},
 {'token': '在', 'postag': 'P'},
 {'token': 'Python', 'postag': 'NN'},
 {'token': '中', 'postag': 'LC'},
 {'token': '使用', 'postag': 'VV'},
 {'token': 'SmoothNLP', 'postag': 'NN'}]
```


#### 3.NER 实体识别
```python
>> smoothnlp.ner("中国平安2019年度长期服务计划于2019年5月7日至5月14日通过二级市场完成购股" )
[{'charStart': 0, 'charEnd': 4, 'text': '中国平安', 'nerTag': 'COMPANY_NAME', 'sTokenList': {'1': {'token': '中国平安', 'postag': None}}, 'normalizedEntityValue': '中国平安'},
{'charStart': 4, 'charEnd': 9, 'text': '2019年', 'nerTag': 'NUMBER', 'sTokenList': {'2': {'token': '2019年', 'postag': 'CD'}}, 'normalizedEntityValue': '2019年'},
{'charStart': 17, 'charEnd': 26, 'text': '2019年5月7日', 'nerTag': 'DATETIME', 'sTokenList': {'8': {'token': '2019年5月', 'postag': None}, '9': {'token': '7日', 'postag': None}}, 'normalizedEntityValue': '2019年5月7日'},
{'charStart': 27, 'charEnd': 32, 'text': '5月14日', 'nerTag': 'DATETIME', 'sTokenList': {'11': {'token': '5月', 'postag': None}, '12': {'token': '14日', 'postag': None}}, 'normalizedEntityValue': '5月14日'}]
```


#### 4. 金融实体识别
```python
>> smoothnlp.company_recognize("旷视科技预计将在今年9月在港IPO")
[{'charStart': 0,
  'charEnd': 4,
  'text': '旷视科技',
  'nerTag': 'COMPANY_NAME',
  'sTokenList': {'1': {'token': '旷视科技', 'postag': None}},
  'normalizedEntityValue': '旷视科技'}]
```


#### 5.数字实体识别
```python
>> smoothnlp.number_recognize("百度移动应用的月活跃设备达11亿台")
[{'charStart': 13,
  'charEnd': 16,
  'text': '11亿',
  'nerTag': 'NUMBER',
  'sTokenList': {'9': {'token': '11亿', 'postag': 'm'}},
  'normalizedEntityValue': '1100000000'}]
```

#### 6. 金额识别与结构化
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

#### 7. 日期描述结构化
```python
>> smoothnlp.parse_date("2018年一季度")
{'startDate': '2018-01-01', 'endDate': '2018-03-31'}
```

----------

## Java
**SmoothNLP**项目的主要功能都在Java中有实现, 打包好的Jar文件会在[Release页面]定期更新, 或者在提供的[maven](https://github.com/smoothnlp/SmoothNLP/tree/master/smoothnlp_maven)项目代码中, 直接编译即可
```
git clone https://github.com/smoothnlp/SmoothNLP.git
cd smoothnlp_maven
mvn clean package
```
编译好的Jar文件会在 `smoothnlp_maven/target/smoothnlp-*.jar`


### 常见问题
* 如果您使用的Mac,且用anaconda管理python, 可能会碰到报错, 请尝试: 
```
export MACOSX_DEPLOYMENT_TARGET=10.10 CFLAGS='-stdlib=libc++' 
pip install jpype1 
pip install smoothnlp
```





