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
请注意使用`python3`安装smoothnlp项目
```shell
pip3 install git+https://github.com/zhangruinan/SmoothNLP.git
```

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




