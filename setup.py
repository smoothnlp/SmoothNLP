import setuptools

# with open("README.md", "r") as fh:
#     long_description = fh.read().decode().encode('utf-8')

setuptools.setup(
    name="SmoothNLP",
    version="0.1",
    author="Ruinan(Victor) Zhang",
    author_email="ruinan.zhang@icloud.com",
    description="Simple utilities for fast and easy NLP pipeline deployment",
    long_description="",
    long_description_content_type="text/markdown",
    url="https://github.com/zhangruinan/SmoothNLP",
    packages=setuptools.find_packages(),
    install_requires=[
        'numpy',
        'pytreebank',
        'stanfordcorenlp'
      ],
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
)