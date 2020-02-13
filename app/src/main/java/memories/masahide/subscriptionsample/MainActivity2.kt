package memories.masahide.subscriptionsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*

/**
 * 課金処理をActivityにまるっと詰め込んだ例
 */
class MainActivity2 : AppCompatActivity(), PurchasesUpdatedListener,
  BillingClientStateListener,
  AcknowledgePurchaseResponseListener, SkuDetailsResponseListener {


  private lateinit var billingClient: BillingClient

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    //1. BillingClientをを作成します
    billingClient =
      BillingClient.newBuilder(this).setListener(this)
        .enablePendingPurchases().build()
    //2. 接続を開始しBillingClientを使用可能な状態にします
    billingClient.startConnection(this)
  }

  //3. startConnection完了時のリスナーです。
  //このFinishedに入ればBillingClientの各メソッドを呼び出すことができます
  override fun onBillingSetupFinished(billingResult: BillingResult?) {
    //4. 定期購入のtest_itemの商品情報を問い合せます。
    val params = SkuDetailsParams.newBuilder()
    params.setSkusList(listOf("newspicks_1month_premium_01_03"))
      .setType(BillingClient.SkuType.SUBS)
    billingClient.querySkuDetailsAsync(params.build(), this)
  }

  //5. 4で定期購入の商品情報を問い合わせ完了時のリスナーです
  override fun onSkuDetailsResponse(
    billingResult: BillingResult?,
    skuDetails: MutableList<SkuDetails>?
  ) {
    if (billingResult == null || skuDetails == null) {
      return
    }
    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
      //6. 定期購入の購入ダイアログを表示します。
      val flowParams = BillingFlowParams.newBuilder()
        .setSkuDetails(skuDetails[0])
        .build()
      billingClient.launchBillingFlow(this, flowParams)
    }
  }


  //7. 6の購入ダイアログの表示後、どのようなアクションを取ったかのリスナーです。
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
          //8. 購入完了時、購入が承認されていない場合acknowledgeします
          //3日以内にacknowledgePurchaseを使用して決済の承認を行わない場合返金されます
          //本来はサーバーでやるのが良いです。
          val acknowledgePurchaseParams =
            AcknowledgePurchaseParams.newBuilder()
              .setPurchaseToken(purchase.purchaseToken)
              .build()
          billingClient.acknowledgePurchase(
            acknowledgePurchaseParams,
            this
          )
        }
      }
    } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
      //購入をキャンセルしたときの処理
    } else {
      //エラー時の処理
    }
  }


  //9. 購入を承認したときの処理です
  override fun onAcknowledgePurchaseResponse(billingResult: BillingResult?) {
    //ここまで来て定期購読が完全に購入状態になります。

  }

  override fun onBillingServiceDisconnected() {
  }

  override fun onDestroy() {
    super.onDestroy()
    billingClient.endConnection()
  }
}
