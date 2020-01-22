
from ...nlp import nlp
from functools import wraps
from copy import copy,deepcopy

prettify = lambda l: "".join([t['token'] for t in l])

def prettify(output):
    if isinstance(output,list):
         return  "".join([t['token'] for t in output])
    elif isinstance(output,dict):
        for k in list(output.keys()):
            output[k] = prettify(output[k])
    return output

def adapt_struct(func):
    @wraps(func)
    def tostruct(struct:dict = None,text:str = None,*arg,**kargs):
        if "struct" in kargs:
            kargs.pop("struct")
        if isinstance(struct,str):
            struct = nlp.analyze(struct)
        if struct is None or isinstance(text,str):
            struct = nlp.analyze(text)
        for i in range(len(struct['tokens'])):
            struct['tokens'][i]['index'] = i+1
        return func(struct = struct,*arg,**kargs)
    return tostruct

def options(func):
    def options_wrapper(*arg,**kargs):
        if "pretty" in kargs:
            pretty = kargs.pop("pretty")
            func_output = func(*arg,**kargs)
            if pretty:
                if not isinstance(func_output,list):
                    raise TypeError("Function Output should be list of list of dictionary")
                func_output = [prettify(output) for output in func_output]
            return func_output
        else:
            return func(*arg,**kargs,pretty = False)
    return options_wrapper