import setuptools

# with open("README.md", "r") as fh:
#     long_description = fh.read().decode().encode('utf-8')

setuptools.setup(
    name="SmoothNLP",
    version="0.2.2",
    author="Ruinan(Victor) Zhang, Jun Yin",
    author_email="ruinan.zhang@icloud.com",
    description="Python Package for SmoothNLP Project",
    long_description="",
    long_description_content_type="text/markdown",
    url="https://github.com/zhangruinan/SmoothNLP",
    packages=setuptools.find_packages(),
    install_requires=[
        'numpy',
        "pandas",
        "jpype1>=0.6.2",
        "requests"
      ],
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
)