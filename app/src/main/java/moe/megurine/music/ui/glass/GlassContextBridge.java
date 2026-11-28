package moe.megurine.music.ui.glass;

import top.yukonga.miuix.kmp.glass.internal.GlassHeaderContextKt;

public final class GlassContextBridge {
    private GlassContextBridge() {
    }

    public static Object localGlassTopAppBarContext() {
        return GlassHeaderContextKt.getLocalGlassHeaderContext();
    }
}
