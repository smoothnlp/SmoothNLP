Python wrapper for SmoothNLP，侧重金融领域的自然语言处理工具的Python接口.

支持中文分词、pos、NER识别、多种金融对象的实体识别等功能.

GitHub: https://github.com/zhangruinan/SmoothNLP


Installation
============

You need Python3 installed on your system to able to use smoothnlp.


>>>
pip3 install smoothnlp


* Install Requirements
    * Python
        * Version : 3.5+
        * package managers : pip
        * packages : numpy, jpype1>=0.6.2, requests (auto check)
    * Java
        * Version : 1.8


Functions
^^^^^^^^^^^
 - segment
 - postag
 - ner
 - financial_agency_recognize
 - number_recognize




Example
^^^^^^^^^^^
Quick start

>>>
import smoothnlp
smoothnlp.segment("国泰君安一季度盈利一千万")
smoothnlp.set_mode('server') # default server, support local

首次使用local模式时会需要下载对应jar包