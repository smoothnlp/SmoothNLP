import unittest
import smoothnlp as nlp
import sys


class Test_Segment(unittest.TestCase):


    def assertEqual(self,first, second, pre_word):
        """
        句首切词情况测试：pre_word = ''
        句中切词情况测试：pre_word = 要检测的字符串之前的那个词
        """
        global error_detail
        global failed_counter
        global num_test_case
        sent = first
        segs = nlp.segment(first)
        if not pre_word:
            first = segs[0]
        elif pre_word in segs:
            first = segs[segs.index(pre_word)+1]
        else:
            first = ''
        if  first != second:
            failed_counter += 1
            error_detail.append(sent+'： 【segment res: '+str(first)+' != expected res: '+str(second)+'】')
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


    def test1_SegEnglish(self):
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('传AMD女掌门苏姿丰将离职','AMD','传')
        self.assertEqual('955 Dreams推出搜寻音乐应用Band Of The Day',' ','955')
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)

        
    def test2_SegNum(self):
        '''
        数字相关的逻辑：
            小数点(.)百分号(%)不和数字切开；
            数字后面有表示时间、金钱的单位(年、万)，则与单位拼在一起；
            部分包含数字的特殊词语(36氪、Q3、K12、B1轮)；
            其他符号与数字切开
        '''
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('营收增加15.2万元','15.2万','增加')
        self.assertEqual('外运发展第一大股东累计减持15.2%','15.2%','减持')
        self.assertEqual('四维图新2018年营收达21.35亿','21.35亿','达')        
        self.assertEqual('奥飞动漫2015年游戏营收1.3亿','1.3亿','营收')
        self.assertEqual('江山控股(00295)拟11.66元出售10个太阳能项目','(','控股')
        self.assertEqual('上市对36氪有三大好处','36氪','对')
        self.assertEqual('帮邦行完成1亿元人民币B1轮投资','B1轮','人民币')
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
    
    
    def test3_SegPunct(self):
        '''测试符号与两边的字符是否切开,按照符号两边字符种类划分'''
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        ## 英文  目前部分切开，如case3 “SKP-S” 切成['SKP', '-S']
        self.assertEqual('年轻人,你需要的SUV是哪辆?对比T-Cross、T-ROC','T-Cross','对比')
        self.assertEqual('同时，首钢股份拟以4.29元/股的价格向首钢总公司发行预计不超过30亿股作为支付对价','元','4.29')
        self.assertEqual('关于Wi-Fi 6、5G和物联网、边缘时代的关键事实','Wi-Fi','关于')
        self.assertEqual('从北京SKP-S解码购物中心艺术商业新方向','SKP-S','北京')
        self.assertEqual('A&F放弃性感营销，打温暖牌专注卖衣服','A&F','')
        ## 数字  目前部分切开，如case1 “1-11月” 切成['1-', '11月']
        self.assertEqual('财政部:1-11月国有企业营业总收入55.7万亿元','1',':')
        self.assertEqual('1-4月天津规模以上营利性服务业营收535.92亿元','1','')
        self.assertEqual('外资便利店巨头7-11登陆郑州','7-11','巨头')
        ## 中文  目前部分切开，bad case如“|潮”、“|图”、“参|”
        self.assertEqual('首发|爱奇艺、哔哩哔哩被纳入MSCI指数','|','首发')
        self.assertEqual('[亚太]日经指数周四收高0.12%',']','亚太')
        self.assertEqual('Google找来两个AI坐在一起玩游戏，他们会合作还是打架？|潮科技','|','？')
        self.assertEqual('乔布斯的“宇宙飞船”着陆了，苹果新总部将于4月正式启用 |图说','|',' ')
        self.assertEqual('快讯|雷石投资完成10亿元人工智能成长基金募集','|','快讯')
        self.assertEqual('Udacity开源“无人驾驶模拟器”，程序员“自学成才”的机会来了|潮科技','|','了')
        self.assertEqual('投资界24h|险资开展股权投资迎来“绿色通道“','|','h')
        self.assertEqual('独家|年营收超50亿 巴拉巴拉“登陆”小程序','|','独家')
        self.assertEqual('新闻晚参|全国建材家居市场运行维稳，定制家居营收、净利增速双下滑','|','参')
        self.assertEqual('国内新旧造车势力牵手：蔚来和长安成立合资公司 发展电动车|钛快讯','|','电动车')
        ## 混合  目前切开
        self.assertEqual('冬至日-44.7℃ “中国最冷小镇”迎入冬来最低温','-','日')
        self.assertEqual('“烈火”-3夜射成功 印弹道导弹战略威慑力初具规模','-','”')
        self.assertEqual('7-Eleven长期未支付员工部分加班费','7-Eleven','')
        self.assertEqual('Intel的东进与ARM的西征(1)--九韶定音剑vs九耳连环刀','-',')')
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
        