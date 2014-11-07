详细文档：[传送门](http://k-lam.github.io/blog/2014/11/05/360%E5%BA%A6%E5%B1%95%E7%A4%BA.html)

注意，由于生成的w.avi，放入assests或raw中，在打包时，都会appt崩溃。所以要试用w.avi的，只能自己手动放到<appname>/files下面。如果不希望这样做，只能提供一个低精度的vedio.avi作为解压演示。

集成ffmpeg，编译ffmpeg其实是一个很蛋疼的事情，不过已经编译成android下可以的静态库了，具体见：[传送门](https://github.com/k-lam/ffmpeg-for-android)

openCV的集成，请参看openCV官网。
openCV配置好后，请修改jni/android.mk的

`include  D:\currentworkspace\android-ndk-r9d-windows-x86\android-ndk-r9d\sources\OpenCV-2.4.9-android-sdk\OpenCV-2.4.9-android-sdk\sdk\native\jni\OpenCV.mk`

这一句，改成自己本地OpenCV的位置。

`注：请不要震惊用到openCV，只是用到bitmap转png格式这么一丁点功能，只是我懒，没有找其他库，知道openCV有这样的方法，就直接用了`