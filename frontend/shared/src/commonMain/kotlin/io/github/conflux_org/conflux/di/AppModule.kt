package io.github.conflux_org.conflux.di

import io.github.conflux_org.conflux.core.network.HttpClientFactory
import io.github.conflux_org.conflux.features.auth.data.repository.AuthRepositoryImpl
import io.github.conflux_org.conflux.features.auth.domain.repository.AuthRepository
import io.github.conflux_org.conflux.features.auth.presentation.AuthViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        single<CoroutineDispatcher> { Dispatchers.Main }
        single<HttpClient> { HttpClientFactory.create() }
        single<AuthRepository> { AuthRepositoryImpl(get()) }
        viewModel { AuthViewModel(get(), get()) }
    }
