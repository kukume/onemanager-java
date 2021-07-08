## OneManager

### 环境准备

安装jdk1.8或以上版本

1、 前往 [https://github.com/kukume/onemanager-java/releases](https://github.com/kukume/onemanager-java/releases) 下载压缩包

2、 解压，里面有4个文件
* start：Linux执行，为后台运行
* start.bat：Windows执行，为后台运行
* run：Linux执行，为前台执行，断开ssh即停止运行
* run.bat：Windows执行，关闭cmd窗口即停止运行

3、 运行对应的执行文件即可

4、程序运行之后，打开 [http://IP:5460](http://IP:5460) 即可，首次登陆即为设置密码。


### 开发

为启动类添加VM属性
```shell
-javaagent:lib/lombok.jar=ECJ
```

打包
```shell
mvn clean package -Dmaven.test.skip=true
```