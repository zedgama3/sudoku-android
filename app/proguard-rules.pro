# Keep kotlinx.serialization generated serializers
-keepclassmembers class com.zedgama.sudoku.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.zedgama.sudoku.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
