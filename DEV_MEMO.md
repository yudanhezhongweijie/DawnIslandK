# Known Bugs
- ~~`ActivityResultContracts.TakePicture() ` `<Uri, Bitmap?>` cannot gurantee result of a bitmap(i.e. default camera), I reported [here](https://issuetracker.google.com/issues/154302879)~~ Fixed on `Activity 1.2.0-alpha05` by changed result to a boolean for succesful save

- [VP2 memory leak](https://issuetracker.google.com/issues/154751401). Fragment Navigation in a fragment within a VP2, which hosted by a Fragment causes memory leak.
- ~~[VP2 same direction nested scrolling](https://issuetracker.google.com/issues/123006042)~~ Currently solved by wrapping VP2 with a outer Frame and intercepts from parent

# Notes

## Saving image in Scoped Storage 
[Open Permission Denied](https://medium.com/@sriramaripirala/android-10-open-failed-eacces-permission-denied-da8b630a89df)
[Scope Stroage](https://proandroiddev.com/scoped-storage-on-android-11-2c5da70fb077)
[Handling files](https://android.jlelse.eu/handling-files-in-code-after-the-android-10-released-2bea0e16d35)

## Kotlin Coroutines
[Patterns](https://proandroiddev.com/kotlin-coroutines-patterns-anti-patterns-f9d12984c68e)
[Link1](https://medium.com/androiddevelopers/coroutines-on-android-part-iii-real-work-2ba8a2ec2f45)
[Link2](https://medium.com/capital-one-tech/kotlin-coroutines-on-android-things-i-wish-i-knew-at-the-beginning-c2f0b1f16cff)
[Flow](https://medium.com/androiddevelopers/lessons-learnt-using-coroutines-flow-4a6b285c0d06)
[Flow+MVVM](https://proandroiddev.com/using-coroutines-and-flow-with-mvvm-architecture-796142dbfc2f)
[Lazy and etc.](https://medium.com/@BladeCoder/exploring-kotlins-hidden-costs-part-3-3bf6e0dbf0a4)

## API Response Wrapper
**convert API responses to a sealed subclass like Resources.Success, Resources.Failed**
[sample1](https://developer.android.com/jetpack/docs/guide#addendum)
[sample2](https://android.jlelse.eu/android-networking-in-2019-retrofit-with-kotlins-coroutines-aefe82c4d777)

## Room
[Migration](https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929)
[Room + LiveData](https://medium.com/corouteam/exploring-kotlin-coroutines-and-lifecycle-architectural-components-integration-on-android-c63bb8a9156f)
[POJO error](https://stackoverflow.com/questions/44485631/room-persistence-errorentities-and-pojos-must-have-a-usable-public-constructor)
[Livedata not updated](https://stackoverflow.com/questions/44742445/room-livedata-observer-does-not-trigger-when-database-is-updated)
[Official Tips](https://medium.com/androiddevelopers/7-pro-tips-for-room-fbadea4bfbd1)

## LiveData
[LiveData & Fragment Lifecycle](https://medium.com/@BladeCoder/architecture-components-pitfalls-part-1-9300dd969808)
[LiveData & ViewModel,Repository](https://medium.com/androiddevelopers/viewmodels-and-livedata-patterns-antipatterns-21efaef74a54)
[cont.](https://medium.com/androiddevelopers/livedata-beyond-the-viewmodel-reactive-patterns-using-transformations-and-mediatorlivedata-fda520ba00b7)
** When using with room, ensure livedata has observer**

## Dagger
- [guide](https://medium.com/androiddevelopers/dagger-in-kotlin-gotchas-and-optimizations-7446d8dfd7dc)
- [Example - todoapp](https://github.com/android/architecture-samples)
- [Example - dagger-android](https://github.com/android/architecture-samples/tree/dagger-android)
- ViewInjection [Link1](https://stackoverflow.com/questions/44844149/customview-dependency-injection-with-dagger-2-within-activity-scope) [Link2](https://medium.com/@ghahremani/android-custom-view-lifecycle-with-dependency-injection-as-a-bonus-4a55217e15d8?sk=b62089ab35a5d0d0f379e194bbd2ae30) 
*Not Recommended*, but sometimes can be handy

## Touch
[onintercepttouchevent-vs-dispatchtouchevent](https://stackoverflow.com/questions/9586032/android-difference-between-onintercepttouchevent-and-dispatchtouchevent)

## Issued Faced
- When appbar ties with recyclerview for scrolling behavior, expand a view in RV to full screen is difficult. Tried solutions in https://stackoverflow.com/questions/32704775/appbarlayout-take-space-after-setvisibilityview-gone however no luck.

- Can disable appbar expansion using https://stackoverflow.com/questions/40636185/how-to-disable-expand-from-appbarlayout-in-fragment-that-has-a-nestedscrollvie & https://stackoverflow.com/questions/35821502/toolbar-expands-on-swipe-down/36018411#36018411 now uses toolbar only

[Expands toolbar(or other layout) to status bar](https://proandroiddev.com/draw-under-status-bar-like-a-pro-db38cfff2870)

[Kotlin data class with default values and null Gson Approach](https://proandroiddev.com/most-elegant-way-of-using-gson-kotlin-with-default-values-and-null-safety-b6216ac5328c), recommends using Moshi instead
