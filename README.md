# DawnIslandK
这是一个浮夸的A岛(adnmb.com)客户端，特性功能请看截图。
如果对app使用有问题或建议请提交新的issue。

**DawnIslandK** is an Android client for adnmb.com. It is based on MVVM, following structural designs of [todoapp](https://github.com/android/architecture-samples) and [dagger-android](https://github.com/android/architecture-samples/tree/dagger-android) by Google.

This app utilizes many latest Android features, concepts, propositions and guidlines, including but not limited to: Single Activity Application, Scoped Storage & MediaStore, Kotlin Coroutine, Navigations, Lifecycle, LiveData, Room, ActivityResultContracts, ViewPager2 and Material Design.

This app uses Retrofit2(& okhttp) for API calls, Dagger2 for Dependency Injection(required for repository behind MVVM), Moshi(which provides better support than Gson in Kotlin, i.e. data class constructor with default value). 

# Dev Memo
[Issue faced & Solution](https://github.com/fishballzzz/DawnIslandK/blob/master/DEV_MEMO.md)

# Screenshots
<img src="https://github.com/fishballzzz/DawnIslandK/blob/master/demo/demo1.gif" width="240">         <img src="https://github.com/fishballzzz/DawnIslandK/blob/master/demo/demo2.gif" width="240">       <img src="https://github.com/fishballzzz/DawnIslandK/blob/master/demo/demo3.gif" width="240"> 
<img src="https://github.com/fishballzzz/DawnIslandK/blob/master/demo/demo4.gif" width="240"> 

# Thanks
- [Nimingban](https://github.com/seven332/Nimingban)
- [DawnIsland](https://github.com/zwt-ss/DawnIsland) 
- @柔柔(代码) @五(设计) @安之(产品) @bug触发器(测试) @第九龙琦(趋势榜)

# License
```
Copyright 2020 Fishballzzz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

