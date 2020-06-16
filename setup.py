import setuptools
import os

rootdir = os.path.abspath(os.path.dirname(__file__))
long_description = open(os.path.join(rootdir, 'README.md'), encoding='utf-8').read()

setuptools.setup(
    name="SmoothNLP",
    version="0.4.0",
    author="SmoothNLP Oraganization",
    author_email="contact@smoothnlp.com",
    description="Python Package for SmoothNLP",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/smoothnlp/SmoothNLP",
    packages=setuptools.find_packages(),
    package_data={'smoothnlp': ['resources/*']},
    install_requires=[
        'numpy',
        "requests",
        "pygtrie",
        "networkx"
      ],
    keywords=["NLP","Python","SmoothNLP","Natural Language Processing","Knowledge Graph Extraction"],
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
    test_suite = "smoothnlp.unittest.testall",
)
