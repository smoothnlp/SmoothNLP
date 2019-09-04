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
>> smoothnlp.ner("中国平安2019年度长期服务计划于2019年5月7日至5月14日通过二级市场完成购股，" \
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





