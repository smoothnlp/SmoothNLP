#SmoothNLP

### 安装
```shell
pip install git+https://github.com/zhangruinan/SmoothNLP.git
```

### 情感标注数据生成
**基于CoreNLP RNTN模型 半监督打标数据生成**
```python
from smoothnlp.treebanks import initNLP,lines2labeled_lines
initNLP("http://127.0.0.1",port=9000) ## 启动一个CoreNLP的Server
sample_liens = ['今天天气不错', "我心情也不错"]
lines2labeled_lines(sample_liens,"labeled_data.txt")  ## 利用现有模型先标注，并写入到文件
```
输出文件地址可以参照[这里]()