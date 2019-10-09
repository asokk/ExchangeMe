package ericdiaz.program.data

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import ericdiaz.program.data.db.ExchangeRateDatabase
import ericdiaz.program.data.model.ExchangeRateResponse
import org.junit.Test

class DatabaseTest {
    private val inMemorySqlDriver = JdbcSqliteDriver(IN_MEMORY).apply {
        ExchangeRateDatabase.Schema.create(this)
    }

    private val exchangeRateAdapter = ExchangeRates.Adapter(object : ColumnAdapter<Map<String, Double>, String> {
        override fun encode(value: Map<String, Double>): String {
            return Gson().toJson(value)
        }

        override fun decode(databaseValue: String): Map<String, Double> {
            return Gson().fromJson(databaseValue, object : TypeToken<Map<String, Double>>() {}.type)
        }
    })


    private val queries = ExchangeRateDatabase(inMemorySqlDriver, exchangeRateAdapter).exchangeRatesQueries

    @Test
    fun `Given a mock response, when mock is inserted and then retrieved, then assert retrieved result is equal to inserted`() {
        // given
        val mockResponse = ExchangeRateResponse.EMPTY

        //when
        queries.insertExchangeRates(
                mockResponse.date,
                mockResponse.ratesMap,
                mockResponse.baseCurrency)

        //then
        val result = queries.selectByDateAndBaseCurrency(
                ExchangeRateResponse.EMPTY.date,
                ExchangeRateResponse.EMPTY.baseCurrency)
                .executeAsOne()

        assertThat(result.exchangeRates_map).isEqualTo(mockResponse.ratesMap)
    }

    @Test
    fun `Given a mock response, when mock is inserted and then retrieved, then assert retrieved result is equal to inserted, then do a insertion and retrieval of a mock response with an empty map, then assert that when performing the same retrieval, the new mock is returned with the empty map`() {
        // Given
        val mockResponse = ExchangeRateResponse.EMPTY
        val expectedEmptyMap = emptyMap<String, Double>()

        // When
        queries.insertExchangeRates(
                mockResponse.date,
                mockResponse.ratesMap,
                mockResponse.baseCurrency)

        val firstInsertResult = queries.selectByDateAndBaseCurrency(
                ExchangeRateResponse.EMPTY.date,
                ExchangeRateResponse.EMPTY.baseCurrency)
                .executeAsOne()

        // Then
        assertThat(firstInsertResult.exchangeRates_map).isEqualTo(mockResponse.ratesMap)
        assertThat(firstInsertResult.exchangeRates_baseCurrency).isEqualTo(mockResponse.baseCurrency)
        assertThat(firstInsertResult.exchangeRates_date).isEqualTo(mockResponse.date)

        //And Then Do
        queries.insertExchangeRates(
                mockResponse.date,
                expectedEmptyMap,
                mockResponse.baseCurrency)

        val secondInsertResult = queries.selectByDateAndBaseCurrency(
                ExchangeRateResponse.EMPTY.date,
                ExchangeRateResponse.EMPTY.baseCurrency)
                .executeAsOne()

        //And Then Assert
        assertThat(secondInsertResult.exchangeRates_map).isEqualTo(expectedEmptyMap)
    }
}


