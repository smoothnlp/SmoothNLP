import setuptools
import os

rootdir = os.path.abspath(os.path.dirname(__file__))
long_description = open(os.path.join(rootdir, 'README.md')).read()

setuptools.setup(
    name="SmoothNLP",
    version="0.3.0",
    author="Ruinan(Victor) Zhang, Jun Yin",
    author_email="zhangruinan@smoothnlp.com, yinjun@smoothnlp.com",
    description="Python Package for SmoothNLP",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/smoothnlp/SmoothNLP",
    packages=setuptools.find_packages(),
    install_requires=[
        'numpy',
        "requests",
        "pygtrie",
        "networkx"
      ],
    keywords=["Chinese","NLP","Python","SmoothNLP"],
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
)
