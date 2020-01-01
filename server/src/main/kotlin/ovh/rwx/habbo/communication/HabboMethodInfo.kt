package ovh.rwx.habbo.communication

import java.lang.invoke.MethodHandle

data class HabboMethodInfo(val requiredAuth: Boolean, val methodHandle: MethodHandle)