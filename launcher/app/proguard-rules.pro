# Shrink and optimise, but keep class and method names: the size win comes from dropping unused
# AndroidX code, not from renaming, and readable stack traces matter more in a sideloaded app
# where crash reports arrive as screenshots.
-dontobfuscate

# Keep the LofiMusicService so R8 doesn't strip it (referenced only in manifest)
-keep class com.gathin.tvhome.LofiMusicService { *; }
