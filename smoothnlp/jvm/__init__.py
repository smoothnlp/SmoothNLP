import jpype
from jpype import JClass, startJVM, getDefaultJVMPath, isThreadAttachedToJVM, attachThreadToJVM
import os
from smoothnlp.jvm.static import STATIC_ROOT

# print("I am static root: ",STATIC_ROOT)
# print("static jvm path: ", getDefaultJVMPath())

def initJVMConnection(jarPath:str):
    ENVIRON = os.environ.copy()
    startJVM(getDefaultJVMPath(),
             "-Djava.class.path=%s" % jarPath)

# initJVMConnection("/Users/victor/Desktop/SmoothNLP_Work/SmoothNLP/smoothnlp_maven/target/smoothnlp-0.2-dpfree-jar-with-dependencies.jar")

def _attach_jvm_to_thread():
    if not isThreadAttachedToJVM():
        attachThreadToJVM()

class SafeJClass(object):
    def __init__(self, proxy):
        """
        JClass的线程安全版
        :param proxy: Java类的完整路径，或者一个Java对象
        """
        self._proxy = JClass(proxy) if type(proxy) is str else proxy

    def __getattr__(self, attr):
        _attach_jvm_to_thread()
        return getattr(self._proxy, attr)

    def __call__(self, *args):
        if args:
            proxy = self._proxy(*args)
        else:
            proxy = self._proxy()
        return SafeJClass(proxy)

class LazyLoadingJClass(object):
    def __init__(self, proxy):
        """
        惰性加载Class。仅在实际发生调用时才触发加载，适用于包含资源文件的静态class
        :param proxy:
        """
        self._proxy = proxy

    def __getattr__(self, attr):
        _attach_jvm_to_thread()
        self._lazy_load_jclass()
        return getattr(self._proxy, attr)

    def _lazy_load_jclass(self):
        if type(self._proxy) is str:
            self._proxy = JClass(self._proxy)

    def __call__(self, *args):
        self._lazy_load_jclass()
        if args:
            proxy = self._proxy(*args)
        else:
            proxy = self._proxy()
        return SafeJClass(proxy)



