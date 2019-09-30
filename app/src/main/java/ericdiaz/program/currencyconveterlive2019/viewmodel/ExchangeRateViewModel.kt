package ericdiaz.program.currencyconveterlive2019.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ericdiaz.program.currencyconveterlive2019.extensions.getExchangeValue

import javax.inject.Inject

import ericdiaz.program.currencyconveterlive2019.repository.BaseRepository
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers

class ExchangeRateViewModel @Inject constructor(private val exchangeRateRepository: BaseRepository) : BaseViewModel() {

    private val exchangeRateData = MutableLiveData<State>()

    fun getConversionValue(data: String,
                           baseCurrency: String,
                           foreignCurrency: String,
                           baseCurrencyAmount: String) {
        addDisposables(
                exchangeRateRepository
                        .requestExchangeRates(data, baseCurrency)
                        .delaySubscription(
                                Completable.fromAction { exchangeRateData.setValue(State.Loading) })
                        .map { (_, ratesMap) ->

                            val conversionRate = ratesMap[foreignCurrency]

                            conversionRate?.getExchangeValue(baseCurrencyAmount) ?: "Error, value is null"
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { exchangeRateResponse -> exchangeRateData.setValue(State.Success(exchangeRateResponse)) },
                                { throwable -> exchangeRateData.setValue(State.Failure(throwable)) }))
    }

    fun getExchangeRateData(): LiveData<State> {
        return exchangeRateData
    }
}