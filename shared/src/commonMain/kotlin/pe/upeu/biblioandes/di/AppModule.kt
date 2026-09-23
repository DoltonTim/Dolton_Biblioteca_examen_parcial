package pe.upeu.biblioandes.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.upeu.biblioandes.data.repository.BibliotecaRepositoryFake
import pe.upeu.biblioandes.domain.repository.BibliotecaRepository
import pe.upeu.biblioandes.domain.usecase.ObtenerCatalogoUseCase
import pe.upeu.biblioandes.domain.usecase.ObtenerPrestamosUseCase
import pe.upeu.biblioandes.domain.usecase.SolicitarPrestamoUseCase
import pe.upeu.biblioandes.presentation.catalogo.CatalogoViewModel
import pe.upeu.biblioandes.presentation.detalle.DetalleLibroViewModel
import pe.upeu.biblioandes.presentation.prestamos.PrestamosViewModel

val dataModule = module {
    single<BibliotecaRepository> { BibliotecaRepositoryFake() }
}

val domainModule = module {
    factory { ObtenerCatalogoUseCase(get()) }
    factory { ObtenerPrestamosUseCase(get()) }
    factory { SolicitarPrestamoUseCase(get()) }
}

val presentationModule = module {
    viewModelOf(::CatalogoViewModel)
    viewModelOf(::DetalleLibroViewModel)
    viewModelOf(::PrestamosViewModel)
}

expect val platformModule: Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        dataModule,
        domainModule,
        presentationModule,
        platformModule
    )
}
