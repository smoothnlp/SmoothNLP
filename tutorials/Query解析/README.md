## Query解析 
> 功能介绍: 基于语义理解, 对(搜索等其他场景下)的用户输入(Query)进行解析

> 该功能为SmoothNLP-Pro专业版功能, 请联系contact@smoothnlp.com 获取相关apikey

### 功能点
1. 区分 Entity 与 **修饰**; 
	> 如: "国产特斯拉" = "国产"+"特斯拉"
	
2. 区分 Entity + Attribute 
	> 如: "特斯拉创始人" = "特斯拉"+"创始人"
	
3. Entity + Entity 的情况
	> 如: "百度与携程" = "百度"+"携程"

### 使用指南
```python
from smoothnlp import config,nlp
config.setApiKey("-- Your API Key here --")
nlp.parseq("陈冠希的新女友")
> {'entities': ## Query中的实体对象
    [
        {'describe': '', 'entity': '陈冠希'}, 
        {'describe': '新', 'entity': '女友'}  ## "女友"实体被"新"修饰
    ], 
    'relationships': 
    [
        {
            'entitiy_pair': 
                [
                    {'describe': '', 'entity': '陈冠希', 'role': 'subject'}, 
                    {'describe': '新', 'entity': '女友', 'role': 'object'}
                ], 
            'type': 'associate'  ## 从属关系; "女友"是"陈冠希"的一个属性
        }
    ]
}
```

### 返回结果说明
- `Entities`: query中提到的(名词对象)
    - `entity`: 往往是名词短语
    - `describe`: `entity`的修饰表述
- `Relationship` : query中提到的entity对象之间的关系, 目前支持: `并列关系`,`从属关系`
    - `entitiy_pair`: 两两实体的关系对
    - `type`: 关系名称
        `从属`: 如: `陈冠希`的`女友`; `腾讯`的`总部`; `阿里`的`CEO`
        `并列`: 如: `陈冠希`和`新女友`; `京东`和`阿里巴巴`
