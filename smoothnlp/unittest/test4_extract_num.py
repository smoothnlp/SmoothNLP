import unittest
from smoothnlp import kg
import sys



class Test_num_extract(unittest.TestCase):


    def assertEqual(self,first, second):
        global error_detail
        global failed_counter
        global num_test_case
        sent = first
        first = kg.phrase.extract_num_phrase(first,pretty=True)
        if  first != second:
            failed_counter += 1
            error_detail.append(sent+'： 【num extract res: '+str(first)+' != expected res: '+str(second)+'】')
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

   
    def test1_num_Passed(self):
        """测试之前通过的case"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('万事达与网联达将合资，进入中国第三方支付市场',['第三'])
        self.assertEqual('营收过千亿，亚马逊仍像创业公司一样高效运作的秘诀',['千亿'])
        self.assertEqual('提升单店营收10% 阿里系小程序矩阵“上线”的五点思考',['10%', '五点'])
        self.assertEqual('记账理财App或迎风口 随手记上半年营收超去年5倍',['5倍'])
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.9)
    

    def test2_num_WrongPostag(self):
        """postag错误导致num抽取错误"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        ## OD误判
        self.assertEqual('建信保险资管变更注册资本金至5亿元',['5亿元'])  ##资本金    
        self.assertEqual('复星昆仲VC合伙人团队离职创业，惠普进军3D打印产业',[])  ##合伙
        self.assertEqual('微信新应用号开启内测',[])  ##应用
        self.assertEqual('致敬改革先锋 禹国刚亲述资本市场开拓历程',[])  ##禹
        self.assertEqual('江粉磁材上半年营收74.43亿元 并购整合将成未来业绩增长点',['74.43亿元']) ##点
        self.assertEqual('燕京啤酒半年报:营收净利双增长 超20%净利来自处置固定资产',['半年','20%']) ##净利
        ## 单位(M)没有识别出来
        self.assertEqual('1药网牵手九州通 提供6小时送药上门服务',['6小时'])
        self.assertEqual('23家医药上市公司上半年营收下滑 健民集团金陵医药等在列',['23家'])
        self.assertEqual('芯片代工商台积电今年前7个月营收173亿美元',['7个月','173亿美元'])
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
        
        
    def test3_num_Mtag(self):
        """建议postag“M”  在前面有“CD/OD”的时候才抽取"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('出租车也可接网约车单',[])  ##单
        self.assertEqual('菜鸟联合天猫启动“神农计划” ，两年开100个生鲜仓',['两年', '100个'])  ##天
        self.assertEqual('腾讯游戏与区块链游戏竞技平台合作推出直播频道',[])  ##块
        self.assertEqual('携手红豆，京东首个服饰类无界零售店即将上线',['首个'])  ##类
        self.assertEqual('跨境电商有棵树2016营收近25亿 净利润过亿',['2016', '25亿'])  ##棵
        self.assertEqual('赞那度：借助VR视频沉浸体验效果，依托顶级酒店资源优势，发力高端旅行VR体验业务',[])  ##度
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
        
        
    def test4_num_pretty(self):
        """相邻结果没有拼接"""
        print('\n######',sys._getframe().f_code.co_name,'测试结果：######')
        self.assertEqual('微软第四财季营收337亿美元',['第四财季', '337亿美元'])
        self.assertEqual('方源资本完成第二期基金募集 总额达13.5亿美元',['第二期', '13.5亿美元'])
        self.assertEqual('途牛第三季度净营收40亿元 净亏损5.2亿元',['40亿元', '5.2亿元'])    ##['40亿', '元', '5.2亿元']     
        self.assertEqual('礼来第一季营收优于预期 略上调全年获利预期',['第一季'])
        self.assertEqual('并购重组委换届在即：扩容4倍 “80后”首现候选名单',['4倍', '80'])
        self.assertEqual('京能置业：2019上半年营收7.48亿元 同比增72.16%',['2019','7.48亿元','72.16%'])
        self.assertEqual('沃博联出资27.67亿元入股国大药房',['27.67亿元'])
        self.assertEqual('王亚伟复出第一单牵手招行',['第一单'])
        self.assertGreaterEqual(1-failed_counter/num_test_case,0.5)
                         
             
# Tocheck：公司名/常用词中存在数量词,是否会对之后的事件抽取产生影响
# '除了品质外卖，百米厨房还帮大厨建立个人品牌',['百米']
# '不要老是看不起二次元，分分钟用300亿收入锤你胸口',['二', '次元', '300亿']


# Tocheck：CD/OD+PU+M能否正确抽取
# self.assertEqual('农业银行二级资本债发行 5+5年期发行利率4.3%',['二级', '5+5年', '4.3%'])  
# self.assertEqual('恒瑞医药2018年喜人业绩背后：销售费用64亿 占营收1/3',['2018年', '64亿', '1/3'])   
# ## NN/NR/NT+DTA or DTA+NN
# self.assertEqual('证监会：1-9月份发生上市公司并购重组近3000单',['1-9月', '3000单'])  ## ['1-','9月']  
# self.assertEqual('1-4月天津规模以上营利性服务业营收535.92亿元',['1-4月', '535.92亿元'])  ## ['1-','4月']  
# self.assertEqual('元码基因新三板挂牌上市 2017年1-4月营收707万元',['2017年1-4月', '707万元']) ## ['2017年1-4','月']


## CD/OD与M之间存在空格 应该无需特意解决，考虑对新闻标题进行预处理
# self.assertEqual('Uber 将实现全程定位，可访问用户订单结束后 5 分钟内的位置信息',['5分钟'])
# self.assertEqual('“看天吃饭”的印度斥资 6 亿打造能精准预测季风的超级计算机',['6亿'])