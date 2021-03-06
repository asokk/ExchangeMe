package ericdiaz.program.currencyconveterlive2019.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ericdiaz.program.currencyconveterlive2019.extensions.getExchangeValue
import ericdiaz.program.data.model.ExchangeRateResponse
import ericdiaz.program.data.repository.ExchangeRateDatabaseRepository
import ericdiaz.program.data.repository.ExchangeRateNetworkRepository
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy

class ExchangeRateViewModel(private val exchangeRateNetworkRepository: ExchangeRateNetworkRepository,
                            private val exchangeRateDatabaseRepository: ExchangeRateDatabaseRepository) : BaseViewModel() {

    private val exchangeRateData = MutableLiveData<State>()
    private val currencyProfilesData = MutableLiveData<State>()
    lateinit var baseCurrency: String
    lateinit var foreignCurrency: String
    lateinit var baseCurrencyAmount: String

    companion object {
        fun formatConversionRate(baseCurrency: String, conversionRate: Double?, foreignCurrency: String): String {
            return "(1) $baseCurrency = ($conversionRate) $foreignCurrency"
        }

        fun formatLastUpdated(date: String): String {
            return "Rates as of : $date"
        }
    }

    fun getConversionValue(date: String = "latest") {
        if (baseCurrencyAmount != "0.00") {
            addDisposables(
                    exchangeRateDatabaseRepository
                            .requestExchangeRates(date, baseCurrency)

                            .delaySubscription(
                                    Completable.fromAction { exchangeRateData.setValue(State.Loading) }
                            )

                            .onErrorResumeNext {
                                exchangeRateNetworkRepository.requestExchangeRates(date, baseCurrency)
                                        .doOnSuccess { exchangeRateResponse ->
                                            exchangeRateResponse?.run {
                                                exchangeRateDatabaseRepository.insertExchangeRateResponse(exchangeRateResponse)
                                            }
                                        }
                            }

                            .map { (baseCurrency, ratesMap, date): ExchangeRateResponse ->

                                val conversionRate = ratesMap[foreignCurrency]

                                val conversionValue = conversionRate?.getExchangeValue(baseCurrencyAmount)
                                        ?: "Error, value is null"

                                State.Success(
                                        conversionValue,
                                        formatConversionRate(baseCurrency, conversionRate, foreignCurrency),
                                        formatLastUpdated(date))
                            }

                            .observeOn(AndroidSchedulers.mainThread())

                            .subscribeBy(
                                    onSuccess = { state -> exchangeRateData.value = state },
                                    onError = { throwable -> exchangeRateData.value = State.Failure(throwable) }
                            )
            )
        } else {
            exchangeRateData.value = State.Zero
        }
    }

    fun getCurrencyProfiles() {
        addDisposables(exchangeRateDatabaseRepository
                .requestCurrencyProfiles()
                .onErrorResumeNext {
                    exchangeRateNetworkRepository
                            .requestCurrencyProfiles()
                            .doOnSuccess {
                                exchangeRateDatabaseRepository.insertCurrencyProfileMap(it)
                            }.retry(3)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onSuccess = { response -> currencyProfilesData.value = State.Success(currencyProfileMap = response) },
                        onError = { throwable -> currencyProfilesData.value = State.Failure(throwable) }
                ))
    }

    fun getExchangeRateData(): LiveData<State> {
        return exchangeRateData
    }

    fun getCurrencyProfilesData(): LiveData<State> {
        return currencyProfilesData
    }
}
