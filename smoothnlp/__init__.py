global __name__
__name__ = "SmoothNLP"

import glob
import os
import sys
import platform
import logging
import jpype

from jpype import JClass,startJVM, getDefaultJVMPath, isThreadAttachedToJVM, attachThreadToJVM

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), os.path.pardir))

if sys.version_info[0] != 3:
    # now support python version 3
    raise EnvironmentError()

def _start_jvm_for_smoothnlp():
    global STATIC_ROOT, SMOOTHNLP_JAR_PATH, PATH_CONFIG, SMOOTHNLP_JAR_VERSION

    # Get ENV
    ENVIRON = os.environ.copy()
    # Load variables in Environment

    if "SMOOTHNLP_STATIC_ROOT" in ENVIRON:
        STATIC_ROOT = ENVIRON['SMOOTHNLP_STATIC_ROOT']
    else:
        from smoothnlp.static import STATIC_ROOT

    if "SMOOTHNLP_JAR_PATH" in ENVIRON:
        SMOOTHNLP_JAR_PATH = ENVIRON["SMOOTHNLP_JAR_PATH"]
    else:
        from smoothnlp.static import SMOOTHNLP_JAR_PATH

    if "SMOOTHNLP_JVM_XMS" in ENVIRON:
        SMOOTHNLP_JVM_XMS = ENVIRON["SMOOTHNLP_JVM_XMS"]
    else:
        SMOOTHNLP_JVM_XMS = "1300m"

    if "SMOOTHNLP_JVM_XMX" in ENVIRON:
        SMOOTHNLP_JVM_XMX = ENVIRON["SMOOTHNLP_JVM_XMX"]
    else:
        SMOOTHNLP_JVM_XMX = "2g"

    PATH_CONFIG = os.path.join(STATIC_ROOT, 'smoothnlp.properties')

    if not os.path.exists(SMOOTHNLP_JAR_PATH):
        raise ValueError(
            "配置错误: SMOOTHNLP_JAR_PATH = %s 不存在" %
            SMOOTHNLP_JAR_PATH
        )
    elif not os.path.isfile(SMOOTHNLP_JAR_PATH) or not SMOOTHNLP_JAR_PATH.endswith('.jar'):
        raise ValueError(
            "配置错误:SMOOTHNLP_JAR_PATH =%s 不是jar 文件" %
            SMOOTHNLP_JAR_PATH
        )
    else:
        SMOOTHNLP_JAR_VERSION = os.path.basename(SMOOTHNLP_JAR_PATH)[len('smoothnlp-'):len('-jar-with-dependencies.jar')]

    pathsep = os.pathsep
    if platform.system().startswith('CYGWIN'):
        jvmpath = getDefaultJVMPath()
        if not jvmpath.startswith('/cygdrive'):  # CYGWIN 使用了宿主机器的JVM，必须将路径翻译为真实路径
            pathsep = ';'
            if STATIC_ROOT.startswith('/usr/lib'):
                cygwin_root = os.popen('cygpath -w /').read().strip().replace('\\', '/')
                STATIC_ROOT = cygwin_root + STATIC_ROOT[len('/usr'):]
                HANLP_JAR_PATH = cygwin_root + SMOOTHNLP_JAR_PATH[len('/usr'):]
                PATH_CONFIG = cygwin_root + PATH_CONFIG[len('/usr'):]
            elif STATIC_ROOT.startswith('/cygdrive'):
                driver = STATIC_ROOT.split('/')
                cygwin_driver = '/'.join(driver[:3])
                win_driver = driver[2].upper() + ':'
                HANLP_JAR_PATH = SMOOTHNLP_JAR_PATH.replace(cygwin_driver, win_driver)
                STATIC_ROOT = STATIC_ROOT.replace(cygwin_driver, win_driver)
                PATH_CONFIG = PATH_CONFIG.replace(cygwin_driver, win_driver)

    JAVA_JAR_CLASSPATH = '-Djava.class.path=%s%s%s'%(
        SMOOTHNLP_JAR_PATH,pathsep,STATIC_ROOT
    )
    # 加载插件 jar
    for jar in glob.glob(os.path.join(STATIC_ROOT, '*.jar')):
        if SMOOTHNLP_JAR_PATH.endswith(jar):
            continue
        JAVA_JAR_CLASSPATH = JAVA_JAR_CLASSPATH + pathsep + os.path.jion(STATIC_ROOT, jar)

    # 启动JVM
    if jpype.isJVMStarted():
        return
    else:
        startJVM(
            getDefaultJVMPath(),
            JAVA_JAR_CLASSPATH,
            '-Xms%s' % SMOOTHNLP_JVM_XMS,
            '-Xmx%s' % SMOOTHNLP_JVM_XMX
        )


def _attach_jvm_to_thread():
    """
    use attachThreadToJVM to fix multi-thread issues: https://github.com/hankcs/pyhanlp/issues/7
    """
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


from smoothnlp.server import smoothNlpRequest


class Smoothnlp(object):
    def __init__(self, mode: str = 'server'):
        self.mode = mode
        if self.mode == 'local':
            _start_jvm_for_smoothnlp()
            self.nlp = SafeJClass('com.smoothnlp.nlp.SmoothNLP')
        else:
            self.nlp= smoothNlpRequest()

global MODE, nlp
MODE = 'server'
nlp = Smoothnlp(MODE).nlp

def set_mode(mode):
    global MODE, nlp
    MODE = mode
    nlp = Smoothnlp(MODE).nlp

from functools import wraps
import requests
import logging
import json

log = logging.getLogger()

########################
## attribute function ##
########################
def localSupportCatch(func):
    @wraps(func)
    def trycatch(text):
        try:
            return func(text)
        except AttributeError:
            log.error("This function does not support local mode : %s " % func.__name__)
    return trycatch

def requestTimeout(func):
    @wraps(func)
    def trycatch(text):
        try:
            return func(text)
        except requests.exceptions.Timeout:
            set_mode('local')
            return func(text)
    return trycatch

def convert(func):
    @wraps(func)
    def toJson(text):
        res = func(text)
        if(isinstance(res, list)):
            return res
        else:
            return json.loads(res)
    return toJson
################################
## smoothnlp support function ##
################################

@requestTimeout
@convert
def segment(text):
    return nlp.segment(text)

@requestTimeout
@convert
def postag(text):
    return nlp.postag(text)

@requestTimeout
@convert
def ner(text):
    return nlp.ner(text)


@localSupportCatch
def financial_agency_recognize(text):
    return nlp.financial_agency_recognize(text)

@localSupportCatch
def number_recognize(text):
    return nlp.number_recognize(text)



