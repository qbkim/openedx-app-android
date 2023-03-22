package com.raccoongang.core.utils

import com.raccoongang.core.AppDataConstants.USER_MAX_YEAR
import com.raccoongang.core.AppDataConstants.defaultLocale
import com.raccoongang.core.domain.model.RegistrationField
import com.raccoongang.core.utils.LocaleUtils.getLanguages
import java.util.*

object LocaleUtils {

    fun getBirthYearsRange(): List<RegistrationField.Option> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return (currentYear - USER_MAX_YEAR..currentYear - 0).reversed().map {
            RegistrationField.Option(it.toString(), it.toString(), "")
        }.toList()
    }

    fun isProfileLimited(inputYear: String?): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return if (!inputYear.isNullOrEmpty()) {
            currentYear - inputYear.toInt() < 13
        } else {
            true
        }
    }

    fun getCountries() = getAvailableCountries()

    fun getLanguages() = getAvailableLanguages()

    fun getCountryByCountryCode(code: String): String? {
        val countryISO = Locale.getISOCountries().firstOrNull { it == code }
        return countryISO?.let {
            Locale("", it).getDisplayCountry(defaultLocale)
        }
    }

    fun getLanguageByLanguageCode(code: String): String? {
        val countryISO = Locale.getISOLanguages().firstOrNull { it == code }
        return countryISO?.let {
            Locale(it, "").getDisplayLanguage(defaultLocale)
        }
    }

    private fun getAvailableCountries() = Locale.getISOCountries()
        .asSequence()
        .map {
            RegistrationField.Option(it, Locale("", it).getDisplayCountry(defaultLocale), "")
        }
        .sortedBy { it.name }
        .toList()


    private fun getAvailableLanguages() = Locale.getISOLanguages()
        .asSequence()
        .filter { it.length == 2 }
        .map {
            RegistrationField.Option(it, Locale(it, "").getDisplayLanguage(defaultLocale), "")
        }
        .sortedBy { it.name }
        .toList()

}

fun main() {
    println(getLanguages())
}
