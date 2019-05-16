import os
import sys
import json
PY = 3
if sys.version_info[0] < 3:
    PY = 2
    raise EnvironmentError("Must be using Python 3")

import urllib.request as urllib
import errno
import time
import glob

release_url = "https://api.github.com/repos/yjun1989/Smoothnlp/releases"
download_jar_url = "http://datashare.smoothnlp.com/data/smoothnlp-{}-jar-with-dependencies.jar"

STATIC_ROOT = os.path.dirname(os.path.realpath(__file__))
SMOOTHNLP_JAR_VERSION = None
SMOOTHNLP_RELEASE = None
INDEX_HTML = os.path.join(STATIC_ROOT, 'index.html')

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

def remove_file(filename):
    try:
        os.remove(filename)
    except OSError as e:  # this would be "except OSError, e:" before Python 2.6
        if e.errno != errno.ENOENT:  # errno.ENOENT = no such file or directory
            raise  # re-raise exception if a different error occurred


def smoothnlp_latest_version():
    #smoothnlp_releases()[0]
    meta = ['0.2']
    return meta


def smoothnlp_releases(cache=True):
    global SMOOTHNLP_RELEASE
    if cache and SMOOTHNLP_RELEASE:
        return SMOOTHNLP_RELEASE
    if PY == 3:
        import ssl
        content = urllib.urlopen(release_url, context=ssl._create_unverified_context()).read()
    else:
        raise EnvironmentError("only support python3 now ")

    content = json.loads(content.decode('utf-8-sig'))
    meta = []
    for r in content:
        jar_name = r['tag-name']
        if jar_name.startwith('smooth'):
            jar_name = jar_name[1:]
        meta.append((jar_name))

    SMOOTHNLP_RELEASE = meta
    return meta


def install_smoothnlp_jar_version(version=None):
    if version is None:
        version = smoothnlp_latest_version()[0]


def download(url, path):
    if os.path.isfile(path):
        print("Using local{}, ignore {}".format(path, url))
        return True
    else:
        print("Downloading {} to {} ".format(url,path))
        tmp_path = '{}.downloading'.format(path)
        remove_file(tmp_path)
        try:
            def reporthook(count, block_size, total_size):
                global start_time, progress_size
                if count == 0:
                    start_time = time.time()
                    progress_size = 0
                    return
                duration = time.time() - start_time
                duration = max(1e-8, duration)  # 防止除零错误
                progress_size = int(count * block_size)
                if progress_size > total_size:
                    progress_size = total_size
                speed = int(progress_size / (1024 * duration))
                ratio = progress_size / total_size
                ratio = max(1e-8, ratio)
                percent = ratio * 100
                eta = duration / ratio * (1 - ratio)
                minutes = eta / 60
                seconds = eta % 60
                sys.stdout.write("\r%.2f%%, %d MB, %d KB/s, ETA %d min %d s" %
                                 (percent, progress_size / (1024 * 1024), speed, minutes, seconds))
                sys.stdout.flush()

            import socket
            socket.setdefaulttimeout(10)
            urllib.urlretrieve(url, tmp_path, reporthook)
        except Exception as e:
            try:
                if os.name !='nt':
                    os.system('wget {} -O {}'.format(url, tmp_path))
                else:
                    raise e
            except:
                eprint('Failed to download {}'.format(url))
                eprint('Please refer to https://github.com/hankcs/pyhanlp for manually installation.')
                return False
        remove_file(path)
        os.rename(tmp_path, path)
    return True


def install_smoothnlp_jar(version=None):
    if version is None:
        version = smoothnlp_latest_version()[0]
    url = download_jar_url.format(version)
    jar_path= os.path.join(STATIC_ROOT, 'smoothnlp-{}-jar-with-dependencies.jar'.format(version))
    download(url, jar_path)
    global SMOOTHNLP_JAR_VERSION
    SMOOTHNLP_JAR_VERSION = version


def smoothnlp_installed_jar_version():
    versions = []
    for jar in glob.glob(os.path.join(STATIC_ROOT, 'smoothnlp-*.jar')):
        versions.append(os.path.basename(jar)[len('smoothnlp-'):-len('-jar-with-dependencies.jar')])

    versions = sorted(versions,reverse=True)

    if versions:
        global SMOOTHNLP_JAR_VERSION
        SMOOTHNLP_JAR_VERSION = versions[0]
    return versions


def smoothnlp_jar_path(version):
    return os.path.join(STATIC_ROOT,'smoothnlp-{}-jar-with-dependencies.jar'.format(version))


if not smoothnlp_installed_jar_version():
    install_smoothnlp_jar()


SMOOTHNLP_JAR_PATH = smoothnlp_jar_path(SMOOTHNLP_JAR_VERSION)