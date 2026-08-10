package io.github.conflux_org.conflux.di

import io.github.conflux_org.conflux.core.network.HttpClientFactory
import io.github.conflux_org.conflux.data.repository.AuthRepositoryImpl
import io.github.conflux_org.conflux.data.repository.ChannelRepositoryImpl
import io.github.conflux_org.conflux.data.repository.GuildRepositoryImpl
import io.github.conflux_org.conflux.data.repository.MessageRepositoryImpl
import io.github.conflux_org.conflux.domain.repository.AuthRepository
import io.github.conflux_org.conflux.domain.repository.ChannelRepository
import io.github.conflux_org.conflux.domain.repository.GuildRepository
import io.github.conflux_org.conflux.domain.repository.MessageRepository
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
        single<ChannelRepository> { ChannelRepositoryImpl(get()) }
        single<GuildRepository> { GuildRepositoryImpl(get()) }
        single<MessageRepository> { MessageRepositoryImpl(get()) }
        viewModel { AuthViewModel(get(), get()) }
    }
