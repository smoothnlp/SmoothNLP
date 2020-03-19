import unittest
import smoothnlp as nlp
import sys

class Test_Segment(unittest.TestCase):
    def assertEqual(self,input_data,first, second):
        global error_detail
        global failed_counter
        global number_test_case
        segs = nlp.segment(input_data)
        first_cut = nlp.segment(first)
        second_cut = nlp.segment(second)
        if set(first_cut) < set(segs):
            if set(second_cut) < set(segs):
                number_test_case +=1
            else:
                error_detail.append(input_data+'：error:!!!!!【exist_first_word: '+str(first)+' non-existent_second_word: '+str(second)+'】!!!!!')
        else:
            failed_counter += 1
            error_detail.append(input_data+'：error:!!!!!【non-existent_first_word: '+str(first)+' existent_second_word: '+str(second)+'】!!!!!')
            number_test_case += 1
            
    def setUp(self):
        global error_detail
        global failed_counter
        global number_test_case
        error_detail = []
        failed_counter = 0
        number_test_case = 0

    def tearDown(self):
        if error_detail:
            for res in error_detail:
                print(res)
    def test1_SegArbitrarily(self):
        """测试任意想要分离的词"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('见证历史，他将要离开娱乐圈！','他','娱乐圈')
        self.assertEqual('阿里云向全球免费开放新冠AI技术','阿里云','新冠')
        self.assertEqual('曹德旺是当前知名的企业家！','企业家','曹德旺')
        self.assertEqual('生活中感到作为“赛博囚徒”的时刻','生活','赛博囚徒')
        self.assertEqual('欧洲机场给马云捐赠最高礼遇','欧洲机场','最高礼遇')
        self.assertEqual('盖茨称美国民众需2至3个月社交隔离','盖茨','社交隔离')
        self.assertEqual('谁是中国企业家里最会穿衣服的男人？','企业家','男人')
        self.assertEqual('千人千面，同样的企业家在不同的人眼里有着不同的含义','企业家','千人千面')
        self.assertEqual('国际组织呼吁查清军运会美国代表所患疟疾与新冠肺炎之关联','国际组织','新冠肺炎')
        self.assertEqual('钟南山今天的讲话精华来了！信息量很大！最后他还向吃货们温馨建议','钟南山','吃货们')
        accuracy_rate = 1-failed_counter/number_test_case
        self.assertGreaterEqual(accuracy_rate,0.8)
        print(accuracy_rate)
        
#若在jupyter-notebook中run上述代码请将下行取消注释
#unittest.main(argv=['ignored', '-v'], exit=False)