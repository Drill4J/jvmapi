package com.epam.drill.jvmapi

import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

val vmGlobal = AtomicReference<CPointer<JavaVMVar>?>(null).freeze()
val jvmti = AtomicReference<CPointer<jvmtiEnvVar>?>(null).freeze()


@CName("getJvm")
fun getJvm(): CPointer<JavaVMVar>? {
    return vmGlobal.value
}
@CName("currentEnvs")
fun currentEnvs(): JNIEnvPointer {
    return env
}

@CName("jvmtii")
fun jvmtii(): CPointer<jvmtiEnvVar>? = jvmti.value

fun AttachNativeThreadToJvm() {
    currentEnvs()
}

val env: JNIEnvPointer
    get() {
        return if (ex != null) {
            ex!!
        } else {
            memScoped {
                val vms = vmGlobal.value!!
                val vmFns = vms.pointed.value!!.pointed
                val jvmtiEnvPtr = alloc<CPointerVar<JNIEnvVar>>()
                vmFns.AttachCurrentThread!!(vms, jvmtiEnvPtr.ptr.reinterpret(), null)
                val value: CPointer<CPointerVarOf<JNIEnv>>? = jvmtiEnvPtr.value
                ex = value
                JNI_VERSION_1_6
                value!!
            }
        }
    }

val jni: JNI
    get() = env.pointed.pointed!!


@ThreadLocal
var ex: JNIEnvPointer? = null

typealias JNIEnvPointer = CPointer<JNIEnvVar>
typealias JNI = JNINativeInterface_