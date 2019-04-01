# PowSyBl Core

[![Build Status](https://api.travis-ci.com/powsybl/powsybl-core.svg?branch=master)](https://travis-ci.com/powsybl/powsybl-core)
[![Build status](https://ci.appveyor.com/api/projects/status/76o2bbmsewpbpr97/branch/master?svg=true)](https://ci.appveyor.com/project/powsybl/powsybl-core/branch/master)
[![Coverage Status](https://coveralls.io/repos/github/powsybl/powsybl-core/badge.svg?branch=master)](https://coveralls.io/github/powsybl/powsybl-core?branch=master)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Apowsybl-core&metric=coverage)](https://sonarcloud.io/component_measures?id=com.powsybl%3Apowsybl-core&metric=coverage)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Apowsybl-core&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.powsybl%3Apowsybl-core)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Join the chat at https://gitter.im/powsybl/powsybl-core](https://badges.gitter.im/powsybl/powsybl-core.svg)](https://gitter.im/powsybl/powsybl-core?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Javadocs](https://www.javadoc.io/badge/com.powsybl/powsybl-core.svg?color=blue)](https://www.javadoc.io/doc/com.powsybl/powsybl-core)

PowSyBl (**Pow**er **Sy**stem **Bl**ocks) is an open source framework written in Java, that makes it easy to write complex software for power systems’ simulations and analysis. Its modular approach allows developers to extend or customize its features.

PowSyBl is part of the LF Energy Foundation, a project of The Linux Foundation that supports open source innovation projects within the energy and electricity sectors.

<p align="center">
<img src="https://raw.githubusercontent.com/powsybl/powsybl-gse/master/gse-spi/src/main/resources/images/logo_lfe_powsybl.svg?sanitize=true" alt="PowSyBl Logo" width="50%"/>
</p>

Read more at https://www.powsybl.org !

This project and everyone participating in it is governed by the [PowSyBl Code of Conduct](https://github.com/powsybl/.github/blob/master/CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [powsybl.ddl@rte-france.com](mailto:powsybl.ddl@rte-france.com).

## PowSyBl vs PowSyBl Core

This document describes how to build the code of PowSyBl Core. If you just want to run PowSyBl demos, please visit https://www.powsybl.org/ where downloads will be available soon. If you want guidance on how to start building your own application based on PowSyBl, please visit the http://www.powsybl.org/docs/tutorials/ page.

The PowSyBl Core project is not a standalone project. Read on to learn how to modify the core code, be it for fun, for diagnosing bugs, for improving your understanding of the framework, or for preparing pull requests to suggest improvements! PowSyBl Core provides library code to build all kinds of applications for power systems: a complete and extendable grid model, support for common exchange formats, APIs for power simulations an analysis, and support for local or distributed computations. For deployment, powsybl-core also provides iTools, a tool to build cross-platform integrated command-line applications. To build cross-platform graphical applications, please visit the PowSyBl GSE repository https://github.com/powsybl/powsybl-gse page.

## Environment requirements

Most of the powsybl-core project is written in Java, with additional C++ components. Unless you are actually using the C++ components, it is not needed to build them and you can start to experiment and run code quickly with the [Simple Java Build](#simple-java-build). If you want to use the C++ components, refer to the [Full Project Build](#full-project-build).

## Simple Java Build

  * JDK *(1.8 or greater)*
  * Maven *(3.3.9 or greater)*

To run all the tests, simply launch the following command from the root of the repository:
```
$ mvn package
```

Modify some existing tests or create your own new tests to experiment with the framework! If it suits you better, import the project in an IDE and use the IDE to launch your own main classes. If you know java and maven and want to do things manually, you can also use maven directly to compute the classpath of all the project jars and run anything you want with it.

Read [Contributing.md](https://github.com/powsybl/.github/blob/master/CONTRIBUTING.md) for more in-depth explanations on how to run code.

## Full Project Build
In order to fully build the project, in addition to the java you need:
  * CMake *(2.6 or greater)*
  * Recent C++ compiler (GNU g++ or Clang)
  * OpenMPI *(1.8.3 or greater)*
  * Some development packages (zlib, bzip2)

### OpenMPI (required)
In order to support the MPI modules, you need to compile and install the [OpenMPI](https://www.open-mpi.org/) library.
```
$> wget http://www.open-mpi.org/software/ompi/v1.8/downloads/openmpi-1.8.3.tar.bz2
$> tar xjf openmpi-1.8.3.tar.bz2
$> cd openmpi-1.8.3
$> ./configure --prefix=<INSTALL_DIR> --enable-mpi-thread-multiple
$> make install
$> export PATH=$PATH:<INSTALL_DIR>/bin
$> export LD_LIBRARY_PATH=<INSTALL_DIR>/lib:$LD_LIBRARY_PATH
```

### zlib (required)
In order to build Boost external package, you have to install [zlib](http://www.zlib.net/) library.
```
$> yum install zlib-devel
```

### bzip2 (required)
In order to build Boost external package, you have to install [bzip](http://www.bzip.org/) library.
```
$> yum install bzip2-devel
```

## Install
To easily compile, you can use the toolchain:
```
$> git clone https://github.com/powsybl/powsybl-core.git
$> ./install.sh
```
By default, the toolchain will:
  * download and compile all external packages from the Internet
  * compile C++ and Java modules
  * install the platform

### Targets

| Target | Description |
| ------ | ----------- |
| clean | Clean modules |
| clean-thirdparty | Clean the thirdparty libraries |
| compile | Compile modules |
| package | Compile modules and create a distributable package |
| __install__ | __Compile modules and install it__ |
| docs | Generate the documentation (Doxygen/Javadoc) |
| help | Display this help |

### Options

The toolchain options are saved in the *install.cfg* configuration file. This configuration file is loaded and updated
each time you use the toolchain.

#### Global options

| Option | Description | Default value |
| ------ | ----------- | ------------- |
| --help | Display this help | |
| --prefix | Set the installation directory | $HOME/powsybl |
| --package-type | Set the package format. The supported formats are zip, tar, tar.gz and tar.bz2 | zip |

#### Third-parties

| Option | Description | Default value |
| ------ | ----------- | ------------- |
| --with-thirdparty | Enable the compilation of thirdparty libraries | |
| --without-thirdparty | Disable the compilation of thirdparty libraries | |
| --thirdparty-prefix | Set the thirdparty installation directory | $HOME/powsybl_thirdparty |
| --thirdparty-download | Sets false to compile thirdparty libraries from a local repository | true |
| --thirdparty-packs | Sets the thirdparty libraries local repository | $HOME/powsybl_packs |

### Default configuration file
```
#  -- Global options --
powsybl_prefix=$HOME/powsybl
powsybl_package_type=zip

#  -- Thirdparty libraries --
thirdparty_build=true
thirdparty_prefix=$HOME/powsybl_thirdparty
thirdparty_download=true
thirdparty_packs=$HOME/powsybl_packs
```
