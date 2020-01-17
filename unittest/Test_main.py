import unittest
test_dir = './'
#定义测试目录为当前目录
discover = unittest.defaultTestLoader.discover(test_dir,pattern='test*.py')

runner = unittest.TextTestRunner()
runner.run(discover)
