package ericdiaz.program.currencyconveterlive2019;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ericdiaz.program.data.repository.BaseRepository;
import ericdiaz.program.data.repository.ExchangeRateNetworkRepository;
import ericdiaz.program.currencyconveterlive2019.viewmodel.ExchangeRateViewModel;
import ericdiaz.program.currencyconveterlive2019.viewmodel.State;
import ericdiaz.program.data.model.ExchangeRateResponse;
import io.reactivex.Single;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.schedulers.Schedulers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ViewModelTest {

    @Rule // -> allows liveData to work on different thread besides main, must be public!
    public InstantTaskExecutorRule executorRule = new InstantTaskExecutorRule();

    private BaseRepository mockRepository;
    private ExchangeRateViewModel testSubject;
    private Observer<State> mockObserver;

    @Before
    public void setUp() {

        //sets a handler that is allowed to execute rx calls of the main thread.
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerCallable -> Schedulers.trampoline());

        //setup view model with dependencies mocked
        mockRepository = mock(ExchangeRateNetworkRepository.class);
        testSubject = new ExchangeRateViewModel(mockRepository);

        //mock the live data observer
        mockObserver = mock(Observer.class);
        testSubject.getExchangeRateData().observeForever(mockObserver);
    }

    @Test
    public void givenValidInputsWhenGetRatesCalledThenLiveDataObserverEmitsSuccessState() {
        //given
        String date = "2000-10-10";
        String baseCurrency = "USD";
        String foreignCurrency = "EUR";
        String baseCurrencyAmount = "100.00";
        String expectedConversionValue = "91.44";
        Single<ExchangeRateResponse> expectedResponse = Single.just(ExchangeRateResponse.Companion.getEMPTY());

        testSubject.baseCurrency = baseCurrency;
        testSubject.foreignCurrency = foreignCurrency;
        testSubject.baseCurrencyAmount = baseCurrencyAmount;
        //when
        when(mockRepository.requestExchangeRates(date, baseCurrency))
          .thenReturn(expectedResponse);

        testSubject.getConversionValue(date);

        //then
        State result = testSubject.getExchangeRateData().getValue();
        assertThat(result).isEqualTo(new State.Success(expectedConversionValue));

        verify(mockObserver).onChanged(isA(State.Loading.class));
        verify(mockObserver).onChanged(isA(State.Success.class));

        verify(mockObserver, times(1)).onChanged(isA(State.Loading.class));
        verify(mockObserver, times(1)).onChanged(isA(State.Success.class));

        verify(mockObserver, never()).onChanged(isA(State.Failure.class));

        verifyNoMoreInteractions(mockObserver);
    }

    @Test
    public void givenInvalidInputsWhenGetRatesCalledThenLiveDataObserverEmitsFailureState() {
        //given
        String date = "2000-10-10";
        String baseCurrency = "USD";
        String foreignCurrency = "EUR";
        String baseCurrencyAmount = "0.00";
        Exception expectedException = new IllegalStateException();
        Single<ExchangeRateResponse> expectedError = Single.error(expectedException);

        testSubject.baseCurrency = baseCurrency;
        testSubject.foreignCurrency = foreignCurrency;
        testSubject.baseCurrencyAmount = baseCurrencyAmount;

        //when
        when(mockRepository.requestExchangeRates(date, baseCurrency))
          .thenReturn(expectedError);

        testSubject.getConversionValue(date);

        //then
        State result = testSubject.getExchangeRateData().getValue();
        assertThat(result).isEqualTo(new State.Failure(expectedException));

        verify(mockObserver).onChanged(isA(State.Loading.class));
        verify(mockObserver).onChanged(isA(State.Failure.class));

        verify(mockObserver, times(1)).onChanged(isA(State.Loading.class));
        verify(mockObserver, times(1)).onChanged(isA(State.Failure.class));

        verify(mockObserver, never()).onChanged(isA(State.Success.class));

        verifyNoMoreInteractions(mockObserver);
    }

    @Test
    public void givenValidInputsWhenGetRatesCalledThenLiveDataObserverEmitsSuccessStateUSDtoHUF() {
        //given
        String date = "2000-10-10";
        String baseCurrency = "USD";
        String foreignCurrency = "HUF";
        String baseCurrencyAmount = "100.00";
        String expectedConversionValue = "30684.95";
        Single<ExchangeRateResponse> expectedResponse = Single.just(ExchangeRateResponse.Companion.getEMPTY());

        testSubject.baseCurrency = baseCurrency;
        testSubject.foreignCurrency = foreignCurrency;
        testSubject.baseCurrencyAmount = baseCurrencyAmount;
        //when
        when(mockRepository.requestExchangeRates(date, baseCurrency))
          .thenReturn(expectedResponse);

        testSubject.getConversionValue(date);

        //then
        State result = testSubject.getExchangeRateData().getValue();
        assertThat(result).isEqualTo(new State.Success(expectedConversionValue));
    }
}
