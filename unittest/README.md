# Test


## 运行测试

* 测试单个函数

```python
python -m unittest 测试文件(test1_Seg or test2_NER or test3_extract_subject)
```

* 测试全部函数
```python
python Test_main.py
```

## 测试结果

```
测试用例1错误信息(sent：   实际输出 != 标准输出)
测试用例1结果
...
======================================================================
函数测试结果
```


* 示例：
```
 test1_SegEnglish 测试结果：
.
 test2_SegNum 测试结果：
.
 test3_SegPunct 测试结果：
财政部:1-11月国有企业营业总收入55.7万亿元： 【segment res: 1- != expected res: 1-11】
F
======================================================================
FAIL: test3_SegPunct (test1_Seg.Test_Segment)
测试符号与两边的字符是否切开,按照符号两边字符种类划分
----------------------------------------------------------------------
Traceback (most recent call last):
  File "/work/volumes/haizhe/SegTest.py", line 16, in wrapper
    return func(*args, **kwargs)
  File "/work/volumes/haizhe/SegTest.py", line 92, in test3_punct
    self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
AssertionError: 0.33333333333333337 not greater than or equal to 0.5

----------------------------------------------------------------------
Ran 3 tests in 3.976s

FAILED (failures=1)
```

测试结果可能包含如下字符：
|字符|含义|
|:---:|---|
|.|测试通过|
|F|测试失败(failure)|
|E|测试出错(error)|
|s|跳过该测试(skip)|

“Ran 3 tests in 3.976s”返回本次测试运行的测试用例数目。

如果最后一行提示 “OK”，表明所有测试用例均通过；否则返回失败的测试用例数目。




**Segment Test**

测试函数smoothnlp.segment

|测试用例|测试内容|
|---|---|
|test1_SegEnglish|英文与两边的字符是否切开|
|test2_SegNum|数字与两边的字符是否切开|
|test3_SegPunct|符号与两边的字符是否切开(按照符号两边字符种类划分)|


**NER Test**

测试函数smoothnlp.ner

|测试用例|测试内容|
|---|---|
|test1_NerPassed|之前pass的case|
|test2_NerGS|公司无法识别|
|test3_NerO|非entity误判为公司|
|test4_NerRM|人名识别情况|


**Extract Subject Test**

测试函数kg.extract_subject

|测试用例|测试内容|
|---|---|
|test1_Subj_Passed|测试之前pass的case|
|test2_Subj_WithPunct|主语两边或中间存在符号|
|test3_Subj_Modifier|主语前存在修饰语，需要完整抽取|
|test4_Subj_RedundantInfo|在主句前面存在主语从句 ，影响主语识别|
|test5_Subj_WrongSegment|切词错误(切出错误的介词“对”、“向”等)导致的主语抽取错误|
|test6_Subj_WrongPostag|postag错误(多为VV识别为NN)导致的主语抽取错误|
|test7_Subj_ConjRelation|双主语|