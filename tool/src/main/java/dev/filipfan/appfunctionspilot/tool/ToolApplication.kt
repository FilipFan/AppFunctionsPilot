package dev.filipfan.appfunctionspilot.tool

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import dev.filipfan.appfunctionspilot.tool.functions.FactoryCreatedFuncAImpl

class ToolApplication :
    Application(),
    AppFunctionConfiguration.Provider {
    // Explicitly define the instantiation method of a class,
    // which is applicable to cases of non-default construction.
    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(FactoryCreatedFuncAImpl::class.java) {
                FactoryCreatedFuncAImpl(
                    "Pong",
                )
            }
            .build()
}
