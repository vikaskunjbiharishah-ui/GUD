package `in`.growurdhan.app.presentation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.growurdhan.app.billing.AnnualPlay
import `in`.growurdhan.app.billing.PlayBillingGateway

@Module
@InstallIn(SingletonComponent::class)
abstract class LicenceModule {
    @Binds abstract fun play(impl: PlayBillingGateway): AnnualPlay
    @Binds abstract fun clock(impl: PrefsTrialMemory): TrialMemory
    @Binds abstract fun install(impl: PackageInstallFacts): InstallFacts
}
