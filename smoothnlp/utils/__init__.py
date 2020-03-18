########################
## attribute function ##
########################

def mapnames(data,NAME_MAPPER,strict:bool=False):
    if isinstance(data,dict):
        if strict:
            pops = [k for k,v in data.items() if k not in NAME_MAPPER and not isinstance(v,dict) and not isinstance(v,list)]
            for p in pops:
                data.pop(p)
        update_keys = [k for k in data.keys() if k in NAME_MAPPER]
        for k in update_keys:
            data[NAME_MAPPER[k]] = data.pop(k)
        for k in data.keys():
            if isinstance(data[k],list):
                data[k] = mapnames(data[k],NAME_MAPPER,strict)
    if isinstance(data,list):
        data = [mapnames(d,NAME_MAPPER,strict) for d in data]
    return data

FORWARD_NAME_MAPPER = {
    "tokens":"ts",
    "token":"t",
    "postag":"p",
    "tagproba":"tp",
    "entities":"ets",
    "charStart":"cs",
    "charEnd":"ce",
    "text":"txt",
    "nerTag":"nt",
    "normalizedEntityValue":"nev",
    "sTokenList":"stl",
    "dependencyRelationships":"drs",
    "relationship":"r",
    "dependentIndex":"di",
    "targetIndex":"ti",
    "_edge_score":"_es",
    "_tag_score":"_ts",
    "errMsg":"em"
}
BACKWARD_NAME_MAPPER = {v:k for k,v in FORWARD_NAME_MAPPER.items()}

compress_meta = lambda meta: mapnames(meta,FORWARD_NAME_MAPPER,strict=False)
decompress_meta = lambda meta: mapnames(meta,BACKWARD_NAME_MAPPER,strict=False)