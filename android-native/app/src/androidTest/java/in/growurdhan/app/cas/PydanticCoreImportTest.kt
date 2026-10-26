package `in`.growurdhan.app.cas

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the debug-ABI pydantic-core wheel loads inside this app process
 * and executes a native validation call. This host can only boot an x86_64
 * image, so a passing run does not execute the arm64 phone wheel.
 * Does not parse a statement.
 */
@RunWith(AndroidJUnit4::class)
class PydanticCoreImportTest {
    @Test
    fun importsPydanticCoreAndValidatesAnInt() {
        val result = callProbe("pydantic_probe")
        assertEquals("2.50.1|ok", result)
    }

    @Test
    fun importsRealPydanticNotTheStub() {
        val result = callProbe("pydantic_model_probe")
        assertEquals("2.14.1|pydantic.main|ok", result)
    }

    @Test
    fun loadsFrozenCasparserModelThroughRealPydantic() {
        val result = callProbe("casparser_model_probe")
        assertEquals("1.4.1|casparser.types|1234.50|ok", result)
    }

    private fun callProbe(moduleName: String): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pythonClass = Class.forName("com.chaquo.python.Python")
        val started = pythonClass.getMethod("isStarted").invoke(null) as Boolean
        if (!started) {
            val platform = Class.forName("com.chaquo.python.android.AndroidPlatform")
                .getConstructor(Context::class.java)
                .newInstance(context)
            pythonClass.methods.single { it.name == "start" && it.parameterTypes.size == 1 }
                .invoke(null, platform)
        }
        val runtime = pythonClass.getMethod("getInstance").invoke(null)
        val module = runtime.javaClass.getMethod("getModule", String::class.java)
            .invoke(runtime, moduleName)
        val call = module.javaClass.methods.first {
            it.name == "callAttr" && it.parameterTypes.size == 2
        }
        return call.invoke(module, "probe", emptyArray<Any>()).toString()
    }
}
