package `in`.growurdhan.app.presentation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.growurdhan.domain.vault.VaultAuthorization
import `in`.growurdhan.domain.vault.AppPasswordAuthorization

@Module
@InstallIn(SingletonComponent::class)
abstract class VaultAuthorizationModule {
    @Binds abstract fun bindAuthorization(gate: VaultSessionGate): VaultAuthorization
    @Binds abstract fun bindAppPassword(store: LocalAppPasswordStore): AppPasswordAuthorization
}
