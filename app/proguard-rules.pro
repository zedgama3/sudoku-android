# Keep kotlinx.serialization generated serializers
-keepclassmembers class net.zg3.sudoku.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class net.zg3.sudoku.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
