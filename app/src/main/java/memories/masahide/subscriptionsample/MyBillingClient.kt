package memories.masahide.subscriptionsample

import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MyBillingClient : CoroutineScope, BillingClientStateListener,
  PurchasesUpdatedListener {


  companion object {
    //GooglePlayに登録した定期購読のIDです。適宜変えてください
    private const val ITEM1 = "test_item01"
    private const val ITEM2 = "test_item02"
  }

  //コネクションの開始が完了したかどうかのstatus
  enum class StartStatus {
    FINISHED, ERROR
  }

  private val startChannel: Channel<GooglePlayResult<GooglePlayException, StartStatus>> =
    Channel(Channel.UNLIMITED)
  private val purchaseChannel: Channel<GooglePlayResult<GooglePlayException, Purchase>> =
    Channel(Channel.UNLIMITED)

  private var job: Job = Job()
  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main


  private val billingClient: BillingClient =
    BillingClient.newBuilder(MyApplication.instance).setListener(this)
      .enablePendingPurchases()
      .build()


  override fun onBillingServiceDisconnected() {
  }

  override fun onBillingSetupFinished(billingResult: BillingResult?) {
    if (billingResult == null || billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
      startChannel.offer(Right(StartStatus.ERROR))
    } else {
      startChannel.offer(Right(StartStatus.FINISHED))
    }
  }

  override fun onPurchasesUpdated(
    billingResult: BillingResult?,
    purchases: MutableList<Purchase>?
  ) {
    if (billingResult == null) {
      return
    }
    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
      purchases.forEach { purchase ->
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
          val acknowledgePurchaseParams =
            AcknowledgePurchaseParams.newBuilder()
              .setPurchaseToken(purchase.purchaseToken)
              .build()
          billingClient.acknowledgePurchase(
            acknowledgePurchaseParams
          ) {
            purchaseChannel.offer(Right(purchase))
          }
        }
      }
    } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
      purchaseChannel.offer(Left(GooglePlayException()))
    } else {
      purchaseChannel.offer(Left(GooglePlayException()))
    }
  }


  /**
   * BillingClientのコネクションを開始します
   */
  suspend fun startConnection(): GooglePlayResult<GooglePlayException, StartStatus> {
    billingClient.startConnection(this)
    return startChannel.receive()
  }

  /**
   * GooglePlayから商品情報を取得します
   */
  suspend fun loadProducts(): GooglePlayResult<GooglePlayException, List<SkuDetails>> {
    return suspendCoroutine {
      val params = SkuDetailsParams.newBuilder()
      params.setType(BillingClient.SkuType.SUBS)
      //2つの商品を問い合わせるように変更
      params.setSkusList(
        listOf(
          ITEM1,
          ITEM2
        )
      )
      billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetails ->
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
          it.resume(Left(GooglePlayException()))
          return@querySkuDetailsAsync
        }
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
          it.resume(Right(skuDetails))
        }
      }
    }
  }

  /**
   * 定期購読を購入します
   */
  suspend fun subscribe(
    item: SkuDetails,//購入する商品
    activity: AppCompatActivity
  ): GooglePlayResult<GooglePlayException, Purchase> {
    val flowParams = BillingFlowParams.newBuilder()
      .setSkuDetails(item)
      .build()
    billingClient.launchBillingFlow(activity, flowParams)
    return purchaseChannel.receive()
  }

  /**
   * 現在有効なレシート情報を取得します
   */
  fun loadCurrentReceipt(): GooglePlayResult<GooglePlayException, List<Purchase>> {
    val purchaseResult =
      billingClient.queryPurchases(BillingClient.SkuType.SUBS)
    if (purchaseResult.responseCode == BillingClient.BillingResponseCode.OK
      && purchaseResult.purchasesList.isNotEmpty()
    ) {
      return Right(purchaseResult.purchasesList)
    }
    return Right(emptyList())
  }

  fun disconnect() {
    (job + Dispatchers.Default).cancel()
    billingClient.endConnection()
  }
}