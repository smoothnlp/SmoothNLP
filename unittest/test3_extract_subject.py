import unittest
from smoothnlp import kg
import sys



class Test_subject_extract(unittest.TestCase):


    def assertEqual(self,first, second):
        global error_detail
        global failed_counter
        global num_test_case
        sent = first
        first = kg.extract_subject(first)      
        if  first != second:
            failed_counter += 1
            error_detail.append(sent+'： 【subject extract res: '+str(first)+' != expected res: '+str(second)+'】')
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

    
    def test1_Subj_Passed(self):
        """测试之前通过的case"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('20亿美金收购美巨头Vizio LeEco打造最完整大屏生态',['LeEco'])
        self.assertEqual('AR公司PLNAR宣布完成390万美元A轮融资',['AR公司PLNAR'])
        self.assertEqual('李泽湘弟子创办李群自动化,获亿元级C轮融资',['李泽湘弟子'])
        self.assertEqual('出门问问宣布D轮融资1.8亿美元 并与大众汽车成立合资公司',['出门问问', '大众汽车'])
        self.assertEqual('阅文与传音达成战略合作，进入非洲市场',['阅文', '传音'])
        self.assertEqual('Facebook与多家主流媒体合作 打造独家新闻节目',['Facebook', '主流媒体'])
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.9)
    

    def test2_Subj_WithPunct(self):
        """主语两边或中间存在符号"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('「潮汐」获数百万天使轮融资,白泽资本担任本轮独家财务顾问',['潮汐'])
        self.assertEqual('获公信宝、了得资本等数千万元融资，「Prophet」想要成为最聪明的“预言家”',['Prophet'])
        self.assertEqual('“图解电影”获数百万元天使投资：同创伟业投资',['图解电影'])
        self.assertEqual('共享平台“同学借书”获数百万种子轮融资',['同学借书'])
        ## 空格
        self.assertEqual('Prelude Therapeutics完成6000万美元B轮融资',['Prelude Therapeutics'])
        self.assertEqual('雷军：向同仁堂、海底捞、Costco学什么',['雷军'])
        self.assertEqual('招商局置地：与中铁建及广东保利等合作开发广州住宅地',['招商局置地'])
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
        
        
    def test3_Subj_Modifier(self):
        """主语前存在修饰语，需要完整抽取"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('新诤信参股公司品源文华完成A轮5000万融资!',['新诤信参股公司品源文华'])
        self.assertEqual('无人机系统创企Kespry完成3300万美元C轮融资,专注工业无人机应用领域',['无人机系统创企Kespry'])
        ## 的
        self.assertEqual('为企业提供直播服务的『目睹直播』获数千万元A轮融资',['目睹直播'])
        self.assertEqual('布局AR技术的OPPO，科技路线迈入进阶模式',['OPPO'])
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
        
    
    def test4_Subj_RedundantInfo(self):
        """在主句前面存在 主语从句 ，影响主语识别"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('12.5亿元受让26.44%股权 远致富海入主麦捷科技',['远致富海'])
        self.assertEqual('定制旅游受追捧 游心旅行完成A轮千万美金融资',['游心旅行'])
        self.assertEqual('解决新高考痛点 “青蚕教育”获立思辰教育天使基金数百万元天使轮融资',['青蚕教育'])
        self.assertEqual('专注于人工肝研发，仝干生物完成1500万Pre-A轮融资',['仝干生物'])
        self.assertEqual('再也不用在加油站排队，汽车上门服务平台「Yoshi」获1370万美元融资',['Yoshi'])
        self.assertEqual('张一鸣排兵布阵，今日头条进军电子商务',['今日头条'])
        self.assertEqual('中证解读：报喜鸟4500万元入股小鬼网络 布局电商平台',['报喜鸟'])##中证
        self.assertEqual('押注汽车，贾跃亭将出任乐视汽车全球董事长',['贾跃亭']) ##押注汽车
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
        
        
    def test5_Subj_WrongSegment(self):
        """切词错误(切出错误的介词“对”、“向”等)导致的主语抽取错误"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('银行、保险业对外开放再放大招，国务院发文修改外资银行、外资保险公司管理条例',['国务院'])  #['外开放']  错切出“对”
        self.assertEqual('央行动向成市场焦点',['央行'])  ##['央行动']  错切出“向”
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
        
        
    def test6_Subj_WrongPostag(self):
        """postag错误(多为VV识别为NN)导致的主语抽取错误"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('大唐电信进军网游昭然若揭',['大唐电信']) ##['大唐电信进军']
        self.assertEqual('安居宝布局停车O2O市场 互联网+战略转型',['安居宝']) ##['安居宝布局']
        self.assertEqual('旅游板块再下一城 探路者2.3亿收购易游天下布局O2O',['探路者']) ##['探路者2.3亿收购']
        self.assertEqual('华为进军车联网 抱团传统车企后能否占领C位？',['华为']) ##['华为进军车']
        self.assertEqual('亚振家居进军家具安装服务和房屋租赁业务',['亚振家居']) ##['亚振家居进军家具']
        self.assertEqual('阿里狂砸62亿 进军增强现实',['阿里']) ##['阿里', '进军']
        self.assertEqual('美团点评投资的OPay推出打车业务OCar',['美团点评']) ##['美团点评投资']
        self.assertEqual('北汽集团谋划做戴姆勒最大股东',['北汽集团']) ##['北汽集团谋划']
        self.assertEqual('家乐福中国回应唐嘉年辞职：将留任至今秋 与股权变动无关',['家乐福中国']) ##['家乐福中国回应']
        self.assertEqual('易到四名乐视系高管离职，下一步还要怎么去乐视化？',['易到']) ##'['乐视', '高管离职']
        self.assertEqual('国际医学牵手阿里云打造云医院 线上线下比翼齐飞',['国际医学','阿里云'])
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
        
        
    def test7_Subj_ConjRelation(self):
        """
        双主语,“A与B做事件C”会将主语抽取成“B”
        case1：'阿里与腾讯合作云游戏' 认为主语是['阿里', '腾讯']
        case2：'阿里与腾讯合作'  认为主语是['阿里'],如果抽取成['腾讯']就是错的
        """
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('腾讯爱奇艺点播惹争议',['腾讯','爱奇艺'])
        self.assertEqual('苏宁小店、零售云独立成为子集团',['苏宁小店','零售云'])
        ## “A与B做事件C”会将主语抽取成“B”
        self.assertEqual('南山铝业与南车四方签订战略合作协议',['南山铝业','南车四方'])  ## 抽取成['南车']
        self.assertEqual('世茂正与福晟集团洽谈',['世茂'])  ## 抽取成['福晟集团']
        self.assertEqual('滴滴与NVIDIA合作,推动自动驾驶和云计算领域发展',['滴滴','NVIDIA'])
        self.assertEqual('产业援疆：中石化与新疆成立合资公司',['中石化','新疆'])
        self.assertEqual('寺库牵手百盛，高端生活服务领域的新零售范本露出',['寺库'])
        self.assertEqual('苏宁汽车与公平价合作 加快布局二手车业务',['苏宁汽车'])
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
        
