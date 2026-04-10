package com.samsse.streamingapp.di

import com.samsse.streamingapp.data.remote.NetworkModule
import com.samsse.streamingapp.data.remote.TokenManager
import com.samsse.streamingapp.data.repository.AuthRepository
import com.samsse.streamingapp.data.repository.HistoryRepository
import com.samsse.streamingapp.data.repository.MovieRepository
import com.samsse.streamingapp.data.repository.SeriesRepository
import com.samsse.streamingapp.data.repository.StreamRepository
import com.samsse.streamingapp.ui.auth.LoginViewModel
import com.samsse.streamingapp.ui.detail.DetailViewModel
import com.samsse.streamingapp.ui.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.samsse.streamingapp.ui.player.PlayerViewModel
import com.samsse.streamingapp.ui.search.SearchViewModel

val appModule = module {

    // Token Manager
    single { TokenManager(androidContext()) }

    // Network
    single { NetworkModule.provideOkHttpClient(get()) }
    single { NetworkModule.provideRetrofit(get()) }
    single { NetworkModule.provideApiService(get()) }

    // Repositories
    single { AuthRepository(get(), get()) }
    single { MovieRepository(get()) }
    single { SeriesRepository(get()) }
    single { StreamRepository(get()) }
    single { HistoryRepository(get()) }

    // ViewModels
    viewModel { LoginViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { PlayerViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { DetailViewModel(get(), get(), get(), get()) }
}