/**
 * Custom AndroidJUnitRunner that uses Hilt's test Application class so
 * that @AndroidEntryPoint components (receivers, activities) work correctly
 * during instrumented tests.
 * Covers: ULT-5
 */
package com.minimalist.launcher

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
