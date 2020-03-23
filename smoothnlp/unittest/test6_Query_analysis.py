import sys
import unittest
import requests
from smoothnlp import config,nlp

class MainTest_Query(unittest.TestCase):
    def setUp(self):
        global failed_counter
        global number_test_case
        failed_counter = 0
        number_test_case = 0

    def GET_Query_msg(self,input_):
        API_Key = "#############请在这里输入您的API,具体获取方式请参考（https://github.com/smoothnlp/SmoothNLP/tree/master/tutorials/Query%E8%A7%A3%E6%9E%90）说明文档#############"
        #格式参考：API_Key = "F3AFbm8YOzl2tTnPzPk884GCL5rNDiME"(仅为事例，该API不可用)
        config.setNLP_Path("/pro/nlp/query")  ## 使用pro接口
        config.setApiKey(API_Key)  ## 设置自己的apikey
        response_data = nlp.parseq(input_)
        text = input_
        return response_data
        
    def Error_Message(self,input_text,result_word_1,result_word_2,want_word_1,want_word_2):
        global failed_counter
        global number_test_case
        if result_word_1 != want_word_1:
            failed_counter += 1
            print(input_text+'：error:!!!!!【result_word_1: '+str(result_word_1)+' != want_word_1: '+str(want_word_1)+'】!!!!!')
            number_test_case += 1
            if result_word_2 != want_word_2:
                print(input_text+'：error:!!!!!【result_word_2: '+str(result_word_2)+' != want_word_2: '+str(want_word_2)+'】!!!!!')
        else:
            number_test_case += 1

    def entityQuery(self,input_text,want_word_1,want_word_2):
        """测试entityQuery："""
        global failed_counter
        global number_test_case
        entity = []
        response_data = self.GET_Query_msg(input_text)
        try:
            for i in range(len(response_data['entities'])):
                entity.append(response_data['entities'][i]['entity'])
            if want_word_1 not in entity:
                result_word_1 = entity
                if want_word_2 in entity:
                    failed_counter += 1
                print(input_text+'：error:!!!!!【result_word_1: '+str(result_word_1)+' != want_word_1: '+str(want_word_1)+'】!!!!!')
            if want_word_2 not in entity:
                result_word_2 = entity
                if want_word_1 in entity:
                    failed_counter += 1
                print(input_text+'：error:!!!!!【result_word_2: '+str(result_word_2)+' != want_word_2: '+str(want_word_2)+'】!!!!!')
            if want_word_1 not in entity:
                if want_word_2 not in entity:
                    failed_counter += 1
            number_test_case += 1
        except:
            failed_counter += 1
            number_test_case += 1
            print("This text does not contain some attributes =",input_text)
            
    def describeQuery(self,input_text,want_word_1,want_word_2):
        """测试describeQuery："""
        global failed_counter
        global number_test_case
        response_data = self.GET_Query_msg(input_text)
        try:
            word_1 = response_data['relationships'][0]['entitiy_pair'][0]['describe']
            word_2 = response_data['relationships'][0]['entitiy_pair'][1]['describe']
            self.Error_Message(input_text,word_1,word_2,want_word_1,want_word_2)
        except:
            failed_counter += 1
            number_test_case += 1
            print("This text does not contain some attributes =",input_text)
        
    def typeQuery(self,input_text,want_word_1):
        """测试typeQuery："""
        global failed_counter
        global number_test_case
        response_data = self.GET_Query_msg(input_text)
        try:
            result_word_1 = response_data['relationships'][0]['type']
            if result_word_1 != want_word_1:
                failed_counter += 1
                print(input_text+'：error:!!!!!【result_word_1: '+str(result_word_1)+' != want_word_1: '+str(want_word_1)+'】!!!!!')
                number_test_case += 1
            else:
                number_test_case += 1
        except:
            failed_counter += 1
            number_test_case += 1
            print("This text does not contain some attributes =",input_text)

###########################实体抽取
    def test1_Entity_extract(self):
        """抽取文本中的对象实体（Entity + Entity）"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.entityQuery("陈冠希和新女友",'陈冠希','女友')
        self.entityQuery("如何评价王瑞恩与毕志飞的直播辩论？",'王瑞恩','毕志飞')
        self.entityQuery("奚梦瑶晒与老公何猷君合照双双素颜出镜超接地",'奚梦瑶','何猷君')
        self.entityQuery("冯小刚与郑爽聚餐暖心安慰希望你的心是暖的",'冯小刚','郑爽')
        self.entityQuery("欣赏乐曲梁山伯与祝英台",'梁山伯','祝英台')
        self.entityQuery("宋仲基推倒与宋慧乔新房",'宋仲基','宋慧乔')
        
        self.entityQuery("尤文愿与C罗续约至39岁",'尤文','C罗')
        self.entityQuery("泰勒和侃爷事件",'泰勒','侃爷')
        self.entityQuery("特朗普宣布华盛顿州为重大灾区",'特朗普','华盛顿州')
        
        self.entityQuery("腾讯和任天堂合作的背后是什么",'腾讯','任天堂')
        self.entityQuery("腾讯云IoT与意法半导体宣布在物联网方面展开合作",'腾讯云','意法半导体')
        self.entityQuery("青海泰丰先行与北汽集团等5家单位签署战略投资协议",'青海泰丰','北汽集团')
        accuracy_rate = 1-failed_counter/number_test_case
        self.assertGreaterEqual(accuracy_rate,0.2)
        print(accuracy_rate)
        print("本功能共测试了%s条"%number_test_case)
###########################对象描述
    def test2_describe_extract(self):
        """抽取文本中的Entity 与 修饰;"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.describeQuery("疫情下影视互联网新机会'",'','新')
        self.describeQuery("久历职场的老油条",'','老')
        self.describeQuery("陈冠希和前女友",'','前')
        
        self.describeQuery("网曝baby和儿子的可爱对话",'','可爱')
        self.describeQuery("无论是即将迈出校门象牙塔的稚嫩社会新人",'','稚嫩')

        self.describeQuery("钟南山的这张车票",'','这张')
        self.describeQuery("客厅和阳台应该分开",'','应该')
        self.describeQuery("教育形成了我们特有的思维",'','特有的')
        self.describeQuery("南非因疫情取消与日本国奥比赛",'','取消')
        accuracy_rate = 1-failed_counter/number_test_case
        self.assertGreaterEqual(accuracy_rate,0.2)
        print(accuracy_rate)
        print("本功能共测试了%s条"%number_test_case)
###########################对象之间的关系
    def test3_Entity_extract(self):
        """抽取文本中entity对象之间的关系"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.typeQuery("陈冠希和新女友",'conjunction')
        self.typeQuery("恋爱和婚姻的心理学知识",'conjunction')
        self.typeQuery("新媒体和数字媒体的区别",'conjunction')
        self.typeQuery("家长和老师应该是最好的搭档",'conjunction')
        self.typeQuery("时彩公司和农业基地签约仪式",'conjunction')
        self.typeQuery("音乐剧和歌剧之间的差别是什么",'conjunction')
        self.typeQuery("这与金钱和权力无关只是我和你在一起",'conjunction')
        
        self.typeQuery("陈冠希的新女友",'associate')
        self.typeQuery("了解新冠肺炎的传播途径",'associate')
        self.typeQuery("那人生的意义到底是什么呢",'associate')
        self.typeQuery("关于此次新型冠状病毒的认识",'associate')
        self.typeQuery("新一线城市的经济将面临怎样的挑战",'associate')
        self.typeQuery("2个新冠肺炎逝者的最后几天，看完真的很想哭",'associate')
        
        accuracy_rate = 1-failed_counter/number_test_case
        self.assertGreaterEqual(accuracy_rate,0.2)
        print(accuracy_rate)
        print("本功能共测试了%s条"%number_test_case)
        print("已结束")
        
        
#若在jupyter-notebook中run上述代码请将下行取消注释
#unittest.main(argv=['ignored', '-v'], exit=False)

if __name__ == '__main__':
    unittest.main()
