/*
 * Lynket
 *
 * Copyright (C) 2024 Arunkumar
 *
 * Replacement for dev.arunkumar.android:rx-utils SNAPSHOT dependency
 * RxJava utilities and scheduler compositions
 */
package dev.arunkumar.android.rxschedulers

import dev.arunkumar.android.common.Resource
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Wrap Observable emissions in Resource wrapper with automatic loading/error states
 */
fun <T> Observable<T>.asResource(): Observable<Resource<T>> {
    return this
        .map<Resource<T>> { Resource.Success(it) }
        .onErrorReturn { Resource.Error(it) }
        .startWith(Resource.Loading)
}

/**
 * Wrap Single emissions in Resource wrapper with automatic loading/error states
 */
fun <T> Single<T>.asResource(): Observable<Resource<T>> {
    return this.toObservable().asResource()
}

/**
 * Apply IO → UI thread scheduling
 * Subscribes on IO thread, observes on UI thread
 */
fun <T> SchedulerProvider.ioToUi(): (Observable<T>) -> Observable<T> = { observable ->
    observable
        .subscribeOn(io)
        .observeOn(ui)
}

/**
 * Apply Pool → UI thread scheduling
 * Subscribes on computation/pool thread, observes on UI thread
 */
fun <T> SchedulerProvider.poolToUi(): (Observable<T>) -> Observable<T> = { observable ->
    observable
        .subscribeOn(pool)
        .observeOn(ui)
}

/**
 * Apply computation → UI thread scheduling
 * Subscribes on computation thread, observes on UI thread
 */
fun <T> SchedulerProvider.computationToUi(): (Observable<T>) -> Observable<T> = { observable ->
    observable
        .subscribeOn(pool) // pool is computation in SchedulerProvider
        .observeOn(ui)
}

/**
 * Extension to apply transformer
 */
fun <T, R> Observable<T>.compose(transformer: (Observable<T>) -> Observable<R>): Observable<R> {
    return transformer(this)
}

/**
 * Apply Pool → UI thread scheduling for Completable
 * Subscribes on computation/pool thread, observes on UI thread
 */
fun SchedulerProvider.poolToUiCompletable(): (Completable) -> Completable = { completable ->
    completable
        .subscribeOn(pool)
        .observeOn(ui)
}

/**
 * Extension to apply transformer for Completable
 */
fun Completable.compose(transformer: (Completable) -> Completable): Completable {
    return transformer(this)
}
