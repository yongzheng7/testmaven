# 起因：

源于自身在学习关于Apt方面的知识，涉及到多个module自动创建在项目包下的class文件获取问题。在网上寻找大多数代码都无法满足运行时直接获取多个module下的指定包的内的class文件。而学习公司的方式是，创建一个类并继承自动生成的class文件，然后将该类的全限定路径设置到AndroidManifest.xml中

![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/F3389BB8A4A141C7B077F8296F343BD8/1050)

配合着代码中的进行获取，设计比较繁琐。但是不失一个简单的办法。

```
public static final String META_DATA_NAME_PREFIX = "com.gpstogis.api.impls.";

@SuppressWarnings("WeakerAccess")
    protected void loadPackages() {
        Context context = mApplication;
        ApplicationInfo appInfo;
        try {
            appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            mIsDebug = (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        for (String key : appInfo.metaData.keySet()) {
            if (!key.startsWith(META_DATA_NAME_PREFIX)) {
                continue;
            }
            String classname = appInfo.metaData.getString(key, null);
            if (classname == null || classname.isEmpty()) {
                continue;
            }
            try {
                Class<?> cls = context.getClassLoader().loadClass(classname);
                if (ApiImplBundle.class.isAssignableFrom(cls)) {
                    ApiImplBundle apiImpls = (ApiImplBundle) cls.newInstance();
                    if (apiImpls instanceof ApiImplContextAware) {
                        ((ApiImplContextAware) apiImpls).setApiImplContext(this);
                    }
                    mApiImpls.add(apiImpls);
                    Log.d(META_DATA_NAME_PREFIX, classname + " succeed");
                } else {
                    throw new RuntimeException(classname + " not implements from ApiImpls");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
```

通过学习自定义插件，做一个可以自动注入的插件更加符合要求


# 准备：
## Maven仓库（本地）

用于存储发布的自定义插件，这里为了使用快捷直接使用本地的maven仓库。
创建本地的maven仓库我这里有一个十分快捷的方式，同时还有个[项目地址](https://note.youdao.com/)可供参考（建议第一次食用，想创建个新项目练手）

1 在Android项目根目录下创建一个maven仓库，其实就是一个文件夹 这里使用response作为名字。


2 在根项目的build.gradle中添加如下

```
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
         //***
        maven {
            url '../repository' // 使用本地的response maven仓库
        }
    }
}

allprojects {
    repositories {
        //***
        maven {
            url '../repository' // 使用本地的response maven仓库
        }
    }
}

```
3 创建一个新的module，这里使用testmodule作为名称， 在testmodule下的build.gradle。内部添加如下

```
apply plugin: 'com.android.library'
apply plugin: 'maven'

****

dependencies {
    implementation fileTree(dir: "libs", include: ["*.jar"])
    ***
}

uploadArchives {
    repositories.mavenDeployer {
        //提交到远程服务器：
        // repository(url: "http://www.xxx.com/repos") {
        //    authentication(userName: "admin", password: "admin")
        // }
        //本地的Maven地址
        repository(url: uri('../repository'))//仓库路径，此处是项目目录下的repo文件夹
        pom.groupId = 'com.atom.test'//groupid自行定义，一般是包名
        pom.artifactId = 'module1'//自行定义
        pom.version = '1.0.1'//版本名称
    }
}
```
3 点击软件的如下图
![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/9E87CC6D1BCA4BC9B0339E3DC6C87C98/1090)
点击进行upload。

4 检查response文件夹是否生成了包。 正确如下：
![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/8E5618083F9E4645AE697EFCCFD4D228/1106)


5 如何使用
    5.1 例如在app module中使用 testmodule 可以这样
-     1 =  pom.groupId
-     2 =  pom.artifactId
-     3 =  pom.version
    ![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/4B7E5FE40BE948B58C04F1FAF19CD158/1113)

## 准备一个简单的插件并发布到本地maven

1 创建一个java / kotlin module 选择编程语音为 java 名字自定义 并设置如下
![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/47F011AC0CE54BC5A3C95BBAAB72A5EA/1126)


```
apply plugin: 'java-library'
apply plugin: 'groovy'
apply plugin: 'maven'
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    implementation gradleApi()//gradle sdk
    implementation localGroovy()//groovy sdk
    compile 'com.android.tools.build:transform-api:1.5.0'
    implementation 'com.android.tools.build:gradle:4.0.0'

}

sourceCompatibility = "1.8"
targetCompatibility = "1.8"

uploadArchives {
    repositories.mavenDeployer {
        //提交到远程服务器：
        // repository(url: "http://www.xxx.com/repos") {
        //    authentication(userName: "admin", password: "admin")
        // }
        //本地的Maven地址
        repository(url: uri('../repository'))//仓库路径，此处是项目目录下的repo文件夹
        pom.groupId = 'com.atom.test'//groupid自行定义，一般是包名
        pom.artifactId = 'plugin'//自行定义
        pom.version = '1.0.0'//版本名称
    }
}
```

2 新建一个Plugin类，后缀为groovy
![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/F8FE714C9D2F4FA0BFEA970464BF703D/1135)


3 在main文件夹下创建resources -》META-INF -》gradle-plugins 文件夹（注意名称固定不可写错），在该文件夹下创建一个【自定义】.properties的文件 ， 例如{com.atom.plugin.properties} ， 如下图
![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/4268F6072440471FA92927DE3CFED2C2/1148)

4 同testmodule点如下图，成功后结果如下
![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/C212D496897F40EFA3DB1885FF4F213F/1155)
![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/0306703C86334DA8B81797B0484CC560/1157)

5 使用
    5.1 在根目录的build.gradle下设置插件的路径如下
    ![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/A6E028977628452A8D46FEC1ABF7207F/1165)
    在app module下如下图
    ![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/C0E9F22170254E55B83B66E77D162CE8/1171)
    点击小锤子编译。可以看到成功添加
    ![image](http://note.youdao.com/yws/public/resource/700744119be54680094b08c3796fdd20/xmlnote/8B9B2F35FC1844FDBDB67433AB6BECE4/1176)
    
[文档地址](http://note.youdao.com/noteshare?id=495da1e1f3b4346478f4399b6e9141eb&sub=3F086FC7AC434F989FC3A5465274CC82)


Github地址
# 开始





