package dev.onboard.app

import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddressProviderTest {
    @Test fun systemProviderResolvesAnActuallyRecordedCoordinate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("기기에 시스템 Geocoder 구현이 없습니다", Geocoder.isPresent())
        val visit = HistoryStore(context).use { store -> store.visits(false).firstOrNull {
            it.latitude != null && it.longitude != null
        } }
        assumeTrue("위치를 수집한 실제 방문이 필요합니다", visit != null)
        val geocoder = Geocoder(context, Locale.KOREA)
        val done = CountDownLatch(1)
        var found = false
        var failure = "주소 응답 없음"
        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocation(visit!!.latitude!!, visit.longitude!!, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    found = addresses.firstOrNull()?.getAddressLine(0)?.isNotBlank() == true
                    done.countDown()
                }
                override fun onError(errorMessage: String?) {
                    failure = "시스템 주소 제공자 오류"
                    done.countDown()
                }
            })
            assertTrue("주소 제공자 응답 20초 초과", done.await(20, TimeUnit.SECONDS))
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(visit!!.latitude!!, visit.longitude!!, 1)
            found = addresses?.firstOrNull()?.getAddressLine(0)?.isNotBlank() == true
        }
        assertTrue(failure, found)
    }
}
