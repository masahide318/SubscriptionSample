package memories.masahide.subscriptionsample

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import kotlinx.coroutines.launch

class MainActivityViewModel(private val myBillingClient: MyBillingClient) :
  ViewModel() {

  private val _skuDetails = MutableLiveData<List<SkuDetails>>()
  val skuDetails: LiveData<List<SkuDetails>> = _skuDetails

  private val _purchase = MutableLiveData<Purchase>()
  val purchase: LiveData<Purchase> = _purchase

  fun startConnection() {
    viewModelScope.launch {
      //BillingClientの接続を開始
      val connectResult = myBillingClient.startConnection()
      connectResult.map {
        when (it) {
          MyBillingClient.StartStatus.FINISHED -> {
            //商品を情報を取得し、LiveDataに商品情報をPost
            val products = myBillingClient.loadProducts().getOrElse {
              //商品情報取得失敗
              return@launch
            }
            _skuDetails.postValue(products)
          }
          MyBillingClient.StartStatus.ERROR -> TODO()
        }
      }
    }
  }

  fun subscribe(skuDetails: SkuDetails, activity: AppCompatActivity) {
    viewModelScope.launch {
      val purchased =
        myBillingClient.subscribe(skuDetails, activity).getOrElse {
          //購入の失敗処理
          return@launch
        }
      //購入完了した商品情報をPOST
      _purchase.postValue(purchased)
    }
  }

  override fun onCleared() {
    super.onCleared()
    myBillingClient.disconnect()
  }
}