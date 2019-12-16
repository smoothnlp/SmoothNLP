from functools import wraps
import requests
import json

from smoothnlp import logger
from ..nlp import MODE
from ..nlp import set_mode
import re


########################
## attribute function ##
########################
def localSupportCatch(func):
    @wraps(func)
    def trycatch(text):
        if MODE !="server":
            logger.error("This function does not support local mode : %s " % func.__name__)
            raise AttributeError("This function does not support local mode : %s ")
        return func(text)
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


def to_sql_update(df, engine, schema, table, id_cols = None, chunksize=100):
    """
    将 pandas.DataFrame
    :param df:
    :param engine:
    :param schema:
    :param table:
    :param id_cols:
    :param chunksize:
    :return:
    """
    if id_cols is None:
        sql = ''' SELECT column_name from information_schema.columns
                  WHERE table_schema = '{schema}' AND table_name = '{table}' AND
                        COLUMN_KEY = 'PRI';
              '''.format(schema=schema, table=table)
        id_cols = [x[0] for x in engine.execute(sql).fetchall()]
    else:
        id_cols = id_cols
    id_vals = [df[col_name].tolist() for col_name in id_cols]
    counter=0
    sql = ''' DELETE FROM {schema}.{table} WHERE 0 '''.format(schema=schema, table=table)
    for row in zip(*id_vals):
        sql_row = ' AND '.join([''' {}='{}' '''.format(n, v) for n, v in zip(id_cols, row)])
        sql += ' OR ({}) '.format(sql_row)
        counter+=1
        if counter>=chunksize:
            engine.execute(sql)
            sql = ''' DELETE FROM {schema}.{table} WHERE 0 '''.format(schema=schema, table=table)
            counter = 0
    engine.execute(sql)
    df.to_sql(table, engine, schema=schema, if_exists='append', index=False)

def remove_nonchinese(text):
    return " ".join(re.findall(r'[\u4e00-\u9fff]+', text))