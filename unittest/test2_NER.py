import unittest
import smoothnlp as nlp
import sys


class Test_NER(unittest.TestCase):
    

    def assertEqual(self,charStart, first, second):
        global error_detail
        global failed_counter
        global num_test_case
        sent = first
        ner_dic = [dic for dic in nlp.ner(first) if dic['charStart']==charStart]
        if ner_dic:
            first = ner_dic[0]['text']
        else:
            first = '\s'
        if  first != second:
            failed_counter += 1
            error_detail.append(sent+'： 【NER res: '+str(first)+' != expected res: '+str(second)+'】')
            num_test_case += 1
        else:
            num_test_case += 1
            
    def setUp(self):
        global error_detail
        global failed_counter
        global num_test_case
        error_detail = []
        failed_counter = 0
        num_test_case = 0
        
    def tearDown(self):
        if error_detail:
            for res in error_detail:
                print(res)

    
    def test1_NerPassed(self):
        """测试之前通过的case"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual(6,'网络安全创企Toka获1250万美元种子轮融资','Toka')
        self.assertEqual(8,'图文创作分享应用美篇完成过亿元B轮融资，芒果文创领投','美篇')
        self.assertEqual(13,'搭建全新智能电视消费场景 电视猫与KFC跨界合作探索','电视猫')
        self.assertEqual(0,'自如工商信息变更：左晖卸任董事 熊林接任法定代表人','自如')
        self.assertEqual(16,'自如工商信息变更：左晖卸任董事 熊林接任法定代表人','熊林')
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.9)
    
    
    def test2_NerGS(self):
        """测试GS-O"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual(0,'新易盛：控股股东、实控人高光荣及董事胡学民拟合计减持不超1.37%股份','新易盛')
        self.assertEqual(20,'微软也要进军直播行业？已收购游戏直播平台Beam','Beam')
        self.assertEqual(6,'社交零售企业微谷中国今日获A轮融资','微谷中国')
        self.assertEqual(9,'新能源运营服务平台地上铁完成近3亿元B1轮融资','地上铁')
        self.assertEqual(0,'玖月教育发布两大核心产品','玖月教育')
        self.assertEqual(0,'罗马仕充电宝起火 事故频发终被召回','罗马仕')
        self.assertEqual(9,'东芝23亿美元收购Landis+Gyr','Landis+Gyr')
        self.assertEqual(13,'金宇车城以13.2亿元购买安必平100%股权','微谷中国')
        self.assertEqual(0,'贵州寿仙药业违法生产香榆胃舒合剂被查处','贵州寿仙药业')
        self.assertEqual(0,'中行9.78亿收购国开行旗下15家村镇银行','中行')
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
     
    
    def test3_NerO(self):
        """测试O-GS"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual(0,'如数家珍 董明珠加码投资半导体，上海文磨入股三安显野心','\s')
        self.assertEqual(0,'大刀阔斧推进组织变革，百度宣布晋升王海峰为CTO','\s')
        self.assertEqual(9,'新易盛：控股股东、实控人高光荣及董事胡学民拟合计减持不超1.37%股份','\s')  
        self.assertEqual(13,'杨学平卸任长城宽带董事长 公司不再经营互联网接入业务','\s')
        self.assertEqual(0,'传上海文磨总经理离职加盟虚拟运营商','\s')
        self.assertEqual(0,'九寨沟考察团赴嘉善经济技术开发区企业考察','\s')
        self.assertEqual(13,'国际油价收市涨1.1% 美联储宣布推出QE4','\s')
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)

     
    def test4_NerRM(self):
        """测试RM"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual(5,'谷歌前高管Vic Gundotra因个人原因辞去AliveCor CEO一职','Vic Gundotra')
        self.assertEqual(21,'海南省政府与中国电子签署深化战略合作协议 沈晓明芮晓武出席','沈晓明')
        self.assertEqual(3,'罗马仕充电宝起火 事故频发终被召回','')
        self.assertEqual(0,'李笑来转行？出任雄岸科技执行董事及联席CEO','李笑来')
        self.assertEqual(5,'传京东副总吴声离职 雷军：成功不靠忽悠','吴声')
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
